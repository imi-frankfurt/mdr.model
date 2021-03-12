package de.mig.mdr.model.dto.element.section;

import com.fasterxml.jackson.annotation.JsonInclude;
import de.mig.mdr.dal.jooq.enums.ElementType;
import de.mig.mdr.dal.jooq.enums.Status;
import java.io.Serializable;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Identification implements Serializable {

  private ElementType elementType;
  private Integer namespaceId;
  private Status status;
  private Integer identifier;
  private Integer revision;
  private String urn;
  private Boolean hideNamespace;
}
