package de.mig.mdr.model.handler.element;

import static de.mig.mdr.dal.jooq.Tables.ELEMENT;

import de.mig.mdr.model.CtxUtil;
import de.mig.mdr.model.DaoUtil;
import de.mig.mdr.model.dto.element.DataElement;
import de.mig.mdr.model.dto.element.Element;
import de.mig.mdr.model.dto.element.section.Identification;
import de.mig.mdr.model.handler.GrantTypeHandler;
import de.mig.mdr.model.handler.element.section.ConceptAssociationHandler;
import de.mig.mdr.model.handler.element.section.DefinitionHandler;
import de.mig.mdr.model.handler.element.section.SlotHandler;
import de.mig.mdr.model.handler.element.section.ValidationHandler;
import de.mig.mdr.dal.jooq.enums.ElementType;
import de.mig.mdr.dal.jooq.enums.GrantType;
import de.mig.mdr.dal.jooq.enums.Status;
import de.mig.mdr.dal.jooq.tables.pojos.ScopedIdentifier;
import de.mig.mdr.dal.jooq.tables.records.IdentifiedElementRecord;
import de.mig.mdr.model.handler.element.section.IdentificationHandler;
import java.util.NoSuchElementException;
import java.util.UUID;
import org.jooq.DSLContext;

public class DataElementHandler extends ElementHandler {

  /**
   * Create a new DataElement and return its new ID.
   */
  public static ScopedIdentifier create(DSLContext ctx, int userId, DataElement dataElement)
      throws IllegalAccessException {

    // Check if the user has the right to write to the namespace
    GrantType grantType = GrantTypeHandler
        .getGrantTypeByUserAndNamespace(ctx, userId,
            dataElement.getIdentification().getNamespaceId());
    if (!DaoUtil.WRITE_ACCESS_GRANTS.contains(grantType)) {
      throw new IllegalAccessException("User has no write access to namespace.");
    }

    final boolean autoCommit = CtxUtil.disableAutoCommit(ctx);

    de.mig.mdr.dal.jooq.tables.pojos.Element domain = ValidationHandler
        .create(ctx, dataElement.getValidation());

    de.mig.mdr.dal.jooq.tables.pojos.Element element
        = new de.mig.mdr.dal.jooq.tables.pojos.Element();
    element.setElementId(domain.getId());
    element.setElementType(ElementType.DATAELEMENT);
    if (element.getUuid() == null) {
      element.setUuid(UUID.randomUUID());
    }

    element.setId(saveElement(ctx, element));

    ScopedIdentifier scopedIdentifier =
        IdentificationHandler.create(ctx, userId, dataElement.getIdentification(), element.getId());
    DefinitionHandler.create(ctx, dataElement.getDefinitions(), element.getId(),
        scopedIdentifier.getId());
    if (dataElement.getSlots() != null) {
      SlotHandler.create(ctx, dataElement.getSlots(), scopedIdentifier.getId());
    }
    if (dataElement.getConceptAssociations() != null) {
      ConceptAssociationHandler
          .save(ctx, dataElement.getConceptAssociations(), userId, scopedIdentifier.getId());
    }

    CtxUtil.commitAndSetAutoCommit(ctx, autoCommit);
    return scopedIdentifier;
  }

  /**
   * TODO.
   */
  public static DataElement get(DSLContext ctx, int userId, String urn) {
    Identification identification = IdentificationHandler.fromUrn(urn);
    if (identification == null) {
      throw new NoSuchElementException(urn);
    }
    IdentifiedElementRecord identifiedElementRecord = ElementHandler
        .getIdentifiedElementRecord(ctx, identification);
    Element element = ElementHandler.convertToElement(ctx, identification, identifiedElementRecord);
    element.getIdentification().setNamespaceId(Integer.parseInt(urn.split(":")[1]));

    DataElement dataElement = new DataElement();
    dataElement.setIdentification(element.getIdentification());
    dataElement.setDefinitions(element.getDefinitions());
    dataElement.setValidation(ValidationHandler.get(ctx, identifiedElementRecord));
    dataElement.setSlots(SlotHandler.get(ctx, element.getIdentification()));
    dataElement
        .setConceptAssociations(ConceptAssociationHandler.get(ctx, element.getIdentification()));
    return dataElement;
  }

  /**
   * Save an element.
   */
  public static int saveElement(DSLContext ctx,
      de.mig.mdr.dal.jooq.tables.pojos.Element dataElement) {

    return ctx.insertInto(ELEMENT)
        .set(ctx.newRecord(ELEMENT, dataElement))
        .returning(ELEMENT.ID)
        .fetchOne().getId();
  }

  /**
   * Update a dataelement.
   */
  public static Identification update(DSLContext ctx, int userId, DataElement dataElement)
      throws IllegalAccessException {
    DataElement previousDataElement = get(ctx, userId, dataElement.getIdentification().getUrn());

    // If the validation differs, an update is not allowed.
    if (!previousDataElement.getValidation().equals(dataElement.getValidation())) {
      throw new UnsupportedOperationException("Validation changes are not allowed during update.");
    }

    //update scopedIdentifier if status != DRAFT
    if (previousDataElement.getIdentification().getStatus() != Status.DRAFT) {

      ScopedIdentifier scopedIdentifier =
          IdentificationHandler.update(ctx, userId, dataElement.getIdentification(),
              ElementHandler.getIdentifiedElementRecord(ctx, dataElement.getIdentification())
                  .getId());
      dataElement.setIdentification(IdentificationHandler.convert(scopedIdentifier));
      dataElement.getIdentification().setNamespaceId(
          Integer.parseInt(previousDataElement.getIdentification().getUrn().split(":")[1]));
    }

    delete(ctx, userId, previousDataElement.getIdentification().getUrn());
    create(ctx, userId, dataElement);

    return dataElement.getIdentification();
  }

}
