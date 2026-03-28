package com.family.assistant.schema;

import com.rpl.rama.Block;
import com.rpl.rama.Depot;
import com.rpl.rama.Expr;
import com.rpl.rama.PState;
import com.rpl.rama.Path;
import com.rpl.rama.RamaModule;

/**
 * FamilySchemaModule
 *
 * Plain RamaModule — owns the $$family-data PState and *family-events depot.
 * Other modules append to *family-events via getMirrorDepot(); the stream
 * topology drains it into $$family-data and two inverted indexes.
 *
 * Schema:
 *   $$family-data        — familyId -> { "events" -> { eventId -> { ...record... } } }
 *   $$events-by-child    — familyId -> childName  -> Set<eventId>
 *   $$events-by-category — familyId -> eventType  -> Set<eventId>
 */
public class FamilySchemaModule implements RamaModule, java.io.Serializable {

    /** Returns true when a String is non-null and non-blank. */
    private static boolean isPresent(String s) {
        return s != null && !s.isBlank();
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
                  Path.key("*familyId").key("*eventType").nullToSet().voidSetElem().termVal("*eventId")));
    }
}