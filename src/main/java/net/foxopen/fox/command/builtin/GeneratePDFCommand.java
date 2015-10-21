package net.foxopen.fox.command.builtin;

import net.foxopen.fox.XFUtil;
import net.foxopen.fox.command.Command;
import net.foxopen.fox.command.CommandFactory;
import net.foxopen.fox.command.flow.XDoControlFlow;
import net.foxopen.fox.command.flow.XDoControlFlowContinue;
import net.foxopen.fox.command.util.GenerateMethod;
import net.foxopen.fox.command.util.GeneratorDestination;
import net.foxopen.fox.command.util.GeneratorDestinationUtils;
import net.foxopen.fox.dom.DOM;
import net.foxopen.fox.ex.ExInternal;
import net.foxopen.fox.module.Mod;
import net.foxopen.fox.module.fieldset.FieldSet;
import net.foxopen.fox.module.parsetree.EvaluatedParseTree;
import net.foxopen.fox.module.serialiser.pdf.PDFSerialiser;
import net.foxopen.fox.thread.ActionRequestContext;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Command for generating a pdf document from a buffer
 */
public class GeneratePDFCommand extends BuiltInCommand {
  private static final String COMMAND_NAME = "generate-pdf";
  private static final String DEFAULT_FILE_EXTENSION = "pdf";
  private static final String DEFAULT_CONTENT_TYPE = "application/pdf";
  /**
   * The allowed values and the generation methods they represent for the generate command method attribute
   */
  private static final Map<String, GenerateMethod> GENERATE_METHOD_VALUES = new HashMap<>();
  static {
    GENERATE_METHOD_VALUES.put("download", GenerateMethod.DOWNLOAD);
    GENERATE_METHOD_VALUES.put("storage-location", GenerateMethod.STORAGE_LOCATION);
  }

  private final GeneratorDestination mGeneratorDestination;
  private final String mBufferName;
  private final boolean mIsDebug;
  private final boolean mIsIgnoreUnsupported;

  private GeneratePDFCommand(DOM pMarkupDOM) {
    super(pMarkupDOM);
    mGeneratorDestination = GeneratorDestinationUtils.getDestinationFromGenerateCommandMarkup(pMarkupDOM, DEFAULT_FILE_EXTENSION,
                                                                                              DEFAULT_CONTENT_TYPE, GENERATE_METHOD_VALUES);
    mBufferName = pMarkupDOM.getAttrOrNull("buffer");
    mIsDebug = Boolean.valueOf(pMarkupDOM.getAttrOrNull("debug"));
    mIsIgnoreUnsupported = Boolean.valueOf(pMarkupDOM.getAttrOrNull("ignore-unsupported"));

    if (XFUtil.isNull(mBufferName)) {
      throw new ExInternal("Required attribute 'buffer' not provided to the " + COMMAND_NAME + " command");
    }
  }

  @Override
  public XDoControlFlow run(ActionRequestContext pRequestContext) {
    mGeneratorDestination.generateToOutputStream(pRequestContext, pOutputStream -> {
      EvaluatedParseTree lEPT = new EvaluatedParseTree(pRequestContext, FieldSet.createNewFieldSet(pRequestContext),
                                                       Collections.emptyList(), mBufferName);
      PDFSerialiser lOutputSerialiser = new PDFSerialiser(lEPT, mIsDebug, mIsIgnoreUnsupported);
      lOutputSerialiser.serialise(pOutputStream);
    });

    return XDoControlFlowContinue.instance();
  }

  public boolean isCallTransition() {
    return false;
  }

  public static class Factory implements CommandFactory {
    @Override
    public Command create(Mod pModule, DOM pMarkupDOM) {
      return new GeneratePDFCommand(pMarkupDOM);
    }

    @Override
    public Collection<String> getCommandElementNames() {
      return Collections.singleton(COMMAND_NAME);
    }
  }
}
