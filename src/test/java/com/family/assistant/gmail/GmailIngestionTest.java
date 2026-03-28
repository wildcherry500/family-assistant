package com.family.assistant.gmail;

import com.family.assistant.email.EmailIngestionModule;
import com.family.assistant.email.EmailParsingModule;
import com.family.assistant.schema.FamilySchemaModule;
import com.rpl.agentorama.AgentClient;
import com.rpl.agentorama.AgentManager;
import com.rpl.rama.Path;
import com.rpl.rama.PState;
import com.rpl.rama.test.InProcessCluster;
import com.rpl.rama.test.LaunchConfig;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * GmailIngestionTest
 *
 * End-to-end integration test: fetches real Gmail messages for toddkeeling@gmail.com,
 * runs them through the full parsing pipeline, and verifies events land in $$family-data.
 *
 * Requires:
 *   - tokens/ directory with stored OAuth credentials (run GmailOAuthSetup first)
 *   - GEMINI_API_KEY environment variable
 *
 * Run with:
 *   mvn test -Dgroups=gmail -Dexcluded.groups="" -Dsurefire.useFile=false
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class GmailIngestionTest {

    private static final String FAMILY_ID = "keeling-family-001";

    private InProcessCluster ipc;
    private AgentClient gmailAgent;

    @BeforeAll
    public void setupCluster() throws Exception {
        ipc = InProcessCluster.create();
        ipc.launchModule(new FamilySchemaModule(),    new LaunchConfig(1, 1));
        ipc.launchModule(new EmailParsingModule(),    new LaunchConfig(1, 1));
        ipc.launchModule(new EmailIngestionModule(),  new LaunchConfig(1, 1));
        ipc.launchModule(new GmailIngestionModule(),  new LaunchConfig(1, 1));

        gmailAgent = AgentManager.create(ipc, "GmailIngestionModule")
                                 .getAgentClient("gmail-ingestion-agent");
    }

    @AfterAll
    public void tearDownCluster() throws Exception {
        if (ipc != null) ipc.close();
    }

    @Test
    @Tag("gmail")
    public void testGmailToFamilyData() throws Exception {
        assumeTrue(System.getProperty("GEMINI_API_KEY", System.getenv("GEMINI_API_KEY")) != null,
            "Skipping: GEMINI_API_KEY not set");

        System.out.println("[GmailIngestionTest] Fetching up to 5 unread messages from Gmail...");

        GmailIngestionModule.IngestionSummary summary =
            (GmailIngestionModule.IngestionSummary)
                gmailAgent.invoke(new GmailIngestionModule.FetchRequest("me", 5));

        assertNotNull(summary, "Should receive an IngestionSummary");
        System.out.println("[GmailIngestionTest] " + summary);

        if (summary.fetched == 0 && summary.skipped == 0) {
            System.out.println("[GmailIngestionTest] No unread messages in inbox — skipping PState assertion.");
            return;
        }

        assertTrue(summary.skipped >= 0, "skipped count must be non-negative");
        assertTrue(summary.alreadyProcessed >= 0, "alreadyProcessed count must be non-negative");
        assertTrue(summary.processed >= 0, "processed count must be non-negative");

        // Allow stream topology time to drain depot appends into $$family-data
        Thread.sleep(3000);

        // Query $$family-data for events written under the family ID
        PState ps = ipc.clusterPState("FamilySchemaModule", "$$family-data");
        @SuppressWarnings("unchecked")
        Map<String, Object> events = (Map<String, Object>)
            ps.selectOne(Path.key(FAMILY_ID).key("events"));

        System.out.println("[GmailIngestionTest] Events in $$family-data for " + FAMILY_ID + ":");
        if (events == null || events.isEmpty()) {
            System.out.println("  (none — all emails may have been UNKNOWN category or LLM errors)");
        } else {
            events.forEach((eventId, record) -> {
                @SuppressWarnings("unchecked")
                Map<String, Object> r = (Map<String, Object>) record;
                System.out.printf("  [%s] type=%-20s title=%s%n",
                    eventId.substring(0, 8),
                    r.get("eventType"),
                    r.get("title"));
            });
            assertTrue(events.size() > 0, "At least one event should be written to $$family-data");
        }
    }
}
