package net.foxopen.fox.command.builtin;

import net.foxopen.fox.command.AbstractCommand;
import net.foxopen.fox.command.Command;
import net.foxopen.fox.dom.DOM;
import net.foxopen.fox.ex.ExInternal;
import net.foxopen.fox.module.Mod;


/**
 * All Commands which are built in to the engine should extend this class.
 */
public abstract class BuiltInCommand
extends AbstractCommand
implements Command {

  /**
  * Constructs and initialises a command from the XML element
  * specified.
  *
  * @param commandElement the element from which the command will
  *        be constructed.
  */
  public BuiltInCommand(DOM pCommandDOM) {
    super(pCommandDOM);
  }

   /**
   * Called to allow the component to validate its syntax and structure.
   *
   * <p>Although XML Schema provides for the basic validation of the command,
   * there is some validation (cross references with the Module/App) that
   * can only be done by the component itself.
   *
   * @param module the module where the component resides
   * @throws ExInternal if the component syntax is invalid.
   */
   public void validate(Mod module) {
   }
}
