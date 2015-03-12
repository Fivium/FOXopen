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

  /** Will be -2 if null, -1 if unrecognised, or >= 0 reflecting FVM index */
  protected final int mSelectedIndex;

  private final boolean mIsNull;

  protected SingleOptionFieldMgr(EvaluatedNodeInfoItem pEvaluatedNodeInfo, FieldSet pFieldSet, FieldSelectConfig pFieldSelectConfig, String pFieldId, FieldValueMapping pFVM) {
    super(pEvaluatedNodeInfo, pFieldSet, pFieldSelectConfig, pFieldId, pFVM);

    //Work out if this is null - it is if there's no text content and no children (ignoring fox error elements)
    //TODO PN consolidate logic for ignoring fox-errors
    mIsNull = "".equals(getValueDOM().value().trim()) && getValueDOM().getChildElements().removeAllNamesFromList("fox-error").size() == 0;

    if(mIsNull) {
      mSelectedIndex = -2;
    }
    else {
      mSelectedIndex = mFVM.getIndexForItem(this, getValueDOM());
    }
  }

  @Override
  protected boolean isNull() {
    return mIsNull;
  }

  @Override
  public boolean isRecognisedOptionSelected() {
    return mSelectedIndex >= 0;
  }

  protected String getSendingStringValue() {

    String lSendingString;
    if(mIsNull) {
      lSendingString = FieldValueMapping.NULL_VALUE;
    }
    else if(mSelectedIndex == -1) {
      lSendingString = getSentValueForUnrecognisedEntry(getValueDOM());
    }
    else {
      lSendingString = String.valueOf(mSelectedIndex);
    }

    return lSendingString;
  }

  @Override
  public FieldInfo createFieldInfoOrNull() {
    return new SingleOptionFieldInfo(getExternalFieldName(), getValueDOM().getRef(), getEvaluatedNodeInfoItem().getChangeActionName(), getSendingStringValue(), mFVM);
  }


  @Override
  public List<FieldSelectOption> getSelectOptions() {

    List<FieldSelectOption> lSelectOptions = mFVM.getSelectOptions(this, Collections.singleton(mSelectedIndex));

    //If null, augment null entry
    augmentNullKeyIntoList(lSelectOptions, getEvaluatedNodeInfoItem());

    //if unrecognised, augment entry
    if(mSelectedIndex == -1) {
      String lUnrecognisedDisplayKey = XFUtil.nvl(getEvaluatedNodeInfoItem().getStringAttribute(NodeAttribute.KEY_UNRECOGNISED), getValueDOM().value());
      FieldSelectOption lUnrecognisedOption = mFVM.createFieldSelectOption(lUnrecognisedDisplayKey, true, false, getExternalValueForUnrecognisedEntry(getValueDOM()));
      lSelectOptions.add(lUnrecognisedOption);
    }

    return lSelectOptions;
  }


  @Override
  protected boolean isIndexSelected(int pIndex) {
    //Check consumer isn't trying to test for a special value
    return mSelectedIndex >= 0 && mSelectedIndex == pIndex;
  }
}
