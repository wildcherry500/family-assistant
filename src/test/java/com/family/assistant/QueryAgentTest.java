package com.family.assistant;

import com.family.assistant.email.EmailIngestionModule;
import com.family.assistant.email.EmailParsingModule;
import com.family.assistant.query.QueryModule;
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
import static org.junit.jupiter.api.Assumptions.assumeTrue;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class QueryAgentTest {

    private static final String FAMILY_ID = "keeling-family-001";
    private static final String TIMEZONE  = "America/Los_Angeles";

    private InProcessCluster ipc;
    private FamilySchemaModule schemaModule;
    private AgentClient queryAgent;

    @BeforeAll
    void setup() throws Exception {
        assumeTrue(
            System.getenv("GEMINI_API_KEY") != null
                && !System.getenv("GEMINI_API_KEY").isBlank(),
            "Skipping — GEMINI_API_KEY not set"
        );

        ipc = InProcessCluster.create();

        schemaModule = new FamilySchemaModule();
        ipc.launchModule(schemaModule, new LaunchConfig(1, 1));

        ipc.launchModule(new EmailParsingModule(),   new LaunchConfig(1, 1));
        ipc.launchModule(new EmailIngestionModule(), new LaunchConfig(1, 1));

        QueryModule queryModule = new QueryModule();
        ipc.launchModule(queryModule, new LaunchConfig(1, 1));

        // Ingest the zoo email
        AgentManager ingestionManager = AgentManager.create(ipc, "EmailIngestionModule");
        AgentClient ingestionAgent = ingestionManager.getAgentClient("email-ingestion-agent");

        EmailIngestionModule.IngestionResult result =
            (EmailIngestionModule.IngestionResult)
                ingestionAgent.invoke(new ArrayList<>(List.of(ZooEmailTest.ZOO_EMAIL)));

        assumeTrue(result.failed == 0,
            "Skipping — email ingestion failed (Gemini rate limit or quota exhausted)");
        assumeTrue(result.eventIds.size() >= 1,
            "Skipping — ingestion returned 0 events");

        // Wait for the stream topology to write the event into $$family-data
        PState ps = ipc.clusterPState(schemaModule.getModuleName(), "$$family-data");
        long deadline = System.currentTimeMillis() + 30_000;
        while (System.currentTimeMillis() < deadline) {
            Map<?, ?> events = (Map<?, ?>) ps.selectOne(Path.key(FAMILY_ID).key("events"));
            if (events != null && !events.isEmpty()) break;
            Thread.sleep(500);
        }

        AgentManager queryManager = AgentManager.create(ipc, queryModule.getModuleName());
        queryAgent = queryManager.getAgentClient("query-agent");
    }

    @AfterAll
    void teardown() throws Exception {
        if (ipc != null) ipc.close();
    }

    @Test
    @Tag("llm")
    @Order(1)
    void testWhatDoesBillyNeedForFieldTrip() throws Exception {
        ask("What does Billy need for the field trip?");
    }

    @Test
    @Tag("llm")
    @Order(2)
    void testWhenIsNextPermissionSlipDue() throws Exception {
        ask("When is the next permission slip due?");
    }

    @Test
    @Tag("llm")
    @Order(3)
    void testWhatIsHappeningOnMarch20() throws Exception {
        ask("What is happening on March 20th?");
    }

    @Test
    @Tag("llm")
    @Order(4)
    void testDoINeedToPickUpBilly() throws Exception {
        ask("Do I need to pick up Billy from school?");
    }

    // -----------------------------------------------------------------------
    // Helper
    // -----------------------------------------------------------------------

    private void ask(String question) {
        QueryModule.QueryRequest request =
            new QueryModule.QueryRequest(FAMILY_ID, question, TIMEZONE);

        String answer = (String) queryAgent.invoke(request);

        System.out.println("\n--- Q: " + question);
        System.out.println("    A: " + answer);

        assertNotNull(answer, "Answer should not be null");
        assertFalse(answer.isBlank(), "Answer should not be blank");
    }
}
