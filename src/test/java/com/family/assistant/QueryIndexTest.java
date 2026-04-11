package com.family.assistant;

import com.family.assistant.schema.FamilySchemaModule;
import com.rpl.rama.Depot;
import com.rpl.rama.Path;
import com.rpl.rama.PState;
import com.rpl.rama.test.InProcessCluster;
import com.rpl.rama.test.LaunchConfig;
import org.junit.jupiter.api.*;

import java.time.Instant;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * QueryIndexTest
 *
 * Verifies that FamilySchemaModule correctly populates $$events-by-child,
 * $$events-by-category, and $$family-data from *family-events depot records.
 *
 * No GEMINI_API_KEY required — all tests use direct PState reads.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class QueryIndexTest {

    private static final String FAMILY_ID = "qfam";

    // Epoch millis for test events
    private static final long MAR_20 = Instant.parse("2026-03-20T08:30:00Z").toEpochMilli();
    private static final long MAR_25 = Instant.parse("2026-03-25T12:00:00Z").toEpochMilli();
    private static final long MAR_18 = Instant.parse("2026-03-18T14:00:00Z").toEpochMilli();
    private static final long MAR_15 = Instant.parse("2026-03-15T09:00:00Z").toEpochMilli();
    private static final long APR_01 = Instant.parse("2026-04-01T10:00:00Z").toEpochMilli();

    private InProcessCluster ipc;
    private Depot familyEventsDepot;
    private PState familyData;
    private PState eventsByChild;
    private PState eventsByCategory;

    @BeforeAll
    void setup() throws Exception {
        ipc = InProcessCluster.create();

        // Launch schema module — owns the depot, PStates, and indexes
        FamilySchemaModule schemaModule = new FamilySchemaModule();
        ipc.launchModule(schemaModule, new LaunchConfig(1, 1));

        familyEventsDepot = ipc.clusterDepot(schemaModule.getModuleName(), "*family-events");
        familyData        = ipc.clusterPState(schemaModule.getModuleName(), "$$family-data");
        eventsByChild     = ipc.clusterPState(schemaModule.getModuleName(), "$$events-by-child");
        eventsByCategory  = ipc.clusterPState(schemaModule.getModuleName(), "$$events-by-category");

        // Seed 5 test events directly into the depot (no LLM involved)
        // evt-1: Billy, SCHOOL_EVENT, MAR_20
        appendEvent("evt-1", FAMILY_ID, "Billy's Zoo Field Trip", "SCHOOL_EVENT",
                MAR_20, null, "Billy");

        // evt-2: Billy, PERMISSION_SLIP, MAR_25
        appendEvent("evt-2", FAMILY_ID, "Billy's Permission Slip Due", "PERMISSION_SLIP",
                MAR_25, null, "Billy");

        // evt-3: Emma, SCHOOL_EVENT, MAR_18
        appendEvent("evt-3", FAMILY_ID, "Emma's Science Fair", "SCHOOL_EVENT",
                MAR_18, null, "Emma");

        // evt-4: no child, DEADLINE, MAR_15
        appendEvent("evt-4", FAMILY_ID, "Read-a-thon Pledge Deadline", "DEADLINE",
                MAR_15, null, null);

        // evt-5: Emma, PERMISSION_SLIP, APR_01
        appendEvent("evt-5", FAMILY_ID, "Emma's Art Show Permission Slip", "PERMISSION_SLIP",
                APR_01, null, "Emma");

        // Wait for stream topology to process all depot records into PStates
        Thread.sleep(2500);
    }

    @AfterAll
    void teardown() throws Exception {
        if (ipc != null) ipc.close();
    }

    private void appendEvent(String id, String familyId, String title,
                              String eventType, Long startTime, Long deadline,
                              String childName) {
        Map<String, Object> event = new HashMap<>();
        event.put("id", id);
        event.put("familyId", familyId);
        event.put("title", title);
        event.put("eventType", eventType);
        event.put("startTime", startTime);
        event.put("deadline", deadline);
        event.put("childName", childName);
        event.put("childId", childName != null ? childName.toLowerCase() : null);
        event.put("status", "pending");
        event.put("description", "Test event " + id);
        event.put("sourceType", "test");
        event.put("created", System.currentTimeMillis());
        event.put("updated", System.currentTimeMillis());
        familyEventsDepot.append(event);
    }

    // =======================================================================
    // $$events-by-child index assertions
    // =======================================================================

    @Test
    @Order(1)
    void testChildIndexPopulated_Billy() {
        Set<String> billyEvents = (Set<String>)
                eventsByChild.selectOne(Path.key(FAMILY_ID).key("Billy"));

        assertNotNull(billyEvents, "Billy's event set should exist in $$events-by-child");
        assertTrue(billyEvents.contains("evt-1"),
                "Billy's set should contain evt-1 (Zoo Field Trip)");
        assertTrue(billyEvents.contains("evt-2"),
                "Billy's set should contain evt-2 (Permission Slip Due)");
        assertEquals(2, billyEvents.size(),
                "Billy should have exactly 2 events");
    }

    @Test
    @Order(2)
    void testChildIndexPopulated_Emma() {
        Set<String> emmaEvents = (Set<String>)
                eventsByChild.selectOne(Path.key(FAMILY_ID).key("Emma"));

        assertNotNull(emmaEvents, "Emma's event set should exist in $$events-by-child");
        assertTrue(emmaEvents.contains("evt-3"),
                "Emma's set should contain evt-3 (Science Fair)");
        assertTrue(emmaEvents.contains("evt-5"),
                "Emma's set should contain evt-5 (Art Show Permission Slip)");
        assertEquals(2, emmaEvents.size(),
                "Emma should have exactly 2 events");
    }

    @Test
    @Order(3)
    void testChildIndex_NullChildNotIndexed() {
        // evt-4 has childName=null — should not appear under any child key
        // We verify it is not present under the null key
        Object nullEntry = eventsByChild.selectOne(Path.key(FAMILY_ID).key(null));
        if (nullEntry instanceof Set) {
            assertFalse(((Set<?>) nullEntry).contains("evt-4"),
                    "evt-4 (null childName) must not appear in $$events-by-child");
        }
        // If nullEntry is null, the null key was never written — that's correct
    }

    // =======================================================================
    // $$events-by-category index assertions
    // =======================================================================

    @Test
    @Order(4)
    void testCategoryIndex_SchoolEvent() {
        Set<String> schoolEvents = (Set<String>)
                eventsByCategory.selectOne(Path.key(FAMILY_ID).key("SCHOOL_EVENT"));

        assertNotNull(schoolEvents, "SCHOOL_EVENT set should exist in $$events-by-category");
        assertTrue(schoolEvents.contains("evt-1"),
                "SCHOOL_EVENT set should contain evt-1 (Billy Zoo Field Trip)");
        assertTrue(schoolEvents.contains("evt-3"),
                "SCHOOL_EVENT set should contain evt-3 (Emma Science Fair)");
        assertEquals(2, schoolEvents.size(),
                "SCHOOL_EVENT should have exactly 2 events");
    }

    @Test
    @Order(5)
    void testCategoryIndex_PermissionSlip() {
        Set<String> permSlipEvents = (Set<String>)
                eventsByCategory.selectOne(Path.key(FAMILY_ID).key("PERMISSION_SLIP"));

        assertNotNull(permSlipEvents, "PERMISSION_SLIP set should exist in $$events-by-category");
        assertTrue(permSlipEvents.contains("evt-2"),
                "PERMISSION_SLIP set should contain evt-2 (Billy Permission Slip)");
        assertTrue(permSlipEvents.contains("evt-5"),
                "PERMISSION_SLIP set should contain evt-5 (Emma Art Show Permission Slip)");
        assertEquals(2, permSlipEvents.size(),
                "PERMISSION_SLIP should have exactly 2 events");
    }

    // =======================================================================
    // $$family-data full-scan assertions (no LLM, no QueryModule agent)
    // =======================================================================

    @Test
    @Order(6)
    void testFamilyDataContainsAllSeededEvents() {
        Map<String, Object> events = (Map<String, Object>)
                familyData.selectOne(Path.key(FAMILY_ID).key("events"));

        assertNotNull(events, "Events map should exist for test family");
        assertEquals(5, events.size(), "Should have 5 seeded events");
        assertTrue(events.containsKey("evt-1"), "Should contain evt-1");
        assertTrue(events.containsKey("evt-5"), "Should contain evt-5");
    }

    @Test
    @Order(7)
    void testUnknownFamilyReturnsNull() {
        Object result = familyData.selectOne(
                Path.key("unknown-family-xyz").key("events"));

        assertNull(result, "Unknown family should return null from PState");
    }
}
