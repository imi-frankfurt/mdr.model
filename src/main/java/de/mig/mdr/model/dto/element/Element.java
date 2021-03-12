package de.mig.mdr.model.dto.element;

import de.mig.mdr.model.dto.element.section.Identification;
import de.mig.mdr.model.dto.element.section.Definition;
import de.mig.mdr.model.dto.element.section.Slot;
import java.io.Serializable;
import java.util.List;
import lombok.Data;

@Data
public class Element implements Serializable {

  private Identification identification;
  private List<Definition> definitions;
  private List<Slot> slots;
}
