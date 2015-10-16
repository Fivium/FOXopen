package net.foxopen.fox.module.serialiser.widgets.pdf;

import net.foxopen.fox.module.datanode.EvaluatedNode;
import net.foxopen.fox.module.datanode.EvaluatedNodeInfoPhantomBufferItem;
import net.foxopen.fox.module.serialiser.SerialisationContext;
import net.foxopen.fox.module.serialiser.pdf.PDFSerialiser;
import net.foxopen.fox.module.serialiser.widgets.WidgetBuilder;


public class PhantomBufferWidgetBuilder extends WidgetBuilderPDFSerialiser<EvaluatedNode> {
  private static final WidgetBuilder<PDFSerialiser, EvaluatedNode> INSTANCE = new PhantomBufferWidgetBuilder();

  public static final WidgetBuilder<PDFSerialiser, EvaluatedNode> getInstance() {
    return INSTANCE;
  }

  private PhantomBufferWidgetBuilder() {
  }

  @Override
  public void buildWidgetInternal(SerialisationContext pSerialisationContext, PDFSerialiser pSerialiser, EvaluatedNode pEvalNode) {
    EvaluatedNodeInfoPhantomBufferItem lEvaluatedNodeInfoPhantomBufferItem = (EvaluatedNodeInfoPhantomBufferItem)pEvalNode;
    // Serialise the buffer
    lEvaluatedNodeInfoPhantomBufferItem.getPhantomBuffer().render(pSerialisationContext, pSerialiser);
  }
}
