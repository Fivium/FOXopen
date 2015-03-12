package net.foxopen.fox.configuration.resourcemaster.model;

import net.foxopen.fox.XFUtil;
import net.foxopen.fox.configuration.resourcemaster.definition.AppProperty;
import net.foxopen.fox.configuration.resourcemaster.definition.FoxApplicationDefinition;
import net.foxopen.fox.entrypoint.FoxGlobals;
import net.foxopen.fox.ex.ExApp;


public class SecurityProperties {
  private final int mPreSessionTimeoutPromptSecs;
  private final boolean mSecureCookies;
  private final boolean mExternalEntryThemeSecurity;

  public static SecurityProperties createSecurityProperties(FoxApplicationDefinition pAppDefinition) throws ExApp {
    return new SecurityProperties(pAppDefinition);
  }

  private SecurityProperties(FoxApplicationDefinition pAppDefinition) throws ExApp {
    super();
    // Security properties
    try {
      mPreSessionTimeoutPromptSecs = pAppDefinition.getPropertyAsInteger(AppProperty.SECURITY_PRE_SESSION_TIMEOUT_PROMPT_SECS);
      mSecureCookies = pAppDefinition.getPropertyAsBoolean(AppProperty.SECURITY_SECURE_COOKIES);

      // Get the entry theme security string
      String lEntryThemeSecurity = pAppDefinition.getPropertyAsString(AppProperty.SECURITY_EXTERNAL_ENTRY_THEME_SECURITY);
      // If it wasn't specified set the property based on the production mode of the engine
      if (XFUtil.isNull(lEntryThemeSecurity)) {
        mExternalEntryThemeSecurity = FoxGlobals.getInstance().getFoxBootConfig().isProduction() ? true : false;
      } else {
        // Otherwise set the property to the provided value
        mExternalEntryThemeSecurity = Boolean.valueOf(lEntryThemeSecurity);
      }
    }
    catch (ExApp e) {
      throw new ExApp("An error occured when trying to create the Security properties of an application. ");
    }
  }

  public int getPreSessionTimeoutPromptSecs() {
    return mPreSessionTimeoutPromptSecs;
  }

  public boolean isSecureCookies() {
    return mSecureCookies;
  }

  public boolean isExternalEntryThemeSecurity() {
    return mExternalEntryThemeSecurity;
  }
}
