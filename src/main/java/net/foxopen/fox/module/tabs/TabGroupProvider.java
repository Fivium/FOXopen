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
   * @return Existing tab group of the given key.
   */
  TabGroup getTabGroupByKey(String pTabGroupKey);

  /**
   * Gets a tab group or creates it if it doesn't exist. The new TabGroup will be stored in this object for later retrieval.
   * This method does not attempt to refresh the group's tab info list and should only be invoked when no tab info provider
   * (i.e. buffer markup) is available. It is assumed that {@link #getOrCreateTabGroup} will be invoked at a later stage to
   * perform this refresh behaviour.
   * @param pTabGroupName User defined TabGroup name.
   * @param pTabGroupAttach TabGroup attach point.
   * @return Existing or new tab group.
   */
  TabGroup getOrCreateEmptyTabGroup(String pTabGroupName, DOM pTabGroupAttach);

  /**
   * Gets a tab group or creates it if it doesn't exist. The new TabGroup will be stored in this object for later retrieval.
   * New tab groups are initialised so the first tab is selected if possible. For existing tab groups, the group's state is
   * refreshed to reflect DOM changes etc. This method also validates that all the tab keys are unique, so an exception will
   * be thrown if the tab info provider markup results in an invalid tab group.
   * @param pTabGroupName User defined TabGroup name.
   * @param pTabGroupAttach TabGroup attach point.
   * @param pTabInfoProviderList This is evaluated in order to create the EvaluatedTabInfo list for the tab group.
   * @param pContextUElem For evaluating XPaths in the TabInfoProvider List.
   * @return Existing or new tab group.
   */
  TabGroup getOrCreateTabGroup(String pTabGroupName, DOM pTabGroupAttach, List<TabInfoProvider> pTabInfoProviderList, ContextUElem pContextUElem);

}
