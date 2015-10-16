package net.foxopen.fox.module.serialiser.widgets.pdf;

import net.foxopen.fox.ex.ExInternal;
import net.foxopen.fox.module.datanode.EvaluatedNode;
import net.foxopen.fox.module.serialiser.SerialisationContext;
import net.foxopen.fox.module.serialiser.pdf.PDFSerialiser;
import net.foxopen.fox.module.serialiser.widgets.WidgetBuilder;

public class UnimplementedWidgetBuilder
extends WidgetBuilderPDFSerialiser<EvaluatedNode> {
  private static final WidgetBuilder<PDFSerialiser, EvaluatedNode> INSTANCE = new UnimplementedWidgetBuilder();

  public static final WidgetBuilder<PDFSerialiser, EvaluatedNode> getInstance() {
    return INSTANCE;
  }

  @Override
  public void buildPrompt(SerialisationContext pSerialisationContext, PDFSerialiser pSerialiser, EvaluatedNode pEvalNode) {
    if (pEvalNode.hasPrompt()) {
      throw new ExInternal("FOX5 PDF generation does not support unimplemented widget prompt: " + pEvalNode.getIdentityInformation());
    }
  }

  @Override
  public void buildWidgetInternal(SerialisationContext pSerialisationContext, PDFSerialiser pSerialiser, EvaluatedNode pEvalNode) {
    throw new ExInternal("FOX5 PDF generation does not support unimplemented widget: " + pEvalNode.getIdentityInformation());
  }
}
