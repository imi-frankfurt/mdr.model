package de.mig.mdr.model.handler;

import static de.mig.mdr.dal.jooq.Tables.ELEMENT;
import static de.mig.mdr.dal.jooq.Tables.MDR_USER;
import static de.mig.mdr.dal.jooq.Tables.SCOPED_IDENTIFIER;
import static de.mig.mdr.dal.jooq.Tables.USER_NAMESPACE_GRANTS;

import de.mig.mdr.dal.jooq.enums.ElementType;
import de.mig.mdr.dal.jooq.enums.GrantType;
import de.mig.mdr.dal.jooq.tables.pojos.UserNamespaceGrants;
import de.mig.mdr.model.dto.MdrUserPermission;
import de.mig.mdr.model.handler.element.NamespaceHandler;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import org.jooq.DSLContext;

public class GrantTypeHandler {

  /**
   * Returns highest grant type of the given user and namespace.
   */
  public static GrantType getGrantTypeByUserAndNamespace(DSLContext ctx, int userId,
      int namespaceId) {
    GrantType highestGrantType = null;
    List<GrantType> grantTypes =
        ctx.select(USER_NAMESPACE_GRANTS.GRANT_TYPE).from(USER_NAMESPACE_GRANTS)
            .leftJoin(SCOPED_IDENTIFIER)
            .on(USER_NAMESPACE_GRANTS.NAMESPACE_ID.eq(SCOPED_IDENTIFIER.NAMESPACE_ID))
            .where(SCOPED_IDENTIFIER.IDENTIFIER.eq(namespaceId))
            .and(SCOPED_IDENTIFIER.ELEMENT_TYPE.eq(ElementType.NAMESPACE))
            .and(USER_NAMESPACE_GRANTS.USER_ID.eq(userId)).fetchInto(GrantType.class);

    for (GrantType grantType : grantTypes) {
      if (grantType.equals(GrantType.ADMIN)) {
        highestGrantType = grantType;
        break;
      } else if (grantType.equals(GrantType.WRITE)) {
        highestGrantType = grantType;
      } else if (grantType == GrantType.READ && highestGrantType == null) {
        highestGrantType = grantType;
      }
    }
    return highestGrantType;
  }


  /**
   * Returns all grants for a given namespace.
   */
  public static List<UserNamespaceGrants> getGrantsForNamespace(DSLContext ctx, int namespaceId) {
    return
        ctx.select(USER_NAMESPACE_GRANTS.fields()).from(USER_NAMESPACE_GRANTS)
            .where(USER_NAMESPACE_GRANTS.NAMESPACE_ID.eq(namespaceId))
            .fetchInto(UserNamespaceGrants.class);
  }


  /**
   * Insert grants for a namespace.
   */
  public static void setGrantsForNamespace(DSLContext ctx, List<UserNamespaceGrants> grants) {
    grants.forEach(g -> {
      ctx.insertInto(USER_NAMESPACE_GRANTS, USER_NAMESPACE_GRANTS.USER_ID,
          USER_NAMESPACE_GRANTS.NAMESPACE_ID, USER_NAMESPACE_GRANTS.GRANT_TYPE)
          .values(g.getUserId(), g.getNamespaceId(), g.getGrantType()).onConflictDoNothing()
          .execute();
    });
  }

  /**
   * Returns a list of users with according grantType from a given namespace.
   */
  public static List<MdrUserPermission> getMdrUserPermissionByNamespace(
      DSLContext ctx, int userId, int namespaceId) {
    if (GrantTypeHandler.getGrantTypeByUserAndNamespace(ctx, userId,
        NamespaceHandler.getLatestNamespaceRecord(ctx, userId, namespaceId).getId())
        .equals(GrantType.ADMIN)) {
      List<MdrUserPermission> mdrUserPermissions;

      mdrUserPermissions = ctx.select(USER_NAMESPACE_GRANTS.GRANT_TYPE)
          .select(MDR_USER.fields())
          .from(USER_NAMESPACE_GRANTS)
          .leftJoin(MDR_USER)
          .on(MDR_USER.ID.eq(USER_NAMESPACE_GRANTS.USER_ID))
          .where(USER_NAMESPACE_GRANTS.USER_ID.eq(userId))
          .and(USER_NAMESPACE_GRANTS.NAMESPACE_ID.eq(ELEMENT.ID
          )).fetch().into(MdrUserPermission.class);

      HashMap<String, List<GrantType>> userPermission = new HashMap<>();

      for (MdrUserPermission mdrUserPermission : mdrUserPermissions) {
        if (userPermission.containsKey(mdrUserPermission.getUserName())) {
          userPermission.get(mdrUserPermission.getUserName()).add(mdrUserPermission.getGrantType());
        } else {
          List<GrantType> grantTypes = new ArrayList<>();
          grantTypes.add(mdrUserPermission.getGrantType());
          userPermission.put(mdrUserPermission.getUserName(), grantTypes);
        }
      }

      return mdrUserPermissions;
    } else {
      throw new IllegalArgumentException("You don´t have permission to access these information.");
    }
  }
}
