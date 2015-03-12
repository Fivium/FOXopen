package net.foxopen.fox.module.parsetree.presentationnode;

import net.foxopen.fox.dom.DOM;
import net.foxopen.fox.module.parsetree.EvaluatedParseTree;
import net.foxopen.fox.module.parsetree.ParseTree;
import net.foxopen.fox.module.parsetree.evaluatedpresentationnode.EvaluatedHeadingPresentationNode;
import net.foxopen.fox.module.parsetree.evaluatedpresentationnode.EvaluatedPresentationNode;

/**
 * This Presentation Node stores attributes for fm:heading elements in a module presentation block
 */
public class HeadingPresentationNode extends PresentationNode {
  private final String mLevel;
  private final String mStyles;
  private final String mClasses;

  public HeadingPresentationNode(DOM pCurrentNode) {
    mLevel = pCurrentNode.getAttr("level");
    mStyles = pCurrentNode.getAttr("style");
    mClasses = pCurrentNode.getAttr("class");

    // Process children
    ParseTree.parseDOMChildren(this, pCurrentNode);
  }

  public String toString() {
    return "Header ("+mLevel+")";
  }

  @Override
  public EvaluatedHeadingPresentationNode evaluate(EvaluatedPresentationNode<? extends PresentationNode> pParent, EvaluatedParseTree pEvaluatedParseTree, DOM pEvalContext) {
    return new EvaluatedHeadingPresentationNode(pParent, this, pEvaluatedParseTree, pEvalContext);
  }

  public String getLevel() {
    return mLevel;
  }

  public String getStyles() {
    return mStyles;
  }

  public String getClasses() {
    return mClasses;
  }
}
