package com.family.assistant;

import com.family.assistant.email.EmailIngestionModule;
import com.family.assistant.email.EmailParsingModule;
import com.family.assistant.gmail.GmailMessage;
import com.family.assistant.schema.FamilySchemaModule;
import com.rpl.agentorama.AgentClient;
import com.rpl.agentorama.AgentManager;
import com.rpl.rama.AckLevel;
import com.rpl.rama.Depot;
import com.rpl.rama.Path;
import com.rpl.rama.PState;
import com.rpl.rama.RamaModule;
import com.rpl.rama.test.InProcessCluster;
import com.rpl.rama.test.LaunchConfig;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * IngestionFailureHandlingTest
 *
 * Covers the four guarded call sites added by the "Ingestion Failure Handling" plan
 * (persist-raw, classify, extract-details, write-to-store in EmailParsingModule) and the
 * new $$ingestion-failures PState (FamilySchemaModule). Also the first real, whole-graph
 * test of Step 0a's hypothesis: does an uncaught agent-node exception (after
 * Agent-o-rama's own built-in max.retries fault-tolerance is exhausted) propagate as a
 * normal Java exception through the FULL multi-node chain, all the way to
 * EmailIngestionModule's existing `f.get()` catch, or only from a single node in
 * isolation? See RAMA_VERIFIED_LEARNINGS.md for the answer this test established.
 *
 * No GEMINI_API_KEY required — every ChatModel here is a test fake injected via
 * EmailParsingModule.setModelOverrideForTesting(...).
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class IngestionFailureHandlingTest {

    private static final String SCHEMA_MODULE_NAME = "FamilySchemaModule";
    private static final String FAMILY_ID = "keeling-family-001"; // EmailParsingModule's hardcoded family-id agent object

    // -----------------------------------------------------------------------
    // Minimal schema-module test doubles — each registers under the literal
    // module name "FamilySchemaModule" (matching EmailParsingModule's hardcoded
    // getMirrorDepot("FamilySchemaModule", ...) call sites) but declares only the
    // depots/PStates needed to force ONE specific guarded append to fail, while still
    // supporting *ingestion-failures so the failure record itself can be verified.
    // -----------------------------------------------------------------------

    /** Omits *raw-emails — forces persist-raw's append to fail. */
    public static class SchemaWithoutRawEmails implements RamaModule, java.io.Serializable {
        @Override public String getModuleName() { return SCHEMA_MODULE_NAME; }
        @Override
        public void define(Setup setup, Topologies topologies) {
            declareIngestionFailuresOnly(setup, topologies);
        }
    }

    /** Omits *family-events — forces write-to-store's append to fail. */
    public static class SchemaWithoutFamilyEvents implements RamaModule, java.io.Serializable {
        @Override public String getModuleName() { return SCHEMA_MODULE_NAME; }
        @Override
        public void define(Setup setup, Topologies topologies) {
            setup.declareDepot("*raw-emails", Depot.hashBy("familyId"));
            declareIngestionFailuresOnly(setup, topologies);
        }
    }

    private static void declareIngestionFailuresOnly(RamaModule.Setup setup, RamaModule.Topologies topologies) {
        setup.declareDepot("*ingestion-failures", Depot.hashBy("familyId"));
        var stream = topologies.stream("ingestion-failures-only-stream");
        stream.pstate("$$ingestion-failures",
            PState.mapSchema(String.class,
                PState.mapSchema(String.class, Object.class)));
        stream.source("*ingestion-failures").out("*failure")
          .select("*failure", Path.key("familyId")).out("*familyId")
          .select("*failure", Path.key("id")).out("*failureId")
          .hashPartition("*familyId")
          .localTransform("$$ingestion-failures",
              Path.key("*familyId").key("*failureId").termVal("*failure"));
    }

    // -----------------------------------------------------------------------
    // Fake ChatModels — override doChat(ChatRequest), NOT chat(String). ChatModel is a
    // pure-default interface in langchain4j 1.8.0 (verified by javap), but chat(String)'s
    // default implementation does NOT reliably dispatch to an anonymous subclass's own
    // chat(String) override under Agent-o-rama's agent-object pooling (verified empirically
    // this session: overriding chat(String) directly resulted in "RuntimeException: Not
    // implemented" — the real chain runs chat(String) -> chat(ChatRequest) -> doChat(...),
    // and doChat's own unimplemented default is what actually got hit). doChat is the root
    // of that chain, per javap -c on ChatModel's default chat(ChatRequest) bytecode, so
    // overriding it directly is robust regardless of which higher-level method is called.
    // -----------------------------------------------------------------------

    private static String promptTextOf(ChatRequest request) {
        return request.messages().toString();
    }

    private static ChatResponse jsonResponse(String json) {
        return ChatResponse.builder().aiMessage(AiMessage.from(json)).build();
    }

    private static ChatModel throwingModel(AtomicInteger callCount) {
        return new ChatModel() {
            @Override
            public ChatResponse doChat(ChatRequest chatRequest) {
                callCount.incrementAndGet();
                throw new RuntimeException("Simulated LLM failure for test");
            }
        };
    }

    private static ChatModel classifyOkExtractThrows() {
        return new ChatModel() {
            @Override
            public ChatResponse doChat(ChatRequest chatRequest) {
                if (promptTextOf(chatRequest).contains("Extract structured data")) {
                    throw new RuntimeException("Simulated extract-details LLM failure for test");
                }
                return jsonResponse("{\"category\":\"TASK\",\"silo\":\"VAULT\",\"intent\":\"FYI\"}");
            }
        };
    }

    private static ChatModel alwaysValid() {
        return new ChatModel() {
            @Override
            public ChatResponse doChat(ChatRequest chatRequest) {
                if (promptTextOf(chatRequest).contains("Extract structured data")) {
                    return jsonResponse("{\"title\":\"Test event\",\"startTime\":null,"
                        + "\"deadline\":null,\"childName\":null,\"relations\":[]}");
                }
                return jsonResponse("{\"category\":\"TASK\",\"silo\":\"VAULT\",\"intent\":\"FYI\"}");
            }
        };
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private GmailMessage message(String gmailMessageId) {
        return new GmailMessage(
            "Test email body for ingestion failure handling.",
            gmailMessageId,
            "test@example.com", null, "Test subject",
            System.currentTimeMillis(), null);
    }

    /** Mirrors EmailParsingModule.recordIngestionFailure's own deterministic id exactly. */
    private String failureId(String gmailMessageId, String failedNode) {
        return UUID.nameUUIDFromBytes(
            (gmailMessageId + "|" + failedNode).getBytes(StandardCharsets.UTF_8)).toString();
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> readFailureRecord(PState ingestionFailures, String gmailMessageId, String failedNode) {
        return (Map<String, Object>) ingestionFailures.selectOne(
            Path.key(FAMILY_ID).key(failureId(gmailMessageId, failedNode)));
    }

    /**
     * Proves the cluster is still healthy after a forced failure — NOT crash-looped —
     * by performing an independent, unrelated write and confirming it completes
     * (doesn't hang or throw), the same shape of check DerivationsPartialWriteProbeTest
     * used to detect a genuine crash-loop.
     */
    private void assertClusterStillHealthy(InProcessCluster ipc) {
        Depot failuresDepot = ipc.clusterDepot(SCHEMA_MODULE_NAME, "*ingestion-failures");
        Map<String, Object> probe = new HashMap<>();
        probe.put("id", "health-check-" + UUID.randomUUID());
        probe.put("familyId", FAMILY_ID);
        probe.put("failedNode", "health-check");
        assertDoesNotThrow(() -> failuresDepot.append(probe, AckLevel.ACK),
            "cluster must still be healthy and accepting writes after the forced failure — "
            + "a crash-loop (per RAMA_VERIFIED_LEARNINGS.md's Gate 3 finding) would make "
            + "this hang or throw");
    }

    // -----------------------------------------------------------------------
    // Test 1 — persist-raw guard (Step 1, highest priority)
    // -----------------------------------------------------------------------

    @Test
    void persistRawFailureIsGuardedRecordedAndStopsTheGraph() throws Exception {
        try (InProcessCluster ipc = InProcessCluster.create()) {
            ipc.launchModule(new SchemaWithoutRawEmails(), new LaunchConfig(1, 1));

            EmailParsingModule parsingModule = new EmailParsingModule();
            // Deliberately the THROWING fake, not alwaysValid — belt-and-braces: even if the
            // graph incorrectly proceeded past persist-raw, classify would ALSO throw
            // immediately, so the failure is impossible to miss either way. The call-count
            // assertion below is what actually proves classify never ran.
            AtomicInteger classifyCallCount = new AtomicInteger(0);
            parsingModule.setModelOverrideForTesting(() -> throwingModel(classifyCallCount));
            ipc.launchModule(parsingModule, new LaunchConfig(1, 1));

            AgentClient agent = AgentManager.create(ipc, "EmailParsingModule")
                                             .getAgentClient("email-parsing-agent");
            PState ingestionFailures = ipc.clusterPState(SCHEMA_MODULE_NAME, "$$ingestion-failures");

            String gmailMessageId = "persist-raw-fail-001";

            // (a) invoke throws — persist-raw's append fails because *raw-emails* was never
            // declared in this test's schema module.
            assertThrows(Exception.class, () -> agent.invoke(message(gmailMessageId)));

            Thread.sleep(2000);

            // (b) failure record landed, self-contained — persist-raw's guard copies `raw`
            // into the record since *raw-emails* never got this message.
            Map<String, Object> failure = readFailureRecord(ingestionFailures, gmailMessageId, "persist-raw");
            assertNotNull(failure, "Expected a persist-raw failure record in $$ingestion-failures");
            assertEquals(gmailMessageId, failure.get("gmailMessageId"));
            assertEquals(FAMILY_ID, failure.get("familyId"));
            assertEquals("Test email body for ingestion failure handling.", failure.get("body"),
                "persist-raw's failure record must be self-contained — *raw-emails* never got this message");
            assertEquals("Test subject", failure.get("emailSubject"));
            assertNotNull(failure.get("exceptionType"));
            assertNotNull(failure.get("exceptionMessage"));
            assertNotNull(failure.get("failedAt"));

            // (c) EXPLICIT, not assumed: classify — and therefore extract-details and
            // write-to-store, which only run after it — never executed for this message.
            // The agent graph does not naturally short-circuit on its own; this is what
            // actually verifies it.
            assertEquals(0, classifyCallCount.get(),
                "classify must never run when persist-raw's guard fails and rethrows");

            // (a, continued) worker/module still healthy — not crash-looped.
            assertClusterStillHealthy(ipc);
        }
    }

    // -----------------------------------------------------------------------
    // Test 2 — classify guard (Step 2), including the full multi-node chain
    // through EmailIngestionModule — the actual mechanism Step 0a asked about.
    //
    // NOTE on cluster-per-method design: an earlier version of this class tried
    // consolidating tests 2 and 3 into ONE shared InProcessCluster (switching
    // EmailParsingModule.modelOverrideForTesting mid-cluster-lifetime between the
    // classify and extract-details checks), specifically to reduce InProcessCluster
    // create/close churn — extra churn was suspected of aggravating the
    // already-documented "Executor pool is shut down" defect
    // (RAMA_VERIFIED_LEARNINGS.md) that hit EmailIngestionTest as collateral damage
    // in a full-suite run. That consolidation introduced a NEW, worse bug instead:
    // Agent-o-rama pools "gemini-model" agent objects (built once per pool slot,
    // reused across invocations — docs/Agent_O_Rama_Complete_Documentation.md,
    // "Thread Safety and Pooling"), so switching the override mid-cluster-lifetime
    // does not reliably affect an already-built pooled instance, causing the
    // extract-details check to intermittently fail even in isolation (no other test
    // class involved). Reverted to one cluster per method — confirmed reliable
    // across repeated isolated runs. Separately confirmed (full-suite reruns with
    // 3 clusters vs. the original 4): the EmailIngestionTest collateral damage
    // persists either way, so consolidating clusters bought no benefit for that
    // problem and wasn't worth the pooling-correctness risk. See this session's
    // report for the full picture — that collateral issue is unresolved.
    // -----------------------------------------------------------------------

    @Test
    void classifyFailurePropagatesThroughTheFullChainToEmailIngestionModule() throws Exception {
        try (InProcessCluster ipc = InProcessCluster.create()) {
            ipc.launchModule(new FamilySchemaModule(), new LaunchConfig(1, 1));

            EmailParsingModule parsingModule = new EmailParsingModule();
            AtomicInteger callCount = new AtomicInteger(0);
            parsingModule.setModelOverrideForTesting(() -> throwingModel(callCount));
            ipc.launchModule(parsingModule, new LaunchConfig(1, 1));
            ipc.launchModule(new EmailIngestionModule(), new LaunchConfig(1, 1));

            AgentClient parsingAgent = AgentManager.create(ipc, "EmailParsingModule")
                                                    .getAgentClient("email-parsing-agent");
            AgentClient ingestionAgent = AgentManager.create(ipc, "EmailIngestionModule")
                                                      .getAgentClient("email-ingestion-agent");
            PState ingestionFailures = ipc.clusterPState(SCHEMA_MODULE_NAME, "$$ingestion-failures");

            // Single-hop check first: direct invocation of email-parsing-agent.
            String directId = "classify-fail-direct-001";
            assertThrows(Exception.class, () -> parsingAgent.invoke(message(directId)));
            Thread.sleep(2000);
            Map<String, Object> directFailure = readFailureRecord(ingestionFailures, directId, "classify");
            assertNotNull(directFailure, "Expected a classify failure record (direct single-hop invoke)");
            assertEquals(directId, directFailure.get("gmailMessageId"));
            assertClusterStillHealthy(ipc);

            // Full-chain check — this is the actual mechanism Step 0a was trying to confirm:
            // does the exception survive classify -> (Agent-o-rama's own retry/propagation
            // machinery) -> the mirror-agent invoke inside EmailIngestionModule's "ingest"
            // node -> its EXISTING f.get() catch (Exception ex) { failed++; }, or does it only
            // work from a single node in isolation?
            String chainId = "classify-fail-chain-001";
            List<GmailMessage> batch = new ArrayList<>();
            batch.add(message(chainId));

            EmailIngestionModule.IngestionResult result =
                (EmailIngestionModule.IngestionResult) ingestionAgent.invoke(batch);

            assertNotNull(result);
            assertEquals(0, result.eventIds.size(), "no event should have been parsed");
            assertEquals(0, result.skipped);
            assertEquals(1, result.failed,
                "EmailIngestionModule's existing f.get() catch (Exception ex) { failed++; } must "
                + "count this — confirms the exception propagates cleanly through the full "
                + "multi-node chain, not just from classify in isolation");

            Thread.sleep(2000);
            Map<String, Object> chainFailure = readFailureRecord(ingestionFailures, chainId, "classify");
            assertNotNull(chainFailure, "Expected a classify failure record for the chain-routed message too");
            assertEquals(chainId, chainFailure.get("gmailMessageId"));

            assertClusterStillHealthy(ipc);
        }
    }

    // -----------------------------------------------------------------------
    // Test 3 — extract-details guard (Step 2)
    // -----------------------------------------------------------------------

    @Test
    void extractDetailsFailureIsGuardedAndRecorded() throws Exception {
        try (InProcessCluster ipc = InProcessCluster.create()) {
            ipc.launchModule(new FamilySchemaModule(), new LaunchConfig(1, 1));

            EmailParsingModule parsingModule = new EmailParsingModule();
            parsingModule.setModelOverrideForTesting(IngestionFailureHandlingTest::classifyOkExtractThrows);
            ipc.launchModule(parsingModule, new LaunchConfig(1, 1));

            AgentClient agent = AgentManager.create(ipc, "EmailParsingModule")
                                             .getAgentClient("email-parsing-agent");
            PState ingestionFailures = ipc.clusterPState(SCHEMA_MODULE_NAME, "$$ingestion-failures");

            String gmailMessageId = "extract-fail-001";
            assertThrows(Exception.class, () -> agent.invoke(message(gmailMessageId)));

            Thread.sleep(2000);

            Map<String, Object> failure = readFailureRecord(ingestionFailures, gmailMessageId, "extract-details");
            assertNotNull(failure, "Expected an extract-details failure record");
            assertEquals(gmailMessageId, failure.get("gmailMessageId"));
            assertEquals(FAMILY_ID, failure.get("familyId"));
            assertNotNull(failure.get("exceptionType"));
            assertNotNull(failure.get("exceptionMessage"));
            assertNotNull(failure.get("failedAt"));
            // Distinct failedNode from classify's — confirms the two guards are independently
            // distinguishable, not conflated into one generic "LLM failed" record.
            assertEquals("extract-details", failure.get("failedNode"));

            assertClusterStillHealthy(ipc);
        }
    }

    // -----------------------------------------------------------------------
    // Test 4 — write-to-store guard (Step 2)
    // -----------------------------------------------------------------------

    @Test
    void writeToStoreFailureIsGuardedAndRecorded() throws Exception {
        try (InProcessCluster ipc = InProcessCluster.create()) {
            ipc.launchModule(new SchemaWithoutFamilyEvents(), new LaunchConfig(1, 1));

            EmailParsingModule parsingModule = new EmailParsingModule();
            parsingModule.setModelOverrideForTesting(IngestionFailureHandlingTest::alwaysValid);
            ipc.launchModule(parsingModule, new LaunchConfig(1, 1));

            AgentClient agent = AgentManager.create(ipc, "EmailParsingModule")
                                             .getAgentClient("email-parsing-agent");
            PState ingestionFailures = ipc.clusterPState(SCHEMA_MODULE_NAME, "$$ingestion-failures");

            String gmailMessageId = "write-to-store-fail-001";
            assertThrows(Exception.class, () -> agent.invoke(message(gmailMessageId)));

            Thread.sleep(2000);

            Map<String, Object> failure = readFailureRecord(ingestionFailures, gmailMessageId, "write-to-store");
            assertNotNull(failure, "Expected a write-to-store failure record");
            assertEquals(gmailMessageId, failure.get("gmailMessageId"));
            assertEquals(FAMILY_ID, failure.get("familyId"));
            assertNotNull(failure.get("exceptionType"));
            assertNotNull(failure.get("exceptionMessage"));
            assertNotNull(failure.get("failedAt"));

            assertClusterStillHealthy(ipc);
        }
    }
}
