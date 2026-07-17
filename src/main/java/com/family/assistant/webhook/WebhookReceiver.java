package com.family.assistant.webhook;

import com.family.assistant.gmail.GmailIngestionModule;
import com.family.assistant.query.QueryModule;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rpl.agentorama.AgentClient;
import com.rpl.rama.Depot;
import com.rpl.rama.PState;
import com.rpl.rama.Path;
import io.javalin.Javalin;

import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * WebhookReceiver
 *
 * Listens for Gmail push notifications delivered via Google Cloud Pub/Sub.
 *
 * Setup (one-time, in Google Cloud Console):
 *   1. Create a Pub/Sub topic and subscription pointing to https://your-host/webhooks/gmail
 *   2. Call gmail.users().watch() with the topic name to register the push subscription
 *
 * Pub/Sub message envelope:
 * {
 *   "message": {
 *     "data": "<base64-encoded JSON>",    ← {"emailAddress":"...", "historyId": 12345}
 *     "messageId": "...",
 *     "publishTime": "..."
 *   },
 *   "subscription": "projects/.../subscriptions/..."
 * }
 *
 * On each notification:
 *   1. Decode and extract historyId (logged for tracing)
 *   2. Acknowledge immediately with HTTP 200
 *   3. Invoke gmail-ingestion-agent asynchronously via virtual thread
 */
public class WebhookReceiver {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final String DEFAULT_FAMILY_ID = "keeling-family-001";

    private final AgentClient gmailIngestionClient;
    private final AgentClient queryAgentClient;
    private final PState commitmentsPState;
    private final Depot statusChangesDepot;
    private Javalin app;

    public WebhookReceiver(AgentClient gmailIngestionClient, AgentClient queryAgentClient,
                            PState commitmentsPState, Depot statusChangesDepot) {
        this.gmailIngestionClient = gmailIngestionClient;
        this.queryAgentClient = queryAgentClient;
        this.commitmentsPState = commitmentsPState;
        this.statusChangesDepot = statusChangesDepot;
    }

    // -----------------------------------------------------------------------
    // Lifecycle
    // -----------------------------------------------------------------------

    public void start(int port) {
        app = Javalin.create().start(port);

        app.post("/webhooks/gmail", ctx -> {
            String rawBody    = ctx.body();
            long historyId    = extractHistoryId(rawBody);
            String accountLabel = extractEmailAddress(rawBody);
            System.out.println("[WebhookReceiver] Gmail notification received, historyId=" + historyId
                + ", account=" + accountLabel);

            // Acknowledge immediately — Google retries if it doesn't receive 200 within ~10 s
            ctx.status(200);

            // Process asynchronously so the HTTP response is not held open
            Thread.ofVirtual().start(() -> {
                try {
                    GmailIngestionModule.IngestionSummary summary =
                        (GmailIngestionModule.IngestionSummary)
                            gmailIngestionClient.invoke(
                                new GmailIngestionModule.FetchRequest("me", 10, accountLabel));
                    System.out.println("[WebhookReceiver] historyId=" + historyId + " → " + summary);
                } catch (Exception e) {
                    System.err.println("[WebhookReceiver] Ingestion failed for historyId="
                        + historyId + ": " + e.getMessage());
                }
            });
        });

        app.post("/query", ctx -> {
            try {
                JsonNode body = MAPPER.readTree(ctx.body());
                String familyId = body.path("familyId").asText("keeling-family-001");
                String question = body.path("question").asText("");
                String timezone = body.path("requesterTimezone").asText("America/Los_Angeles");

                if (question.isBlank()) {
                    ctx.status(400);
                    ctx.json(Map.of("error", "question is required"));
                    return;
                }

                QueryModule.QueryRequest request =
                    new QueryModule.QueryRequest(familyId, question, timezone);
                String answer = (String) queryAgentClient.invoke(request);

                ctx.json(Map.of("answer", answer));
            } catch (Exception e) {
                ctx.status(500);
                ctx.json(Map.of("error", e.getMessage()));
            }
        });

        // -----------------------------------------------------------------------
        // Layer 2 commitments — open-items view (scan-and-filter, no new index;
        // $$commitments-by-status is deliberately deferred to Layer 3) and mark-done
        // (appends to *commitment-status-changes only — $$commitments itself is owned
        // exclusively by FamilySchemaModule's stream topology, never written here)
        // -----------------------------------------------------------------------

        app.get("/commitments/{familyId}", ctx -> {
            String familyId = ctx.pathParam("familyId");
            ctx.json(openCommitments(familyId));
        });

        app.post("/commitments/{commitmentId}/done", ctx -> {
            String commitmentId = ctx.pathParam("commitmentId");
            String familyId = DEFAULT_FAMILY_ID;
            if (!ctx.body().isBlank()) {
                JsonNode body = MAPPER.readTree(ctx.body());
                familyId = body.path("familyId").asText(DEFAULT_FAMILY_ID);
            }

            // Existence read is transport-level request validation only — it decides the
            // HTTP response code, not what gets appended. markDone always appends regardless;
            // the authoritative guard against an unknown-ID stub lives in FamilySchemaModule's
            // status-change branch (gate-review revision C, EDGE_CODE_RULES.md Gate 6/Gate 8).
            // Can race a mid-flight event (a commitment created moments ago, not yet drained)
            // and return a false 404 — accepted tradeoff for a human tapping an item on screen.
            boolean existed = commitmentExists(familyId, commitmentId);
            markDone(familyId, commitmentId);

            if (existed) {
                ctx.json(Map.of("status", "ok"));
            } else {
                ctx.status(404);
                ctx.json(Map.of("status", "not_found"));
            }
        });

        System.out.println("[WebhookReceiver] Listening on port " + port
            + " at POST /webhooks/gmail, POST /query, GET /commitments/{familyId}, POST /commitments/{id}/done");
    }

    /**
     * Open-items view: scan $$commitments for a family, filter out status == DONE in plain
     * Java. No $$commitments-by-status index — deliberately deferred to Layer 3.
     */
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> openCommitments(String familyId) {
        Map<String, Object> all = (Map<String, Object>) commitmentsPState.selectOne(Path.key(familyId));
        List<Map<String, Object>> openItems = new ArrayList<>();
        if (all == null) return openItems;

        for (Map.Entry<String, Object> entry : all.entrySet()) {
            Map<String, Object> record = (Map<String, Object>) entry.getValue();
            Object status = record.get("status");
            if (!"DONE".equals(status)) {
                Map<String, Object> withId = new HashMap<>(record);
                withId.put("commitmentId", entry.getKey());
                openItems.add(withId);
            }
        }
        return openItems;
    }

    /**
     * True if commitmentId already has a record in $$commitments for this family. Used only
     * to pick the HTTP response code on mark-done (transport-level request validation) — it
     * does not gate whether markDone appends; the real guard is in the topology.
     */
    @SuppressWarnings("unchecked")
    public boolean commitmentExists(String familyId, String commitmentId) {
        Map<String, Object> record = (Map<String, Object>) commitmentsPState.selectOne(
            Path.key(familyId).key(commitmentId));
        return record != null;
    }

    /**
     * Mark-done: appends a status-change event to *commitment-status-changes only.
     * Never writes $$commitments directly — that PState is owned exclusively by
     * FamilySchemaModule's stream topology.
     */
    public void markDone(String familyId, String commitmentId) {
        Map<String, Object> change = new HashMap<>();
        change.put("familyId", familyId);
        change.put("commitmentId", commitmentId);
        change.put("newStatus", "DONE");
        change.put("changedAt", System.currentTimeMillis());
        statusChangesDepot.append(change);
    }

    public void stop() {
        if (app != null) app.stop();
    }

    public Javalin getApp() { return app; }

    // -----------------------------------------------------------------------
    // Pub/Sub decoding
    // -----------------------------------------------------------------------

    /**
     * Parses the Pub/Sub envelope, base64-decodes the payload, and extracts historyId.
     * Returns 0 if the message is malformed or has no historyId (e.g. a sync message).
     */
    private long extractHistoryId(String body) {
        try {
            JsonNode notification = decodeNotification(body);
            return notification == null ? 0 : notification.path("historyId").asLong(0);
        } catch (Exception e) {
            System.err.println("[WebhookReceiver] Failed to parse Pub/Sub message: " + e.getMessage());
            return 0;
        }
    }

    /**
     * Extracts the emailAddress field from the Pub/Sub notification payload.
     * Returns null if absent or malformed.
     */
    private String extractEmailAddress(String body) {
        try {
            JsonNode notification = decodeNotification(body);
            if (notification == null) return null;
            String email = notification.path("emailAddress").asText(null);
            return (email != null && !email.isBlank()) ? email : null;
        } catch (Exception e) {
            return null;
        }
    }

    /** Decodes the base64 Pub/Sub data field and returns the parsed notification JSON. */
    private JsonNode decodeNotification(String body) throws Exception {
        JsonNode root    = MAPPER.readTree(body);
        JsonNode message = root.path("message");
        if (message.isMissingNode()) return null;
        String data = message.path("data").asText("");
        if (data.isBlank()) return null;
        byte[] decoded = Base64.getDecoder().decode(data);
        return MAPPER.readTree(decoded);
    }
}