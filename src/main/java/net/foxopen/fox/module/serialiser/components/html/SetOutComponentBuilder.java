package net.foxopen.fox.module.serialiser.components.html;

import net.foxopen.fox.module.datanode.EvaluatedNode;
import net.foxopen.fox.module.parsetree.evaluatedpresentationnode.EvaluatedSetOutPresentationNode;
import net.foxopen.fox.module.serialiser.SerialisationContext;
import net.foxopen.fox.module.serialiser.components.ComponentBuilder;
import net.foxopen.fox.module.serialiser.html.HTMLSerialiser;
import net.foxopen.fox.module.serialiser.widgets.WidgetBuilderType;


public class SetOutComponentBuilder extends ComponentBuilder<HTMLSerialiser, EvaluatedSetOutPresentationNode> {
  private static final ComponentBuilder<HTMLSerialiser, EvaluatedSetOutPresentationNode> INSTANCE = new SetOutComponentBuilder();

  public static final ComponentBuilder<HTMLSerialiser, EvaluatedSetOutPresentationNode> getInstance() {
    return INSTANCE;
  }

  private SetOutComponentBuilder() {
  }

  @Override
  public void buildComponent(SerialisationContext pSerialisationContext, HTMLSerialiser pSerialiser, EvaluatedSetOutPresentationNode pEvalNode) {
    EvaluatedNode lENI = pEvalNode.getChildEvaluatedNodeOrNull();
    if(lENI != null) {
      WidgetBuilderType lWidgetBuilder = lENI.getWidgetBuilderType();
      if (!lWidgetBuilder.isInternalOnly()) {
        SingleWidgetBuildHelper.buildWidget(pSerialisationContext, lENI, pSerialiser);
      }
      else {
        pSerialiser.getWidgetBuilder(lWidgetBuilder).buildWidget(pSerialisationContext, pSerialiser, lENI);
      }
    }
  }
}
