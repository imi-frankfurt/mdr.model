package de.mig.mdr.model.handler;

import static de.mig.mdr.dal.jooq.tables.MdrUser.MDR_USER;
import static de.mig.mdr.dal.jooq.tables.UserNamespaceGrants.USER_NAMESPACE_GRANTS;

import de.mig.mdr.dal.ResourceManager;
import de.mig.mdr.dal.jooq.enums.GrantType;
import de.mig.mdr.dal.jooq.tables.pojos.MdrUser;
import de.mig.mdr.dal.jooq.tables.pojos.UserNamespaceGrants;
import de.mig.mdr.dal.jooq.tables.records.IdentifiedElementRecord;
import de.mig.mdr.model.handler.element.NamespaceHandler;
import org.jooq.DSLContext;

public class UserHandler {

  /**
   * Get a user by auth id.
   */
  public static MdrUser getUserByIdentity(DSLContext ctx, String identity) {
    if ("anonymousUser".equals(identity)) {
      MdrUser anon = new MdrUser();
      anon.setId(-1);
      return anon;
    }
    try {
      return ctx.fetchOne(MDR_USER, MDR_USER.AUTH_ID.equal(identity)).into(MdrUser.class);
    } catch (NullPointerException npe) {
      return null;
    }
  }

  /**
   * Get a user by database id.
   */
  public static MdrUser getUserById(DSLContext ctx, int userId) {
    return ctx.fetchOne(MDR_USER, MDR_USER.ID.equal(userId)).into(MdrUser.class);
  }

  /**
   * Store a user in the database.
   */
  public static int saveUser(DSLContext ctx, MdrUser mdrUser) {
    return ctx.newRecord(MDR_USER, mdrUser).store();
  }

  /**
   * Update a user.
   */
  public static void updateUser(DSLContext ctx, MdrUser mdrUser) {
    ctx.newRecord(MDR_USER, mdrUser).update();
  }

  public static MdrUser createDefaultUser(DSLContext ctx, String authId, String email,
      String userName) {
    return createDefaultUser(ctx, authId, email, userName, null, null);
  }

  /**
   * Create a default user.
   */
  public static MdrUser createDefaultUser(DSLContext ctx, String authId, String email,
      String userName, String firstName, String lastName) {
    MdrUser mdrUser = new MdrUser();
    mdrUser.setAuthId(authId);
    mdrUser.setEmail(email);
    mdrUser.setUserName(userName);
    mdrUser.setFirstName(firstName);
    mdrUser.setLastName(lastName);
    int userId = saveUser(ctx, mdrUser);
    mdrUser.setId(userId);
    return mdrUser;
  }

  /**
   * Give a user access to a namespace.
   */
  public static void setUserAccessToNamespace(int userId, int namespaceIdentifier,
      GrantType grantType) {
    try (DSLContext ctx = ResourceManager.getDslContext()) {
      IdentifiedElementRecord namespaceRecord = NamespaceHandler
          .getLatestNamespaceRecord(ctx, userId, namespaceIdentifier);
      UserNamespaceGrants userNamespaceGrants = new UserNamespaceGrants();
      userNamespaceGrants.setUserId(userId);
      userNamespaceGrants.setNamespaceId(namespaceRecord.getId());
      userNamespaceGrants.setGrantType(grantType);
      ctx.newRecord(USER_NAMESPACE_GRANTS, userNamespaceGrants).insert();
    }
  }
}
