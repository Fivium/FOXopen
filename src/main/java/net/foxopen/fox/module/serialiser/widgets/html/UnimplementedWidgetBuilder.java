package net.foxopen.fox.module.serialiser.widgets.html;

import net.foxopen.fox.module.datanode.EvaluatedNode;
import net.foxopen.fox.module.serialiser.SerialisationContext;
import net.foxopen.fox.module.serialiser.html.HTMLSerialiser;
import net.foxopen.fox.module.serialiser.widgets.WidgetBuilder;

public class UnimplementedWidgetBuilder extends WidgetBuilderHTMLSerialiser<EvaluatedNode> {
  private static final WidgetBuilder<HTMLSerialiser, EvaluatedNode> INSTANCE = new UnimplementedWidgetBuilder();

  public static final WidgetBuilder<HTMLSerialiser, EvaluatedNode> getInstance() {
    return INSTANCE;
  }

  @Override
  public void buildPrompt(HTMLSerialiser pSerialiser, EvaluatedNode pEvalNode) {
    if (pEvalNode.hasPrompt()) {
      pSerialiser.append("UNIMPLEMENTED-PROMPT " + pEvalNode);
    }
  }

  @Override
  public void buildWidgetInternal(SerialisationContext pSerialisationContext, HTMLSerialiser pSerialiser, EvaluatedNode pEvalNode) {
    pSerialiser.append("UNIMPLEMENTED-WIDGET " + pEvalNode);
  }
}
