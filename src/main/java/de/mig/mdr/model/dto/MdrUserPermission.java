package de.mig.mdr.model.dto;

import de.mig.mdr.dal.jooq.enums.GrantType;
import lombok.Data;

@Data
public class MdrUserPermission {

  private String userName;
  private String firstName;
  private String lastName;
  private String email;
  private GrantType grantType;
}
