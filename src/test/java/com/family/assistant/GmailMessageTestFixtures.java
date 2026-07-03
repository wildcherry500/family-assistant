package com.family.assistant;

import com.family.assistant.gmail.GmailMessage;

/**
 * Shared helper for tests that still hold raw email body strings (fixtures
 * predating the GmailMessage-based ingestion pipeline) and need to feed them
 * through EmailIngestionModule, whose "ingest" node takes List<GmailMessage>.
 *
 * gmailMessageId is left null on purpose — EmailParsingModule falls back to a
 * random UUID for the event id when it's null/blank, and these fixtures don't
 * depend on a specific id. emailSubject is left null so title extraction
 * falls back to EmailParsingModule's body-based extractTitle(), matching the
 * behavior these fixtures had before GmailMessage existed.
 */
final class GmailMessageTestFixtures {

    private GmailMessageTestFixtures() {}

    static GmailMessage fromRawBody(String rawEmailBody) {
        return new GmailMessage(
            rawEmailBody,
            null,                 // gmailMessageId
            "test@example.com",   // senderEmail
            null,                 // senderName
            null,                 // emailSubject
            System.currentTimeMillis(),
            null                  // accountLabel
        );
    }
}
