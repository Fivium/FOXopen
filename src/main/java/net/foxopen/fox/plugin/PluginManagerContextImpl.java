package net.foxopen.fox.plugin;

import net.foxopen.fox.XFUtil;
import net.foxopen.fox.dom.DOM;
import net.foxopen.fox.plugin.api.PluginManagerContext;
import net.foxopen.fox.plugin.api.dom.FxpDOM;
import net.foxopen.fox.plugin.api.ex.ExInternalPlugin;

/**
 * Bridge between a FoxPlugin and a LoadedPlugin registration entry in the PluginManager.
 */
public class PluginManagerContextImpl
implements PluginManagerContext {
  private final LoadedPlugin mLoadedPlugin;
  private DOM mPluginConfig;

  PluginManagerContextImpl(LoadedPlugin pLoadedPlugin, DOM pPluginConfig) {
    mLoadedPlugin = pLoadedPlugin;
    mPluginConfig = pPluginConfig;
  }

  @Override
  public boolean hasConfiguration() {
    if (mPluginConfig != null) {
      return true;
    }
    else {
     return false;
    }
  }

  @Override
  public String getConfigParamString(String pParamName) {
    if (!XFUtil.isNull(pParamName)) {
      if (mPluginConfig != null) {
        return mPluginConfig.get1SNoEx("/*/" + pParamName);
      }
      else {
        throw new ExInternalPlugin("Try to acquire a parameter '" + pParamName + "' that was not found in the config xml");
      }
    } else {
      throw new ExInternalPlugin("Try to acquire a parameter that was specified as null");
    }
  }

  @Override
  public FxpDOM getConfigParamDOM(String pParamName) {
    if (!XFUtil.isNull(pParamName)) {
      return mPluginConfig.get1EOrNull("/*/" + pParamName);
    }
    else {
      throw new ExInternalPlugin("Try to acquire a parameter that was specified as null");
    }
  }

  @Override
  public void logMessage(String pMessage) {
    mLoadedPlugin.logMessage(pMessage);
  }
}
