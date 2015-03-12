package net.foxopen.fox.module.parsetree.presentationnode;

import net.foxopen.fox.XFUtil;
import net.foxopen.fox.dom.DOM;
import net.foxopen.fox.ex.ExInternal;
import net.foxopen.fox.module.datanode.NodeAttribute;
import net.foxopen.fox.module.parsetree.EvaluatedParseTree;
import net.foxopen.fox.module.parsetree.evaluatedpresentationnode.EvaluatedActionOutPresentationNode;
import net.foxopen.fox.module.parsetree.evaluatedpresentationnode.EvaluatedPresentationNode;


public class ActionOutPresentationNode extends GenericAttributesPresentationNode {
  private final String mActionName;
  private final String mActionContextXPath;

  public ActionOutPresentationNode(DOM pCurrentNode) {
    super(pCurrentNode);

    mActionName = getAttrOrNull("action");
    if (XFUtil.isNull(mActionName)) {
      throw new ExInternal("Found an fm:action-out with no action attribute containing an action name");
    }

    mActionContextXPath = getAttrOrNull(NodeAttribute.ACTION_CONTEXT_DOM.getExternalString());

    // This type of node has no children to process
  }

  public String getActionName() {
    return mActionName;
  }

  public String getActionContextXPath() {
    return mActionContextXPath;
  }

  public String toString() {
    return "ActionOutNode ("+mActionName+")";
  }

  public EvaluatedActionOutPresentationNode evaluate(EvaluatedPresentationNode<? extends PresentationNode> pParent, EvaluatedParseTree pEvaluatedParseTree, DOM pEvalContext) {
    return new EvaluatedActionOutPresentationNode(pParent, this, pEvaluatedParseTree, pEvalContext);
  }
}
