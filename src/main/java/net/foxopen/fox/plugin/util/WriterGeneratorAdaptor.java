package net.foxopen.fox.plugin.util;

import net.foxopen.fox.command.util.WriterGenerator;
import net.foxopen.fox.plugin.api.command.util.FxpWriterGenerator;

import java.io.IOException;
import java.io.Writer;

public class WriterGeneratorAdaptor
implements WriterGenerator {

  private final FxpWriterGenerator mWrappedGenerator;

  public WriterGeneratorAdaptor(FxpWriterGenerator pWrappedGenerator) {
    mWrappedGenerator = pWrappedGenerator;
  }

  @Override
  public void writeOutput(Writer pWriter) throws IOException {
    //Proxy the generation request through to the plugin's generator
    mWrappedGenerator.writeOutput(pWriter);
  }
}
