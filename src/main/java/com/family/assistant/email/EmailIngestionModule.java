package com.family.assistant.email;

import com.family.assistant.gmail.GmailMessage;
import com.rpl.agentorama.AgentClient;
import com.rpl.agentorama.AgentModule;
import com.rpl.agentorama.AgentNode;
import com.rpl.agentorama.AgentTopology;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;

/**
 * EmailIngestionModule
 *
 * Accepts a batch of raw email strings and fans each one out to
 * email-parsing-agent (declared in EmailParsingModule) in parallel.
 *
 * Agent graph: ingest → finalize
 *
 * Input:  List<String> rawEmails
 * Output: IngestionResult (eventIds, skipped count, failed count)
 *
 * Null or blank emails are silently skipped.
 * Parsing failures per email are counted but do not abort the batch.
 */
public class EmailIngestionModule extends AgentModule implements java.io.Serializable {

    // -----------------------------------------------------------------------
    // Result type returned to callers
    // -----------------------------------------------------------------------
    public static class IngestionResult implements com.rpl.rama.RamaSerializable {
        public final List<String> eventIds;  // one per successfully parsed email
        public final int skipped;            // null/blank emails ignored
        public final int failed;             // emails that threw during parsing

        public IngestionResult(List<String> eventIds, int skipped, int failed) {
            this.eventIds = eventIds;
            this.skipped  = skipped;
            this.failed   = failed;
        }

        @Override
        public String toString() {
            return "IngestionResult{parsed=" + eventIds.size()
                + ", skipped=" + skipped
                + ", failed=" + failed + "}";
        }
    }

    @Override
    public String getModuleName() {
        return "EmailIngestionModule";
    }

    @Override
    public void defineAgents(AgentTopology topology) {

        topology.newAgent("email-ingestion-agent")

            // ----------------------------------------------------------------
            // Node 1: ingest
            // Input:  List<String> rawEmails
            // Output: emits IngestionResult to finalize
            // ----------------------------------------------------------------
            .node("ingest", "finalize",
                (AgentNode agentNode, List<GmailMessage> messages) -> {

                    AgentClient parsingClient = agentNode.getMirrorAgentClient(
                        "EmailParsingModule", "email-parsing-agent");

                    // invokeAsync is not supported for mirror agent clients — use blocking
                    // invoke dispatched onto virtual threads for true parallelism.
                    int skipped = 0;
                    List<CompletableFuture<String>> futures = new ArrayList<>();

                    try (var exec = Executors.newVirtualThreadPerTaskExecutor()) {
                        for (GmailMessage msg : messages) {
                            if (msg == null || msg.body == null || msg.body.isBlank()) {
                                skipped++;
                                continue;
                            }
                            final GmailMessage m = msg;
                            futures.add(CompletableFuture.supplyAsync(
                                () -> parsingClient.invoke(m), exec));
                        }

                        List<String> eventIds = new ArrayList<>();
                        int failed = 0;
                        for (CompletableFuture<String> f : futures) {
                            try {
                                eventIds.add(f.get());
                            } catch (Exception ex) {
                                failed++;
                            }
                        }

                        agentNode.emit("finalize", new IngestionResult(eventIds, skipped, failed));
                    }
                })

            // ----------------------------------------------------------------
            // Node 2: finalize  (terminal)
            // ----------------------------------------------------------------
            .node("finalize", null,
                (AgentNode agentNode, IngestionResult result) -> {
                    agentNode.result(result);
                });
    }
}
