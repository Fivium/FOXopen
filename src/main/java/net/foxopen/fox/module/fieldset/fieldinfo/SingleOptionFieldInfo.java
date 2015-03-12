package net.foxopen.fox.module.fieldset.fieldinfo;

import java.util.Collections;
import java.util.List;

import net.foxopen.fox.dom.DOM;
import net.foxopen.fox.module.ChangeActionContext;
import net.foxopen.fox.module.fieldset.fvm.FieldValueMapping;
import net.foxopen.fox.module.fieldset.fvm.FVMOption;
import net.foxopen.fox.module.fieldset.fieldmgr.OptionFieldMgr;
import net.foxopen.fox.thread.ActionRequestContext;


public class SingleOptionFieldInfo
extends SingleValueFieldInfo {

  private final FieldValueMapping mFVM;

  public SingleOptionFieldInfo(String pExternalName, String pDOMRef, String pChangeActionName, String pSentValue, FieldValueMapping pFVM) {
    super(pExternalName, pDOMRef, pChangeActionName, pSentValue);
    mFVM = pFVM;
  }

  /**
   * Overloaded getSinglePostedValue for getting slash-seperated suffix from the actual posted value. For Option fields
   * this suffix either represents the index of the selected option or is a special single character value for the null/unrecognised key.
   * @param pPostedValues
   * @return
   */
  @Override
  protected String getSinglePostedValue(String[] pPostedValues) {

    String lPostedValue = super.getSinglePostedValue(pPostedValues);
    //Trim prefix off posted value
    lPostedValue = OptionFieldMgr.getOptionPostedValue(lPostedValue);

    //If nothing was sent, could be either a mandatory radio which hasn't been set yet, or part of a radio group which has been de-selected
    //Either way it's effectively been set to null
    lPostedValue = "".equals(lPostedValue) ? FieldValueMapping.NULL_VALUE : lPostedValue;

    return lPostedValue;
  }

  @Override
  public List<ChangeActionContext> applyPostedValues(ActionRequestContext pRequestContext, String[] pPostedValues) {

    String lPostedValue = getSinglePostedValue(pPostedValues);

    //Skip apply if the returned value was a "key-missing", or if no change has occurred
    if(!lPostedValue.startsWith(FieldValueMapping.UNRECOGNISED_PREFIX) && !lPostedValue.equals(getSentValue())) {

      DOM lTargetDOM = resolveAndClearTargetDOM(pRequestContext);

      if(!FieldValueMapping.NULL_VALUE.equals(lPostedValue)) {
        int lPostedValueAsInt = Integer.parseInt(lPostedValue);
        FVMOption lSelectedOption = mFVM.getFVMOptionList(pRequestContext, lTargetDOM).get(lPostedValueAsInt);
        lSelectedOption.applyToNode(lTargetDOM);
      }

      return createChangeActionContext(lTargetDOM);
    }
    else {
      return Collections.emptyList();
    }
  }

}
