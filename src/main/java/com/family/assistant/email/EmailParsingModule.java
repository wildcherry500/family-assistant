package com.family.assistant.email;

import com.family.assistant.gmail.GmailMessage;
import com.rpl.agentorama.AgentModule;
import com.rpl.agentorama.AgentNode;
import com.rpl.agentorama.AgentTopology;
import com.rpl.rama.AckLevel;
import com.rpl.rama.Depot;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.googleai.GoogleAiGeminiChatModel;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * EmailParsingModule
 *
 * Agent graph: classify → extract-details → write-to-store → finalize
 *
 * Input:  String rawEmail  (full email text)
 * Output: String eventId   (the ID written to $$family-data)
 *
 * Stores accessed:
 *   $$family-data  (declared by FamilySchemaModule, read/written via PStateStore)
 *
 * Agent objects required (declared by caller / test harness):
 *   "openai-model"   — ChatModel (LangChain4j or compatible)
 *   "family-id"      — String, the family partition key
 *
 * TODO (pending AgentTopology source read):
 *   - Confirm declareAgentObject syntax inside AgentModule.defineAgents()
 *   - Confirm PStateStore cross-module access pattern (getStore vs getMirrorStore)
 *   - Add LangChain4j model call once model dependency is in pom.xml
 */
public class EmailParsingModule extends AgentModule implements java.io.Serializable {

    @Override
    public String getModuleName() {
        return "EmailParsingModule";
    }

    // -----------------------------------------------------------------------
    // Classification result — passed between classify and extract-details nodes
    // -----------------------------------------------------------------------
    public enum EmailCategory {
        SCHOOL_EVENT,
        DEADLINE,
        PERMISSION_SLIP,
        TASK,
        UNKNOWN
    }

    // -----------------------------------------------------------------------
    // Simple data carrier — passed between extract-details and write-to-store
    // -----------------------------------------------------------------------
    public static class ParsedEvent implements com.rpl.rama.RamaSerializable {
        public final String category;
        public final String title;
        public final String description;
        public final String startTime;      // ISO-8601 — event date mentioned in content
        public final String deadline;       // ISO-8601 or null
        public final String childId;        // null until resolved
        public final String childName;      // extracted by LLM, e.g. "Billy"
        public final String sourceEmail;    // raw body for audit
        // Provenance fields
        public final String senderEmail;    // e.g. acemystuff@gmail.com
        public final String senderName;     // display name, may be null
        public final String emailSubject;   // Subject header
        public final String gmailMessageId; // Gmail message ID for traceability
        public final long   receivedAt;     // epoch millis when Gmail received the email
        public final String accountLabel;   // Gmail account that received this email, may be null
        public final String silo;           // VAULT, OFFICE, STUDIO, or UNKNOWN
        public final String intent;         // ACTION_REQUIRED, DECISION_NEEDED, FYI, SCHEDULING, or UNKNOWN
        // Typed relation triples: subject is implicit (this event's own id, filled in at
        // write-to-store, never emitted by the LLM). Each entry: relation/objectType/object,
        // all validated against a closed enum before landing here (see parseRelations).
        public final List<Map<String, String>> relations;

        public ParsedEvent(String category, String title, String description,
                           String startTime, String deadline,
                           String childId, String childName, String sourceEmail,
                           String senderEmail, String senderName, String emailSubject,
                           String gmailMessageId, long receivedAt, String accountLabel,
                           String silo, String intent, List<Map<String, String>> relations) {
            this.category      = category;
            this.title         = title;
            this.description   = description;
            this.startTime     = startTime;
            this.deadline      = deadline;
            this.childId       = childId;
            this.childName     = childName;
            this.sourceEmail   = sourceEmail;
            this.senderEmail   = senderEmail;
            this.senderName    = senderName;
            this.emailSubject  = emailSubject;
            this.gmailMessageId = gmailMessageId;
            this.receivedAt    = receivedAt;
            this.accountLabel  = accountLabel;
            this.silo          = silo;
            this.intent        = intent;
            this.relations     = relations;
        }
    }

    // -----------------------------------------------------------------------
    // defineAgents — single entry point for AgentModule
    // -----------------------------------------------------------------------
    @Override
    public void defineAgents(AgentTopology topology) {

        topology.declareAgentObject("family-id", "keeling-family-001");

        topology.declareAgentObjectBuilder("gemini-model",
            (setup) -> {
                String key = System.getProperty("GEMINI_API_KEY",
                                System.getenv("GEMINI_API_KEY"));
                return GoogleAiGeminiChatModel.builder()
                    .apiKey(key)
                    .modelName("gemini-2.5-flash")
                    .maxRetries(5)
                    .build();
            });

        // ------------------------------------------------------------------
        // Agent: email-parsing-agent
        // Graph: classify → extract-details → write-to-store → finalize
        // ------------------------------------------------------------------
        topology.newAgent("email-parsing-agent")

            // ----------------------------------------------------------------
            // Node 0: persist-raw  (write-ahead log — runs before any parsing)
            // Input:  GmailMessage message  (agent entry point)
            // Output: emits the unchanged message to classify
            //
            // Durably appends the complete raw email to FamilySchemaModule's
            // *raw-emails depot BEFORE parsing, so a future reparse-on-replay can
            // regenerate every PState from the original input. Appended as a Map
            // carrying familyId so the depot's hashBy("familyId") co-partitions it
            // with *family-events. Append-only by design; parsed-event idempotency
            // is unaffected because events key on gmailMessageId in write-to-store.
            // ----------------------------------------------------------------
            .node("persist-raw", "classify",
                (AgentNode agentNode, GmailMessage message) -> {

                    String familyId = (String) agentNode.getAgentObject("family-id");

                    Map<String, Object> raw = new HashMap<>();
                    raw.put("familyId",       familyId);
                    raw.put("body",           message.body);
                    raw.put("emailSubject",   message.emailSubject);
                    raw.put("senderEmail",    message.senderEmail);
                    raw.put("senderName",     message.senderName);
                    raw.put("gmailMessageId", message.gmailMessageId);
                    raw.put("accountLabel",   message.accountLabel);
                    raw.put("receivedAt",     message.receivedAt);

                    Depot rawDepot = agentNode.getMirrorDepot("FamilySchemaModule", "*raw-emails");
                    // APPEND_ACK: block until the raw record is durably appended and
                    // replicated before parsing proceeds ("write-ahead" semantics).
                    rawDepot.append(raw, AckLevel.APPEND_ACK);

                    agentNode.emit("classify", message);
                })

            // ----------------------------------------------------------------
            // Node 1: classify
            // Input:  String rawEmail
            // Output: emits (rawEmail, category) to extract-details
            // ----------------------------------------------------------------
            .node("classify", "extract-details",
                (AgentNode agentNode, GmailMessage message) -> {

                    ChatModel model = (ChatModel) agentNode.getAgentObject("gemini-model");
                    String classifyPrompt = "Classify this email along three independent dimensions. "
                        + "Reply with only valid JSON, no markdown fences:\n"
                        + "{\"category\": \"SCHOOL_EVENT|DEADLINE|PERMISSION_SLIP|TASK|UNKNOWN\", "
                        + "\"silo\": \"VAULT|OFFICE|STUDIO|UNKNOWN\", "
                        + "\"intent\": \"ACTION_REQUIRED|DECISION_NEEDED|FYI|SCHEDULING|UNKNOWN\"}\n\n"
                        + "category: what the event IS.\n"
                        + "silo: which life domain it belongs to — VAULT (personal/family: logistics, "
                        + "medical, private financial, household), OFFICE (business: clients, operations, "
                        + "strategy, business correspondence), STUDIO (creative/public: art, music, "
                        + "content, cultural projects, public-facing work).\n"
                        + "intent: what the email asks of you — ACTION_REQUIRED (must do something: "
                        + "sign, pay, reply, attend), DECISION_NEEDED (must choose before anything can "
                        + "proceed), FYI (awareness only, nothing required), SCHEDULING (primarily a "
                        + "calendar/time-coordination matter).\n"
                        + "Use UNKNOWN for any dimension you are not confident about — never guess.\n\n"
                        + message.body;

                    String classifyJson = model.chat(classifyPrompt).trim();

                    String categoryStr = "UNKNOWN";
                    String silo = "UNKNOWN";
                    String intent = "UNKNOWN";
                    try {
                        String clean = classifyJson.replaceAll("```json", "").replaceAll("```", "").trim();
                        com.fasterxml.jackson.databind.ObjectMapper mapper =
                            new com.fasterxml.jackson.databind.ObjectMapper();
                        Map<String, String> parsed = mapper.readValue(clean,
                            new com.fasterxml.jackson.core.type.TypeReference<Map<String, String>>() {});

                        String cat = parsed.get("category");
                        if (cat != null && cat.toUpperCase()
                                .matches("SCHOOL_EVENT|DEADLINE|PERMISSION_SLIP|TASK|UNKNOWN")) {
                            categoryStr = cat.toUpperCase();
                        }
                        String s = parsed.get("silo");
                        if (s != null && s.toUpperCase().matches("VAULT|OFFICE|STUDIO|UNKNOWN")) {
                            silo = s.toUpperCase();
                        }
                        String i = parsed.get("intent");
                        if (i != null && i.toUpperCase()
                                .matches("ACTION_REQUIRED|DECISION_NEEDED|FYI|SCHEDULING|UNKNOWN")) {
                            intent = i.toUpperCase();
                        }
                    } catch (Exception e) {
                        // keep UNKNOWN defaults for silo/intent — never guess;
                        // category falls back to keyword classification below
                    }

                    if ("UNKNOWN".equals(categoryStr)) {
                        categoryStr = classifyByKeyword(message.body);
                    }

                    agentNode.emit("extract-details", message, categoryStr, silo, intent);
                })

            // ----------------------------------------------------------------
            // Node 2: extract-details
            // Input:  String rawEmail, String categoryStr, String silo, String intent
            // Output: emits ParsedEvent to write-to-store
            // ----------------------------------------------------------------
            .node("extract-details", "write-to-store",
                (AgentNode agentNode, GmailMessage message, String categoryStr,
                 String silo, String intent) -> {

                    ChatModel model = (ChatModel) agentNode.getAgentObject("gemini-model");
                    String today = java.time.LocalDate.now().toString();
                    String extractPrompt = "Today's date is " + today + ". All dates should be in 2026 unless explicitly stated otherwise. "
                        + "Extract structured data from this email. "
                        + "Reply with only valid JSON, no markdown fences:\n"
                        + "{\"title\": \"short title\", "
                        + "\"startTime\": \"ISO-8601 datetime or null\", "
                        + "\"deadline\": \"ISO-8601 datetime or null\", "
                        + "\"childName\": \"first name of child or student mentioned, or null\", "
                        + "\"relations\": [{\"relation\": \"MENTIONS_PERSON|PART_OF|LOCATED_AT|ACTION_NEEDED|UNKNOWN\", "
                        + "\"objectType\": \"PERSON|ORG|PLACE|PROJECT|UNKNOWN\", \"object\": \"the mentioned name\"}]}\n\n"
                        + "For childName: extract any student or child first name explicitly mentioned "
                        + "(e.g. 'Billy', 'Emma'). Use null if no specific child is named.\n\n"
                        + "For relations: emit one entry per distinct person, organization, place, or "
                        + "project explicitly mentioned in the email. relation describes how it connects "
                        + "to this email's event — MENTIONS_PERSON (a person is named), PART_OF (this "
                        + "event/task is part of a larger project or effort), LOCATED_AT (a place is "
                        + "where this happens), ACTION_NEEDED (this specific person needs to take "
                        + "action, distinct from merely being mentioned). objectType is what kind of "
                        + "thing \"object\" is. Use UNKNOWN for either field only when genuinely "
                        + "uncertain — never guess. Omit relations entirely (empty array) if nothing "
                        + "qualifies.\n\n"
                        + message.body;
                    String json = model.chat(extractPrompt).trim();

                    String title     = message.emailSubject != null
                                       ? message.emailSubject : extractTitle(message.body);
                    String startTime = null;
                    String deadline  = null;
                    String childName = null;
                    List<Map<String, String>> relations = new ArrayList<>();
                    try {
                        com.fasterxml.jackson.databind.ObjectMapper mapper =
                            new com.fasterxml.jackson.databind.ObjectMapper();
                        Map<String, Object> extracted = mapper.readValue(json,
                            new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() {});
                        if (extracted.get("title") instanceof String) title = (String) extracted.get("title");
                        String st = (String) extracted.get("startTime");
                        String dl = (String) extracted.get("deadline");
                        String cn = (String) extracted.get("childName");
                        startTime = (st == null || st.equals("null")) ? null : st;
                        deadline  = (dl == null || dl.equals("null")) ? null : dl;
                        childName = (cn == null || cn.equals("null")) ? null : cn;
                        relations = parseRelations(extracted.get("relations"));
                    } catch (Exception e) {
                        // keep fallback values set above
                    }

                    ParsedEvent event = new ParsedEvent(
                        categoryStr, title, message.body,
                        startTime, deadline,
                        null, childName, message.body,
                        message.senderEmail, message.senderName,
                        message.emailSubject, message.gmailMessageId,
                        message.receivedAt, message.accountLabel,
                        silo, intent, relations
                    );

                    agentNode.emit("write-to-store", event);
                })

            // ----------------------------------------------------------------
            // Node 3: write-to-store
            // Input:  ParsedEvent
            // Output: emits eventId String to finalize
            // ----------------------------------------------------------------
            .node("write-to-store", "finalize",
                (AgentNode agentNode, ParsedEvent event) -> {

                    // Use gmailMessageId as eventId for natural idempotency —
                    // writing the same message twice overwrites with identical data.
                    String eventId = (event.gmailMessageId != null && !event.gmailMessageId.isBlank())
                        ? event.gmailMessageId
                        : UUID.randomUUID().toString();
                    long now = System.currentTimeMillis();

                    String familyId = (String) agentNode.getAgentObject("family-id");

                    // tags: List<String> replacing the single-value eventType. This session the
                    // classifier still emits one category, so tags carries 0..1 element; the List
                    // shape is what lets a later parser attach several (e.g. PERMISSION_SLIP + DEADLINE).
                    List<String> tags = new ArrayList<>();
                    if (event.category != null && !event.category.isBlank()) {
                        tags.add(event.category);
                    }

                    // personId: List<String> replacing childId/childName. This session it carries the
                    // extracted child *name* (0..1 element) until an id-resolver is built; the List shape
                    // is what lets a later parser tag every family member (senders and subjects both).
                    List<String> personId = new ArrayList<>();
                    if (event.childName != null && !event.childName.isBlank()) {
                        personId.add(event.childName);
                    }

                    Map<String, Object> eventRecord = new HashMap<>();
                    eventRecord.put("id",             eventId);
                    eventRecord.put("familyId",       familyId);
                    eventRecord.put("personId",       personId);
                    eventRecord.put("sourceType",     "email");
                    eventRecord.put("accountLabel",   event.accountLabel);
                    eventRecord.put("tags",           tags);
                    // Typed relation triples (source-neutral contract — any future parser
                    // populating this same field gets edge materialization for free from
                    // FamilySchemaModule; subject is this record's own "id", filled in there).
                    eventRecord.put("relations",      event.relations);
                    // Classifier-output fields — schema only this session. Plumbed into the record and
                    // serialization but NOT populated by the parsing agent; null/empty is the correct
                    // passing state until the classifier is wired to fill them. confidence is a Double
                    // (never Integer) for serialization consistency.
                    eventRecord.put("documentType",    (String) null);
                    eventRecord.put("relatedEventIds", new ArrayList<String>());
                    eventRecord.put("confidence",      (Double) null);
                    eventRecord.put("reason",          (String) null);
                    eventRecord.put("silo",           event.silo);
                    eventRecord.put("intent",         event.intent);
                    eventRecord.put("title",          event.title);
                    eventRecord.put("description",    event.description);
                    eventRecord.put("startTime",      parseIsoToEpoch(event.startTime));
                    eventRecord.put("deadline",       parseIsoToEpoch(event.deadline));
                    eventRecord.put("receivedAt",     event.receivedAt);
                    eventRecord.put("senderEmail",    event.senderEmail);
                    eventRecord.put("senderName",     event.senderName);
                    eventRecord.put("emailSubject",   event.emailSubject);
                    eventRecord.put("gmailMessageId", event.gmailMessageId);
                    eventRecord.put("status",         "pending");
                    eventRecord.put("created",        now);
                    eventRecord.put("updated",        now);

                    Depot depot = agentNode.getMirrorDepot("FamilySchemaModule", "*family-events");
                    depot.append(eventRecord);

                    agentNode.emit("finalize", eventId);
                })

            // ----------------------------------------------------------------
            // Node 4: finalize  (terminal)
            // Input:  String eventId
            // Output: result(eventId) — returned to caller
            // ----------------------------------------------------------------
            .node("finalize", null,
                (AgentNode agentNode, String eventId) -> {
                    // Terminal node — set the agent result
                    agentNode.result(eventId);
                });
    }

    // -----------------------------------------------------------------------
    // Private helpers (stubs — replace with LLM calls)
    // -----------------------------------------------------------------------

    private String classifyByKeyword(String email) {
        String lower = email.toLowerCase();
        if (lower.contains("permission") || lower.contains("consent")) return "PERMISSION_SLIP";
        if (lower.contains("deadline") || lower.contains("due date"))  return "DEADLINE";
        if (lower.contains("event") || lower.contains("field trip"))   return "SCHOOL_EVENT";
        if (lower.contains("task") || lower.contains("to-do"))         return "TASK";
        return "UNKNOWN";
    }

    private Long parseIsoToEpoch(String iso) {
        if (iso == null || iso.isBlank() || "null".equalsIgnoreCase(iso)) return null;
        try {
            return Instant.parse(iso).toEpochMilli();
        } catch (DateTimeParseException e1) {
            try {
                // ISO-8601 local datetime without zone — treat as UTC
                return LocalDateTime.parse(iso).toInstant(ZoneOffset.UTC).toEpochMilli();
            } catch (DateTimeParseException e2) {
                try {
                    // Date-only string like "2026-06-20"
                    return LocalDate.parse(iso).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli();
                } catch (DateTimeParseException e3) {
                    return null;
                }
            }
        }
    }

    /**
     * Validates the LLM's raw "relations" array against the closed relation/objectType
     * enums before it reaches the depot — same never-guess posture as classify's
     * category/silo/intent regex checks. Malformed or unrecognized entries are dropped,
     * not coerced to UNKNOWN, since a shape we don't recognize isn't safely "unknown."
     */
    private List<Map<String, String>> parseRelations(Object raw) {
        List<Map<String, String>> relations = new ArrayList<>();
        if (!(raw instanceof List)) return relations;
        for (Object item : (List<?>) raw) {
            if (!(item instanceof Map)) continue;
            Map<?, ?> m = (Map<?, ?>) item;
            Object relationObj = m.get("relation");
            Object objectTypeObj = m.get("objectType");
            Object objectObj = m.get("object");
            if (!(relationObj instanceof String) || !(objectTypeObj instanceof String)
                    || !(objectObj instanceof String)) continue;
            String relation = ((String) relationObj).toUpperCase();
            String objectType = ((String) objectTypeObj).toUpperCase();
            String object = (String) objectObj;
            if (object.isBlank()) continue;
            if (!relation.matches("MENTIONS_PERSON|PART_OF|LOCATED_AT|ACTION_NEEDED|UNKNOWN")) continue;
            if (!objectType.matches("PERSON|ORG|PLACE|PROJECT|UNKNOWN")) continue;

            Map<String, String> triple = new HashMap<>();
            triple.put("relation", relation);
            triple.put("objectType", objectType);
            triple.put("object", object);
            relations.add(triple);
        }
        return relations;
    }

    private String extractTitle(String email) {
        // Naive: use the first non-blank line as title
        for (String line : email.split("\\n")) {
            String trimmed = line.trim();
            if (!trimmed.isEmpty()) return trimmed.substring(0, Math.min(trimmed.length(), 80));
        }
        return "Untitled Email";
    }
}
