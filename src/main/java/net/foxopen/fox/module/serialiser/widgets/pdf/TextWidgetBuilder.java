package net.foxopen.fox.module.serialiser.widgets.pdf;

import net.foxopen.fox.module.datanode.EvaluatedNode;
import net.foxopen.fox.module.serialiser.SerialisationContext;
import net.foxopen.fox.module.serialiser.pdf.PDFSerialiser;
import net.foxopen.fox.module.serialiser.widgets.WidgetBuilder;


public class TextWidgetBuilder
extends WidgetBuilderPDFSerialiser<EvaluatedNode> {
  private static final WidgetBuilder<PDFSerialiser, EvaluatedNode> INSTANCE = new TextWidgetBuilder();

  public static final WidgetBuilder<PDFSerialiser, EvaluatedNode> getInstance() {
    return INSTANCE;
  }

  private TextWidgetBuilder() {
  }

  @Override
  public boolean hasPrompt(EvaluatedNode pEvalNode) {
    return false;
  }

  @Override
  public void buildPrompt(SerialisationContext pSerialisationContext, PDFSerialiser pSerialiser, EvaluatedNode pEvalNode) {
  }

  @Override
  public void buildWidgetInternal(SerialisationContext pSerialisationContext, PDFSerialiser pSerialiser, EvaluatedNode pEvalNode) {
    pSerialiser.addText(pEvalNode.getFieldMgr().getSingleTextValue());
  }
}
