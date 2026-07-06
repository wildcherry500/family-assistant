package com.family.assistant;

import com.family.assistant.schema.FamilySchemaModule;
import com.rpl.rama.Depot;
import com.rpl.rama.Path;
import com.rpl.rama.PState;
import com.rpl.rama.test.InProcessCluster;
import com.rpl.rama.test.LaunchConfig;
import org.junit.jupiter.api.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * SiloIntentIndexTest
 *
 * Verifies that FamilySchemaModule correctly populates the two new inverted
 * index PStates, $$events-by-silo and $$events-by-intent, when events carrying
 * silo/intent tags are appended to the *family-events depot — and that both
 * fields are persisted on the primary $$family-data record.
 *
 * silo:   VAULT, OFFICE, STUDIO, UNKNOWN
 * intent: ACTION_REQUIRED, DECISION_NEEDED, FYI, SCHEDULING, UNKNOWN
 *
 * UNKNOWN is deliberately indexed like any other value (not excluded) — an
 * UNKNOWN bucket is how a correction-loop query would find events that still
 * need classification.
 *
 * No GEMINI_API_KEY required — this test does NOT use any LLM; events are
 * appended directly with silo/intent already set, exercising only
 * FamilySchemaModule's indexing, not EmailParsingModule's classify stage.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class SiloIntentIndexTest {

    private static final String FAMILY_ID = "silo-intent-test-family";
    private static final int EXPECTED_EVENT_COUNT = 5;

    private InProcessCluster ipc;
    private Depot  familyEventsDepot;
    private PState familyData;
    private PState eventsBySilo;
    private PState eventsByIntent;

    @BeforeAll
    void setup() throws Exception {
        ipc = InProcessCluster.create();

        FamilySchemaModule schemaModule = new FamilySchemaModule();
        ipc.launchModule(schemaModule, new LaunchConfig(1, 1));

        familyEventsDepot = ipc.clusterDepot(schemaModule.getModuleName(), "*family-events");
        familyData        = ipc.clusterPState(schemaModule.getModuleName(), "$$family-data");
        eventsBySilo       = ipc.clusterPState(schemaModule.getModuleName(), "$$events-by-silo");
        eventsByIntent     = ipc.clusterPState(schemaModule.getModuleName(), "$$events-by-intent");

        appendEvent("evt-vault-action",       "VAULT",   "ACTION_REQUIRED");
        appendEvent("evt-office-decision",    "OFFICE",  "DECISION_NEEDED");
        appendEvent("evt-studio-fyi",         "STUDIO",  "FYI");
        appendEvent("evt-studio-scheduling",  "STUDIO",  "SCHEDULING");
        appendEvent("evt-unknown-unknown",    "UNKNOWN", "UNKNOWN");

        waitForAllEventsIndexed(10_000);
    }

    @AfterAll
    void teardown() throws Exception {
        if (ipc != null) ipc.close();
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private void appendEvent(String id, String silo, String intent) {
        Map<String, Object> event = new HashMap<>();
        event.put("id",         id);
        event.put("familyId",   FAMILY_ID);
        event.put("title",      "Test event " + id);
        List<String> tags = new ArrayList<>();
        tags.add("TASK");
        event.put("tags",       tags);
        event.put("silo",       silo);
        event.put("intent",     intent);
        event.put("startTime",  null);
        event.put("deadline",   null);
        event.put("personId",   new ArrayList<String>());
        event.put("status",     "pending");
        event.put("description", "Test event " + id);
        event.put("sourceType", "test");
        event.put("accountLabel", null);
        event.put("created",    System.currentTimeMillis());
        event.put("updated",    System.currentTimeMillis());
        familyEventsDepot.append(event);
    }

    // Condition-poll-with-timeout — NOT a fixed sleep, NOT waitForStreamProcessedCount
    // (that method does not exist; see RAMA_VERIFIED_LEARNINGS.md). Same pattern as
    // QueryAgentTest's wait for $$family-data to be populated.
    private void waitForAllEventsIndexed(long timeoutMs) throws InterruptedException {
        long deadline = System.currentTimeMillis() + timeoutMs;
        while (System.currentTimeMillis() < deadline) {
            Map<?, ?> events = (Map<?, ?>) familyData.selectOne(Path.key(FAMILY_ID).key("events"));
            if (events != null && events.size() >= EXPECTED_EVENT_COUNT) return;
            Thread.sleep(50);
        }
        fail("Timed out waiting for all " + EXPECTED_EVENT_COUNT + " events to be indexed");
    }

    // -----------------------------------------------------------------------
    // $$events-by-silo
    // -----------------------------------------------------------------------

    @Test
    @Order(1)
    void testSiloIndexPopulated_Vault() {
        Set<String> ids = (Set<String>) eventsBySilo.selectOne(Path.key(FAMILY_ID).key("VAULT"));
        assertNotNull(ids, "VAULT silo index should exist");
        assertEquals(1, ids.size());
        assertTrue(ids.contains("evt-vault-action"));
    }

    @Test
    @Order(2)
    void testSiloIndexPopulated_Studio_TwoEvents() {
        Set<String> ids = (Set<String>) eventsBySilo.selectOne(Path.key(FAMILY_ID).key("STUDIO"));
        assertNotNull(ids, "STUDIO silo index should exist");
        assertEquals(2, ids.size(), "STUDIO should have 2 events in index");
        assertTrue(ids.contains("evt-studio-fyi"));
        assertTrue(ids.contains("evt-studio-scheduling"));
    }

    @Test
    @Order(3)
    void testSiloIndexIncludesUnknown() {
        Set<String> ids = (Set<String>) eventsBySilo.selectOne(Path.key(FAMILY_ID).key("UNKNOWN"));
        assertNotNull(ids, "UNKNOWN silo bucket should exist, not be excluded");
        assertTrue(ids.contains("evt-unknown-unknown"),
            "UNKNOWN-silo event should be queryable via the UNKNOWN bucket (correction-loop feature)");
    }

    @Test
    @Order(4)
    void testSiloIndexesAreIsolated_NoOverlap() {
        Set<String> vault  = (Set<String>) eventsBySilo.selectOne(Path.key(FAMILY_ID).key("VAULT"));
        Set<String> studio = (Set<String>) eventsBySilo.selectOne(Path.key(FAMILY_ID).key("STUDIO"));
        assertNotNull(vault);
        assertNotNull(studio);
        for (String id : vault) {
            assertFalse(studio.contains(id), "Event " + id + " should not appear in STUDIO index");
        }
    }

    // -----------------------------------------------------------------------
    // $$events-by-intent
    // -----------------------------------------------------------------------

    @Test
    @Order(5)
    void testIntentIndexPopulated_ActionRequired() {
        Set<String> ids = (Set<String>) eventsByIntent.selectOne(Path.key(FAMILY_ID).key("ACTION_REQUIRED"));
        assertNotNull(ids, "ACTION_REQUIRED intent index should exist");
        assertTrue(ids.contains("evt-vault-action"));
    }

    @Test
    @Order(6)
    void testIntentIndexPopulated_DecisionNeeded() {
        Set<String> ids = (Set<String>) eventsByIntent.selectOne(Path.key(FAMILY_ID).key("DECISION_NEEDED"));
        assertNotNull(ids, "DECISION_NEEDED intent index should exist");
        assertTrue(ids.contains("evt-office-decision"));
    }

    @Test
    @Order(7)
    void testIntentIndexIncludesUnknown() {
        Set<String> ids = (Set<String>) eventsByIntent.selectOne(Path.key(FAMILY_ID).key("UNKNOWN"));
        assertNotNull(ids, "UNKNOWN intent bucket should exist, not be excluded");
        assertTrue(ids.contains("evt-unknown-unknown"),
            "UNKNOWN-intent event should be queryable via the UNKNOWN bucket (correction-loop feature)");
    }

    // -----------------------------------------------------------------------
    // $$family-data — both fields persisted on the record
    // -----------------------------------------------------------------------

    @Test
    @Order(8)
    void testSiloAndIntentPersistedOnEventRecord() {
        Map<String, Object> event = (Map<String, Object>) familyData.selectOne(
            Path.key(FAMILY_ID).key("events").key("evt-office-decision"));
        assertNotNull(event, "Event evt-office-decision should be in $$family-data");
        assertEquals("OFFICE", event.get("silo"),
            "silo field should be persisted on the event record");
        assertEquals("DECISION_NEEDED", event.get("intent"),
            "intent field should be persisted on the event record");
        assertTrue(((List<?>) event.get("tags")).contains("TASK"),
            "tags must remain intact (still contain TASK) after the silo/intent fields were added");
    }
}
