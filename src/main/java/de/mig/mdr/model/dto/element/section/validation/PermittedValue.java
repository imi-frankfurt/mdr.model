package de.mig.mdr.model.dto.element.section.validation;

import de.mig.mdr.model.dto.element.section.Definition;
import java.io.Serializable;
import java.util.List;
import java.util.Objects;
import lombok.Data;

@Data
public class PermittedValue implements Serializable {

  private String value;
  private List<Definition> meanings;

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    PermittedValue that = (PermittedValue) o;
    return value.equals(that.value) && Objects.equals(meanings, that.meanings);
  }

  @Override
  public int hashCode() {
    return Objects.hash(value, meanings);
  }
}
