package com.family.assistant.gmail;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * GmailQueryConfigTest
 *
 * Covers the Gmail ingestion query's resolution chain and the derivation of the
 * already-processed count query.
 *
 * Why this exists: before 2026-08-01 the query was hardcoded in two places in
 * GmailIngestionModule (fetch and already-processed count), which meant the Gemini cost
 * gate could price one set of messages while the module ingested a different set. These
 * tests pin the default, pin the absence of is:unread, and pin the fetch/count
 * derivation so the two can never drift apart again.
 *
 * All pure — no Gmail, no network, no cluster, no environment mutation.
 */
class GmailQueryConfigTest {

    @Test
    @DisplayName("env value wins over the properties value")
    void envWinsOverProperty() {
        assertEquals("in:INBOX after:2026-01-01",
            GmailQueryConfig.resolve("in:INBOX after:2026-01-01", "in:INBOX after:2020-01-01"));
    }

    @Test
    @DisplayName("falls back to the properties value when env is null or blank")
    void propertyUsedWhenEnvAbsent() {
        assertEquals("in:INBOX after:2020-01-01",
            GmailQueryConfig.resolve(null, "in:INBOX after:2020-01-01"));
        assertEquals("in:INBOX after:2020-01-01",
            GmailQueryConfig.resolve("   ", "in:INBOX after:2020-01-01"));
    }

    @Test
    @DisplayName("falls back to DEFAULT_QUERY when neither env nor property is set")
    void defaultUsedWhenNothingSet() {
        assertEquals(GmailQueryConfig.DEFAULT_QUERY, GmailQueryConfig.resolve(null, null));
        assertEquals(GmailQueryConfig.DEFAULT_QUERY, GmailQueryConfig.resolve("", "  "));
    }

    @Test
    @DisplayName("DEFAULT_QUERY is the agreed filter and carries NO is:unread")
    void defaultQueryIsTheAgreedFilter() {
        assertEquals("in:INBOX -label:FamilyAssistant/Processed after:2026-07-01",
            GmailQueryConfig.DEFAULT_QUERY);
        // The regression this whole class exists to prevent: read/unread state is a
        // mailbox UI concern, not a record of what this system has ingested.
        assertFalse(GmailQueryConfig.DEFAULT_QUERY.contains("is:unread"),
            "the ingestion query must not filter on unread state");
    }

    @Test
    @DisplayName("count query is the default query with the label exclusion flipped to inclusion")
    void countQueryFlipsTheLabelExclusion() {
        String counted = GmailQueryConfig.processedCountQuery(GmailQueryConfig.DEFAULT_QUERY);

        assertEquals("in:INBOX label:FamilyAssistant/Processed after:2026-07-01", counted);
        // Same scope, opposite label sense — every other filter is preserved verbatim,
        // which is what makes the counter describe the same message population.
        assertTrue(counted.contains("after:2026-07-01"));
        assertFalse(counted.contains("-label:"));
        assertFalse(counted.contains("is:unread"));
    }

    @Test
    @DisplayName("count query appends the label term when the fetch query does not exclude it")
    void countQueryAppendsWhenNoExclusionPresent() {
        assertEquals("in:INBOX after:2026-07-01 label:FamilyAssistant/Processed",
            GmailQueryConfig.processedCountQuery("in:INBOX after:2026-07-01"));
    }

    @Test
    @DisplayName("count query falls back to the default when handed null or blank")
    void countQueryHandlesNullAndBlank() {
        String expected = "in:INBOX label:FamilyAssistant/Processed after:2026-07-01";
        assertEquals(expected, GmailQueryConfig.processedCountQuery(null));
        assertEquals(expected, GmailQueryConfig.processedCountQuery("   "));
    }
}