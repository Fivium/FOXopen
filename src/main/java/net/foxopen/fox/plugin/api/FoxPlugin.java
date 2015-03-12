package net.foxopen.fox.plugin.api;

import net.foxopen.fox.plugin.api.callout.PluginCallout;

public interface FoxPlugin {

  public String getName();

  public boolean canHandleCallout(PluginCallout pCallout);

  public void processCallout(PluginCallout pCallout);

  public void setManagerContext(PluginManagerContext pManagerContext);

}
