package net.foxopen.fox.module.parsetree.presentationnode;

import net.foxopen.fox.XFUtil;
import net.foxopen.fox.dom.DOM;
import net.foxopen.fox.ex.ExActionFailed;
import net.foxopen.fox.ex.ExInternal;
import net.foxopen.fox.module.parsetree.EvaluatedParseTree;
import net.foxopen.fox.module.parsetree.evaluatedpresentationnode.EvaluatedContainerPresentationNode;
import net.foxopen.fox.module.parsetree.evaluatedpresentationnode.EvaluatedPresentationNode;

import java.util.ArrayList;
import java.util.List;


/**
 * This is a Presentation Node for fm:case blocks from a presentation block in a module
 */
public class CasePresentationNode extends PresentationNode {
  private final List<WhenContainerPresentationNode> mWhenNodes = new ArrayList<>();
  private ContainerPresentationNode mDefaultNode;
  private CaseContainerPresentationNode mCaseContainer;
  private final String mCaseValue;
  private final String mCaseXPath;

  /**
   * Parse the current DOM node, assuming it's an fm:case
   *
   * @param pCurrentNode Current DOM Node for an fm:case
   */
  public CasePresentationNode(DOM pCurrentNode) {
    mCaseValue = pCurrentNode.getAttr("value");
    mCaseXPath = pCurrentNode.getAttr("expr");

    // Create a general container that's from just this case node, but emptied, so it can be filled with when/default containers later
    mCaseContainer = new CaseContainerPresentationNode(pCurrentNode.clone(false));

    // Parse children locally
    for (DOM lCaseChildNode : pCurrentNode.getChildNodes()) {
      // Process Child Node
      String lNodeName = lCaseChildNode.getName();

      if ("fm:when".equals(lNodeName)) {
        WhenContainerPresentationNode lWhenNode = new WhenContainerPresentationNode(lCaseChildNode);
        lWhenNode.setDebugInfo("WhenNode: fm:when@test=\"" + XFUtil.nvl(lWhenNode.getValue(), lWhenNode.getXPath()) + "\"");
        mWhenNodes.add(lWhenNode);
      }
      else if ("fm:default".equals(lNodeName)) {
        if (mDefaultNode != null) {
          throw new ExInternal("More than one Default node under an fm:case");
        }
        mDefaultNode = new ContainerPresentationNode(lCaseChildNode);
        mDefaultNode.setDebugInfo("DefaultNode");
      }
    }

    if (mDefaultNode == null && mWhenNodes.size() == 0) {
      throw new ExInternal("An fm:case should have at least one default or when node inside it");
    }

    mCaseContainer.setDebugInfo(this.toString());
  }

  public String toString() {
    return "CaseNode: @value=\"" + mCaseValue + "\" @expr=\"" + mCaseXPath + "\"";
  }

  /**
   * Evaluate when nodes or default under the case block
   *
   * @inheritDoc
   */
  @Override
  public EvaluatedPresentationNode<? extends PresentationNode> evaluate(EvaluatedPresentationNode<? extends PresentationNode> pParent, EvaluatedParseTree pEvaluatedParseTree, DOM pEvalContext) {
    // TODO - NP - Try and find a way to refactor this out so that the regular command can use the same logic. Perhaps for 5.1?
    EvaluatedPresentationNode<? extends PresentationNode> lEvaluatedContainerNode = pEvaluatedParseTree.evaluateNode(pParent, mCaseContainer, pEvalContext);
    try {
      String lPivot = !XFUtil.isNull(mCaseXPath) ? pEvaluatedParseTree.getContextUElem().extendedXPathString(pEvalContext, mCaseXPath) : mCaseValue;
      if (XFUtil.isNull(lPivot)) {
        throw new ExInternal("Error evaluating fm:case command - No valid value/expr attribute specified");
      }
      MATCH_BLOCK: {
        WHEN_LOOP: for (WhenContainerPresentationNode lWhenNodeContainer : mWhenNodes) {
          String lWhenNodeCondition = !XFUtil.isNull(lWhenNodeContainer.getXPath()) ? pEvaluatedParseTree.getContextUElem().extendedXPathString(pEvalContext, lWhenNodeContainer.getXPath()) : lWhenNodeContainer.getValue();
          if (lPivot.equals(lWhenNodeCondition)) {
            lEvaluatedContainerNode.addChild(pEvaluatedParseTree.evaluateNode(pParent, lWhenNodeContainer, pEvalContext));
            if(lWhenNodeContainer.isContinueMatching()){
              continue WHEN_LOOP;
            }
            break MATCH_BLOCK;
          }
        }

        if(!XFUtil.isNull(mDefaultNode)){
          lEvaluatedContainerNode.addChild(pEvaluatedParseTree.evaluateNode(pParent, mDefaultNode, pEvalContext));
        }
      }
    }
    catch (ExActionFailed ex) {
      throw ex.toUnexpected("Failed evaluating the attributes on fm:case");
    }

    return lEvaluatedContainerNode;
  }

  static private class WhenContainerPresentationNode extends ContainerPresentationNode {
    private final String mValue;
    private final String mXPath;
    private final boolean mContinueMatching;

    public WhenContainerPresentationNode(DOM pCurrentNode) {
      super(pCurrentNode);

      mValue = pCurrentNode.getAttr("value");
      mXPath = pCurrentNode.getAttr("expr");
      mContinueMatching = "true".equals(pCurrentNode.getAttr("continue-matching"));
    }

    public String getValue() {
      return mValue;
    }

    public String getXPath() {
      return mXPath;
    }

    public boolean isContinueMatching() {
      return mContinueMatching;
    }
  }

  /**
   * Extension of ContainerPresentationNode to make it evaluate to a CaseEvaluatedContainerPresentationNode
   */
  static private class CaseContainerPresentationNode extends ContainerPresentationNode {
    public CaseContainerPresentationNode(DOM pSourceDOM) {
      super(pSourceDOM);
    }

    @Override
    public EvaluatedContainerPresentationNode evaluate(EvaluatedPresentationNode<? extends PresentationNode> pParent, EvaluatedParseTree pEvaluatedParseTree, DOM pEvalContext) {
      return new CaseEvaluatedContainerPresentationNode(pParent, this, pEvaluatedParseTree, pEvalContext);
    }
  }

  /**
   * Extension of EvaluatedContainerPresentationNode to set canEvaluateChildren == false so that we can add one or more
   * when container nodes to this manually without the parsetree re-evaluating them
   */
  static private class CaseEvaluatedContainerPresentationNode extends EvaluatedContainerPresentationNode {
    public CaseEvaluatedContainerPresentationNode(EvaluatedPresentationNode<? extends PresentationNode> pParent,
                                                     ContainerPresentationNode pOriginalPresentationNode,
                                                     EvaluatedParseTree pEvaluatedParseTree, DOM pEvalContext) {
      super(pParent, pOriginalPresentationNode, pEvaluatedParseTree, pEvalContext);
    }

    @Override
    public boolean canEvaluateChildren() {
      return false;
    }
  }
}
