package com.family.assistant.gmail;

import com.google.api.services.gmail.Gmail;

/**
 * GmailOAuthSetup
 *
 * Standalone OAuth consent runner. Triggers the browser-based Google consent
 * flow and writes the resulting refresh token to tokens/StoredCredential.
 *
 * This does nothing except authorize — no Pub/Sub watch, no ingestion, no
 * cluster connection. Use it when the stored token has expired or been revoked
 * (symptom: invalid_grant on the next Gmail call).
 *
 * Re-auth from scratch:
 *   rm -f tokens/StoredCredential
 *   mvn -q compile exec:java \
 *     -Dexec.mainClass="com.family.assistant.gmail.GmailOAuthSetup"
 *
 * The OAuth callback binds 127.0.0.1:8888 (see GmailService). Rama's cluster UI
 * was moved to 8889 in rama.yaml precisely to keep 8888 free for this flow.
 */
public class GmailOAuthSetup {

    public static void main(String[] args) throws Exception {
        System.out.println("[GmailOAuthSetup] Starting OAuth consent flow...");
        System.out.println("[GmailOAuthSetup] A browser window will open. "
            + "Callback listener binds 127.0.0.1:8888.");

        Gmail gmail = GmailService.getService();

        // Cheap authenticated call to prove the token actually works.
        String address = gmail.users().getProfile("me").execute().getEmailAddress();
        long total     = gmail.users().getProfile("me").execute().getMessagesTotal();

        System.out.println("[GmailOAuthSetup] SUCCESS — authorized as " + address);
        System.out.println("[GmailOAuthSetup] Mailbox total messages: " + total);
        System.out.println("[GmailOAuthSetup] Token written to tokens/StoredCredential");
    }
}
