package net.foxopen.fox.thread.facet;

import net.foxopen.fox.ContextUElem;
import net.foxopen.fox.dom.DOM;
import net.foxopen.fox.ex.ExInternal;
import net.foxopen.fox.module.facet.ModuleFacetType;
import net.foxopen.fox.module.tabs.EvaluatedTabInfo;
import net.foxopen.fox.module.tabs.TabGroup;
import net.foxopen.fox.module.tabs.TabGroupProvider;
import net.foxopen.fox.module.tabs.TabInfoProvider;
import net.foxopen.fox.thread.persistence.PersistenceContext;
import net.foxopen.fox.thread.stack.ModuleCall;
import net.foxopen.fox.track.Track;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Implementation of a TabGroupProvider which can be serialised to the database.
 */
public class ModuleCallTabGroupProvider
extends ModuleCallFacetProvider<TabGroup>
implements TabGroupProvider {

  public static ModuleCallFacetProvider.Builder getBuilder() {
    return new Builder();
  }

  private static String getTabGroupKey(String pTabGroupName, DOM pTabGroupAttach) {
    return pTabGroupAttach.getFoxId() + "/" + pTabGroupName;
  }

  private ModuleCallTabGroupProvider(PersistenceContext pPersistenceContext, ModuleCall pModuleCall, Map<String, TabGroup> pFacetKeyToFacetMap) {
    super(pPersistenceContext, pModuleCall, pFacetKeyToFacetMap);
  }

  @Override
  public TabGroup getTabGroupByKey(String pTabGroupKey) {
    TabGroup lTabGroup = getFacetByKey(pTabGroupKey);
    if(lTabGroup == null) {
      throw new ExInternal("No tab group found for key " + pTabGroupKey);
    }

    return lTabGroup;
  }

  @Override
  public TabGroup getOrCreateEmptyTabGroup(String pTabGroupName, DOM pTabGroupAttach) {
    return getOrCreateTabGroupInternal(pTabGroupName, pTabGroupAttach, Collections.<TabInfoProvider>emptyList(), null);
  }

  @Override
  public TabGroup getOrCreateTabGroup(String pTabGroupName, DOM pTabGroupAttach, List<TabInfoProvider> pTabInfoProviderList, ContextUElem pContextUElem) {
    return getOrCreateTabGroupInternal(pTabGroupName, pTabGroupAttach, pTabInfoProviderList, pContextUElem);
  }

  /**
   * Gets/creates a tab group and refreshes its tab info list if a TabInfoProviderList and ContextUElem are provided.
   * @param pTabGroupName Tab group name.
   * @param pTabGroupAttach Tab group attach DOM.
   * @param pTabInfoProviderList Cannot be null but can be 0-length.
   * @param pContextUElem Can be null if pTabInfoProviderList is 0-length.
   * @return New/existing tab group, refreshed if possible.
   */
  private TabGroup getOrCreateTabGroupInternal(String pTabGroupName, DOM pTabGroupAttach, List<TabInfoProvider> pTabInfoProviderList, ContextUElem pContextUElem) {
    String lTabGroupKey = getTabGroupKey(pTabGroupName, pTabGroupAttach);
    TabGroup lTabGroup = getFacetByKey(lTabGroupKey);

    boolean lIsNewTabGroup = false;
    if(lTabGroup == null) {
      lIsNewTabGroup = true;
      lTabGroup = new ModuleCallTabGroup(lTabGroupKey, getModuleCall().getCallId());

      Track.info("CreateNewTabGroup", "Creating new TabGroup for key " + lTabGroupKey + " (" + pTabInfoProviderList.size() + " info providers)");

      //Mark as requiring an insert
      registerNewFacet(lTabGroup);
    }

    //Belt and braces to check this method's been called correctly - we only need a ContextUElem if we're constructing tab info objects at this point
    if(pTabInfoProviderList.size() > 0 && pContextUElem == null) {
      throw new ExInternal("ContextUElem cannot be null if tab info providers are available");
    }

    //Refresh the tab info list every time in case DOM has changed etc
    List<EvaluatedTabInfo> lTabInfoList = new ArrayList<>();
    for(TabInfoProvider lTabInfoProvider : pTabInfoProviderList) {
      try {
        //Only evaluate "default" XPaths if this is a new group (it's a waste of time otherwise, we'll already have a selected tab)
        lTabInfoList.addAll(lTabInfoProvider.evaluate(pTabGroupAttach, pContextUElem, lIsNewTabGroup));
      }
      catch (Throwable th) {
        //Add some context to the error message
        throw new ExInternal("Failed to construct tab group " + pTabGroupName, th);
      }
    }

    //Check tab keys for duplicates
    Set<String> lTabKeyDuplicateCheck = new HashSet<>();
    for(EvaluatedTabInfo lTabInfo : lTabInfoList) {
      if(lTabKeyDuplicateCheck.contains(lTabInfo.getTabKey())) {
        throw new ExInternal("Tab group " + pTabGroupName + " contains tabs with duplicate key " + lTabInfo.getTabKey());
      }
      lTabKeyDuplicateCheck.add(lTabInfo.getTabKey());
    }

    //Force the refresh (note this may re-order the info list)
    lTabGroup.refreshTabInfoList(lTabInfoList);

    if(lIsNewTabGroup && lTabInfoList.size() > 0) {
      //This is a new TabGroup - determine the default selection - use the list in the TabGroup as it is correctly ordered
      lTabGroup.selectTab(getPersistenceContext(), getDefaultTabKey(lTabGroup));
    }

    return lTabGroup;
  }

  private static String getDefaultTabKey(TabGroup pTabGroup) {

    List<EvaluatedTabInfo> lTabInfoList = pTabGroup.getTabInfoList();

    //Loop through the tabs and return the first one marked as default
    for(EvaluatedTabInfo lEvaluatedTabInfo : lTabInfoList) {
      if(lEvaluatedTabInfo.isDefault()) {
        return lEvaluatedTabInfo.getTabKey();
      }
    }

    //Nothing marked as default in the list; return the first tab
    return lTabInfoList.get(0).getTabKey();
  }

  private static class Builder extends ModuleCallFacetProvider.Builder<TabGroupProvider> {

    private Builder() {
      super(TabGroupProvider.class);
    }

    @Override
    public TabGroupProvider createNewProvider(PersistenceContext pPersistenceContext, ModuleCall pModuleCall) {
      return new ModuleCallTabGroupProvider(pPersistenceContext, pModuleCall, new HashMap<String, TabGroup>());
    }

    @Override
    public TabGroupProvider deserialiseExistingProvider(PersistenceContext pPersistenceContext, ModuleCall pModuleCall) {
      return new ModuleCallTabGroupProvider(pPersistenceContext, pModuleCall, deserialiseFacets(pPersistenceContext, pModuleCall, ModuleFacetType.TAB_GROUP, TabGroup.class));
    }
  }
}
