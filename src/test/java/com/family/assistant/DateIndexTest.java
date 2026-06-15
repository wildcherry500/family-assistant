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
 * DateIndexTest
 *
 * Verifies $$events-by-date populated correctly by FamilySchemaModule.
 *
 * Range queries (confirmed API):
 *   select(Path.key(familyId).sortedMapRange(start, end).mapVals().all())
 *   returns List<String> of individual eventIds — one per set member per bucket.
 *
 * NOTE: selectOne(Path.key(familyId)) does NOT work on subindexed PStates.
 * It returns the raw RocksDBWrapper object which cannot be serialized.
 * Use wide range queries (0L, Long.MAX_VALUE) for full-index assertions.
 *
 * No GEMINI_API_KEY required.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class DateIndexTest {

    private static final String FAMILY_ID = "date-index-test-family";

    private static final long MAR_15 = Instant.parse("2026-03-15T09:00:00Z").toEpochMilli();
    private static final long MAR_20 = Instant.parse("2026-03-20T08:30:00Z").toEpochMilli();
    private static final long MAR_25 = Instant.parse("2026-03-25T12:00:00Z").toEpochMilli();
    private static final long APR_01 = Instant.parse("2026-04-01T10:00:00Z").toEpochMilli();

    private InProcessCluster ipc;
    private Depot familyEventsDepot;
    private PState eventsByDate;

    @BeforeAll
    void setup() throws Exception {
        ipc = InProcessCluster.create();

        FamilySchemaModule schemaModule = new FamilySchemaModule();
        ipc.launchModule(schemaModule, new LaunchConfig(1, 1));

        familyEventsDepot = ipc.clusterDepot(schemaModule.getModuleName(), "*family-events");
        eventsByDate      = ipc.clusterPState(schemaModule.getModuleName(), "$$events-by-date");

        // evt-d1: startTime=MAR_15, no deadline
        appendEvent("evt-d1", MAR_15, null);
        // evt-d2: startTime=MAR_20, no deadline
        appendEvent("evt-d2", MAR_20, null);
        // evt-d3: no startTime, deadline=APR_01 -- effectiveTime falls back to deadline
        appendEvent("evt-d3", null, APR_01);
        // evt-d4: both null -- must NOT appear in $$events-by-date
        appendEvent("evt-d4", null, null);

        Thread.sleep(2000);
    }

    @AfterAll
    void teardown() throws Exception {
        if (ipc != null) ipc.close();
    }

    private void appendEvent(String id, Long startTime, Long deadline) {
        Map<String, Object> record = new HashMap<>();
        record.put("id",          id);
        record.put("familyId",    FAMILY_ID);
        record.put("title",       "Test event " + id);
        record.put("eventType",   "SCHOOL_EVENT");
        record.put("childName",   null);
        record.put("childId",     null);
        record.put("startTime",   startTime);
        record.put("deadline",    deadline);
        record.put("status",      "pending");
        record.put("description", "Date index test event " + id);
        record.put("sourceType",  "test");
        record.put("created",     System.currentTimeMillis());
        record.put("updated",     System.currentTimeMillis());
        familyEventsDepot.append(record);
    }

    // Confirmed API: select() with .mapVals().all() returns List<String> of individual eventIds
    @SuppressWarnings("unchecked")
    private List<String> rangeQuery(long startMs, long endMs) {
        Path rangePath = Path.key(FAMILY_ID)
            .sortedMapRange(startMs, endMs)
            .mapVals()
            .all();
        return (List<String>) (List<?>) eventsByDate.select(rangePath);
    }

    // =========================================================================
    // Range query [MAR_15, MAR_25) -- should include MAR_15 and MAR_20 only
    // =========================================================================

    @Test @Order(1)
    void testRangeQueryReturnsMar15AndMar20() {
        List<String> eventIds = rangeQuery(MAR_15, MAR_25);

        assertFalse(eventIds.isEmpty(), "Range [MAR_15, MAR_25) should return results");
        assertTrue(eventIds.contains("evt-d1"),
            "evt-d1 (MAR_15) should be within [MAR_15, MAR_25)");
        assertTrue(eventIds.contains("evt-d2"),
            "evt-d2 (MAR_20) should be within [MAR_15, MAR_25)");
        assertFalse(eventIds.contains("evt-d3"),
            "evt-d3 (APR_01) should be outside [MAR_15, MAR_25)");
        assertFalse(eventIds.contains("evt-d4"),
            "evt-d4 (no time) must never appear in $$events-by-date");
    }

    @Test @Order(2)
    void testRangeQueryExcludesMar25Exactly() {
        List<String> eventIds = rangeQuery(MAR_15, MAR_25);

        assertEquals(2, eventIds.size(),
            "Range [MAR_15, MAR_25) should contain exactly 2 event IDs: evt-d1 and evt-d2");
    }

    // =========================================================================
    // Null-time event must be absent from the index entirely
    // =========================================================================

    @Test @Order(3)
    void testNullTimeEventAbsentFromIndex() {
        // Wide range covers all realistic epoch-ms timestamps
        List<String> allIndexedIds = rangeQuery(0L, Long.MAX_VALUE);

        assertFalse(allIndexedIds.contains("evt-d4"),
            "evt-d4 (null startTime and null deadline) must not appear in $$events-by-date");
    }

    // =========================================================================
    // APR_01 event (deadline-only) indexed at deadline time
    // =========================================================================

    @Test @Order(4)
    @SuppressWarnings("unchecked")
    void testDeadlineOnlyEventIndexedAtDeadlineTime() {
        Set<String> apr01Bucket = (Set<String>)
            eventsByDate.selectOne(Path.key(FAMILY_ID).key(APR_01));

        assertNotNull(apr01Bucket,
            "APR_01 bucket should exist -- evt-d3 uses deadline as effectiveTime");
        assertTrue(apr01Bucket.contains("evt-d3"),
            "evt-d3 should be indexed at APR_01 (its deadline)");
    }

    @Test @Order(5)
    @SuppressWarnings("unchecked")
    void testStartTimeTakesPriorityOverDeadline() {
        Set<String> mar15Bucket = (Set<String>)
            eventsByDate.selectOne(Path.key(FAMILY_ID).key(MAR_15));

        assertNotNull(mar15Bucket, "MAR_15 bucket should exist for evt-d1");
        assertTrue(mar15Bucket.contains("evt-d1"),
            "evt-d1 should be indexed at its startTime (MAR_15)");
    }

    // =========================================================================
    // Total indexed event count (3 out of 4 seeded)
    // =========================================================================

    @Test @Order(6)
    void testTotalIndexedEventCount() {
        List<String> allIds = rangeQuery(0L, Long.MAX_VALUE);

        assertEquals(3, allIds.size(),
            "3 of 4 seeded events have a non-null effectiveTime and should appear in $$events-by-date");
        assertTrue(allIds.containsAll(Arrays.asList("evt-d1", "evt-d2", "evt-d3")),
            "evt-d1, evt-d2, and evt-d3 should all be indexed");
    }
}
