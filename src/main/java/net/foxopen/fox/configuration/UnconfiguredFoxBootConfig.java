package net.foxopen.fox.configuration;

import java.util.Collections;
import java.util.Map;

public class UnconfiguredFoxBootConfig implements FoxBootConfig {

  @Override
  public String getDatabaseURL() {
    return "";
  }

  @Override
  public String getMainDatabaseUsername() {
    return "";
  }

  @Override
  public String getDbPassword() {
    return "";
  }

  @Override
  public String getFoxEnvironmentKey() {
    return "";
  }

  @Override
  public String getProductionStatus() {
    return "";
  }

  @Override
  public String getFoxServiceList() {
    return "";
  }

  @Override
  public String getSupportUsername() {
    return "";
  }

  @Override
  public String getSupportPassword() {
    return "";
  }

  @Override
  public String getAdminUsername() {
    return DEFAULT_ADMIN_USERNAME;
  }

  @Override
  public String getAdminPassword() {
    return FoxConfigHelper.hashInternalPassword(DEFAULT_ADMIN_PASSWORD);
  }

  @Override
  public boolean isProduction() {
    return false;
  }

  @Override
  public Map<String, String> getFoxDatabaseUserMap() {
    return Collections.emptyMap();
  }

  @Override
  public String getDatabaseUserPassword(String pUsername) { return ""; }

  @Override
  public String getFoxEnginePort() { return ""; }
}
