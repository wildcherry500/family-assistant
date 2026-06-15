package com.family.assistant.schema;

import com.rpl.rama.Block;
import com.rpl.rama.Depot;
import com.rpl.rama.Expr;
import com.rpl.rama.PState;
import com.rpl.rama.Path;
import com.rpl.rama.RamaModule;

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
 *   $$events-by-child    — familyId -> childName  -> Set<eventId>
 *   $$events-by-category — familyId -> eventType  -> Set<eventId>
 *   $$events-by-account  — familyId -> accountLabel -> Set<eventId>
 *   $$events-by-date     — familyId -> epochMs (sorted) -> Set<eventId>
 */
public class FamilySchemaModule implements RamaModule, java.io.Serializable {

    /** Returns true when a String is non-null and non-blank. */
    private static boolean isPresent(String s) {
        return s != null && !s.isBlank();
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

        var stream = topologies.stream("family-events-stream");

        // Primary store
        stream.pstate("$$family-data",
            PState.mapSchema(String.class,
                PState.mapSchema(String.class,
                    PState.mapSchema(String.class, Object.class))));

        // Inverted index: familyId -> childName -> Set<eventId>
        stream.pstate("$$events-by-child",
            PState.mapSchema(String.class,
                PState.mapSchema(String.class,
                    PState.setSchema(String.class))));

        // Inverted index: familyId -> eventType -> Set<eventId>
        stream.pstate("$$events-by-category",
            PState.mapSchema(String.class,
                PState.mapSchema(String.class,
                    PState.setSchema(String.class))));

        // Inverted index: familyId -> accountLabel -> Set<eventId>
        stream.pstate("$$events-by-account",
            PState.mapSchema(String.class,
                PState.mapSchema(String.class,
                    PState.setSchema(String.class))));

        // Sorted date index: familyId -> epochMs -> Set<eventId>
        stream.pstate("$$events-by-date",
            PState.mapSchema(String.class,
                PState.mapSchema(Long.class,
                    PState.setSchema(String.class)
                ).subindexed()));

        stream.source("*family-events").out("*record")
          .select("*record", Path.key("familyId")).out("*familyId")
          .select("*record", Path.key("id")).out("*eventId")
          .hashPartition("*familyId")
          // Write primary store
          .localTransform("$$family-data",
              Path.key("*familyId").key("events").key("*eventId").termVal("*record"))
          // Extract child name and event type
          .select("*record", Path.key("childName")).out("*childName")
          .select("*record", Path.key("eventType")).out("*eventType")
          // Conditionally write child index (only when childName is non-null and non-blank)
          .ifTrue(new Expr(FamilySchemaModule::isPresent, "*childName"),
              Block.localTransform("$$events-by-child",
                  Path.key("*familyId").key("*childName").nullToSet().voidSetElem().termVal("*eventId")))
          // Conditionally write category index (only when eventType is non-null and non-blank)
          .ifTrue(new Expr(FamilySchemaModule::isPresent, "*eventType"),
              Block.localTransform("$$events-by-category",
                  Path.key("*familyId").key("*eventType").nullToSet().voidSetElem().termVal("*eventId")))
          // Extract accountLabel and conditionally write account index
          .select("*record", Path.key("accountLabel")).out("*accountLabel")
          .ifTrue(new Expr(FamilySchemaModule::isPresent, "*accountLabel"),
              Block.localTransform("$$events-by-account",
                  Path.key("*familyId").key("*accountLabel").nullToSet().voidSetElem().termVal("*eventId")))
          // Compute effective time (startTime ?? deadline) and write sorted date index
          .select("*record", Path.key("startTime")).out("*startTime")
          .select("*record", Path.key("deadline")).out("*deadline")
          .macro(Block.each(FamilySchemaModule::effectiveTime, "*record").out("*epochMs"))
          .ifTrue(new Expr((Long t) -> t != null, "*epochMs"),
              Block.localTransform("$$events-by-date",
                  Path.key("*familyId").key("*epochMs")
                      .nullToSet().voidSetElem().termVal("*eventId")));
    }
}