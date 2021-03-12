package de.mig.mdr.model.handler.element.section;

import static de.mig.mdr.dal.jooq.tables.ScopedIdentifier.SCOPED_IDENTIFIER;
import static de.mig.mdr.dal.jooq.tables.ScopedIdentifierHierarchy.SCOPED_IDENTIFIER_HIERARCHY;

import de.mig.mdr.model.dto.element.Element;
import de.mig.mdr.model.dto.element.section.Identification;
import de.mig.mdr.dal.jooq.enums.Status;
import de.mig.mdr.model.dto.element.section.Member;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.jooq.DSLContext;

public class MemberHandler {

  /**
   * Create Members (DataElements/DataElementGroups/Records) for a given DataElementGroup or
   * Record.
   **/
  public static void create(
      DSLContext ctx, int userId, List<Element> members, int superScopedIdentifierId)
      throws IllegalArgumentException {
    if (members != null || !(members.isEmpty())) {
      saveElementsAsMembers(ctx, members, superScopedIdentifierId, userId);
    }
    //TODO: check member.status
  }

  /**
   * get all Members (DataElements/DataElementGroups/Records) for a given DataElementGroup, Record.
   **/
  public static List<Member> get(
      DSLContext ctx, Identification identification) {
    List<Member> members = new ArrayList<>();
    List<Integer> subIds = getSubIds(ctx, identification);
    List<de.mig.mdr.dal.jooq.tables.pojos.ScopedIdentifier> scopedIdentifiers =
        getScopedIdentifiers(ctx, subIds/*, identification.getElementType()*/);
    scopedIdentifiers.forEach(
        (scopedIdentifier) -> {
          Member member = new Member();
          member.setElementUrn(IdentificationHandler.toUrn(scopedIdentifier));
          member.setStatus(scopedIdentifier.getStatus());
          members.add(member);
        });
    return members;
  }

  /**
   * Update Members (DataElements/Records) for a DataElementGroup or Record.
   **/
  public static void update(
      DSLContext ctx, int userId, Member members, int superScopedIdentifierId)  {
    // TODO
  }

  /**
   * Save the relationship between DataElements and their parent DataElementGroup/Record.
   **/
  public static void saveElementsAsMembers(
      DSLContext ctx, List<Element> members, int superScopedIdentifierId, int userId)
      throws IllegalArgumentException {
    members.forEach(
        (member) -> saveElementAsMember(ctx, member, superScopedIdentifierId, userId));
  }

  /**
   * Save the relationship between DataElements and their parent DataElementGroup/Record.
   **/
  public static void saveElementAsMember(
      DSLContext ctx, Element member, int superScopedIdentifierId, int userId)
      throws IllegalArgumentException {
    saveScopedIdentifierHierarchy(ctx, superScopedIdentifierId, member.getIdentification(), userId);
  }

  /**
   * Save relationship between SuperScopedIdentifierId & SubScopedIdentifierId into DB.
   **/
  public static void saveScopedIdentifierHierarchy(DSLContext ctx, Integer superScopedIdentifierId,
      Identification identification, int userId) {
    int scopedIdentifierId = IdentificationHandler.getScopedIdentifier(ctx, userId, identification)
        .getId();
    ctx.insertInto(SCOPED_IDENTIFIER_HIERARCHY)
        .set(SCOPED_IDENTIFIER_HIERARCHY.SUPER_ID, superScopedIdentifierId)
        .set(SCOPED_IDENTIFIER_HIERARCHY.SUB_ID, scopedIdentifierId)
        .execute();
  }

  /**
   * get subIds for a given superScopedIdentifierId.
   **/
  public static List<Integer> getSubIds(DSLContext ctx, Identification identification) {
    return ctx.select()
        .from(
            de.mig.mdr.dal.jooq.tables.Element.ELEMENT
                .join(SCOPED_IDENTIFIER)
                .on(
                    de.mig.mdr.dal.jooq.tables.Element.ELEMENT.ID.eq(
                        SCOPED_IDENTIFIER.ELEMENT_ID))
                .join(SCOPED_IDENTIFIER_HIERARCHY)
                .on(SCOPED_IDENTIFIER.ID.eq(SCOPED_IDENTIFIER_HIERARCHY.SUPER_ID)))
        .where(SCOPED_IDENTIFIER.IDENTIFIER.eq(identification.getIdentifier()))
        .and(SCOPED_IDENTIFIER.VERSION.eq(identification.getRevision()))
        .and(SCOPED_IDENTIFIER.ELEMENT_TYPE.eq(identification.getElementType()))
        .fetch()
        .getValues(SCOPED_IDENTIFIER_HIERARCHY.SUB_ID);
  }

  /**
   * get scopedIdentifiers for a given list of subIds (just one ElementType).
   **/
  public static List<de.mig.mdr.dal.jooq.tables.pojos.ScopedIdentifier> getScopedIdentifiers(
      DSLContext ctx, List<Integer> subIds) {
    return ctx.select()
        .from(SCOPED_IDENTIFIER)
        .where(SCOPED_IDENTIFIER.ID.in(subIds))
        .fetchInto(SCOPED_IDENTIFIER)
        .into(de.mig.mdr.dal.jooq.tables.pojos.ScopedIdentifier.class);
  }

  /**
   * Check if all SubIds are Released.
   **/
  public static Boolean allSubIdsAreReleased(DSLContext ctx, Identification identification) {
    List<Integer> subIds = getSubIds(ctx, identification);
    AtomicReference<Boolean> allReleased = new AtomicReference<>(true);
    List<de.mig.mdr.dal.jooq.tables.pojos.ScopedIdentifier> scopedIdentifiers =
        new ArrayList<>();
    scopedIdentifiers.addAll(getScopedIdentifiers(ctx, subIds));
    scopedIdentifiers.forEach(
        (scopedIdentifier) -> {
          if (scopedIdentifier.getStatus() != Status.RELEASED) {
            allReleased.set(false);
          }
        });
    return allReleased.get();
  }

  /**
   * remove unwanted relationships between a given superScopedIdentifierId &
   * subScopedIdentifierIds.
   **/
  public static void deleteUnmentionedSubIds(
      DSLContext ctx, List<Integer> newSubIds, int superScopedIdentifierId) {
    // TODO
  }
}
