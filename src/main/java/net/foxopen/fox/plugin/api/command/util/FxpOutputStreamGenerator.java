package net.foxopen.fox.plugin.api.command.util;

import java.io.IOException;
import java.io.OutputStream;

/**
 * Implementors of this interface write to an output stream which has been pre-selected by the main engine. The engine
 * handles all opening/closing logic.
 */
public interface FxpOutputStreamGenerator
extends FxpGenerator {

  /**
   * Writes output to an output stream which is managed by the main engine.
   * @param pOutputStream Destination.
   * @throws IOException
   */
  void writeOutput(OutputStream pOutputStream) throws IOException;

}
