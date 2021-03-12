package de.mig.mdr.model.handler.element;

import static de.mig.mdr.dal.jooq.Tables.ELEMENT;
import static de.mig.mdr.dal.jooq.Tables.IDENTIFIED_ELEMENT;

import de.mig.mdr.model.dto.element.Element;
import de.mig.mdr.model.dto.element.section.Identification;
import de.mig.mdr.model.handler.element.section.DefinitionHandler;
import de.mig.mdr.dal.jooq.enums.Status;
import de.mig.mdr.dal.jooq.tables.records.IdentifiedElementRecord;
import de.mig.mdr.model.handler.element.section.IdentificationHandler;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import org.jooq.DSLContext;

public abstract class ElementHandler {

  /**
   * Fetch a unique record that has <code>id = value</code>.
   */
  public static Element fetchOneByIdentification(DSLContext ctx, int userId,
      Identification identification) {

    if (identification == null) {
      return null;
    }

    IdentifiedElementRecord identifiedElementRecord = getIdentifiedElementRecord(
        ctx, identification);

    return convertToElement(ctx, identification, identifiedElementRecord);
  }

  /**
   * Get an identified element record by its identification.
   */
  public static IdentifiedElementRecord getIdentifiedElementRecord(DSLContext ctx,
      Identification identification) {
    return ctx.fetchOne(IDENTIFIED_ELEMENT,
        IDENTIFIED_ELEMENT.SI_IDENTIFIER.equal(identification.getIdentifier())
            .and(IDENTIFIED_ELEMENT.SI_VERSION
                .equal(identification.getRevision())
                .and(IDENTIFIED_ELEMENT.SI_NAMESPACE_ID.equal(identification.getNamespaceId()))
                .and(IDENTIFIED_ELEMENT.ELEMENT_TYPE.equal(identification.getElementType()))));
  }

  /**
   * Fetch a unique record that has <code>id = value</code>.
   */
  public static Element fetchOneByUrn(DSLContext ctx, int userId, String urn) {
    Identification identification = IdentificationHandler.fromUrn(urn);
    if (identification != null) {
      return fetchOneByIdentification(ctx, userId, identification);
    } else {
      return null;
    }
  }

  /**
   * Get an identified element record by its urn.
   */
  public static IdentifiedElementRecord getIdentifiedElementRecordByUrn(DSLContext ctx,
      String urn) {
    return getIdentifiedElementRecord(ctx, IdentificationHandler.fromUrn(urn));
  }

  /**
   * Get all elements from a list of urns.
   */
  public static List<Element> fetchByUrns(DSLContext ctx, int userId, List<String> urns) {
    List<Element> elements = new ArrayList<>();
    for (String urn : urns) {
      Element element = fetchOneByIdentification(ctx, userId, IdentificationHandler.fromUrn(urn));
      if (element != null) {
        elements.add(element);
      }
    }
    return elements;
  }

  /**
   * Convert an identified element record to an element.
   */
  public static Element convertToElement(DSLContext ctx, Identification identification,
      IdentifiedElementRecord identifiedElementRecord) {
    Element element = new Element();
    identification.setStatus(identifiedElementRecord.getSiStatus());
    element.setIdentification(identification);
    element.setDefinitions(DefinitionHandler
        .get(ctx, identifiedElementRecord.getId(), identifiedElementRecord.getSiId()));
    return element;
  }

  /**
   * Save an element in the database.
   */
  public static int saveElement(DSLContext ctx,
                                de.mig.mdr.dal.jooq.tables.pojos.Element element) {

    return ctx.insertInto(ELEMENT)
            .set(ctx.newRecord(ELEMENT, element))
            .returning(ELEMENT.ID)
            .fetchOne().getId();
  }


  /**
   * Outdates or deletes the given element. Depending on the status of the element. Drafts are
   * deleted, released elements are outdated.
   */
  public static void delete(DSLContext ctx, int userId, String urn) {
    Element element = fetchOneByUrn(ctx, userId, urn);

    if (element == null) {
      throw new NoSuchElementException(urn);
    }

    switch (element.getIdentification().getStatus()) {
      case DRAFT:
      case STAGED:
        IdentificationHandler
            .deleteDraftIdentifier(ctx, userId, element.getIdentification().getUrn());
        break;
      case RELEASED:
        IdentificationHandler
            .outdateIdentifier(ctx, userId, element.getIdentification().getUrn());
        break;
      case OUTDATED:
      default:
        throw new IllegalArgumentException();
    }
  }


  /**
   * Outdates or deletes the given element. Depending on the status of the element. Drafts are
   * deleted, released elements are outdated.
   */
  public static void delete(DSLContext ctx, int userId, Identification identification) {
    Element element = fetchOneByIdentification(ctx, userId, identification);

    if (element == null) {
      throw new NoSuchElementException();
    }

    switch (element.getIdentification().getStatus()) {
      case DRAFT:
      case STAGED:
        IdentificationHandler
            .deleteDraftIdentifier(ctx, userId, element.getIdentification().getUrn());
        break;
      case RELEASED:
        IdentificationHandler
            .outdateIdentifier(ctx, userId, element.getIdentification().getUrn());
        break;
      case OUTDATED:
      default:
        throw new IllegalArgumentException();
    }
  }
}
