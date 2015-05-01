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

  static DataFieldMgr createOptionFieldMgr(EvaluatedNodeInfoItem pEvalNode, FieldSet pFieldSet, FieldSelectConfig pFieldSelectConfig, String pFieldId){

    //Establish the FVM
    FieldValueMapping lFVM;
    NodeInfo lNodeInfo = pEvalNode.getNodeInfo();
    boolean lStrictBoolean = false;

    //Assume presence of "key-true/false" indicates a type of xs:boolean for phantom data nodes
    //A phantom-data-xpath with no corresponding target node must have a schema type of "phantom", so the developer needs some way of indicating that they require a boolean field
    if("xs:boolean".equals(lNodeInfo.getDataType()) || (pEvalNode.isPhantomDataNode() && (pEvalNode.isAttributeDefined(NodeAttribute.KEY_TRUE) || pEvalNode.isAttributeDefined(NodeAttribute.KEY_FALSE)))) {
      //Force "strict" booleans for radio buttons in groups - doesn't make sense to show true/false option tuples if at most one is selectable
      lStrictBoolean = pFieldSelectConfig.isStrictBoolean() || pEvalNode.isInRadioGroup();
      lFVM = BooleanFVM.getInstance(lStrictBoolean);
    }
    else if(pEvalNode.getSchemaEnumeration() != null) {
      lFVM = SchemaEnumFVM.createSchemaEnumFVM(pEvalNode);
    }
    else if(MapSetFVM.validateENI(pEvalNode)) {
      //Note mapset may be defined on repeating child element for legacy modules
      lFVM = MapSetFVM.createMapSetFVM(pEvalNode);
    }
    else {
      throw new ExInternal("Error in definition for " + pEvalNode.getIdentityInformation() + " - the widget has no source of options (map-set, schema enumeration or xs:boolean type)");
    }

    DataFieldMgr lNewFieldMgr;
    if(!pEvalNode.isMultiSelect()) {
      //Construct the new FieldMgr
      if(lStrictBoolean) {
        lNewFieldMgr = new StrictBooleanFieldMgr(pEvalNode, pFieldSet, pFieldSelectConfig, pFieldId);
      }
      else {
        lNewFieldMgr = new SingleOptionFieldMgr(pEvalNode, pFieldSet, pFieldSelectConfig, pFieldId, lFVM);
      }
    }
    else {
      lNewFieldMgr = new MultiOptionFieldMgr(pEvalNode, pFieldSet, pFieldSelectConfig, pFieldId, lFVM);
    }

    //If this field is in a radio group, we need to wrap the fieldmgr
    if(pEvalNode.isInRadioGroup()) {
      return new RadioGroupValueFieldMgr(pEvalNode, pFieldSet, pFieldId, lNewFieldMgr);
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

  /**
   * Gets the external value (i.e. the "value" attribute in HTML) for the FVMOption with the given ref of this FieldMgr's
   * FieldValueMapping.
   * @param pOptionRef FVM option ref.
   * @return External value.
   */
  public String getExternalValueForOptionRef(String pOptionRef) {
    return getFieldId() + "/" + pOptionRef;
  }

  /**
   * Gets the external value (i.e. the "value" attribute in HTML) for an "unrecognised" entry which will never have a
   * corresponding FVMOption because it is not valid for this FieldMgr's FVM.
   * @param pUnrecognisedItemDOM DOM containing unrecognised data.
   * @return External value.
   */
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
    String lItemRef = mFVM.getFVMOptionRefForItem(this, DOM.createUnconnectedText(pStringValue));
    if(lItemRef != null) {
      return isFVMOptionRefSelected(lItemRef);
    }
    else {
      Track.alert("OptionMissing", "Failed to locate value for '" + pStringValue + "' in FieldValueMapping for field " + getEvaluatedNodeInfoItem().getIdentityInformation());
      return false;
    }
  }

  /**
   * Uses the FieldMgr's FVM to determine what the external value (i.e. value HTML attribute) will be for the given string.
   * I.e. for a mapset the data value "OPTION_1" may have an external value of "g22/1".
   * @param pOptionValue Data DOM value to determine external value for.
   * @return External value of if a corresponding FVMOption exists for the given DOM value, or empty string if one doesn't.
   */
  public String getExternalValueForOption(String pOptionValue) {
    String lItemRef = mFVM.getFVMOptionRefForItem(this, DOM.createUnconnectedText(pOptionValue));
    if(lItemRef != null) {
      return getExternalValueForOptionRef(lItemRef);
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

  /**
   * Tests if the given FVMOption ref is currently selected in this FieldMgr.
   * @param pRef Ref of an FVMOption.
   * @return True if selected.
   */
  protected abstract boolean isFVMOptionRefSelected(String pRef);
}
