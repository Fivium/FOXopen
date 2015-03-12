package net.foxopen.fox.module.fieldset;

import net.foxopen.fox.module.fieldset.fieldinfo.InternalFieldInfo;

/**
 * FieldMgr style class for non-EvaluatedNode based hidden fields. This should be used by client side page components
 * in order to save the state of the component when the form is submitted. All hidden fields should have a name and initial
 * text value - client side JavaScript should manipulate the field value when appropriate (see {@link InternalFieldInfo}).
 */
public abstract class InternalHiddenField {

  private final String mFieldId;
  private final String mSendingValue;

  protected InternalHiddenField(String pFieldId, String pSendingValue) {
    mFieldId = pFieldId;
    mSendingValue = pSendingValue;
  }

  protected abstract InternalFieldInfo createFieldInfo();

  public void addToFieldSet(FieldSet pFieldSet) {
    pFieldSet.addInternalField(createFieldInfo());
  }

  public String getExternalFieldName() {
    return mFieldId;
  }

  public String getSendingValue() {
    return mSendingValue;
  }

}
