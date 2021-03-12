package de.mig.mdr.model.dto.element.section;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import de.mig.mdr.model.dto.element.section.validation.Datetime;
import de.mig.mdr.model.dto.element.section.validation.Numeric;
import de.mig.mdr.model.dto.element.section.validation.PermittedValue;
import de.mig.mdr.model.dto.element.section.validation.Text;
import java.io.Serializable;
import java.util.List;
import java.util.Objects;
import lombok.Data;

@Data
@JsonInclude(Include.NON_NULL)
public class Validation implements Serializable {

  public static final String TYPE_STRING = "STRING";
  public static final String TYPE_INTEGER = "INTEGER";
  public static final String TYPE_FLOAT = "FLOAT";
  public static final String TYPE_BOOLEAN = "BOOLEAN";
  public static final String TYPE_TBD = "TBD";
  public static final String TYPE_ENUMERATED = "enumerated";
  public static final String TYPE_CATALOG = "CATALOG";
  public static final String TYPE_DATE = "DATE";
  public static final String TYPE_DATETIME = "DATETIME";
  public static final String TYPE_TIME = "TIME";
  private String type;
  private Text text;
  private List<PermittedValue> permittedValues;
  private Numeric numeric;
  private Datetime datetime;

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    Validation that = (Validation) o;
    return type.equals(that.type) && Objects.equals(text, that.text) && Objects
        .equals(permittedValues, that.permittedValues) && Objects
        .equals(numeric, that.numeric) && Objects.equals(datetime, that.datetime);
  }

  @Override
  public int hashCode() {
    return Objects.hash(type, text, permittedValues, numeric, datetime);
  }
}
