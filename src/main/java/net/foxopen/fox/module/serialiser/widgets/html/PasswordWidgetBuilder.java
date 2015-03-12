package net.foxopen.fox.module.serialiser.widgets.html;

import net.foxopen.fox.module.datanode.EvaluatedNode;
import net.foxopen.fox.module.serialiser.html.HTMLSerialiser;
import net.foxopen.fox.module.serialiser.widgets.WidgetBuilder;

public class PasswordWidgetBuilder {
  private static final WidgetBuilder<HTMLSerialiser, EvaluatedNode> INSTANCE = new InputWidgetBuilder("password");

  public static final WidgetBuilder<HTMLSerialiser, EvaluatedNode> getInstance() {
    return INSTANCE;
  }
}
