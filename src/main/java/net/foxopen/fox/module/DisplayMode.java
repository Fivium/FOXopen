package net.foxopen.fox.module;

import net.foxopen.fox.XFUtil;
import net.foxopen.fox.module.datanode.EvaluatedNode;
import net.foxopen.fox.module.datanode.NodeAttribute;
import net.foxopen.fox.module.datanode.NodeVisibility;

public enum DisplayMode {
  NEVER,
  RO,
  EDIT,
  ALWAYS;

  /**
   * Test an Evaluated Node to see if the visibility mode matches the modeDisplay NodeAttribute given.
   * If the NodeAttribute given wasn't specified then it will default to "both" to reflect the behaviour already in use.
   *
   * @param pEvaluatedNode EvaluatedNode to test the visibility of
   * @param pModeDisplayNodeAttribute NodeAttribute that should have a value matching this enum
   * @return true if the DisplayMode-related NodeAttributes given visibility level matches the visibility level of the given EvaluatedNode
   */
  public static boolean isDisplayAllowed(EvaluatedNode pEvaluatedNode, NodeAttribute pModeDisplayNodeAttribute) {
    return isDisplayAllowed(pEvaluatedNode, pModeDisplayNodeAttribute, ALWAYS);
  }


  public static boolean isDisplayAllowed(EvaluatedNode pEvaluatedNode, NodeAttribute pModeDisplayNodeAttribute, DisplayMode pDefaultDisplayMode) {
    String lModeDisplayAttribute = pEvaluatedNode.getStringAttribute(pModeDisplayNodeAttribute);

    // Map the NodeAttribute value to a value from this enum
    DisplayMode lDisplayMode;
    if (!XFUtil.isNull(lModeDisplayAttribute)) {
      lDisplayMode = DisplayMode.valueOf(lModeDisplayAttribute.toUpperCase());
    }
    else {
      lDisplayMode = pDefaultDisplayMode;
    }

    if (pEvaluatedNode.getVisibility() == NodeVisibility.EDIT && (lDisplayMode == EDIT || lDisplayMode == ALWAYS)) {
      return true;
    }
    else if (pEvaluatedNode.getVisibility() == NodeVisibility.VIEW && (lDisplayMode == RO || lDisplayMode == ALWAYS)) {
      return true;
    }

    return false;
  }
}
