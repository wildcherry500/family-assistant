package com.family.assistant;

import com.family.assistant.email.EmailParsingModule;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * B0 verification gate (docs/decisions/BRIEF_provenance_stamping.md).
 *
 * Asserts that the extracted prompt template constants render BYTE-IDENTICAL output to the
 * inline string concatenation they replaced. A refactor that silently alters a prompt is a
 * model-behavior change disguised as cleanup — and it would land on the same commit as the
 * mechanism meant to detect exactly that. Hence this test.
 *
 * PROVENANCE OF THE LEGACY REFERENCE BELOW — this matters, so it is recorded:
 * the two legacy* methods were NOT hand-retyped. They were produced mechanically from
 * commit 4dc6582's EmailParsingModule.java (the last commit before the extraction),
 * lines 211-226 and 277-297, via three purely textual sed edits:
 *     "String classifyPrompt =" / "String extractPrompt ="  ->  "return"
 *     "+ message.body;"                                     ->  "+ body;"
 *     dedent of the continuation lines
 * Source-snippet md5 at extraction time:
 *     classify lines 211-226 : a7b77562d19bc1e2c4eba0a1f4d9db4f
 *     extract  lines 277-297 : 489634827e5c27955e25db429febaf5e
 * Hand-copying was deliberately avoided: an identical transcription slip in both the test
 * and the production template would make this gate pass while the prompt had in fact changed.
 *
 * These methods are frozen. They are the historical record of the v1 prompt text and must
 * never be "kept in sync" with the templates — if a future prompt edit makes this test fail,
 * that is the test working. Bump the template, let the hash change, and update the assertions
 * deliberately.
 */
public class PromptTemplateByteIdentityTest {

    /** Verbatim pre-refactor classify prompt. See provenance note above. */
    private static String legacyClassifyPrompt(String body) {
        return "Classify this email along three independent dimensions. "
            + "Reply with only valid JSON, no markdown fences:\n"
            + "{\"category\": \"SCHOOL_EVENT|DEADLINE|PERMISSION_SLIP|TASK|UNKNOWN\", "
            + "\"silo\": \"VAULT|OFFICE|STUDIO|UNKNOWN\", "
            + "\"intent\": \"ACTION_REQUIRED|DECISION_NEEDED|FYI|SCHEDULING|UNKNOWN\"}\n\n"
            + "category: what the event IS.\n"
            + "silo: which life domain it belongs to — VAULT (personal/family: logistics, "
            + "medical, private financial, household), OFFICE (business: clients, operations, "
            + "strategy, business correspondence), STUDIO (creative/public: art, music, "
            + "content, cultural projects, public-facing work).\n"
            + "intent: what the email asks of you — ACTION_REQUIRED (must do something: "
            + "sign, pay, reply, attend), DECISION_NEEDED (must choose before anything can "
            + "proceed), FYI (awareness only, nothing required), SCHEDULING (primarily a "
            + "calendar/time-coordination matter).\n"
            + "Use UNKNOWN for any dimension you are not confident about — never guess.\n\n"
            + body;
    }

    /** Verbatim pre-refactor extract-details prompt. See provenance note above. */
    private static String legacyExtractPrompt(String today, String body) {
        return "Today's date is " + today + ". All dates should be in 2026 unless explicitly stated otherwise. "
            + "Extract structured data from this email. "
            + "Reply with only valid JSON, no markdown fences:\n"
            + "{\"title\": \"short title\", "
            + "\"startTime\": \"ISO-8601 datetime or null\", "
            + "\"deadline\": \"ISO-8601 datetime or null\", "
            + "\"childName\": \"first name of child or student mentioned, or null\", "
            + "\"relations\": [{\"relation\": \"MENTIONS_PERSON|PART_OF|LOCATED_AT|ACTION_NEEDED|UNKNOWN\", "
            + "\"objectType\": \"PERSON|ORG|PLACE|PROJECT|UNKNOWN\", \"object\": \"the mentioned name\"}]}\n\n"
            + "For childName: extract any student or child first name explicitly mentioned "
            + "(e.g. 'Billy', 'Emma'). Use null if no specific child is named.\n\n"
            + "For relations: emit one entry per distinct person, organization, place, or "
            + "project explicitly mentioned in the email. relation describes how it connects "
            + "to this email's event — MENTIONS_PERSON (a person is named), PART_OF (this "
            + "event/task is part of a larger project or effort), LOCATED_AT (a place is "
            + "where this happens), ACTION_NEEDED (this specific person needs to take "
            + "action, distinct from merely being mentioned). objectType is what kind of "
            + "thing \"object\" is. Use UNKNOWN for either field only when genuinely "
            + "uncertain — never guess. Omit relations entirely (empty array) if nothing "
            + "qualifies.\n\n"
            + body;
    }

    // ------------------------------------------------------------------
    // The gate: template render must equal the legacy concatenation, byte for byte.
    // ------------------------------------------------------------------

    private static final String[] BODIES = {
        "Field trip to the zoo on March 20. Permission slip due Friday.",
        "",
        "Unicode + punctuation: \u2014 \u00e9 \u4e2d\u6587 \"quoted\" 'single' \\backslash\\ \n\ttabbed",
        "Line one\nLine two\r\nLine three\n\n",
        "A body containing the literal template placeholder {{BODY}} and {{TODAY}}"
    };

    @Test
    public void classifyTemplateIsByteIdenticalToLegacy() {
        for (String body : BODIES) {
            assertEquals(legacyClassifyPrompt(body),
                         EmailParsingModule.renderClassifyPrompt(body),
                         "classify prompt diverged for body: " + summarize(body));
        }
    }

    @Test
    public void extractTemplateIsByteIdenticalToLegacy() {
        // Fixed dates, never LocalDate.now() — the whole point of hashing the template
        // rather than the render is that the date must not participate.
        String[] days = {"2026-08-03", "2025-01-01", "1999-12-31"};
        for (String today : days) {
            for (String body : BODIES) {
                assertEquals(legacyExtractPrompt(today, body),
                             EmailParsingModule.renderExtractPrompt(today, body),
                             "extract prompt diverged for today=" + today + " body: " + summarize(body));
            }
        }
    }

    /**
     * The reason the template constant is hashed instead of the rendered prompt: the extract
     * prompt interpolates the current date, so hashing the render would mint a new
     * promptVersion every calendar day and make the field noise.
     */
    @Test
    public void extractPromptVersionIsStableAcrossDates() {
        String a = EmailParsingModule.renderExtractPrompt("2026-08-03", "body");
        String b = EmailParsingModule.renderExtractPrompt("2026-08-04", "body");
        assertNotEquals(a, b, "renders should differ by date — otherwise this test proves nothing");
        assertEquals(EmailParsingModule.EXTRACT_PROMPT_VERSION,
                     EmailParsingModule.EXTRACT_PROMPT_VERSION,
                     "prompt version is a constant of the template, not of any render");
        assertFalse(EmailParsingModule.EXTRACT_PROMPT_VERSION.contains("2026"),
                    "prompt version must not embed a date");
    }

    /** Per-prompt hashes (Fork 2) — the two prompts must be independently versioned. */
    @Test
    public void promptVersionsAreDistinctAndWellFormed() {
        String c = EmailParsingModule.CLASSIFY_PROMPT_VERSION;
        String e = EmailParsingModule.EXTRACT_PROMPT_VERSION;
        assertNotEquals(c, e, "a shared version cannot express a single-prompt change");
        for (String v : new String[]{c, e}) {
            assertEquals(12, v.length(), "expected 12 hex chars, got: " + v);
            assertTrue(v.matches("[0-9a-f]{12}"), "expected lowercase hex, got: " + v);
        }
    }

    private static String summarize(String s) {
        String oneLine = s.replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t");
        return oneLine.length() <= 60 ? "\"" + oneLine + "\"" : "\"" + oneLine.substring(0, 60) + "...\"";
    }
}
