package net.foxopen.fox.module.datanode;


import net.foxopen.fox.module.clientvisibility.EvaluatedClientVisibilityRule;
import net.foxopen.fox.module.parsetree.evaluatedpresentationnode.GenericAttributesEvaluatedPresentationNode;
import net.foxopen.fox.module.parsetree.presentationnode.GenericAttributesPresentationNode;

import java.util.Collection;
import java.util.Collections;

/**
 * All typed of real EvaluatedNodeInfo have Client Visbility, so take that into account here.
 * This class is not extended by the Stub ENI.
 */
public abstract class EvaluatedNodeInfoGeneric extends EvaluatedNodeInfo {
  // Nullable
  private final EvaluatedClientVisibilityRule mEvalClientVisibilityRule;

  public EvaluatedNodeInfoGeneric(EvaluatedNode pParent, GenericAttributesEvaluatedPresentationNode<? extends GenericAttributesPresentationNode> pEvaluatedPresentationNode, NodeEvaluationContext pNodeEvaluationContext, NodeVisibility pNodeVisibility, NodeInfo pNodeInfo) {
    super(pParent, pEvaluatedPresentationNode, pNodeEvaluationContext, pNodeVisibility, pNodeInfo);

    //Mark widget type as implicated if the node is visible
    if(getVisibility() != NodeVisibility.DENIED) {
      super.getEvaluatedParseTree().addImplicatedWidget(getWidgetBuilderType(), this);
    }

    mEvalClientVisibilityRule = getEvaluatedParseTree().evaluateClientVisibilityRuleOrNull(this, pNodeEvaluationContext);
  }

  @Override
  public Collection<String> getCellInternalClasses() {
    if(mEvalClientVisibilityRule != null) {
      return Collections.singleton(mEvalClientVisibilityRule.getInitialCSSClass());
    }
    else {
      return Collections.emptySet();
    }
  }
}
