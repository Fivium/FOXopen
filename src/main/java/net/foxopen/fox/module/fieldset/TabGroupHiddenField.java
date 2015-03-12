package net.foxopen.fox.module.fieldset;

import net.foxopen.fox.module.fieldset.fieldinfo.InternalFieldInfo;
import net.foxopen.fox.module.fieldset.fieldinfo.TabGroupInternalFieldInfo;

/**
 * Hidden field for submitting the currently selected tab for a client side TabGroup.
 */
public class TabGroupHiddenField extends InternalHiddenField {

  public static final String FIELD_NAME_PREFIX = "_int_tab_";

  public TabGroupHiddenField(String pTabGroupKey, String pCurrentTabKey) {
    super(FIELD_NAME_PREFIX + pTabGroupKey, pCurrentTabKey);
  }

  @Override
  protected InternalFieldInfo createFieldInfo() {
    return new TabGroupInternalFieldInfo(getExternalFieldName(), getSendingValue());
  }
}
