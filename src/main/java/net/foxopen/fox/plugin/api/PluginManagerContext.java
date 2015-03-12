package net.foxopen.fox.plugin.api;

import net.foxopen.fox.plugin.api.dom.FxpDOM;

/**
 * This object is provided to a FoxPlugin to allow it to communicate with its owning PluginManager, in particular to
 * retrieve config details.
 */
public interface PluginManagerContext {

  public boolean hasConfiguration();

  public String getConfigParamString(String pParamName);

  public FxpDOM getConfigParamDOM(String pParamName);

  /**
   * Logs a message from the plugin which will be visible in !STATUS.
   * @param pMessage Message string to log.
   */
  public void logMessage(String pMessage);

}
