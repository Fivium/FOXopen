package net.foxopen.fox.command;

import net.foxopen.fox.command.flow.XDoControlFlow;
import net.foxopen.fox.module.Mod;
import net.foxopen.fox.thread.ActionRequestContext;
import net.foxopen.fox.track.Trackable;

/**
 * Representation of a command as marked up in FOX module XML. Typically each command element is represented by a separate
 * Command object, although some nesting may occur (in this case, parent commands will contain nested XDoCommandLists which
 * they control the execution of).<br><br>
 *
 * A Command is instantiated by its associated CommandFactory, which is in turn registered on the global CommandProvider.
 */
public interface Command
extends Trackable {

  public XDoControlFlow run(ActionRequestContext pRequestContext);

  public boolean isCallTransition();

  public String getCommandName();

  public String getDebugInfo();

  public void validate(Mod pModule);

}
