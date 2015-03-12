package net.foxopen.fox.plugin.api.command.util;

import java.io.InputStream;

/**
 * Implementors of this interface consume an input stream which has been pre-selected by the main engine. The engine
 * handles all opening/closing logic.
 */
public interface FxpInputStreamConsumer extends FxpConsumer {

  /**
   * Performs any actions required to read an input stream while it is open.
   * @param pInputStream Stream to be read.
   */
  public void consume(InputStream pInputStream);

}
