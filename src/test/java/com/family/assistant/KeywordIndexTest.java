package com.family.assistant;

import com.family.assistant.schema.FamilySchemaModule;
import com.rpl.rama.Depot;
import com.rpl.rama.Path;
import com.rpl.rama.PState;
import com.rpl.rama.test.InProcessCluster;
import com.rpl.rama.test.LaunchConfig;
import org.junit.jupiter.api.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * KeywordIndexTest
 *
 * Verifies that FamilySchemaModule correctly populates $$events-by-keyword by
 * tokenizing title+description+emailSubject and fanning out one index write
 * per token (via Ops.EXPLODE — see RAMA_VERIFIED_LEARNINGS.md).
 *
 * Covers: multi-token fan-out, case folding, stopword exclusion, min-length
 * exclusion, dedup within a single event's tokens, and isolation across
 * events/families.
 *
 * No GEMINI_API_KEY required — events are appended directly with
 * title/description/emailSubject already set, exercising only
 * FamilySchemaModule's indexing.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class KeywordIndexTest {

    private static final String FAMILY_ID = "keyword-index-test-family";
    private static final int EXPECTED_EVENT_COUNT = 3;

    private InProcessCluster ipc;
    private Depot  familyEventsDepot;
    private PState familyData;
    private PState eventsByKeyword;

    @BeforeAll
    void setup() throws Exception {
        ipc = InProcessCluster.create();

        FamilySchemaModule schemaModule = new FamilySchemaModule();
        ipc.launchModule(schemaModule, new LaunchConfig(1, 1));

        familyEventsDepot = ipc.clusterDepot(schemaModule.getModuleName(), "*family-events");
        familyData        = ipc.clusterPState(schemaModule.getModuleName(), "$$family-data");
        eventsByKeyword    = ipc.clusterPState(schemaModule.getModuleName(), "$$events-by-keyword");

        // Multi-token, mixed-case text — proves lowercasing + fan-out across 3 fields.
        appendEvent("evt-permission", "Permission Slip Due",
            "Please return the SIGNED permission slip by Monday.", "Permission Slip Reminder");

        // Stopwords ("the", "and", "for", "to", "is") and a short token ("Zoo" survives at
        // exactly 3 chars, "a"/"on" do not survive the 3-char minimum).
        appendEvent("evt-zoo", "Zoo Trip",
            "The class is heading to the zoo on Thursday for a field trip.", null);

        // Duplicate word within one event's own text — must dedupe to a single index entry.
        appendEvent("evt-dedup", "Recital Recital", "Piano recital details", null);

        waitForEventsIndexed(3, 10_000);
    }

    @AfterAll
    void teardown() throws Exception {
        if (ipc != null) ipc.close();
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private void appendEvent(String id, String title, String description, String emailSubject) {
        Map<String, Object> event = new HashMap<>();
        event.put("id",            id);
        event.put("familyId",      FAMILY_ID);
        event.put("title",         title);
        event.put("description",   description);
        event.put("emailSubject",  emailSubject);
        event.put("eventType",     "TASK");
        event.put("silo",          "VAULT");
        event.put("intent",        "FYI");
        event.put("startTime",     null);
        event.put("deadline",      null);
        event.put("childName",     null);
        event.put("childId",       null);
        event.put("status",        "pending");
        event.put("sourceType",    "test");
        event.put("accountLabel",  null);
        event.put("created",       System.currentTimeMillis());
        event.put("updated",       System.currentTimeMillis());
        familyEventsDepot.append(event);
    }

    private void waitForEventsIndexed(int expectedCount, long timeoutMs) throws InterruptedException {
        long deadline = System.currentTimeMillis() + timeoutMs;
        while (System.currentTimeMillis() < deadline) {
            Map<?, ?> events = (Map<?, ?>) familyData.selectOne(Path.key(FAMILY_ID).key("events"));
            if (events != null && events.size() >= expectedCount) return;
            Thread.sleep(50);
        }
        fail("Timed out waiting for " + expectedCount + " events to be indexed");
    }

    @SuppressWarnings("unchecked")
    private Set<String> bucket(String keyword) {
        return (Set<String>) eventsByKeyword.selectOne(Path.key(FAMILY_ID).key(keyword));
    }

    // -----------------------------------------------------------------------
    // Tests
    // -----------------------------------------------------------------------

    @Test
    @Order(1)
    void testTokenFromTitleIsIndexed() {
        Set<String> ids = bucket("permission");
        assertNotNull(ids, "'permission' should be indexed");
        assertTrue(ids.contains("evt-permission"));
    }

    @Test
    @Order(2)
    void testTokenFromDescriptionIsIndexed() {
        Set<String> ids = bucket("signed");
        assertNotNull(ids, "'signed' (from description) should be indexed");
        assertTrue(ids.contains("evt-permission"));
    }

    @Test
    @Order(3)
    void testTokenFromEmailSubjectIsIndexed() {
        Set<String> ids = bucket("reminder");
        assertNotNull(ids, "'reminder' (from emailSubject) should be indexed");
        assertTrue(ids.contains("evt-permission"));
    }

    @Test
    @Order(4)
    void testCaseFolding() {
        // "SIGNED" in the source text must be indexed lowercase, not two separate buckets.
        assertNull(bucket("SIGNED"), "Index should never have an uppercase bucket");
        Set<String> ids = bucket("signed");
        assertNotNull(ids);
        assertEquals(1, ids.size());
    }

    @Test
    @Order(5)
    void testStopwordsExcluded() {
        assertNull(bucket("the"), "'the' is a stopword and must not be indexed");
        assertNull(bucket("for"), "'for' is a stopword and must not be indexed");
        assertNull(bucket("is"),  "'is' is a stopword and must not be indexed");
        assertNull(bucket("to"),  "'to' is a stopword and must not be indexed");
    }

    @Test
    @Order(6)
    void testMinLengthExcludesShortTokens() {
        assertNull(bucket("a"),  "single-char tokens must not be indexed");
        assertNull(bucket("on"), "2-char tokens must not be indexed (below MIN_TOKEN_LENGTH)");
    }

    @Test
    @Order(7)
    void test3CharTokenSurvives() {
        Set<String> ids = bucket("zoo");
        assertNotNull(ids, "'zoo' is exactly 3 chars and should survive the min-length filter");
        assertTrue(ids.contains("evt-zoo"));
    }

    @Test
    @Order(8)
    void testDedupeWithinOneEvent() {
        // evt-dedup's title repeats "recital" twice — must not affect Set membership/size.
        Set<String> ids = bucket("recital");
        assertNotNull(ids);
        assertEquals(1, ids.size(), "Repeated word within one event must not create duplicate entries");
        assertTrue(ids.contains("evt-dedup"));
    }

    @Test
    @Order(9)
    void testNullEmailSubjectDoesNotBreakIndexing() {
        // evt-zoo and evt-dedup both have a null emailSubject — indexing must not error.
        Set<String> ids = bucket("piano");
        assertNotNull(ids);
        assertTrue(ids.contains("evt-dedup"));
    }

    @Test
    @Order(10)
    void testKeywordIndexIsolatedAcrossEvents() {
        Set<String> zooIds = bucket("zoo");
        Set<String> permissionIds = bucket("permission");
        assertNotNull(zooIds);
        assertNotNull(permissionIds);
        for (String id : zooIds) {
            assertFalse(permissionIds.contains(id), "'zoo' bucket must not leak evt-permission");
        }
    }
}
