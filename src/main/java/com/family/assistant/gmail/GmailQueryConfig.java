package com.family.assistant.gmail;

import java.io.InputStream;
import java.util.Properties;

/**
 * GmailQueryConfig
 *
 * Single source of truth for the Gmail search query used by GmailIngestionModule.
 *
 * Before 2026-08-01 the query was hardcoded in two places in GmailIngestionModule —
 * once for the fetch and once for the already-processed count — which meant the cost
 * gate could price one set of messages while the module ingested a different set.
 * Both call sites now resolve through this class, and the count query is DERIVED from
 * the fetch query rather than written independently, so the two cannot drift.
 *
 * Resolution precedence (matches PersonalAssistantConfig's convention):
 *   1. env GMAIL_INGEST_QUERY
 *   2. application.properties key pa.gmail.query
 *   3. DEFAULT_QUERY
 *
 * Operational note: GmailIngestionModule runs inside a Supervisor-launched worker JVM,
 * so GMAIL_INGEST_QUERY must be exported BEFORE starting the Supervisor to take effect —
 * workers inherit the Supervisor's environment. application.properties is baked into the
 * jar and is therefore a build-time lever, not a runtime one.
 *
 * Deliberately NOT is:unread. Read/unread state is a mailbox UI concern, not a record of
 * what this system has ingested — that is what the FamilyAssistant/Processed label is for.
 */
public final class GmailQueryConfig {

    /** Gmail label applied to every message this system has ingested. */
    public static final String PROCESSED_LABEL_NAME = "FamilyAssistant/Processed";

    /** Date-bounded, label-excluded, NOT unread-filtered. */
    public static final String DEFAULT_QUERY =
        "in:INBOX -label:" + PROCESSED_LABEL_NAME + " after:2026-07-01";

    static final String ENV_VAR      = "GMAIL_INGEST_QUERY";
    static final String PROPERTY_KEY = "pa.gmail.query";

    private GmailQueryConfig() { }

    /**
     * The query used to select messages for ingestion. This is the exact string the
     * cost gate must count against — GmailBacklogCount calls this same method.
     */
    public static String fetchQuery() {
        return resolve(System.getenv(ENV_VAR), loadProperty(PROPERTY_KEY));
    }

    /**
     * Pure resolution of the precedence chain, split out so it is unit-testable without
     * mutating the process environment.
     */
    static String resolve(String envValue, String propValue) {
        if (envValue != null && !envValue.isBlank())   return envValue.trim();
        if (propValue != null && !propValue.isBlank()) return propValue.trim();
        return DEFAULT_QUERY;
    }

    /**
     * The complement of the fetch query over the same scope: same filters, but matching
     * messages that HAVE been processed rather than excluding them.
     *
     * Derived, never independently configured — that derivation is what guarantees the
     * "already processed" counter describes the same message population the fetch does.
     *
     * If the configured query does not exclude the processed label at all (someone
     * overrode it), the label term is appended instead, so the counter still means
     * "of this scope, how many are already processed."
     */
    public static String processedCountQuery(String fetchQuery) {
        String query = (fetchQuery == null || fetchQuery.isBlank())
            ? DEFAULT_QUERY
            : fetchQuery.trim();

        String excludeToken = "-label:" + PROCESSED_LABEL_NAME;
        String includeToken = "label:" + PROCESSED_LABEL_NAME;

        if (query.contains(excludeToken)) {
            return query.replace(excludeToken, includeToken);
        }
        return query + " " + includeToken;
    }

    private static String loadProperty(String key) {
        Properties props = new Properties();
        try (InputStream is = GmailQueryConfig.class.getClassLoader()
                .getResourceAsStream("application.properties")) {
            if (is != null) props.load(is);
        } catch (Exception ignored) {
            // no file / unreadable — fall through to the compiled default
        }
        return props.getProperty(key);
    }
}