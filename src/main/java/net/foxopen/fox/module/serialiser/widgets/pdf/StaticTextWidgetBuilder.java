package net.foxopen.fox.module.serialiser.widgets.pdf;

import net.foxopen.fox.module.datanode.EvaluatedNode;
import net.foxopen.fox.module.serialiser.SerialisationContext;
import net.foxopen.fox.module.serialiser.pdf.PDFSerialiser;
import net.foxopen.fox.module.serialiser.widgets.WidgetBuilder;


/**
 * Static Text widget doesn't actually show any text content, it only shows the prompt text
 */
public class StaticTextWidgetBuilder extends WidgetBuilderPDFSerialiser<EvaluatedNode> {
  private static final WidgetBuilder<PDFSerialiser, EvaluatedNode> INSTANCE = new StaticTextWidgetBuilder();

  public static final WidgetBuilder<PDFSerialiser, EvaluatedNode> getInstance() {
    return INSTANCE;
  }

  private StaticTextWidgetBuilder() {
  }

  @Override
  public void buildWidgetInternal(SerialisationContext pSerialisationContext, PDFSerialiser pSerialiser, EvaluatedNode pEvalNode) {
  }
}
