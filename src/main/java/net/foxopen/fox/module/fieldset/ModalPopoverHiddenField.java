package net.foxopen.fox.module.fieldset;

import net.foxopen.fox.module.fieldset.fieldinfo.InternalFieldInfo;
import net.foxopen.fox.module.fieldset.fieldinfo.ModalPopoverInternalFieldInfo;

/**
 * Hidden field for saving the scroll position of a modal popover.
 */
public class ModalPopoverHiddenField
extends InternalHiddenField {

  private static final String MODAL_INTERNAL_FIELD_NAME = "modal_scroll_position";

  public ModalPopoverHiddenField(String pCurrentScrollPosition) {
    //Fixed name as there will only ever be one modal on a screen and therefore only one field required
    super(MODAL_INTERNAL_FIELD_NAME, pCurrentScrollPosition);
  }

  @Override
  protected InternalFieldInfo createFieldInfo() {
    return new ModalPopoverInternalFieldInfo(getExternalFieldName(), getSendingValue());
  }
}
