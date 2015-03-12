package net.foxopen.fox.configuration;

import net.foxopen.fox.ex.ExFoxConfiguration;

import java.util.Map;

public interface FoxBootConfig {

  public static final String DEFAULT_ADMIN_USERNAME = "admin";
  public static final String DEFAULT_ADMIN_PASSWORD = "admin";

  String getDatabaseURL();

  String getMainDatabaseUsername();

  String getDbPassword();

  /**
   * "DEVELOPMENT" or "PRODUCTION".
   * @return
   */
  String getProductionStatus();

  String getFoxServiceList();

  String getSupportUsername();

  String getSupportPassword();

  String getAdminUsername();

  String getAdminPassword();

  boolean isProduction();

  Map<String, String> getFoxDatabaseUserMap();

  String getFoxEnvironmentKey();

  String getDatabaseUserPassword(String pUsername) throws ExFoxConfiguration;

  String getFoxEnginePort();
}
