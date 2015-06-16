package net.foxopen.fox.module.parsetree.evaluatedpresentationnode;

import net.foxopen.fox.XFUtil;
import net.foxopen.fox.dom.DOM;
import net.foxopen.fox.ex.ExActionFailed;
import net.foxopen.fox.ex.ExCardinality;
import net.foxopen.fox.ex.ExInternal;
import net.foxopen.fox.module.ActionDefinition;
import net.foxopen.fox.module.DisplayOrder;
import net.foxopen.fox.module.MenuOutActionProvider;
import net.foxopen.fox.module.datanode.EvaluatedNodeAction;
import net.foxopen.fox.module.datanode.EvaluatedNodeFactory;
import net.foxopen.fox.module.datanode.NodeAttribute;
import net.foxopen.fox.module.datanode.NodeEvaluationContext;
import net.foxopen.fox.module.datanode.NodeVisibility;
import net.foxopen.fox.module.evaluatedattributeresult.FixedStringAttributeResult;
import net.foxopen.fox.module.evaluatedattributeresult.StringAttributeResult;
import net.foxopen.fox.module.parsetree.EvaluatedParseTree;
import net.foxopen.fox.module.parsetree.presentationnode.MenuOutPresentationNode;
import net.foxopen.fox.module.parsetree.presentationnode.PresentationNode;
import net.foxopen.fox.module.serialiser.components.ComponentBuilderType;
import net.foxopen.fox.module.serialiser.components.html.MenuOutWidgetHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;


public class EvaluatedMenuOutPresentationNode
extends GenericAttributesEvaluatedPresentationNode<MenuOutPresentationNode>
implements MenuOutActionProvider {
  private final List<EvaluatedNodeAction> mEvaluatedNodeActionList = new ArrayList<>();
  private final StringAttributeResult mFlow;
  private final List<String> mClasses;
  private final List<String> mStyles;
  private final NodeEvaluationContext mNodeEvaluationContext;

  public EvaluatedMenuOutPresentationNode(EvaluatedPresentationNode<? extends PresentationNode> pParent, MenuOutPresentationNode pOriginalPresentationNode, EvaluatedParseTree pEvaluatedParseTree, DOM pEvalContext) {
    super(pParent, pOriginalPresentationNode, pEvalContext);

    // Add all state level actions
    Map<String, ActionDefinition> lStateActions = pEvaluatedParseTree.getState().getActionDefinitionMap();
    List<ActionDefinition> lActionList = new ArrayList<>();
    lActionList.addAll(lStateActions.values());

    // Add all module level actions
    Map<String, ActionDefinition> lModuleActions = pEvaluatedParseTree.getModule().getActionDefinitionMap();
    for (ActionDefinition lModuleAction : lModuleActions.values()) {
      if (lStateActions.containsKey(lModuleAction.getActionName())) {
        // If there is already an action with the same name at state level, don't override it with the module level one
        continue;
      }
      lActionList.add(lModuleAction);
    }

    // Set up the evaluation context for the menu-out
    mNodeEvaluationContext = NodeEvaluationContext.createPresentationNodeEvaluationContext(pEvaluatedParseTree, this, pEvalContext, pEvalContext);

    DOM lActionContextDOM = pEvalContext;
    String lActionContextXPath = pOriginalPresentationNode.getAttrOrNull(NodeAttribute.ACTION_CONTEXT_DOM.getExternalString());
    if (XFUtil.exists(lActionContextXPath)) {
      try {
        lActionContextDOM = pEvaluatedParseTree.getContextUElem().extendedXPath1E(getEvalContext(), lActionContextXPath, false);
      }
      catch (ExCardinality e) {
        throw new ExInternal("Invalid action-context XPath in menu-out: " + lActionContextXPath);
      }
      catch (ExActionFailed e) {
        throw new ExInternal("Invalid action-context XPath in menu-out: " + lActionContextXPath);
      }
    }

    // Get EvaluatedNodeAction items for everything in the module matching the mode rules
    for (ActionDefinition lAction : lActionList) {
      MODE_LOOP: for (String lMode : mNodeEvaluationContext.getModeList()) {
        if (lAction.getNamespaceAttributeTable().containsNamespace(lMode)) {
          NodeEvaluationContext lActionNodeEvaluationContext = NodeEvaluationContext.createNodeInfoEvaluationContext(pEvaluatedParseTree, this, pEvalContext, pEvalContext, lActionContextDOM, lAction.getNamespaceAttributeTable(), null, mNodeEvaluationContext);
          EvaluatedNodeAction lEvaluatedNodeAction = EvaluatedNodeFactory.createEvaluatedNodeAction(this, lActionNodeEvaluationContext, lAction);
          if (lEvaluatedNodeAction != null && (lEvaluatedNodeAction.isRunnable() || lEvaluatedNodeAction.getVisibility() != NodeVisibility.DENIED)) {
            mEvaluatedNodeActionList.add(lEvaluatedNodeAction);
            break MODE_LOOP;
          }
        }
      }
    }

    DisplayOrder.sort(mEvaluatedNodeActionList);

    mFlow = XFUtil.nvl(mNodeEvaluationContext.getStringAttributeOrNull(NodeAttribute.FLOW), new FixedStringAttributeResult(MenuOutWidgetHelper.MENU_FLOW_ACROSS));
    mClasses = mNodeEvaluationContext.getStringAttributes(NodeAttribute.MENU_CLASS, NodeAttribute.MENU_TABLE_CLASS);
    mStyles = mNodeEvaluationContext.getStringAttributes(NodeAttribute.MENU_STYLE, NodeAttribute.MENU_TABLE_STYLE);
  }

  @Override
  public String toString() {
    return getOriginalNode().toString();
  }

  @Override
  public ComponentBuilderType getPageComponentType() {
    return ComponentBuilderType.MENU_OUT;
  }

  /**
   * Get a list of EvaluatedNodeActions to provide to a menu out command
   *
   * @return List of actions to set out in a menu
   */
  @Override
  public List<EvaluatedNodeAction> getActionList() {
    return mEvaluatedNodeActionList;
  }

  /**
   * Get the direction this menu out should flow
   *
   * @return across or down
   */
  @Override
  public String getFlow() {
    if (mFlow == null) {
      return null;
    }
    return mFlow.getString();
  }

  /**
   * Get list of classes to apply to the menu out container
   *
   * @return
   */
  @Override
  public List<String> getClasses() {
    return mClasses;
  }

  /**
   * Get list of styles to apply to the menu out container
   *
   * @return
   */
  @Override
  public List<String> getStyles() {
    return mStyles;
  }
}
