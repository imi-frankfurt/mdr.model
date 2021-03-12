package de.mig.mdr.model.handler.element.section.validation;

import de.mig.mdr.model.dto.element.section.Validation;
import de.mig.mdr.model.dto.element.section.validation.Numeric;
import de.mig.mdr.dal.jooq.enums.ElementType;
import de.mig.mdr.dal.jooq.enums.ValidationType;
import de.mig.mdr.dal.jooq.tables.pojos.Element;

public class NumericHandler {

  /**
   * Convert a ValueDomain object of MDR DAL to a Numeric object of MDR Model.
   */
  public static Numeric convert(Element valueDomain) {
    Numeric numeric = new Numeric();
    numeric.setUnitOfMeasure(valueDomain.getUnitOfMeasure());

    String[] parts = valueDomain.getFormat().split("<=");
    if (parts.length == 1) {
      numeric.setUseMinimum(false);
      numeric.setUseMaximum(false);
    } else if (parts.length == 2) {
      if (parts[0].equals("x")) {
        numeric.setUseMinimum(false);
        numeric.setUseMaximum(true);
        numeric.setMaximum(Integer.valueOf(parts[1]));
      } else {
        numeric.setUseMinimum(true);
        numeric.setUseMaximum(false);
        numeric.setMinimum(Integer.valueOf(parts[0]));
      }
    } else if (parts.length == 3) {
      numeric.setUseMinimum(true);
      numeric.setUseMaximum(true);
      numeric.setMinimum(Integer.valueOf(parts[0]));
      numeric.setMaximum(Integer.valueOf(parts[2]));
    }

    return numeric;
  }

  /**
   * Convert a Validation object of MDR Model to a DescribedValueDomain object of MDR DAL.
   */
  public static Element convert(Validation validation) {
    Element domain = new Element();
    Numeric numeric = validation.getNumeric();
    String min = String.valueOf(numeric.getMinimum());
    String max = String.valueOf(numeric.getMaximum());
    String validationData = "x";

    // Convert null in use min and use max to false. Also set use minimum to false if the minimum is
    // null. vice versa for max.
    if (numeric.getUseMaximum() == null || numeric.getMaximum() == null) {
      numeric.setUseMaximum(Boolean.FALSE);
    }
    if (numeric.getUseMinimum() == null || numeric.getMinimum() == null) {
      numeric.setUseMinimum(Boolean.FALSE);
    }

    if (numeric.getUseMinimum() || numeric.getUseMaximum()) {
      domain.setValidationType(ValidationType.valueOf(validation.getType() + "RANGE"));
      domain.setMaximumCharacters(Math.max(min.length(), max.length()));

      if (numeric.getUseMinimum()) {
        validationData = min + "<=" + validationData;
      }

      if (numeric.getUseMaximum()) {
        validationData = validationData + "<=" + max;
      }
    } else {
      domain.setValidationType(ValidationType.valueOf(validation.getType()));
      domain.setMaximumCharacters(0);
    }

    domain.setValidationData(validationData);
    domain.setDescription(validationData);
    domain.setFormat(validationData);
    domain.setDatatype(validation.getType());
    domain.setUnitOfMeasure(numeric.getUnitOfMeasure());
    domain.setElementType(ElementType.DESCRIBED_VALUE_DOMAIN);
    return domain;
  }

}
