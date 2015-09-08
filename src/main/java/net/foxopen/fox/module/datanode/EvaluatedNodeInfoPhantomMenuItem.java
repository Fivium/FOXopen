package net.foxopen.fox.module.datanode;

import net.foxopen.fox.dom.DOM;
import net.foxopen.fox.ex.ExActionFailed;
import net.foxopen.fox.module.ActionDefinition;
import net.foxopen.fox.module.DisplayOrder;
import net.foxopen.fox.module.MenuOutActionProvider;
import net.foxopen.fox.module.evaluatedattributeresult.DOMAttributeResult;
import net.foxopen.fox.module.parsetree.evaluatedpresentationnode.GenericAttributesEvaluatedPresentationNode;
import net.foxopen.fox.module.parsetree.presentationnode.GenericAttributesPresentationNode;
import net.foxopen.fox.module.serialiser.components.html.MenuOutWidgetHelper;
import net.foxopen.fox.module.serialiser.widgets.WidgetBuilderType;
import net.foxopen.fox.module.serialiser.widgets.WidgetType;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

// TODO - This duplicates a lot of code from the EvaluatedMenuOutPresentationNode class and should share the logic
public class EvaluatedNodeInfoPhantomMenuItem
extends EvaluatedNodeInfoItem
  implements MenuOutActionProvider {
  private final List<EvaluatedNodeAction> mEvaluatedNodeActionList = new ArrayList<>();

  EvaluatedNodeInfoPhantomMenuItem(EvaluatedNode pParent, GenericAttributesEvaluatedPresentationNode<? extends GenericAttributesPresentationNode> pEvaluatedPresentationNode, NodeEvaluationContext pNodeEvaluationContext, NodeVisibility pNodeVisibility, NodeInfo pNodeInfo) {
    super(pParent, pEvaluatedPresentationNode, pNodeEvaluationContext, pNodeVisibility, pNodeInfo);

    DOM lActionContextDOM = getEvaluateContextRuleItem();
    DOMAttributeResult lActionContextAttribute = getDOMAttributeOrNull(NodeAttribute.ACTION_CONTEXT_DOM);
    if (lActionContextAttribute != null && lActionContextAttribute.getDOM() != null) {
      lActionContextDOM = lActionContextAttribute.getDOM();
    }

    // Add all state level actions
    Map<String, ActionDefinition> lStateActions = getEvaluatedParseTree().getState().getActionDefinitionMap();
    List<ActionDefinition> lActionList = new ArrayList<>();
    lActionList.addAll(lStateActions.values());

    // Add all module level actions
    Map<String, ActionDefinition> lModuleActions = getEvaluatedParseTree().getModule().getActionDefinitionMap();
    for (ActionDefinition lModuleAction : lModuleActions.values()) {
      if (lStateActions.containsKey(lModuleAction.getActionName())) {
        // If there is already an action with the same name at state level, don't override it with the module level one
        continue;
      }
      lActionList.add(lModuleAction);
    }

    // Work out Mode List using regular mode list cut down to only enabled phantom menu mode attribute enabled modes
    List<String> lPhantomMenuModeList = new ArrayList<>();
    for (String lMode : getNodeEvaluationContext().getModeList()) {
      String lNodeModeAttribute = getNodeEvaluationContext().getNodeAttribute(lMode, NodeAttribute.PHANTOM_MENU_MODE.getExternalString());
      if (lNodeModeAttribute != null) {
        try {
          if (getContextUElem().extendedXPathBoolean(getEvaluateContextRuleItem(), lNodeModeAttribute)) {
            lPhantomMenuModeList.add(lMode);
          }
        }
        catch (ExActionFailed e) {
          throw e.toUnexpected("XPath failed while evaluating a " + NodeAttribute.PHANTOM_MENU_MODE.getExternalString() + " attribute on: " + getIdentityInformation());
        }
      }
    }

    // Get EvaluatedNodeAction items for everything in the module matching the mode rules
    for (ActionDefinition lAction : lActionList) {
      MODE_LOOP: for (String lMode : lPhantomMenuModeList) {
        if (lAction.getNamespaceAttributeTable().containsNamespace(lMode)) {
          NodeEvaluationContext lActionNodeEvaluationContext = NodeEvaluationContext.createNodeInfoEvaluationContext(getEvaluatedParseTree(), getEvaluatedPresentationNode(), getEvaluateContextRuleItem(), getEvaluateContextRuleItem(), lActionContextDOM, lAction.getNamespaceAttributeTable(), null);
          EvaluatedNodeAction lEvaluatedAction = EvaluatedNodeFactory.createEvaluatedNodeAction(getEvaluatedPresentationNode(), lActionNodeEvaluationContext, lAction.createActionIdentifier(false));
          if (lEvaluatedAction != null && (lEvaluatedAction.isRunnable() || lEvaluatedAction.getVisibility() != NodeVisibility.DENIED)) {
            mEvaluatedNodeActionList.add(lEvaluatedAction);
            break MODE_LOOP;
          }
        }
      }
    }

    DisplayOrder.sort(mEvaluatedNodeActionList);
  }

  @Override
  protected WidgetType getWidgetType() {
    return WidgetType.fromBuilderType(WidgetBuilderType.PHANTOM_MENU);
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
    return getStringAttribute(NodeAttribute.FLOW, MenuOutWidgetHelper.MENU_FLOW_ACROSS);
  }

  /**
   * Get list of classes to apply to the menu out container
   *
   * @return
   */
  @Override
  public List<String> getClasses() {
    return getStringAttributes(NodeAttribute.MENU_CLASS, NodeAttribute.CLASS);
  }

  /**
   * Get list of styles to apply to the menu out container
   *
   * @return
   */
  @Override
  public List<String> getStyles() {
    return getStringAttributes(NodeAttribute.MENU_STYLE, NodeAttribute.STYLE);
  }
}
