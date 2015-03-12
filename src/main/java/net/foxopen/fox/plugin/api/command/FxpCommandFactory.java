package net.foxopen.fox.plugin.api.command;

import java.util.Collection;

import net.foxopen.fox.plugin.api.dom.FxpDOM;

public interface FxpCommandFactory {

  public FxpCommand create(FxpDOM pCommandDOM);

  Collection<String> getCommandElementNames();

}
