package net.foxopen.fox.module.serialiser.widgets.html;

import net.foxopen.fox.module.datanode.EvaluatedNode;
import net.foxopen.fox.module.serialiser.html.HTMLSerialiser;
import net.foxopen.fox.module.serialiser.widgets.WidgetBuilder;

public class SubmitWidgetBuilder {
  private static final WidgetBuilder<HTMLSerialiser, EvaluatedNode> INSTANCE = new ButtonWidgetBuilder("submit");

  public static final WidgetBuilder<HTMLSerialiser, EvaluatedNode> getInstance() {
    return INSTANCE;
  }
}
