package net.foxopen.fox.module.fieldset;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.foxopen.fox.ex.ExInternal;
import net.foxopen.fox.module.ChangeActionContext;
import net.foxopen.fox.module.fieldset.fieldinfo.FieldInfo;
import net.foxopen.fox.thread.ActionRequestContext;


public class RadioGroup
implements PostedValueProcessor {

  /** Key composed of owner/group name tuple */
  private final String mRadioKey;

  /** ID of the group within the FieldSet (to be used as the HTML "name" attribute) */
  private final String mRadioGroupId;

  /** Map of Field IDs to the FieldInfos which are in this group.  */
  private final Map<String, FieldInfo> mGroupedFields = new HashMap<>();

  private RadioGroup(String pRadioKey, String pExternalFieldName) {
    mRadioKey = pRadioKey;
    mRadioGroupId = pExternalFieldName;
  }

  public static String getRadioGroupKey(String pOwnerDOMRef, String pGroupName) {
    return pOwnerDOMRef + "/" + pGroupName;
  }

  public static RadioGroup createRadioGroup(String pOwnerDOMRef, String pGroupName, String pFieldId) {
    return new RadioGroup(getRadioGroupKey(pOwnerDOMRef, pGroupName), pFieldId);
  }

  public void addGroupedField(String pInternalFieldId, FieldInfo pFieldInfo) {
    mGroupedFields.put(pInternalFieldId, pFieldInfo);
  }

  @Override
  public List<ChangeActionContext> applyPostedValues(ActionRequestContext pRequestContext, String[] pPostedValues) {

    //Work out the actual field a value was submitted for
    String lSubmittedFieldId;
    if(pPostedValues == null) {
      lSubmittedFieldId = "";
    }
    else if (pPostedValues.length > 1) {
      throw new ExInternal("Radio group expected exactly one value, got " + pPostedValues.length);
    }
    else {
      lSubmittedFieldId = pPostedValues[0].split("/")[0];
    }

    List<ChangeActionContext> lChangeActions = new ArrayList<>();

    String[] lEmptyPostedValue = {""};

    for(Map.Entry<String, FieldInfo> lGroupedFieldEntry : mGroupedFields.entrySet()) {
      //Tell the FieldInfo for the submitted field what was actually sent - all other fields are effectively sent an empty string
      String[] lPostedValue;
      if(lGroupedFieldEntry.getKey().equals(lSubmittedFieldId)) {
        lPostedValue = pPostedValues;
      }
      else {
        lPostedValue = lEmptyPostedValue;
      }
      List<ChangeActionContext> lResultChangeActions = lGroupedFieldEntry.getValue().applyPostedValues(pRequestContext, lPostedValue);
      lChangeActions.addAll(lResultChangeActions);
    }

    return lChangeActions;
  }

  public Collection<FieldInfo> getGroupedFieldInfos() {
    return mGroupedFields.values();
  }

  /**
   * Gets the radio key for this group - typically the tuple of group name plus optional group owner. This allows the group
   * to be identified
   * @return
   */
  public String getRadioKey() {
    return mRadioKey;
  }

  /**
   * Gets the ID of this radio group. All radio options belonging to this group should use this as their field name.
   * @return
   */
  public String getRadioGroupId() {
    return mRadioGroupId;
  }

  @Override
  public String getExternalName() {
    return mRadioGroupId;
  }
}
