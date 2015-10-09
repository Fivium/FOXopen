package net.foxopen.fox.module.fieldset.fieldinfo;

import net.foxopen.fox.module.ChangeActionContext;
import net.foxopen.fox.module.facet.ModalPopover;
import net.foxopen.fox.module.facet.ModalPopoverProvider;
import net.foxopen.fox.thread.ActionRequestContext;

import java.util.Collections;
import java.util.List;

/**
 * InternalFieldInfo used to control the scroll position of a modal popover based on the posted value from a
 * {@link net.foxopen.fox.module.fieldset.ModalPopoverHiddenField}.
 */
public class ModalPopoverInternalFieldInfo
extends InternalFieldInfo {

  public ModalPopoverInternalFieldInfo(String pExternalName, String pCurrentScrollPosition) {
    super(pExternalName, pCurrentScrollPosition);
  }

  @Override
  public List<ChangeActionContext> applyPostedValues(ActionRequestContext pRequestContext, String[] pPostedValues) {

    String lPostedValue = SingleValueFieldInfo.singlePostedValue(pPostedValues, getExternalName());
    if (!lPostedValue.equals(getSentValue())) {
      int lScrollPos = (int) Double.parseDouble(lPostedValue);
      ModalPopover lModalPopover = pRequestContext.getModuleFacetProvider(ModalPopoverProvider.class).getCurrentPopoverOrNull();
      if(lModalPopover != null) {
        lModalPopover.setScrollPosition(pRequestContext.getPersistenceContext(), lScrollPos);
      }
    }

    return Collections.emptyList();
  }
}
