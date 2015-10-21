package net.foxopen.fox.module.serialiser.widgets.pdf;

import net.foxopen.fox.module.datanode.EvaluatedNode;
import net.foxopen.fox.module.serialiser.SerialisationContext;
import net.foxopen.fox.module.serialiser.pdf.PDFSerialiser;
import net.foxopen.fox.module.serialiser.widgets.WidgetBuilder;
import net.foxopen.fox.track.Track;

public class IgnoreUnimplementedWidgetBuilder
extends WidgetBuilderPDFSerialiser<EvaluatedNode> {
  private static final WidgetBuilder<PDFSerialiser, EvaluatedNode> INSTANCE = new IgnoreUnimplementedWidgetBuilder();

  public static final WidgetBuilder<PDFSerialiser, EvaluatedNode> getInstance() {
    return INSTANCE;
  }

  @Override
  public void buildPrompt(SerialisationContext pSerialisationContext, PDFSerialiser pSerialiser, EvaluatedNode pEvalNode) {
    if (pEvalNode.hasPrompt()) {
      Track.alert("IgnoreUnsupported", "Ignoring unsupported widget prompt: " + pEvalNode.getIdentityInformation());
    }
  }

  @Override
  public void buildWidgetInternal(SerialisationContext pSerialisationContext, PDFSerialiser pSerialiser, EvaluatedNode pEvalNode) {
    Track.alert("IgnoreUnsupported", "Ignoring unsupported widget: " + pEvalNode.getIdentityInformation());
  }
}
