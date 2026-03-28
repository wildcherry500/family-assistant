package com.family.assistant.webhook;

import com.family.assistant.gmail.GmailIngestionModule;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rpl.agentorama.AgentClient;
import io.javalin.Javalin;

import java.util.Base64;

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
    private Javalin app;

    public WebhookReceiver(AgentClient gmailIngestionClient) {
        this.gmailIngestionClient = gmailIngestionClient;
    }

    // -----------------------------------------------------------------------
    // Lifecycle
    // -----------------------------------------------------------------------

    public void start(int port) {
        app = Javalin.create().start(port);

        app.post("/webhooks/gmail", ctx -> {
            long historyId = extractHistoryId(ctx.body());
            System.out.println("[WebhookReceiver] Gmail notification received, historyId=" + historyId);

            // Acknowledge immediately — Google retries if it doesn't receive 200 within ~10 s
            ctx.status(200);

            // Process asynchronously so the HTTP response is not held open
            Thread.ofVirtual().start(() -> {
                try {
                    GmailIngestionModule.IngestionSummary summary =
                        (GmailIngestionModule.IngestionSummary)
                            gmailIngestionClient.invoke(
                                new GmailIngestionModule.FetchRequest("me", 10));
                    System.out.println("[WebhookReceiver] historyId=" + historyId + " → " + summary);
                } catch (Exception e) {
                    System.err.println("[WebhookReceiver] Ingestion failed for historyId="
                        + historyId + ": " + e.getMessage());
                }
            });
        });

        System.out.println("[WebhookReceiver] Listening on port " + port + " at POST /webhooks/gmail");
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
            JsonNode root     = MAPPER.readTree(body);
            JsonNode message  = root.path("message");
            if (message.isMissingNode()) return 0;

            String data = message.path("data").asText("");
            if (data.isBlank()) return 0;

            byte[]   decoded      = Base64.getDecoder().decode(data);
            JsonNode notification = MAPPER.readTree(decoded);

            return notification.path("historyId").asLong(0);
        } catch (Exception e) {
            System.err.println("[WebhookReceiver] Failed to parse Pub/Sub message: " + e.getMessage());
            return 0;
        }
    }
}