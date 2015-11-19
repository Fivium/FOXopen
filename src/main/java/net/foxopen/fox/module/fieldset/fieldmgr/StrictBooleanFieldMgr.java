package net.foxopen.fox.module.fieldset.fieldmgr;

import net.foxopen.fox.XFUtil;
import net.foxopen.fox.dom.DOM;
import net.foxopen.fox.module.ChangeActionContext;
import net.foxopen.fox.module.datanode.EvaluatedNodeInfoItem;
import net.foxopen.fox.module.fieldset.FieldSelectConfig;
import net.foxopen.fox.module.fieldset.FieldSet;
import net.foxopen.fox.module.fieldset.fieldinfo.FieldInfo;
import net.foxopen.fox.module.fieldset.fieldinfo.SingleOptionFieldInfo;
import net.foxopen.fox.module.fieldset.fvm.BooleanFVM;
import net.foxopen.fox.module.fieldset.fvm.FieldSelectOption;
import net.foxopen.fox.module.fieldset.fvm.FieldValueMapping;
import net.foxopen.fox.thread.ActionRequestContext;

import java.util.Collections;
import java.util.List;

/**
 * An OptionFieldMgr representing a boolean which cannot be null (i.e. a checkbox). C.f. other FieldMgr types which allow
 * null booleans (such as radio or selector widgets).
 */
public class StrictBooleanFieldMgr
extends SingleOptionFieldMgr {

  protected StrictBooleanFieldMgr(EvaluatedNodeInfoItem pEvaluatedNodeInfo, FieldSet pFieldSet, FieldSelectConfig pConfig, String pFieldId) {
    super(pEvaluatedNodeInfo, pFieldSet, pConfig, pFieldId, BooleanFVM.getInstance(true));
  }

  @Override
  public FieldInfo createFieldInfoOrNull() {
    return new StrictBooleanFieldInfo(getExternalFieldName(), getValueDOM().getRef(), getEvaluatedNodeInfoItem().getChangeActionName(), getSendingStringValue());
  }

  public List<FieldSelectOption> getSelectOptions() {
    //Overloaded parent to prevent addition of null option for strict boolean
    return mFVM.getSelectOptions(this, Collections.singleton(mSelectedFVMOptionRef));
  }

  private static class StrictBooleanFieldInfo
  extends SingleOptionFieldInfo {
    public StrictBooleanFieldInfo(String pExternalName, String pDOMRef, String pChangeActionName, String pSentValue) {
      super(pExternalName, pDOMRef, pChangeActionName, pSentValue, null, false);
    }

    public List<ChangeActionContext> applyPostedValues(ActionRequestContext pRequestContext, String[] pPostedValues) {

      String lPostedValue = getSinglePostedValue(pPostedValues);

      //Skip apply if FALSE was sent out and null returned - for strict booleans null is equivelant to false
      if(!getSentValue().equals(lPostedValue) && !(BooleanFVM.FALSE_VALUE.equals(getSentValue()) && FieldValueMapping.NULL_VALUE.equals(lPostedValue) )) {

        DOM lTargetDOM = resolveAndClearTargetDOM(pRequestContext);
        if(BooleanFVM.TRUE_VALUE.equals(lPostedValue)) {
          lTargetDOM.setText(BooleanFVM.TRUE_STRING);
        }
        else {
          lTargetDOM.setText(BooleanFVM.FALSE_STRING);
        }

        return createChangeActionContext(lTargetDOM);
      }
      else {
        //Skip apply if we didn't send true and didn't get it back for strict booleans - prevents null value in DOM becoming "false" on post when not actually touched
        return Collections.emptyList();
      }
    }
  }

  @Override
  public boolean isStringValueSelected(String pStringValue) {
    //Overloaded so "empty string" is interpreted as false for the purpose of determining what has been selected
    return super.isStringValueSelected(XFUtil.nvl(pStringValue, "false"));
  }
}
