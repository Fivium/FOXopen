package net.foxopen.fox.module.serialiser.widgets.html;

import net.foxopen.fox.module.datanode.EvaluatedNodeInfo;
import net.foxopen.fox.module.datanode.EvaluatedNodeInfoPhantomMenuItem;
import net.foxopen.fox.module.serialiser.SerialisationContext;
import net.foxopen.fox.module.serialiser.components.html.MenuOutWidgetHelper;
import net.foxopen.fox.module.serialiser.html.HTMLSerialiser;
import net.foxopen.fox.module.serialiser.widgets.WidgetBuilder;


// TODO - This duplicates a lot of code from the MenuOutComponentBuilder class and should ideally share the logic
public class PhantomMenuWidgetBuilder extends WidgetBuilderHTMLSerialiser<EvaluatedNodeInfo> {
  private static final WidgetBuilder<HTMLSerialiser, EvaluatedNodeInfo> INSTANCE = new PhantomMenuWidgetBuilder();

  public static final WidgetBuilder<HTMLSerialiser, EvaluatedNodeInfo> getInstance() {
    return INSTANCE;
  }

  private PhantomMenuWidgetBuilder () {
  }

  @Override
  public boolean hasPrompt(EvaluatedNodeInfo pEvalNode) {
    return false;
  }

  @Override
  public void buildPrompt(HTMLSerialiser pSerialiser, EvaluatedNodeInfo pEvalNode) {
  }

  @Override
  public void buildWidgetInternal(SerialisationContext pSerialisationContext, HTMLSerialiser pSerialiser, EvaluatedNodeInfo pEvalNode) {
    EvaluatedNodeInfoPhantomMenuItem lPhantomMenuNode = (EvaluatedNodeInfoPhantomMenuItem)pEvalNode;

    MenuOutWidgetHelper.buildWidget(pSerialisationContext, pSerialiser, lPhantomMenuNode);
  }
}
