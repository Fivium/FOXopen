package net.foxopen.fox.module.parsetree.evaluatedpresentationnode;


import net.foxopen.fox.XFUtil;
import net.foxopen.fox.dom.DOM;
import net.foxopen.fox.ex.ExActionFailed;
import net.foxopen.fox.ex.ExCardinality;
import net.foxopen.fox.ex.ExInternal;
import net.foxopen.fox.module.ActionDefinition;
import net.foxopen.fox.module.datanode.EvaluatedNodeAction;
import net.foxopen.fox.module.datanode.EvaluatedNodeFactory;
import net.foxopen.fox.module.datanode.NodeEvaluationContext;
import net.foxopen.fox.module.parsetree.EvaluatedParseTree;
import net.foxopen.fox.module.parsetree.presentationnode.ActionOutPresentationNode;
import net.foxopen.fox.module.parsetree.presentationnode.PresentationNode;
import net.foxopen.fox.module.serialiser.components.ComponentBuilderType;


public class EvaluatedActionOutPresentationNode
extends GenericAttributesEvaluatedPresentationNode<ActionOutPresentationNode> {
  private EvaluatedNodeAction mEvaluatedNodeAction;

  public EvaluatedActionOutPresentationNode(EvaluatedPresentationNode<? extends PresentationNode> pParent,
                                            ActionOutPresentationNode pOriginalPresentationNode, EvaluatedParseTree pEvaluatedParseTree, DOM pEvalContext) {
    super(pParent, pOriginalPresentationNode, pEvalContext);

    String lActionName = pOriginalPresentationNode.getActionName();
    ActionDefinition lAction = pEvaluatedParseTree.getState().getActionByName(lActionName);
    if (lAction == null) {
      throw new ExInternal("The action-out has an invalid action [" + lActionName + "] in the state [" + pEvaluatedParseTree.getState().getName() + "]\n " +
                             "The action can not be found in the state or gobal action-list\n\n");
    }

    DOM lActionContextDOM = pEvalContext;
    String lActionContextXPath = pOriginalPresentationNode.getActionContextXPath();
    if (XFUtil.exists(lActionContextXPath)) {
      try {
        lActionContextDOM = pEvaluatedParseTree.getContextUElem().extendedXPath1E(getEvalContext(), lActionContextXPath, false);
      }
      catch (ExCardinality | ExActionFailed e) {
        throw new ExInternal("Invalid action-context XPath in action-out: " + lActionContextXPath);
      }
    }

    // Get node
    NodeEvaluationContext lNodeEvaluationContext = NodeEvaluationContext.createNodeInfoEvaluationContext(pEvaluatedParseTree, this, pEvalContext, pEvalContext,
                                                                                                         lActionContextDOM, lAction.getNamespaceAttributeTable(), null);
    //Include the state name in the ActionIdentifier if the action-out is targeting an action in a different state
    mEvaluatedNodeAction = EvaluatedNodeFactory.createEvaluatedNodeAction(this, lNodeEvaluationContext, lAction.createActionIdentifier(lActionName.contains("/")));
  }

  @Override
  public String toString() {
    return getOriginalNode().toString();
  }

  public EvaluatedNodeAction getEvaluatedNodeAction() {
    return mEvaluatedNodeAction;
  }

  @Override
  public ComponentBuilderType getPageComponentType() {
    return ComponentBuilderType.ACTION_OUT;
  }
}
