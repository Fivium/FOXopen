package net.foxopen.fox.module.serialiser.widgets.html;

import net.foxopen.fox.module.datanode.EvaluatedNode;
import net.foxopen.fox.module.datanode.EvaluatedNodeInfoPhantomBufferItem;
import net.foxopen.fox.module.serialiser.SerialisationContext;
import net.foxopen.fox.module.serialiser.html.HTMLSerialiser;
import net.foxopen.fox.module.serialiser.widgets.WidgetBuilder;


public class PhantomBufferWidgetBuilder extends WidgetBuilderHTMLSerialiser<EvaluatedNode> {
  private static final WidgetBuilder<HTMLSerialiser, EvaluatedNode> INSTANCE = new PhantomBufferWidgetBuilder();

  public static final WidgetBuilder<HTMLSerialiser, EvaluatedNode> getInstance() {
    return INSTANCE;
  }

  private PhantomBufferWidgetBuilder () {
  }

  @Override
  public void buildWidgetInternal(SerialisationContext pSerialisationContext, HTMLSerialiser pSerialiser, EvaluatedNode pEvalNode) {
    EvaluatedNodeInfoPhantomBufferItem lEvaluatedNodeInfoPhantomBufferItem = (EvaluatedNodeInfoPhantomBufferItem)pEvalNode;
    // Serialise the buffer
    lEvaluatedNodeInfoPhantomBufferItem.getPhantomBuffer().render(pSerialisationContext, pSerialiser);
  }
}
