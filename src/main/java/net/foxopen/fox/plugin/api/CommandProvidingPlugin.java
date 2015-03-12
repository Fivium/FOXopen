package net.foxopen.fox.plugin.api;

import java.util.Collection;

import net.foxopen.fox.plugin.api.command.FxpCommandFactory;

public interface CommandProvidingPlugin
extends FoxPlugin {

  public Collection<? extends FxpCommandFactory> getCommandFactories();

}
