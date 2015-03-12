package net.foxopen.fox.module;

import net.foxopen.fox.module.datanode.EvaluatedNodeAction;

import java.util.List;

public interface MenuOutActionProvider {
  /**
   * Get a list of EvaluatedNodeActions to provide to a menu out command
   *
   * @return List of actions to set out in a menu
   */
  public List<EvaluatedNodeAction> getActionList();

  /**
   * Get the direction this menu out should flow
   *
   * @return across or down
   */
  public String getFlow();

  /**
   * Get list of classes to apply to the menu out container
   *
   * @return
   */
  public List<String> getClasses();

  /**
   * Get list of styles to apply to the menu out container
   *
   * @return
   */
  public List<String> getStyles();
}
