package net.foxopen.fox.module.datanode;


import net.foxopen.fox.XFUtil;
import net.foxopen.fox.module.clientvisibility.EvaluatedClientVisibilityRule;
import net.foxopen.fox.module.parsetree.evaluatedpresentationnode.GenericAttributesEvaluatedPresentationNode;
import net.foxopen.fox.module.parsetree.presentationnode.GenericAttributesPresentationNode;

import java.util.Collection;
import java.util.Collections;

/**
 * All types of real EvaluatedNodeInfo have Client Visibility, so take that into account here.
 * This class is not extended by the Stub ENI.
 *
 * It also holds the generic method for registering an EvaluatedNodeInfo on its grandparent list if it's marked up as
 * non-collapsible.
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
  public boolean isInitiallyDisplayed() {
    return mEvalClientVisibilityRule == null || mEvalClientVisibilityRule.isVisible();
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

  /**
   * Register the EvaluatedNodeInfo as a non-collapsible column if it's a column in a list, will at least be visible,
   * and has a fox:hasContent attribute defined and set to true, or it has no fox:hasContent attribute but has text
   * content somewhere in it.
   */
  protected void registerCollapsibleColumnStatus() {
    // If this item is going to be in a list...
    if (getParent() instanceof EvaluatedNodeInfoCollection && getParent().getParent() instanceof EvaluatedNodeInfoList) {
      // ...and it's going to be visible ...
      if (getVisibility().asInt() >= NodeVisibility.VIEW.asInt()) {
        EvaluatedNodeInfoList lListContainer = (EvaluatedNodeInfoList) getParent().getParent();
        // ...and it has a fox:hasContent attribute defined and set to true, or it has no fox:hasContent attribute but has text content somewhere in it...
        if ((isAttributeDefined(NodeAttribute.HAS_CONTENT) && getBooleanAttribute(NodeAttribute.HAS_CONTENT, false))
          || (!isAttributeDefined(NodeAttribute.HAS_CONTENT) && XFUtil.exists(getDataItem().value(true)))) {
          // Mark the column as non-collapsible on the list
          lListContainer.registerNonCollapsibleColumn(getNodeInfo());
        }
      }
    }
  }
}
