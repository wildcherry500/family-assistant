package com.family.assistant;

import com.family.assistant.schema.FamilySchemaModule;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rpl.rama.Depot;
import com.rpl.rama.Path;
import com.rpl.rama.PState;
import com.rpl.rama.test.InProcessCluster;
import com.rpl.rama.test.LaunchConfig;
import org.junit.jupiter.api.*;

import java.io.InputStream;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * CohenFamilyDatasetTest
 *
 * Loads cohen_family_test_dataset_complete.json from src/test/resources/,
 * seeds all email-channel communications directly into the *family-events depot
 * via InProcessCluster (no LLM), and verifies PState integrity.
 *
 * No @Tag("llm") — runs in default mvn test without GEMINI_API_KEY.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class CohenFamilyDatasetTest {

    private static final String FAMILY_ID = "cohen-family-001";

    // Dataset window: Feb 10 – Mar 2, 2026 (Pacific time)
    private static final long FEB_10_START =
        OffsetDateTime.parse("2026-02-10T00:00:00-08:00").toInstant().toEpochMilli();
    private static final long MAR_02_END =
        OffsetDateTime.parse("2026-03-02T23:59:59-08:00").toInstant().toEpochMilli();

    private InProcessCluster ipc;
    private Depot familyEventsDepot;
    private PState familyData;
    private PState eventsByChild;
    private PState eventsByCategory;

    private final List<String> seededEventIds = new ArrayList<>();
    private int expectedEmailCount = 0;

    @BeforeAll
    void setup() throws Exception {
        ipc = InProcessCluster.create();

        FamilySchemaModule schemaModule = new FamilySchemaModule();
        ipc.launchModule(schemaModule, new LaunchConfig(1, 1));

        familyEventsDepot = ipc.clusterDepot(schemaModule.getModuleName(), "*family-events");
        familyData        = ipc.clusterPState(schemaModule.getModuleName(), "$$family-data");
        eventsByChild     = ipc.clusterPState(schemaModule.getModuleName(), "$$events-by-child");
        eventsByCategory  = ipc.clusterPState(schemaModule.getModuleName(), "$$events-by-category");

        seedFromDataset();
        Thread.sleep(2500);
    }

    @AfterAll
    void teardown() throws Exception {
        if (ipc != null) ipc.close();
    }

    @SuppressWarnings("unchecked")
    private void seedFromDataset() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        InputStream is = getClass().getClassLoader()
            .getResourceAsStream("cohen_family_test_dataset_complete.json");
        assertNotNull(is, "cohen_family_test_dataset_complete.json must be on classpath");

        Map<String, Object> dataset = mapper.readValue(is, Map.class);
        List<Map<String, Object>> communications =
            (List<Map<String, Object>>) dataset.get("communications");

        for (Map<String, Object> msg : communications) {
            String channel = (String) msg.get("channel");
            if (!"email".equals(channel)) continue;

            String  messageId  = (String)  msg.get("message_id");
            String  subject    = (String)  msg.get("subject");
            String  body       = (String)  msg.get("body");
            String  category   = (String)  msg.get("category");
            String  source     = (String)  msg.get("source");
            String  childField = (String)  msg.get("child");
            String  urgency    = (String)  msg.get("urgency");
            Boolean actionReq  = (Boolean) msg.get("action_required");
            String  tsStr      = (String)  msg.get("timestamp");
            String  dlStr      = (String)  msg.get("action_deadline");

            Long startTime = tsStr != null
                ? OffsetDateTime.parse(tsStr).toInstant().toEpochMilli() : null;
            Long deadline  = dlStr != null
                ? OffsetDateTime.parse(dlStr).toInstant().toEpochMilli() : null;

            String eventType = mapEventType(category, source, actionReq);
            String childName = mapChildName(childField);
            long   now       = System.currentTimeMillis();

            Map<String, Object> record = new HashMap<>();
            record.put("id",          messageId);
            record.put("familyId",    FAMILY_ID);
            record.put("title",       subject);
            record.put("description", body);
            record.put("eventType",   eventType);
            record.put("childName",   childName);
            record.put("childId",     childName != null ? childName.toLowerCase() : null);
            record.put("startTime",   startTime);
            record.put("deadline",    deadline);
            record.put("urgency",     urgency);
            record.put("status",      "pending");
            record.put("sourceType",  "email");
            record.put("sourceLabel", source);
            record.put("created",     now);
            record.put("updated",     now);

            familyEventsDepot.append(record);
            seededEventIds.add(messageId);
        }
        expectedEmailCount = seededEventIds.size();
    }

    /**
     * Maps JSON category + source + action_required → eventType used by FamilySchemaModule indexes.
     *
     * "administrative" + school source + action_required → PERMISSION_SLIP
     * "administrative" | "academic" | "sports" | "social" → SCHOOL_EVENT
     * "work"                                              → TASK
     * anything else (e.g. "religious")                   → UNKNOWN
     */
    private static String mapEventType(String category, String source, Boolean actionRequired) {
        if (category == null) return "UNKNOWN";
        boolean schoolSource = source != null && source.startsWith("school");
        switch (category) {
            case "administrative":
                return (Boolean.TRUE.equals(actionRequired) && schoolSource)
                    ? "PERMISSION_SLIP" : "SCHOOL_EVENT";
            case "academic":
            case "sports":
            case "social":
                return "SCHOOL_EVENT";
            case "work":
                return "TASK";
            default:
                return "UNKNOWN";
        }
    }

    /**
     * Maps JSON "child" field → childName stored in the record.
     * "Both" and "None" are not indexable child names → null.
     */
    private static String mapChildName(String childField) {
        if ("Maya".equals(childField) || "Josh".equals(childField)) return childField;
        return null;
    }

    // =========================================================================
    // 1. Dataset loads and seeds the correct number of email records
    // =========================================================================

    @Test @Order(1)
    void testEmailEventCount() {
        assertEquals(13, expectedEmailCount,
            "Dataset statistics.by_channel.email = 13; should seed exactly 13 email records");
    }

    // =========================================================================
    // 2. $$family-data contains all seeded events
    // =========================================================================

    @Test @Order(2)
    @SuppressWarnings("unchecked")
    void testFamilyDataContainsAllEmailEvents() {
        Map<String, Object> events = (Map<String, Object>)
            familyData.selectOne(Path.key(FAMILY_ID).key("events"));

        assertNotNull(events, "$$family-data should have an events map for cohen-family-001");
        assertEquals(expectedEmailCount, events.size(),
            "All 13 seeded email events should be present in $$family-data");
    }

    @Test @Order(3)
    @SuppressWarnings("unchecked")
    void testKnownEventRecordIntegrity() {
        // email_0004: IEP meeting — critical urgency, Maya, action_required=true, school source
        Map<String, Object> record = (Map<String, Object>)
            familyData.selectOne(Path.key(FAMILY_ID).key("events").key("email_0004"));

        assertNotNull(record, "email_0004 (IEP Annual Review) should exist in $$family-data");
        assertTrue(((String) record.get("title")).contains("IEP"),
            "Title should contain 'IEP'");
        assertEquals("PERMISSION_SLIP", record.get("eventType"),
            "IEP meeting (admin + school_elementary + action_required) → PERMISSION_SLIP");
        assertEquals("Maya",     record.get("childName"));
        assertEquals("critical", record.get("urgency"));
        assertNotNull(record.get("startTime"), "startTime should be parsed from timestamp field");
        assertNotNull(record.get("deadline"),  "deadline should be parsed from action_deadline field");
    }

    @Test @Order(4)
    @SuppressWarnings("unchecked")
    void testAllSeededIdsAreRetrievable() {
        for (String id : seededEventIds) {
            Object record = familyData.selectOne(
                Path.key(FAMILY_ID).key("events").key(id));
            assertNotNull(record, "Event " + id + " should be retrievable from $$family-data");
        }
    }

    // =========================================================================
    // 3. $$events-by-child — Maya and Josh both indexed
    // =========================================================================

    @Test @Order(10)
    @SuppressWarnings("unchecked")
    void testEventsByChildContainsMaya() {
        Set<String> mayaEvents = (Set<String>)
            eventsByChild.selectOne(Path.key(FAMILY_ID).key("Maya"));

        assertNotNull(mayaEvents, "$$events-by-child should have a 'Maya' entry");
        assertFalse(mayaEvents.isEmpty(), "Maya's event set should be non-empty");
        assertTrue(mayaEvents.contains("email_0002"),
            "email_0002 (weekly newsletter, child=Maya) should be in Maya's set");
        assertTrue(mayaEvents.contains("email_0004"),
            "email_0004 (IEP meeting, child=Maya) should be in Maya's set");
        assertTrue(mayaEvents.contains("email_0003"),
            "email_0003 (learning goals, child=Maya) should be in Maya's set");
    }

    @Test @Order(11)
    @SuppressWarnings("unchecked")
    void testEventsByChildContainsJosh() {
        Set<String> joshEvents = (Set<String>)
            eventsByChild.selectOne(Path.key(FAMILY_ID).key("Josh"));

        assertNotNull(joshEvents, "$$events-by-child should have a 'Josh' entry");
        assertFalse(joshEvents.isEmpty(), "Josh's event set should be non-empty");
        assertTrue(joshEvents.contains("email_0005"),
            "email_0005 (graduation deadlines, child=Josh) should be in Josh's set");
        assertTrue(joshEvents.contains("email_0006"),
            "email_0006 (basketball schedule, child=Josh) should be in Josh's set");
        assertTrue(joshEvents.contains("email_0014"),
            "email_0014 (winter concert, child=Josh) should be in Josh's set");
    }

    @Test @Order(12)
    void testBothAndNoneNotIndexedAsChildNames() {
        // "Both" and "None" → childName=null → must not appear as keys in the index
        Object bothEntry = eventsByChild.selectOne(Path.key(FAMILY_ID).key("Both"));
        Object noneEntry = eventsByChild.selectOne(Path.key(FAMILY_ID).key("None"));

        assertNull(bothEntry, "'Both' must not be a key in $$events-by-child");
        assertNull(noneEntry, "'None' must not be a key in $$events-by-child");
    }

    @Test @Order(13)
    @SuppressWarnings("unchecked")
    void testMayaAndJoshEventCountsMatchDataset() {
        // Dataset statistics: Maya=8 (all channels). Email-only: 5 Maya, 4 Josh.
        Set<String> mayaEvents = (Set<String>)
            eventsByChild.selectOne(Path.key(FAMILY_ID).key("Maya"));
        Set<String> joshEvents = (Set<String>)
            eventsByChild.selectOne(Path.key(FAMILY_ID).key("Josh"));

        assertEquals(5, mayaEvents.size(), "5 email records have child=Maya");
        assertEquals(4, joshEvents.size(), "4 email records have child=Josh");
    }

    // =========================================================================
    // 4. $$events-by-category — SCHOOL_EVENT, PERMISSION_SLIP, TASK, UNKNOWN
    // =========================================================================

    @Test @Order(20)
    @SuppressWarnings("unchecked")
    void testEventsByCategoryContainsSchoolEvent() {
        Set<String> schoolEvents = (Set<String>)
            eventsByCategory.selectOne(Path.key(FAMILY_ID).key("SCHOOL_EVENT"));

        assertNotNull(schoolEvents, "SCHOOL_EVENT should exist in $$events-by-category");
        assertFalse(schoolEvents.isEmpty(), "SCHOOL_EVENT set should be non-empty");
        assertTrue(schoolEvents.contains("email_0001"),
            "email_0001 (Mid-Winter Break reminder, admin+school_district) → SCHOOL_EVENT");
        assertTrue(schoolEvents.contains("email_0003"),
            "email_0003 (learning goals, academic+school_elementary) → SCHOOL_EVENT");
    }

    @Test @Order(21)
    @SuppressWarnings("unchecked")
    void testEventsByCategoryContainsPermissionSlip() {
        Set<String> permSlipEvents = (Set<String>)
            eventsByCategory.selectOne(Path.key(FAMILY_ID).key("PERMISSION_SLIP"));

        assertNotNull(permSlipEvents, "PERMISSION_SLIP should exist in $$events-by-category");
        assertFalse(permSlipEvents.isEmpty(), "PERMISSION_SLIP set should be non-empty");
        assertTrue(permSlipEvents.contains("email_0004"),
            "email_0004 (IEP meeting, admin+school+action_required) → PERMISSION_SLIP");
        assertTrue(permSlipEvents.contains("email_0002"),
            "email_0002 (newsletter w/ PT conf signup, admin+school+action_required) → PERMISSION_SLIP");
        assertTrue(permSlipEvents.contains("email_0005"),
            "email_0005 (graduation deadlines, admin+school_high+action_required) → PERMISSION_SLIP");
        assertEquals(3, permSlipEvents.size(), "Exactly 3 emails map to PERMISSION_SLIP");
    }

    @Test @Order(22)
    @SuppressWarnings("unchecked")
    void testEventsByCategoryContainsTask() {
        Set<String> taskEvents = (Set<String>)
            eventsByCategory.selectOne(Path.key(FAMILY_ID).key("TASK"));

        assertNotNull(taskEvents, "TASK should exist in $$events-by-category");
        assertTrue(taskEvents.contains("email_0009"),
            "email_0009 (shift swap, work source) → TASK");
        assertTrue(taskEvents.contains("email_0017"),
            "email_0017 (Boeing travel approval, work source) → TASK");
        assertEquals(2, taskEvents.size(), "Exactly 2 emails map to TASK");
    }

    @Test @Order(23)
    @SuppressWarnings("unchecked")
    void testEventsByCategoryContainsUnknown() {
        Set<String> unknownEvents = (Set<String>)
            eventsByCategory.selectOne(Path.key(FAMILY_ID).key("UNKNOWN"));

        assertNotNull(unknownEvents, "UNKNOWN should exist in $$events-by-category (religious emails)");
        assertTrue(unknownEvents.contains("email_0010"),
            "email_0010 (Shabbat Shalom, religious) → UNKNOWN");
        assertTrue(unknownEvents.contains("email_0018"),
            "email_0018 (Hebrew School, religious) → UNKNOWN");
    }

    @Test @Order(24)
    @SuppressWarnings("unchecked")
    void testCategoryCountsSumToTotal() {
        Set<String> school   = (Set<String>) eventsByCategory.selectOne(Path.key(FAMILY_ID).key("SCHOOL_EVENT"));
        Set<String> permSlip = (Set<String>) eventsByCategory.selectOne(Path.key(FAMILY_ID).key("PERMISSION_SLIP"));
        Set<String> task     = (Set<String>) eventsByCategory.selectOne(Path.key(FAMILY_ID).key("TASK"));
        Set<String> unknown  = (Set<String>) eventsByCategory.selectOne(Path.key(FAMILY_ID).key("UNKNOWN"));

        int total = school.size() + permSlip.size() + task.size() + unknown.size();
        assertEquals(expectedEmailCount, total,
            "Category counts should sum to " + expectedEmailCount + " (no event double-counted)");
    }

    // =========================================================================
    // 5. Date filtering — events in the Feb 10–Mar 2 window
    // =========================================================================

    @Test @Order(30)
    @SuppressWarnings("unchecked")
    void testAllEventsHaveStartTimeSet() {
        Map<String, Object> events = (Map<String, Object>)
            familyData.selectOne(Path.key(FAMILY_ID).key("events"));
        assertNotNull(events);

        long missing = events.values().stream()
            .map(v -> (Map<String, Object>) v)
            .filter(r -> r.get("startTime") == null)
            .count();

        assertEquals(0, missing,
            "All email records have a 'timestamp' field; startTime should always be set");
    }

    @Test @Order(31)
    @SuppressWarnings("unchecked")
    void testAllEventsStartTimeWithinDatasetWindow() {
        Map<String, Object> events = (Map<String, Object>)
            familyData.selectOne(Path.key(FAMILY_ID).key("events"));
        assertNotNull(events);

        long inWindow = events.values().stream()
            .map(v -> (Map<String, Object>) v)
            .filter(r -> {
                Long startTime = (Long) r.get("startTime");
                return startTime != null
                    && startTime >= FEB_10_START
                    && startTime <= MAR_02_END;
            })
            .count();

        assertEquals(expectedEmailCount, inWindow,
            "All 13 email timestamps fall within the dataset window Feb 10–Mar 2");
    }

    @Test @Order(32)
    @SuppressWarnings("unchecked")
    void testNoEventsOutsideDatasetWindow() {
        Map<String, Object> events = (Map<String, Object>)
            familyData.selectOne(Path.key(FAMILY_ID).key("events"));
        assertNotNull(events);

        List<String> outsideWindow = events.values().stream()
            .map(v -> (Map<String, Object>) v)
            .filter(r -> {
                Long startTime = (Long) r.get("startTime");
                return startTime != null
                    && (startTime < FEB_10_START || startTime > MAR_02_END);
            })
            .map(r -> (String) r.get("id"))
            .collect(Collectors.toList());

        assertTrue(outsideWindow.isEmpty(),
            "No email events should have startTime outside Feb 10–Mar 2; found: " + outsideWindow);
    }

    // =========================================================================
    // 6. Urgency field preserved in stored records
    // =========================================================================

    @Test @Order(40)
    @SuppressWarnings("unchecked")
    void testUrgencyFieldStoredInRecord() {
        Map<String, Object> record = (Map<String, Object>)
            familyData.selectOne(Path.key(FAMILY_ID).key("events").key("email_0004"));

        assertNotNull(record);
        assertEquals("critical", record.get("urgency"),
            "email_0004 urgency should be 'critical'");
    }

    @Test @Order(41)
    @SuppressWarnings("unchecked")
    void testAllFourUrgencyLevelsPresent() {
        Map<String, Object> events = (Map<String, Object>)
            familyData.selectOne(Path.key(FAMILY_ID).key("events"));
        assertNotNull(events);

        Set<String> urgencies = events.values().stream()
            .map(v -> (String) ((Map<String, Object>) v).get("urgency"))
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());

        assertTrue(urgencies.contains("critical"), "Should have at least one 'critical' email");
        assertTrue(urgencies.contains("high"),     "Should have at least one 'high' email");
        assertTrue(urgencies.contains("medium"),   "Should have at least one 'medium' email");
        assertTrue(urgencies.contains("low"),      "Should have at least one 'low' email");
    }

    @Test @Order(42)
    @SuppressWarnings("unchecked")
    void testCriticalUrgencyEventIsMayaIEP() {
        // Only email_0004 has urgency=critical in the email channel
        Map<String, Object> events = (Map<String, Object>)
            familyData.selectOne(Path.key(FAMILY_ID).key("events"));
        assertNotNull(events);

        List<String> criticalIds = events.values().stream()
            .map(v -> (Map<String, Object>) v)
            .filter(r -> "critical".equals(r.get("urgency")))
            .map(r -> (String) r.get("id"))
            .collect(Collectors.toList());

        assertEquals(1, criticalIds.size(), "Exactly 1 email has urgency=critical");
        assertEquals("email_0004", criticalIds.get(0),
            "The critical email should be email_0004 (IEP Annual Review)");
    }
}
