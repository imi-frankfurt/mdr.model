package de.mig.mdr.model.handler.element;

import static java.util.stream.Collectors.toList;

import de.mig.mdr.model.dto.element.Element;
import de.mig.mdr.model.dto.element.section.Identification;
import de.mig.mdr.model.handler.element.section.DefinitionHandler;
import de.mig.mdr.model.handler.element.section.MemberHandler;
import de.mig.mdr.model.handler.element.section.SlotHandler;
import de.mig.mdr.dal.jooq.enums.ElementType;
import de.mig.mdr.dal.jooq.enums.Status;
import de.mig.mdr.dal.jooq.tables.pojos.ScopedIdentifier;
import de.mig.mdr.dal.jooq.tables.records.IdentifiedElementRecord;
import de.mig.mdr.model.CtxUtil;
import de.mig.mdr.model.dto.element.DataElementGroup;
import de.mig.mdr.model.dto.element.section.Member;
import de.mig.mdr.model.handler.element.section.IdentificationHandler;
import java.util.List;
import java.util.UUID;
import org.jooq.DSLContext;

public class DataElementGroupHandler extends ElementHandler {

  /**
   *  Create a new DataElementGroup and return its new ID.
   */
  public static DataElementGroup get(DSLContext ctx, int userId, String urn) {
    Identification identification = IdentificationHandler.fromUrn(urn);
    IdentifiedElementRecord identifiedElementRecord = ElementHandler
        .getIdentifiedElementRecord(ctx, identification);
    Element element = ElementHandler.convertToElement(ctx, identification, identifiedElementRecord);

    DataElementGroup newDataElementGroup = new DataElementGroup();
    newDataElementGroup.setIdentification(identification);
    newDataElementGroup.setDefinitions(element.getDefinitions());
    newDataElementGroup.setMembers(MemberHandler.get(ctx, identification));
    newDataElementGroup.setSlots(SlotHandler.get(ctx, element.getIdentification()));
    return newDataElementGroup;
  }

  /** Create a new DataElementGroup and return its new ID. */
  public static ScopedIdentifier create(
      DSLContext ctx, int userId, DataElementGroup dataElementGroup)
      throws IllegalArgumentException {

    // Check if all member urns are present
    List<Element> members = ElementHandler
        .fetchByUrns(ctx, userId, dataElementGroup.getMembers().stream().map(Member::getElementUrn)
            .collect(toList()));
    if (members.size() != dataElementGroup.getMembers().size()) {
      throw new IllegalArgumentException();
    }

    final boolean autoCommit = CtxUtil.disableAutoCommit(ctx);
    de.mig.mdr.dal.jooq.tables.pojos.Element element =
        new de.mig.mdr.dal.jooq.tables.pojos.Element();
    element.setElementType(ElementType.DATAELEMENTGROUP);
    if (element.getUuid() == null) { //TODO delete?
      element.setUuid(UUID.randomUUID());
    }
    element.setId(saveElement(ctx, element));
    ScopedIdentifier scopedIdentifier =
        IdentificationHandler.create(
            ctx, userId, dataElementGroup.getIdentification(), element.getId());
    DefinitionHandler.create(
        ctx, dataElementGroup.getDefinitions(), element.getId(), scopedIdentifier.getId());
    if (dataElementGroup.getSlots() != null) {
      SlotHandler.create(ctx, dataElementGroup.getSlots(), scopedIdentifier.getId());
    }
    if (dataElementGroup.getMembers() != null) {
      MemberHandler.create(ctx, userId, members, scopedIdentifier.getId());
    }
    CtxUtil.commitAndSetAutoCommit(ctx, autoCommit);
    return scopedIdentifier;
  }

  /**
   * Update a dataelementgroup.
   */
  public static Identification update(DSLContext ctx, int userId, DataElementGroup dataElementGroup)
      throws IllegalAccessException {
    DataElementGroup previousDataElementGroup = get(ctx, userId,
        dataElementGroup.getIdentification().getUrn());

    //update scopedIdentifier if status != DRAFT
    if (previousDataElementGroup.getIdentification().getStatus() != Status.DRAFT) {

      ScopedIdentifier scopedIdentifier =
          IdentificationHandler.update(ctx, userId, dataElementGroup.getIdentification(),
              ElementHandler.getIdentifiedElementRecord(ctx, dataElementGroup.getIdentification())
                  .getId());
      dataElementGroup.setIdentification(IdentificationHandler.convert(scopedIdentifier));
      dataElementGroup.getIdentification().setNamespaceId(
          Integer.parseInt(previousDataElementGroup.getIdentification().getUrn().split(":")[1]));
    }

    delete(ctx, userId, previousDataElementGroup.getIdentification().getUrn());
    create(ctx, userId, dataElementGroup);

    return dataElementGroup.getIdentification();
  }

}
