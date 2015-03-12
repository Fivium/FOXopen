package net.foxopen.fox.command.util;

import java.io.IOException;
import java.io.OutputStream;

/**
 * An object which can provide a binary OutputStream to a {@link GeneratorDestination}.
 */
public interface OutputStreamGenerator {

  void writeOutput(OutputStream pOutputStream) throws IOException;

}
