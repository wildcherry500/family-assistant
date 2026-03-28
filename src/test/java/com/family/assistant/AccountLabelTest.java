package com.family.assistant;

import com.family.assistant.digest.DigestModule;
import com.family.assistant.schema.FamilySchemaModule;
import com.rpl.agentorama.AgentClient;
import com.rpl.agentorama.AgentManager;
import com.rpl.rama.Depot;
import com.rpl.rama.PState;
import com.rpl.rama.Path;
import com.rpl.rama.test.InProcessCluster;
import com.rpl.rama.test.LaunchConfig;
import org.junit.jupiter.api.*;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * AccountLabelTest — Phase 1 gate test
 *
 * Verifies that:
 *  1. Events seeded with different accountLabel values are stored correctly
 *     in $$family-data and the new $$events-by-account index.
 *  2. DigestRequest with an accountLabel filter returns only events from
 *     that account.
 *  3. DigestRequest with no accountLabel filter returns all events.
 *
 * No GEMINI_API_KEY required.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class AccountLabelTest {

    private static final String FAMILY_ID = "acct-label-test-family";
    private static final String ACCOUNT_A = "alice@test.com";
    private static final String ACCOUNT_B = "bob@test.com";

    // Four events spread across two accounts, all within the same digest window
    private static final long T1 = Instant.parse("2026-04-10T09:00:00Z").toEpochMilli();
    private static final long T2 = Instant.parse("2026-04-11T09:00:00Z").toEpochMilli();
    private static final long T3 = Instant.parse("2026-04-12T09:00:00Z").toEpochMilli();
    private static final long T4 = Instant.parse("2026-04-13T09:00:00Z").toEpochMilli();

    private static final long WIN_START = Instant.parse("2026-04-09T00:00:00Z").toEpochMilli();
    private static final long WIN_END   = Instant.parse("2026-04-14T23:59:59Z").toEpochMilli();

    private InProcessCluster ipc;
    private Depot  familyEventsDepot;
    private PState familyData;
    private PState eventsByAccount;
    private AgentClient digestAgent;

    @BeforeAll
    void setup() throws Exception {
        ipc = InProcessCluster.create();

        FamilySchemaModule schemaModule = new FamilySchemaModule();
        ipc.launchModule(schemaModule, new LaunchConfig(1, 1));

        DigestModule digestModule = new DigestModule();
        ipc.launchModule(digestModule, new LaunchConfig(1, 1));

        familyEventsDepot = ipc.clusterDepot(schemaModule.getModuleName(), "*family-events");
        familyData        = ipc.clusterPState(schemaModule.getModuleName(), "$$family-data");
        eventsByAccount   = ipc.clusterPState(schemaModule.getModuleName(), "$$events-by-account");

        AgentManager mgr = AgentManager.create(ipc, digestModule.getModuleName());
        digestAgent = mgr.getAgentClient("digest-agent");

        // Account A — 2 events
        appendEvent("acct-a-1", "Alice Event 1", T1, ACCOUNT_A);
        appendEvent("acct-a-2", "Alice Event 2", T2, ACCOUNT_A);
        // Account B — 2 events
        appendEvent("acct-b-1", "Bob Event 1",   T3, ACCOUNT_B);
        appendEvent("acct-b-2", "Bob Event 2",   T4, ACCOUNT_B);

        Thread.sleep(2500); // wait for stream topology to process all 4 records
    }

    @AfterAll
    void teardown() throws Exception {
        if (ipc != null) ipc.close();
    }

    // -----------------------------------------------------------------------
    // Helper
    // -----------------------------------------------------------------------

    private void appendEvent(String id, String title, long startTime, String accountLabel) {
        Map<String, Object> event = new HashMap<>();
        event.put("id",           id);
        event.put("familyId",     FAMILY_ID);
        event.put("title",        title);
        event.put("eventType",    "SCHOOL_EVENT");
        event.put("startTime",    startTime);
        event.put("deadline",     null);
        event.put("childName",    null);
        event.put("childId",      null);
        event.put("status",       "pending");
        event.put("description",  "Test event " + id);
        event.put("sourceType",   "test");
        event.put("accountLabel", accountLabel);
        event.put("created",      System.currentTimeMillis());
        event.put("updated",      System.currentTimeMillis());
        familyEventsDepot.append(event);
    }

    // -----------------------------------------------------------------------
    // Index tests
    // -----------------------------------------------------------------------

    @Test
    @Order(1)
    void testAccountIndexPopulated_AccountA() {
        Set<String> ids = (Set<String>) eventsByAccount.selectOne(
            Path.key(FAMILY_ID).key(ACCOUNT_A));
        assertNotNull(ids, "Index for account A should exist");
        assertEquals(2, ids.size(), "Account A should have 2 events in index");
        assertTrue(ids.contains("acct-a-1"));
        assertTrue(ids.contains("acct-a-2"));
    }

    @Test
    @Order(2)
    void testAccountIndexPopulated_AccountB() {
        Set<String> ids = (Set<String>) eventsByAccount.selectOne(
            Path.key(FAMILY_ID).key(ACCOUNT_B));
        assertNotNull(ids, "Index for account B should exist");
        assertEquals(2, ids.size(), "Account B should have 2 events in index");
        assertTrue(ids.contains("acct-b-1"));
        assertTrue(ids.contains("acct-b-2"));
    }

    @Test
    @Order(3)
    void testAccountLabelStoredOnEventRecord() {
        Map<String, Object> event = (Map<String, Object>) familyData.selectOne(
            Path.key(FAMILY_ID).key("events").key("acct-a-1"));
        assertNotNull(event, "Event acct-a-1 should be in $$family-data");
        assertEquals(ACCOUNT_A, event.get("accountLabel"),
            "accountLabel field should be persisted on the event record");
    }

    @Test
    @Order(4)
    void testAccountsAreIsolated_NoOverlap() {
        Set<String> idsA = (Set<String>) eventsByAccount.selectOne(
            Path.key(FAMILY_ID).key(ACCOUNT_A));
        Set<String> idsB = (Set<String>) eventsByAccount.selectOne(
            Path.key(FAMILY_ID).key(ACCOUNT_B));
        assertNotNull(idsA);
        assertNotNull(idsB);
        // No ID should appear in both sets
        for (String id : idsA) {
            assertFalse(idsB.contains(id), "Event " + id + " should not appear in account B index");
        }
    }

    // -----------------------------------------------------------------------
    // DigestModule filter tests
    // -----------------------------------------------------------------------

    @Test
    @Order(5)
    void testDigestFilterByAccountA_ReturnsOnlyAccountAEvents() {
        String digest = (String) digestAgent.invoke(
            new DigestModule.DigestRequest(FAMILY_ID, WIN_START, WIN_END, ACCOUNT_A));

        assertNotNull(digest);
        assertTrue(digest.contains("Alice Event 1"), "Account A digest must include Alice Event 1");
        assertTrue(digest.contains("Alice Event 2"), "Account A digest must include Alice Event 2");
        assertFalse(digest.contains("Bob Event"),    "Account A digest must not include Bob events");
    }

    @Test
    @Order(6)
    void testDigestFilterByAccountB_ReturnsOnlyAccountBEvents() {
        String digest = (String) digestAgent.invoke(
            new DigestModule.DigestRequest(FAMILY_ID, WIN_START, WIN_END, ACCOUNT_B));

        assertNotNull(digest);
        assertTrue(digest.contains("Bob Event 1"),  "Account B digest must include Bob Event 1");
        assertTrue(digest.contains("Bob Event 2"),  "Account B digest must include Bob Event 2");
        assertFalse(digest.contains("Alice Event"), "Account B digest must not include Alice events");
    }

    @Test
    @Order(7)
    void testDigestNoFilter_ReturnsAllFourEvents() {
        String digest = (String) digestAgent.invoke(
            new DigestModule.DigestRequest(FAMILY_ID, WIN_START, WIN_END));

        assertNotNull(digest);
        assertTrue(digest.contains("Alice Event 1"), "No-filter digest must include all 4 events");
        assertTrue(digest.contains("Alice Event 2"));
        assertTrue(digest.contains("Bob Event 1"));
        assertTrue(digest.contains("Bob Event 2"));
    }

    @Test
    @Order(8)
    void testDigestUnknownAccount_ReturnsEmpty() {
        String digest = (String) digestAgent.invoke(
            new DigestModule.DigestRequest(FAMILY_ID, WIN_START, WIN_END, "nobody@test.com"));

        assertNotNull(digest);
        // No events for this account — digest should report empty window gracefully
        assertFalse(digest.contains("Alice Event"), "Unknown account should yield no Alice events");
        assertFalse(digest.contains("Bob Event"),   "Unknown account should yield no Bob events");
    }
}