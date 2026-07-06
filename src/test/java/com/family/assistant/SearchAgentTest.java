package com.family.assistant;

import com.family.assistant.query.QueryModule;
import com.family.assistant.schema.FamilySchemaModule;
import com.rpl.agentorama.AgentClient;
import com.rpl.agentorama.AgentManager;
import com.rpl.rama.Depot;
import com.rpl.rama.Path;
import com.rpl.rama.PState;
import com.rpl.rama.test.InProcessCluster;
import com.rpl.rama.test.LaunchConfig;
import org.junit.jupiter.api.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * SearchAgentTest
 *
 * Invokes QueryModule's search-agent DIRECTLY (no LLM — search-agent has no
 * LLM calls at all; hand-built QueryParams stand in for what interpret-query
 * would have extracted). Reproduces the exact bug this session fixes: a
 * wrong categoryFilter guess used to zero out results even when a matching
 * event genuinely existed. Also proves the two-tier hard/soft fallback
 * doesn't over-correct — wrong HARD dimensions (keywords) still correctly
 * find nothing, even when a SOFT dimension would have matched.
 *
 * No GEMINI_API_KEY required.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class SearchAgentTest {

    private static final String FAMILY_ID       = "search-agent-test-family";
    private static final String SCAN_FAMILY_ID  = "search-agent-scan-family";
    private static final String TIMEZONE        = "America/Los_Angeles";

    // Within the "March" range used by the date-range test.
    private static final long T1_MAR20 = Instant.parse("2026-03-20T08:00:00Z").toEpochMilli();
    // Same keyword as evt-A, but far outside the March range.
    private static final long T_FAR    = Instant.parse("2026-06-01T08:00:00Z").toEpochMilli();
    private static final long T2_MAR22 = Instant.parse("2026-03-22T08:00:00Z").toEpochMilli();

    private InProcessCluster ipc;
    private Depot familyEventsDepot;
    private PState familyData;
    private AgentClient searchAgent;

    @BeforeAll
    void setup() throws Exception {
        ipc = InProcessCluster.create();

        FamilySchemaModule schemaModule = new FamilySchemaModule();
        ipc.launchModule(schemaModule, new LaunchConfig(1, 1));

        QueryModule queryModule = new QueryModule();
        ipc.launchModule(queryModule, new LaunchConfig(1, 1));

        familyEventsDepot = ipc.clusterDepot(schemaModule.getModuleName(), "*family-events");
        familyData        = ipc.clusterPState(schemaModule.getModuleName(), "$$family-data");

        AgentManager searchManager = AgentManager.create(ipc, queryModule.getModuleName());
        searchAgent = searchManager.getAgentClient("search-agent");

        // --- Main fixture family (tests 2-5) ---
        // evt-A: SCHOOL_EVENT, "permission slip" text, within the March date range.
        appendEvent("evt-A", FAMILY_ID, "Permission Slip Due",
            "Please return the signed permission slip", "SCHOOL_EVENT",
            "VAULT", "ACTION_REQUIRED", T1_MAR20);
        // evt-D: same category/silo/intent AND shares the "permission" keyword with
        // evt-A, but far outside the March date range — proves HARD (date) actually
        // filters even when every SOFT dimension would agree.
        appendEvent("evt-D", FAMILY_ID, "Permission form for club",
            "Submit permission paperwork", "SCHOOL_EVENT",
            "VAULT", "ACTION_REQUIRED", T_FAR);
        // evt-E: different category, but shares the "permission" keyword with evt-A —
        // proves a SOFT dimension (categoryFilter) genuinely narrows results in the
        // non-fallback path (fullResult non-empty), not just when it happens to agree.
        appendEvent("evt-E", FAMILY_ID, "Permission granted",
            "Permission was granted for the recital", "TASK",
            "STUDIO", "FYI", T2_MAR22);

        waitForEventsIndexed(FAMILY_ID, 3, 10_000);

        // --- Isolated family for the zero-dimension full-scan test ---
        appendEvent("evt-scan-1", SCAN_FAMILY_ID, "First event", "Some text", "TASK",
            "VAULT", "FYI", T1_MAR20);
        appendEvent("evt-scan-2", SCAN_FAMILY_ID, "Second event", "Other text", "TASK",
            "VAULT", "FYI", T2_MAR22);

        waitForEventsIndexed(SCAN_FAMILY_ID, 2, 10_000);
    }

    @AfterAll
    void teardown() throws Exception {
        if (ipc != null) ipc.close();
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private void appendEvent(String id, String familyId, String title, String description,
                              String eventType, String silo, String intent, long startTime) {
        Map<String, Object> event = new HashMap<>();
        event.put("id",           id);
        event.put("familyId",     familyId);
        event.put("title",        title);
        event.put("description",  description);
        event.put("emailSubject", null);
        List<String> tags = new ArrayList<>();
        if (eventType != null) tags.add(eventType);
        event.put("tags",         tags);
        event.put("silo",         silo);
        event.put("intent",       intent);
        event.put("startTime",    startTime);
        event.put("deadline",     null);
        event.put("personId",     new ArrayList<String>());
        event.put("status",       "pending");
        event.put("sourceType",   "test");
        event.put("accountLabel", null);
        event.put("created",      System.currentTimeMillis());
        event.put("updated",      System.currentTimeMillis());
        familyEventsDepot.append(event);
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

    private QueryModule.QueryParams params(String familyId, List<String> keywords,
                                            String dateFrom, String dateTo,
                                            String childName, String categoryFilter,
                                            String siloFilter, String intentFilter) {
        return new QueryModule.QueryParams(
            "GENERAL", childName, dateFrom, dateTo, categoryFilter,
            "test question", familyId, TIMEZONE, null,
            keywords != null ? keywords : new ArrayList<>(), siloFilter, intentFilter);
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> search(QueryModule.QueryParams params) {
        return (List<Map<String, Object>>) searchAgent.invoke(params);
    }

    private boolean containsEvent(List<Map<String, Object>> results, String eventId) {
        return results.stream().anyMatch(e -> eventId.equals(e.get("id")));
    }

    // -----------------------------------------------------------------------
    // Test 1: zero dimensions -> full scan
    // -----------------------------------------------------------------------

    @Test
    @Order(1)
    void testZeroDimensionsFallsBackToFullScan() {
        QueryModule.QueryParams p = params(SCAN_FAMILY_ID, null, null, null, null, null, null, null);
        List<Map<String, Object>> results = search(p);

        assertEquals(2, results.size());
        assertTrue(containsEvent(results, "evt-scan-1"));
        assertTrue(containsEvent(results, "evt-scan-2"));
    }

    // -----------------------------------------------------------------------
    // Test 2: wrong categoryFilter (SOFT) + right keywords (HARD) -> found via fallback
    // -----------------------------------------------------------------------

    @Test
    @Order(2)
    void testWrongCategoryRightKeywordsFindsEventViaFallback() {
        // evt-A is actually SCHOOL_EVENT — PERMISSION_SLIP is a wrong guess, exactly
        // reproducing the original QueryAgentTest bug scenario.
        QueryModule.QueryParams p = params(FAMILY_ID,
            List.of("permission", "slip"), null, null, null, "PERMISSION_SLIP", null, null);
        List<Map<String, Object>> results = search(p);

        assertTrue(containsEvent(results, "evt-A"),
            "Wrong categoryFilter must not zero out a real keyword match — this is the bug being fixed");
    }

    // -----------------------------------------------------------------------
    // Test 3 (control): wrong keywords (HARD) + right categoryFilter (SOFT) -> empty
    // -----------------------------------------------------------------------

    @Test
    @Order(3)
    void testWrongKeywordsRightCategoryFindsNothing() {
        // categoryFilter=SCHOOL_EVENT would match evt-A/evt-D on its own, but wrong
        // keywords must NOT be rescued by a correct soft dimension — HARD stays HARD,
        // proving the fallback doesn't quietly become "any one matching dimension wins."
        QueryModule.QueryParams p = params(FAMILY_ID,
            List.of("banana", "spaceship"), null, null, null, "SCHOOL_EVENT", null, null);
        List<Map<String, Object>> results = search(p);

        assertTrue(results.isEmpty(),
            "Wrong keywords must not be rescued by a correct soft dimension");
    }

    // -----------------------------------------------------------------------
    // Test 4: two HARD dimensions (keywords + dateRange) genuinely filter each other
    // -----------------------------------------------------------------------

    @Test
    @Order(4)
    void testTwoHardDimensionsFilterEachOther() {
        // Both evt-A and evt-D contain "permission", but only evt-A is in the March
        // date range. The fallback only ever drops SOFT dimensions — it must not
        // loosen this HARD∩HARD intersection.
        QueryModule.QueryParams p = params(FAMILY_ID,
            List.of("permission"), "2026-03-01", "2026-03-31", null, null, null, null);
        List<Map<String, Object>> results = search(p);

        assertTrue(containsEvent(results, "evt-A"), "evt-A is in range and matches keyword");
        assertFalse(containsEvent(results, "evt-D"), "evt-D matches keyword but is outside the date range");
    }

    // -----------------------------------------------------------------------
    // Test 5: SOFT dimension narrows correctly in the non-fallback path
    // -----------------------------------------------------------------------

    @Test
    @Order(5)
    void testSoftDimensionNarrowsWhenFullResultNonEmpty() {
        // "permission" matches evt-A, evt-D, and evt-E. categoryFilter=SCHOOL_EVENT
        // matches evt-A and evt-D but not evt-E. Full intersection (keywords ∩
        // category) = {evt-A, evt-D}, non-empty, so NO fallback triggers — proving
        // the soft dimension is actually applied, not just ignored until it's needed.
        QueryModule.QueryParams p = params(FAMILY_ID,
            List.of("permission"), null, null, null, "SCHOOL_EVENT", null, null);
        List<Map<String, Object>> results = search(p);

        assertTrue(containsEvent(results, "evt-A"));
        assertTrue(containsEvent(results, "evt-D"));
        assertFalse(containsEvent(results, "evt-E"), "evt-E is TASK, not SCHOOL_EVENT — soft dimension should exclude it");
    }

    // -----------------------------------------------------------------------
    // Test 6: SOFT-only dimensions with no HARD anchor -> empty stays empty, no fallback
    // -----------------------------------------------------------------------

    @Test
    @Order(6)
    void testSoftOnlyEmptyIntersectionStaysEmpty() {
        // No keywords, no dateRange (no HARD dimension at all) — categoryFilter and
        // siloFilter disagree (no single event is both DEADLINE and STUDIO in this
        // fixture), and there's no hard anchor to fall back to.
        QueryModule.QueryParams p = params(FAMILY_ID,
            null, null, null, null, "DEADLINE", "STUDIO", null);
        List<Map<String, Object>> results = search(p);

        assertTrue(results.isEmpty(),
            "All-soft empty intersection with no HARD dimension present must stay empty");
    }
}
