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
import java.util.Collections;
import java.util.Map;

public class UnconfiguredFoxEnvironment implements FoxEnvironment {
  public UnconfiguredFoxEnvironment() {
    super();
  }

  @Override
  public Map<String, String> getEnvDisplayAttributeList() {
    return Collections.emptyMap();
  }

  @Override
  public FileServiceProperties getFileServiceProperties() {
    throw new ExInternal("Attempted to get FileServiceProperties from an unconfigured fox environment");
  }

  @Override
  public DatabaseProperties getDatabaseProperties() {
    //Don't fail here - it's legitimate to ask for database properties for an unconfigured engine
    return DatabaseProperties.defaultForUnconfiguredEngine();
  }

  @Override
  public AuthenticationProperties getAuthenticationProperties() {
    throw new ExInternal("Attempted to get AuthenticationProperties from an unconfigured fox environment");
  }

  @Override
  public String getCookieDomainMethod() {
    return "";
  }

  @Override
  public String getDefaultAppMnem() {
    return "";
  }

  @Override
  public App getAppByMnem(String pAppMnem) throws ExServiceUnavailable, ExApp, ExInternal {
    throw new ExInternal("Attempted to get an app from an unconfigured fox environment. App mnem : " + pAppMnem);
  }

  @Override
  public App getAppByMnem(String pAppMnem, boolean pReturnDefaultIfNotKnown) throws ExServiceUnavailable, ExApp, ExInternal {
    throw new ExInternal("Attempted to get an app from an unconfigured fox environment. App mnem : " + pAppMnem);
  }

  @Override
  public App getDefaultApp() {
    throw new ExInternal("Attempted to get default app from an unconfigured fox environment.");
  }

  @Override
  public Collection<App> getAllApps() {
    throw new ExInternal("Attempted to get all apps from an unconfigured fox environment.");
  }

  @Override
  public void flushApplicationCache() throws ExApp, ExFoxConfiguration, ExServiceUnavailable {
    // Do nothing on an unconfigured app
  }

  @Override
  public FoxEnvironmentDefinition getEnvironmentDefinition() {
    throw new ExInternal("Attempted to get an environment definition from an unconfigured environment");
  }

  @Override
  public boolean isValidAppMnem(String pAppMnem) {
    throw new ExInternal("Attempted to check a valid app when the engine was not configured. App Mnem : " + pAppMnem);
  }

  @Override
  public Reader getClobAuxiliaryConfigOrNull(ContextUCon pContextUCon, String pConfigMnem) {
    return null;
  }
}
