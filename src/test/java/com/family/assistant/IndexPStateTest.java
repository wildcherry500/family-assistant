package com.family.assistant;

import com.family.assistant.schema.FamilySchemaModule;
import com.rpl.rama.Depot;
import com.rpl.rama.Path;
import com.rpl.rama.PState;
import com.rpl.rama.test.InProcessCluster;
import com.rpl.rama.test.LaunchConfig;
import org.junit.jupiter.api.*;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * IndexPStateTest
 *
 * Verifies that FamilySchemaModule correctly populates the two inverted index
 * PStates ($$events-by-child and $$events-by-category) when events are appended
 * to the *family-events depot.
 *
 * No GEMINI_API_KEY required — this test does NOT use any LLM.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class IndexPStateTest {

    private static final String FAMILY_ID = "fam-test";
    private static final String MODULE_NAME = "FamilySchemaModule";

    private InProcessCluster ipc;
    private Depot familyEventsDepot;
    private PState eventsByChild;
    private PState eventsByCategory;

    @BeforeAll
    void setup() throws Exception {
        ipc = InProcessCluster.create();

        FamilySchemaModule schemaModule = new FamilySchemaModule();
        ipc.launchModule(schemaModule, new LaunchConfig(1, 1));

        familyEventsDepot = ipc.clusterDepot(MODULE_NAME, "*family-events");
        eventsByChild     = ipc.clusterPState(MODULE_NAME, "$$events-by-child");
        eventsByCategory  = ipc.clusterPState(MODULE_NAME, "$$events-by-category");

        // evt-A: Billy, SCHOOL_EVENT
        appendEvent("evt-A", FAMILY_ID, "Billy", "SCHOOL_EVENT");

        // evt-B: Billy, PERMISSION_SLIP
        appendEvent("evt-B", FAMILY_ID, "Billy", "PERMISSION_SLIP");

        // evt-C: Emma, SCHOOL_EVENT
        appendEvent("evt-C", FAMILY_ID, "Emma", "SCHOOL_EVENT");

        // evt-D: null childName, DEADLINE  — should NOT appear in child index
        appendEvent("evt-D", FAMILY_ID, null, "DEADLINE");

        // Wait for stream topology to process all records
        Thread.sleep(2000);
    }

    @AfterAll
    void teardown() throws Exception {
        if (ipc != null) ipc.close();
    }

    private void appendEvent(String id, String familyId, String childName, String eventType) {
        Map<String, Object> event = new HashMap<>();
        event.put("id", id);
        event.put("familyId", familyId);
        event.put("childName", childName);
        event.put("eventType", eventType);
        familyEventsDepot.append(event);
    }

    // =======================================================================
    // $$events-by-child
    // =======================================================================

    @Test
    @Order(1)
    void testChildIndexBillyContainsEvtA() {
        Set<String> billyEvents = (Set<String>)
            eventsByChild.selectOne(Path.key(FAMILY_ID).key("Billy"));
        assertNotNull(billyEvents, "Billy's event set should exist");
        assertTrue(billyEvents.contains("evt-A"),
            "Billy's set should contain evt-A");
    }

    @Test
    @Order(2)
    void testChildIndexBillyContainsEvtB() {
        Set<String> billyEvents = (Set<String>)
            eventsByChild.selectOne(Path.key(FAMILY_ID).key("Billy"));
        assertNotNull(billyEvents, "Billy's event set should exist");
        assertTrue(billyEvents.contains("evt-B"),
            "Billy's set should contain evt-B");
    }

    @Test
    @Order(3)
    void testChildIndexEmmaContainsEvtC() {
        Set<String> emmaEvents = (Set<String>)
            eventsByChild.selectOne(Path.key(FAMILY_ID).key("Emma"));
        assertNotNull(emmaEvents, "Emma's event set should exist");
        assertTrue(emmaEvents.contains("evt-C"),
            "Emma's set should contain evt-C");
    }

    @Test
    @Order(4)
    void testNullChildNameNotIndexed() {
        // evt-D has null childName — should not create any child entry for null
        // The null key should not exist in the child index for this family
        Object nullEntry = eventsByChild.selectOne(Path.key(FAMILY_ID).key(null));
        if (nullEntry instanceof Set) {
            Set<?> s = (Set<?>) nullEntry;
            assertFalse(s.contains("evt-D"),
                "evt-D (null childName) must not appear in child index");
        }
        // If nullEntry is null, that's fine — null key was never written
    }

    @Test
    @Order(5)
    void testBillyHasExactlyTwoEvents() {
        Set<String> billyEvents = (Set<String>)
            eventsByChild.selectOne(Path.key(FAMILY_ID).key("Billy"));
        assertNotNull(billyEvents);
        assertEquals(2, billyEvents.size(),
            "Billy should have exactly 2 events (evt-A, evt-B)");
    }

    @Test
    @Order(6)
    void testEmmaHasExactlyOneEvent() {
        Set<String> emmaEvents = (Set<String>)
            eventsByChild.selectOne(Path.key(FAMILY_ID).key("Emma"));
        assertNotNull(emmaEvents);
        assertEquals(1, emmaEvents.size(),
            "Emma should have exactly 1 event (evt-C)");
    }

    // =======================================================================
    // $$events-by-category
    // =======================================================================

    @Test
    @Order(10)
    void testCategoryIndexSchoolEventContainsEvtA() {
        Set<String> schoolEvents = (Set<String>)
            eventsByCategory.selectOne(Path.key(FAMILY_ID).key("SCHOOL_EVENT"));
        assertNotNull(schoolEvents, "SCHOOL_EVENT set should exist");
        assertTrue(schoolEvents.contains("evt-A"),
            "SCHOOL_EVENT set should contain evt-A");
    }

    @Test
    @Order(11)
    void testCategoryIndexSchoolEventContainsEvtC() {
        Set<String> schoolEvents = (Set<String>)
            eventsByCategory.selectOne(Path.key(FAMILY_ID).key("SCHOOL_EVENT"));
        assertNotNull(schoolEvents, "SCHOOL_EVENT set should exist");
        assertTrue(schoolEvents.contains("evt-C"),
            "SCHOOL_EVENT set should contain evt-C");
    }

    @Test
    @Order(12)
    void testCategoryIndexPermissionSlipContainsEvtB() {
        Set<String> permSlipEvents = (Set<String>)
            eventsByCategory.selectOne(Path.key(FAMILY_ID).key("PERMISSION_SLIP"));
        assertNotNull(permSlipEvents, "PERMISSION_SLIP set should exist");
        assertTrue(permSlipEvents.contains("evt-B"),
            "PERMISSION_SLIP set should contain evt-B");
    }

    @Test
    @Order(13)
    void testCategoryIndexDeadlineContainsEvtD() {
        Set<String> deadlineEvents = (Set<String>)
            eventsByCategory.selectOne(Path.key(FAMILY_ID).key("DEADLINE"));
        assertNotNull(deadlineEvents, "DEADLINE set should exist");
        assertTrue(deadlineEvents.contains("evt-D"),
            "DEADLINE set should contain evt-D");
    }

    @Test
    @Order(14)
    void testSchoolEventHasExactlyTwoEvents() {
        Set<String> schoolEvents = (Set<String>)
            eventsByCategory.selectOne(Path.key(FAMILY_ID).key("SCHOOL_EVENT"));
        assertNotNull(schoolEvents);
        assertEquals(2, schoolEvents.size(),
            "SCHOOL_EVENT should have exactly 2 events (evt-A, evt-C)");
    }

    @Test
    @Order(15)
    void testPermissionSlipHasExactlyOneEvent() {
        Set<String> permSlipEvents = (Set<String>)
            eventsByCategory.selectOne(Path.key(FAMILY_ID).key("PERMISSION_SLIP"));
        assertNotNull(permSlipEvents);
        assertEquals(1, permSlipEvents.size(),
            "PERMISSION_SLIP should have exactly 1 event (evt-B)");
    }

    @Test
    @Order(16)
    void testDeadlineHasExactlyOneEvent() {
        Set<String> deadlineEvents = (Set<String>)
            eventsByCategory.selectOne(Path.key(FAMILY_ID).key("DEADLINE"));
        assertNotNull(deadlineEvents);
        assertEquals(1, deadlineEvents.size(),
            "DEADLINE should have exactly 1 event (evt-D)");
    }
}
