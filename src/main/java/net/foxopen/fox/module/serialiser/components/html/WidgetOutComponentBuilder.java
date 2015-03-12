package net.foxopen.fox.module.serialiser.components.html;

import net.foxopen.fox.module.datanode.EvaluatedNode;
import net.foxopen.fox.module.parsetree.evaluatedpresentationnode.EvaluatedPresentationNode;
import net.foxopen.fox.module.parsetree.evaluatedpresentationnode.EvaluatedWidgetOutPresentationNode;
import net.foxopen.fox.module.serialiser.SerialisationContext;
import net.foxopen.fox.module.serialiser.components.ComponentBuilder;
import net.foxopen.fox.module.serialiser.html.HTMLSerialiser;


public class WidgetOutComponentBuilder extends ComponentBuilder<HTMLSerialiser, EvaluatedPresentationNode> {
  private static final ComponentBuilder<HTMLSerialiser, EvaluatedPresentationNode> INSTANCE = new WidgetOutComponentBuilder();

  public static final ComponentBuilder<HTMLSerialiser, EvaluatedPresentationNode> getInstance() {
    return INSTANCE;
  }

  private WidgetOutComponentBuilder() {
  }

  @Override
  public void buildComponent(SerialisationContext pSerialisationContext, HTMLSerialiser pSerialiser, EvaluatedPresentationNode pEvalNode) {
    EvaluatedWidgetOutPresentationNode lWidgetOutNode = (EvaluatedWidgetOutPresentationNode)pEvalNode;
    EvaluatedNode lWidgetOutEvalNode = lWidgetOutNode.getEvaluatedNode();

    SingleWidgetBuildHelper.buildWidget(pSerialisationContext, lWidgetOutEvalNode, pSerialiser, lWidgetOutNode.isShowPrompt(), lWidgetOutNode.isShowWidget(), lWidgetOutNode.isShowError(), lWidgetOutNode.isShowHint(), lWidgetOutNode.isShowDescription());
  }
}
