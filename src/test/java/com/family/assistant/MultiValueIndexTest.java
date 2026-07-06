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
 * MultiValueIndexTest
 *
 * Exercises the multi-valued inverted indexes introduced by the eventType->tags /
 * childName->personId refactor. The existing IndexPStateTest only ever puts ONE
 * element in each list, so it never drives the Ops.EXPLODE-over-a-list path. This
 * test appends records carrying SEVERAL tags and SEVERAL persons and verifies:
 *
 *   1. Fan-out completeness — every element of a list becomes its own index key
 *      pointing back at the event (a single-element test cannot catch an EXPLODE
 *      that only wrote the first element).
 *   2. Branch isolation — the tags fan-out reads the "tags" field and the personId
 *      fan-out reads the "personId" field; no value bleeds from one index into the
 *      other (catches a mis-wired .select key).
 *   3. Branch coexistence — tags, personId, and the pre-existing keyword fan-out all
 *      fire for the same record. This is the observable guarantee that the
 *      anchor/hook composition (each fan-out branching independently off the same
 *      input) leaves all three branches working.
 *
 * NOTE on what this test does NOT prove: index values are Sets, so a fork that
 * cartesian-nested the branches would produce REDUNDANT writes, not wrong
 * membership — set dedup hides it. anchor/hook's job is to prevent that write
 * amplification (an N-tag x M-person x K-token record would otherwise do N*M*K
 * keyword writes instead of K); membership assertions cannot distinguish the two.
 *
 * No GEMINI_API_KEY required — no LLM is used.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class MultiValueIndexTest {

    private static final String FAMILY_ID = "fam-multi";
    private static final String MODULE_NAME = "FamilySchemaModule";

    private InProcessCluster ipc;
    private Depot familyEventsDepot;
    private PState familyData;
    private PState eventsByTag;
    private PState eventsByPerson;
    private PState eventsByKeyword;

    @BeforeAll
    void setup() throws Exception {
        ipc = InProcessCluster.create();

        FamilySchemaModule schemaModule = new FamilySchemaModule();
        ipc.launchModule(schemaModule, new LaunchConfig(1, 1));

        familyEventsDepot = ipc.clusterDepot(MODULE_NAME, "*family-events");
        familyData      = ipc.clusterPState(MODULE_NAME, "$$family-data");
        eventsByTag     = ipc.clusterPState(MODULE_NAME, "$$events-by-tag");
        eventsByPerson  = ipc.clusterPState(MODULE_NAME, "$$events-by-person");
        eventsByKeyword = ipc.clusterPState(MODULE_NAME, "$$events-by-keyword");

        // evt-M1: two tags, two persons, a distinctive title for the keyword branch.
        appendEvent("evt-M1", FAMILY_ID,
            List.of("PERMISSION_SLIP", "DEADLINE"),
            List.of("Emma", "Dad"),
            "fieldtrip permission");

        // evt-M2: one tag, one person (Emma again — shared across events).
        appendEvent("evt-M2", FAMILY_ID,
            List.of("TASK"),
            List.of("Emma"),
            "weekly chores");

        // evt-M3: carries the plumbed-but-unpopulated classifier-output fields exactly as
        // the parsing agent writes them this session — null/empty defaults. Verifies they
        // round-trip into $$family-data (i.e. Rama serialization keeps a null map value).
        // Distinct tag/person values so this record cannot perturb the fan-out
        // membership/count assertions on evt-M1 and evt-M2 above.
        Map<String, Object> m3 = new HashMap<>();
        m3.put("id", "evt-M3");
        m3.put("familyId", FAMILY_ID);
        m3.put("tags", new ArrayList<>(List.of("ARCHIVE")));
        m3.put("personId", new ArrayList<>(List.of("Zoe")));
        m3.put("documentType", (String) null);
        m3.put("relatedEventIds", new ArrayList<String>());
        m3.put("confidence", (Double) null);
        m3.put("reason", (String) null);
        familyEventsDepot.append(m3);

        // Let the stream topology drain all records into every index.
        Thread.sleep(2000);
    }

    @AfterAll
    void teardown() throws Exception {
        if (ipc != null) ipc.close();
    }

    /** Appends a record with mutable list copies (never List.of into the depot). */
    private void appendEvent(String id, String familyId,
                             List<String> tags, List<String> persons, String title) {
        Map<String, Object> event = new HashMap<>();
        event.put("id", id);
        event.put("familyId", familyId);
        event.put("tags", new ArrayList<>(tags));
        event.put("personId", new ArrayList<>(persons));
        event.put("title", title);
        event.put("description", "");
        event.put("emailSubject", "");
        familyEventsDepot.append(event);
    }

    @SuppressWarnings("unchecked")
    private Set<String> tagSet(String tag) {
        return (Set<String>) eventsByTag.selectOne(Path.key(FAMILY_ID).key(tag));
    }

    @SuppressWarnings("unchecked")
    private Set<String> personSet(String person) {
        return (Set<String>) eventsByPerson.selectOne(Path.key(FAMILY_ID).key(person));
    }

    @SuppressWarnings("unchecked")
    private Set<String> keywordSet(String token) {
        return (Set<String>) eventsByKeyword.selectOne(Path.key(FAMILY_ID).key(token));
    }

    // ---- 1. Fan-out completeness: BOTH tags of evt-M1 are indexed ----

    @Test
    @Order(1)
    void bothTagsOfM1AreIndexed() {
        Set<String> permSlip = tagSet("PERMISSION_SLIP");
        Set<String> deadline = tagSet("DEADLINE");
        assertNotNull(permSlip, "PERMISSION_SLIP key should exist");
        assertNotNull(deadline, "DEADLINE key should exist (second element of the tags list)");
        assertTrue(permSlip.contains("evt-M1"), "PERMISSION_SLIP should index evt-M1");
        assertTrue(deadline.contains("evt-M1"),
            "DEADLINE should index evt-M1 — proves EXPLODE iterated the whole tags list, not just element 0");
    }

    @Test
    @Order(2)
    void bothPersonsOfM1AreIndexed() {
        Set<String> emma = personSet("Emma");
        Set<String> dad  = personSet("Dad");
        assertNotNull(emma, "Emma key should exist");
        assertNotNull(dad,  "Dad key should exist (second element of the personId list)");
        assertTrue(emma.contains("evt-M1"), "Emma should index evt-M1");
        assertTrue(dad.contains("evt-M1"),
            "Dad should index evt-M1 — proves personId fan-out iterated the whole list");
    }

    @Test
    @Order(3)
    void sharedPersonAccumulatesAcrossEvents() {
        Set<String> emma = personSet("Emma");
        assertNotNull(emma);
        assertTrue(emma.containsAll(Set.of("evt-M1", "evt-M2")),
            "Emma appears on both events and should index both");
        assertEquals(2, emma.size(), "Emma should index exactly evt-M1 and evt-M2");
    }

    @Test
    @Order(4)
    void unsharedTagIndexesOnlyItsEvent() {
        Set<String> task = tagSet("TASK");
        assertNotNull(task);
        assertEquals(Set.of("evt-M2"), task, "TASK belongs only to evt-M2");
    }

    // ---- 2. Branch isolation: no field bleed between the two indexes ----

    @Test
    @Order(10)
    void personValuesAreNotInTagIndex() {
        assertNull(tagSet("Emma"), "a person name must never become a tag-index key");
        assertNull(tagSet("Dad"),  "a person name must never become a tag-index key");
    }

    @Test
    @Order(11)
    void tagValuesAreNotInPersonIndex() {
        assertNull(personSet("PERMISSION_SLIP"), "a tag must never become a person-index key");
        assertNull(personSet("DEADLINE"),        "a tag must never become a person-index key");
    }

    // ---- 3. Branch coexistence: the keyword fan-out still fires too ----

    @Test
    @Order(20)
    void keywordBranchStillFiresAlongsideTagAndPersonBranches() {
        Set<String> fieldtrip = keywordSet("fieldtrip");
        assertNotNull(fieldtrip,
            "keyword branch should still index title tokens after the tag/person branches were added");
        assertTrue(fieldtrip.contains("evt-M1"),
            "'fieldtrip' (from evt-M1 title) should index evt-M1 — all three hooked fan-outs ran");
    }

    // ---- 4. Classifier-output fields: plumbed into the record, null/empty this session ----

    @Test
    @Order(30)
    @SuppressWarnings("unchecked")
    void classifierOutputFieldsRoundTripAsNullOrEmpty() {
        Map<String, Object> rec = (Map<String, Object>)
            familyData.selectOne(Path.key(FAMILY_ID).key("events").key("evt-M3"));
        assertNotNull(rec, "evt-M3 should be stored in $$family-data");

        // Presence-of-field: the keys must survive serialization even with null values.
        assertTrue(rec.containsKey("confidence"),   "confidence must be plumbed into the record");
        assertTrue(rec.containsKey("reason"),       "reason must be plumbed into the record");
        assertTrue(rec.containsKey("documentType"), "documentType must be plumbed into the record");
        assertTrue(rec.containsKey("relatedEventIds"),
            "relatedEventIds must be plumbed into the record");

        // Null-tolerance: this session they are never populated — null/empty is the passing state.
        assertNull(rec.get("confidence"), "confidence is unpopulated this session");
        assertNull(rec.get("reason"),     "reason is unpopulated this session");
        assertNull(rec.get("documentType"), "documentType is unpopulated this session");
        assertEquals(new ArrayList<>(), rec.get("relatedEventIds"),
            "relatedEventIds starts empty this session");
    }
}
