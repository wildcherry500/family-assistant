package com.family.assistant;

import com.family.assistant.PersonalAssistantConfig;
import com.family.assistant.digest.DigestModule;
import com.family.assistant.email.EmailIngestionModule;
import com.family.assistant.email.EmailParsingModule;
import com.family.assistant.gmail.GmailIngestionModule;
import com.family.assistant.gmail.GmailWatchSetup;
import com.family.assistant.query.QueryModule;
import com.family.assistant.schema.FamilySchemaModule;
import com.family.assistant.webhook.WebhookReceiver;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rpl.agentorama.AgentClient;
import com.rpl.agentorama.AgentManager;
import com.rpl.rama.Path;
import com.rpl.rama.PState;
import com.rpl.rama.RamaClusterManager;
import com.rpl.rama.cluster.ClusterManagerBase;
import com.rpl.rama.test.InProcessCluster;
import com.rpl.rama.test.LaunchConfig;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * FamilyAssistantApp
 *
 * Boots the full Family Assistant pipeline and starts the Javalin webhook
 * server that receives Gmail Pub/Sub push notifications.
 *
 * Module launch order (dependency first):
 *   1. FamilySchemaModule    — owns $$family-data PState + *family-events depot
 *   2. EmailParsingModule    — mirrors FamilySchemaModule; parses raw email → event
 *   3. EmailIngestionModule  — fan-out batch dispatcher to email-parsing-agent
 *   4. GmailIngestionModule  — fetches Gmail INBOX → routes through EmailIngestionModule
 *   5. DigestModule          — mirrors $$family-data; answers digest queries
 *
 * Prerequisites:
 *   In "cluster" mode, the Rama cluster (ZooKeeper + Conductor + Supervisor)
 *   must be running and all modules must be deployed via `rama deploy`.
 *   In "local" mode (default), an InProcessCluster is created and modules
 *   are launched automatically — no external cluster required.
 *
 * Run with:
 *   mvn compile exec:exec
 *
 * Environment variables / system properties:
 *   RAMA_MODE    — "local" (default) uses InProcessCluster; "cluster" uses RamaClusterManager
 *   GEMINI_API_KEY  — required for LLM-backed parsing and digest agents
 *   WEBHOOK_PORT    — HTTP port for Pub/Sub push endpoint (default: 8080)
 */
public class FamilyAssistantApp {

    private static final Logger LOG = Logger.getLogger(FamilyAssistantApp.class.getName());
    private static final int DEFAULT_PORT = 8080;
    private static final long WATCH_RENEWAL_DAYS = 5;

    public static void main(String[] args) throws Exception {

        PersonalAssistantConfig config = PersonalAssistantConfig.load();
        LOG.info("[FamilyAssistantApp] Owner: " + config.ownerName
            + " | Accounts: " + config.gmailAccounts
            + " | Timezone: " + config.timezone);

        int port = DEFAULT_PORT;
        String portEnv = System.getenv("WEBHOOK_PORT");
        if (portEnv != null && !portEnv.isBlank()) {
            port = Integer.parseInt(portEnv.trim());
        }

        // -----------------------------------------------------------------------
        // Determine cluster mode: "local" (default) or "cluster"
        // -----------------------------------------------------------------------
        String ramaMode = System.getProperty("rama.mode");
        if (ramaMode == null || ramaMode.isBlank()) {
            ramaMode = System.getenv("RAMA_MODE");
        }
        if (ramaMode == null || ramaMode.isBlank()) {
            ramaMode = "local";
        }
        ramaMode = ramaMode.trim().toLowerCase();

        boolean localMode = ramaMode.equals("local");
        System.out.println("[FamilyAssistantApp] RAMA_MODE=" + ramaMode);

        // -----------------------------------------------------------------------
        // Create cluster
        // -----------------------------------------------------------------------
        ClusterManagerBase cluster;
        if (localMode) {
            System.out.println("[FamilyAssistantApp] Starting InProcessCluster...");
            InProcessCluster ipc = InProcessCluster.create();
            LaunchConfig lc = new LaunchConfig(1, 1);

            ipc.launchModule(new FamilySchemaModule(),    lc);
            ipc.launchModule(new EmailParsingModule(),    lc);
            ipc.launchModule(new EmailIngestionModule(),  lc);
            ipc.launchModule(new GmailIngestionModule(),  lc);
            ipc.launchModule(new DigestModule(),          lc);
            ipc.launchModule(new QueryModule(),           lc);

            cluster = ipc;
            System.out.println("[FamilyAssistantApp] InProcessCluster ready. Modules launched: "
                + cluster.getDeployedModuleNames());
        } else {
            System.out.println("[FamilyAssistantApp] Connecting to Rama cluster...");
            cluster = RamaClusterManager.open();
            System.out.println("[FamilyAssistantApp] Connected. Deployed modules: "
                + cluster.getDeployedModuleNames());
        }

        // -----------------------------------------------------------------------
        // Wire AgentClient for gmail-ingestion-agent
        // -----------------------------------------------------------------------
        String gmailModuleName = new GmailIngestionModule().getModuleName();
        AgentManager agentManager = AgentManager.create(cluster, gmailModuleName);
        AgentClient gmailIngestionClient = agentManager.getAgentClient("gmail-ingestion-agent");
        System.out.println("[FamilyAssistantApp] AgentClient ready: gmail-ingestion-agent");

        // -----------------------------------------------------------------------
        // Start webhook receiver
        // -----------------------------------------------------------------------
        WebhookReceiver receiver = new WebhookReceiver(gmailIngestionClient);
        receiver.start(port);

        // -----------------------------------------------------------------------
        // Debug endpoint: GET /debug/pstate/{familyId}
        // Returns the full $$family-data entry for the given familyId as JSON.
        // -----------------------------------------------------------------------
        String schemaModuleName = new FamilySchemaModule().getModuleName();
        PState familyData = cluster.clusterPState(schemaModuleName, "$$family-data");
        ObjectMapper mapper = new ObjectMapper();

        // GET /debug/pstate/{familyId} — all events for a family
        receiver.getApp().get("/debug/pstate/{familyId}", ctx -> {
            String familyId = ctx.pathParam("familyId");
            Object data = familyData.selectOne(Path.key(familyId));
            ctx.result(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(data));
            ctx.contentType("application/json");
        });

        // GET /debug/pstate — shortcut for the hardcoded family id
        receiver.getApp().get("/debug/pstate", ctx -> {
            Object data = familyData.selectOne(Path.key("keeling-family-001"));
            ctx.result(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(data));
            ctx.contentType("application/json");
        });

        // -----------------------------------------------------------------------
        // Gmail watch renewal scheduler — renew every 5 days (7-day expiry)
        // -----------------------------------------------------------------------
        ScheduledExecutorService watchScheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "gmail-watch-renewal");
            t.setDaemon(true);
            return t;
        });
        watchScheduler.scheduleAtFixedRate(() -> {
            try {
                GmailWatchSetup.renewAllWatches(config.gmailAccounts);
                LOG.info("[WatchScheduler] Next renewal in " + WATCH_RENEWAL_DAYS + " days");
            } catch (Exception e) {
                LOG.log(Level.SEVERE, "[WatchScheduler] Failed to renew Gmail watch", e);
            }
        }, 0, WATCH_RENEWAL_DAYS, TimeUnit.DAYS);
        LOG.info("[FamilyAssistantApp] Gmail watch renewal scheduled every "
            + WATCH_RENEWAL_DAYS + " days");

        // -----------------------------------------------------------------------
        // Shutdown hook — clean up cluster and HTTP server on SIGTERM / Ctrl+C
        // -----------------------------------------------------------------------
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("[FamilyAssistantApp] Shutting down...");
            watchScheduler.shutdownNow();
            receiver.stop();
            try { cluster.close(); } catch (Exception e) { /* best effort */ }
        }));

        System.out.println("[FamilyAssistantApp] Ready. Webhook listening on port " + port
            + " at POST /webhooks/gmail");
    }
}
