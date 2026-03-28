package com.family.assistant;

import com.family.assistant.email.EmailIngestionModule;
import com.family.assistant.email.EmailParsingModule;
import com.family.assistant.gmail.GmailMessage;
import com.family.assistant.schema.FamilySchemaModule;
import com.rpl.agentorama.AgentClient;
import com.rpl.agentorama.AgentManager;
import com.rpl.rama.test.InProcessCluster;
import com.rpl.rama.test.LaunchConfig;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * EmailIngestionTest
 *
 * All tests share one InProcessCluster to avoid JVM-level executor pool
 * conflicts between sequential cluster creates.
 *
 * Tests that don't send emails through (empty list, blank filtering) run
 * without a Gemini key. Tests that invoke email-parsing-agent require
 * GEMINI_API_KEY to be set and are skipped otherwise.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class EmailIngestionTest {

    private InProcessCluster ipc;
    private AgentClient agent;

    @BeforeAll
    public void setupCluster() throws Exception {
        ipc = InProcessCluster.create();
        ipc.launchModule(new FamilySchemaModule(),   new LaunchConfig(1, 1));
        ipc.launchModule(new EmailParsingModule(),   new LaunchConfig(1, 1));
        ipc.launchModule(new EmailIngestionModule(), new LaunchConfig(1, 1));
        agent = AgentManager.create(ipc, "EmailIngestionModule")
                            .getAgentClient("email-ingestion-agent");
    }

    @AfterAll
    public void tearDownCluster() throws Exception {
        if (ipc != null) ipc.close();
    }

    @BeforeEach
    public void pauseBetweenTests() throws Exception {
        // Avoid Gemini free-tier rate limits between LLM test invocations
        Thread.sleep(3000);
    }

    // -----------------------------------------------------------------------
    // Test 1: Empty list → empty result, no LLM needed
    // -----------------------------------------------------------------------
    @Test
    public void testEmptyListReturnsEmptyResult() throws Exception {
        EmailIngestionModule.IngestionResult result =
            (EmailIngestionModule.IngestionResult) agent.invoke(new ArrayList<GmailMessage>());

        assertNotNull(result);
        assertTrue(result.eventIds.isEmpty(), "No events from empty input");
        assertEquals(0, result.skipped);
        assertEquals(0, result.failed);
    }

    // -----------------------------------------------------------------------
    // Test 2: All blank/null → all skipped, no LLM needed
    // -----------------------------------------------------------------------
    @Test
    public void testBlankAndNullEmailsAreSkipped() throws Exception {
        // EmailIngestionModule.ingest expects List<GmailMessage>; blank/null bodies are skipped
        List<GmailMessage> inputs = new ArrayList<>(Arrays.asList(
            new GmailMessage("",   "id1", "test@test.com", "Test", "Subject", 0L, null),
            new GmailMessage("   ","id2", "test@test.com", "Test", "Subject", 0L, null),
            new GmailMessage("\n", "id3", "test@test.com", "Test", "Subject", 0L, null),
            null,
            new GmailMessage("\t", "id5", "test@test.com", "Test", "Subject", 0L, null)
        ));

        EmailIngestionModule.IngestionResult result =
            (EmailIngestionModule.IngestionResult) agent.invoke(inputs);

        assertNotNull(result);
        assertTrue(result.eventIds.isEmpty(), "No events from blank input");
        assertEquals(5, result.skipped, "All 5 entries should be skipped");
        assertEquals(0, result.failed);
    }

    // -----------------------------------------------------------------------
    // Test 3: Mixed blank + valid — only valid emails are parsed
    //         Requires GEMINI_API_KEY
    // -----------------------------------------------------------------------
    @Test
    @Tag("llm")
    public void testMixedBatchSkipsBlanksAndParsesValid() throws Exception {
        assumeTrue(System.getProperty("GEMINI_API_KEY", System.getenv("GEMINI_API_KEY")) != null,
            "Skipping: GEMINI_API_KEY not set");

        List<String> inputs = new ArrayList<>(Arrays.asList(
            "",
            "Field Trip to the Zoo\nPlease return permission slip by Friday.\nBring $5.",
            "   ",
            "Reminder: Science project due Monday."
        ));

        EmailIngestionModule.IngestionResult result =
            (EmailIngestionModule.IngestionResult) agent.invoke(inputs);

        assertNotNull(result);
        assertEquals(2, result.skipped, "Two blank entries should be skipped");
        assertEquals(2, result.eventIds.size(), "Two valid emails should produce event IDs");
        assertEquals(0, result.failed);
        result.eventIds.forEach(id -> assertFalse(id.isBlank(), "Event ID should not be blank"));
        System.out.println(result);
    }

    // -----------------------------------------------------------------------
    // Test 4: Duplicate emails — each gets its own event ID
    //         Requires GEMINI_API_KEY
    // -----------------------------------------------------------------------
    @Test
    @Tag("llm")
    public void testDuplicateEmailsProduceSeparateEvents() throws Exception {
        assumeTrue(System.getProperty("GEMINI_API_KEY", System.getenv("GEMINI_API_KEY")) != null,
            "Skipping: GEMINI_API_KEY not set");

        String email = "Book Fair next Thursday in the school gym.";
        List<String> inputs = new ArrayList<>(Arrays.asList(email, email));

        EmailIngestionModule.IngestionResult result =
            (EmailIngestionModule.IngestionResult) agent.invoke(inputs);

        assertEquals(2, result.eventIds.size(), "Duplicate emails produce 2 separate events");
        assertNotEquals(result.eventIds.get(0), result.eventIds.get(1),
            "Event IDs should be distinct UUIDs");
        System.out.println(result);
    }

    // -----------------------------------------------------------------------
    // Test 5: Malformed / edge-case email content — batch must not crash
    //         Requires GEMINI_API_KEY
    // -----------------------------------------------------------------------
    @Test
    @Tag("llm")
    public void testMalformedEmailsDoNotCrashBatch() throws Exception {
        assumeTrue(System.getProperty("GEMINI_API_KEY", System.getenv("GEMINI_API_KEY")) != null,
            "Skipping: GEMINI_API_KEY not set");

        List<String> inputs = new ArrayList<>(Arrays.asList(
            "Normal school event email. Bake sale on April 5th.",
            "!@#$%^&*()_+",                              // garbage
            "a",                                          // single char
            "<html><body>HTML email</body></html>",       // HTML
            "Subject: \n\n",                             // headers only, empty body
            "Another normal email. Deadline: turn in forms by March 31."
        ));

        EmailIngestionModule.IngestionResult result =
            (EmailIngestionModule.IngestionResult) agent.invoke(inputs);

        assertNotNull(result);
        assertEquals(0, result.skipped, "None are blank");
        assertEquals(6, result.eventIds.size() + result.failed,
            "All 6 inputs must be accounted for");
        System.out.println("Malformed batch: " + result);
    }
}
