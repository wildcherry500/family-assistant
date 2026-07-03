package com.family.assistant;

import com.family.assistant.digest.DigestModule;
import com.family.assistant.schema.FamilySchemaModule;
import com.rpl.agentorama.AgentClient;
import com.rpl.agentorama.AgentManager;
import com.rpl.rama.Depot;
import com.rpl.rama.Path;
import com.rpl.rama.PState;
import com.rpl.rama.test.InProcessCluster;
import com.rpl.rama.test.LaunchConfig;
import org.junit.jupiter.api.*;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * WeaknessLeverageMapTest
 *
 * Verifies that FamilySchemaModule correctly populates $$leverage-map and
 * $$weakness-map from *weakness-leverage-config records, and that DigestModule
 * reads both at digest time to (a) reorder events so leverage matches float to
 * the top and (b) annotate weakness-matched events with a "Note:" line.
 *
 * Also verifies the graceful-no-op case: a family with no config entries at
 * all gets today's unmodified digest behavior (soonest-first, no annotation).
 *
 * No GEMINI_API_KEY required — events are appended directly to *family-events
 * with fields already set, exercising only FamilySchemaModule's indexing and
 * DigestModule's ordering/annotation logic, not any LLM classify stage.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class WeaknessLeverageMapTest {

    private static final String FAMILY_ID       = "weakness-leverage-test-family";
    private static final String EMPTY_FAMILY_ID = "weakness-leverage-empty-family";
    private static final String TIMEZONE        = "America/Los_Angeles";

    private InProcessCluster ipc;
    private Depot  familyEventsDepot;
    private Depot  configDepot;
    private PState familyData;
    private PState leverageMap;
    private PState weaknessMap;
    private AgentClient digestAgent;

    // A window wide enough to cover all three test events' startTime values below.
    private static final long WINDOW_START = 1_700_000_000_000L;
    private static final long WINDOW_END   = 1_700_100_000_000L;

    @BeforeAll
    void setup() throws Exception {
        ipc = InProcessCluster.create();

        FamilySchemaModule schemaModule = new FamilySchemaModule();
        ipc.launchModule(schemaModule, new LaunchConfig(1, 1));

        DigestModule digestModule = new DigestModule();
        ipc.launchModule(digestModule, new LaunchConfig(1, 1));

        familyEventsDepot = ipc.clusterDepot(schemaModule.getModuleName(), "*family-events");
        configDepot       = ipc.clusterDepot(schemaModule.getModuleName(), "*weakness-leverage-config");
        familyData        = ipc.clusterPState(schemaModule.getModuleName(), "$$family-data");
        leverageMap       = ipc.clusterPState(schemaModule.getModuleName(), "$$leverage-map");
        weaknessMap       = ipc.clusterPState(schemaModule.getModuleName(), "$$weakness-map");

        AgentManager digestManager = AgentManager.create(ipc, digestModule.getModuleName());
        digestAgent = digestManager.getAgentClient("digest-agent");

        // Three events in FAMILY_ID's window, soonest-first would be: early, mid, late.
        // "late" carries a STUDIO/ACTION_REQUIRED leverage match and should float to top.
        // "mid" carries an OFFICE/DECISION_NEEDED weakness match and should get annotated.
        appendEvent("evt-early", FAMILY_ID, "VAULT",  "FYI",              WINDOW_START + 1_000);
        appendEvent("evt-mid",   FAMILY_ID, "OFFICE", "DECISION_NEEDED",  WINDOW_START + 2_000);
        appendEvent("evt-late",  FAMILY_ID, "STUDIO", "ACTION_REQUIRED",  WINDOW_START + 3_000);

        appendConfigEntry(FAMILY_ID, "LEVERAGE", "lev-studio-action", "STUDIO", "ACTION_REQUIRED", 100L, null, null);
        appendConfigEntry(FAMILY_ID, "LEVERAGE", "lev-any-action",    null,     "ACTION_REQUIRED", 50L,  null, null);
        appendConfigEntry(FAMILY_ID, "WEAKNESS", "weak-decision",     "OFFICE", "DECISION_NEEDED", null, "SUMMARIZE_LONG_TEXT", "Decisions like this tend to get missed — summarized here.");

        waitForEventsIndexed(FAMILY_ID, 3, 10_000);
        waitForConfigIndexed(FAMILY_ID, 10_000);

        // EMPTY_FAMILY_ID gets one event and zero config entries — graceful no-op case.
        appendEvent("evt-empty-only", EMPTY_FAMILY_ID, "VAULT", "FYI", WINDOW_START + 1_000);
        waitForEventsIndexed(EMPTY_FAMILY_ID, 1, 10_000);
    }

    @AfterAll
    void teardown() throws Exception {
        if (ipc != null) ipc.close();
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private void appendEvent(String id, String familyId, String silo, String intent, long startTime) {
        Map<String, Object> event = new HashMap<>();
        event.put("id",          id);
        event.put("familyId",    familyId);
        event.put("title",       "Test event " + id);
        event.put("eventType",   "TASK");
        event.put("silo",        silo);
        event.put("intent",      intent);
        event.put("startTime",   startTime);
        event.put("deadline",    null);
        event.put("childName",   null);
        event.put("childId",     null);
        event.put("status",      "pending");
        event.put("description", "Test event " + id);
        event.put("sourceType",  "test");
        event.put("accountLabel", null);
        event.put("created",     System.currentTimeMillis());
        event.put("updated",     System.currentTimeMillis());
        familyEventsDepot.append(event);
    }

    private void appendConfigEntry(String familyId, String mapType, String entryId,
                                    String silo, String intent, Long weight, String tag, String note) {
        Map<String, Object> entry = new HashMap<>();
        entry.put("silo",   silo);
        entry.put("intent", intent);
        if (weight != null) entry.put("weight", weight);
        if (tag != null)    entry.put("tag", tag);
        if (note != null)   entry.put("note", note);

        Map<String, Object> configRecord = new HashMap<>();
        configRecord.put("familyId", familyId);
        configRecord.put("mapType",  mapType);
        configRecord.put("entryId",  entryId);
        configRecord.put("entry",    entry);
        configDepot.append(configRecord);
    }

    private void waitForEventsIndexed(String familyId, int expectedCount, long timeoutMs) throws InterruptedException {
        long deadline = System.currentTimeMillis() + timeoutMs;
        while (System.currentTimeMillis() < deadline) {
            Map<?, ?> events = (Map<?, ?>) familyData.selectOne(Path.key(familyId).key("events"));
            if (events != null && events.size() >= expectedCount) return;
            Thread.sleep(50);
        }
        fail("Timed out waiting for " + expectedCount + " events to be indexed for " + familyId);
    }

    private void waitForConfigIndexed(String familyId, long timeoutMs) throws InterruptedException {
        long deadline = System.currentTimeMillis() + timeoutMs;
        while (System.currentTimeMillis() < deadline) {
            Map<?, ?> leverage = (Map<?, ?>) leverageMap.selectOne(Path.key(familyId));
            Map<?, ?> weakness = (Map<?, ?>) weaknessMap.selectOne(Path.key(familyId));
            if (leverage != null && leverage.size() >= 2 && weakness != null && weakness.size() >= 1) return;
            Thread.sleep(50);
        }
        fail("Timed out waiting for weakness/leverage config to be indexed for " + familyId);
    }

    // -----------------------------------------------------------------------
    // $$leverage-map / $$weakness-map population
    // -----------------------------------------------------------------------

    @Test
    @Order(1)
    void testLeverageMapPopulated() {
        Map<?, ?> entries = (Map<?, ?>) leverageMap.selectOne(Path.key(FAMILY_ID));
        assertNotNull(entries, "leverage-map should exist for family");
        assertEquals(2, entries.size());
        assertTrue(entries.containsKey("lev-studio-action"));
        assertTrue(entries.containsKey("lev-any-action"));
    }

    @Test
    @Order(2)
    void testLeverageEntryFieldsPersisted() {
        @SuppressWarnings("unchecked")
        Map<String, Object> entry = (Map<String, Object>)
            ((Map<?, ?>) leverageMap.selectOne(Path.key(FAMILY_ID))).get("lev-studio-action");
        assertNotNull(entry);
        assertEquals("STUDIO", entry.get("silo"));
        assertEquals("ACTION_REQUIRED", entry.get("intent"));
        assertEquals(100L, ((Number) entry.get("weight")).longValue());
    }

    @Test
    @Order(3)
    void testWeaknessMapPopulated() {
        Map<?, ?> entries = (Map<?, ?>) weaknessMap.selectOne(Path.key(FAMILY_ID));
        assertNotNull(entries, "weakness-map should exist for family");
        assertEquals(1, entries.size());
        assertTrue(entries.containsKey("weak-decision"));
    }

    @Test
    @Order(4)
    void testWeaknessEntryFieldsPersisted() {
        @SuppressWarnings("unchecked")
        Map<String, Object> entry = (Map<String, Object>)
            ((Map<?, ?>) weaknessMap.selectOne(Path.key(FAMILY_ID))).get("weak-decision");
        assertNotNull(entry);
        assertEquals("OFFICE", entry.get("silo"));
        assertEquals("DECISION_NEEDED", entry.get("intent"));
        assertEquals("SUMMARIZE_LONG_TEXT", entry.get("tag"));
    }

    @Test
    @Order(5)
    void testEmptyFamilyHasNoConfigEntries() {
        Map<?, ?> leverage = (Map<?, ?>) leverageMap.selectOne(Path.key(EMPTY_FAMILY_ID));
        Map<?, ?> weakness = (Map<?, ?>) weaknessMap.selectOne(Path.key(EMPTY_FAMILY_ID));
        assertTrue(leverage == null || leverage.isEmpty());
        assertTrue(weakness == null || weakness.isEmpty());
    }

    // -----------------------------------------------------------------------
    // DigestModule behavior
    // -----------------------------------------------------------------------

    @Test
    @Order(6)
    void testLeverageMatchFloatsToTop() {
        DigestModule.DigestRequest request =
            new DigestModule.DigestRequest(FAMILY_ID, WINDOW_START, WINDOW_END, null, TIMEZONE);
        String digest = (String) digestAgent.invoke(request);

        assertNotNull(digest);
        int lateIdx  = digest.indexOf("evt-late");
        int midIdx   = digest.indexOf("evt-mid");
        int earlyIdx = digest.indexOf("evt-early");
        assertTrue(lateIdx >= 0 && midIdx >= 0 && earlyIdx >= 0, "All three events should appear in digest");

        // evt-late has the highest leverage score (STUDIO+ACTION_REQUIRED, weight 100)
        // and should appear before both other events, even though it's chronologically last.
        assertTrue(lateIdx < midIdx, "Leverage-matched evt-late should float above evt-mid");
        assertTrue(lateIdx < earlyIdx, "Leverage-matched evt-late should float above evt-early");

        // evt-mid and evt-early are tied at score 0 — soonest-first tiebreak applies.
        assertTrue(earlyIdx < midIdx, "Among non-matching events, soonest-first tiebreak should hold");
    }

    @Test
    @Order(7)
    void testWeaknessMatchIsAnnotated() {
        DigestModule.DigestRequest request =
            new DigestModule.DigestRequest(FAMILY_ID, WINDOW_START, WINDOW_END, null, TIMEZONE);
        String digest = (String) digestAgent.invoke(request);

        int midIdx = digest.indexOf("evt-mid");
        assertTrue(midIdx >= 0);
        int nextEventIdx = digest.indexOf("•", midIdx + 1); // next "• " bullet, or -1 if evt-mid is last
        String midBlock = nextEventIdx >= 0 ? digest.substring(midIdx, nextEventIdx) : digest.substring(midIdx);

        assertTrue(midBlock.contains("Note:"), "Weakness-matched event should have a Note: line");
        assertTrue(midBlock.contains("SUMMARIZE_LONG_TEXT"), "Note should carry the weakness tag");
    }

    @Test
    @Order(8)
    void testNonMatchedEventHasNoAnnotation() {
        DigestModule.DigestRequest request =
            new DigestModule.DigestRequest(FAMILY_ID, WINDOW_START, WINDOW_END, null, TIMEZONE);
        String digest = (String) digestAgent.invoke(request);

        int earlyIdx = digest.indexOf("evt-early");
        int midIdx   = digest.indexOf("evt-mid");
        assertTrue(earlyIdx >= 0 && midIdx >= 0);
        String earlyBlock = digest.substring(earlyIdx, midIdx > earlyIdx ? midIdx : digest.length());

        assertFalse(earlyBlock.contains("Note:"), "Non-matched event should not carry a Note: line");
    }

    @Test
    @Order(9)
    void testGracefulNoOpWhenConfigMapsEmpty() {
        DigestModule.DigestRequest request =
            new DigestModule.DigestRequest(EMPTY_FAMILY_ID, WINDOW_START, WINDOW_END, null, TIMEZONE);
        String digest = (String) digestAgent.invoke(request);

        assertNotNull(digest);
        assertFalse(digest.isBlank());
        assertTrue(digest.contains("evt-empty-only"));
        assertFalse(digest.contains("Note:"), "No weakness config should mean no annotation");
    }
}
