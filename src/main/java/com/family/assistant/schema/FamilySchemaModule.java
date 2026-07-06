package com.family.assistant.schema;

import com.family.assistant.util.EventUtils;
import com.rpl.rama.Block;
import com.rpl.rama.Depot;
import com.rpl.rama.Expr;
import com.rpl.rama.PState;
import com.rpl.rama.Path;
import com.rpl.rama.RamaModule;
import com.rpl.rama.ops.Ops;

import java.util.Map;

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
    }
}