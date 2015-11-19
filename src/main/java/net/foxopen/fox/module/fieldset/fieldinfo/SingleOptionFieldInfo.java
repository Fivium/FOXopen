package net.foxopen.fox.module.fieldset.fieldinfo;

import net.foxopen.fox.dom.DOM;
import net.foxopen.fox.module.ChangeActionContext;
import net.foxopen.fox.module.fieldset.fieldmgr.OptionFieldMgr;
import net.foxopen.fox.module.fieldset.fvm.FVMOption;
import net.foxopen.fox.module.fieldset.fvm.FieldValueMapping;
import net.foxopen.fox.thread.ActionRequestContext;

import java.util.Collections;
import java.util.List;


public class SingleOptionFieldInfo
extends SingleValueFieldInfo {

  private final FieldValueMapping mFVM;

  private final boolean mIsFreeTextAllowed;

  public SingleOptionFieldInfo(String pExternalName, String pDOMRef, String pChangeActionName, String pSentValue, FieldValueMapping pFVM, boolean pIsFreeTextAllowed) {
    super(pExternalName, pDOMRef, pChangeActionName, pSentValue);
    mFVM = pFVM;
    mIsFreeTextAllowed = pIsFreeTextAllowed;
  }

  /**
   * Overloaded getSinglePostedValue for getting slash-seperated suffix from the actual posted value. For Option fields
   * this suffix either represents the ref of the selected option or is a special single character value for the null/unrecognised key.
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
      lTargetDOM.removeAttr(OptionFieldMgr.FREE_TEXT_ATTR);

      if (mIsFreeTextAllowed && lPostedValue.startsWith(FieldValueMapping.FREE_TEXT_PREFIX)) {
        // Make sure the element has the free text attr on it
        lTargetDOM.setAttr(OptionFieldMgr.FREE_TEXT_ATTR, "true");
        // Set the new free text value
        lTargetDOM.setText(lPostedValue.substring(FieldValueMapping.FREE_TEXT_PREFIX.length()));
      }
      else if(!FieldValueMapping.NULL_VALUE.equals(lPostedValue)) {
        FVMOption lSelectedOption = mFVM.getFVMOptionForRef(pRequestContext, lTargetDOM, lPostedValue);
        lSelectedOption.applyToNode(pRequestContext, lTargetDOM);
      }

      return createChangeActionContext(lTargetDOM);
    }
    else {
      return Collections.emptyList();
    }
  }

}
