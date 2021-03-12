package de.mig.mdr.model.service;

import de.mig.mdr.dal.ResourceManager;
import de.mig.mdr.dal.jooq.enums.Status;
import de.mig.mdr.dal.jooq.tables.pojos.ScopedIdentifier;
import de.mig.mdr.model.dto.element.DataElement;
import de.mig.mdr.model.dto.element.DataElementGroup;
import de.mig.mdr.model.dto.element.Element;
import de.mig.mdr.model.dto.element.Namespace;
import de.mig.mdr.model.dto.element.Record;
import de.mig.mdr.model.dto.element.section.Identification;
import de.mig.mdr.model.handler.element.DataElementGroupHandler;
import de.mig.mdr.model.handler.element.DataElementHandler;
import de.mig.mdr.model.handler.element.ElementHandler;
import de.mig.mdr.model.handler.element.NamespaceHandler;
import de.mig.mdr.model.handler.element.RecordHandler;
import de.mig.mdr.model.handler.element.section.IdentificationHandler;
import de.mig.mdr.model.handler.element.section.MemberHandler;
import java.util.NoSuchElementException;
import org.jooq.DSLContext;
import org.springframework.stereotype.Service;

@Service
public class ElementService {

  /**
   * Create a new Element and return its new ID.
   */
  public ScopedIdentifier create(int userId, Element element)
      throws IllegalAccessException, IllegalArgumentException {
    try (DSLContext ctx = ResourceManager.getDslContext()) {
      ScopedIdentifier scopedIdentifier;
      switch (element.getIdentification().getElementType()) {
        case NAMESPACE:
          return NamespaceHandler.create(ctx, userId, (Namespace) element);
        case DATAELEMENT:
          return DataElementHandler.create(ctx, userId, (DataElement) element);
        case DATAELEMENTGROUP:
          return DataElementGroupHandler.create(ctx, userId, (DataElementGroup) element);
        case RECORD:
          return RecordHandler.create(ctx, userId, (Record) element);
        default:
          throw new IllegalArgumentException("Element Type is not supported");
      }
    }
  }

  /**
   * Get an Element by its urn.
   */
  public Element read(int userId, String urn) {
    try (DSLContext ctx = ResourceManager.getDslContext()) {
      if (!IdentificationHandler.isUrn(urn)) {
        try {
          Namespace namespace = NamespaceHandler.get(ctx, userId, Integer.parseInt(urn));
          if (namespace == null) {
            throw new NoSuchElementException();
          } else {
            return namespace;
          }
        } catch (NumberFormatException e) {
          throw new NoSuchElementException();
        }
      } else {
        // Read other elements with proper urn
        Identification identification = IdentificationHandler.fromUrn(urn);
        if (identification == null) {
          throw new NoSuchElementException(urn);
        }
        switch (identification.getElementType()) {
          case DATAELEMENT:
            return DataElementHandler.get(ctx, userId, urn);
          case DATAELEMENTGROUP:
            return DataElementGroupHandler.get(ctx, userId, urn);
          case RECORD:
            return RecordHandler.get(ctx, userId, urn);
          default:
            throw new IllegalArgumentException("Element Type is not supported");
        }
      }
    }
  }

  /**
   * Update an Element and return its urn.
   */
  public Identification update(int userId, Element element) throws IllegalAccessException {
    try (DSLContext ctx = ResourceManager.getDslContext()) {
      switch (element.getIdentification().getElementType()) {
        case NAMESPACE:
          return NamespaceHandler.update(ctx, userId, (Namespace) element);
        case DATAELEMENT:
          return DataElementHandler.update(ctx, userId, (DataElement) element);
        case DATAELEMENTGROUP:
          return DataElementGroupHandler.update(ctx, userId, (DataElementGroup) element);
        case RECORD:
          return RecordHandler.update(ctx, userId, (Record) element);
        default:
          throw new IllegalArgumentException("Element Type is not supported");
      }
    }
  }

  /**
   * Delete an identifier with the given urn.
   */
  public void delete(int userId, String urn) {
    try (DSLContext ctx = ResourceManager.getDslContext()) {
      if (!IdentificationHandler.isUrn(urn)) {
        throw new IllegalArgumentException();
      } else {
        Identification identification = IdentificationHandler.fromUrn(urn);
        if (identification == null) {
          throw new NoSuchElementException(urn);
        }
        ElementHandler.delete(ctx, userId, urn);
      }
    }
  }

  /**
   * Release an Element.
   */
  public void release(int userId, String urn) {
    Identification identification = IdentificationHandler.fromUrn(urn);

    if (identification == null) {
      throw new NoSuchElementException(urn);
    }

    if (!identification.getStatus().equals(Status.STAGED) && !identification.getStatus()
        .equals(Status.DRAFT)) {
      throw new IllegalStateException("Neither a draft nor staged");
    }

    try (DSLContext ctx = ResourceManager.getDslContext()) {
      switch (identification.getElementType()) {
        case NAMESPACE:
        case DATAELEMENT:
          IdentificationHandler.updateStatus(ctx, userId, identification, Status.RELEASED);
          break;
        case DATAELEMENTGROUP:
        case RECORD:
          if (MemberHandler.allSubIdsAreReleased(ctx, identification)) {
            IdentificationHandler.updateStatus(ctx, userId, identification, Status.RELEASED);
          } else {
            throw new IllegalStateException("Not all members are released");
          }
          break;
        default:
          throw new IllegalArgumentException(
              "Element Type is not supported: " + identification.getElementType());
      }
    }
  }
}
