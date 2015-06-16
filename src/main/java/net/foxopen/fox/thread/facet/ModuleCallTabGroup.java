package net.foxopen.fox.thread.facet;

import net.foxopen.fox.dom.DOM;
import net.foxopen.fox.module.DisplayOrder;
import net.foxopen.fox.module.facet.ModuleFacetType;
import net.foxopen.fox.module.tabs.EvaluatedTabInfo;
import net.foxopen.fox.module.tabs.TabGroup;
import net.foxopen.fox.thread.persistence.DeserialisationHandler;
import net.foxopen.fox.thread.persistence.PersistableType;
import net.foxopen.fox.thread.persistence.PersistenceContext;
import net.foxopen.fox.thread.persistence.PersistenceMethod;
import net.foxopen.fox.thread.persistence.PersistenceResult;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * TabGroup for a module call which can be serialised to the database.
 */
class ModuleCallTabGroup
implements TabGroup, Serializable, DeserialisationHandler {

  private final String mTabGroupKey;
  private final String mModuleCallId;
  private String mSelectedTabKey;

  //List refreshed every time this TabGroup is used in HTML gen
  private transient List<EvaluatedTabInfo> mTabInfoList = new ArrayList<>();

  ModuleCallTabGroup(String pTabGroupKey, String pModuleCallId) {
    mTabGroupKey = pTabGroupKey;
    mModuleCallId = pModuleCallId;
    mSelectedTabKey = "";
  }

  @Override
  public void handleDeserialisation() {
    //Restore this transient field after deserialise (Kryo doesn't do it)
    mTabInfoList = new ArrayList<>();
  }

  @Override
  public String getTabGroupKey() {
    return mTabGroupKey;
  }

  @Override
  public String getModuleCallId() {
    return mModuleCallId;
  }

  @Override
  public void selectTab(PersistenceContext pPersistenceContext, String pTabKey) {
    mSelectedTabKey = pTabKey;
    pPersistenceContext.requiresPersisting(this, PersistenceMethod.UPDATE);
  }

  @Override
  public void selectTab(PersistenceContext pPersistenceContext, DOM pTabDOM) {
    mSelectedTabKey = pTabDOM.getFoxId();
    pPersistenceContext.requiresPersisting(this, PersistenceMethod.UPDATE);
  }

  @Override
  public List<EvaluatedTabInfo> getTabInfoList() {
    return mTabInfoList;
  }

  @Override
  public boolean isTabSelected(EvaluatedTabInfo pTabInfo) {
    return mSelectedTabKey.equals(pTabInfo.getTabKey());
  }

  @Override
  public String getSelectedTabKey() {
    return mSelectedTabKey;
  }

  @Override
  public void refreshTabInfoList(List<EvaluatedTabInfo> pTabInfoList) {
    mTabInfoList.clear();
    mTabInfoList.addAll(pTabInfoList);
    DisplayOrder.sort(mTabInfoList);
  }

  @Override
  public Collection<PersistenceResult> create(PersistenceContext pPersistenceContext) {
    pPersistenceContext.getSerialiser().createModuleFacet(this);
    return Collections.singletonList(new PersistenceResult(this, PersistenceMethod.CREATE));
  }

  @Override
  public Collection<PersistenceResult> update(PersistenceContext pPersistenceContext) {
    pPersistenceContext.getSerialiser().updateModuleFacet(this);
    return Collections.singletonList(new PersistenceResult(this, PersistenceMethod.UPDATE));
  }

  @Override
  public Collection<PersistenceResult> delete(PersistenceContext pPersistenceContext) {
    //Should be deleted by module call delete
    return Collections.emptySet();
  }

  @Override
  public PersistableType getPersistableType() {
    return PersistableType.MODULE_FACET;
  }

  @Override
  public String getFacetKey() {
    return getTabGroupKey();
  }

  @Override
  public ModuleFacetType getFacetType() {
    return ModuleFacetType.TAB_GROUP;
  }
}
