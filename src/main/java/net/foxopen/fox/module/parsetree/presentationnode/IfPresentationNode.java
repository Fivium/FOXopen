package net.foxopen.fox.module.parsetree.presentationnode;

import java.util.ArrayList;
import java.util.List;

import net.foxopen.fox.dom.DOM;
import net.foxopen.fox.dom.DOMList;
import net.foxopen.fox.ex.ExActionFailed;
import net.foxopen.fox.ex.ExInternal;
import net.foxopen.fox.module.parsetree.EvaluatedParseTree;
import net.foxopen.fox.module.parsetree.evaluatedpresentationnode.EvaluatedPresentationNode;


/**
 * This is a Presentation Node for fm:if blocks from a presentation block in a module. It splits out fm:then, fm:else-if
 * and fm:else sub-elements into pre-parsed PresentationNodes
 */
public class IfPresentationNode extends PresentationNode {
  private final String mTestXPath;

  private ContainerPresentationNode mThenNode;
  private ContainerPresentationNode mElseNode;
  private final List<ElseIfContainerPresentationNode> mElseIfNodes = new ArrayList<>();

  /**
   * Parse the current DOM node, assuming it's an fm:if
   * This Presentation Node parses its own children locally, splitting out fm:then, fm:else-if and fm:else elements into
   * their own Presentation Nodes to be used at evaluate time
   *
   * @param pCurrentNode Current DOM Node for an fm:if
   */
  public IfPresentationNode(DOM pCurrentNode) {
    mTestXPath = pCurrentNode.getAttr("test");

    // Parse children locally
    DOMList lDOMChildNodeList = pCurrentNode.getChildNodes();
    int lDOMChildCount = lDOMChildNodeList.getLength();
    for (int i = 0; i < lDOMChildCount; i++) {
      // Process Child Node
      DOM lIfChild = lDOMChildNodeList.item(i);
      String lNodeName = lIfChild.getName();

      if ("fm:then".equals(lNodeName)) {
        if (mThenNode != null) {
          throw new ExInternal("More than one Then node under an fm:if");
        }
        mThenNode = new ContainerPresentationNode(lIfChild);
        mThenNode.setDebugInfo("ThenNode: fm:if@test=\"" + mTestXPath + "\"");
      }
      else if ("fm:else-if".equals(lNodeName)) {
        ElseIfContainerPresentationNode lElseIfNode = new ElseIfContainerPresentationNode(lIfChild);
        lElseIfNode.setDebugInfo("ElseIfNode: fm:else-if@test=\"" + lElseIfNode.getTestXPath() + "\"");
        mElseIfNodes.add(lElseIfNode);
      }
      else if ("fm:else".equals(lNodeName)) {
        if (mElseNode != null) {
          throw new ExInternal("More than one Else node under an fm:if");
        }
        mElseNode = new ContainerPresentationNode(lIfChild);
        mElseNode.setDebugInfo("ElseNode: fm:if@test=\"" + mTestXPath + "\"");
      }
    }

    if (mThenNode == null) {
      throw new ExInternal("An fm:if should have at least one Then node inside it");
    }
  }

  public String toString() {
    return "IfNode";
  }

  /**
   * Evaluate the conditions on the fm:if against the current context and return a container of the correct sub-section
   *
   * @inheritDoc
   */
  public EvaluatedPresentationNode<? extends PresentationNode> evaluate(EvaluatedPresentationNode<? extends PresentationNode> pParent, EvaluatedParseTree pEvaluatedParseTree, DOM pEvalContext) {
    String lTestXPath = mTestXPath;

    if (lTestXPath == null) {
      throw new ExInternal("Error evaluating fm:if command - No test attribute specified");
    }

    try {
      if (pEvaluatedParseTree.getContextUElem().extendedXPathBoolean(pEvalContext, lTestXPath)) {
        // Evaluate the node into a container node, then take those nodes and reposition them in the tree in place of this if node
        return mThenNode.evaluate(pParent, pEvaluatedParseTree, pEvalContext);
      }
      else {
        // Evaluate else-if's
        ELSE_IF_LOOP: for (ElseIfContainerPresentationNode lElseIfPresentationNode : mElseIfNodes) {
          lTestXPath = lElseIfPresentationNode.getTestXPath();
          if (lTestXPath == null) {
            throw new ExInternal("Error evaluating else-if command - No test attribute specified");
          }
          if (pEvaluatedParseTree.getContextUElem().extendedXPathBoolean(pEvalContext, lTestXPath)) {
            return lElseIfPresentationNode.evaluate(pParent, pEvaluatedParseTree, pEvalContext);
          }
        }

        // If no else-if's matched, go for the else (provided there is one)
        if (mElseNode != null) {
          return mElseNode.evaluate(pParent, pEvaluatedParseTree, pEvalContext);
        }
      }
    }
    catch (ExActionFailed ex) {
      throw ex.toUnexpected("Failed evaluating the test attribute on fm:if: " + lTestXPath);
    }

    return null;
  }

  static private class ElseIfContainerPresentationNode extends ContainerPresentationNode {
    private final String mTestXPath;

    public ElseIfContainerPresentationNode(DOM pCurrentNode) {
      super(pCurrentNode);
      mTestXPath = pCurrentNode.getAttr("test");
    }

    public String getTestXPath() {
      return mTestXPath;
    }
  }
}
