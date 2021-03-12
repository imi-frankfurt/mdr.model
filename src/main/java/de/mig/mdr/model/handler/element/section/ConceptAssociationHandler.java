package de.mig.mdr.model.handler.element.section;

import static de.mig.mdr.dal.jooq.Tables.CONCEPTS;
import static de.mig.mdr.dal.jooq.Tables.CONCEPT_ELEMENT_ASSOCIATIONS;
import static de.mig.mdr.dal.jooq.Tables.ELEMENT;
import static de.mig.mdr.dal.jooq.Tables.SCOPED_IDENTIFIER;

import de.mig.mdr.model.dto.element.section.ConceptAssociation;
import de.mig.mdr.model.dto.element.section.Identification;
import de.mig.mdr.dal.jooq.tables.Element;
import de.mig.mdr.dal.jooq.tables.ScopedIdentifier;
import de.mig.mdr.dal.jooq.tables.pojos.ConceptElementAssociations;
import de.mig.mdr.dal.jooq.tables.records.ConceptElementAssociationsRecord;
import de.mig.mdr.dal.jooq.tables.records.ConceptsRecord;
import java.util.ArrayList;
import java.util.List;
import org.jooq.DSLContext;

public class ConceptAssociationHandler {

  /**
   * Get a list of concept associations from a given urn from the database.
   *
   * @param identifier of the element associated to the concept association to get
   */
  public static List<ConceptAssociation> get(DSLContext ctx, Identification identifier) {
    de.mig.mdr.dal.jooq.tables.ConceptElementAssociations conceptElementAssociations
        = CONCEPT_ELEMENT_ASSOCIATIONS.as("ceassociation");
    de.mig.mdr.dal.jooq.tables.Concepts concepts = CONCEPTS.as("concepts");
    ScopedIdentifier si = SCOPED_IDENTIFIER.as("si");
    Element ns = ELEMENT.as("ns");

    List<ConceptElementAssociations> conceptElementAssociationsList =
        ctx.select()
            .from(conceptElementAssociations)
            .leftJoin(si).on(conceptElementAssociations.SCOPEDIDENTIFIER_ID.eq(si.ID))
            .leftJoin(ns).on(ns.ID.eq(si.NAMESPACE_ID))
            .leftJoin(concepts).on(conceptElementAssociations.CONCEPT_ID.eq(concepts.ID))
            .where(si.IDENTIFIER.eq(identifier.getIdentifier()))
            .and(si.VERSION.eq(identifier.getRevision()))
            .and(si.ELEMENT_TYPE.eq(identifier.getElementType()))
            .and(si.NAMESPACE_ID.eq(identifier.getNamespaceId()))
            .fetchInto(conceptElementAssociations)
            .into(de.mig.mdr.dal.jooq.tables.pojos.ConceptElementAssociations.class);

    List<de.mig.mdr.dal.jooq.tables.pojos.Concepts> conceptList =
        new ArrayList<>(ctx.select()
            .from(concepts)
            .leftJoin(conceptElementAssociations)
            .on(concepts.ID.eq(conceptElementAssociations.CONCEPT_ID))
            .fetch()
            .into(concepts)
            .into(de.mig.mdr.dal.jooq.tables.pojos.Concepts.class));

    List<ConceptAssociation> conceptAssociations = new ArrayList<>();

    for (de.mig.mdr.dal.jooq.tables.pojos.ConceptElementAssociations cea :
        conceptElementAssociationsList) {
      ConceptAssociation conceptAssociation = new ConceptAssociation();
      conceptAssociation.setConceptId(cea.getConceptId());
      conceptAssociation.setScopedIdentifierId(cea.getScopedidentifierId());
      conceptAssociation.setLinktype(cea.getLinktype());

      de.mig.mdr.dal.jooq.tables.pojos.Concepts concept = conceptList.stream()
          .filter(c -> c.getId().equals(conceptAssociation.getConceptId())).findAny().get();
      conceptAssociation.setSystem(concept.getSystem());
      conceptAssociation.setText(concept.getText());
      conceptAssociation.setTerm(concept.getTerm());
      conceptAssociation.setVersion(concept.getVersion());
      conceptAssociation.setSourceId(concept.getSourceId());

      conceptAssociations.add(conceptAssociation);
    }

    return conceptAssociations;
  }

  /**
   * Get a List of concept association.
   */
  public static List<ConceptAssociation> get(DSLContext ctx, String urn) {
    return get(ctx, IdentificationHandler.fromUrn(urn));
  }

  /**
   * Saves the given list of concept associations in the database.
   */
  public static void save(DSLContext ctx,
      List<ConceptAssociation> conceptAssociations, Integer userId, int scopedIdentifierId) {
    if (conceptAssociations != null) {
      for (ConceptAssociation conceptAssociation : conceptAssociations) {
        save(ctx, conceptAssociation, userId, scopedIdentifierId);
      }
    }
  }


  /**
   * Saves the given concept association in the database. Expects a concept association linked to an
   * element.
   *
   * @param conceptAssociation the concept association to store
   */
  public static void save(DSLContext ctx, ConceptAssociation conceptAssociation,
      Integer userId, int scopedIdentifier) {
    de.mig.mdr.dal.jooq.tables.pojos.ConceptElementAssociations conceptElementAssociations =
        new de.mig.mdr.dal.jooq.tables.pojos.ConceptElementAssociations();
    conceptElementAssociations.setScopedidentifierId(scopedIdentifier);
    conceptElementAssociations.setLinktype(conceptAssociation.getLinktype());
    conceptElementAssociations.setCreatedBy(userId);

    de.mig.mdr.dal.jooq.tables.pojos.Concepts concepts =
        new de.mig.mdr.dal.jooq.tables.pojos.Concepts();
    concepts.setCreatedBy(userId);

    ConceptsRecord conceptRecord = ctx
        .fetchOne(CONCEPTS, CONCEPTS.TERM.eq(conceptAssociation.getTerm()));
    if (conceptRecord == null) {
      conceptRecord = ctx.newRecord(CONCEPTS, concepts);
    }

    conceptRecord.setVersion(conceptAssociation.getVersion());
    conceptRecord.setText(conceptAssociation.getText());
    conceptRecord.setTerm(conceptAssociation.getTerm());
    conceptRecord.setSystem(conceptAssociation.getSystem());
    conceptRecord.setSourceId(conceptAssociation.getSourceId());
    conceptRecord.store();

    Integer conceptId = conceptRecord.getId();

    conceptElementAssociations.setConceptId(conceptId);

    ConceptElementAssociationsRecord ceaRecord = ctx.fetchOne(CONCEPT_ELEMENT_ASSOCIATIONS,
        CONCEPT_ELEMENT_ASSOCIATIONS.SCOPEDIDENTIFIER_ID
            .eq(scopedIdentifier)
            .and(CONCEPT_ELEMENT_ASSOCIATIONS.CONCEPT_ID.eq(conceptId)));
    if (ceaRecord == null) {
      ceaRecord = ctx.newRecord(CONCEPT_ELEMENT_ASSOCIATIONS, conceptElementAssociations);
    }

    ceaRecord.store();
  }

}
