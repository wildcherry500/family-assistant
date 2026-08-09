package com.family.assistant.gmail;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.GmailScopes;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;

/**
 * GmailService
 *
 * Provides an authorized Gmail client.
 *
 * First run: opens a browser for OAuth consent and stores the token in the tokens
 * directory. Subsequent runs: load that token without prompting.
 *
 * Credentials are loaded from src/main/resources/credentials.json (classpath).
 *
 * TOKEN DIRECTORY — ABSOLUTE, NOT cwd-RELATIVE (2026-08-09, deploy blocker D1)
 * -----------------------------------------------------------------------------
 * This was `new File("tokens")` — a cwd-relative path. In cluster mode the worker
 * resolves it against the SUPERVISOR's cwd (see RAMA_VERIFIED_LEARNINGS.md), so
 * starting the Supervisor from anywhere but the project root sent GmailService
 * looking in a directory with no token — and the failure mode was a browser OAuth
 * flow that no worker can complete, i.e. a hang, AFTER startup had reported success.
 *
 * cwd on this box is overloaded: `rama devZookeeper` hardcodes a RELATIVE "local-zk"
 * dataset dir (verified in Rama 1.5.0 bytecode, see RAMA_VERIFIED_LEARNINGS.md), so
 * one setting was silently selecting both the ZooKeeper dataset and the OAuth token.
 * Pinning this absolute removes half of that coupling.
 */
public class GmailService {

    private static final String APPLICATION_NAME  = "Family Assistant";
    private static final JsonFactory JSON_FACTORY  = GsonFactory.getDefaultInstance();
    private static final String CREDENTIALS_FILE   = "/credentials.json";

    /**
     * Absolute default: the directory holding the credential verified working by the
     * 2026-08-09 durability run (refreshed silently, 7 days after the Aug 2 re-auth).
     * Overridable for other machines/checkouts via the FA_TOKENS_DIR env var, which
     * must itself be absolute — a relative override would reintroduce the exact bug.
     */
    private static final String TOKENS_DIR_ENV = "FA_TOKENS_DIR";
    private static final String DEFAULT_TOKENS_DIRECTORY =
        "/Users/toddkeelingfolder/CORSAIR/family_assistant/tokens";

    /**
     * Set to "true" by the interactive setup mains (GmailOAuthSetup, GmailWatchSetup)
     * to permit the browser consent flow. Unset everywhere else — notably in cluster
     * workers, where a missing token must fail loudly instead of hanging on a browser
     * flow nobody can answer.
     */
    private static final String ALLOW_INTERACTIVE_PROP = "fa.oauth.interactive";

    /** Marks this JVM as an interactive OAuth entry point. Call before getService(). */
    public static void allowInteractiveConsent() {
        System.setProperty(ALLOW_INTERACTIVE_PROP, "true");
    }

    private static final List<String> SCOPES = List.of(
        GmailScopes.GMAIL_READONLY,
        GmailScopes.GMAIL_MODIFY
    );

    /**
     * Returns an authorized Gmail service instance.
     * Triggers the browser-based OAuth flow on first call.
     */
    public static Gmail getService() throws Exception {
        final NetHttpTransport transport = GoogleNetHttpTransport.newTrustedTransport();
        Credential credential = authorize(transport);
        return new Gmail.Builder(transport, JSON_FACTORY, credential)
            .setApplicationName(APPLICATION_NAME)
            .build();
    }

    // -----------------------------------------------------------------------
    // Private helpers
    // -----------------------------------------------------------------------

    /**
     * Resolves the token directory and refuses every way this can silently go wrong.
     *
     * Three checks, each guarding a failure that previously looked like success:
     *   1. absolute        — a relative override reintroduces the cwd bug
     *   2. directory exists — a missing dir means the token cannot be there
     *   3. StoredCredential present, unless interactive consent is explicitly allowed
     *      — this is the cluster-worker case, where the browser flow would hang
     */
    private static java.io.File tokensDirectory() throws FileNotFoundException {
        String configured = System.getenv(TOKENS_DIR_ENV);
        String path = (configured == null || configured.isBlank())
            ? DEFAULT_TOKENS_DIRECTORY
            : configured.trim();

        java.io.File dir = new java.io.File(path);

        if (!dir.isAbsolute()) {
            throw new IllegalStateException(
                TOKENS_DIR_ENV + " must be an ABSOLUTE path, got: " + path
                + " — a relative token dir resolves against the Supervisor's cwd in "
                + "cluster mode, which is the bug this pin exists to prevent.");
        }
        if (!dir.isDirectory()) {
            throw new FileNotFoundException(
                "OAuth token directory does not exist: " + dir
                + " — set " + TOKENS_DIR_ENV + " or restore the directory. "
                + "Re-authorize with: mvn compile exec:java "
                + "-Dexec.mainClass=\"com.family.assistant.gmail.GmailOAuthSetup\"");
        }

        java.io.File stored = new java.io.File(dir, "StoredCredential");
        boolean interactiveAllowed =
            "true".equals(System.getProperty(ALLOW_INTERACTIVE_PROP));

        if (!stored.isFile() && !interactiveAllowed) {
            throw new FileNotFoundException(
                "No StoredCredential in " + dir + " and interactive consent is not "
                + "allowed in this JVM. A cluster worker cannot complete a browser "
                + "OAuth flow — it would hang on 127.0.0.1:8888 after startup already "
                + "reported success. Run GmailOAuthSetup from the project root first.");
        }

        System.out.println("[GmailService] token dir: " + dir
            + " (StoredCredential " + (stored.isFile() ? "present" : "ABSENT")
            + ", source: " + (configured == null || configured.isBlank()
                ? "built-in default" : TOKENS_DIR_ENV) + ")");

        return dir;
    }

    private static Credential authorize(NetHttpTransport transport) throws Exception {
        InputStream in = GmailService.class.getResourceAsStream(CREDENTIALS_FILE);
        if (in == null) {
            throw new FileNotFoundException(
                "Credentials file not found on classpath: " + CREDENTIALS_FILE
                + " — copy credentials.json to src/main/resources/");
        }

        GoogleClientSecrets clientSecrets =
            GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(in));

        GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
            transport, JSON_FACTORY, clientSecrets, SCOPES)
            .setDataStoreFactory(
                new FileDataStoreFactory(tokensDirectory()))
            .setAccessType("offline")
            .build();

        LocalServerReceiver receiver =
            new LocalServerReceiver.Builder().setPort(8888).build();

        return new AuthorizationCodeInstalledApp(flow, receiver).authorize("toddkeeling@gmail.com");
    }
}
