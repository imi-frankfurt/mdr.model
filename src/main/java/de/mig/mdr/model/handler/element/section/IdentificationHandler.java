package de.mig.mdr.model.handler.element.section;

import static de.mig.mdr.dal.jooq.Tables.ELEMENT;
import static de.mig.mdr.dal.jooq.Tables.SCOPED_IDENTIFIER;

import de.mig.mdr.model.dto.element.section.Identification;
import de.mig.mdr.model.handler.element.NamespaceHandler;
import de.mig.mdr.dal.ResourceManager;
import de.mig.mdr.dal.jooq.Routines;
import de.mig.mdr.dal.jooq.enums.ElementType;
import de.mig.mdr.dal.jooq.enums.Status;
import de.mig.mdr.dal.jooq.routines.GetNamespaceScopedIdentifierByUrn;
import de.mig.mdr.dal.jooq.routines.GetScopedIdentifierByUrn;
import de.mig.mdr.dal.jooq.tables.Element;
import de.mig.mdr.dal.jooq.tables.pojos.ScopedIdentifier;
import de.mig.mdr.dal.jooq.tables.records.IdentifiedElementRecord;
import de.mig.mdr.dal.jooq.tables.records.ScopedIdentifierRecord;
import java.util.UUID;
import org.jooq.DSLContext;
import org.jooq.impl.AbstractRoutine;
import org.jooq.impl.DSL;

public class IdentificationHandler {

  /**
   * Convert a ScopedIdentifier object of MDR DAL to a Identification object of MDR Model.
   */
  public static Identification convert(ScopedIdentifier scopedIdentifier) {
    if (scopedIdentifier == null || scopedIdentifier.getIdentifier() == null) {
      return null;
    }
    Identification identification = new Identification();
    identification.setElementType(scopedIdentifier.getElementType());
    identification.setNamespaceId(scopedIdentifier.getNamespaceId());
    identification.setStatus(scopedIdentifier.getStatus());
    identification.setIdentifier(scopedIdentifier.getIdentifier());
    identification.setRevision(scopedIdentifier.getVersion());
    identification.setUrn(toUrn(scopedIdentifier));
    return identification;
  }

  /**
   * Convert a Identification object of MDR Model to a ScopedIdentifier object of MDR DAL.
   */
  public static ScopedIdentifier convert(int userId, Identification identification, int elementId,
      int namespaceId) {
    ScopedIdentifier scopedIdentifier = new ScopedIdentifier();
    scopedIdentifier.setElementType(identification.getElementType());
    scopedIdentifier.setNamespaceId(namespaceId);
    scopedIdentifier.setStatus(identification.getStatus());
    scopedIdentifier.setIdentifier(identification.getIdentifier());
    scopedIdentifier.setElementId(elementId);
    scopedIdentifier.setCreatedBy(userId);
    scopedIdentifier.setUrl("none"); //TODO: delete?
    scopedIdentifier.setVersion(identification.getRevision());
    return scopedIdentifier;
  }

  /**
   * Returns the specified scoped identifier.
   */
  public static ScopedIdentifier getScopedIdentifier(DSLContext ctx, int userId, String urn) {
    GetScopedIdentifierByUrn procedure = new GetScopedIdentifierByUrn();
    procedure.setUrn(urn);
    procedure.execute(ctx.configuration());
    ScopedIdentifierRecord scopedIdentifierRecord = procedure.getReturnValue();
    return scopedIdentifierRecord.into(ScopedIdentifier.class);
  }

  /**
   * Returns the specified scoped identifier.
   */
  public static ScopedIdentifier getScopedIdentifier(DSLContext ctx, int userId,
      Identification identification) {
    return getScopedIdentifier(ctx, userId, identification.getUrn());
  }

  /**
   * Create a new ScopedIdentifier of MDR DAL with a given Identification of MDR Model.
   */
  public static ScopedIdentifier create(DSLContext ctx, int userId, Identification identification,
      int elementId) {

    ScopedIdentifier scopedIdentifier;
    if (identification.getElementType().equals(ElementType.NAMESPACE)) {
      //elementId is saved in scoped_Identifier.namespace_id cause column has Not-Null-Constraint
      // and identifier is created later
      scopedIdentifier =
          IdentificationHandler.convert(userId, identification, elementId, elementId);
    } else {
      IdentifiedElementRecord namespaceRecord = NamespaceHandler
          .getLatestNamespaceRecord(ctx, userId, identification.getNamespaceId());
      scopedIdentifier =
          IdentificationHandler.convert(userId, identification, elementId, namespaceRecord.getId());
    }

    // Set proper values for creating new elements TODO: check if it is right
    scopedIdentifier.setStatus(identification.getStatus());
    if (identification.getRevision() == null) {
      scopedIdentifier.setVersion(
          0);  //version = 0, when creating new elements so that the trigger in the db is called
    }
    if (scopedIdentifier.getUuid() == null) {
      scopedIdentifier.setUuid(UUID.randomUUID());
    }

    ScopedIdentifierRecord scopedIdentifierRecord = ctx
        .newRecord(SCOPED_IDENTIFIER, scopedIdentifier);
    scopedIdentifierRecord.store();
    scopedIdentifierRecord.refresh();
    scopedIdentifier.setIdentifier(ctx.selectFrom(SCOPED_IDENTIFIER)
        .where(SCOPED_IDENTIFIER.ID
            .eq(scopedIdentifierRecord.getId()))
        .fetchOne()
        .getValue(SCOPED_IDENTIFIER.IDENTIFIER));
    scopedIdentifier.setId(scopedIdentifierRecord.getId());
    scopedIdentifier.setVersion(scopedIdentifierRecord.getVersion());

    return scopedIdentifier;
  }


  /**
   * Returns a free identifier for the given namespace and element type.
   */
  public static String getFreeIdentifier(DSLContext ctx, Integer namespaceId, ElementType type) {
    Element ns = ELEMENT.as("ns");
    Integer max = ctx.select(DSL.max(
        DSL.when(SCOPED_IDENTIFIER.IDENTIFIER.likeRegex("^\\d+$"),
            SCOPED_IDENTIFIER.IDENTIFIER.cast(Integer.class))
            .else_(0)))
        .from(SCOPED_IDENTIFIER)
        .leftJoin(ns)
        .on(ns.ID.eq(SCOPED_IDENTIFIER.NAMESPACE_ID))
        .where(ns.ID.eq(namespaceId))
        .and(SCOPED_IDENTIFIER.ELEMENT_TYPE.eq(type))
        .fetchOne().value1();
    if (max != null) {
      return String.valueOf(max + 1);
    } else {
      return "1";
    }
  }

  /**
   * Updates a ScopedIdentifier of MDR DAL with a given Identification of MDR Model.
   */
  public static ScopedIdentifier update(DSLContext ctx, int userId, Identification identification,
      int elementId) {
    ScopedIdentifier scopedIdentifier = getScopedIdentifier(ctx, userId, identification);
    scopedIdentifier.setElementId(elementId);
    scopedIdentifier.setCreatedBy(userId);
    scopedIdentifier.setUrl("none");
    scopedIdentifier.setVersion(identification.getRevision() + 1);
    return scopedIdentifier;
  }


  /**
   * Updates a ScopedIdentifier of MDR DAL with a given Identification of MDR Model.
   */
  public static void updateStatus(DSLContext ctx, int userId, Identification identification,
      Status status) {
    Element ns = ELEMENT.as("ns");
    ctx.update(SCOPED_IDENTIFIER)
        .set(SCOPED_IDENTIFIER.STATUS, status)
        .where(SCOPED_IDENTIFIER.ID.in(
            ctx.select(SCOPED_IDENTIFIER.ID)
                .from(SCOPED_IDENTIFIER)
                .leftJoin(ns)
                .on(ns.ID.eq(SCOPED_IDENTIFIER.NAMESPACE_ID))
                .where(SCOPED_IDENTIFIER.IDENTIFIER.eq(identification.getIdentifier()))
                .and(SCOPED_IDENTIFIER.ELEMENT_TYPE.eq(identification.getElementType()))
                .and(SCOPED_IDENTIFIER.NAMESPACE_ID.eq(identification.getNamespaceId()))
                .and(SCOPED_IDENTIFIER.VERSION.eq(identification.getRevision()))))
        .execute();
  }

  /**
   * Updates a ScopedIdentifier of MDR DAL with a given Identification of MDR Model.
   */
  public static ScopedIdentifier updateNamespaceIdentifier(DSLContext ctx, int userId,
      Identification identification,
      int elementId) {
    ScopedIdentifier scopedIdentifier = ctx.select(SCOPED_IDENTIFIER.fields())
        .from(SCOPED_IDENTIFIER)
        .where(SCOPED_IDENTIFIER.ELEMENT_TYPE.equal(ElementType.NAMESPACE))
        .and(SCOPED_IDENTIFIER.IDENTIFIER.equal(identification.getIdentifier()))
        .and(SCOPED_IDENTIFIER.VERSION.equal(identification.getRevision()))
        .fetchOneInto(ScopedIdentifier.class);
    scopedIdentifier.setVersion(identification.getRevision() + 1);
    return scopedIdentifier;
  }

  /**
   * Convert a urn to a Identification object.
   */
  public static Identification fromUrn(String urn) {
    try (DSLContext ctx = ResourceManager.getDslContext()) {
      AbstractRoutine<ScopedIdentifierRecord> procedure;
      if (urn.toLowerCase().contains("namespace")) {
        procedure = new GetNamespaceScopedIdentifierByUrn();
        ((GetNamespaceScopedIdentifierByUrn) procedure).setUrn(urn);
      } else {
        procedure = new GetScopedIdentifierByUrn();
        ((GetScopedIdentifierByUrn) procedure).setUrn(urn);
      }
      procedure.execute(ctx.configuration());
      ScopedIdentifierRecord scopedIdentifierRecord = procedure.getReturnValue();
      ScopedIdentifier scopedIdentifier = scopedIdentifierRecord.into(ScopedIdentifier.class);
      return convert(scopedIdentifier);
    }
  }

  /**
   * Check if a String could be an urn. This only checks if the String has the correct "layout"
   */
  public static boolean isUrn(String urn) {
    if (urn == null) {
      return false;
    }
    String[] parts = urn.split(":");
    return parts.length == 5 && parts[0].equals("urn");
  }

  /**
   * Accept scopedIdentifier and return Urn.
   */
  public static String toUrn(ScopedIdentifier scopedIdentifier) {
    try (DSLContext ctx = ResourceManager.getDslContext()) {
      ScopedIdentifierRecord scopedIdentifierRecord = ctx
          .newRecord(SCOPED_IDENTIFIER, scopedIdentifier);
      return ctx.select(Routines.urn(scopedIdentifierRecord)).fetchOneInto(String.class);
    }
  }

  /**
   * Outdates the scoped identifier with the given URN.
   */
  public static void outdateIdentifier(DSLContext ctx, int userId, String urn) {
    Identification identification = IdentificationHandler.fromUrn(urn);
    Element ns = ELEMENT.as("ns");
    ctx.update(SCOPED_IDENTIFIER)
        .set(SCOPED_IDENTIFIER.STATUS, Status.OUTDATED)
        .where(SCOPED_IDENTIFIER.ID.in(
            ctx.select(SCOPED_IDENTIFIER.ID)
                .from(SCOPED_IDENTIFIER)
                .leftJoin(ns)
                .on(ns.ID.eq(SCOPED_IDENTIFIER.NAMESPACE_ID))
                .where(SCOPED_IDENTIFIER.IDENTIFIER.eq(identification.getIdentifier()))
                .and(SCOPED_IDENTIFIER.ELEMENT_TYPE.eq(identification.getElementType()))
                .and(SCOPED_IDENTIFIER.NAMESPACE_ID.eq(identification.getNamespaceId()))
                .and(SCOPED_IDENTIFIER.VERSION.eq(identification.getRevision()))))
        .execute();
    //TODO: catalogs!
  }


  /**
   * Deletes the scoped identifier with the given URN.
   */
  public static void deleteDraftIdentifier(DSLContext ctx, int userId, String urn)
      throws IllegalArgumentException {
    ScopedIdentifier scopedIdentifier = getScopedIdentifier(ctx, userId, urn);

    if (scopedIdentifier.getStatus() == Status.DRAFT
        || scopedIdentifier.getStatus() == Status.STAGED) {
      ctx.deleteFrom(SCOPED_IDENTIFIER)
          .where(SCOPED_IDENTIFIER.ID.eq(scopedIdentifier.getId()))
          .execute();
    } else {
      throw new IllegalArgumentException(
          "Identifier is not a draft. Call outdateIdentifier instead.");
    }
  }
}
