package de.mig.mdr.model.handler.element.section.validation;

import de.mig.mdr.model.dto.element.section.Validation;
import de.mig.mdr.model.dto.element.section.validation.Text;
import de.mig.mdr.dal.jooq.enums.ElementType;
import de.mig.mdr.dal.jooq.enums.ValidationType;

public class TextHandler {

  /**
   * Convert a ValueDomain object of MDR DAL to a Text object of MDR Model.
   */
  public static Text convert(de.mig.mdr.dal.jooq.tables.pojos.Element valueDomain) {
    Text text = new Text();
    text.setUseRegEx(!valueDomain.getFormat().isEmpty());
    text.setRegEx(valueDomain.getFormat());

    if (valueDomain.getMaximumCharacters() > 0) {
      text.setUseMaximumLength(true);
      text.setMaximumLength(valueDomain.getMaximumCharacters());
    } else {
      text.setUseMaximumLength(false);
    }

    return text;
  }

  /**
   * Convert a Validation object of MDR Model to a DescribedValueDomain object of MDR DAL.
   */
  public static de.mig.mdr.dal.jooq.tables.pojos.Element convert(Validation validation) {
    de.mig.mdr.dal.jooq.tables.pojos.Element domain
        = new de.mig.mdr.dal.jooq.tables.pojos.Element();

    String regEx;
    if (validation.getText().getUseRegEx()) {
      regEx = validation.getText().getRegEx();
      domain.setValidationType(ValidationType.REGEX);
      domain.setValidationData(regEx);
    } else {
      regEx = "";
      domain.setValidationType(ValidationType.NONE);
      domain.setValidationData(null);
    }

    if (validation.getText().getUseMaximumLength()) {
      domain.setMaximumCharacters(validation.getText().getMaximumLength());
    } else {
      domain.setMaximumCharacters(0);
    }

    domain.setDescription(regEx);
    domain.setFormat(regEx);
    domain.setDatatype(Validation.TYPE_STRING);
    domain.setElementType(ElementType.DESCRIBED_VALUE_DOMAIN);
    return domain;
  }
}
