package net.foxopen.fox.plugin.api.command.util;

import java.io.Reader;

/**
 * Implementors of this interface consume a reader which has been pre-selected by the main engine. The engine
 * handles all opening/closing logic.
 */
public interface FxpReaderConsumer {

  /**
   * Performs any actions required to read a reader while it is open.
   * @param pReader Reader to be read.
   */
  public void consume(Reader pReader);

}
