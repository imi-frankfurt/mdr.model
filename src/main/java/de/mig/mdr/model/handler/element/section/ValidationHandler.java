package de.mig.mdr.model.handler.element.section;

import static de.mig.mdr.dal.jooq.tables.Element.ELEMENT;

import de.mig.mdr.dal.jooq.Tables;
import de.mig.mdr.dal.jooq.enums.ElementType;
import de.mig.mdr.dal.jooq.enums.ValidationType;
import de.mig.mdr.dal.jooq.tables.pojos.Element;
import de.mig.mdr.dal.jooq.tables.records.ElementRecord;
import de.mig.mdr.dal.jooq.tables.records.IdentifiedElementRecord;
import de.mig.mdr.model.dto.element.section.Validation;
import de.mig.mdr.model.handler.element.section.validation.DatetimeHandler;
import de.mig.mdr.model.handler.element.section.validation.NumericHandler;
import de.mig.mdr.model.handler.element.section.validation.TextHandler;
import java.util.Comparator;
import java.util.UUID;
import org.jooq.DSLContext;

public class ValidationHandler {


  /**
   * Get the validation for an identified element record.
   */
  public static Validation get(DSLContext ctx, IdentifiedElementRecord identifiedElementRecord) {
    ElementRecord elementRecord = ctx
        .fetchOne(ELEMENT, ELEMENT.ID.equal(identifiedElementRecord.getElementId()));
    Element valueDomain = elementRecord
        .into(Element.class);

    Validation validation = new Validation();
    validation.setType(valueDomain.getDatatype());

    switch (validation.getType()) {
      case Validation.TYPE_DATE:
      case Validation.TYPE_DATETIME:
      case Validation.TYPE_TIME:
        validation.setDatetime(DatetimeHandler.convert(valueDomain));
        break;
      case Validation.TYPE_FLOAT:
      case Validation.TYPE_INTEGER:
        validation.setNumeric(NumericHandler.convert(valueDomain));
        break;
      case Validation.TYPE_STRING:
        validation.setText(TextHandler.convert(valueDomain));
        break;
      case Validation.TYPE_ENUMERATED:
        // TODO
      default:
        break;
    }
    return validation;
  }

  /**
   * Save validation.
   */
  public static int saveValidation(DSLContext ctx, Element validation) {
    if (validation.getUuid() == null) {
      validation.setUuid(UUID.randomUUID());
    }
    return ctx.insertInto(Tables.ELEMENT)
        .set(ctx.newRecord(Tables.ELEMENT, validation))
        .returning(Tables.ELEMENT.ID)
        .fetchOne().getId();
  }

  /**
   * Convert a Validation of MDR Model to a ValueDomain of MDR DAL.
   */
  public static Element convert(Validation validation) {
    switch (validation.getType()) {
      case Validation.TYPE_DATE:
      case Validation.TYPE_DATETIME:
      case Validation.TYPE_TIME:
        return DatetimeHandler.convert(validation);
      case Validation.TYPE_FLOAT:
      case Validation.TYPE_INTEGER:
        return NumericHandler.convert(validation);
      case Validation.TYPE_STRING:
        return TextHandler.convert(validation);
      case Validation.TYPE_BOOLEAN: {
        Element domain = new Element();
        domain.setDatatype(validation.getType());
        domain.setDescription("(true|false|yes|no|f|t)");
        domain.setFormat(domain.getDescription());
        domain.setValidationData(domain.getDescription());
        domain.setValidationType(ValidationType.BOOLEAN);
        domain.setMaximumCharacters(5);
        domain.setElementType(ElementType.DESCRIBED_VALUE_DOMAIN);
        return domain;
      }
      case Validation.TYPE_TBD: {
        Element domain = new Element();
        domain.setDatatype(validation.getType());
        domain.setDescription(ValidationType.TBD.getName());
        domain.setFormat(ValidationType.TBD.getName());
        domain.setValidationType(ValidationType.TBD);
        domain.setMaximumCharacters(0);
        domain.setElementType(ElementType.DESCRIBED_VALUE_DOMAIN);
        return domain;
      }
      case Validation.TYPE_ENUMERATED: {
        Element domain = new Element();
        domain.setDatatype(validation.getType());
        domain.setFormat(validation.getType());
        domain.setMaximumCharacters(validation.getPermittedValues().stream()
            .map(pv -> pv.getValue().length())
            .max(Comparator.naturalOrder()).orElse(0));
        domain.setElementType(ElementType.ENUMERATED_VALUE_DOMAIN);
        return domain;
      }
      case Validation.TYPE_CATALOG:
        // TODO
      default:
        return null;
    }
  }

  /**
   * Create a new ValueDomain of MDR DAL with a Validation of MDR Model.
   */
  public static Element create(DSLContext ctx, Validation validation) {
    Element domain = ValidationHandler.convert(validation);
    domain.setId(saveValidation(ctx, domain));
    return domain;
  }
}
