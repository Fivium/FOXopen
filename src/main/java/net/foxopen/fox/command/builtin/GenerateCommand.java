package net.foxopen.fox.command.builtin;

import net.foxopen.fox.XFUtil;
import net.foxopen.fox.command.Command;
import net.foxopen.fox.command.CommandFactory;
import net.foxopen.fox.command.flow.XDoControlFlow;
import net.foxopen.fox.command.flow.XDoControlFlowContinue;
import net.foxopen.fox.command.util.GeneratorDestination;
import net.foxopen.fox.command.util.GeneratorDestinationUtils;
import net.foxopen.fox.command.util.WriterGenerator;
import net.foxopen.fox.dom.DOM;
import net.foxopen.fox.ex.ExDoSyntax;
import net.foxopen.fox.module.Mod;
import net.foxopen.fox.module.State;
import net.foxopen.fox.thread.ActionRequestContext;

import java.io.IOException;
import java.io.Writer;
import java.util.Collection;
import java.util.Collections;


/**
 * Generates a buffer and sends the output somewhere
 */
public class GenerateCommand
extends BuiltInCommand {

  private final GeneratorDestination mGeneratorDestination;

  /** The buffer to generate */
  private final String mBuffer;
  private final String mState;

  private GenerateCommand(DOM pMarkupDOM) throws ExDoSyntax {
    super(pMarkupDOM);

    mBuffer = pMarkupDOM.getAttrOrNull("buffer");
    mState = pMarkupDOM.getAttrOrNull("state");

    mGeneratorDestination = GeneratorDestinationUtils.getDestinationFromGenerateCommandMarkup(pMarkupDOM, "html", "text/html");
  }


  @Override
  public XDoControlFlow run(final ActionRequestContext pRequestContext) {

    //Default state = current state
    final State lState;
    if(!XFUtil.isNull(mState)) {
      lState = pRequestContext.getCurrentModule().getState(mState);
    }
    else {
      lState = pRequestContext.getCurrentState();
    }

    mGeneratorDestination.generateToWriter(pRequestContext, new WriterGenerator(){
      @Override
      public void writeOutput(Writer pWriter) throws IOException {
        pWriter.append("HTML Gen TODO: state " + lState.getName() + " buffer " + mBuffer);
      }
    });

    return XDoControlFlowContinue.instance();
  }


  public boolean isCallTransition() {
    return false;
  }

  public static class Factory implements CommandFactory {

    @Override
    public Command create(Mod pModule, DOM pMarkupDOM) throws ExDoSyntax {

      String lOutputType = pMarkupDOM.getAttr("output-type");
      switch (lOutputType) {
        case GenerateLegacySpreadsheetCommand.OUTPUT_TYPE_CSV:
        case GenerateLegacySpreadsheetCommand.OUTPUT_TYPE_XLS:
          return new GenerateLegacySpreadsheetCommand(pMarkupDOM);
        case "SPATIAL-EMF":
          return new GenerateSpatialEMFCommand(pMarkupDOM);
        default:
          return new GenerateCommand(pMarkupDOM);
      }
    }

    @Override
    public Collection<String> getCommandElementNames() {
      return Collections.singleton("generate");
    }
  }
}
