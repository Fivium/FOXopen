package net.foxopen.fox.plugin.api.command.util;

import net.foxopen.fox.plugin.api.dom.FxpDOM;

/**
 * An object which writes output to a DOM which has been pre-selected by the engine. The DOM will be written back to its
 * original location.
 */
public interface FxpDOMGenerator
extends FxpGenerator {

  /**
   * Manipulates the given DOM before it is written back to its original location.
   * @param pTargetDOM DOM to write output to.
   */
  void writeOutput(FxpDOM pTargetDOM);

}
