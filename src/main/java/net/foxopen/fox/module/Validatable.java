package net.foxopen.fox.module;

import net.foxopen.fox.ex.ExInternal;

/**
 * An interface that should be implemented by all or most components of the Fox module, such as Entry Themes and
 * commands, to force them to have a validate method.
 */
public interface Validatable {
   /**
   * Called to allow the component to validate it's syntax and structure.
   *
   * <p>Although XML Schema provides for the basic validation of the command,
   * there is some validation (cross references with the Module/App) that
   * can only be done by the component itself.</p>
   *
   * @param module the module where the component resides
   * @throws ExInternal if the component syntax is invalid.
   */
   public void validate(Mod module) throws ExInternal;
}
