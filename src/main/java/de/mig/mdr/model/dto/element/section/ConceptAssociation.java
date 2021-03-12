package de.mig.mdr.model.dto.element.section;

import de.mig.mdr.dal.jooq.enums.RelationType;
import de.mig.mdr.dal.jooq.tables.pojos.ConceptElementAssociations;
import de.mig.mdr.dal.jooq.tables.pojos.Concepts;
import java.io.Serializable;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class ConceptAssociation implements Serializable {

  private Integer conceptId;
  private String system;
  private Integer sourceId;
  private String version;
  private String term;
  private String text;
  private RelationType linktype;
  private Integer scopedIdentifierId;

  /**
   * Construct a new ConceptAssociation.
   */
  public ConceptAssociation(ConceptElementAssociations cea, Concepts concepts) {
    setConceptId(concepts.getId());
    setSystem(concepts.getSystem());
    setSourceId(concepts.getSourceId());
    setVersion(concepts.getVersion());
    setTerm(concepts.getTerm());
    setText(concepts.getText());
    setLinktype(cea.getLinktype());
    setScopedIdentifierId(cea.getScopedidentifierId());
  }
}
