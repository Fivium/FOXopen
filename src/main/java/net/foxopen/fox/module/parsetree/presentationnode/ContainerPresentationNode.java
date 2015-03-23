package net.foxopen.fox.module.parsetree.presentationnode;

import net.foxopen.fox.dom.DOM;
import net.foxopen.fox.module.parsetree.EvaluatedParseTree;
import net.foxopen.fox.module.parsetree.ParseTree;
import net.foxopen.fox.module.parsetree.evaluatedpresentationnode.EvaluatedContainerPresentationNode;
import net.foxopen.fox.module.parsetree.evaluatedpresentationnode.EvaluatedPresentationNode;

/**
 * This Presentation Node stores the any fm: prefixed elements that we want to ignore, yet contain other elements
 */
public class ContainerPresentationNode extends PresentationNode {
  private final String mTagName;
  private String mDebugInfo;

  /**
   * Attributes are stored by default in PresentationNode so just construct this to store the tag name
   *
   * @param pSourceDOM Container element
   */
  public ContainerPresentationNode(DOM pSourceDOM) {
    mTagName = pSourceDOM.getName();

    // Process children
    ParseTree.parseDOMChildren(this, pSourceDOM, false);
  }

  @Override
  public EvaluatedContainerPresentationNode evaluate(EvaluatedPresentationNode<? extends PresentationNode> pParent, EvaluatedParseTree pEvaluatedParseTree, DOM pEvalContext) {
    return new EvaluatedContainerPresentationNode(pParent, this, pEvaluatedParseTree, pEvalContext);
  }

  public void setDebugInfo(String pDebugInfo) {
    mDebugInfo = pDebugInfo;
  }

  public String getDebugInfo() {
    return mDebugInfo;
  }

  public String toString() {
    return "Container (" + mTagName + " : " + getDebugInfo() + ")";
  }
}
