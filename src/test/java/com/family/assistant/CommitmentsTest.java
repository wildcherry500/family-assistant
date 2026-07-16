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
 * CommitmentsTest
 *
 * Exercises Layer 2's $$commitments write-path (2026-07-16 implementation session):
 * creation recomputed every redrain from ACTION_NEEDED edges only (Fork 3), content
 * fields (sourceEventId/objectId/createdAt) always refreshed, status write-once via a
 * separate *commitment-status-changes depot/branch (Fork 1's corrected mechanism).
 *
 * No GEMINI_API_KEY required — no LLM is used.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class CommitmentsTest {

    private static final String FAMILY_ID = "fam-commitments";
    private static final String MODULE_NAME = "FamilySchemaModule";

    private InProcessCluster ipc;
    private Depot familyEventsDepot;
    private Depot statusChangesDepot;
    private PState edgesForward;
    private PState commitments;

    private String c1Id;
    private String c2Id;

    @BeforeAll
    void setup() throws Exception {
        ipc = InProcessCluster.create();

        FamilySchemaModule schemaModule = new FamilySchemaModule();
        ipc.launchModule(schemaModule, new LaunchConfig(1, 1));

        familyEventsDepot = ipc.clusterDepot(MODULE_NAME, "*family-events");
        statusChangesDepot = ipc.clusterDepot(MODULE_NAME, "*commitment-status-changes");
        edgesForward = ipc.clusterPState(MODULE_NAME, "$$edges-forward");
        commitments = ipc.clusterPState(MODULE_NAME, "$$commitments");

        // evt-K1: one ACTION_NEEDED edge -> PERSON Billy. Should seed exactly one commitment.
        appendEvent("evt-K1", 1000L, List.of(
            triple("ACTION_NEEDED", "PERSON", "Billy")
        ));

        // evt-K2: independent ACTION_NEEDED edge -> PERSON Sam, different event entirely.
        // Must mint a DISTINCT commitmentId from evt-K1's (Fork 5 — no cross-event dedup).
        appendEvent("evt-K2", 2000L, List.of(
            triple("ACTION_NEEDED", "PERSON", "Sam")
        ));

        // evt-K3: MENTIONS_PERSON only, no ACTION_NEEDED triple at all. Must produce ZERO
        // $$commitments writes — Fork 3 scopes seeding to ACTION_NEEDED exclusively.
        appendEvent("evt-K3", 3000L, List.of(
            triple("MENTIONS_PERSON", "PERSON", "NotAnActionItem")
        ));

        Thread.sleep(2000);

        c1Id = commitmentIdForSourceEvent("evt-K1");
        c2Id = commitmentIdForSourceEvent("evt-K2");
    }

    @AfterAll
    void teardown() throws Exception {
        if (ipc != null) ipc.close();
    }

    /** Appends a record with mutable list/map copies (never List.of/Map.of into the depot). */
    private void appendEvent(String id, long createdAt, List<Map<String, String>> relations) {
        List<Map<String, String>> mutableRelations = new ArrayList<>();
        for (Map<String, String> t : relations) mutableRelations.add(new HashMap<>(t));

        Map<String, Object> event = new HashMap<>();
        event.put("id", id);
        event.put("familyId", FAMILY_ID);
        event.put("tags", new ArrayList<String>());
        event.put("personId", new ArrayList<String>());
        event.put("relations", mutableRelations);
        event.put("created", createdAt);
        event.put("title", "evt " + id);
        event.put("description", "");
        event.put("emailSubject", "");
        familyEventsDepot.append(event);
    }

    private void appendStatusChange(String commitmentId, String newStatus, long changedAt) {
        Map<String, Object> change = new HashMap<>();
        change.put("familyId", FAMILY_ID);
        change.put("commitmentId", commitmentId);
        change.put("newStatus", newStatus);
        change.put("changedAt", changedAt);
        statusChangesDepot.append(change);
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

    /**
     * Finds a commitment by scanning the family's whole $$commitments map for a matching
     * sourceEventId, rather than re-deriving the deterministic hash in test code — keeps
     * this test decoupled from FamilySchemaModule's private mintCommitmentId formula.
     */
    @SuppressWarnings("unchecked")
    private String commitmentIdForSourceEvent(String eventId) {
        Map<String, Object> all = (Map<String, Object>) commitments.selectOne(Path.key(FAMILY_ID));
        if (all == null) return null;
        for (Map.Entry<String, Object> e : all.entrySet()) {
            Map<String, Object> rec = (Map<String, Object>) e.getValue();
            if (eventId.equals(rec.get("sourceEventId"))) return e.getKey();
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> commitmentRecord(String commitmentId) {
        return (Map<String, Object>) commitments.selectOne(Path.key(FAMILY_ID).key(commitmentId));
    }

    // ---- 1. Creation fires on first sight ----

    @Test
    @Order(1)
    void creationFiresOnFirstSightWithCorrectContentAndInitialStatus() {
        assertNotNull(c1Id, "an ACTION_NEEDED edge on evt-K1 must seed a commitment");
        Map<String, Object> rec = commitmentRecord(c1Id);
        assertNotNull(rec);

        String expectedObjectId = soleElement(forwardSet("evt-K1", "ACTION_NEEDED"));
        assertEquals("evt-K1", rec.get("sourceEventId"));
        assertEquals(expectedObjectId, rec.get("objectId"));
        assertEquals(1000L, rec.get("createdAt"));
        assertEquals("OPEN", rec.get("status"), "status must initialize to OPEN on first sight");
    }

    // ---- 2. Independent ACTION_NEEDED edges mint distinct commitment IDs ----

    @Test
    @Order(2)
    void secondIndependentActionNeededMintsDistinctCommitmentId() {
        assertNotNull(c2Id, "an ACTION_NEEDED edge on evt-K2 must seed its own commitment");
        assertNotEquals(c1Id, c2Id,
            "evt-K1 and evt-K2 are different events — their commitments must not collapse " +
            "into one, even though both target a PERSON (Fork 5: no cross-event dedup)");
    }

    // ---- 3. Non-ACTION_NEEDED relations seed nothing (Fork 3 scoping) ----

    @Test
    @Order(3)
    void nonActionNeededRelationProducesNoCommitment() {
        assertNull(commitmentIdForSourceEvent("evt-K3"),
            "evt-K3 has only a MENTIONS_PERSON triple — Fork 3 scopes seeding to " +
            "ACTION_NEEDED exclusively, so it must produce zero $$commitments writes");
    }

    // ---- 4. Status change updates status/updatedAt, leaves content untouched ----

    @Test
    @Order(4)
    void statusChangeUpdatesStatusAndLeavesContentUntouched() {
        appendStatusChange(c1Id, "DONE", 5000L);
        waitUntilStatus(c1Id, "DONE");

        Map<String, Object> rec = commitmentRecord(c1Id);
        assertEquals("DONE", rec.get("status"));
        assertEquals(5000L, rec.get("updatedAt"));
        assertEquals("evt-K1", rec.get("sourceEventId"), "content must survive a status change untouched");
        assertEquals(1000L, rec.get("createdAt"), "content must survive a status change untouched");
    }

    // ---- 5. THE guarantee this layer exists to protect: redrain doesn't clobber a DONE ----

    @Test
    @Order(5)
    void redrainOfSourceEdgeDoesNotClobberADoneStatus() throws Exception {
        // Precondition from test 4: c1 is DONE. Re-append the IDENTICAL evt-K1 record,
        // simulating a *family-events redrain (e.g. a parser re-run reprocessing history).
        appendEvent("evt-K1", 1000L, List.of(
            triple("ACTION_NEEDED", "PERSON", "Billy")
        ));
        Thread.sleep(2000);

        Map<String, Object> rec = commitmentRecord(c1Id);
        assertEquals("DONE", rec.get("status"),
            "*** THE CORE GUARANTEE: status must STILL read DONE after redraining the source "
            + "ACTION_NEEDED edge, NOT reset to OPEN. This is the entire reason creation is "
            + "recomputed-but-status-guarded (Fork 1) instead of a plain re-seed. ***");
        assertEquals("evt-K1", rec.get("sourceEventId"), "content refreshed to the same value, idempotently");
        assertEquals(1000L, rec.get("createdAt"), "content refreshed to the same value, idempotently");
    }

    // ---- 6. Status change arriving before creation ever ran: auto-vivified stub ----

    @Test
    @Order(6)
    void statusChangeForNeverSeenCommitmentCreatesStub() {
        String ghostId = "stub-ghost-commitment-1";
        appendStatusChange(ghostId, "WAITING", 9000L);
        waitUntilStatus(ghostId, "WAITING");

        Map<String, Object> rec = commitmentRecord(ghostId);
        assertNotNull(rec, "a status change for a not-yet-materialized commitment must still "
            + "apply — auto-vivify, not silently dropped");
        assertEquals("WAITING", rec.get("status"));
        assertEquals(9000L, rec.get("updatedAt"));
        assertNull(rec.get("sourceEventId"), "identity fields stay absent until/unless the "
            + "creation branch ever processes a matching ACTION_NEEDED edge for this id");
        assertNull(rec.get("objectId"));
        assertNull(rec.get("createdAt"));
    }

    private String soleElement(Set<String> set) {
        assertNotNull(set, "expected a populated Set");
        assertEquals(1, set.size(), "expected exactly one element, got " + set);
        return set.iterator().next();
    }

    /** Condition-poll-with-timeout, per RAMA_VERIFIED_LEARNINGS.md's mirror-depot guidance. */
    private void waitUntilStatus(String commitmentId, String expectedStatus) {
        long deadline = System.currentTimeMillis() + 30_000;
        while (System.currentTimeMillis() < deadline) {
            Map<String, Object> rec = commitmentRecord(commitmentId);
            if (rec != null && expectedStatus.equals(rec.get("status"))) return;
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }
        fail("Timed out waiting for commitment " + commitmentId + " to reach status " + expectedStatus);
    }
}