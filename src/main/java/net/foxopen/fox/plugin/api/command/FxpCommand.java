package net.foxopen.fox.plugin.api.command;

import java.util.HashMap;
import java.util.Map;

import net.foxopen.fox.plugin.api.dom.FxpDOM;

public abstract class FxpCommand {

  private final Map<String, String> mAttributeMap;

  protected FxpCommand(FxpDOM<FxpDOM> pCommandDOM) {
    mAttributeMap = new HashMap<String, String>();
    mAttributeMap.putAll(pCommandDOM.getAttributeMap());
  }

  public Map<String, String> getAttributeMap() {
    return mAttributeMap;
  }

  public abstract String getCommandName();

  //TODO return type?
  public abstract void run(FxpCommandContext pCommandContext);

}
