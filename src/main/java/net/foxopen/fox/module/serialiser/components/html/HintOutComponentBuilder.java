package net.foxopen.fox.module.serialiser.components.html;

import net.foxopen.fox.module.parsetree.evaluatedpresentationnode.EvaluatedHintOutPresentationNode;
import net.foxopen.fox.module.parsetree.evaluatedpresentationnode.EvaluatedPresentationNode;
import net.foxopen.fox.module.serialiser.SerialisationContext;
import net.foxopen.fox.module.serialiser.components.ComponentBuilder;
import net.foxopen.fox.module.serialiser.html.HTMLSerialiser;


public class HintOutComponentBuilder extends ComponentBuilder<HTMLSerialiser, EvaluatedPresentationNode> {
  private static final ComponentBuilder<HTMLSerialiser, EvaluatedPresentationNode> INSTANCE = new HintOutComponentBuilder();

  public static final ComponentBuilder<HTMLSerialiser, EvaluatedPresentationNode> getInstance() {
    return INSTANCE;
  }

  private HintOutComponentBuilder() {
  }

  @Override
  public void buildComponent(SerialisationContext pSerialisationContext, HTMLSerialiser pSerialiser, EvaluatedPresentationNode pEvalNode) {
    EvaluatedHintOutPresentationNode lHintOutNode = (EvaluatedHintOutPresentationNode)pEvalNode;
    pSerialiser.addHint(lHintOutNode.getHint());
  }
}
