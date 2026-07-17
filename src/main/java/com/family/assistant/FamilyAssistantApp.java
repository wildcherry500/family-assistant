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
import com.rpl.rama.Depot;
import com.rpl.rama.Path;
import com.rpl.rama.PState;
import com.rpl.rama.RamaClusterManager;
import com.rpl.rama.cluster.ClusterManagerBase;
import com.rpl.rama.test.InProcessCluster;
import com.rpl.rama.test.LaunchConfig;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
        // Wire AgentClient for query-agent
        // -----------------------------------------------------------------------
        String queryModuleName = new QueryModule().getModuleName();
        AgentManager queryAgentManager = AgentManager.create(cluster, queryModuleName);
        AgentClient queryAgentClient = queryAgentManager.getAgentClient("query-agent");
        System.out.println("[FamilyAssistantApp] AgentClient ready: query-agent");

        // -----------------------------------------------------------------------
        // Start webhook receiver
        // -----------------------------------------------------------------------
        String schemaModuleName = new FamilySchemaModule().getModuleName();
        PState commitmentsPState = cluster.clusterPState(schemaModuleName, "$$commitments");
        Depot statusChangesDepot = cluster.clusterDepot(schemaModuleName, "*commitment-status-changes");

        WebhookReceiver receiver = new WebhookReceiver(
            gmailIngestionClient, queryAgentClient, commitmentsPState, statusChangesDepot);
        receiver.start(port);

        // -----------------------------------------------------------------------
        // Debug routes — gated behind DEBUG_ROUTES_ENABLED (default off), per gate-review
        // revision D. When unset/false, these routes are never registered at all (not just
        // 404'd) — nothing to strip before the Cloudflare tunnel exposes the app. See
        // README.md's "Debug endpoints" section for the flag.
        // -----------------------------------------------------------------------
        boolean debugRoutesEnabled = Boolean.parseBoolean(System.getenv("DEBUG_ROUTES_ENABLED"));
        if (debugRoutesEnabled) {
            System.out.println("[FamilyAssistantApp] DEBUG_ROUTES_ENABLED=true — /debug/* routes registered");

            // GET /debug/pstate/{familyId} and /debug/pstate — full $$family-data entry as JSON.
            PState familyData = cluster.clusterPState(schemaModuleName, "$$family-data");
            ObjectMapper mapper = new ObjectMapper();

            receiver.getApp().get("/debug/pstate/{familyId}", ctx -> {
                String familyId = ctx.pathParam("familyId");
                Object data = familyData.selectOne(Path.key(familyId));
                ctx.result(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(data));
                ctx.contentType("application/json");
            });

            receiver.getApp().get("/debug/pstate", ctx -> {
                Object data = familyData.selectOne(Path.key("keeling-family-001"));
                ctx.result(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(data));
                ctx.contentType("application/json");
            });

            // POST /debug/inject-test-event — appends directly to *family-events, the same
            // depot EmailParsingModule appends to after LLM extraction
            // (EmailParsingModule.java:374-375). Bypasses Gmail fetch and LLM classification
            // only; everything downstream (FamilySchemaModule's real stream topology:
            // $$family-data, $$edges-forward/inverse, $$entities, $$commitments) runs exactly
            // as in production. Manual test-injection utility, not part of the reviewed
            // webhook route surface.
            Depot familyEventsDepot = cluster.clusterDepot(schemaModuleName, "*family-events");

            receiver.getApp().post("/debug/inject-test-event", ctx -> {
                String familyId = "keeling-family-001";
                String eventId = "debug-evt-" + System.currentTimeMillis();

                Map<String, String> relation = new HashMap<>();
                relation.put("relation", "ACTION_NEEDED");
                relation.put("objectType", "PERSON");
                relation.put("object", "Todd");
                List<Map<String, String>> relations = new ArrayList<>();
                relations.add(relation);

                Map<String, Object> event = new HashMap<>();
                event.put("id", eventId);
                event.put("familyId", familyId);
                event.put("title", "Debug injected event — sign permission slip");
                event.put("description", "Manually injected via /debug/inject-test-event to verify the open-items/mark-done loop.");
                event.put("tags", new ArrayList<String>());
                event.put("personId", new ArrayList<String>());
                event.put("relations", relations);
                event.put("created", System.currentTimeMillis());
                event.put("sourceType", "test");

                familyEventsDepot.append(event);

                ctx.json(Map.of("status", "ok", "eventId", eventId, "familyId", familyId));
            });
        } else {
            System.out.println("[FamilyAssistantApp] DEBUG_ROUTES_ENABLED not set — /debug/* routes not registered");
        }

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
