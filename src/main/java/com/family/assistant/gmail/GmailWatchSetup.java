package com.family.assistant.gmail;

import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.model.WatchRequest;
import com.google.api.services.gmail.model.WatchResponse;

import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * GmailWatchSetup
 *
 * Registers Gmail push notifications via Google Cloud Pub/Sub.
 * Google will POST to your webhook URL whenever new mail arrives in INBOX.
 *
 * Watch registrations expire after ~7 days. Use {@link #renewWatch()} for
 * programmatic renewal from a scheduler, or run main() manually.
 *
 * Prerequisites:
 *   - Pub/Sub topic exists: projects/family-assistant-dev-490204/topics/gmail-notifications
 *   - The Gmail service account has Pub/Sub Publisher role on that topic
 *   - OAuth tokens already stored (run GmailOAuthSetup first)
 *
 * Run manually:
 *   mvn compile exec:java \
 *     -Dexec.mainClass="com.family.assistant.gmail.GmailWatchSetup"
 */
public class GmailWatchSetup {

    private static final Logger LOG = Logger.getLogger(GmailWatchSetup.class.getName());

    static final String TOPIC_NAME =
        "projects/family-assistant-dev-490204/topics/gmail-notifications";

    /**
     * Renews the Gmail INBOX watch and returns the response.
     *
     * @return WatchResponse containing historyId and expiration timestamp
     * @throws Exception if the Gmail API call fails
     */
    public static WatchResponse renewWatch() throws Exception {
        Gmail gmail = GmailService.getService();

        WatchRequest watchRequest = new WatchRequest()
            .setTopicName(TOPIC_NAME)
            .setLabelIds(List.of("INBOX"));

        WatchResponse response = gmail.users().watch("me", watchRequest).execute();

        long expirationMs = response.getExpiration();
        LOG.info("Gmail watch renewed — expires " + new Date(expirationMs)
            + " (epoch " + expirationMs + "), historyId=" + response.getHistoryId());

        return response;
    }

    /**
     * Renews Gmail push watches for all configured accounts.
     *
     * NOTE: GmailService currently authenticates a single OAuth account via the
     * tokens/ directory. True multi-account support requires per-account token
     * directories and is tracked as future work. For now, renewWatch() is called
     * once regardless of how many accounts are listed in config.
     *
     * @param accounts list of Gmail account emails from PersonalAssistantConfig
     */
    public static void renewAllWatches(List<String> accounts) throws Exception {
        if (accounts == null || accounts.isEmpty()) {
            LOG.info("[GmailWatchSetup] No accounts configured — renewing default account");
            renewWatch();
            return;
        }
        LOG.info("[GmailWatchSetup] Renewing watch for configured accounts: " + accounts);
        renewWatch(); // single OAuth token for now; TODO: per-account GmailService
    }

    public static void main(String[] args) throws Exception {
        System.out.println("Connecting to Gmail...");
        WatchResponse response = renewWatch();
        System.out.println();
        System.out.println("Watch registered successfully:");
        System.out.println("  historyId  : " + response.getHistoryId());
        System.out.println("  expiration : " + response.getExpiration()
            + " (" + new Date(response.getExpiration()) + ")");
    }
}
