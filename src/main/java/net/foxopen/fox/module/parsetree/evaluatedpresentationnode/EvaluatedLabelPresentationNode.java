package net.foxopen.fox.module.parsetree.evaluatedpresentationnode;

import net.foxopen.fox.XFUtil;
import net.foxopen.fox.dom.DOM;
import net.foxopen.fox.ex.ExActionFailed;
import net.foxopen.fox.ex.ExCardinality;
import net.foxopen.fox.ex.ExInternal;
import net.foxopen.fox.module.parsetree.EvaluatedParseTree;
import net.foxopen.fox.module.parsetree.presentationnode.LabelPresentationNode;
import net.foxopen.fox.module.parsetree.presentationnode.PresentationNode;
import net.foxopen.fox.module.serialiser.components.ComponentBuilderType;

/**
 * Evaluate an fm:label for serialising later. The label might be targeting an element that may be setout or, if this
 * is being evaluated in a buffer while there is a context node available on the Evaluated Parse Tree, target a node
 * which is currently being evaluated for setout.
 */
public class EvaluatedLabelPresentationNode extends EvaluatedPresentationNode<PresentationNode> {
  private final DOM mForTargetElement;

  /**
   * Evaluate a LabelPresentationNode object by evaluating the attributes
   *
   * @param pParent
   * @param pOriginalPresentationNode
   * @param pEvaluatedParseTree
   * @param pEvalContext
   */
  public EvaluatedLabelPresentationNode(EvaluatedPresentationNode<? extends PresentationNode> pParent, LabelPresentationNode pOriginalPresentationNode, EvaluatedParseTree pEvaluatedParseTree, DOM pEvalContext) {
    super(pParent, pOriginalPresentationNode, pEvalContext);

    String lForTarget = pOriginalPresentationNode.getForTarget();

    if (!XFUtil.isNull(lForTarget)) {
      try {
        mForTargetElement = pEvaluatedParseTree.getContextUElem().extendedXPath1E(getEvalContext(), lForTarget);
      }
      catch (ExCardinality pExCardinality) {
        throw new ExInternal("For Target attribute on fm:label must match 1 element", pExCardinality);
      }
      catch (ExActionFailed pExActionFailed) {
        throw new ExInternal("Failed to run XPath in attribute on fm:label", pExActionFailed);
      }
    }
    else {
      mForTargetElement = pEvaluatedParseTree.getCurrentBufferLabelTargetElement();
    }
  }

  @Override
  public String toString() {
    return getOriginalNode().toString();
  }

  @Override
  public ComponentBuilderType getPageComponentType() {
    return ComponentBuilderType.LABEL;
  }

  /**
   * Get the target element for the label or null if none specified.
   * @return
   */
  public DOM getForTargetElementOrNull() {
    return mForTargetElement;
  }
}
