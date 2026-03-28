package com.family.assistant;

import com.family.assistant.digest.DigestModule;
import com.family.assistant.email.EmailIngestionModule;
import com.family.assistant.email.EmailParsingModule;
import com.family.assistant.schema.FamilySchemaModule;
import com.rpl.agentorama.AgentClient;
import com.rpl.agentorama.AgentManager;
import com.rpl.rama.Path;
import com.rpl.rama.PState;
import com.rpl.rama.test.InProcessCluster;
import com.rpl.rama.test.LaunchConfig;
import org.junit.jupiter.api.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ZooEmailTest
 *
 * Feeds the real Mr. Jacobs zoo trip email through the full pipeline
 * and prints what Gemini extracted — category, title, dates, items.
 *
 * Run with: GEMINI_API_KEY=yourkey mvn test -Dtest=ZooEmailTest
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class ZooEmailTest {

    private static final String FAMILY_ID = "keeling-family-001";

    private InProcessCluster ipc;
    private FamilySchemaModule schemaModule;
    private AgentClient ingestionAgent;
    private AgentClient digestAgent;

    static final String ZOO_EMAIL =
        "Tor Keeling\n" +
        "9:29 AM\n" +
        "to lawnchair60@gmail.com, me\n\n" +
        "Dear Families,\n" +
        "I hope this note finds you well! Just a quick heads-up that our spring " +
        "read-a-thon pledge forms are still due — if you haven't turned those in " +
        "yet please do so by end of week, we're so close to our class goal!\n\n" +
        "Now, on to the exciting news — our 3rd grade class will be heading to the " +
        "Woodland Park Zoo on Thursday, March 20th! We've been looking forward to " +
        "this all semester and the kids are going to love the new African Savanna exhibit.\n\n" +
        "Permission Slip Please make sure your child's signed permission slip is " +
        "returned to me no later than Monday, March 16th. Slips can be sent in your " +
        "child's folder or dropped off at the front office. We cannot allow students " +
        "to attend without a completed form — no exceptions, per district policy.\n\n" +
        "Lunch This will be a full-day trip. Students should bring a sack lunch and " +
        "a water bottle. Please be advised that we have students in our class with " +
        "severe peanut allergies — all lunches must be completely peanut-free. This " +
        "includes peanut butter, trail mix with peanuts, and some granola bars. " +
        "When in doubt, check the label!\n\n" +
        "Dismissal — Important Due to the timing of our return, bus service will NOT " +
        "be available on March 20th. All students must be picked up directly from " +
        "Lockwood Elementary by 4:00 PM. If someone other than a parent or guardian " +
        "will be picking up your child, please send a note or email me in advance.\n\n" +
        "A few housekeeping items: our classroom volunteer sign-up for April is now " +
        "open, and don't forget that Spring Picture Day is March 18th — two days " +
        "before the trip, so your kids will look great in their zoo photos!\n\n" +
        "If you have any questions don't hesitate to reach out. Looking forward to " +
        "a wonderful day with your kids! Warm regards, Mr. R. Jacobs " +
        "3rd Grade — Lockwood Elementary Todd Jaco, Lockwood Elementary " +
        "todd@brandacer.com";

    @BeforeAll
    void setup() throws Exception {
        ipc = InProcessCluster.create();

        schemaModule = new FamilySchemaModule();
        ipc.launchModule(schemaModule, new LaunchConfig(1, 1));

        EmailParsingModule emailModule = new EmailParsingModule();
        ipc.launchModule(emailModule, new LaunchConfig(1, 1));

        EmailIngestionModule ingestionModule = new EmailIngestionModule();
        ipc.launchModule(ingestionModule, new LaunchConfig(1, 1));

        DigestModule digestModule = new DigestModule();
        ipc.launchModule(digestModule, new LaunchConfig(1, 1));

        AgentManager ingestionManager = AgentManager.create(ipc, ingestionModule.getModuleName());
        ingestionAgent = ingestionManager.getAgentClient("email-ingestion-agent");

        AgentManager digestManager = AgentManager.create(ipc, digestModule.getModuleName());
        digestAgent = digestManager.getAgentClient("digest-agent");
    }

    @AfterAll
    void teardown() throws Exception {
        if (ipc != null) ipc.close();
    }

    // -----------------------------------------------------------------------
    // Test: feed zoo email, print what Gemini extracted
    // -----------------------------------------------------------------------
    @Test
    @org.junit.jupiter.api.Tag("llm")
    @org.junit.jupiter.api.Order(1)
    void testZooEmailExtraction() throws Exception {
        System.out.println("KEY=" + System.getenv("GEMINI_API_KEY"));
        String apiKey = System.getenv("GEMINI_API_KEY");
        assumeGeminiKey(apiKey);

        // Ingest the zoo email
        EmailIngestionModule.IngestionResult result =
            (EmailIngestionModule.IngestionResult)
                ingestionAgent.invoke(new ArrayList<>(List.of(ZOO_EMAIL)));

        System.out.println("\n=== INGESTION RESULT ===");
        System.out.println(result);

        assertEquals(1, result.eventIds.size(), "Should parse 1 email");
        assertEquals(0, result.failed,  "Should have 0 failures");

        com.rpl.rama.Depot familyEventsDepot = ipc.clusterDepot(schemaModule.getModuleName(), "*family-events");
        com.rpl.rama.object.DepotPartitionInfo partInfo = familyEventsDepot.getPartitionInfo(0);
        System.out.println("\n=== DEPOT PARTITION INFO (*family-events, partition 0) ===");
        System.out.println("  startOffset=" + partInfo.getStartOffset() + "  endOffset=" + partInfo.getEndOffset());

        // Poll PState until events appear or 30s timeout
        PState ps = ipc.clusterPState(schemaModule.getModuleName(), "$$family-data");
        Map<String, Object> events = null;
        long deadline = System.currentTimeMillis() + 30_000;
        while (System.currentTimeMillis() < deadline) {
            events = (Map<String, Object>) ps.selectOne(Path.key(FAMILY_ID).key("events"));
            if (events != null && !events.isEmpty()) break;
            Thread.sleep(500);
        }

        System.out.println("\n=== EVENTS IN $family-data ===");
        if (events == null || events.isEmpty()) {
            System.out.println("  (no events found — familyId may not match)");
        } else {
            for (Map.Entry<String, Object> entry : events.entrySet()) {
                System.out.println("\nEvent ID: " + entry.getKey());
                Map<String, Object> event = (Map<String, Object>) entry.getValue();
                event.forEach((k, v) ->
                    System.out.printf("  %-15s: %s%n", k, v));
            }
        }
    }

    // -----------------------------------------------------------------------
    // Test: digest after zoo email ingestion
    // -----------------------------------------------------------------------
    @Test
    @org.junit.jupiter.api.Tag("llm")
    @org.junit.jupiter.api.Order(2)
    void testDigestAfterZooEmail() throws Exception {
        String apiKey = System.getenv("GEMINI_API_KEY");
        assumeGeminiKey(apiKey);

        // Wait for microbatch
        Thread.sleep(3000);

        // Print startTime from $$family-data to compare against the window
        PState ps = ipc.clusterPState(schemaModule.getModuleName(), "$$family-data");
        Map<String, Object> events = (Map<String, Object>) ps.selectOne(Path.key(FAMILY_ID).key("events"));
        System.out.println("\n=== EVENTS BEFORE DIGEST ===");
        if (events == null || events.isEmpty()) {
            System.out.println("  (no events found)");
        } else {
            for (Map.Entry<String, Object> entry : events.entrySet()) {
                Map<String, Object> ev = (Map<String, Object>) entry.getValue();
                String startTime = (String) ev.get("startTime");
                String deadline  = (String) ev.get("deadline");
                long startEpoch = startTime != null
                    ? java.time.Instant.parse(startTime.length() == 19 ? startTime + "Z" : startTime).toEpochMilli()
                    : -1;
                long deadlineEpoch = deadline != null
                    ? java.time.Instant.parse(deadline.length() == 19 ? deadline + "Z" : deadline).toEpochMilli()
                    : -1;
                System.out.printf("  startTime=%-25s  epochMs=%d%n", startTime, startEpoch);
                System.out.printf("  deadline =%-25s  epochMs=%d%n", deadline, deadlineEpoch);
            }
        }

        // Request digest for March 2026 window
        long windowStart = 1773273600000L; // Mar 12 2026 UTC
        long windowEnd   = 1774828800000L; // Mar 30 2026 UTC
        System.out.printf("%nWindow:  start=%d  end=%d%n", windowStart, windowEnd);

        DigestModule.DigestRequest request =
            new DigestModule.DigestRequest(FAMILY_ID, windowStart, windowEnd);

        String digest = (String) digestAgent.invoke(request);

        System.out.println("\n=== DIGEST OUTPUT ===");
        System.out.println(digest);

        assertNotNull(digest);
        assertFalse(digest.isBlank());
    }

    private void assumeGeminiKey(String key) {
        org.junit.jupiter.api.Assumptions.assumeTrue(
            key != null && !key.isBlank(),
            "Skipping — GEMINI_API_KEY not set"
        );
    }
}