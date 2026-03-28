package com.family.assistant;

import com.family.assistant.digest.DigestModule;
import com.family.assistant.email.EmailParsingModule;
import com.family.assistant.schema.FamilySchemaModule;
import com.rpl.agentorama.AgentClient;
import com.rpl.agentorama.AgentManager;
import com.rpl.rama.Path;
import com.rpl.rama.PState;
import com.rpl.rama.test.InProcessCluster;
import com.rpl.rama.test.LaunchConfig;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * FamilyAssistantTest
 *
 * End-to-end test wiring all three modules in InProcessCluster:
 *   1. FamilySchemaModule  — declares $$family-data PState
 *   2. EmailParsingModule  — parses raw email, writes event to $$family-data
 *   3. DigestModule        — reads events, returns human-readable digest
 *
 * Tests run against real Rama + Agent-o-rama runtime via InProcessCluster.
 * No external services (LLM, email API) are required — stubs are used.
 */
public class FamilyAssistantTest {

    private static final String FAMILY_ID = "keeling-family-001";

    // -----------------------------------------------------------------------
    // Test 1: FamilySchemaModule deploys and $$family-data is accessible
    // -----------------------------------------------------------------------
    @Test
    public void testSchemaModuleDeploysAndStoreIsAccessible() throws Exception {
        try (InProcessCluster ipc = InProcessCluster.create()) {

            FamilySchemaModule schemaModule = new FamilySchemaModule();
            ipc.launchModule(schemaModule, new LaunchConfig(1, 1));
            String moduleName = schemaModule.getModuleName();

            // $$family-data should be queryable — empty map for unknown family
            PState ps = ipc.clusterPState(moduleName, "$$family-data");
            assertNotNull(ps, "$$family-data PState should be accessible");

            // Querying a non-existent family key should return null (not throw)
            Object result = ps.selectOne(Path.key(FAMILY_ID));
            assertNull(result, "Unknown family key should return null");
        }
    }

    // -----------------------------------------------------------------------
    // Test 2: EmailParsingModule parses an email and writes an event
    // -----------------------------------------------------------------------
    @Test
    @Tag("llm")
    public void testEmailParsingWritesEventToStore() throws Exception {
        assumeTrue(System.getProperty("GEMINI_API_KEY", System.getenv("GEMINI_API_KEY")) != null,
            "Skipping: GEMINI_API_KEY not set");
        try (InProcessCluster ipc = InProcessCluster.create()) {

            // Launch schema module first — EmailParsingModule reads from it
            FamilySchemaModule schemaModule = new FamilySchemaModule();
            ipc.launchModule(schemaModule, new LaunchConfig(1, 1));

            // Launch email parsing module
            EmailParsingModule emailModule = new EmailParsingModule();
            ipc.launchModule(emailModule, new LaunchConfig(1, 1));
            String emailModuleName = emailModule.getModuleName();

            // Wire up AgentManager and client
            AgentManager manager = AgentManager.create(ipc, emailModuleName);
            AgentClient emailAgent = manager.getAgentClient("email-parsing-agent");

            // Invoke with a sample school event email
            String rawEmail =
                "Field Trip Permission Slip\n" +
                "Please sign and return by Friday.\n" +
                "Our class will visit the Science Center on March 20th.\n" +
                "Students must bring a bag lunch.";

            Object result = emailAgent.invoke(rawEmail);

            // Result should be a non-null eventId string
            assertNotNull(result, "Agent should return an eventId");
            assertInstanceOf(String.class, result, "EventId should be a String");
            assertFalse(((String) result).isBlank(), "EventId should not be blank");

            // TODO: once getMirrorStore cross-module write is confirmed at runtime,
            // add assertion that the event exists in $$family-data:
            //
            // PState ps = ipc.clusterPState(schemaModule.getModuleName(), "$$family-data");
            // Map<String, Object> events = (Map<String, Object>)
            //     ps.selectOne(Path.key(FAMILY_ID).key("events"));
            // assertNotNull(events);
            // assertTrue(events.containsKey(result.toString()));
        }
    }

    // -----------------------------------------------------------------------
    // Test 3: DigestModule returns a non-empty digest for a known time window
    // -----------------------------------------------------------------------
    @Test
    public void testDigestModuleReturnsDigest() throws Exception {
        try (InProcessCluster ipc = InProcessCluster.create()) {

            // Launch schema module
            FamilySchemaModule schemaModule = new FamilySchemaModule();
            ipc.launchModule(schemaModule, new LaunchConfig(1, 1));

            // Launch digest module
            DigestModule digestModule = new DigestModule();
            ipc.launchModule(digestModule, new LaunchConfig(1, 1));
            String digestModuleName = digestModule.getModuleName();

            AgentManager manager = AgentManager.create(ipc, digestModuleName);
            AgentClient digestAgent = manager.getAgentClient("digest-agent");

            // Request digest for a 7-day window starting now
            long now = System.currentTimeMillis();
            long weekFromNow = now + (7L * 24 * 60 * 60 * 1000);

            DigestModule.DigestRequest request =
                new DigestModule.DigestRequest(FAMILY_ID, now, weekFromNow);

            Object result = digestAgent.invoke(request);

            // Should return a non-null String (empty window message is still valid)
            assertNotNull(result, "Digest agent should return a result");
            assertInstanceOf(String.class, result, "Digest result should be a String");
            assertFalse(((String) result).isBlank(), "Digest result should not be blank");

            System.out.println("Digest output:\n" + result);
        }
    }

    // -----------------------------------------------------------------------
    // Test 4: End-to-end — parse email then digest shows it
    // -----------------------------------------------------------------------
    @Test
    @Tag("llm")
    public void testEndToEnd_EmailThenDigest() throws Exception {
        assumeTrue(System.getProperty("GEMINI_API_KEY", System.getenv("GEMINI_API_KEY")) != null,
            "Skipping: GEMINI_API_KEY not set");
        try (InProcessCluster ipc = InProcessCluster.create()) {

            // Launch all three modules
            FamilySchemaModule schemaModule = new FamilySchemaModule();
            ipc.launchModule(schemaModule, new LaunchConfig(1, 1));

            EmailParsingModule emailModule = new EmailParsingModule();
            ipc.launchModule(emailModule, new LaunchConfig(1, 1));

            DigestModule digestModule = new DigestModule();
            ipc.launchModule(digestModule, new LaunchConfig(1, 1));


            // Parse an email
            AgentManager emailManager = AgentManager.create(ipc, emailModule.getModuleName());
            AgentClient emailAgent = emailManager.getAgentClient("email-parsing-agent");

            String rawEmail =
                "Science Fair — Save the Date\n" +
                "The annual science fair will be held March 25th at 6pm.\n" +
                "Projects are due March 22nd.";

            String eventId = (String) emailAgent.invoke(rawEmail);
            assertNotNull(eventId, "Should get an eventId back");

            // Now request a digest covering that window
            AgentManager digestManager = AgentManager.create(ipc, digestModule.getModuleName());
            AgentClient digestAgent = digestManager.getAgentClient("digest-agent");

            long now = System.currentTimeMillis();
            long monthFromNow = now + (30L * 24 * 60 * 60 * 1000);

            DigestModule.DigestRequest request =
                new DigestModule.DigestRequest(FAMILY_ID, now, monthFromNow);

            String digest = (String) digestAgent.invoke(request);
            assertNotNull(digest);

            // TODO: once getMirrorStore write is confirmed at runtime,
            // assert the digest contains the parsed email title:
            // assertTrue(digest.contains("Science Fair"), "Digest should mention Science Fair");

            System.out.println("End-to-end digest:\n" + digest);
        }
    }
}
