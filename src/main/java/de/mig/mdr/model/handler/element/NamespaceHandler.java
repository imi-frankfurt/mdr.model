package de.mig.mdr.model.handler.element;

import static de.mig.mdr.dal.jooq.Tables.DEFINITION;
import static de.mig.mdr.dal.jooq.Tables.ELEMENT;
import static de.mig.mdr.dal.jooq.Tables.IDENTIFIED_ELEMENT;
import static de.mig.mdr.dal.jooq.Tables.SCOPED_IDENTIFIER;

import de.mig.mdr.model.CtxUtil;
import de.mig.mdr.model.DaoUtil;
import de.mig.mdr.model.dto.element.Namespace;
import de.mig.mdr.model.dto.element.section.Identification;
import de.mig.mdr.model.handler.GrantTypeHandler;
import de.mig.mdr.model.handler.UserHandler;
import de.mig.mdr.model.handler.element.section.DefinitionHandler;
import de.mig.mdr.model.handler.element.section.SlotHandler;
import de.mig.mdr.dal.jooq.enums.ElementType;
import de.mig.mdr.dal.jooq.enums.GrantType;
import de.mig.mdr.dal.jooq.enums.Status;
import de.mig.mdr.dal.jooq.tables.pojos.ScopedIdentifier;
import de.mig.mdr.dal.jooq.tables.pojos.UserNamespaceGrants;
import de.mig.mdr.dal.jooq.tables.records.IdentifiedElementRecord;
import de.mig.mdr.model.handler.element.section.IdentificationHandler;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.Result;
import org.jooq.SelectConditionStep;
import org.jooq.impl.DSL;

public class NamespaceHandler extends ElementHandler {

  /**
   * Create a new Namespace and return its new ID.
   */
  public static ScopedIdentifier create(DSLContext ctx, int userId, Namespace namespace) {

    final boolean autoCommit = CtxUtil.disableAutoCommit(ctx);
    de.mig.mdr.dal.jooq.tables.pojos.Element element
        = new de.mig.mdr.dal.jooq.tables.pojos.Element();
    element.setElementType(ElementType.NAMESPACE);
    element.setHidden(namespace.getIdentification().getHideNamespace());
    element.setCreatedBy(userId);
    if (element.getUuid() == null) {
      element.setUuid(UUID.randomUUID());
    }
    element.setId(saveElement(ctx, element));
    ScopedIdentifier scopedIdentifier =
        IdentificationHandler.create(
            ctx, userId, namespace.getIdentification(), element.getId());
    DefinitionHandler
        .create(ctx, namespace.getDefinitions(), element.getId(), scopedIdentifier.getId());
    if (namespace.getSlots() != null) {
      SlotHandler.create(ctx, namespace.getSlots(), scopedIdentifier.getId());
    }

    CtxUtil.commitAndSetAutoCommit(ctx, autoCommit);

    // Creator of the namespace gets admin rights by default
    UserHandler
        .setUserAccessToNamespace(userId, scopedIdentifier.getIdentifier(), GrantType.ADMIN);

    return scopedIdentifier;
  }

  /**
   * Get a Namespace.
   */
  public static Namespace get(DSLContext ctx, int userId, Integer namespaceIdentifier) {
    try {
      Integer namespaceId = ctx.select(SCOPED_IDENTIFIER.NAMESPACE_ID).from(SCOPED_IDENTIFIER)
          .where(SCOPED_IDENTIFIER.ELEMENT_TYPE.equal(ElementType.NAMESPACE))
          .and(SCOPED_IDENTIFIER.IDENTIFIER.equal(namespaceIdentifier))
          .and(SCOPED_IDENTIFIER.VERSION.equal(
              ctx.select(DSL.max(SCOPED_IDENTIFIER.VERSION)).from(SCOPED_IDENTIFIER)
                  .where(SCOPED_IDENTIFIER.IDENTIFIER.equal(namespaceIdentifier))))
          .fetchOneInto(Integer.class);

      Namespace namespace = getNamespace(ctx, userId, namespaceId);
      namespace.setSlots(SlotHandler.get(ctx, namespace.getIdentification()));
      return namespace;
    } catch (NullPointerException e) {
      throw new NoSuchElementException();
    }
  }

  /**
   * Get all Namespace Records by its Identifier.
   */
  public static List<IdentifiedElementRecord> getNamespaceRecords(DSLContext ctx, int userId,
      Integer namespaceIdentifier) {
    return ctx
        .selectFrom(IDENTIFIED_ELEMENT)
        .where(IDENTIFIED_ELEMENT.ELEMENT_TYPE.equal(ElementType.NAMESPACE)
            .and(IDENTIFIED_ELEMENT.SI_IDENTIFIER.equal(namespaceIdentifier))
        )
        .fetch();
  }

  /**
   * Get the latest Namespace Record by its Identifier.
   * If there is a released namespace, return that one, otherwise the latest version (be it outdated
   * or a draft)
   */
  public static IdentifiedElementRecord getLatestNamespaceRecord(DSLContext ctx, int userId,
      Integer namespaceIdentifier) {
    List<IdentifiedElementRecord> namespaceRecords = getNamespaceRecords(ctx, userId,
        namespaceIdentifier);

    Optional<IdentifiedElementRecord> releasedNamespaceOptional =
        namespaceRecords.stream().filter(r -> r.getSiStatus().equals(Status.RELEASED)).findFirst();

    return releasedNamespaceOptional.orElseGet(() -> namespaceRecords.stream().max(
        Comparator.comparing(IdentifiedElementRecord::getSiVersion))
        .orElseThrow(NoSuchElementException::new));
  }

  /**
   * Get a namespace by its id.
   */
  public static Namespace getNamespace(DSLContext ctx, int userId, Integer namespaceId) {
    SelectConditionStep<Record> query = getNamespacesQuery(ctx);
    query.and(DaoUtil.accessibleByUserId(ctx, userId))
        .and(ELEMENT.ID.eq(namespaceId));
    return fetchNamespaceQuery(query).stream().findFirst().orElse(null);
  }

  private static SelectConditionStep<Record> getNamespacesQuery(DSLContext ctx) {
    return ctx.select(ELEMENT.fields())
        .select(DEFINITION.fields())
        .select(SCOPED_IDENTIFIER.fields())
        .from(ELEMENT)
        .leftJoin(DEFINITION)
        .on(DEFINITION.ELEMENT_ID.eq(ELEMENT.ID))
        .leftJoin(SCOPED_IDENTIFIER)
        .on(SCOPED_IDENTIFIER.ELEMENT_ID.eq(ELEMENT.ID))
        .where(ELEMENT.ELEMENT_TYPE.eq(ElementType.NAMESPACE));
  }

  /**
   * Fetch and convert DescribedElements from a given query.
   */
  private static List<Namespace> fetchNamespaceQuery(SelectConditionStep<Record> query) {
    Result<?> result = query.fetch();
    Map<Integer, Namespace> namespaces = new HashMap<>();

    for (Record r : result) {
      de.mig.mdr.dal.jooq.tables.pojos.Element element =
          r.into(ELEMENT.fields()).into(de.mig.mdr.dal.jooq.tables.pojos.Element.class);
      de.mig.mdr.dal.jooq.tables.pojos.Definition definition =
          r.into(DEFINITION.fields()).into(de.mig.mdr.dal.jooq.tables.pojos.Definition.class);
      de.mig.mdr.dal.jooq.tables.pojos.ScopedIdentifier scopedIdentifier =
          r.into(SCOPED_IDENTIFIER.fields())
              .into(de.mig.mdr.dal.jooq.tables.pojos.ScopedIdentifier.class);

      if (namespaces.containsKey(element.getId())) {
        namespaces.get(element.getId()).getDefinitions().add(DefinitionHandler.convert(definition));
      } else {
        Namespace namespace = new Namespace();
        namespace.setDefinitions(new ArrayList<>());
        namespace.getDefinitions().add(DefinitionHandler.convert(definition));
        namespace.setIdentification(IdentificationHandler.convert(scopedIdentifier));
        namespaces.put(element.getId(), namespace);
      }
    }

    return new ArrayList<>(namespaces.values());
  }

  /**
   * Returns all readable namespaces, including the implicitly readable namespaces.
   */
  public static List<Namespace> getReadableNamespaces(DSLContext ctx, int userId) {
    SelectConditionStep<Record> query = getNamespacesQuery(ctx);
    query.and(DaoUtil.accessibleByUserId(ctx, userId));
    return fetchNamespaceQuery(query);
  }

  /**
   * Returns a list of namespaces which the user can explicitly read (as defined in the
   * "user_namespace_grants" table).
   */
  public static List<Namespace> getExplicitlyReadableNamespaces(DSLContext ctx, int userId) {
    return getNamespacesByGrantType(ctx, userId, GrantType.READ);
  }

  /**
   * Returns all writable namespaces (which are not hidden or which are writable for the user as
   * defined in the "user_namespace_grants" table.
   */
  public static List<Namespace> getWritableNamespaces(DSLContext ctx, int userId) {
    return getNamespacesByGrantType(ctx, userId, GrantType.WRITE);
  }

  /**
   * Returns a list of namespaces which the user has admin access to (as defined in the
   * "user_namespace_grants" table).
   */
  public static List<Namespace> getAdministrableNamespaces(DSLContext ctx, int userId) {
    return getNamespacesByGrantType(ctx, userId, GrantType.ADMIN);
  }

  /**
   * Returns a list of namespaces the user has the given access type to.
   *
   * @param grantType "READ", "WRITE" or "ADMIN"
   */
  public static List<Namespace> getNamespacesByGrantType(
      DSLContext ctx, int userId, GrantType grantType) {
    SelectConditionStep<Record> query = getNamespacesQuery(ctx);
    query.and(ELEMENT.ID.in(DaoUtil
        .getUserNamespaceGrantsQuery(ctx, userId, Collections.singletonList(grantType))));
    return fetchNamespaceQuery(query);
  }

  /**
   * Updates definition of a namespace.
   */
  public static Identification update(DSLContext ctx, int userId, Namespace namespace) {
    Namespace previousNamespace = get(ctx, userId, namespace.getIdentification().getIdentifier());

    //update scopedIdentifier if status != DRAFT
    if (previousNamespace.getIdentification().getStatus() != Status.DRAFT) {

      ScopedIdentifier scopedIdentifier =
          IdentificationHandler
              .updateNamespaceIdentifier(ctx, userId, namespace.getIdentification(),
                  ElementHandler.getIdentifiedElementRecord(ctx, namespace.getIdentification())
                      .getId());
      namespace.setIdentification(IdentificationHandler.convert(scopedIdentifier));
      namespace.getIdentification().setNamespaceId(
          Integer.parseInt(previousNamespace.getIdentification().getUrn().split(":")[1]));
    }

    List<UserNamespaceGrants> namespaceGrants = GrantTypeHandler
        .getGrantsForNamespace(ctx, previousNamespace.getIdentification().getNamespaceId());
    delete(ctx, userId, previousNamespace.getIdentification().getUrn());
    ScopedIdentifier newScopedIdentifier = create(ctx, userId, namespace);
    IdentificationHandler.convert(newScopedIdentifier);

    namespaceGrants.forEach(g -> {
      g.setNamespaceId(newScopedIdentifier.getNamespaceId());
    });

    GrantTypeHandler.setGrantsForNamespace(ctx, namespaceGrants);

    return IdentificationHandler.convert(newScopedIdentifier);
  }
}
