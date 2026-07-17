package com.family.assistant;

import com.family.assistant.schema.FamilySchemaModule;
import com.family.assistant.webhook.WebhookReceiver;
import com.rpl.rama.Depot;
import com.rpl.rama.PState;
import com.rpl.rama.test.InProcessCluster;
import com.rpl.rama.test.LaunchConfig;
import org.junit.jupiter.api.*;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * OpenItemsAndMarkDoneTest
 *
 * First real consumer/producer pair on top of Layer 2 $$commitments: the open-items view
 * (scan-and-filter, no $$commitments-by-status index — deliberately deferred to Layer 3)
 * and the mark-done path (appends to *commitment-status-changes only; $$commitments itself
 * is owned exclusively by FamilySchemaModule's stream topology).
 *
 * Exercises WebhookReceiver's openCommitments/markDone directly (package-private, no HTTP)
 * against InProcessCluster-sourced PState/Depot handles — the exact same code path the
 * /commitments/{familyId} and /commitments/{id}/done routes call.
 *
 * No GEMINI_API_KEY required — no LLM is used.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class OpenItemsAndMarkDoneTest {

    private static final String FAMILY_ID = "fam-open-items";
    private static final String MODULE_NAME = "FamilySchemaModule";

    private InProcessCluster ipc;
    private Depot familyEventsDepot;
    private WebhookReceiver receiver;
    private PState commitments;

    @BeforeAll
    void setup() throws Exception {
        ipc = InProcessCluster.create();

        FamilySchemaModule schemaModule = new FamilySchemaModule();
        ipc.launchModule(schemaModule, new LaunchConfig(1, 1));

        familyEventsDepot = ipc.clusterDepot(MODULE_NAME, "*family-events");
        Depot statusChangesDepot = ipc.clusterDepot(MODULE_NAME, "*commitment-status-changes");
        commitments = ipc.clusterPState(MODULE_NAME, "$$commitments");

        receiver = new WebhookReceiver(null, null, commitments, statusChangesDepot);
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

    private Map<String, String> triple(String relation, String objectType, String object) {
        Map<String, String> t = new HashMap<>();
        t.put("relation", relation);
        t.put("objectType", objectType);
        t.put("object", object);
        return t;
    }

    @SuppressWarnings("unchecked")
    private String commitmentIdForSourceEvent(String eventId) {
        Map<String, Object> all = (Map<String, Object>) commitments.selectOne(
            com.rpl.rama.Path.key(FAMILY_ID));
        if (all == null) return null;
        for (Map.Entry<String, Object> e : all.entrySet()) {
            Map<String, Object> rec = (Map<String, Object>) e.getValue();
            if (eventId.equals(rec.get("sourceEventId"))) return e.getKey();
        }
        return null;
    }

    private boolean isInOpenItems(String commitmentId) {
        for (Map<String, Object> rec : receiver.openCommitments(FAMILY_ID)) {
            if (commitmentId.equals(rec.get("commitmentId"))) return true;
        }
        return false;
    }

    /** Condition-poll-with-timeout, per RAMA_VERIFIED_LEARNINGS.md's mirror-depot guidance. */
    private void waitUntil(String description, java.util.function.BooleanSupplier condition) {
        long deadline = System.currentTimeMillis() + 30_000;
        while (System.currentTimeMillis() < deadline) {
            if (condition.getAsBoolean()) return;
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }
        fail("Timed out waiting for: " + description);
    }

    @Test
    void fullLoop_ingestThenOpenItemsThenMarkDoneThenRedrainDoesNotResurrect() {
        // 1. Ingest an ACTION_NEEDED event.
        appendEvent("evt-open-1", 1000L, List.of(
            triple("ACTION_NEEDED", "PERSON", "Alex")
        ));

        // 2. Commitment appears in the open-items view.
        waitUntil("commitment materialized for evt-open-1",
            () -> commitmentIdForSourceEvent("evt-open-1") != null);
        String commitmentId = commitmentIdForSourceEvent("evt-open-1");
        assertNotNull(commitmentId);

        waitUntil("commitment appears in open-items view",
            () -> isInOpenItems(commitmentId));

        // 3. POST mark-done.
        receiver.markDone(FAMILY_ID, commitmentId);

        // 4. Commitment no longer appears in open-items view.
        waitUntil("commitment removed from open-items view after mark-done",
            () -> !isInOpenItems(commitmentId));

        // 5. Redrain the identical source event — must not resurrect it as open.
        appendEvent("evt-open-1", 1000L, List.of(
            triple("ACTION_NEEDED", "PERSON", "Alex")
        ));
        // Give the redrain a moment to process, then assert it stays absent from open-items
        // for the remainder of the poll window (not just at one instant).
        long deadline = System.currentTimeMillis() + 3000;
        while (System.currentTimeMillis() < deadline) {
            assertFalse(isInOpenItems(commitmentId),
                "redraining the source ACTION_NEEDED edge after mark-done must NOT resurrect "
                + "the commitment in the open-items view");
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }

        // Sanity: the underlying record is genuinely DONE, not just filtered out by accident.
        @SuppressWarnings("unchecked")
        Map<String, Object> rec = (Map<String, Object>) commitments.selectOne(
            com.rpl.rama.Path.key(FAMILY_ID).key(commitmentId));
        assertEquals("DONE", rec.get("status"));
    }
}