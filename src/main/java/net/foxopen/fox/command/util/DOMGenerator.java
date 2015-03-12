package net.foxopen.fox.command.util;

import net.foxopen.fox.dom.DOM;

/**
 * An object which provides a DOM for appending/editing to a GeneratorDestination.
 */
public interface DOMGenerator {

  void writeOutput(DOM pTargetDOM);

}
