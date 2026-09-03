package com.family.assistant.schema;

import com.family.assistant.util.EventUtils;
import com.rpl.rama.Block;
import com.rpl.rama.Depot;
import com.rpl.rama.Expr;
import com.rpl.rama.PState;
import com.rpl.rama.Path;
import com.rpl.rama.RamaModule;
import com.rpl.rama.ops.Ops;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.UUID;

/**
 * FamilySchemaModule
 *
 * Plain RamaModule — owns the $$family-data PState and *family-events depot.
 * Other modules append to *family-events via getMirrorDepot(); the stream
 * topology drains it into $$family-data and the inverted indexes.
 *
 * Schema:
 *   $$family-data        — familyId -> { "events" -> { eventId -> { ...record... } } }
 *   $$raw-emails         — familyId -> gmailMessageId -> { ...complete raw email... }
 *                          (write-ahead log; the *raw-emails depot is the durable
 *                           replay source, this PState is the inspectable view)
 *   $$events-by-person   — familyId -> personId (family member; a record carries a
 *                          List<String>, so this index is fanned out one entry per element) -> Set<eventId>
 *   $$events-by-tag      — familyId -> tag (a record carries a List<String>, so this index
 *                          is fanned out one entry per element) -> Set<eventId>
 *   $$events-by-account  — familyId -> accountLabel -> Set<eventId>
 *   $$events-by-date     — familyId -> epochMs (sorted) -> Set<eventId>
 *   $$events-by-silo     — familyId -> silo (VAULT/OFFICE/STUDIO/UNKNOWN) -> Set<eventId>
 *   $$events-by-intent   — familyId -> intent (ACTION_REQUIRED/DECISION_NEEDED/FYI/SCHEDULING/UNKNOWN) -> Set<eventId>
 *   $$events-by-keyword  — familyId -> keyword (tokenized title+description+emailSubject) -> Set<eventId>
 *   $$edges-forward      — familyId -> subjectId(eventId) -> relation (MENTIONS_PERSON/PART_OF/
 *                          LOCATED_AT/ACTION_NEEDED/UNKNOWN) -> Set<objectId>
 *   $$edges-inverse      — familyId -> objectId -> relation -> Set<subjectId>(eventId)
 *   $$entities           — familyId -> entityId -> { "type"->String, "canonicalName"->String,
 *                          "aliases"->Set<String> }. entityId = hash(eventId|objectType|object) —
 *                          one row per distinct mention, not deduped across events; aliases starts
 *                          empty, populated only by a future entity-resolution effort.
 *   $$entities-by-type   — familyId -> entityType (PERSON/ORG/PLACE/PROJECT/UNKNOWN) -> Set<entityId>
 *   $$leverage-map       — familyId -> entryId -> { "silo"->String|null, "intent"->String|null, "weight"->Long }
 *   $$weakness-map       — familyId -> entryId -> { "silo"->String|null, "intent"->String|null, "tag"->String, "note"->String }
 *
 * Config records (weakness-map / leverage-map):
 *   *weakness-leverage-config depot carries records shaped
 *   { "familyId"->String, "mapType"->"LEVERAGE"|"WEAKNESS", "entryId"->String, "entry"->Map }.
 *   silo/intent on an entry are wildcards when null/absent — DigestModule treats a
 *   missing dimension as "matches any value" for that dimension.
 */
public class FamilySchemaModule implements RamaModule, java.io.Serializable {

    /** Returns true when a String is non-null and non-blank. */
    private static boolean isPresent(String s) {
        return s != null && !s.isBlank();
    }

    private static boolean isLeverageMapType(String mapType) {
        return "LEVERAGE".equals(mapType);
    }

    private static boolean isWeaknessMapType(String mapType) {
        return "WEAKNESS".equals(mapType);
    }

    /** Returns startTime if set, deadline if set, or null if neither is present. */
    private static Long effectiveTime(Map<String, Object> record) {
        Object st = record.get("startTime");
        if (st instanceof Long) return (Long) st;
        Object dl = record.get("deadline");
        if (dl instanceof Long) return (Long) dl;
        return null;
    }

    /**
     * Deterministic per-mention entity ID: hash(eventId|objectType|object), no mention
     * index. Two different relations targeting the same object+type within one event
     * collapse to the same entityId (a single $$entities row, not one per relation-slot);
     * two different events mentioning the same object+type still mint different entityIds
     * until a future entity-resolution effort merges them. Must be nameUUIDFromBytes, never
     * randomUUID — Rama requires deterministic processing for PStates recomputed from depot
     * data (verified: redplanetlabs.com/docs/~/operating-rama.html, "Task scaling" section).
     */
    private static String mintEntityId(String eventId, String objectType, String object) {
        String key = eventId + "|" + objectType + "|" + object;
        return UUID.nameUUIDFromBytes(key.getBytes(StandardCharsets.UTF_8)).toString();
    }

    /** Entity record shape for $$entities. aliases starts empty — populated only by a future resolution effort. */
    private static Map<String, Object> buildEntityRecord(String objectType, String object) {
        Map<String, Object> entity = new HashMap<>();
        entity.put("type", objectType);
        entity.put("canonicalName", object);
        entity.put("aliases", new HashSet<String>());
        return entity;
    }

    /** Layer 2 commitments seed from ACTION_NEEDED edges only (Fork 3) — intent stays untouched. */
    private static boolean isActionNeeded(String relation) {
        return "ACTION_NEEDED".equals(relation);
    }

    /** True when a localSelect read found nothing at that PState path. */
    private static boolean isAbsent(Object existing) {
        return existing == null;
    }

    /**
     * True when a localSelect read found an existing record at that PState path. Guards the
     * status-change branch: a status change for a commitmentId never seeded by the
     * ACTION_NEEDED creation branch must be dropped, not auto-vivified into a permanent stub
     * with an unbounded key-space (see EDGE_CODE_RULES.md Gate 6 / REASONING.md 2026-07-17).
     */
    private static boolean isPresentRecord(Object existing) {
        return existing != null;
    }

    /**
     * Deterministic per-edge commitment ID: hash(sourceEventId|relation|objectId), same
     * nameUUIDFromBytes discipline as mintEntityId — redraining *family-events* must
     * regenerate the identical ID for the identical ACTION_NEEDED edge, or every redrain
     * would mint a duplicate commitment instead of reconciling with the existing one.
     * Two different edges describing the same real-world commitment still mint different
     * IDs (Fork 5) — entity-resolution-shaped, deferred, not solved here.
     */
    private static String mintCommitmentId(String sourceEventId, String relation, String objectId) {
        String key = sourceEventId + "|" + relation + "|" + objectId;
        return UUID.nameUUIDFromBytes(key.getBytes(StandardCharsets.UTF_8)).toString();
    }

    @Override
    public String getModuleName() {
        return "FamilySchemaModule";
    }

    @Override
    public void define(Setup setup, Topologies topologies) {
        setup.declareDepot("*family-events", Depot.hashBy("familyId"));
        setup.declareDepot("*weakness-leverage-config", Depot.hashBy("familyId"));
        // Write-ahead log for raw ingested emails. hashBy("familyId") keeps the raw
        // record co-partitioned with *family-events; requires the appended record to
        // be a Map carrying a "familyId" key (Chat-o-rama 2026-03-11: the hashBy(String)
        // overload looks up the key from the record, which must implement Map).
        setup.declareDepot("*raw-emails", Depot.hashBy("familyId"));
        // Layer 2 commitments: the only permanent, append-only record in this layer — a
        // commitment's CREATION is recomputed every redrain (not a depot record; see the
        // ACTION_NEEDED branch below), but a status change is a real thing that happened
        // once and must never be regenerated or replayed differently. hashBy("familyId"),
        // never random, matching every other depot in this module (verified last session:
        // redplanetlabs.com/docs/~/depots.html, ordering is only guaranteed within a
        // partition — not that it's needed here, since this depot's branch never depends on
        // *family-events'* processing order, but consistency with the rest of the module's
        // partitioning is still the safe default).
        setup.declareDepot("*commitment-status-changes", Depot.hashBy("familyId"));
        // Ingestion failure records — EmailParsingModule's persist-raw/classify/
        // extract-details/write-to-store guards append here on a caught exception, after
        // LangChain4j's own retry budget (or the append itself) is exhausted. Immutable,
        // write-once by design: no field on a failure record is ever updated in place —
        // "resolved" is inferred externally (does a matching $$family-data record now exist
        // for the same gmailMessageId), same corrections-as-new-events doctrine as
        // $$family-data itself. hashBy("familyId"), matching every other depot here.
        setup.declareDepot("*ingestion-failures", Depot.hashBy("familyId"));

        var stream = topologies.stream("family-events-stream");
        var configStream = topologies.stream("weakness-leverage-config-stream");
        var rawStream = topologies.stream("raw-emails-stream");

        // Primary store
        stream.pstate("$$family-data",
            PState.mapSchema(String.class,
                PState.mapSchema(String.class,
                    PState.mapSchema(String.class, Object.class))));

        // Write-ahead log view: familyId -> gmailMessageId -> raw email record
        rawStream.pstate("$$raw-emails",
            PState.mapSchema(String.class,
                PState.mapSchema(String.class,
                    PState.mapSchema(String.class, Object.class))));

        // Inverted index: familyId -> personId -> Set<eventId>
        // (record's personId is a List<String>; the topology fans out one write per element)
        stream.pstate("$$events-by-person",
            PState.mapSchema(String.class,
                PState.mapSchema(String.class,
                    PState.setSchema(String.class))));

        // Inverted index: familyId -> tag -> Set<eventId>
        // (record's tags is a List<String>; the topology fans out one write per element)
        stream.pstate("$$events-by-tag",
            PState.mapSchema(String.class,
                PState.mapSchema(String.class,
                    PState.setSchema(String.class))));

        // Inverted index: familyId -> accountLabel -> Set<eventId>
        stream.pstate("$$events-by-account",
            PState.mapSchema(String.class,
                PState.mapSchema(String.class,
                    PState.setSchema(String.class))));

        // Inverted index: familyId -> silo -> Set<eventId>
        stream.pstate("$$events-by-silo",
            PState.mapSchema(String.class,
                PState.mapSchema(String.class,
                    PState.setSchema(String.class))));

        // Inverted index: familyId -> intent -> Set<eventId>
        stream.pstate("$$events-by-intent",
            PState.mapSchema(String.class,
                PState.mapSchema(String.class,
                    PState.setSchema(String.class))));

        // Sorted date index: familyId -> epochMs -> Set<eventId>
        stream.pstate("$$events-by-date",
            PState.mapSchema(String.class,
                PState.mapSchema(Long.class,
                    PState.setSchema(String.class)
                ).subindexed()));

        // Inverted index: familyId -> keyword -> Set<eventId>
        stream.pstate("$$events-by-keyword",
            PState.mapSchema(String.class,
                PState.mapSchema(String.class,
                    PState.setSchema(String.class))));

        // Typed relation edge, forward direction: familyId -> subjectId(eventId) -> relation -> Set<objectId>
        stream.pstate("$$edges-forward",
            PState.mapSchema(String.class,
                PState.mapSchema(String.class,
                    PState.mapSchema(String.class,
                        PState.setSchema(String.class)))));

        // Typed relation edge, inverse direction: familyId -> objectId -> relation -> Set<subjectId>(eventId)
        stream.pstate("$$edges-inverse",
            PState.mapSchema(String.class,
                PState.mapSchema(String.class,
                    PState.mapSchema(String.class,
                        PState.setSchema(String.class)))));

        // Entity foundation: familyId -> entityId -> { type, canonicalName, aliases }
        stream.pstate("$$entities",
            PState.mapSchema(String.class,
                PState.mapSchema(String.class,
                    PState.mapSchema(String.class, Object.class))));

        // Inverted index: familyId -> entityType -> Set<entityId> — makes the UNKNOWN bucket
        // inspectable (a real indexed queue) rather than requiring a full $$entities scan.
        stream.pstate("$$entities-by-type",
            PState.mapSchema(String.class,
                PState.mapSchema(String.class,
                    PState.setSchema(String.class))));

        // Layer 2 commitments: familyId -> commitmentId -> {sourceEventId, objectId,
        // createdAt, status, updatedAt}. Seeded ONLY from ACTION_NEEDED edges (Fork 3).
        // sourceEventId/objectId/createdAt are content — always refreshed on redrain;
        // status is write-once via a separate depot/branch (commitment-status-changes,
        // below) — see the ACTION_NEEDED branch's comment for the localSelect-guarded
        // write that protects it. No $$commitments-by-status index this session —
        // that's Layer 3 scanning infrastructure, deliberately deferred.
        stream.pstate("$$commitments",
            PState.mapSchema(String.class,
                PState.mapSchema(String.class,
                    PState.mapSchema(String.class, Object.class))));

        // Ingestion failures: familyId -> failureId -> {gmailMessageId, failedNode,
        // exceptionType, exceptionMessage, failedAt, ...}. Two levels (familyId -> id ->
        // record), matching $$commitments' shape, not $$family-data's three-level
        // (familyId -> "events" -> id -> record) — the extra "events" level there exists to
        // leave room for other categories under one family, which doesn't apply here.
        // Written ONLY by EmailParsingModule's guard sites via *ingestion-failures below.
        // Immutable, write-once (see the depot declaration's comment) — a single whole-record
        // termVal, same as $$family-data, NOT $$commitments' sequential-localTransform
        // pattern, since nothing here ever targets a narrower key within an already-written
        // record. Gate 3 never gets triggered for this PState, by construction.
        stream.pstate("$$ingestion-failures",
            PState.mapSchema(String.class,
                PState.mapSchema(String.class, Object.class)));

        // Config: familyId -> entryId -> leverage entry (silo/intent -> weight)
        configStream.pstate("$$leverage-map",
            PState.mapSchema(String.class,
                PState.mapSchema(String.class,
                    PState.mapSchema(String.class, Object.class))));

        // Config: familyId -> entryId -> weakness entry (silo/intent -> tag/note)
        configStream.pstate("$$weakness-map",
            PState.mapSchema(String.class,
                PState.mapSchema(String.class,
                    PState.mapSchema(String.class, Object.class))));

        stream.source("*family-events").out("*record")
          .select("*record", Path.key("familyId")).out("*familyId")
          .select("*record", Path.key("id")).out("*eventId")
          .select("*record", Path.key("created")).out("*eventCreatedAt")
          .hashPartition("*familyId")
          // Write primary store
          .localTransform("$$family-data",
              Path.key("*familyId").key("events").key("*eventId").termVal("*record"))
          // Extract accountLabel and conditionally write account index
          .select("*record", Path.key("accountLabel")).out("*accountLabel")
          .ifTrue(new Expr(FamilySchemaModule::isPresent, "*accountLabel"),
              Block.localTransform("$$events-by-account",
                  Path.key("*familyId").key("*accountLabel").nullToSet().voidSetElem().termVal("*eventId")))
          // Extract silo and conditionally write silo index (UNKNOWN is a valid, present value and IS indexed)
          .select("*record", Path.key("silo")).out("*silo")
          .ifTrue(new Expr(FamilySchemaModule::isPresent, "*silo"),
              Block.localTransform("$$events-by-silo",
                  Path.key("*familyId").key("*silo").nullToSet().voidSetElem().termVal("*eventId")))
          // Extract intent and conditionally write intent index (UNKNOWN is a valid, present value and IS indexed)
          .select("*record", Path.key("intent")).out("*intent")
          .ifTrue(new Expr(FamilySchemaModule::isPresent, "*intent"),
              Block.localTransform("$$events-by-intent",
                  Path.key("*familyId").key("*intent").nullToSet().voidSetElem().termVal("*eventId")))
          // Compute effective time (startTime ?? deadline) and write sorted date index
          .select("*record", Path.key("startTime")).out("*startTime")
          .select("*record", Path.key("deadline")).out("*deadline")
          .macro(Block.each(FamilySchemaModule::effectiveTime, "*record").out("*epochMs"))
          .ifTrue(new Expr((Long t) -> t != null, "*epochMs"),
              Block.localTransform("$$events-by-date",
                  Path.key("*familyId", "*epochMs")
                      .nullToSet().voidSetElem().termVal("*eventId")))
          // --- Multi-valued inverted indexes: tags, personId, keywords ---
          // Each is List-valued on the record, so each write fans out one entry per
          // element via Ops.EXPLODE. Operations after an EXPLODE run once per exploded
          // element, so three sequential explodes on the same branch would
          // cartesian-multiply. anchor/hook isolates each fan-out as an independent
          // branch off the same input node, keeping index membership correct.
          // (verified: redplanetlabs.com/docs ~ intermediate-dataflow, anchor/hook.)
          .anchor("fanoutRoot")
          // Inverted index: familyId -> tag -> Set<eventId>
          .select("*record", Path.key("tags")).out("*tags")
          .macro(Block.each(Ops.EXPLODE, "*tags").out("*tag"))
          .localTransform("$$events-by-tag",
              Path.key("*familyId").key("*tag").nullToSet().voidSetElem().termVal("*eventId"))
          .hook("fanoutRoot")
          // Inverted index: familyId -> personId -> Set<eventId>
          .select("*record", Path.key("personId")).out("*persons")
          .macro(Block.each(Ops.EXPLODE, "*persons").out("*person"))
          .localTransform("$$events-by-person",
              Path.key("*familyId").key("*person").nullToSet().voidSetElem().termVal("*eventId"))
          .hook("fanoutRoot")
          // Typed relation edges + entity foundation. "relations" is a List<Map> on the record
          // (absent on records with no parser-side extraction; EXPLODE on an absent/empty list
          // is a no-op, same as tags/personId above). One EXPLODE, then plain sequential writes
          // off the same exploded triple — not a second EXPLODE, so no further anchor/hook
          // isolation is needed within this branch (see the invariant explained above).
          .select("*record", Path.key("relations")).out("*relations")
          .macro(Block.each(Ops.EXPLODE, "*relations").out("*triple"))
          .select("*triple", Path.key("relation")).out("*relation")
          .select("*triple", Path.key("objectType")).out("*objectType")
          .select("*triple", Path.key("object")).out("*object")
          .macro(Block.each(FamilySchemaModule::mintEntityId, "*eventId", "*objectType", "*object").out("*objectId"))
          .localTransform("$$edges-forward",
              Path.key("*familyId").key("*eventId").key("*relation").nullToSet().voidSetElem().termVal("*objectId"))
          .localTransform("$$edges-inverse",
              Path.key("*familyId").key("*objectId").key("*relation").nullToSet().voidSetElem().termVal("*eventId"))
          .macro(Block.each(FamilySchemaModule::buildEntityRecord, "*objectType", "*object").out("*entityRecord"))
          .localTransform("$$entities",
              Path.key("*familyId").key("*objectId").termVal("*entityRecord"))
          .localTransform("$$entities-by-type",
              Path.key("*familyId").key("*objectType").nullToSet().voidSetElem().termVal("*objectId"))
          // Layer 2 commitments: seed ONLY from ACTION_NEEDED edges (Fork 3) — MENTIONS_PERSON/
          // LOCATED_AT/etc. triples skip this block entirely. Creation is recomputed every
          // redrain (Fork 1): sourceEventId/objectId/createdAt are content, always refreshed;
          // status is set to OPEN only the first time this commitment is seen, and is
          // otherwise owned exclusively by the commitment-status-changes branch below. The
          // localSelect READS BEFORE any write below it in this same event, so it reflects
          // pre-event state — an event sees its own writes immediately (verified last
          // session: redplanetlabs.com/docs/~/pstates.html), so reading after writing would
          // always see "present" and never fire the OPEN-initialization branch.
          .ifTrue(new Expr(FamilySchemaModule::isActionNeeded, "*relation"),
              Block.each(FamilySchemaModule::mintCommitmentId, "*eventId", "*relation", "*objectId").out("*commitmentId")
                   .localSelect("$$commitments", Path.key("*familyId").key("*commitmentId")).out("*existingCommitment")
                   .ifTrue(new Expr(FamilySchemaModule::isAbsent, "*existingCommitment"),
                       Block.localTransform("$$commitments",
                           Path.key("*familyId").key("*commitmentId").key("status").termVal("OPEN")))
                   .localTransform("$$commitments",
                       Path.key("*familyId").key("*commitmentId").key("sourceEventId").termVal("*eventId"))
                   .localTransform("$$commitments",
                       Path.key("*familyId").key("*commitmentId").key("objectId").termVal("*objectId"))
                   .localTransform("$$commitments",
                       Path.key("*familyId").key("*commitmentId").key("createdAt").termVal("*eventCreatedAt")))
          .hook("fanoutRoot")
          // Tokenize title+description+emailSubject and fan out one write per token
          .macro(Block.each(EventUtils::tokenizeEvent, "*record").out("*tokens"))
          .macro(Block.each(Ops.EXPLODE, "*tokens").out("*token"))
          .localTransform("$$events-by-keyword",
              Path.key("*familyId").key("*token").nullToSet().voidSetElem().termVal("*eventId"));

        // Drain the raw-email write-ahead log into the inspectable $$raw-emails view.
        // Keyed by gmailMessageId (present on every real Gmail message). The depot
        // itself remains the complete, append-only replay source; records lacking a
        // gmailMessageId still land durably in the depot but are not surfaced here.
        rawStream.source("*raw-emails").out("*raw")
          .select("*raw", Path.key("familyId")).out("*familyId")
          .select("*raw", Path.key("gmailMessageId")).out("*rawId")
          .hashPartition("*familyId")
          .ifTrue(new Expr(FamilySchemaModule::isPresent, "*rawId"),
              Block.localTransform("$$raw-emails",
                  Path.key("*familyId").key("*rawId").termVal("*raw")));

        configStream.source("*weakness-leverage-config").out("*configRecord")
          .select("*configRecord", Path.key("familyId")).out("*familyId")
          .select("*configRecord", Path.key("entryId")).out("*entryId")
          .select("*configRecord", Path.key("entry")).out("*entry")
          .select("*configRecord", Path.key("mapType")).out("*mapType")
          .hashPartition("*familyId")
          .ifTrue(new Expr(FamilySchemaModule::isLeverageMapType, "*mapType"),
              Block.localTransform("$$leverage-map",
                  Path.key("*familyId").key("*entryId").termVal("*entry")))
          .ifTrue(new Expr(FamilySchemaModule::isWeaknessMapType, "*mapType"),
              Block.localTransform("$$weakness-map",
                  Path.key("*familyId").key("*entryId").termVal("*entry")));

        // Layer 2 commitments: the ONLY writer of "status"/"updatedAt" on $$commitments.
        // Genuinely permanent records — {commitmentId, familyId, newStatus, changedAt} — a
        // status change really happened once and must never be regenerated by a redrain
        // (unlike the ACTION_NEEDED branch's creation logic above).
        //
        // REVISED 2026-07-17 (gate-review revision C, EDGE_CODE_RULES.md Gate 6/Gate 8):
        // this branch now `localSelect`s the existing $$commitments record first and only
        // applies the status/updatedAt write if it's already present — a status change for a
        // commitmentId the ACTION_NEEDED branch never seeded is dropped, not auto-vivified.
        // Previously this was a plain unconditional partial write relying on Rama's
        // auto-vivify behavior to create a {status, updatedAt} stub for free — that stub
        // creation is exactly what this guard now prevents (see REASONING.md's 2026-07-17
        // entry: this supersedes the auto-vivify-stub behavior CommitmentsTest's test 6
        // originally asserted; the *separate* dropped-source-edge "ghost" case — a real
        // commitment whose ACTION_NEEDED edge later stops being extracted — is unaffected and
        // stays on the accepted-risk list, since that commitment DID exist at creation time).
        // $$commitments-by-status is still out of scope (Layer 3) — this guard only needs a
        // presence check, not the old status value, so it doesn't require that index either.
        // NOTE (2026-08-03 audit correction): an earlier version of this comment claimed
        // "actor is durably captured in this depot's own replay log but not projected into
        // $$commitments." That was never true — no append site has ever written an "actor"
        // field. The only producer is WebhookReceiver.markDone, whose payload is
        // {familyId, commitmentId, newStatus, changedAt} and nothing else. Actor attribution
        // (actor / actorBasis) is planned work, not existing behavior — see
        // docs/decisions/PLAN_provenance_temporal.md step B4.
        //
        // This MUST be a second .source(...) branch on the SAME "stream" topology object
        // that declared $$commitments (family-events-stream) — a PState can only be
        // written by the topology that declared it (confirmed the hard way: an earlier
        // attempt to declare $$commitments in "stream" but write it from a separate
        // "commitment-status-changes-stream" topology threw
        // IllegalWriteException{:topology-id family-events-stream,
        // :curr-topology-id commitment-status-changes-stream} at runtime, every single
        // append). Verified against redplanetlabs.com/docs/~/stream.html: a single
        // StreamTopology can consume multiple depots via successive .source(...) calls,
        // and "it's typical for each source block to modify the same PStates in different
        // ways" — no partitioning-match requirement between the two depots.
        stream.source("*commitment-status-changes").out("*statusChange")
          .select("*statusChange", Path.key("familyId")).out("*familyId")
          .select("*statusChange", Path.key("commitmentId")).out("*commitmentId")
          .select("*statusChange", Path.key("newStatus")).out("*newStatus")
          .select("*statusChange", Path.key("changedAt")).out("*changedAt")
          .hashPartition("*familyId")
          .localSelect("$$commitments", Path.key("*familyId").key("*commitmentId")).out("*existingCommitment")
          .ifTrue(new Expr(FamilySchemaModule::isPresentRecord, "*existingCommitment"),
              Block.localTransform("$$commitments",
                  Path.key("*familyId").key("*commitmentId").key("status").termVal("*newStatus"))
              .localTransform("$$commitments",
                  Path.key("*familyId").key("*commitmentId").key("updatedAt").termVal("*changedAt")));

        // Third .source(...) branch on the SAME "stream" topology object that declared
        // $$ingestion-failures above — same rule as the *commitment-status-changes branch's
        // comment: a PState can only be written by the topology that declared it. Pure
        // whole-record termVal, no localSelect/ifTrue guard needed — unlike the status-change
        // branch above, there is no "existing record" precondition here: every failure record
        // is either genuinely new or a legitimate overwrite of a prior identical-key failure
        // (same message, same node, redelivered) — both cases are correct as a plain
        // overwrite, matching $$family-data's own creation-write pattern.
        stream.source("*ingestion-failures").out("*failure")
          .select("*failure", Path.key("familyId")).out("*familyId")
          .select("*failure", Path.key("id")).out("*failureId")
          .hashPartition("*familyId")
          .localTransform("$$ingestion-failures",
              Path.key("*familyId").key("*failureId").termVal("*failure"));
    }
}