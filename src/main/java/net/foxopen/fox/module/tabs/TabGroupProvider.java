package net.foxopen.fox.module.tabs;

import net.foxopen.fox.ContextUElem;
import net.foxopen.fox.dom.DOM;
import net.foxopen.fox.module.facet.ModuleFacetProvider;

import java.util.List;

/**
 * Implementors of this interface are capable of creating new TabGroups and holding a list of currently active TabGroups.
 */
public interface TabGroupProvider
extends ModuleFacetProvider {

  /**
   * Gets a TabGroup by its key. An error is thrown if tab group does not exist. Use this method when you are expecting
   * the TabGroup to exist and do not wish to cause a new one to be created.
   * @param pTabGroupKey
   * @return
   */
  public TabGroup getTabGroupByKey(String pTabGroupKey);

  /**
   * Gets a tab group. An error is thrown if tab group does not exist. Use this method when you are expecting the TabGroup
   * to exist and do not wish to cause a new one to be created, and if you do not know the TabGroup key.
   * @param pTabGroupName User defined TabGroup name.
   * @param pTabGroupAttach TabGroup attach point.
   * @return
   */
  public TabGroup getTabGroup(String pTabGroupName, DOM pTabGroupAttach);

  /**
   * Gets a tab group or creates it if it doesn't exist. A new TabGroup will be stored in this object for later retrieval.
   * New tab groups are initialised so the first tab is selected if possible. Creating a new TabGroup also validates that
   * all the tab keys are unique, so this method may not succeed if the markup is invalid.
   * @param pTabGroupName User defined TabGroup name.
   * @param pTabGroupAttach TabGroup attach point.
   * @param pTabInfoProviderList This is evaluated in order to create the EvaluatedTabInfo list for the tab group.
   * @param pContextUElem For evaluating XPaths in the TabInfoProvider List.
   * @return
   */
  public TabGroup getOrCreateTabGroup(String pTabGroupName, DOM pTabGroupAttach, List<TabInfoProvider> pTabInfoProviderList, ContextUElem pContextUElem);

}
