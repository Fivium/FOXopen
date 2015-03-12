package net.foxopen.fox.module.fieldset.fieldinfo;

import net.foxopen.fox.module.ChangeActionContext;
import net.foxopen.fox.module.fieldset.TabGroupHiddenField;
import net.foxopen.fox.module.tabs.TabGroup;
import net.foxopen.fox.module.tabs.TabGroupProvider;
import net.foxopen.fox.thread.ActionRequestContext;

import java.util.Collections;
import java.util.List;

public class TabGroupInternalFieldInfo
extends InternalFieldInfo {

  public TabGroupInternalFieldInfo(String pExternalName, String pCurrentTabKey) {
    super(pExternalName, pCurrentTabKey);
  }

  @Override
  public List<ChangeActionContext> applyPostedValues(ActionRequestContext pRequestContext, String[] pPostedValues) {

    String lPostedValue = SingleValueFieldInfo.singlePostedValue(pPostedValues, getExternalName());
    if(!lPostedValue.equals(getSentValue())) {
      String lTabGroupKey = getExternalName().replace(TabGroupHiddenField.FIELD_NAME_PREFIX, "");
      TabGroup lTabGroup = pRequestContext.getModuleFacetProvider(TabGroupProvider.class).getTabGroupByKey(lTabGroupKey);
      lTabGroup.selectTab(pRequestContext.getPersistenceContext(), lPostedValue);
    }

    return Collections.emptyList();
  }
}
