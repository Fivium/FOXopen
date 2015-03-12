package net.foxopen.fox.configuration.resourcemaster.model;

import net.foxopen.fox.App;
import net.foxopen.fox.ContextUCon;
import net.foxopen.fox.configuration.resourcemaster.definition.FoxEnvironmentDefinition;
import net.foxopen.fox.ex.ExApp;
import net.foxopen.fox.ex.ExFoxConfiguration;
import net.foxopen.fox.ex.ExInternal;
import net.foxopen.fox.ex.ExServiceUnavailable;

import java.io.Reader;
import java.util.Collection;
import java.util.Map;

public interface FoxEnvironment {

  Map<String, String> getEnvDisplayAttributeList();

  FileServiceProperties getFileServiceProperties();

  DatabaseProperties getDatabaseProperties();

  AuthenticationProperties getAuthenticationProperties();

  String getCookieDomainMethod();

  String getDefaultAppMnem();

  App getAppByMnem(String pAppMnem) throws ExServiceUnavailable, ExApp, ExInternal;

  App getAppByMnem(String pAppMnem, boolean pReturnDefaultIfNotKnown) throws ExServiceUnavailable, ExApp, ExInternal;

  App getDefaultApp() throws ExServiceUnavailable, ExApp;

  Collection<App> getAllApps();

  boolean isValidAppMnem(String pAppMnem);

  /**Flushes internal Application Cache and nested Conponents (modules)
   * so they get read in again on next URL
   */
  void flushApplicationCache() throws ExApp, ExFoxConfiguration, ExServiceUnavailable;

  FoxEnvironmentDefinition getEnvironmentDefinition();

  Reader getClobAuxiliaryConfigOrNull(ContextUCon pContextUCon, String pConfigMnem);

}
