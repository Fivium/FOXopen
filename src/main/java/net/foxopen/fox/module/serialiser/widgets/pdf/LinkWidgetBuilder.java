package net.foxopen.fox.module.serialiser.widgets.pdf;

import net.foxopen.fox.ex.ExInternal;
import net.foxopen.fox.module.datanode.EvaluatedNode;
import net.foxopen.fox.module.serialiser.SerialisationContext;
import net.foxopen.fox.module.serialiser.pdf.PDFSerialiser;
import net.foxopen.fox.module.serialiser.widgets.WidgetBuilder;


public class LinkWidgetBuilder extends WidgetBuilderPDFSerialiser<EvaluatedNode> {
  private static final WidgetBuilder<PDFSerialiser, EvaluatedNode> INSTANCE = new LinkWidgetBuilder();

  public static final WidgetBuilder<PDFSerialiser, EvaluatedNode> getInstance() {
    return INSTANCE;
  }

  private LinkWidgetBuilder() {
  }

  @Override
  public boolean hasPrompt(EvaluatedNode pEvalNodeInfo) {
    return false;
  }

  @Override
  public void buildPrompt(SerialisationContext pSerialisationContext, PDFSerialiser pSerialiser, EvaluatedNode pEvalNodeInfo) {
  }

  @Override
  public void buildWidgetInternal(SerialisationContext pSerialisationContext, PDFSerialiser pSerialiser, EvaluatedNode pEvalNode) {
    if (pEvalNode.isRunnable() || pEvalNode.isPlusWidget()) {
      if (!pEvalNode.hasPrompt()) {
        throw new ExInternal("Link widget found with no prompt, this is invisible on the page but the action is still there and likely not intended: " + pEvalNode.getIdentityInformation());
      }

      pSerialiser.addText(pEvalNode.getPrompt().getString());
    }
  }
}
