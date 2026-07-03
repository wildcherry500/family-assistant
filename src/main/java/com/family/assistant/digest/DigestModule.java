package com.family.assistant.digest;

import com.rpl.agentorama.AgentModule;
import com.rpl.agentorama.AgentNode;
import com.rpl.agentorama.AgentTopology;
import com.rpl.agentorama.store.PStateStore;
import com.rpl.rama.Path;

import static com.family.assistant.util.EventUtils.*;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * DigestModule
 *
 * Produces a human-readable summary of upcoming family events and tasks
 * within a requested time window.
 *
 * Agent graph: query-events → build-summary → finalize
 *
 * Input:  DigestRequest (familyId, windowStartMs, windowEndMs)
 * Output: String digest text
 *
 * Stores accessed (read-only):
 *   $$family-data  (declared by FamilySchemaModule, read via getMirrorStore)
 *
 * Agent objects required:
 *   "openai-model"  — ChatModel (optional — used only for narrative polish node)
 *   "family-id"     — String partition key (fallback if not in request)
 */
public class DigestModule extends AgentModule implements java.io.Serializable {

    @Override
    public String getModuleName() {
        return "DigestModule";
    }

    // -----------------------------------------------------------------------
    // Weakness-map / leverage-map matching
    //
    // Both maps hold entries shaped { "silo"->String|null, "intent"->String|null, ... }.
    // A null/absent silo or intent on an entry is a wildcard for that dimension.
    // -----------------------------------------------------------------------

    private static boolean matchesDimension(Object entryVal, Object eventVal) {
        return entryVal == null || entryVal.equals(eventVal);
    }

    private static boolean entryMatches(Map<String, Object> entry, Map<String, Object> event) {
        return matchesDimension(entry.get("silo"), event.get("silo"))
            && matchesDimension(entry.get("intent"), event.get("intent"));
    }

    /** Highest weight among leverage entries matching this event, or 0 if none match. */
    private static long leverageScore(Map<String, Object> event, Map<String, Object> leverageEntries) {
        long score = 0;
        if (leverageEntries != null) {
            for (Object v : leverageEntries.values()) {
                @SuppressWarnings("unchecked")
                Map<String, Object> entry = (Map<String, Object>) v;
                if (entryMatches(entry, event)) {
                    long weight = toLong(entry.get("weight")) != null ? toLong(entry.get("weight")) : 0;
                    if (weight > score) score = weight;
                }
            }
        }
        return score;
    }

    /** Combined [tag] note text for every weakness entry matching this event, or null if none match. */
    private static String weaknessNote(Map<String, Object> event, Map<String, Object> weaknessEntries) {
        if (weaknessEntries == null) return null;
        StringBuilder sb = new StringBuilder();
        for (Object v : weaknessEntries.values()) {
            @SuppressWarnings("unchecked")
            Map<String, Object> entry = (Map<String, Object>) v;
            if (entryMatches(entry, event)) {
                String tag  = str(entry.get("tag"), "FLAGGED");
                String note = str(entry.get("note"), "");
                if (sb.length() > 0) sb.append("; ");
                sb.append("[").append(tag).append("]");
                if (!note.isEmpty()) sb.append(" ").append(note);
            }
        }
        return sb.length() > 0 ? sb.toString() : null;
    }

    // -----------------------------------------------------------------------
    // Input type
    // -----------------------------------------------------------------------
    public static class DigestRequest implements com.rpl.rama.RamaSerializable {
        public final String familyId;
        public final long windowStartMs;
        public final long windowEndMs;
        public final String accountLabel; // null = no filter; non-null = return only events from this account
        public final String timezone;     // e.g. "America/Los_Angeles"

        public DigestRequest(String familyId, long windowStartMs, long windowEndMs) {
            this(familyId, windowStartMs, windowEndMs, null, null);
        }

        public DigestRequest(String familyId, long windowStartMs, long windowEndMs, String accountLabel) {
            this(familyId, windowStartMs, windowEndMs, accountLabel, null);
        }

        public DigestRequest(String familyId, long windowStartMs, long windowEndMs,
                             String accountLabel, String timezone) {
            this.familyId       = familyId;
            this.windowStartMs  = windowStartMs;
            this.windowEndMs    = windowEndMs;
            this.accountLabel   = accountLabel;
            this.timezone       = timezone != null ? timezone : "America/Los_Angeles";
        }
    }

    // -----------------------------------------------------------------------
    // defineAgents
    // -----------------------------------------------------------------------
    @Override
    public void defineAgents(AgentTopology topology) {

        topology.newAgent("digest-agent")

            // ----------------------------------------------------------------
            // Node 1: query-events
            // Input:  DigestRequest
            // Output: emits (familyId, List<Map<String,Object>> events) to build-summary
            // ----------------------------------------------------------------
            .node("query-events", "build-summary",
                (AgentNode agentNode, DigestRequest request) -> {

                    PStateStore psMain = agentNode.getMirrorStore(
                        "FamilySchemaModule", "$$family-data");
                    PStateStore psDate = agentNode.getMirrorStore(
                        "FamilySchemaModule", "$$events-by-date");
                    PStateStore psLeverage = agentNode.getMirrorStore(
                        "FamilySchemaModule", "$$leverage-map");
                    PStateStore psWeakness = agentNode.getMirrorStore(
                        "FamilySchemaModule", "$$weakness-map");

                    // Use sorted date index for efficient range lookup
                    @SuppressWarnings("unchecked")
                    List<String> eventIds = (List<String>) (List<?>) psDate.select(
                        Path.key(request.familyId)
                            .sortedMapRange(request.windowStartMs, request.windowEndMs)
                            .mapVals().all());

                    List<Map<String, Object>> windowEvents = new ArrayList<>();
                    if (eventIds != null) {
                        for (String eventId : eventIds) {
                            @SuppressWarnings("unchecked")
                            Map<String, Object> event = (Map<String, Object>)
                                psMain.selectOne(Path.key(request.familyId)
                                                     .key("events").key(eventId));
                            if (event != null) {
                                windowEvents.add(new HashMap<>(event));
                            }
                        }
                    }

                    // Apply accountLabel filter in memory
                    if (request.accountLabel != null) {
                        windowEvents.removeIf(ev ->
                            !request.accountLabel.equals(ev.get("accountLabel")));
                    }

                    // weakness-map / leverage-map are per-family config; both are optional
                    // (empty or missing for a family is a graceful no-op, not a failure).
                    @SuppressWarnings("unchecked")
                    Map<String, Object> leverageEntries = (Map<String, Object>)
                        psLeverage.selectOne(Path.key(request.familyId));
                    @SuppressWarnings("unchecked")
                    Map<String, Object> weaknessEntries = (Map<String, Object>)
                        psWeakness.selectOne(Path.key(request.familyId));

                    for (Map<String, Object> event : windowEvents) {
                        String note = weaknessNote(event, weaknessEntries);
                        if (note != null) {
                            event.put("_weaknessNote", note);
                        }
                    }

                    // Sort by leverage score first (leverage matches float to the top),
                    // soonest-first as the tiebreak among equal (including zero/no-match) scores.
                    windowEvents.sort((a, b) -> {
                        long scoreA = leverageScore(a, leverageEntries);
                        long scoreB = leverageScore(b, leverageEntries);
                        if (scoreA != scoreB) return Long.compare(scoreB, scoreA);
                        return Long.compare(effectiveTime(a), effectiveTime(b));
                    });

                    agentNode.emit("build-summary", request.familyId, windowEvents, request.timezone);
                })

            // ----------------------------------------------------------------
            // Node 2: build-summary
            // Input:  String familyId, List<Map<String,Object>> events, String timezone
            // Output: emits String digestText to finalize
            // ----------------------------------------------------------------
            .node("build-summary", "finalize",
                (AgentNode agentNode, String familyId,
                 List<Map<String, Object>> events, String timezone) -> {

                    if (events.isEmpty()) {
                        agentNode.emit("finalize",
                            "No upcoming events or deadlines in this window.");
                        return;
                    }

                    DateTimeFormatter displayFmt = DateTimeFormatter
                        .ofPattern("EEE MMM d 'at' h:mm a")
                        .withZone(ZoneId.of(timezone));

                    StringBuilder sb = new StringBuilder();
                    sb.append("Family Digest — ")
                      .append(events.size())
                      .append(events.size() == 1 ? " item" : " items")
                      .append(" upcoming:\n\n");

                    for (Map<String, Object> event : events) {
                        String title      = str(event.get("title"), "Untitled");
                        String eventType  = str(event.get("eventType"), "event");
                        String status     = str(event.get("status"), "pending");
                        String assignedTo = str(event.get("assignedTo"), "unassigned");
                        Long startTime    = toLong(event.get("startTime"));
                        Long deadline     = toLong(event.get("deadline"));

                        sb.append("• ").append(title).append("\n");
                        sb.append("  Type: ").append(eventType).append("\n");

                        if (startTime != null) {
                            sb.append("  When: ")
                              .append(displayFmt.format(Instant.ofEpochMilli(startTime)))
                              .append("\n");
                        }
                        if (deadline != null) {
                            sb.append("  Deadline: ")
                              .append(displayFmt.format(Instant.ofEpochMilli(deadline)))
                              .append("\n");
                        }

                        sb.append("  Status: ").append(status).append("\n");
                        sb.append("  Assigned: ").append(assignedTo).append("\n");

                        String weaknessNote = str(event.get("_weaknessNote"), "");
                        if (!weaknessNote.isEmpty()) {
                            sb.append("  Note: ").append(weaknessNote).append("\n");
                        }

                        sb.append("\n");
                    }

                    // TODO: optionally pass digestText through an LLM polish node
                    // ChatModel model = (ChatModel) agentNode.getAgentObject("openai-model");
                    // String polished = model.generate("Rewrite as a friendly parent summary:\n" + sb);
                    // agentNode.emit("finalize", polished);

                    agentNode.emit("finalize", sb.toString());
                })

            // ----------------------------------------------------------------
            // Node 3: finalize  (terminal)
            // Input:  String digestText
            // Output: result(digestText)
            // ----------------------------------------------------------------
            .node("finalize", null,
                (AgentNode agentNode, String digestText) -> {
                    agentNode.result(digestText);
                });
    }

}
