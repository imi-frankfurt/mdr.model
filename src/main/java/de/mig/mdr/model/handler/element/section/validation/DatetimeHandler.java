package de.mig.mdr.model.handler.element.section.validation;

import de.mig.mdr.dal.jooq.enums.ElementType;
import de.mig.mdr.dal.jooq.enums.ValidationType;
import de.mig.mdr.dal.jooq.tables.pojos.Element;
import de.mig.mdr.model.dto.element.section.Validation;
import de.mig.mdr.model.dto.element.section.validation.Datetime;

public class DatetimeHandler {

  /**
   * Convert an Element object of MDR DAL to a Datetime object of MDR Model.
   */
  public static Datetime convert(Element valueDomain) {
    Datetime datetime = new Datetime();
    datetime.setHourFormat(valueDomain.getValidationData().contains("HOURS_24") ? "24h" : "12h");

    String type = valueDomain.getDatatype();
    if (type.equals(Validation.TYPE_DATETIME)) {
      String[] parts = valueDomain.getFormat().split(" ");
      datetime.setDate(parts[0]);
      datetime.setTime(parts[1]);
    } else if (type.equals(Validation.TYPE_DATE)) {
      datetime.setDate(valueDomain.getFormat());
      datetime.setTime(null);
      datetime.setHourFormat("");
    } else {
      datetime.setDate(null);
      datetime.setTime(valueDomain.getFormat());
    }

    return datetime;
  }

  /**
   * Convert a Validation object of MDR Model to an Element object of MDR DAL.
   */
  public static Element convert(Validation validation) {
    Element domain = new Element();
    Datetime datetime = validation.getDatetime();
    String format = "";
    String validationData = "";

    switch (validation.getType()) {
      case Validation.TYPE_DATE:
        format = datetime.getDate();
        break;
      case Validation.TYPE_DATETIME:
        format = datetime.getDate() + " " + datetime.getTime();
        break;
      case Validation.TYPE_TIME:
        format = datetime.getTime();
        break;
      default:
        break;
    }

    domain.setDatatype(validation.getType());
    domain.setValidationType(ValidationType.valueOf(validation.getType()));
    domain.setFormat(format);
    domain.setDescription(format);
    domain.setMaximumCharacters(format.length());
    domain.setValidationData(buildValidationData(validation));
    domain.setElementType(ElementType.DESCRIBED_VALUE_DOMAIN);
    return domain;
  }

  private static String buildValidationData(Validation validation) {
    String validationData = "";
    Datetime datetime = validation.getDatetime();

    if (validation.getType().equals(Validation.TYPE_DATE)
        || validation.getType().equals(Validation.TYPE_DATETIME)) {
      validationData = datetime.getDate().contains("YYYY-MM") ? "ISO_8601" : "DIN_5008";
      validationData += datetime.getDate().contains("DD") ? "_WITH_DAYS" : "";
    }

    if (validation.getType().equals(Validation.TYPE_DATETIME)) {
      validationData += ";";
    }

    if (validation.getType().equals(Validation.TYPE_TIME)
        || validation.getType().equals(Validation.TYPE_DATETIME)) {
      validationData += datetime.getHourFormat().equals("24h") ? "HOURS_24" : "HOURS_12";
      validationData += datetime.getTime().contains("ss") ? "_WITH_SECONDS" : "";
    }

    return validationData;
  }

}
