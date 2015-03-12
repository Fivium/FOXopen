package net.foxopen.fox.module.tabs;

import net.foxopen.fox.dom.DOM;
import net.foxopen.fox.module.facet.ModuleFacet;
import net.foxopen.fox.thread.persistence.PersistenceContext;

import java.util.List;

/**
 * A tab group stores a temporary list of EvaluatedTabInfo objects (in the order they should be displayed), and the tab key
 * of the currently selected tab, which should be serialised. The list of EvaluatedTabInfos needs to be refreshed on every
 * page churn due to the dynamic nature of tab XPaths, so it should not be serialised. This means TabGroups cannot enforce
 * full validation, i.e. a key can be selected which does not exist, because the TabGroup is not guaranteed to know what
 * tabs it actually contains.<br/><br/>
 *
 * Tabs may be DOM-based if they are created from an XPath loop. All tabs are addressable by a string key. The TabGroup must
 * provide a way of seemlessly mapping a DOM-based tab to a string key - typically this can be achieved using the DOM's FOXID.<br/><br/>
 *
 * A TabGroup should be uniquely identifiable within a module call by its tab group key. Typically this will be a concatenation
 * of the TabGroup's user-defined name and its attach point.
 */
public interface TabGroup
extends ModuleFacet {

  public String getTabGroupKey();

  public String getModuleCallId();

  /**
   * Tests if the given TabInfo is currently the selected tab in this TabGroup.
   * @param pTabInfo
   * @return
   */
  public boolean isTabSelected(EvaluatedTabInfo pTabInfo);

  /**
   * Gets the currently selected tab key for this TabGroup.
   * @return Currently selected tab key, or empty string if no tab selected.
   */
  public String getSelectedTabKey();

  public void refreshTabInfoList(List<EvaluatedTabInfo> pTabInfoList);

  /**
   * Selects a tab by its string-based key.
   * @param pPersistenceContext For serialising the TabGroup.
   * @param pTabKey
   */
  public void selectTab(PersistenceContext pPersistenceContext, String pTabKey);

  /**
   * Selects a DOM-based tab.
   * @param pPersistenceContext For serialising the TabGroup.
   * @param pTabDOM The DOM represented by the tab (i.e. the :{tab} context).
   */
  public void selectTab(PersistenceContext pPersistenceContext, DOM pTabDOM);

  /**
   * Gets the list of Tabs contained by this TabGroup. This may be empty if the TabGroup does not know what its tabs
   * are yet (the list is refreshed during HTML generation, but the TabGroup may have been created before that). The
   * list is ordered according to the tab displayOrders.
   * @return
   */
  public List<EvaluatedTabInfo> getTabInfoList();

}
