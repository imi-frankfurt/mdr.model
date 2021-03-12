package de.mig.mdr.model.dto.element.section.validation;

import java.io.Serializable;
import lombok.Data;

@Data
public class Numeric implements Serializable {

  private Boolean useMinimum;
  private Boolean useMaximum;
  private Integer minimum;
  private Integer maximum;
  private String unitOfMeasure;
}
