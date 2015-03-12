package net.foxopen.fox.plugin.api.command.util;

import java.io.IOException;
import java.io.Writer;

/**
 * Implementors of this interface write to a writer which has been pre-selected by the main engine. The engine
 * handles all opening/closing logic.
 */
public interface FxpWriterGenerator
extends FxpGenerator {

  /**
   * Writes output to a writer which is managed by the main engine.
   * @param pWriter Destination.
   * @throws IOException
   */
  void writeOutput(Writer pWriter) throws IOException;

}
