package net.foxopen.fox.plugin.util;

import net.foxopen.fox.command.util.DOMGenerator;
import net.foxopen.fox.dom.DOM;
import net.foxopen.fox.plugin.api.command.util.FxpDOMGenerator;

public class DOMGeneratorAdaptor
implements DOMGenerator {

  private final FxpDOMGenerator mWrappedGenerator;

  public DOMGeneratorAdaptor(FxpDOMGenerator pWrappedGenerator) {
    mWrappedGenerator = pWrappedGenerator;
  }

  @Override
  public void writeOutput(DOM pTargetDOM) {
    mWrappedGenerator.writeOutput(pTargetDOM);
  }
}
