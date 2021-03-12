package de.mig.mdr.model;

import static de.mig.mdr.dal.jooq.Tables.ELEMENT;
import static de.mig.mdr.dal.jooq.Tables.HIERARCHY;
import static de.mig.mdr.dal.jooq.Tables.USER_NAMESPACE_GRANTS;

import de.mig.mdr.dal.ResourceManager;
import de.mig.mdr.dal.jooq.enums.GrantType;
import de.mig.mdr.dal.jooq.tables.Element;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.Record1;
import org.jooq.SelectConditionStep;

public class DaoUtil {

  private static final String[] materializedViews = new String[] {HIERARCHY.getName()};
  public static final List<GrantType> READ_ACCESS_GRANTS =
      Collections.unmodifiableList(Arrays.asList(GrantType.READ, GrantType.WRITE, GrantType.ADMIN));
  public static final List<GrantType> WRITE_ACCESS_GRANTS =
      Collections.unmodifiableList(Arrays.asList(GrantType.WRITE, GrantType.ADMIN));
  public static final List<GrantType> ADMIN_ACCESS_GRANTS =
      Collections.unmodifiableList(Arrays.asList(GrantType.ADMIN));

  /**
   * Returns a condition which checks whether a user is able to access and see a namespace or not.
   */
  public static Condition accessibleByUserId(DSLContext ctx, int userId) {
    return ELEMENT.HIDDEN.eq(false)
        .or(ELEMENT.ID.in(getUserNamespaceGrantsQuery(ctx, userId, READ_ACCESS_GRANTS)));
  }

  /**
   * Returns a condition which checks whether a user is able to access and see a namespace or not.
   * Requires the Element table for check as parameter.
   */
  public static Condition accessibleByUserId(DSLContext ctx, int userId, Element element) {
    return element.HIDDEN.eq(false)
        .or(element.ID.in(getUserNamespaceGrantsQuery(ctx, userId, READ_ACCESS_GRANTS)));
  }

  /**
   * Returns a condition which checks whether a user has the required grant to a namespace or not.
   */
  public static SelectConditionStep<Record1<Integer>> getUserNamespaceGrantsQuery(
      DSLContext ctx, int userId, List<GrantType> grantTypes) {
    return ctx.select(USER_NAMESPACE_GRANTS.NAMESPACE_ID)
        .from(USER_NAMESPACE_GRANTS)
        .where(USER_NAMESPACE_GRANTS.USER_ID.eq(userId))
        .and(USER_NAMESPACE_GRANTS.GRANT_TYPE.in(grantTypes));
  }

  /** Refreshes all materialized views. */
  public static void refreshMaterializedViews() {
    try (DSLContext ctx = ResourceManager.getDslContext()) {
      for (String view : materializedViews) {
        // TODO: Maybe there is a better way to do this with JOOQ?
        ctx.execute("REFRESH MATERIALIZED VIEW " + view);
      }
    }
  }

}
