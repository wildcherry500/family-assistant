package com.family.assistant.query;

import com.rpl.agentorama.AgentModule;
import com.rpl.agentorama.AgentNode;
import com.rpl.agentorama.AgentTopology;
import com.rpl.agentorama.store.PStateStore;
import com.rpl.rama.Path;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.googleai.GoogleAiGeminiChatModel;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * QueryModule
 *
 * Answers natural language questions about family events and tasks.
 *
 * Agent graph: interpret-query → fetch-data → generate-answer → finalize
 *
 * Input:  QueryRequest (familyId, question, requesterTimezone)
 * Output: String natural language answer
 *
 * Reads from: $family-data (via getMirrorStore from FamilySchemaModule)
 *
 * Agent objects required:
 *   "gemini-model" — ChatModel
 */
public class QueryModule extends AgentModule implements java.io.Serializable {

    @Override
    public String getModuleName() {
        return "QueryModule";
    }

    // -----------------------------------------------------------------------
    // Input type
    // -----------------------------------------------------------------------
    public static class QueryRequest implements com.rpl.rama.RamaSerializable {
        public final String familyId;
        public final String question;
        public final String requesterTimezone; // e.g. "America/Los_Angeles", "America/New_York"
        public final String accountLabel;      // null = no filter; non-null = restrict to this account

        public QueryRequest(String familyId, String question, String requesterTimezone) {
            this(familyId, question, requesterTimezone, null);
        }

        public QueryRequest(String familyId, String question, String requesterTimezone, String accountLabel) {
            this.familyId           = familyId;
            this.question           = question;
            this.requesterTimezone  = requesterTimezone != null
                                      ? requesterTimezone : "America/Los_Angeles";
            this.accountLabel       = accountLabel;
        }
    }

    // -----------------------------------------------------------------------
    // Internal data carrier — interpret-query → fetch-data
    // -----------------------------------------------------------------------
    public static class QueryParams implements com.rpl.rama.RamaSerializable {
        public final String queryType;      // UPCOMING_EVENTS, DEADLINES, TASKS, SPECIFIC_DATE, GENERAL
        public final String childName;      // null if not specified
        public final String dateFrom;       // ISO-8601 or null
        public final String dateTo;         // ISO-8601 or null
        public final String categoryFilter; // SCHOOL_EVENT, PERMISSION_SLIP, TASK, etc. or null
        public final String originalQuestion;
        public final String familyId;
        public final String requesterTimezone;
        public final String accountLabel;   // null = no filter; non-null = restrict to this account

        public QueryParams(String queryType, String childName,
                           String dateFrom, String dateTo, String categoryFilter,
                           String originalQuestion, String familyId,
                           String requesterTimezone) {
            this(queryType, childName, dateFrom, dateTo, categoryFilter,
                 originalQuestion, familyId, requesterTimezone, null);
        }

        public QueryParams(String queryType, String childName,
                           String dateFrom, String dateTo, String categoryFilter,
                           String originalQuestion, String familyId,
                           String requesterTimezone, String accountLabel) {
            this.queryType         = queryType;
            this.childName         = childName;
            this.dateFrom          = dateFrom;
            this.dateTo            = dateTo;
            this.categoryFilter    = categoryFilter;
            this.originalQuestion  = originalQuestion;
            this.familyId          = familyId;
            this.requesterTimezone = requesterTimezone;
            this.accountLabel      = accountLabel;
        }
    }

    // -----------------------------------------------------------------------
    // defineAgents
    // -----------------------------------------------------------------------
    @Override
    public void defineAgents(AgentTopology topology) {

        // Declare the Gemini model
        topology.declareAgentObjectBuilder("gemini-model", setup -> {
            String apiKey = System.getenv("GEMINI_API_KEY");
            if (apiKey == null || apiKey.isBlank()) return null;
            return GoogleAiGeminiChatModel.builder()
                .apiKey(apiKey)
                .modelName("gemini-2.5-flash")
                .build();
        });

        topology.newAgent("query-agent")

            // ----------------------------------------------------------------
            // Node 1: interpret-query
            // Input:  QueryRequest
            // Output: QueryParams → fetch-data
            // ----------------------------------------------------------------
            .node("interpret-query", "fetch-data",
                (AgentNode agentNode, QueryRequest request) -> {

                    ChatModel model =
                        (ChatModel) agentNode.getAgentObject("gemini-model");

                    QueryParams params;

                    if (model == null) {
                        // Stub fallback — no API key
                        params = new QueryParams(
                            "UPCOMING_EVENTS", null, null, null, null,
                            request.question, request.familyId,
                            request.requesterTimezone, request.accountLabel);
                    } else {
                        String today = Instant.now()
                            .atZone(ZoneId.of(request.requesterTimezone))
                            .toLocalDate().toString();

                        String prompt =
                            "You are a family assistant query parser. Today is " + today + ".\n\n" +
                            "Parse this question into structured JSON. Respond ONLY with JSON, no markdown.\n\n" +
                            "Question: \"" + request.question + "\"\n\n" +
                            "Return this exact JSON structure:\n" +
                            "{\n" +
                            "  \"queryType\": \"UPCOMING_EVENTS|DEADLINES|TASKS|SPECIFIC_DATE|GENERAL\",\n" +
                            "  \"childName\": \"name or null\",\n" +
                            "  \"dateFrom\": \"YYYY-MM-DD or null\",\n" +
                            "  \"dateTo\": \"YYYY-MM-DD or null\",\n" +
                            "  \"categoryFilter\": \"SCHOOL_EVENT|PERMISSION_SLIP|TASK|DEADLINE|null\"\n" +
                            "}\n\n" +
                            "Rules:\n" +
                            "- 'this week' = " + today + " to 7 days from now\n" +
                            "- 'today' = " + today + "\n" +
                            "- 'next week' = 7 to 14 days from now\n" +
                            "- If no date range implied, use null for both dates\n" +
                            "- If no child mentioned, use null for childName";

                        String response = model.chat(prompt);
                        params = parseQueryParams(
                            response, request.question,
                            request.familyId, request.requesterTimezone,
                            request.accountLabel);
                    }

                    agentNode.emit("fetch-data", params);
                })

            // ----------------------------------------------------------------
            // Node 2: fetch-data
            // Input:  QueryParams
            // Output: (QueryParams, List<Map> matchingEvents) → generate-answer
            //
            // When childName or categoryFilter is set, uses inverted index
            // PStates ($$events-by-child, $$events-by-category) to narrow
            // candidate event IDs before fetching full records from $$family-data.
            // When neither filter is present, falls back to a full scan so that
            // date-only queries continue to work (no date index exists yet).
            // ----------------------------------------------------------------
            .node("fetch-data", "generate-answer",
                (AgentNode agentNode, QueryParams params) -> {

                    PStateStore psMain = agentNode.getMirrorStore(
                        "FamilySchemaModule", "$$family-data");
                    PStateStore psChild = agentNode.getMirrorStore(
                        "FamilySchemaModule", "$$events-by-child");
                    PStateStore psCategory = agentNode.getMirrorStore(
                        "FamilySchemaModule", "$$events-by-category");

                    List<Map<String, Object>> candidates = new ArrayList<>();

                    if (params.childName != null || params.categoryFilter != null) {
                        // ---------------------------------------------------
                        // Index-assisted path
                        // ---------------------------------------------------
                        Set<String> candidateIds = null;

                        if (params.childName != null) {
                            // TODO: normalize case — index keys use the exact childName
                            // stored in the event record (e.g. "Billy"), so lookup is
                            // case-sensitive for now.  A future enhancement should
                            // store index keys in lower-case and compare lower-cased.
                            Set<String> childSet = (Set<String>)
                                psChild.selectOne(Path.key(params.familyId)
                                                     .key(params.childName));
                            candidateIds = childSet != null
                                ? new HashSet<>(childSet)
                                : new HashSet<>();
                        }

                        if (params.categoryFilter != null) {
                            Set<String> catSet = (Set<String>)
                                psCategory.selectOne(Path.key(params.familyId)
                                                        .key(params.categoryFilter));
                            if (candidateIds == null) {
                                candidateIds = catSet != null
                                    ? new HashSet<>(catSet)
                                    : new HashSet<>();
                            } else {
                                candidateIds.retainAll(
                                    catSet != null ? catSet : new HashSet<>());
                            }
                        }

                        // Fetch full event records for each candidate ID
                        if (candidateIds != null) {
                            for (String eventId : candidateIds) {
                                Map<String, Object> event = (Map<String, Object>)
                                    psMain.selectOne(Path.key(params.familyId)
                                                        .key("events")
                                                        .key(eventId));
                                if (event != null) {
                                    candidates.add(event);
                                }
                            }
                        }
                    } else {
                        // ---------------------------------------------------
                        // No index-applicable filters — full scan as before
                        // ---------------------------------------------------
                        Map<String, Object> allEvents = (Map<String, Object>)
                            psMain.selectOne(Path.key(params.familyId).key("events"));

                        if (allEvents != null) {
                            for (Object val : allEvents.values()) {
                                candidates.add((Map<String, Object>) val);
                            }
                        }
                    }

                    // Apply date filter in memory (no date index exists yet)
                    List<Map<String, Object>> matched = new ArrayList<>();
                    long fromMs = parseToEpoch(params.dateFrom, Long.MIN_VALUE);
                    long toMs   = parseToEpoch(params.dateTo,   Long.MAX_VALUE);

                    for (Map<String, Object> event : candidates) {
                        long eventTime = effectiveTime(event);
                        boolean inWindow = (params.dateFrom == null && params.dateTo == null)
                            || (eventTime >= fromMs && eventTime <= toMs);
                        if (inWindow) {
                            matched.add(event);
                        }
                    }

                    // Apply accountLabel filter in memory
                    if (params.accountLabel != null) {
                        matched.removeIf(ev ->
                            !params.accountLabel.equals(ev.get("accountLabel")));
                    }

                    // Sort by soonest first
                    matched.sort((a, b) ->
                        Long.compare(effectiveTime(a), effectiveTime(b)));

                    agentNode.emit("generate-answer", params, matched);
                })

            // ----------------------------------------------------------------
            // Node 3: generate-answer
            // Input:  QueryParams, List<Map> matchingEvents
            // Output: String answer → finalize
            // ----------------------------------------------------------------
            .node("generate-answer", "finalize",
                (AgentNode agentNode, QueryParams params,
                 List<Map<String, Object>> events) -> {

                    ChatModel model =
                        (ChatModel) agentNode.getAgentObject("gemini-model");

                    String answer;

                    if (events.isEmpty()) {
                        answer = "I didn't find any events matching your question"
                            + (params.childName != null ? " for " + params.childName : "")
                            + ".";
                    } else if (model == null) {
                        // Stub fallback
                        answer = formatEventsPlain(events, params.requesterTimezone);
                    } else {
                        String eventSummary = formatEventsForPrompt(
                            events, params.requesterTimezone);

                        String prompt =
                            "You are a helpful family assistant. Answer the parent's question " +
                            "naturally and concisely based on the family event data below.\n\n" +
                            "Question: \"" + params.originalQuestion + "\"\n\n" +
                            "Family event data:\n" + eventSummary + "\n\n" +
                            "Rules:\n" +
                            "- Be warm and helpful, like a knowledgeable friend\n" +
                            "- Mention specific dates, times, and requirements\n" +
                            "- If pickup or dropoff is needed, highlight it\n" +
                            "- Keep the answer concise — 2-4 sentences max unless detail is needed\n" +
                            "- Times should be in " + params.requesterTimezone + " timezone";

                        answer = model.chat(prompt);
                    }

                    agentNode.emit("finalize", answer);
                })

            // ----------------------------------------------------------------
            // Node 4: finalize (terminal)
            // ----------------------------------------------------------------
            .node("finalize", null,
                (AgentNode agentNode, String answer) -> {
                    agentNode.result(answer);
                });
    }

    // -----------------------------------------------------------------------
    // Private helpers
    // -----------------------------------------------------------------------

    private QueryParams parseQueryParams(String json, String originalQuestion,
                                          String familyId, String timezone, String accountLabel) {
        try {
            String clean = json.replaceAll("```json", "").replaceAll("```", "").trim();
            // Simple field extraction without Jackson dependency
            String queryType      = extractJsonString(clean, "queryType", "GENERAL");
            String childName      = extractJsonString(clean, "childName", null);
            String dateFrom       = extractJsonString(clean, "dateFrom", null);
            String dateTo         = extractJsonString(clean, "dateTo", null);
            String categoryFilter = extractJsonString(clean, "categoryFilter", null);

            if ("null".equalsIgnoreCase(childName))      childName = null;
            if ("null".equalsIgnoreCase(dateFrom))       dateFrom = null;
            if ("null".equalsIgnoreCase(dateTo))         dateTo = null;
            if ("null".equalsIgnoreCase(categoryFilter)) categoryFilter = null;

            return new QueryParams(queryType, childName, dateFrom, dateTo,
                categoryFilter, originalQuestion, familyId, timezone, accountLabel);
        } catch (Exception e) {
            return new QueryParams("GENERAL", null, null, null, null,
                originalQuestion, familyId, timezone, accountLabel);
        }
    }

    private String extractJsonString(String json, String key, String defaultVal) {
        String pattern = "\"" + key + "\"";
        int idx = json.indexOf(pattern);
        if (idx < 0) return defaultVal;
        int colon = json.indexOf(":", idx);
        if (colon < 0) return defaultVal;
        String rest = json.substring(colon + 1).trim();
        if (rest.startsWith("\"")) {
            int end = rest.indexOf("\"", 1);
            return end > 0 ? rest.substring(1, end) : defaultVal;
        }
        if (rest.startsWith("null")) return null;
        return defaultVal;
    }

    private long parseToEpoch(String dateStr, long fallback) {
        if (dateStr == null) return fallback;
        try {
            return Instant.parse(dateStr + "T00:00:00Z").toEpochMilli();
        } catch (Exception e) {
            try {
                return Instant.parse(dateStr).toEpochMilli();
            } catch (Exception e2) {
                return fallback;
            }
        }
    }

    private long effectiveTime(Map<String, Object> event) {
        Long s = toLong(event.get("startTime"));
        Long d = toLong(event.get("deadline"));
        if (s != null) return s;
        if (d != null) return d;
        return Long.MAX_VALUE;
    }

    private Long toLong(Object val) {
        if (val == null) return null;
        if (val instanceof Long)    return (Long) val;
        if (val instanceof Integer) return ((Integer) val).longValue();
        if (val instanceof String) {
            String s = (String) val;
            try { return Long.parseLong(s); } catch (Exception ignored) {}
            try { return Instant.parse(s).toEpochMilli(); } catch (Exception ignored) {}
            try { return Instant.parse(s + "T00:00:00Z").toEpochMilli(); }
            catch (Exception ignored) {}
        }
        return null;
    }

    private boolean childNameMatches(Map<String, Object> event, String filter) {
        String lower = filter.toLowerCase();
        String childId   = str(event.get("childId"),   "").toLowerCase();
        String childName = str(event.get("childName"), "").toLowerCase();
        return childId.contains(lower) || childName.contains(lower);
    }

    private String str(Object val, String fallback) {
        if (val == null) return fallback;
        String s = val.toString().trim();
        return s.isEmpty() ? fallback : s;
    }

    private String formatEventsForPrompt(List<Map<String, Object>> events,
                                          String timezone) {
        ZoneId zone = ZoneId.of(timezone);
        DateTimeFormatter fmt = DateTimeFormatter
            .ofPattern("EEEE MMMM d 'at' h:mm a z").withZone(zone);
        StringBuilder sb = new StringBuilder();
        for (Map<String, Object> e : events) {
            sb.append("- ").append(str(e.get("title"), "Untitled")).append("\n");
            sb.append("  Type: ").append(str(e.get("eventType"), "unknown")).append("\n");
            Long st = toLong(e.get("startTime"));
            Long dl = toLong(e.get("deadline"));
            if (st != null) sb.append("  Date: ")
                .append(fmt.format(Instant.ofEpochMilli(st))).append("\n");
            if (dl != null) sb.append("  Deadline: ")
                .append(fmt.format(Instant.ofEpochMilli(dl))).append("\n");
            Object desc = e.get("description");
            if (desc != null) sb.append("  Notes: ")
                .append(desc.toString(), 0,
                    Math.min(desc.toString().length(), 200)).append("\n");
        }
        return sb.toString();
    }

    private String formatEventsPlain(List<Map<String, Object>> events, String timezone) {
        StringBuilder sb = new StringBuilder("Here's what I found:\n\n");
        ZoneId zone = ZoneId.of(timezone);
        DateTimeFormatter fmt = DateTimeFormatter
            .ofPattern("EEE MMM d 'at' h:mm a").withZone(zone);
        for (Map<String, Object> e : events) {
            sb.append("• ").append(str(e.get("title"), "Untitled")).append("\n");
            Long st = toLong(e.get("startTime"));
            Long dl = toLong(e.get("deadline"));
            if (st != null) sb.append("  When: ")
                .append(fmt.format(Instant.ofEpochMilli(st))).append("\n");
            if (dl != null) sb.append("  Due: ")
                .append(fmt.format(Instant.ofEpochMilli(dl))).append("\n");
        }
        return sb.toString();
    }
}
