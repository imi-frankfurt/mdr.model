package de.mig.mdr.model.dto.element.section;

import java.io.Serializable;
import lombok.Data;

@Data
public class Slot implements Serializable {

  private String name;
  private String value;
}
