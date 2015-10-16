package net.foxopen.fox.module.parsetree.presentationnode;

import net.foxopen.fox.dom.DOM;
import net.foxopen.fox.module.parsetree.EvaluatedParseTree;
import net.foxopen.fox.module.parsetree.ParseTree;
import net.foxopen.fox.module.parsetree.evaluatedpresentationnode.EvaluatedPageNumberPresentationNode;
import net.foxopen.fox.module.parsetree.evaluatedpresentationnode.EvaluatedPresentationNode;

/**
 * This Presentation Node stores attributes for fm:current-page-number or fm:last-page-number
 */
public abstract class PageNumberPresentationNode extends PresentationNode {
  private final String mStyles;
  private final String mClasses;

  protected PageNumberPresentationNode(DOM pCurrentNode) {
    mStyles = pCurrentNode.getAttr("style");
    mClasses = pCurrentNode.getAttr("class");

    // Process children
    ParseTree.parseDOMChildren(this, pCurrentNode, false);
  }

  @Override
  public abstract EvaluatedPageNumberPresentationNode evaluate(EvaluatedPresentationNode<? extends PresentationNode> pParent,
                                                               EvaluatedParseTree pEvaluatedParseTree, DOM pEvalContext);

  public String getStyles() {
    return mStyles;
  }

  public String getClasses() {
    return mClasses;
  }
}
