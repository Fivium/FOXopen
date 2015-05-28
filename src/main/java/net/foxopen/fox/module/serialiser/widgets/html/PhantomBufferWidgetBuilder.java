package net.foxopen.fox.module.serialiser.widgets.html;

import net.foxopen.fox.module.datanode.EvaluatedNodeInfoPhantomBufferItem;
import net.foxopen.fox.module.serialiser.SerialisationContext;
import net.foxopen.fox.module.serialiser.html.HTMLSerialiser;
import net.foxopen.fox.module.serialiser.widgets.WidgetBuilder;


public class PhantomBufferWidgetBuilder extends WidgetBuilderHTMLSerialiser<EvaluatedNodeInfoPhantomBufferItem> {
  private static final WidgetBuilder<HTMLSerialiser, EvaluatedNodeInfoPhantomBufferItem> INSTANCE = new PhantomBufferWidgetBuilder();

  public static WidgetBuilder<HTMLSerialiser, EvaluatedNodeInfoPhantomBufferItem> getInstance() {
    return INSTANCE;
  }

  private PhantomBufferWidgetBuilder () {
  }

  @Override
  public void buildWidgetInternal(SerialisationContext pSerialisationContext, HTMLSerialiser pSerialiser, EvaluatedNodeInfoPhantomBufferItem pEvalNode) {
    // Serialise the buffer
    pEvalNode.getPhantomBuffer().render(pSerialisationContext, pSerialiser);
  }
}
