package com.family.assistant;

import com.family.assistant.digest.DigestModule;
import com.family.assistant.email.EmailParsingModule;
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

import java.io.*;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * NonLlmPipelineTest
 *
 * Tests the full Rama pipeline (schema → digest → query) WITHOUT any LLM calls.
 * Pre-populates $$family-data by appending event records directly to the
 * *family-events depot, then tests filtering, sorting, and edge cases.
 *
 * No GEMINI_API_KEY required. All tests run in `mvn test` by default.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class NonLlmPipelineTest {

    private static final String FAMILY_ID = "test-family-001";

    private InProcessCluster ipc;
    private Depot familyEventsDepot;
    private PState familyData;
    private AgentClient digestAgent;

    // Pre-built test events with known dates
    private static final long MAR_15 = toEpoch("2026-03-15T09:00:00Z");
    private static final long MAR_18 = toEpoch("2026-03-18T14:00:00Z");
    private static final long MAR_20 = toEpoch("2026-03-20T08:30:00Z");
    private static final long MAR_22 = toEpoch("2026-03-22T18:00:00Z");
    private static final long MAR_25 = toEpoch("2026-03-25T12:00:00Z");
    private static final long APR_01 = toEpoch("2026-04-01T10:00:00Z");

    private static long toEpoch(String iso) {
        return Instant.parse(iso).toEpochMilli();
    }

    @BeforeAll
    void setup() throws Exception {
        ipc = InProcessCluster.create();

        FamilySchemaModule schemaModule = new FamilySchemaModule();
        ipc.launchModule(schemaModule, new LaunchConfig(1, 1));

        DigestModule digestModule = new DigestModule();
        ipc.launchModule(digestModule, new LaunchConfig(1, 1));

        familyEventsDepot = ipc.clusterDepot(schemaModule.getModuleName(), "*family-events");
        familyData = ipc.clusterPState(schemaModule.getModuleName(), "$$family-data");

        AgentManager digestManager = AgentManager.create(ipc, digestModule.getModuleName());
        digestAgent = digestManager.getAgentClient("digest-agent");

        // Seed test data
        seedTestEvents();

        // Wait for stream topology to process
        Thread.sleep(2000);
    }

    @AfterAll
    void teardown() throws Exception {
        if (ipc != null) ipc.close();
    }

    private void seedTestEvents() {
        appendEvent("evt-001", FAMILY_ID, "Zoo Field Trip", "SCHOOL_EVENT",
            MAR_20, null, "Billy", "pending",
            "Visit to Woodland Park Zoo for 3rd graders");

        appendEvent("evt-002", FAMILY_ID, "Permission Slip Due", "PERMISSION_SLIP",
            null, MAR_18, "Billy", "pending",
            "Sign and return zoo field trip permission slip");

        appendEvent("evt-003", FAMILY_ID, "Science Fair", "SCHOOL_EVENT",
            MAR_25, MAR_22, "Emma", "pending",
            "Annual science fair, projects due Mar 22");

        appendEvent("evt-004", FAMILY_ID, "Spring Picture Day", "SCHOOL_EVENT",
            MAR_18, null, null, "completed",
            "School picture day");

        appendEvent("evt-005", FAMILY_ID, "Read-a-thon Pledge Forms", "DEADLINE",
            null, MAR_15, null, "pending",
            "Return read-a-thon pledge forms by end of week");

        appendEvent("evt-006", FAMILY_ID, "Parent-Teacher Conference", "SCHOOL_EVENT",
            APR_01, null, "Billy", "pending",
            "Spring parent-teacher conference");
    }

    private void appendEvent(String id, String familyId, String title,
                             String eventType, Long startTime, Long deadline,
                             String childName, String status, String description) {
        Map<String, Object> event = new HashMap<>();
        event.put("id", id);
        event.put("familyId", familyId);
        event.put("title", title);
        event.put("eventType", eventType);
        event.put("startTime", startTime);
        event.put("deadline", deadline);
        event.put("childName", childName);
        event.put("childId", childName != null ? childName.toLowerCase() : null);
        event.put("status", status);
        event.put("description", description);
        event.put("sourceType", "email");
        event.put("created", System.currentTimeMillis());
        event.put("updated", System.currentTimeMillis());
        familyEventsDepot.append(event);
    }

    // =======================================================================
    // FamilySchemaModule: stream topology drains depot into PState
    // =======================================================================

    @Test
    @Order(1)
    void testStreamTopologyDrainsDepotIntoPState() {
        Map<String, Object> events = (Map<String, Object>)
            familyData.selectOne(Path.key(FAMILY_ID).key("events"));

        assertNotNull(events, "Events map should exist for test family");
        assertEquals(6, events.size(), "Should have 6 seeded events");
    }

    @Test
    @Order(2)
    void testEventRecordIntegrity() {
        Map<String, Object> event = (Map<String, Object>)
            familyData.selectOne(Path.key(FAMILY_ID).key("events").key("evt-001"));

        assertNotNull(event, "evt-001 should exist");
        assertEquals("Zoo Field Trip", event.get("title"));
        assertEquals("SCHOOL_EVENT", event.get("eventType"));
        assertEquals(MAR_20, event.get("startTime"));
        assertNull(event.get("deadline"));
        assertEquals("Billy", event.get("childName"));
        assertEquals("pending", event.get("status"));
    }

    @Test
    @Order(3)
    void testUnknownFamilyReturnsNull() {
        Object result = familyData.selectOne(Path.key("nonexistent-family"));
        assertNull(result);
    }

    @Test
    @Order(4)
    void testDepotPartitionHasRecords() {
        var partInfo = familyEventsDepot.getPartitionInfo(0);
        assertTrue(partInfo.getEndOffset() > 0,
            "Depot partition should have records");
    }

    // =======================================================================
    // DigestModule: time-window filtering, sort order, empty windows
    // =======================================================================

    @Test
    @Order(10)
    void testDigestReturnsEventsInWindow() {
        // Window: Mar 17–21 should include evt-002 (deadline Mar 18),
        // evt-004 (startTime Mar 18), evt-001 (startTime Mar 20)
        long windowStart = toEpoch("2026-03-17T00:00:00Z");
        long windowEnd   = toEpoch("2026-03-21T23:59:59Z");

        String digest = (String) digestAgent.invoke(
            new DigestModule.DigestRequest(FAMILY_ID, windowStart, windowEnd));

        assertNotNull(digest);
        assertFalse(digest.isBlank());
        assertTrue(digest.contains("3 items"), "Should find 3 events in window");
        assertTrue(digest.contains("Permission Slip Due"), "Should include permission slip");
        assertTrue(digest.contains("Zoo Field Trip"), "Should include zoo trip");
        assertTrue(digest.contains("Spring Picture Day"), "Should include picture day");
    }

    @Test
    @Order(11)
    void testDigestSortsByChronologicalOrder() {
        long windowStart = toEpoch("2026-03-17T00:00:00Z");
        long windowEnd   = toEpoch("2026-03-21T23:59:59Z");

        String digest = (String) digestAgent.invoke(
            new DigestModule.DigestRequest(FAMILY_ID, windowStart, windowEnd));

        // Permission Slip Due (deadline Mar 18) should appear before
        // Zoo Field Trip (startTime Mar 20)
        int permSlipPos = digest.indexOf("Permission Slip Due");
        int zooPos = digest.indexOf("Zoo Field Trip");
        assertTrue(permSlipPos < zooPos,
            "Permission slip (Mar 18) should appear before zoo trip (Mar 20)");
    }

    @Test
    @Order(12)
    void testDigestEmptyWindowReturnsMessage() {
        // Window far in the future — no events
        long windowStart = toEpoch("2027-01-01T00:00:00Z");
        long windowEnd   = toEpoch("2027-01-31T23:59:59Z");

        String digest = (String) digestAgent.invoke(
            new DigestModule.DigestRequest(FAMILY_ID, windowStart, windowEnd));

        assertNotNull(digest);
        assertTrue(digest.contains("No upcoming events"),
            "Empty window should return 'no events' message");
    }

    @Test
    @Order(13)
    void testDigestNarrowWindowSingleEvent() {
        // Narrow window around Mar 20 — only zoo trip
        long windowStart = toEpoch("2026-03-20T00:00:00Z");
        long windowEnd   = toEpoch("2026-03-20T23:59:59Z");

        String digest = (String) digestAgent.invoke(
            new DigestModule.DigestRequest(FAMILY_ID, windowStart, windowEnd));

        assertTrue(digest.contains("1 item"), "Should find exactly 1 event");
        assertTrue(digest.contains("Zoo Field Trip"));
    }

    @Test
    @Order(14)
    void testDigestFullRangeIncludesAllEvents() {
        // Very wide window — should include all 6 events
        long windowStart = toEpoch("2026-03-01T00:00:00Z");
        long windowEnd   = toEpoch("2026-04-30T23:59:59Z");

        String digest = (String) digestAgent.invoke(
            new DigestModule.DigestRequest(FAMILY_ID, windowStart, windowEnd));

        assertTrue(digest.contains("6 items"), "Should find all 6 events");
    }

    @Test
    @Order(15)
    void testDigestUnknownFamilyReturnsEmptyMessage() {
        long windowStart = toEpoch("2026-03-01T00:00:00Z");
        long windowEnd   = toEpoch("2026-04-30T23:59:59Z");

        String digest = (String) digestAgent.invoke(
            new DigestModule.DigestRequest("nonexistent-family", windowStart, windowEnd));

        assertTrue(digest.contains("No upcoming events"),
            "Unknown family should return 'no events' message");
    }

    @Test
    @Order(16)
    void testDigestShowsEventType() {
        long windowStart = toEpoch("2026-03-17T00:00:00Z");
        long windowEnd   = toEpoch("2026-03-21T23:59:59Z");

        String digest = (String) digestAgent.invoke(
            new DigestModule.DigestRequest(FAMILY_ID, windowStart, windowEnd));

        assertTrue(digest.contains("SCHOOL_EVENT") || digest.contains("PERMISSION_SLIP"),
            "Digest should display event type");
    }

    @Test
    @Order(17)
    void testDigestShowsStatus() {
        long windowStart = toEpoch("2026-03-17T00:00:00Z");
        long windowEnd   = toEpoch("2026-03-21T23:59:59Z");

        String digest = (String) digestAgent.invoke(
            new DigestModule.DigestRequest(FAMILY_ID, windowStart, windowEnd));

        assertTrue(digest.contains("pending") || digest.contains("completed"),
            "Digest should display event status");
    }

    // =======================================================================
    // Serialization round-trips for RamaSerializable classes
    // =======================================================================

    @Test
    @Order(30)
    void testDigestRequestSerialization() throws Exception {
        DigestModule.DigestRequest original =
            new DigestModule.DigestRequest("fam-001", 1000L, 2000L);

        DigestModule.DigestRequest deserialized = roundTrip(original);

        assertEquals(original.familyId, deserialized.familyId);
        assertEquals(original.windowStartMs, deserialized.windowStartMs);
        assertEquals(original.windowEndMs, deserialized.windowEndMs);
    }

    @Test
    @Order(31)
    void testQueryRequestSerialization() throws Exception {
        QueryModule.QueryRequest original =
            new QueryModule.QueryRequest("fam-001", "What's due this week?", "America/New_York");

        QueryModule.QueryRequest deserialized = roundTrip(original);

        assertEquals(original.familyId, deserialized.familyId);
        assertEquals(original.question, deserialized.question);
        assertEquals(original.requesterTimezone, deserialized.requesterTimezone);
    }

    @Test
    @Order(32)
    void testQueryParamsSerialization() throws Exception {
        QueryModule.QueryParams original = new QueryModule.QueryParams(
            "UPCOMING_EVENTS", "Billy", "2026-03-15", "2026-03-31",
            "SCHOOL_EVENT", "What events are coming up for Billy?",
            "fam-001", "America/Los_Angeles");

        QueryModule.QueryParams deserialized = roundTrip(original);

        assertEquals(original.queryType, deserialized.queryType);
        assertEquals(original.childName, deserialized.childName);
        assertEquals(original.dateFrom, deserialized.dateFrom);
        assertEquals(original.dateTo, deserialized.dateTo);
        assertEquals(original.categoryFilter, deserialized.categoryFilter);
        assertEquals(original.originalQuestion, deserialized.originalQuestion);
        assertEquals(original.familyId, deserialized.familyId);
        assertEquals(original.requesterTimezone, deserialized.requesterTimezone);
    }

    @Test
    @Order(33)
    void testParsedEventSerialization() throws Exception {
        EmailParsingModule.ParsedEvent original = new EmailParsingModule.ParsedEvent(
            "SCHOOL_EVENT", "Zoo Trip", "Visit to zoo",
            "2026-03-20T08:30:00Z", "2026-03-18T00:00:00Z",
            "billy", "Billy", "raw email body",
            "teacher@school.edu", "Mr. Jacobs", "Zoo Field Trip",
            "gmail-msg-123", System.currentTimeMillis(), "todd@gmail.com");

        EmailParsingModule.ParsedEvent deserialized = roundTrip(original);

        assertEquals(original.category, deserialized.category);
        assertEquals(original.title, deserialized.title);
        assertEquals(original.startTime, deserialized.startTime);
        assertEquals(original.deadline, deserialized.deadline);
        assertEquals(original.childName, deserialized.childName);
        assertEquals(original.senderEmail, deserialized.senderEmail);
        assertEquals(original.gmailMessageId, deserialized.gmailMessageId);
        assertEquals(original.receivedAt, deserialized.receivedAt);
    }

    @Test
    @Order(34)
    void testQueryRequestDefaultTimezone() {
        QueryModule.QueryRequest req =
            new QueryModule.QueryRequest("fam-001", "test?", null);

        assertEquals("America/Los_Angeles", req.requesterTimezone,
            "Null timezone should default to America/Los_Angeles");
    }

    // =======================================================================
    // Edge cases
    // =======================================================================

    @Test
    @Order(40)
    void testAppendEventWithNullFields() throws Exception {
        // Event with many null fields — should not crash the stream topology
        appendEvent("evt-nulls", FAMILY_ID, null, null,
            null, null, null, null, null);

        Thread.sleep(1000);

        Map<String, Object> event = (Map<String, Object>)
            familyData.selectOne(Path.key(FAMILY_ID).key("events").key("evt-nulls"));

        assertNotNull(event, "Event with null fields should still be stored");
        assertEquals("evt-nulls", event.get("id"));
    }

    @Test
    @Order(41)
    void testDigestHandlesEventsWithOnlyDeadline() {
        // evt-005 has only a deadline (no startTime) — Mar 15
        long windowStart = toEpoch("2026-03-14T00:00:00Z");
        long windowEnd   = toEpoch("2026-03-16T00:00:00Z");

        String digest = (String) digestAgent.invoke(
            new DigestModule.DigestRequest(FAMILY_ID, windowStart, windowEnd));

        assertTrue(digest.contains("Read-a-thon"),
            "Events with only deadline should be included when deadline is in window");
    }

    @Test
    @Order(42)
    void testDigestExcludesEventsWithNoTimeInfo() {
        // evt-nulls has no startTime or deadline — effectiveTime returns Long.MAX_VALUE
        // A normal date range should NOT include it
        long windowStart = toEpoch("2026-03-01T00:00:00Z");
        long windowEnd   = toEpoch("2026-04-30T23:59:59Z");

        String digest = (String) digestAgent.invoke(
            new DigestModule.DigestRequest(FAMILY_ID, windowStart, windowEnd));

        // Should have 6 events (the original 6), not 7
        // evt-nulls has effectiveTime = Long.MAX_VALUE which is outside window
        assertFalse(digest.contains("7 items"),
            "Events with no time info should be excluded from normal date ranges");
    }

    // =======================================================================
    // Helpers
    // =======================================================================

    @SuppressWarnings("unchecked")
    private <T extends Serializable> T roundTrip(T obj) throws Exception {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try (ObjectOutputStream oos = new ObjectOutputStream(bos)) {
            oos.writeObject(obj);
        }
        ByteArrayInputStream bis = new ByteArrayInputStream(bos.toByteArray());
        try (ObjectInputStream ois = new ObjectInputStream(bis)) {
            return (T) ois.readObject();
        }
    }
}
