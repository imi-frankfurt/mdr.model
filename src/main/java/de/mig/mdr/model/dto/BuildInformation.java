package de.mig.mdr.model.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BuildInformation {

  private String buildVersion;
  private String buildDate;
  private String buildBranch;
  private String buildHash;

}
