package net.foxopen.fox.module.fieldset.fieldmgr;

import net.foxopen.fox.module.datanode.EvaluatedNodeInfoItem;
import net.foxopen.fox.module.fieldset.FieldSet;
import net.foxopen.fox.module.fieldset.RadioGroup;
import net.foxopen.fox.module.fieldset.fieldinfo.FieldInfo;
import net.foxopen.fox.module.fieldset.fvm.FieldSelectOption;

import java.util.List;


/**
 * Decorator for a FieldMgr which represents a field within a radio group. This ensures the underlying FieldMgr is given
 * the correct field name when output, and ensures the FieldInfo is created from the correct location (the RadioGroup rather
 * than the FieldSet - a RadioGroup only has one FieldInfo for all its fields).
 */
public class RadioGroupValueFieldMgr
extends DataFieldMgr {

  private final RadioGroup mRadioGroup;
  private final FieldMgr mWrappedFieldMgr;

  public RadioGroupValueFieldMgr(EvaluatedNodeInfoItem pEvaluatedNodeInfo, FieldSet pFieldSet, String pFieldId, FieldMgr pWrappedFieldMgr) {
    super(pEvaluatedNodeInfo, pFieldSet, pFieldId);
    mRadioGroup = pFieldSet.getOrCreateRadioGroup(pEvaluatedNodeInfo);
    mWrappedFieldMgr = pWrappedFieldMgr;
  }

  @Override
  public FieldInfo createFieldInfoOrNull() {
    mRadioGroup.addGroupedField(getFieldId(), mWrappedFieldMgr.createFieldInfoOrNull());
    return null; //will be handled by radio group
  }

  public String getExternalFieldName() {
    return mRadioGroup.getRadioGroupId();
  }

  @Override
  public List<FieldSelectOption> getSelectOptions() {
    return mWrappedFieldMgr.getSelectOptions();
  }

  @Override
  public String getSingleTextValue() {
    return mWrappedFieldMgr.getSingleTextValue();
  }
}
