package net.foxopen.fox.module.fieldset.fieldmgr;

import net.foxopen.fox.XFUtil;
import net.foxopen.fox.module.datanode.EvaluatedNodeInfoItem;
import net.foxopen.fox.module.datanode.NodeAttribute;
import net.foxopen.fox.module.fieldset.FieldSelectConfig;
import net.foxopen.fox.module.fieldset.FieldSet;
import net.foxopen.fox.module.fieldset.fieldinfo.FieldInfo;
import net.foxopen.fox.module.fieldset.fieldinfo.SingleOptionFieldInfo;
import net.foxopen.fox.module.fieldset.fvm.FieldSelectOption;
import net.foxopen.fox.module.fieldset.fvm.FieldValueMapping;

import java.util.Collections;
import java.util.List;


public class SingleOptionFieldMgr
extends OptionFieldMgr {

  /** Will be null if value is null or unrecognised */
  protected final String mSelectedFVMOptionRef;

  private final boolean mIsNull;
  private final boolean mIsUnrecognised;

  protected SingleOptionFieldMgr(EvaluatedNodeInfoItem pEvaluatedNodeInfo, FieldSet pFieldSet, FieldSelectConfig pFieldSelectConfig, String pFieldId, FieldValueMapping pFVM) {
    super(pEvaluatedNodeInfo, pFieldSet, pFieldSelectConfig, pFieldId, pFVM);

    //Work out if this is null - it is if there's no text content and no children (ignoring fox error elements)
    //TODO PN consolidate logic for ignoring fox-errors
    mIsNull = "".equals(getValueDOM().value().trim()) && getValueDOM().getChildElements().removeAllNamesFromList("fox-error").size() == 0;

    if(mIsNull) {
      mSelectedFVMOptionRef = null;
      mIsUnrecognised = false;
    }
    else {
      mSelectedFVMOptionRef = mFVM.getFVMOptionRefForItem(this, getValueDOM());
      mIsUnrecognised = mSelectedFVMOptionRef == null;
    }
  }

  @Override
  protected boolean isNull() {
    return mIsNull;
  }

  @Override
  public boolean isRecognisedNotNullOptionSelected() {
    return !isNull() && !mIsUnrecognised;
  }

  protected String getSendingStringValue() {

    String lSendingString;
    if(mIsNull) {
      lSendingString = FieldValueMapping.NULL_VALUE;
    }
    else if(mIsUnrecognised) {
      lSendingString = getSentValueForUnrecognisedEntry(getValueDOM());
    }
    else {
      lSendingString = mSelectedFVMOptionRef;
    }

    return lSendingString;
  }

  @Override
  public FieldInfo createFieldInfoOrNull() {
    return new SingleOptionFieldInfo(getExternalFieldName(), getValueDOM().getRef(), getEvaluatedNodeInfoItem().getChangeActionName(), getSendingStringValue(), mFVM);
  }

  @Override
  public List<FieldSelectOption> getSelectOptions() {

    List<FieldSelectOption> lSelectOptions = mFVM.getSelectOptions(this, mIsNull ? Collections.<String>emptySet() : Collections.singleton(mSelectedFVMOptionRef));

    //If null, augment null entry
    augmentNullKeyIntoList(lSelectOptions, getEvaluatedNodeInfoItem());

    //if unrecognised, augment entry
    if(mIsUnrecognised) {
      String lUnrecognisedDisplayKey = XFUtil.nvl(getEvaluatedNodeInfoItem().getStringAttribute(NodeAttribute.KEY_UNRECOGNISED), getValueDOM().value());
      FieldSelectOption lUnrecognisedOption = mFVM.createFieldSelectOption(lUnrecognisedDisplayKey, true, false, getExternalValueForUnrecognisedEntry(getValueDOM()));
      lSelectOptions.add(lUnrecognisedOption);
    }

    return lSelectOptions;
  }

  @Override
  protected boolean isFVMOptionRefSelected(String pRef) {
    //Selected ref could be null for null/unrecognised value
    return mSelectedFVMOptionRef != null && mSelectedFVMOptionRef.equals(pRef);
  }
}
