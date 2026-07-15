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
 * EdgesEntityIndexTest
 *
 * Exercises the typed-relation-edge + entity-foundation branch added to
 * FamilySchemaModule's stream topology (2026-07-15 graph-schema-evolution session).
 * No existing fixture in the 126-green suite ever populates a "relations" field, so
 * that branch was previously compiled but never actually run — this test drives it
 * and checks the specific design properties the schema was built around:
 *
 *   1. Forward/inverse edges materialize for every triple, both directions.
 *   2. Entity-ID collapse: the same (objectType, object) reached via two DIFFERENT
 *      relations within one event mints ONE $$entities row, not two.
 *   3. Cross-event distinctness: the same (objectType, object) mentioned in two
 *      DIFFERENT events still mints two different entityIds (no dedup — that's
 *      resolution's job, explicitly out of scope this session).
 *   4. $$entities-by-type makes the entity set inspectable per type.
 *   5. Absent "relations" (not just empty) produces zero writes — no-op, same as
 *      the pre-existing tags/personId branches on records that lack those fields.
 *   6. Idempotency: re-appending an identical record (simulating a depot redrain)
 *      does not create new entity rows or grow edge sets — the deterministic-ID
 *      formula is what makes this true, not incidental.
 *
 * No GEMINI_API_KEY required — no LLM is used.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class EdgesEntityIndexTest {

    private static final String FAMILY_ID = "fam-edges";
    private static final String MODULE_NAME = "FamilySchemaModule";

    private InProcessCluster ipc;
    private Depot familyEventsDepot;
    private PState edgesForward;
    private PState edgesInverse;
    private PState entities;
    private PState entitiesByType;

    private String billyIdFromE1;
    private String billyIdFromE2;
    private String jeffersonId;
    private String unknownWorkId;

    @BeforeAll
    void setup() throws Exception {
        ipc = InProcessCluster.create();

        FamilySchemaModule schemaModule = new FamilySchemaModule();
        ipc.launchModule(schemaModule, new LaunchConfig(1, 1));

        familyEventsDepot = ipc.clusterDepot(MODULE_NAME, "*family-events");
        edgesForward   = ipc.clusterPState(MODULE_NAME, "$$edges-forward");
        edgesInverse   = ipc.clusterPState(MODULE_NAME, "$$edges-inverse");
        entities       = ipc.clusterPState(MODULE_NAME, "$$entities");
        entitiesByType = ipc.clusterPState(MODULE_NAME, "$$entities-by-type");

        // evt-E1: MENTIONS_PERSON->Billy and ACTION_NEEDED->Billy are the SAME
        // (objectType, object) via two different relations — must collapse to one
        // entity. LOCATED_AT->Jefferson Elementary is a distinct entity.
        appendEvent("evt-E1", List.of(
            triple("MENTIONS_PERSON", "PERSON", "Billy"),
            triple("LOCATED_AT", "PLACE", "Jefferson Elementary"),
            triple("ACTION_NEEDED", "PERSON", "Billy")
        ));

        // evt-E2: mentions the same real-world "Billy" as evt-E1, but from a
        // different event — must mint a DIFFERENT entityId (no cross-event dedup).
        appendEvent("evt-E2", List.of(
            triple("MENTIONS_PERSON", "PERSON", "Billy")
        ));

        // evt-E4: a genuinely-unrecognized mention — the creative-work case the design
        // session deferred to UNKNOWN rather than guessing a WORK type. Must land in
        // $$entities-by-type["UNKNOWN"], since that bucket being inspectable (not a
        // silent dump) is the whole reason $$entities-by-type exists.
        appendEvent("evt-E4", List.of(
            triple("UNKNOWN", "UNKNOWN", "Blue Sky Symphony")
        ));

        // evt-E3: no "relations" key at all — must produce zero writes to the new PStates.
        Map<String, Object> e3 = new HashMap<>();
        e3.put("id", "evt-E3");
        e3.put("familyId", FAMILY_ID);
        e3.put("tags", new ArrayList<String>());
        e3.put("personId", new ArrayList<String>());
        e3.put("title", "no relations here");
        e3.put("description", "");
        e3.put("emailSubject", "");
        familyEventsDepot.append(e3);

        Thread.sleep(2000);

        billyIdFromE1 = soleElement(forwardSet("evt-E1", "MENTIONS_PERSON"));
        billyIdFromE2 = soleElement(forwardSet("evt-E2", "MENTIONS_PERSON"));
        jeffersonId   = soleElement(forwardSet("evt-E1", "LOCATED_AT"));
        unknownWorkId = soleElement(forwardSet("evt-E4", "UNKNOWN"));
    }

    @AfterAll
    void teardown() throws Exception {
        if (ipc != null) ipc.close();
    }

    /** Appends a record with mutable list/map copies (never List.of/Map.of into the depot). */
    private void appendEvent(String id, List<Map<String, String>> relations) {
        List<Map<String, String>> mutableRelations = new ArrayList<>();
        for (Map<String, String> t : relations) mutableRelations.add(new HashMap<>(t));

        Map<String, Object> event = new HashMap<>();
        event.put("id", id);
        event.put("familyId", FAMILY_ID);
        event.put("tags", new ArrayList<String>());
        event.put("personId", new ArrayList<String>());
        event.put("relations", mutableRelations);
        event.put("title", "evt " + id);
        event.put("description", "");
        event.put("emailSubject", "");
        familyEventsDepot.append(event);
    }

    private Map<String, String> triple(String relation, String objectType, String object) {
        Map<String, String> t = new HashMap<>();
        t.put("relation", relation);
        t.put("objectType", objectType);
        t.put("object", object);
        return t;
    }

    @SuppressWarnings("unchecked")
    private Set<String> forwardSet(String eventId, String relation) {
        return (Set<String>) edgesForward.selectOne(Path.key(FAMILY_ID).key(eventId).key(relation));
    }

    @SuppressWarnings("unchecked")
    private Set<String> inverseSet(String objectId, String relation) {
        return (Set<String>) edgesInverse.selectOne(Path.key(FAMILY_ID).key(objectId).key(relation));
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> entityRecord(String entityId) {
        return (Map<String, Object>) entities.selectOne(Path.key(FAMILY_ID).key(entityId));
    }

    @SuppressWarnings("unchecked")
    private Set<String> entityTypeSet(String type) {
        return (Set<String>) entitiesByType.selectOne(Path.key(FAMILY_ID).key(type));
    }

    private String soleElement(Set<String> set) {
        assertNotNull(set, "expected a populated Set");
        assertEquals(1, set.size(), "expected exactly one element, got " + set);
        return set.iterator().next();
    }

    // ---- 1. Forward/inverse edges materialize for every triple ----

    @Test
    @Order(1)
    void forwardEdgesExistForAllThreeRelationsOnE1() {
        assertNotNull(forwardSet("evt-E1", "MENTIONS_PERSON"));
        assertNotNull(forwardSet("evt-E1", "LOCATED_AT"));
        assertNotNull(forwardSet("evt-E1", "ACTION_NEEDED"));
    }

    @Test
    @Order(2)
    void inverseEdgesPointBackAtTheSubjectEvent() {
        assertTrue(inverseSet(billyIdFromE1, "MENTIONS_PERSON").contains("evt-E1"));
        assertTrue(inverseSet(billyIdFromE1, "ACTION_NEEDED").contains("evt-E1"));
        assertTrue(inverseSet(jeffersonId, "LOCATED_AT").contains("evt-E1"));
    }

    // ---- 2. Entity-ID collapse within one event ----

    @Test
    @Order(10)
    void sameObjectViaTwoRelationsInOneEventCollapsesToOneEntityId() {
        String viaMentions = soleElement(forwardSet("evt-E1", "MENTIONS_PERSON"));
        String viaAction   = soleElement(forwardSet("evt-E1", "ACTION_NEEDED"));
        assertEquals(viaMentions, viaAction,
            "MENTIONS_PERSON->Billy and ACTION_NEEDED->Billy in the same event must mint the " +
            "same entityId (hash drops the mention index by design) — a mention log would fail this");
    }

    @Test
    @Order(11)
    void collapsedEntityHasExactlyOneEntitiesRow() {
        Map<String, Object> rec = entityRecord(billyIdFromE1);
        assertNotNull(rec, "collapsed Billy entity should have exactly one $$entities row");
        assertEquals("PERSON", rec.get("type"));
        assertEquals("Billy", rec.get("canonicalName"));
        assertEquals(new HashSet<String>(), rec.get("aliases"), "aliases starts empty until resolution runs");
    }

    // ---- 3. Cross-event distinctness — no dedup across events ----

    @Test
    @Order(20)
    void sameObjectInDifferentEventsMintsDifferentEntityIds() {
        assertNotEquals(billyIdFromE1, billyIdFromE2,
            "Billy in evt-E1 and Billy in evt-E2 are different mentions until a future " +
            "resolution effort merges them — this session must NOT dedup across events");
    }

    @Test
    @Order(21)
    void bothBillyMentionsHaveTheirOwnEntitiesRow() {
        assertNotNull(entityRecord(billyIdFromE1));
        assertNotNull(entityRecord(billyIdFromE2));
    }

    // ---- 4. $$entities-by-type inspectability ----

    @Test
    @Order(30)
    void entitiesByTypeIndexesBothPersonMentions() {
        Set<String> persons = entityTypeSet("PERSON");
        assertNotNull(persons);
        assertTrue(persons.containsAll(Set.of(billyIdFromE1, billyIdFromE2)),
            "both Billy mentions (distinct entityIds) should appear under PERSON");
    }

    @Test
    @Order(31)
    void entitiesByTypeIndexesThePlaceMention() {
        Set<String> places = entityTypeSet("PLACE");
        assertNotNull(places);
        assertTrue(places.contains(jeffersonId));
    }

    @Test
    @Order(32)
    void entitiesByTypeIndexesTheUnknownBucketAsARealInspectableQueue() {
        Set<String> unknowns = entityTypeSet("UNKNOWN");
        assertNotNull(unknowns, "$$entities-by-type[\"UNKNOWN\"] must exist and be queryable, " +
            "not a silent dump — this is what makes deferring WORK-type mentions to UNKNOWN safe");
        assertTrue(unknowns.contains(unknownWorkId));

        Map<String, Object> rec = entityRecord(unknownWorkId);
        assertNotNull(rec, "the UNKNOWN-bucket queue must resolve to a real $$entities row");
        assertEquals("UNKNOWN", rec.get("type"));
        assertEquals("Blue Sky Symphony", rec.get("canonicalName"),
            "the raw mention string must be recoverable from the queue — that's the signal");
    }

    // ---- 5. Absent "relations" field is a no-op ----

    @Test
    @Order(40)
    void eventWithNoRelationsFieldWritesNothingToEdges() {
        assertNull(forwardSet("evt-E3", "MENTIONS_PERSON"),
            "an event that never had a \"relations\" field must not appear as a subject in $$edges-forward");
    }

    // ---- 6. Idempotency: re-append the identical record (simulated redrain) ----

    @Test
    @Order(50)
    void reappendingAnIdenticalRecordDoesNotMintNewEntitiesOrGrowSets() throws Exception {
        int personCountBefore = entityTypeSet("PERSON").size();
        int placeCountBefore  = entityTypeSet("PLACE").size();
        int mentionsSetSizeBefore = forwardSet("evt-E1", "MENTIONS_PERSON").size();

        appendEvent("evt-E1", List.of(
            triple("MENTIONS_PERSON", "PERSON", "Billy"),
            triple("LOCATED_AT", "PLACE", "Jefferson Elementary"),
            triple("ACTION_NEEDED", "PERSON", "Billy")
        ));
        Thread.sleep(2000);

        assertEquals(personCountBefore, entityTypeSet("PERSON").size(),
            "re-appending the same event's relations must not mint new PERSON entities");
        assertEquals(placeCountBefore, entityTypeSet("PLACE").size(),
            "re-appending the same event's relations must not mint new PLACE entities");
        assertEquals(mentionsSetSizeBefore, forwardSet("evt-E1", "MENTIONS_PERSON").size(),
            "re-appending must not grow the forward-edge Set — same deterministic objectId every time");
        assertEquals(billyIdFromE1, soleElement(forwardSet("evt-E1", "MENTIONS_PERSON")),
            "the re-derived entityId must be identical to the original — this is the property " +
            "that makes PState redrain safe (Rama requires deterministic processing; " +
            "verified at redplanetlabs.com/docs/~/operating-rama.html, \"Task scaling\" section)");
    }
}