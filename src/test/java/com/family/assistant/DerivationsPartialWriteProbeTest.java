package com.family.assistant;

import com.rpl.rama.AckLevel;
import com.rpl.rama.Depot;
import com.rpl.rama.Path;
import com.rpl.rama.PState;
import com.rpl.rama.RamaModule;
import com.rpl.rama.test.InProcessCluster;
import com.rpl.rama.test.LaunchConfig;
import org.junit.jupiter.api.*;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * DerivationsPartialWriteProbeTest
 *
 * Standalone probe (2026-09-02) for the B1 "derivations" nested-map design
 * (docs/decisions/PLAN_provenance_temporal.md, Fork 1). Does NOT touch production
 * $$family-data or FamilySchemaModule -- Gate 2 forbids a second topology writing an
 * existing PState, so this declares its own PState with $$family-data's EXACT schema
 * (three nested PState.mapSchema calls ending in Object.class, verified against
 * FamilySchemaModule.java:166-169) inside a throwaway module, mirroring the precedent
 * set by the 2026-07-16 probe recorded in RAMA_VERIFIED_LEARNINGS.md ("Real failure
 * mode the probe caught, with a confirmed fix").
 *
 * Two things verified against the actual current source before this test was written,
 * not assumed:
 *   1. write-to-store's ACTUAL record-assembly pattern (EmailParsingModule.java:438-479)
 *      is ONE java.util.HashMap built via sequential .put() calls in plain agent-node
 *      Java code, then depot.append()'d ONCE -- NOT sequential PState-level
 *      localTransform calls. (That key-by-key localTransform pattern belongs to
 *      $$commitments' creation branch, FamilySchemaModule.java:358-364 -- a different
 *      PState, deliberately built that way so it CAN take a later partial write.) The
 *      PState write itself (FamilySchemaModule.java:279-280) is a SINGLE whole-record
 *      termVal of that already-assembled HashMap. Branch 1 below reproduces exactly
 *      that shape, not the commitments shape.
 *   2. A second, separate write targeting a DIFFERENT top-level key of the SAME
 *      already-stored record is the shape RAMA_VERIFIED_LEARNINGS.md's "later, separate
 *      write" entry already proved throws when the container was stored via whole-record
 *      termVal. $$family-data has never had a second write path in production, so this
 *      is what B1 would hit the moment it ever needed to update a record after creation
 *      (e.g. a derivations sub-key). Branch 2 models that hypothetical write.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class DerivationsPartialWriteProbeTest {

    private static final String FAMILY_ID = "probe-family-001";
    private static final String EVENT_ID = "probe-event-001";

    private InProcessCluster ipc;
    private Depot probeEventsDepot;
    private Depot probeUpdatesDepot;
    private PState probeFamilyData;

    /**
     * Mirrors FamilySchemaModule's $$family-data shape and write pattern exactly
     * (schema: FamilySchemaModule.java:166-169; write: :274-280), plus ONE extra
     * .source(...) branch modeling a hypothetical future second write into an
     * existing record -- the thing production code has never done to this PState
     * shape, and the thing B1 would need eventually.
     */
    public static class ProbeModule implements RamaModule, java.io.Serializable {
        @Override
        public void define(Setup setup, Topologies topologies) {
            setup.declareDepot("*probe-events", Depot.hashBy("familyId"));
            setup.declareDepot("*probe-updates", Depot.hashBy("familyId"));

            var stream = topologies.stream("probe-stream");

            // Exact schema shape as FamilySchemaModule.java:166-169's $$family-data.
            stream.pstate("$$probe-family-data",
                PState.mapSchema(String.class,
                    PState.mapSchema(String.class,
                        PState.mapSchema(String.class, Object.class))));

            // Branch 1 -- mirrors FamilySchemaModule.java:274-280 exactly: whole-record
            // termVal from a single already-assembled HashMap, matching write-to-store's
            // real assembly pattern.
            stream.source("*probe-events").out("*record")
              .select("*record", Path.key("familyId")).out("*familyId")
              .select("*record", Path.key("id")).out("*eventId")
              .hashPartition("*familyId")
              .localTransform("$$probe-family-data",
                  Path.key("*familyId").key("events").key("*eventId").termVal("*record"));

            // Branch 2 -- the write $$family-data has NEVER had in production: a second,
            // narrower write into a DIFFERENT top-level key ("status") of the SAME record
            // already written by branch 1. Same-topology successive .source(...) calls,
            // so Gate 2 (a PState may only be written by the topology that declared it)
            // holds -- this is not testing an IllegalWriteException, it's testing what
            // happens to a legally-scoped second write.
            stream.source("*probe-updates").out("*update")
              .select("*update", Path.key("familyId")).out("*familyId")
              .select("*update", Path.key("eventId")).out("*eventId")
              .select("*update", Path.key("newStatus")).out("*newStatus")
              .hashPartition("*familyId")
              .localTransform("$$probe-family-data",
                  Path.key("*familyId").key("events").key("*eventId").key("status").termVal("*newStatus"));
        }
    }

    private String moduleName;

    @BeforeAll
    void setup() throws Exception {
        ipc = InProcessCluster.create();
        ProbeModule module = new ProbeModule();
        ipc.launchModule(module, new LaunchConfig(1, 1));
        // Never hardcode a guessed module name -- ask the launched instance directly.
        moduleName = module.getModuleName();

        probeEventsDepot  = ipc.clusterDepot(moduleName, "*probe-events");
        probeUpdatesDepot = ipc.clusterDepot(moduleName, "*probe-updates");
        probeFamilyData   = ipc.clusterPState(moduleName, "$$probe-family-data");
    }

    @AfterAll
    void tearDown() throws Exception {
        if (ipc != null) ipc.close();
    }

    @Test
    void derivationsRoundTripThenPartialWrite() throws Exception {
        // ---- Build the derivations map exactly per the B1 design in
        // docs/decisions/PLAN_provenance_temporal.md (Fork 1b + the "basis" correction):
        //   derivations: { "classify": {...}, "extract-details": {...} }
        // Gate 4 discipline throughout: new HashMap<>()/new ArrayList<>() only, never
        // Map.of()/List.of() -- a test that violates the gate it's checking proves nothing.

        Map<String, Object> classifyDerivation = new HashMap<>();
        classifyDerivation.put("modelId", "gemini-2.5-flash");
        classifyDerivation.put("promptVersion", "a1b2c3d4e5f6");
        classifyDerivation.put("derivedAt", 1735689600000L);
        classifyDerivation.put("basis", "model");

        Map<String, Object> extractDerivation = new HashMap<>();
        // basis = keyword-fallback case: modelId deliberately null, per the plan doc's A1
        // finding that classifyByKeyword output must not claim a model produced it. Also
        // exercises the existing "null map values round-trip" precedent, one level deeper
        // than that entry tested (a null nested inside a nested map, not a top-level field).
        extractDerivation.put("modelId", (String) null);
        extractDerivation.put("promptVersion", "f6e5d4c3b2a1");
        extractDerivation.put("derivedAt", 1735689601500L);
        extractDerivation.put("basis", "keyword-fallback");

        Map<String, Object> derivations = new HashMap<>();
        derivations.put("classify", classifyDerivation);
        derivations.put("extract-details", extractDerivation);

        // ---- Build eventRecord the SAME way write-to-store actually does
        // (EmailParsingModule.java:438-479): one HashMap, sequential .put() calls in
        // plain Java (write-to-store is an agent node, it never touches a PState
        // directly), appended ONCE.
        Map<String, Object> eventRecord = new HashMap<>();
        eventRecord.put("id", EVENT_ID);
        eventRecord.put("familyId", FAMILY_ID);
        eventRecord.put("personId", new ArrayList<String>());
        eventRecord.put("tags", new ArrayList<>(List.of("SCHOOL_EVENT")));
        eventRecord.put("relations", new ArrayList<Map<String, String>>());
        eventRecord.put("title", "Probe event");
        eventRecord.put("description", "Derivations round-trip probe");
        eventRecord.put("emailSubject", "");
        eventRecord.put("created", 1735689600000L);
        eventRecord.put("updated", 1735689600000L);
        eventRecord.put("derivations", derivations);

        // ---- Write #1: mirrors write-to-store -> $$family-data exactly (single
        // whole-record termVal). AckLevel.ACK (RAMA_VERIFIED_LEARNINGS.md:165-169) waits
        // for the colocated stream topology to finish processing, so a clean return here
        // means the write genuinely landed, not just got queued.
        probeEventsDepot.append(eventRecord, AckLevel.ACK);

        // ---- Read back and assert types at every nesting level -- not just non-null.
        @SuppressWarnings("unchecked")
        Map<String, Object> storedRecord = (Map<String, Object>)
            probeFamilyData.selectOne(Path.key(FAMILY_ID).key("events").key(EVENT_ID));

        assertNotNull(storedRecord, "Record must be present after write #1");
        assertInstanceOf(HashMap.class, storedRecord, "Stored record must deserialize as a HashMap");
        assertInstanceOf(String.class, storedRecord.get("id"));
        assertEquals(EVENT_ID, storedRecord.get("id"));

        Object derivationsBack = storedRecord.get("derivations");
        assertNotNull(derivationsBack, "derivations must survive the round trip, not silently drop");
        assertInstanceOf(Map.class, derivationsBack, "derivations must read back as a Map, not e.g. a String");
        @SuppressWarnings("unchecked")
        Map<String, Object> derivationsMap = (Map<String, Object>) derivationsBack;
        assertEquals(2, derivationsMap.size(), "both node entries must survive");

        Object classifyBack = derivationsMap.get("classify");
        assertInstanceOf(Map.class, classifyBack, "derivations.classify must read back as a Map, not a String/other type");
        @SuppressWarnings("unchecked")
        Map<String, Object> classifyMap = (Map<String, Object>) classifyBack;
        assertInstanceOf(String.class, classifyMap.get("modelId"));
        assertEquals("gemini-2.5-flash", classifyMap.get("modelId"));
        assertInstanceOf(String.class, classifyMap.get("promptVersion"));
        assertEquals("a1b2c3d4e5f6", classifyMap.get("promptVersion"));
        assertInstanceOf(Long.class, classifyMap.get("derivedAt"));
        assertEquals(1735689600000L, classifyMap.get("derivedAt"));
        assertInstanceOf(String.class, classifyMap.get("basis"));
        assertEquals("model", classifyMap.get("basis"));

        Object extractBack = derivationsMap.get("extract-details");
        assertInstanceOf(Map.class, extractBack, "derivations.extract-details must read back as a Map");
        @SuppressWarnings("unchecked")
        Map<String, Object> extractMap = (Map<String, Object>) extractBack;
        assertTrue(extractMap.containsKey("modelId"), "the key itself must still be present, not dropped, even though its value is null");
        assertNull(extractMap.get("modelId"), "null modelId (keyword-fallback case) must round-trip as null, not coerce to a string/sentinel");
        assertInstanceOf(String.class, extractMap.get("promptVersion"));
        assertInstanceOf(Long.class, extractMap.get("derivedAt"));
        assertInstanceOf(String.class, extractMap.get("basis"));
        assertEquals("keyword-fallback", extractMap.get("basis"));

        // ---- Write #2: a second, separate, narrower write into a DIFFERENT top-level
        // key ("status") of the SAME already-stored record. This is the exact shape
        // RAMA_VERIFIED_LEARNINGS.md's "Real failure mode the probe caught" entry already
        // proved throws for a record stored via whole-record termVal:
        //   java.lang.ClassCastException: class java.util.HashMap cannot be cast to class
        //   clojure.lang.Associative
        // Confirming it reproduces here -- for $$family-data's actual schema shape and a
        // record that actually carries a derivations map -- rather than assuming the
        // prior generic probe's finding transfers unchanged.
        Map<String, Object> update = new HashMap<>();
        update.put("familyId", FAMILY_ID);
        update.put("eventId", EVENT_ID);
        update.put("newStatus", "reviewed");

        Throwable topLevel = null;
        try {
            probeUpdatesDepot.append(update, AckLevel.ACK);
        } catch (Exception e) {
            topLevel = e;
        }

        if (topLevel == null) {
            // AckLevel.ACK's exception-propagation behavior on a downstream topology
            // failure is itself unverified (RAMA_VERIFIED_LEARNINGS.md:165-169 flags this
            // exact gap) -- if nothing was thrown, check whether the write silently
            // failed instead of assuming success.
            Thread.sleep(2000);
            @SuppressWarnings("unchecked")
            Map<String, Object> afterSecondWrite = (Map<String, Object>)
                probeFamilyData.selectOne(Path.key(FAMILY_ID).key("events").key(EVENT_ID));
            fail("Write #2 did not throw ClassCastException as expected. Record after write #2: "
                + afterSecondWrite);
        }

        // Empirically (this run), the ClassCastException does not surface as a bare
        // java.lang.ClassCastException on the caller's thread, and it is NOT reachable
        // via getCause() either -- topLevel is
        // rpl.rama.distributed.exceptions.CallbackException, a clojure.lang.ExceptionInfo
        // subclass that carries the underlying failure as ex-data rather than a chained
        // Throwable (confirmed by javap: CallbackException overrides toString() and its
        // getCause() chain is empty here). The ClassCastException text IS present inside
        // that rendered ex-data. Search the full toString()/getMessage() text rather than
        // assuming standard Java cause-chaining -- Rama's own exception shape, verified
        // empirically, not guessed.
        String fullText = topLevel.toString() + " | cause-chain: ";
        Throwable cursor = topLevel;
        while (cursor != null) {
            fullText += cursor.getClass().getName() + ": " + cursor.getMessage() + " -> ";
            cursor = cursor.getCause();
        }

        boolean foundSpecificFailure = fullText.contains("ClassCastException")
            && fullText.contains("clojure.lang.Associative");

        // Known result from the prior probe session (see RAMA_VERIFIED_LEARNINGS.md's
        // "AckLevel.ACK does not surface the Gate 3 ClassCastException..." entry):
        // foundSpecificFailure is expected to be false -- the caller never sees the
        // specific exception. Recorded here, not fail()'d, so Write #3 below still runs
        // regardless of this outcome. Write #2's own write/detection logic above is
        // otherwise byte-for-byte unchanged from the prior session.
        String write2Note = foundSpecificFailure
            ? "Write #2: specific ClassCastException text WAS found in caller-visible output (unexpected vs. prior session)."
            : "Write #2: as previously documented, the specific failure mode was NOT found in "
                + "caller-visible text. Top-level: " + topLevel.getClass().getName()
                + " -- full text: " + fullText;
        System.out.println("[DerivationsPartialWriteProbeTest] " + write2Note);

        // ---- Write #3: a full-record REPLACEMENT termVal at the SAME top-level path used
        // by Write #1 (Path.key(familyId).key("events").key(eventId).termVal(...)) -- not a
        // narrower key like Write #2's ".key(\"status\")". Tests the "full replace, not
        // assoc" theory: does Branch 1's write path still work a second time against a
        // record whose underlying container Write #2 already tried (and, per the worker
        // log, failed) to assoc into? If the record was left in some poisoned/partial state
        // by Write #2's crash, this should fail too, regardless of write shape.
        Map<String, Object> classifyDerivationV2 = new HashMap<>();
        classifyDerivationV2.put("modelId", "gemini-2.5-flash");
        classifyDerivationV2.put("promptVersion", "REPLACED-v2-hash");
        classifyDerivationV2.put("derivedAt", 1735689699999L);
        classifyDerivationV2.put("basis", "model");

        Map<String, Object> derivationsV2 = new HashMap<>();
        derivationsV2.put("classify", classifyDerivationV2);
        // Deliberately drop "extract-details" and add a new key, so the read-back can only
        // match this new map, never a leftover of Write #1's or a merge of the two.
        Map<String, Object> reclassifyDerivation = new HashMap<>();
        reclassifyDerivation.put("modelId", "gemini-2.5-flash");
        reclassifyDerivation.put("promptVersion", "REPLACED-v2-hash-2");
        reclassifyDerivation.put("derivedAt", 1735689700000L);
        reclassifyDerivation.put("basis", "model");
        derivationsV2.put("reclassify", reclassifyDerivation);

        Map<String, Object> eventRecordV2 = new HashMap<>();
        eventRecordV2.put("id", EVENT_ID);
        eventRecordV2.put("familyId", FAMILY_ID);
        eventRecordV2.put("personId", new ArrayList<String>());
        eventRecordV2.put("tags", new ArrayList<>(List.of("SCHOOL_EVENT", "REPLACED")));
        eventRecordV2.put("relations", new ArrayList<Map<String, String>>());
        eventRecordV2.put("title", "Probe event (replaced by write #3)");
        eventRecordV2.put("description", "Full-record replacement probe");
        eventRecordV2.put("emailSubject", "");
        eventRecordV2.put("created", 1735689600000L);
        eventRecordV2.put("updated", 1735689700000L);
        eventRecordV2.put("derivations", derivationsV2);

        Throwable write3Exception = null;
        try {
            // Same depot as Write #1 -- routes through Branch 1's identical whole-record
            // termVal write at the identical path, this time against an eventId that
            // already has a stored value (and which Write #2 already tried to assoc into).
            probeEventsDepot.append(eventRecordV2, AckLevel.ACK);
        } catch (Exception e) {
            write3Exception = e;
        }

        if (write3Exception != null) {
            String w3Text = write3Exception.toString();
            Throwable w3Cursor = write3Exception.getCause();
            while (w3Cursor != null) {
                w3Text += " | caused by: " + w3Cursor.getClass().getName() + ": " + w3Cursor.getMessage();
                w3Cursor = w3Cursor.getCause();
            }
            fail("Write #3 (full-record replacement termVal, same path as Write #1) threw. "
                + "Theory ('full replace, not assoc, should succeed') is WRONG, or the record "
                + "was left poisoned by Write #2. Exact exception: " + w3Text
                + " || " + write2Note);
        }

        // No exception -- confirm it's a genuine replacement, not a silent no-op, by
        // reading back and checking the NEW derivations content specifically.
        @SuppressWarnings("unchecked")
        Map<String, Object> afterThirdWrite = (Map<String, Object>)
            probeFamilyData.selectOne(Path.key(FAMILY_ID).key("events").key(EVENT_ID));

        assertNotNull(afterThirdWrite, "Record must still be present after write #3");
        assertEquals("Probe event (replaced by write #3)", afterThirdWrite.get("title"),
            "title must reflect write #3's new value, not write #1's original value");

        Object derivationsV2Back = afterThirdWrite.get("derivations");
        assertInstanceOf(Map.class, derivationsV2Back, "derivations must still read back as a Map after write #3");
        @SuppressWarnings("unchecked")
        Map<String, Object> derivationsV2Map = (Map<String, Object>) derivationsV2Back;
        assertEquals(2, derivationsV2Map.size(), "must reflect write #3's key set exactly (classify + reclassify), not a merge with write #1's (classify + extract-details)");
        assertTrue(derivationsV2Map.containsKey("reclassify"), "must contain write #3's new 'reclassify' key");
        assertFalse(derivationsV2Map.containsKey("extract-details"), "must NOT contain write #1's 'extract-details' key -- this must be a replace, not a merge");

        @SuppressWarnings("unchecked")
        Map<String, Object> classifyV2Back = (Map<String, Object>) derivationsV2Map.get("classify");
        assertEquals("REPLACED-v2-hash", classifyV2Back.get("promptVersion"),
            "classify.promptVersion must reflect write #3's new value, not write #1's original value");

        System.out.println("[DerivationsPartialWriteProbeTest] Write #3 (full-record replacement "
            + "termVal, same path as write #1) SUCCEEDED and verified as a genuine replace, not a "
            + "silent no-op or merge. || " + write2Note);
    }
}
