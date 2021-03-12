package de.mig.mdr.model;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import de.mig.mdr.model.dto.element.DataElement;
import de.mig.mdr.model.dto.element.DataElementGroup;
import de.mig.mdr.model.dto.element.Element;
import de.mig.mdr.model.dto.element.Namespace;
import de.mig.mdr.model.dto.element.Record;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class Deserializer {

  /**
   * Deserialize JSON elements into the corresponding class.
   */
  public static Element getElement(String content) {
    Gson gson = new GsonBuilder().create();
    // TODO: add missing element types.
    switch (gson.fromJson(content, Element.class).getIdentification().getElementType()) {
      case NAMESPACE:
        return gson.fromJson(content, Namespace.class);
      case DATAELEMENT:
        return gson.fromJson(content, DataElement.class);
      case DATAELEMENTGROUP:
        return gson.fromJson(content, DataElementGroup.class);
      case RECORD:
        return gson.fromJson(content, Record.class);
      default:
        throw new IllegalArgumentException("Element Type is not supported");
    }
  }
}
