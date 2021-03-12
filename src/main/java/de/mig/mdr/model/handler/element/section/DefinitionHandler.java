package de.mig.mdr.model.handler.element.section;

import static de.mig.mdr.dal.jooq.Tables.DEFINITION;

import de.mig.mdr.dal.jooq.tables.records.DefinitionRecord;
import de.mig.mdr.model.CtxUtil;
import de.mig.mdr.model.dto.element.section.Definition;
import java.util.List;
import java.util.stream.Collectors;
import org.jooq.DSLContext;

public class DefinitionHandler {

  /**
   * Get all definitions for a scoped identifier.
   */
  public static List<Definition> get(DSLContext ctx, int elementId, int scopedIdentifierId) {
    List<DefinitionRecord> definitionRecords =
        ctx.fetch(DEFINITION,
            DEFINITION.SCOPED_IDENTIFIER_ID.equal(scopedIdentifierId)
                .and(DEFINITION.ELEMENT_ID.equal(elementId)));

    List<de.mig.mdr.dal.jooq.tables.pojos.Definition> definitions = definitionRecords.stream()
        .map(definitionRecord -> definitionRecord.into(
            de.mig.mdr.dal.jooq.tables.pojos.Definition.class))
        .collect(Collectors.toList());
    return convert(definitions);
  }

  /**
   * Convert a list of Definition objects of MDR DAL to a list of Definition objects of MDR Model.
   */
  public static List<Definition> convert(
      List<de.mig.mdr.dal.jooq.tables.pojos.Definition> definitions) {
    return definitions.stream().map(DefinitionHandler::convert).collect(Collectors.toList());
  }

  /**
   * Convert a Definition object of MDR DAL to a Definition object of MDR Model.
   */
  public static Definition convert(de.mig.mdr.dal.jooq.tables.pojos.Definition definition) {
    Definition newDefinition = new Definition();
    newDefinition.setLanguage(definition.getLanguage());
    newDefinition.setDesignation(definition.getDesignation());
    newDefinition.setDefinition(definition.getDefinition());
    return newDefinition;
  }

  /**
   * Convert a list of Definition objects of MDR Model to a list of Definition objects of MDR DAL.
   */
  public static List<de.mig.mdr.dal.jooq.tables.pojos.Definition> convert(
      List<Definition> definitions, Integer elementId) {
    return convert(definitions, elementId, null);
  }

  /**
   * Convert a list of Definition objects of MDR Model to a list of Definition objects of MDR DAL.
   */
  public static List<de.mig.mdr.dal.jooq.tables.pojos.Definition> convert(
      List<Definition> definitions, Integer elementId, Integer scopedIdentifierId) {
    return definitions.stream().map(d -> convert(d, elementId, scopedIdentifierId))
        .collect(Collectors.toList());
  }

  /**
   * Convert a Definition object of MDR Model to a Definition object of MDR DAL.
   */
  public static de.mig.mdr.dal.jooq.tables.pojos.Definition convert(Definition definition,
      Integer elementId) {
    return convert(definition, elementId, null);
  }

  /**
   * Convert a Definition object of MDR Model to a Definition object of MDR DAL.
   */
  public static de.mig.mdr.dal.jooq.tables.pojos.Definition convert(Definition definition,
      Integer elementId, Integer scopedIdentifierId) {
    de.mig.mdr.dal.jooq.tables.pojos.Definition newDefinition =
        new de.mig.mdr.dal.jooq.tables.pojos.Definition();
    newDefinition.setLanguage(definition.getLanguage());
    newDefinition.setDesignation(definition.getDesignation());
    newDefinition.setDefinition(definition.getDefinition());
    newDefinition.setElementId(elementId);
    newDefinition.setScopedIdentifierId(scopedIdentifierId);
    return newDefinition;
  }

  /**
   * Insert a list of definitions for an element.
   */
  public static void create(DSLContext ctx, List<Definition> definitions, Integer elementId) {
    create(ctx, definitions, elementId, null);
  }

  /**
   * Insert a list of definitions for an element / scoped identifier.
   */
  public static void create(DSLContext ctx, List<Definition> definitions, Integer elementId,
      Integer scopedIdentifierId) {
    final boolean autoCommit = CtxUtil.disableAutoCommit(ctx);
    List<de.mig.mdr.dal.jooq.tables.pojos.Definition> definitionPojos = DefinitionHandler
        .convert(definitions, elementId);
    definitionPojos.forEach(d -> {
      d.setElementId(elementId);
      d.setScopedIdentifierId(scopedIdentifierId);
    });
    saveDefinitions(ctx, definitionPojos);
    CtxUtil.commitAndSetAutoCommit(ctx, autoCommit);
  }

  /**
   * Save definitions.s
   */
  public static void saveDefinitions(DSLContext ctx,
      List<de.mig.mdr.dal.jooq.tables.pojos.Definition> definitions) {
    definitions.forEach(d -> ctx.newRecord(DEFINITION, d).store());
  }

  /**
   * Save definition.n
   */
  public static void saveDefinition(DSLContext ctx,
      de.mig.mdr.dal.jooq.tables.pojos.Definition definition) {
    ctx.newRecord(DEFINITION, definition).store();
  }
}
