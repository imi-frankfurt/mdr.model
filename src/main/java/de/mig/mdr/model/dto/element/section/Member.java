package de.mig.mdr.model.dto.element.section;

import com.fasterxml.jackson.annotation.JsonInclude;
import de.mig.mdr.dal.jooq.enums.Status;
import java.io.Serializable;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Member implements Serializable {

  private String elementUrn;
  private Status status;
  private Integer order;
}
