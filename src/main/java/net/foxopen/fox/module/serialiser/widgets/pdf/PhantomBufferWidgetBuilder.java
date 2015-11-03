package net.foxopen.fox.module.serialiser.widgets.pdf;

import net.foxopen.fox.module.datanode.EvaluatedNodeInfoPhantomBufferItem;
import net.foxopen.fox.module.serialiser.SerialisationContext;
import net.foxopen.fox.module.serialiser.pdf.PDFSerialiser;
import net.foxopen.fox.module.serialiser.widgets.WidgetBuilder;


public class PhantomBufferWidgetBuilder extends WidgetBuilderPDFSerialiser<EvaluatedNodeInfoPhantomBufferItem> {
  private static final WidgetBuilder<PDFSerialiser, EvaluatedNodeInfoPhantomBufferItem> INSTANCE = new PhantomBufferWidgetBuilder();

  public static final WidgetBuilder<PDFSerialiser, EvaluatedNodeInfoPhantomBufferItem> getInstance() {
    return INSTANCE;
  }

  private PhantomBufferWidgetBuilder() {
  }

  @Override
  public void buildWidgetInternal(SerialisationContext pSerialisationContext, PDFSerialiser pSerialiser, EvaluatedNodeInfoPhantomBufferItem pEvalNode) {

    // Serialise the buffer
    pEvalNode.getPhantomBuffer().render(pSerialisationContext, pSerialiser);
  }
}
