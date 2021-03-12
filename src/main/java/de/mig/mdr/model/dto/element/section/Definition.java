package de.mig.mdr.model.dto.element.section;

import java.io.Serializable;
import java.util.Objects;
import lombok.Data;

@Data
public class Definition implements Serializable {

  private String designation;
  private String definition;
  private String language;

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    Definition that = (Definition) o;
    return designation.equals(that.designation) && Objects
        .equals(definition, that.definition) && Objects.equals(language, that.language);
  }

  @Override
  public int hashCode() {
    return Objects.hash(designation, definition, language);
  }
}
