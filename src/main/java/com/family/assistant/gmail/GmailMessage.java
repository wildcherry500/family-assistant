package com.family.assistant.gmail;

import com.rpl.rama.RamaSerializable;

/**
 * GmailMessage
 *
 * Carries Gmail-sourced provenance data alongside the raw email body
 * through the ingestion pipeline so every stored event has full traceability.
 */
public class GmailMessage implements RamaSerializable {

    public final String body;           // plain-text body for LLM parsing
    public final String gmailMessageId; // Gmail API message id
    public final String senderEmail;    // e.g. acemystuff@gmail.com
    public final String senderName;     // display name from From header, may be null
    public final String emailSubject;   // Subject header value, may be null
    public final long   receivedAt;     // internalDate — epoch millis when Gmail received it
    public final String accountLabel;   // Gmail account that received this message, e.g. "user@gmail.com"

    public GmailMessage(String body, String gmailMessageId,
                        String senderEmail, String senderName,
                        String emailSubject, long receivedAt,
                        String accountLabel) {
        this.body           = body;
        this.gmailMessageId = gmailMessageId;
        this.senderEmail    = senderEmail;
        this.senderName     = senderName;
        this.emailSubject   = emailSubject;
        this.receivedAt     = receivedAt;
        this.accountLabel   = accountLabel;
    }
}
