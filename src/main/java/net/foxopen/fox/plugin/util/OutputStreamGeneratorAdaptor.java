package net.foxopen.fox.plugin.util;

import net.foxopen.fox.command.util.OutputStreamGenerator;
import net.foxopen.fox.plugin.api.command.util.FxpOutputStreamGenerator;

import java.io.IOException;
import java.io.OutputStream;

public class OutputStreamGeneratorAdaptor
implements OutputStreamGenerator{

  private final FxpOutputStreamGenerator mWrappedGenerator;

  public OutputStreamGeneratorAdaptor(FxpOutputStreamGenerator pWrappedGenerator) {
    mWrappedGenerator = pWrappedGenerator;
  }

  @Override
  public void writeOutput(OutputStream pOutputStream) throws IOException {
    //Proxy the generation request through to the plugin's generator
    mWrappedGenerator.writeOutput(pOutputStream);
  }
}
