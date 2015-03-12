package net.foxopen.fox.plugin;

import com.google.common.base.Joiner;
import net.foxopen.fox.command.Command;
import net.foxopen.fox.command.flow.XDoControlFlow;
import net.foxopen.fox.command.flow.XDoControlFlowContinue;
import net.foxopen.fox.dom.DOM;
import net.foxopen.fox.ex.ExInternal;
import net.foxopen.fox.module.Mod;
import net.foxopen.fox.plugin.api.CommandProvidingPlugin;
import net.foxopen.fox.plugin.api.PluginManagerContext;
import net.foxopen.fox.plugin.api.command.FxpCommandContext;
import net.foxopen.fox.plugin.api.command.FxpCommandFactory;
import net.foxopen.fox.thread.ActionRequestContext;
import net.foxopen.fox.track.Track;

import java.util.Collection;
import java.util.Map;

/**
 * Implementation of a Command provided by a Plugin. The parsed command is not stored or cached in case the plugin is reloaded
 * or disabled (modules should not store references such as these, as it could cause memory leaks). When the command is
 * invoked, an actual command object is constructed and executed just in time using the currently active version of the plugin.
 */
public class PluginCommandWrapper
implements Command {

  /** Command markup as it appeared in the module definition */
  private final DOM mCommandMarkup;

  /** Name of plugin to use to create the actual command from */
  private final String mPluginName;

  PluginCommandWrapper(DOM pCommandMarkup, String pPluginName) {
    mCommandMarkup = pCommandMarkup;
    mPluginName = pPluginName;
  }

  @Override
  public XDoControlFlow run(ActionRequestContext pRequestContext) {

    CommandProvidingPlugin lCommandPlugin = PluginManager.instance().resolveCommandProvidingPlugin(mPluginName);

    //Create command JIT in case plugin has changed
    Collection<? extends FxpCommandFactory> lFactories = lCommandPlugin.getCommandFactories();

    //Locate the correct factory in the target plugin
    FxpCommandFactory lTargetFactory = null;
    String lCommandName = mCommandMarkup.getLocalName();
    for(FxpCommandFactory lFactory : lFactories) {
      if(lFactory.getCommandElementNames().contains(lCommandName)) {
        lTargetFactory = lFactory;
      }
    }

    if(lTargetFactory != null) {
      // Get the command plugin context
      PluginManagerContext lLoadedPlugin =  PluginManager.instance().getLoadedPluginManagerContext(mPluginName);
      //Create a new command context from the current request context so the command has access to ContextUElem, ContextUCon etc
      FxpCommandContext lRequestContextWrapper = new PluginCommandRequestContextWrapper(pRequestContext, lLoadedPlugin);
      //Create and run the command
      lTargetFactory.create(mCommandMarkup).run(lRequestContextWrapper);
      //TODO PN - should the plugin command be able to override this?
      return XDoControlFlowContinue.instance();
    }
    else {
      throw new ExInternal("Failed to find a command factory in plugin " + mPluginName + " to run command " + lCommandName);
    }
  }

  @Override
  public boolean isCallTransition() {
    return false;
  }

  @Override
  public String getCommandName() {
    return  mCommandMarkup.getLocalName();
  }

  @Override
  public void writeTrackData() {
    for(Map.Entry<String, String> lAttr : mCommandMarkup.getAttributeMap().entrySet()) {
      Track.addAttribute(lAttr.getKey(), lAttr.getValue());
    }
  }

  public String getDebugInfo() {
    return getCommandName() + " " + Joiner.on(", ").withKeyValueSeparator("=").join(mCommandMarkup.getAttributeMap());
  }

  @Override
  public void validate(Mod pModule) {
  }
}
