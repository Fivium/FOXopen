package net.foxopen.fox.module.parsetree.presentationnode;


import java.util.ArrayList;
import java.util.List;

import net.foxopen.fox.dom.DOM;
import net.foxopen.fox.module.parsetree.EvaluatedParseTree;
import net.foxopen.fox.module.parsetree.evaluatedpresentationnode.EvaluatedPresentationNode;


/**
 * Abstract class for all un-evaluated presentation nodes. Presentation Nodes are representations of module presentation
 * level elements in Java objects
 */
public abstract class PresentationNode {
  // List to hold all sub-nodes
  private final List<PresentationNode> mChildNodes = new ArrayList<PresentationNode>();

  /**
   * Creates a new PresentatioNode with no initial attributes.
   */
  protected PresentationNode() {
  }

  public void addChildNode(PresentationNode pChild) {
    mChildNodes.add(pChild);
  }

  public List<PresentationNode> getChildNodes() {
    return mChildNodes;
  }

  /**
   * A text representation of the metatdata attributed to each PresentationNode
   *
   * @return String representation of the current PresentationNode
   */
  public abstract String toString();

  /**
   * Construct an evaluated version of this presentation node
   *
   * @param pParent Evaluated Parent Node
   * @param pEvalParseTree Evaluated Parse Tree this node is being evaluated for
   * @param pEvalContext DOM context to use
   * @return Evaluated version of this presentation node
   */
  public abstract EvaluatedPresentationNode<? extends PresentationNode> evaluate(EvaluatedPresentationNode<? extends PresentationNode> pParent, EvaluatedParseTree pEvaluatedParseTree, DOM pEvalContext);
}
