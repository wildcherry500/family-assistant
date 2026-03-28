package com.family.assistant.digest;

import com.rpl.agentorama.AgentModule;
import com.rpl.agentorama.AgentNode;
import com.rpl.agentorama.AgentTopology;
import com.rpl.agentorama.store.PStateStore;
import com.rpl.rama.Path;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
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
    // Input type
    // -----------------------------------------------------------------------
    public static class DigestRequest implements com.rpl.rama.RamaSerializable {
        public final String familyId;
        public final long windowStartMs;
        public final long windowEndMs;

        public DigestRequest(String familyId, long windowStartMs, long windowEndMs) {
            this.familyId       = familyId;
            this.windowStartMs  = windowStartMs;
            this.windowEndMs    = windowEndMs;
        }
    }

    private static final DateTimeFormatter DISPLAY_FMT =
        DateTimeFormatter.ofPattern("EEE MMM d 'at' h:mm a")
                         .withZone(ZoneId.of("America/Los_Angeles"));

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

                    PStateStore ps = agentNode.getMirrorStore(
                        "FamilySchemaModule", "$$family-data");

                    // Pull all events for this family
                    // TODO: replace with a selective path query once we confirm
                    //       sorted/filtered PState path syntax from RPL docs.
                    //       For now we read all events and filter in memory.
                    Map<String, Object> allEvents = (Map<String, Object>)
                        ps.selectOne(Path.key(request.familyId).key("events"));

                    List<Map<String, Object>> windowEvents = new ArrayList<>();

                    if (allEvents != null) {
                        for (Map.Entry<String, Object> entry : allEvents.entrySet()) {
                            Map<String, Object> event =
                                (Map<String, Object>) entry.getValue();

                            // Filter by time window using startTime or deadline
                            Long startTime = toLong(event.get("startTime"));
                            Long deadline  = toLong(event.get("deadline"));
                            long relevant  = startTime != null ? startTime
                                           : deadline  != null ? deadline
                                           : Long.MAX_VALUE;

                            if (relevant >= request.windowStartMs
                                    && relevant <= request.windowEndMs) {
                                windowEvents.add(event);
                            }
                        }
                    }

                    // Sort by soonest first
                    windowEvents.sort((a, b) -> {
                        long ta = effectiveTime(a);
                        long tb = effectiveTime(b);
                        return Long.compare(ta, tb);
                    });

                    agentNode.emit("build-summary", request.familyId, windowEvents);
                })

            // ----------------------------------------------------------------
            // Node 2: build-summary
            // Input:  String familyId, List<Map<String,Object>> events
            // Output: emits String digestText to finalize
            // ----------------------------------------------------------------
            .node("build-summary", "finalize",
                (AgentNode agentNode, String familyId,
                 List<Map<String, Object>> events) -> {

                    if (events.isEmpty()) {
                        agentNode.emit("finalize",
                            "No upcoming events or deadlines in this window.");
                        return;
                    }

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
                              .append(DISPLAY_FMT.format(Instant.ofEpochMilli(startTime)))
                              .append("\n");
                        }
                        if (deadline != null) {
                            sb.append("  Deadline: ")
                              .append(DISPLAY_FMT.format(Instant.ofEpochMilli(deadline)))
                              .append("\n");
                        }

                        sb.append("  Status: ").append(status).append("\n");
                        sb.append("  Assigned: ").append(assignedTo).append("\n");
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

    // -----------------------------------------------------------------------
    // Private helpers
    // -----------------------------------------------------------------------

    private Long toLong(Object val) {
        if (val == null) return null;
        if (val instanceof Long)    return (Long) val;
        if (val instanceof Integer) return ((Integer) val).longValue();
        if (val instanceof String s) {
            try { return Long.parseLong(s); } catch (NumberFormatException ignored) {}
            try {
                // ISO-8601 local datetime (no zone) — treat as UTC
                String normalized = s.length() == 19 ? s + "Z" : s;
                return Instant.parse(normalized).toEpochMilli();
            } catch (Exception ignored) {}
        }
        return null;
    }

    private long effectiveTime(Map<String, Object> event) {
        Long s = toLong(event.get("startTime"));
        Long d = toLong(event.get("deadline"));
        if (s != null) return s;
        if (d != null) return d;
        return Long.MAX_VALUE;
    }

    private String str(Object val, String fallback) {
        if (val == null) return fallback;
        String s = val.toString().trim();
        return s.isEmpty() ? fallback : s;
    }
}
