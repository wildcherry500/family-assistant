package com.family.assistant.gmail;

import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.model.ListMessagesResponse;
import com.google.api.services.gmail.model.Message;

import java.util.List;

/**
 * GmailOAuthSetup
 *
 * Run once to complete the OAuth browser flow and verify credentials.
 * Tokens are stored in tokens/ for all subsequent runs.
 *
 * Run with:
 *   mvn test-compile exec:java \
 *     -Dexec.mainClass="com.family.assistant.gmail.GmailOAuthSetup" \
 *     -Dexec.classpathScope=test
 */
public class GmailOAuthSetup {

    public static void main(String[] args) throws Exception {
        System.out.println("Authorizing with Gmail...");

        Gmail gmail = GmailService.getService();

        System.out.println("Authorization successful. Fetching 5 most recent inbox messages...\n");

        ListMessagesResponse response = gmail.users().messages()
            .list("me")
            .setQ("in:INBOX")
            .setMaxResults(5L)
            .execute();

        List<Message> messages = response.getMessages();
        if (messages == null || messages.isEmpty()) {
            System.out.println("No messages found.");
            return;
        }

        for (Message msg : messages) {
            Message full = gmail.users().messages()
                .get("me", msg.getId())
                .setFormat("metadata")
                .setMetadataHeaders(List.of("Subject", "From"))
                .execute();

            String subject = full.getPayload().getHeaders().stream()
                .filter(h -> "Subject".equalsIgnoreCase(h.getName()))
                .map(h -> h.getValue())
                .findFirst().orElse("(no subject)");

            String from = full.getPayload().getHeaders().stream()
                .filter(h -> "From".equalsIgnoreCase(h.getName()))
                .map(h -> h.getValue())
                .findFirst().orElse("(unknown)");

            System.out.printf("From:    %s%n", from);
            System.out.printf("Subject: %s%n%n", subject);
        }
    }
}
