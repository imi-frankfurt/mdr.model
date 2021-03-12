package de.mig.mdr.model.dto.element;

import de.mig.mdr.model.dto.element.section.Validation;
import de.mig.mdr.model.dto.element.section.ConceptAssociation;
import java.io.Serializable;
import java.util.List;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class DataElement extends Element implements Serializable {

  private Validation validation;
  private List<ConceptAssociation> conceptAssociations;
}
