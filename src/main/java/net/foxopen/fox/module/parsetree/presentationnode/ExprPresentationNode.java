package net.foxopen.fox.module.parsetree.presentationnode;

import net.foxopen.fox.dom.DOM;
import net.foxopen.fox.module.parsetree.EvaluatedParseTree;
import net.foxopen.fox.module.parsetree.evaluatedpresentationnode.EvaluatedExprPresentationNode;
import net.foxopen.fox.module.parsetree.evaluatedpresentationnode.EvaluatedPresentationNode;

/**
 * This Presentation Node stores attributes for fm:expr-out elements in a module presentation block
 */
public class ExprPresentationNode extends PresentationNode {
  private final String mMatch;
  private final String mType; // TODO - NP - Perhaps there should be an enum somewhere for these things?
  private final String mFormatMask;
  private final String mMapSetName;
  private final String mMapSetItemXPath;
  private final String mMapSetAttachXPath;

  public ExprPresentationNode(DOM pCurrentNode) {
    mMatch = pCurrentNode.getAttr("match");
    mType = pCurrentNode.getAttr("type");
    mFormatMask = pCurrentNode.getAttr("formatMask");
    mMapSetName = pCurrentNode.getAttr("mapsetName");
    mMapSetItemXPath = pCurrentNode.getAttr("mapsetItem");
    mMapSetAttachXPath = pCurrentNode.getAttr("mapsetAttach");

    // This type of node has no children to process
  }

  public String getMatch() {
    return mMatch;
  }

  public String getType() {
    return mType;
  }

  public String getFormatMask() {
    return mFormatMask;
  }

  public String getMapSetName() {
    return mMapSetName;
  }

  public String getMapSetItemXPath() {
    return mMapSetItemXPath;
  }

  public String getMapSetAttachXPath() {
    return mMapSetAttachXPath;
  }

  public String toString() {
    return "Expr ("+mMatch+")";
  }

  public EvaluatedExprPresentationNode evaluate(EvaluatedPresentationNode<? extends PresentationNode> pParent, EvaluatedParseTree pEvaluatedParseTree, DOM pEvalContext) {
    return new EvaluatedExprPresentationNode(pParent, this, pEvaluatedParseTree, pEvalContext);
  }
}
