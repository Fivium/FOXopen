package net.foxopen.fox.module.serialiser.components;

import net.foxopen.fox.module.parsetree.evaluatedpresentationnode.EvaluatedPresentationNode;
import net.foxopen.fox.module.parsetree.presentationnode.PresentationNode;
import net.foxopen.fox.module.serialiser.OutputSerialiser;
import net.foxopen.fox.module.serialiser.SerialisationContext;

public abstract class ComponentBuilder<OS extends OutputSerialiser, EPN extends EvaluatedPresentationNode> {
  /**
   * Called by the serialiser to render the component to the serialisers writer
   *
   * @param Serialiser Serialiser to serialise with
   * @param EvalNode Node for the Page Component Builders
   */
  public abstract void buildComponent(SerialisationContext pSerialisationContext, OS pSerialiser, EPN pEvalNode);

  /**
   * Recurse through the children
   */
  protected void processChildren(SerialisationContext pSerialisationContext, OutputSerialiser pSerialiser, EvaluatedPresentationNode<? extends PresentationNode> pEvalNode) {
    if (pEvalNode.getChildren().size() > 0) {
      for (EvaluatedPresentationNode lNestedNode : pEvalNode.getChildren()) {
        lNestedNode.render(pSerialisationContext, pSerialiser);
      }
    }
  }
}
