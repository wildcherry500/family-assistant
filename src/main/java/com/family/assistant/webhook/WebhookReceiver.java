package com.family.assistant.webhook;

import com.family.assistant.gmail.GmailIngestionModule;
import com.family.assistant.query.QueryModule;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rpl.agentorama.AgentClient;
import io.javalin.Javalin;

import java.util.Base64;
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

    private final AgentClient gmailIngestionClient;
    private final AgentClient queryAgentClient;
    private Javalin app;

    public WebhookReceiver(AgentClient gmailIngestionClient, AgentClient queryAgentClient) {
        this.gmailIngestionClient = gmailIngestionClient;
        this.queryAgentClient = queryAgentClient;
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

        System.out.println("[WebhookReceiver] Listening on port " + port + " at POST /webhooks/gmail, POST /query");
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