package net.foxopen.fox.command.util;

import java.io.IOException;
import java.io.Writer;

/**
 * An object which can provide a character Writer to a {@link GeneratorDestination}.
 */
public interface WriterGenerator {

  void writeOutput(Writer pWriter) throws IOException;

}
