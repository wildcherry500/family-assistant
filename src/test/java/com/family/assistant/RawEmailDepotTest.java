package com.family.assistant;

import com.family.assistant.schema.FamilySchemaModule;
import com.rpl.rama.Depot;
import com.rpl.rama.Path;
import com.rpl.rama.PState;
import com.rpl.rama.test.InProcessCluster;
import com.rpl.rama.test.LaunchConfig;
import org.junit.jupiter.api.*;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * RawEmailDepotTest
 *
 * Verifies the raw-email write-ahead log: records appended to FamilySchemaModule's
 * *raw-emails depot are durably captured, complete (all seven GmailMessage fields
 * plus familyId), and drained into the inspectable $$raw-emails PState keyed by
 * gmailMessageId.
 *
 * This exercises the SCHEMA side (depot + drain topology) directly, exactly as
 * IndexPStateTest does for the inverted indexes — appending raw record Maps to the
 * depot rather than going through email-parsing-agent's persist-raw node (which sits
 * upstream of the LLM classify node). The persist-raw node wiring is exercised by the
 * LLM-gated end-to-end tests.
 *
 * No GEMINI_API_KEY required — this test does NOT use any LLM.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class RawEmailDepotTest {

    private static final String FAMILY_ID = "fam-test";
    private static final String MODULE_NAME = "FamilySchemaModule";

    private InProcessCluster ipc;
    private Depot rawEmailsDepot;
    private PState rawEmails;

    @BeforeAll
    void setup() throws Exception {
        ipc = InProcessCluster.create();

        FamilySchemaModule schemaModule = new FamilySchemaModule();
        ipc.launchModule(schemaModule, new LaunchConfig(1, 1));

        rawEmailsDepot = ipc.clusterDepot(MODULE_NAME, "*raw-emails");
        rawEmails      = ipc.clusterPState(MODULE_NAME, "$$raw-emails");

        // Complete raw record for a real Gmail message.
        appendRaw("gmail-msg-1", "Field trip permission slip due Friday.",
            "Field Trip Permission", "teacher@school.edu", "Ms. Cohen",
            "acemystuff@gmail.com", 1_700_000_000_000L);

        // A second message, different id, null senderName (allowed).
        appendRaw("gmail-msg-2", "Reminder: picture day next week.",
            "Picture Day", "office@school.edu", null,
            "acemystuff@gmail.com", 1_700_000_500_000L);

        // Wait for the raw-emails-stream topology to drain both records.
        long deadline = System.currentTimeMillis() + 30_000;
        while (System.currentTimeMillis() < deadline) {
            Object r1 = rawEmails.selectOne(Path.key(FAMILY_ID).key("gmail-msg-1"));
            Object r2 = rawEmails.selectOne(Path.key(FAMILY_ID).key("gmail-msg-2"));
            if (r1 != null && r2 != null) break;
            Thread.sleep(500);
        }
    }

    @AfterAll
    void teardown() throws Exception {
        if (ipc != null) ipc.close();
    }

    private void appendRaw(String gmailMessageId, String body, String emailSubject,
                           String senderEmail, String senderName,
                           String accountLabel, long receivedAt) {
        Map<String, Object> raw = new HashMap<>();
        raw.put("familyId",       FAMILY_ID);
        raw.put("body",           body);
        raw.put("emailSubject",   emailSubject);
        raw.put("senderEmail",    senderEmail);
        raw.put("senderName",     senderName);
        raw.put("gmailMessageId", gmailMessageId);
        raw.put("accountLabel",   accountLabel);
        raw.put("receivedAt",     receivedAt);
        rawEmailsDepot.append(raw);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> rawRecord(String gmailMessageId) {
        return (Map<String, Object>)
            rawEmails.selectOne(Path.key(FAMILY_ID).key(gmailMessageId));
    }

    @Test
    @Order(1)
    void testRawRecordExists() {
        assertNotNull(rawRecord("gmail-msg-1"), "raw record for gmail-msg-1 should exist");
        assertNotNull(rawRecord("gmail-msg-2"), "raw record for gmail-msg-2 should exist");
    }

    @Test
    @Order(2)
    void testRawRecordIsComplete() {
        Map<String, Object> raw = rawRecord("gmail-msg-1");
        assertNotNull(raw);
        // All seven GmailMessage fields plus familyId must be present for replay.
        assertEquals(FAMILY_ID,                         raw.get("familyId"));
        assertEquals("Field trip permission slip due Friday.", raw.get("body"));
        assertEquals("Field Trip Permission",           raw.get("emailSubject"));
        assertEquals("teacher@school.edu",              raw.get("senderEmail"));
        assertEquals("Ms. Cohen",                       raw.get("senderName"));
        assertEquals("gmail-msg-1",                     raw.get("gmailMessageId"));
        assertEquals("acemystuff@gmail.com",            raw.get("accountLabel"));
        assertEquals(1_700_000_000_000L,                raw.get("receivedAt"));
    }

    @Test
    @Order(3)
    void testNullSenderNameIsPreserved() {
        Map<String, Object> raw = rawRecord("gmail-msg-2");
        assertNotNull(raw);
        assertTrue(raw.containsKey("senderName"),
            "senderName key must exist even when null, so replay sees the full shape");
        assertNull(raw.get("senderName"), "senderName was null on this message");
    }

    @Test
    @Order(4)
    void testReappendSameMessageIdOverwritesNotDuplicates() throws Exception {
        // Re-ingesting the same gmailMessageId overwrites the keyed view with identical
        // data (the depot log itself is append-only; the $$raw-emails view is a
        // latest-per-message projection). Mirrors parsed-event idempotency.
        appendRaw("gmail-msg-1", "Field trip permission slip due Friday.",
            "Field Trip Permission", "teacher@school.edu", "Ms. Cohen",
            "acemystuff@gmail.com", 1_700_000_000_000L);
        Thread.sleep(2000);

        Map<String, Object> family = (Map<String, Object>)
            rawEmails.selectOne(Path.key(FAMILY_ID));
        assertNotNull(family);
        assertEquals(2, family.size(),
            "two distinct gmailMessageIds → exactly two keyed raw records, no duplicate");
    }
}
