package net.foxopen.fox.module.serialiser.widgets.html;

import net.foxopen.fox.module.datanode.EvaluatedNodeInfo;
import net.foxopen.fox.module.serialiser.html.HTMLSerialiser;
import net.foxopen.fox.module.serialiser.widgets.WidgetBuilder;
import net.foxopen.fox.module.serialiser.widgets.WidgetBuilderType;

public class RadioWidgetBuilder {
  private static final WidgetBuilder<HTMLSerialiser, EvaluatedNodeInfo> INSTANCE = new MultiOptionSelectWidget(WidgetBuilderType.RADIO);

  public static final WidgetBuilder<HTMLSerialiser, EvaluatedNodeInfo> getInstance() {
    return INSTANCE;
  }
}
