package net.foxopen.fox.command.builtin;

import java.io.IOException;
import java.io.OutputStream;

import net.foxopen.fox.ContextUCon;
import net.foxopen.fox.ContextUElem;
import net.foxopen.fox.command.flow.XDoControlFlow;
import net.foxopen.fox.command.flow.XDoControlFlowContinue;
import net.foxopen.fox.command.util.DownloadGeneratorDestination;
import net.foxopen.fox.command.util.GeneratorDestinationUtils;
import net.foxopen.fox.command.util.OutputStreamGenerator;
import net.foxopen.fox.dom.DOM;
import net.foxopen.fox.plugin.PluginManager;
import net.foxopen.fox.plugin.api.callout.DocgenPluginCallout;
import net.foxopen.fox.plugin.callout.DocgenPluginCalloutImpl;
import net.foxopen.fox.thread.ActionRequestContext;

public class ShowPopupDocTemplateCommand
extends BuiltInCommand {

  /** Document template preview attributes. */
  private final String mDocTemplateName;

  private final DownloadGeneratorDestination mDownloadDestination;

  ShowPopupDocTemplateCommand(DOM pCommandDOM) {
    super(pCommandDOM);

    mDocTemplateName = pCommandDOM.getAttrOrNull("doc-template-name");

    mDownloadDestination = GeneratorDestinationUtils.getDestinationFromShowPopupCommandMarkup(pCommandDOM, "pdf", "application/pdf");
  }

  @Override
  public XDoControlFlow run(ActionRequestContext pRequestContext) {

    final ContextUElem lContextUElem = pRequestContext.getContextUElem();
    final ContextUCon lContextUCon = pRequestContext.getContextUCon();

    mDownloadDestination.generateToOutputStream(pRequestContext, new OutputStreamGenerator() {
      @Override
      public void writeOutput(OutputStream pOutputStream) throws IOException {
        DocgenPluginCallout lCallout = new DocgenPluginCalloutImpl(lContextUElem, lContextUCon, mAttributeMap, pOutputStream);
        PluginManager.instance().processCallout(lCallout);
      }
    });

    return XDoControlFlowContinue.instance();
  }

  @Override
  public boolean isCallTransition() {
    return false;
  }
}
