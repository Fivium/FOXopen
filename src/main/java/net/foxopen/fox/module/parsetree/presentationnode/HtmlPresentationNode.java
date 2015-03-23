package net.foxopen.fox.module.parsetree.presentationnode;

import net.foxopen.fox.dom.DOM;
import net.foxopen.fox.module.parsetree.EvaluatedParseTree;
import net.foxopen.fox.module.parsetree.ParseTree;
import net.foxopen.fox.module.parsetree.evaluatedpresentationnode.EvaluatedHtmlPresentationNode;
import net.foxopen.fox.module.parsetree.evaluatedpresentationnode.EvaluatedPresentationNode;


/**
 * This Presentation Node stores the any non fm: prefixed elements, assumed to be HTML
 */
public class HtmlPresentationNode extends GenericAttributesPresentationNode {
  public static final String FORCE_SELF_CLOSE_TAG_NAME = "forceSelfCloseTag";

  private final String mTagName;
  private final boolean mForceSelfCloseTag;

  /**
   * Attributes are stored by default in PresentationNode so just construct this to store the tag name
   *
   * @param pCurrentNode HTML element
   */
  public HtmlPresentationNode(DOM pCurrentNode) {
    super(pCurrentNode);

    mTagName = pCurrentNode.getName();
    mForceSelfCloseTag = pCurrentNode.hasAttr(FORCE_SELF_CLOSE_TAG_NAME);

    // Process children
    ParseTree.parseDOMChildren(this, pCurrentNode, false);
  }

  /**
   * Get the name of the HTML tag
   * @return Tag name
   */
  public String getTagName() {
    return mTagName;
  }

  /**
   * Check to see if this tag is self-closing rather than the default of having a separate close tag
   * @return boolean
   */
  public boolean isForceSelfCloseTag() {
    return mForceSelfCloseTag;
  }

  public String toString() {
    return "Html ("+mTagName+")";
  }

  public EvaluatedHtmlPresentationNode evaluate(EvaluatedPresentationNode<? extends PresentationNode> pParent, EvaluatedParseTree pEvaluatedParseTree, DOM pEvalContext) {
    return new EvaluatedHtmlPresentationNode(pParent, this, pEvaluatedParseTree, pEvalContext);
  }
}
