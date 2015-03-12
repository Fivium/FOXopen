package net.foxopen.fox.module.fieldset.fieldmgr;


import com.google.common.base.Joiner;
import net.foxopen.fox.dom.DOM;
import net.foxopen.fox.ex.ExInternal;
import net.foxopen.fox.module.datanode.EvaluatedNodeInfoItem;
import net.foxopen.fox.module.fieldset.FieldSelectConfig;
import net.foxopen.fox.module.fieldset.FieldSet;
import net.foxopen.fox.module.fieldset.fvm.BooleanFVM;
import net.foxopen.fox.module.fieldset.fvm.FieldSelectOption;
import net.foxopen.fox.module.fieldset.fvm.FieldValueMapping;
import net.foxopen.fox.module.fieldset.fvm.MapSetFVM;
import net.foxopen.fox.module.fieldset.fvm.SchemaEnumFVM;
import net.foxopen.fox.module.datanode.EvaluatedNodeInfo;
import net.foxopen.fox.module.datanode.NodeAttribute;
import net.foxopen.fox.module.datanode.NodeInfo;
import net.foxopen.fox.track.Track;

import java.util.ArrayList;
import java.util.List;


public abstract class OptionFieldMgr
extends DataFieldMgr {

  //TODO make private and add accessors
  protected final FieldSelectConfig mConfig;
  protected final FieldValueMapping mFVM;

  static DataFieldMgr createOptionFieldMgr(EvaluatedNodeInfoItem pEvaluatedNodeInfoItem, FieldSet pFieldSet, FieldSelectConfig pFieldSelectConfig, String pFieldId){

    //Establish the FVM
    FieldValueMapping lFVM;
    NodeInfo lNodeInfo = pEvaluatedNodeInfoItem.getNodeInfo();
    boolean lStrictBoolean = false;

    if("xs:boolean".equals(lNodeInfo.getDataType())) {
      //Force "strict" booleans for radio buttons in groups - doesn't make sense to show true/false option tuples if at most one is selectable
      lStrictBoolean = pFieldSelectConfig.isStrictBoolean() || pEvaluatedNodeInfoItem.isInRadioGroup();
      lFVM = BooleanFVM.getInstance(lStrictBoolean);
    }
    else if(pEvaluatedNodeInfoItem.getSchemaEnumeration() != null) {
      lFVM = SchemaEnumFVM.createSchemaEnumFVM(pEvaluatedNodeInfoItem);
    }
    else if(MapSetFVM.validateENI(pEvaluatedNodeInfoItem)) {
      //Note mapset may be defined on repeating child element for legacy modules
      lFVM = MapSetFVM.createMapSetFVM(pEvaluatedNodeInfoItem);
    }
    else {
      throw new ExInternal("Error in definition for " + pEvaluatedNodeInfoItem.getIdentityInformation() + " - the widget has no source of options (map-set, schema enumeration or xs:boolean type)");
    }

    DataFieldMgr lNewFieldMgr;
    if(!pEvaluatedNodeInfoItem.isMultiSelect()) {
      //Construct the new FieldMgr
      if(lStrictBoolean) {
        lNewFieldMgr = new StrictBooleanFieldMgr(pEvaluatedNodeInfoItem, pFieldSet, pFieldSelectConfig, pFieldId);
      }
      else {
        lNewFieldMgr = new SingleOptionFieldMgr(pEvaluatedNodeInfoItem, pFieldSet, pFieldSelectConfig, pFieldId, lFVM);
      }
    }
    else {
      lNewFieldMgr = new MultiOptionFieldMgr(pEvaluatedNodeInfoItem, pFieldSet, pFieldSelectConfig, pFieldId, lFVM);
    }

    //If this field is in a radio group, we need to wrap the fieldmgr
    if(pEvaluatedNodeInfoItem.isInRadioGroup()) {
      return new RadioGroupValueFieldMgr(pEvaluatedNodeInfoItem, pFieldSet, pFieldId, lNewFieldMgr);
    }
    else {
      return lNewFieldMgr;
    }
  }

  public static String getOptionPostedValue(String pPostedValue) {
    if(pPostedValue.indexOf('/') != -1) {
      return pPostedValue.split("/")[1];
    }
    else {
      return pPostedValue;
    }
  }

  public static String getDOMRefFromUnrecognisedEntry(String pPostedString) {
    if(pPostedString.startsWith(FieldValueMapping.UNRECOGNISED_PREFIX)) {
      return pPostedString.replaceFirst(FieldValueMapping.UNRECOGNISED_PREFIX, "");
    }
    else {
      return pPostedString;
    }
  }

  protected OptionFieldMgr(EvaluatedNodeInfoItem pEvaluatedNodeInfo, FieldSet pFieldSet, FieldSelectConfig pFieldSelectConfig, String pFieldId, FieldValueMapping pFVM) {
    super(pEvaluatedNodeInfo, pFieldSet, pFieldId);
    mConfig = pFieldSelectConfig;
    mFVM = pFVM;
  }

  @Override
  public String getSingleTextValue() {

    List<String> lSelectedExternalStrings = new ArrayList<>();
    for(FieldSelectOption lOption : getSelectOptions()) {
      if(lOption.isSelected()) {
        lSelectedExternalStrings.add(lOption.getDisplayKey());
      }
    }

    return Joiner.on(", ").join(lSelectedExternalStrings);
  }

  @Override
  public String getExternalFieldName() {
    return getFieldId();
  }

  public String getExternalValueForOption(int pOptionIndex) {
    return getFieldId() + "/" + pOptionIndex;
  }

  protected String getExternalValueForUnrecognisedEntry(DOM pUnrecognisedItemDOM) {
    return getFieldId() + "/" + getSentValueForUnrecognisedEntry(pUnrecognisedItemDOM);
  }

  protected String getSentValueForUnrecognisedEntry(DOM pUnrecognisedItemDOM) {
    return FieldValueMapping.UNRECOGNISED_PREFIX + pUnrecognisedItemDOM.getRef();
  }

  /**
   * Tests if the user has selected at least 1 valid option for this field.
   * @return
   */
  public abstract boolean isRecognisedOptionSelected();

  protected abstract boolean isNull();

  protected void augmentNullKeyIntoList(List<FieldSelectOption> pSelectList, EvaluatedNodeInfo pEvaluatedNodeInfo) {
    boolean lIsNull = isNull();

    String lDisplayKey;
    boolean lIsKeyNull;
    boolean lIsMand = pEvaluatedNodeInfo.isMandatory();
    if(lIsMand && mConfig.isAddKeyMissing() && lIsNull) {
      //If field is mandatory and currently null, and widget allows, add key-missing
      lDisplayKey = pEvaluatedNodeInfo.getStringAttribute(NodeAttribute.KEY_MISSING);
      if(lDisplayKey == null) {
        lDisplayKey = pEvaluatedNodeInfo.isMultiSelect() ? "Select Many" : "Select One";
      }
      lIsKeyNull = true;
    }
    else if(!lIsMand && mConfig.isAddKeyNull()) {
      //If field is not mandatory and currently null, and widget allows, add key-null
      lDisplayKey = pEvaluatedNodeInfo.getStringAttribute(NodeAttribute.KEY_NULL, "None");
      lIsKeyNull = false;
    }
    else {
      //Skip adding anything if no extra key is required
      return;
    }

    //Add to front of list
    pSelectList.add(0, mFVM.createFieldSelectOption(lDisplayKey, lIsNull, lIsKeyNull, getExternalValueForNullSelection()));
  }

  /**
   * Tests if the given string value is currently selected in this field. Note that this test can only be used if the
   * underlying data this field represents is in String format (this will not work with complex mapsets, for instance).
   * @param pStringValue Value to test.
   * @return
   */
  public boolean isStringValueSelected(String pStringValue) {
    //TODO PN pass string through directly to FVM (requires API changes - StringOrDOM)
    int lItemIndex = mFVM.getIndexForItem(this, DOM.createUnconnectedText(pStringValue));
    if(lItemIndex != -1) {
      return isIndexSelected(lItemIndex);
    }
    else {
      Track.alert("OptionMissing", "Failed to locate value for '" + pStringValue + "' in FieldValueMapping for field " + getEvaluatedNodeInfoItem().getIdentityInformation());
      return false;
    }
  }

  public String getExternalValueForOption(String pOptionValue) {
    int lItemIndex = mFVM.getIndexForItem(this, DOM.createUnconnectedText(pOptionValue));
    if(lItemIndex != -1) {
      return getFieldId() + "/" + lItemIndex;
    }
    else {
      return "";
    }
  }

  public String getExternalValueForNullSelection() {
    return getFieldId() + "/" + FieldValueMapping.NULL_VALUE;
  }

  public boolean isStrictBoolean() {
    return mConfig.isStrictBoolean();
  }

  protected abstract boolean isIndexSelected(int pIndex);
}
