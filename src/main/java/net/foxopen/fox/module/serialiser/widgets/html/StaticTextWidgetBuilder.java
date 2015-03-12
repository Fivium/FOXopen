package net.foxopen.fox.module.serialiser.widgets.html;

import net.foxopen.fox.module.datanode.EvaluatedNode;
import net.foxopen.fox.module.serialiser.SerialisationContext;
import net.foxopen.fox.module.serialiser.html.HTMLSerialiser;
import net.foxopen.fox.module.serialiser.widgets.WidgetBuilder;


/**
 * Static Text widget doesn't actually show any text content, it only shows the prompt text
 */
public class StaticTextWidgetBuilder extends WidgetBuilderHTMLSerialiser<EvaluatedNode> {
  private static final WidgetBuilder<HTMLSerialiser, EvaluatedNode> INSTANCE = new StaticTextWidgetBuilder();

  public static final WidgetBuilder<HTMLSerialiser, EvaluatedNode> getInstance() {
    return INSTANCE;
  }

  private StaticTextWidgetBuilder () {
  }

  @Override
  public void buildWidgetInternal(SerialisationContext pSerialisationContext, HTMLSerialiser pSerialiser, EvaluatedNode pEvalNode) {
  }
}
