package net.foxopen.fox.module.parsetree.presentationnode;

import net.foxopen.fox.dom.DOM;
import net.foxopen.fox.module.parsetree.EvaluatedParseTree;
import net.foxopen.fox.module.parsetree.ParseTree;
import net.foxopen.fox.module.parsetree.evaluatedpresentationnode.EvaluatedHeaderFooterPresentationNode;
import net.foxopen.fox.module.parsetree.evaluatedpresentationnode.EvaluatedPresentationNode;

/**
 * This Presentation Node stores attributes for fm:header or fm:footer in a buffer
 */
public abstract class HeaderFooterPresentationNode extends PresentationNode {
  private final String mStyles;
  private final String mClasses;

  protected HeaderFooterPresentationNode(DOM pCurrentNode) {
    mStyles = pCurrentNode.getAttr("style");
    mClasses = pCurrentNode.getAttr("class");

    // Process children
    ParseTree.parseDOMChildren(this, pCurrentNode, false);
  }

  @Override
  public abstract EvaluatedHeaderFooterPresentationNode evaluate(EvaluatedPresentationNode<? extends PresentationNode> pParent,
                                                                 EvaluatedParseTree pEvaluatedParseTree, DOM pEvalContext);

  public String getStyles() {
    return mStyles;
  }

  public String getClasses() {
    return mClasses;
  }
}
