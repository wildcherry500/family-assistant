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
                                windowEvents.add(event);
                            }
                        }
                    }

                    // Apply accountLabel filter in memory
                    if (request.accountLabel != null) {
                        windowEvents.removeIf(ev ->
                            !request.accountLabel.equals(ev.get("accountLabel")));
                    }

                    // Sort by soonest first
                    windowEvents.sort((a, b) -> {
                        long ta = effectiveTime(a);
                        long tb = effectiveTime(b);
                        return Long.compare(ta, tb);
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
