package com.family.assistant.gmail;

import com.family.assistant.email.EmailIngestionModule;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.model.Label;
import com.google.api.services.gmail.model.Message;
import com.google.api.services.gmail.model.MessagePart;
import com.google.api.services.gmail.model.MessagePartBody;
import com.google.api.services.gmail.model.ModifyMessageRequest;
import com.rpl.agentorama.AgentClient;
import com.rpl.agentorama.AgentModule;
import com.rpl.agentorama.AgentNode;
import com.rpl.agentorama.AgentTopology;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Set;

/**
 * GmailIngestionModule
 *
 * Fetches unread Gmail messages and routes them through the email parsing
 * pipeline (EmailIngestionModule → EmailParsingModule → FamilySchemaModule).
 *
 * Agent graph: fetchAndProcess (terminal)
 *
 * Input:  FetchRequest(userId, maxResults)
 * Output: IngestionSummary(fetched, processed, failed)
 */
public class GmailIngestionModule extends AgentModule implements java.io.Serializable {

    @Override
    public String getModuleName() {
        return "GmailIngestionModule";
    }

    // -----------------------------------------------------------------------
    // Input type
    // -----------------------------------------------------------------------
    public static class FetchRequest implements com.rpl.rama.RamaSerializable {
        public final String userId;
        public final int maxResults;

        public FetchRequest(String userId, int maxResults) {
            this.userId     = userId;
            this.maxResults = maxResults;
        }
    }

    private static final String PROCESSED_LABEL_NAME = "FamilyAssistant/Processed";

    // -----------------------------------------------------------------------
    // Sender blocklist — matched against the From header (case-insensitive)
    // -----------------------------------------------------------------------
    private static final Set<String> BLOCKED_SENDER_TOKENS = Set.of(
        "linkedin.com", "google.com", "googlemail.com",
        "noreply", "no-reply", "mailer-daemon"
    );

    // -----------------------------------------------------------------------
    // Output type
    // -----------------------------------------------------------------------
    public static class IngestionSummary implements com.rpl.rama.RamaSerializable {
        public final int fetched;           // passed filter, sent to LLM pipeline
        public final int skipped;           // blocked by sender pre-filter
        public final int alreadyProcessed;  // excluded by FamilyAssistant/Processed label
        public final int processed;         // events successfully written to store
        public final int failed;            // emails that errored during parsing

        public IngestionSummary(int fetched, int skipped, int alreadyProcessed, int processed, int failed) {
            this.fetched          = fetched;
            this.skipped          = skipped;
            this.alreadyProcessed = alreadyProcessed;
            this.processed        = processed;
            this.failed           = failed;
        }

        @Override
        public String toString() {
            return "IngestionSummary{fetched=" + fetched
                + ", skipped=" + skipped
                + ", alreadyProcessed=" + alreadyProcessed
                + ", processed=" + processed
                + ", failed=" + failed + "}";
        }
    }

    // -----------------------------------------------------------------------
    // defineAgents
    // -----------------------------------------------------------------------
    @Override
    public void defineAgents(AgentTopology topology) {

        topology.newAgent("gmail-ingestion-agent")

            // ----------------------------------------------------------------
            // Node: fetchAndProcess  (terminal)
            // Input:  FetchRequest
            // Output: IngestionSummary — returned to caller
            // ----------------------------------------------------------------
            .node("fetchAndProcess", null,
                (AgentNode agentNode, FetchRequest request) -> {

                    // 1. Authorize and build Gmail client
                    Gmail gmail;
                    try {
                        gmail = GmailService.getService();
                    } catch (Exception e) {
                        System.err.println("[GmailIngestionModule] Failed to initialize Gmail: "
                            + e.getMessage());
                        agentNode.result(new IngestionSummary(0, 0, 0, 0, 0));
                        return;
                    }

                    // 2. Resolve processed label ID (create if absent) — needed for query
                    String processedLabelId;
                    try {
                        processedLabelId = getOrCreateLabel(gmail, request.userId, PROCESSED_LABEL_NAME);
                    } catch (Exception e) {
                        System.err.println("[GmailIngestionModule] Could not resolve label '"
                            + PROCESSED_LABEL_NAME + "': " + e.getMessage());
                        processedLabelId = null;
                    }

                    // 3. Count already-processed unread messages (excluded from main fetch)
                    int alreadyProcessed = 0;
                    if (processedLabelId != null) {
                        try {
                            var countResponse = gmail.users().messages()
                                .list(request.userId)
                                .setQ("is:unread in:INBOX label:" + PROCESSED_LABEL_NAME)
                                .execute();
                            var counted = countResponse.getMessages();
                            alreadyProcessed = (counted != null) ? counted.size() : 0;
                        } catch (Exception e) {
                            // non-fatal — summary field stays 0
                        }
                    }

                    // 4. List unread messages not yet labeled as processed
                    String gmailQuery = "is:unread in:INBOX -label:" + PROCESSED_LABEL_NAME;
                    System.out.println("[GmailIngestionModule] Query: " + gmailQuery);
                    List<Message> messages;
                    try {
                        var listResponse = gmail.users().messages()
                            .list(request.userId)
                            .setQ(gmailQuery)
                            .setMaxResults((long) request.maxResults)
                            .execute();
                        messages = listResponse.getMessages();
                        System.out.println("[GmailIngestionModule] Raw API result: "
                            + (messages == null ? "null (0 messages)" : messages.size() + " message(s)"));
                    } catch (Exception e) {
                        System.err.println("[GmailIngestionModule] Failed to list messages: "
                            + e.getMessage());
                        agentNode.result(new IngestionSummary(0, 0, alreadyProcessed, 0, 0));
                        return;
                    }

                    if (messages == null || messages.isEmpty()) {
                        // Probe without the unread filter to diagnose whether emails exist but are read
                        try {
                            var probe = gmail.users().messages()
                                .list(request.userId)
                                .setQ("in:INBOX from:acemystuff@gmail.com")
                                .setMaxResults(5L)
                                .execute();
                            var probeMessages = probe.getMessages();
                            System.out.println("[GmailIngestionModule] Probe (no unread filter): "
                                + (probeMessages == null ? "0" : probeMessages.size())
                                + " message(s) from acemystuff@gmail.com in INBOX");
                            if (probeMessages != null) {
                                for (Message pm : probeMessages) {
                                    Message pf = gmail.users().messages()
                                        .get(request.userId, pm.getId())
                                        .setFormat("metadata")
                                        .setMetadataHeaders(List.of("Subject", "From"))
                                        .execute();
                                    String subj = extractHeader(pf, "Subject");
                                    String frm  = extractHeader(pf, "From");
                                    System.out.println("[GmailIngestionModule]   id=" + pm.getId()
                                        + " labels=" + pf.getLabelIds()
                                        + " from=" + frm + " subject=" + subj);
                                }
                            }
                        } catch (Exception probe) {
                            System.err.println("[GmailIngestionModule] Probe failed: " + probe.getMessage());
                        }
                        agentNode.result(new IngestionSummary(0, 0, alreadyProcessed, 0, 0));
                        return;
                    }

                    // 5. Fetch full message, apply sender pre-filter, build GmailMessage objects
                    List<GmailMessage> gmailMessages = new ArrayList<>();
                    List<String> messageIds          = new ArrayList<>();
                    int skipped = 0;
                    for (Message msg : messages) {
                        try {
                            Message full = gmail.users().messages()
                                .get(request.userId, msg.getId())
                                .setFormat("full")
                                .execute();

                            String from    = extractHeader(full, "From");
                            String subject = extractHeader(full, "Subject");
                            System.out.println("[GmailIngestionModule] Checking message id="
                                + msg.getId() + " from=" + from
                                + " subject=" + subject
                                + " labels=" + full.getLabelIds());
                            if (isBlockedSender(from)) {
                                System.out.println("[GmailIngestionModule] Skipping blocked sender: " + from);
                                skipped++;
                                continue;
                            }

                            String body = extractBody(full);
                            if (body == null || body.isBlank()) {
                                System.out.println("[GmailIngestionModule] Skipping message "
                                    + msg.getId() + ": body is null/blank");
                            } else {
                                String[] sender    = parseSender(from);
                                long     received  = full.getInternalDate() != null
                                                     ? full.getInternalDate() : System.currentTimeMillis();
                                gmailMessages.add(new GmailMessage(
                                    body, msg.getId(),
                                    sender[0], sender[1],
                                    subject, received));
                                messageIds.add(msg.getId());
                            }
                        } catch (Exception e) {
                            System.err.println("[GmailIngestionModule] Skipping message "
                                + msg.getId() + ": " + e.getMessage());
                        }
                    }

                    int fetched = gmailMessages.size();
                    if (fetched == 0) {
                        // Probe: show acemystuff messages regardless of read/label status
                        try {
                            var probe = gmail.users().messages()
                                .list(request.userId)
                                .setQ("in:INBOX from:acemystuff@gmail.com")
                                .setMaxResults(5L)
                                .execute();
                            var pm = probe.getMessages();
                            System.out.println("[GmailIngestionModule] Probe (acemystuff, no filters): "
                                + (pm == null ? "0" : pm.size()) + " message(s)");
                            if (pm != null) {
                                for (Message m : pm) {
                                    Message pf = gmail.users().messages()
                                        .get(request.userId, m.getId())
                                        .setFormat("metadata")
                                        .setMetadataHeaders(List.of("Subject", "From"))
                                        .execute();
                                    System.out.println("[GmailIngestionModule]   id=" + m.getId()
                                        + " labels=" + pf.getLabelIds()
                                        + " subject=" + extractHeader(pf, "Subject"));
                                }
                            }
                        } catch (Exception probe) {
                            System.err.println("[GmailIngestionModule] Probe failed: " + probe.getMessage());
                        }
                        agentNode.result(new IngestionSummary(0, skipped, alreadyProcessed, 0, 0));
                        return;
                    }

                    // 6. Route through the email parsing pipeline
                    AgentClient ingestionClient = agentNode.getMirrorAgentClient(
                        "EmailIngestionModule", "email-ingestion-agent");

                    EmailIngestionModule.IngestionResult result =
                        (EmailIngestionModule.IngestionResult)
                            ingestionClient.invoke(new ArrayList<>(gmailMessages));

                    // 7. Label processed messages and mark as read
                    if (processedLabelId != null) {
                        applyProcessedLabel(gmail, request.userId, messageIds, processedLabelId);
                    }

                    agentNode.result(new IngestionSummary(
                        fetched,
                        skipped,
                        alreadyProcessed,
                        result.eventIds.size(),
                        result.failed
                    ));
                });
    }

    // -----------------------------------------------------------------------
    // Private helpers
    // -----------------------------------------------------------------------

    /**
     * Returns the label ID for the given label name, creating the label if it doesn't exist.
     */
    private String getOrCreateLabel(Gmail gmail, String userId, String labelName) throws Exception {
        var existing = gmail.users().labels().list(userId).execute().getLabels();
        if (existing != null) {
            for (Label label : existing) {
                if (labelName.equalsIgnoreCase(label.getName())) {
                    return label.getId();
                }
            }
        }
        // Not found — create it
        Label newLabel = new Label()
            .setName(labelName)
            .setLabelListVisibility("labelShow")
            .setMessageListVisibility("show");
        Label created = gmail.users().labels().create(userId, newLabel).execute();
        System.out.println("[GmailIngestionModule] Created label '" + labelName + "' (id=" + created.getId() + ")");
        return created.getId();
    }

    /**
     * Adds the processed label and removes UNREAD from each message ID.
     * Failures are logged but do not abort the batch.
     */
    private void applyProcessedLabel(Gmail gmail, String userId,
                                     List<String> messageIds, String processedLabelId) {
        ModifyMessageRequest modify = new ModifyMessageRequest()
            .setAddLabelIds(List.of(processedLabelId))
            .setRemoveLabelIds(List.of("UNREAD"));
        for (String id : messageIds) {
            try {
                gmail.users().messages().modify(userId, id, modify).execute();
            } catch (Exception e) {
                System.err.println("[GmailIngestionModule] Failed to label message " + id + ": " + e.getMessage());
            }
        }
        System.out.println("[GmailIngestionModule] Labeled " + messageIds.size()
            + " message(s) as '" + PROCESSED_LABEL_NAME + "'");
    }

    /**
     * Parses a From header into [senderEmail, senderName].
     * Handles "Display Name <email@example.com>" and bare "email@example.com" forms.
     */
    private String[] parseSender(String from) {
        if (from == null) return new String[]{null, null};
        from = from.trim();
        int lt = from.lastIndexOf('<');
        int gt = from.lastIndexOf('>');
        if (lt >= 0 && gt > lt) {
            String email = from.substring(lt + 1, gt).trim();
            String name  = from.substring(0, lt).trim()
                               .replaceAll("^\"|\"$", "").trim();
            return new String[]{email, name.isEmpty() ? null : name};
        }
        return new String[]{from, null};
    }

    /**
     * Returns the value of the named header from the message payload, or null.
     */
    private String extractHeader(Message message, String name) {
        if (message.getPayload() == null || message.getPayload().getHeaders() == null) return null;
        return message.getPayload().getHeaders().stream()
            .filter(h -> name.equalsIgnoreCase(h.getName()))
            .map(h -> h.getValue())
            .findFirst().orElse(null);
    }

    /**
     * Returns true if the From header matches any blocked sender token.
     * Matching is case-insensitive and checks for substring presence.
     */
    private boolean isBlockedSender(String from) {
        if (from == null) return false;
        String lower = from.toLowerCase();
        return BLOCKED_SENDER_TOKENS.stream().anyMatch(lower::contains);
    }

    /**
     * Extracts plain-text body from a Gmail Message.
     * Prefers text/plain parts; falls back to the snippet if no body found.
     */
    private String extractBody(Message message) {
        if (message.getPayload() == null) {
            return message.getSnippet();
        }

        String body = extractFromPart(message.getPayload());
        if (body != null && !body.isBlank()) {
            return body;
        }

        // Last resort: use the snippet
        return message.getSnippet();
    }

    private String extractFromPart(MessagePart part) {
        if (part == null) return null;

        String mimeType = part.getMimeType();

        // Leaf part with data
        if (part.getParts() == null || part.getParts().isEmpty()) {
            if ("text/plain".equalsIgnoreCase(mimeType)) {
                return decodeBase64(part.getBody());
            }
            // Accept text/html only if no plain text found
            if ("text/html".equalsIgnoreCase(mimeType)) {
                return decodeBase64(part.getBody());
            }
            return null;
        }

        // Multipart — prefer text/plain child, fall back to first non-null
        String fallback = null;
        for (MessagePart child : part.getParts()) {
            String childMime = child.getMimeType();
            if ("text/plain".equalsIgnoreCase(childMime)) {
                String text = decodeBase64(child.getBody());
                if (text != null && !text.isBlank()) return text;
            }
            if (fallback == null) {
                fallback = extractFromPart(child);
            }
        }
        return fallback;
    }

    private String decodeBase64(MessagePartBody body) {
        if (body == null || body.getData() == null) return null;
        try {
            byte[] decoded = Base64.getUrlDecoder().decode(body.getData());
            return new String(decoded, java.nio.charset.StandardCharsets.UTF_8);
        } catch (Exception e) {
            return null;
        }
    }
}
