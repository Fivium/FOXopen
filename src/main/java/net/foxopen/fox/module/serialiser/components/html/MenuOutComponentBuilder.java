package net.foxopen.fox.module.serialiser.components.html;

import net.foxopen.fox.module.parsetree.evaluatedpresentationnode.EvaluatedMenuOutPresentationNode;
import net.foxopen.fox.module.parsetree.evaluatedpresentationnode.EvaluatedPresentationNode;
import net.foxopen.fox.module.serialiser.SerialisationContext;
import net.foxopen.fox.module.serialiser.components.ComponentBuilder;
import net.foxopen.fox.module.serialiser.html.HTMLSerialiser;


public class MenuOutComponentBuilder extends ComponentBuilder<HTMLSerialiser, EvaluatedPresentationNode> {
  private static final ComponentBuilder<HTMLSerialiser, EvaluatedPresentationNode> INSTANCE = new MenuOutComponentBuilder();

  public static final ComponentBuilder<HTMLSerialiser, EvaluatedPresentationNode> getInstance() {
    return INSTANCE;
  }

  private MenuOutComponentBuilder() {
  }

  @Override
  public void buildComponent(SerialisationContext pSerialisationContext, HTMLSerialiser pSerialiser, EvaluatedPresentationNode pEvalNode) {
    EvaluatedMenuOutPresentationNode lMenuOutNode = (EvaluatedMenuOutPresentationNode)pEvalNode;

    MenuOutWidgetHelper.buildWidget(pSerialisationContext, pSerialiser, lMenuOutNode);
  }
}
