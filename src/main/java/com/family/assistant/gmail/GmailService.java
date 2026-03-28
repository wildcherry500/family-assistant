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
 * First run: opens a browser for OAuth consent and stores tokens in tokens/.
 * Subsequent runs: loads tokens from tokens/ without prompting.
 *
 * Credentials are loaded from src/main/resources/credentials.json (classpath).
 */
public class GmailService {

    private static final String APPLICATION_NAME  = "Family Assistant";
    private static final JsonFactory JSON_FACTORY  = GsonFactory.getDefaultInstance();
    private static final String TOKENS_DIRECTORY   = "tokens";
    private static final String CREDENTIALS_FILE   = "/credentials.json";

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
                new FileDataStoreFactory(new java.io.File(TOKENS_DIRECTORY)))
            .setAccessType("offline")
            .build();

        LocalServerReceiver receiver =
            new LocalServerReceiver.Builder().setPort(8888).build();

        return new AuthorizationCodeInstalledApp(flow, receiver).authorize("toddkeeling@gmail.com");
    }
}
