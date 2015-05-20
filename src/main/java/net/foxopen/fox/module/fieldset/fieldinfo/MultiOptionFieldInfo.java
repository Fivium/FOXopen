package net.foxopen.fox.module.fieldset.fieldinfo;


import net.foxopen.fox.dom.DOM;
import net.foxopen.fox.ex.ExInternal;
import net.foxopen.fox.ex.ExTooMany;
import net.foxopen.fox.module.ChangeActionContext;
import net.foxopen.fox.module.fieldset.fieldmgr.OptionFieldMgr;
import net.foxopen.fox.module.fieldset.fvm.FVMOption;
import net.foxopen.fox.module.fieldset.fvm.FieldValueMapping;
import net.foxopen.fox.thread.ActionRequestContext;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


public class MultiOptionFieldInfo
extends FieldInfo {

  private final Set<String> mSentValues;
  private final FieldValueMapping mFVM;
  private final String mSelectorPath;

  public MultiOptionFieldInfo(String pExternalName, String pDOMRef, String pChangeActionName, Set<String> pSentStrings, FieldValueMapping pFVM, String pSelectorPath) {
    super(pExternalName, pDOMRef, pChangeActionName);
    mSentValues = pSentStrings;
    mFVM = pFVM;
    mSelectorPath = pSelectorPath;
  }

  @Override
  public List<ChangeActionContext> applyPostedValues(ActionRequestContext pRequestContext, String[] pPostedValues) {

    //The complex container
    DOM lTargetDOM = resolveTargetDOM(pRequestContext);

    //Determine actual strings posted back (remove prefix)
    Set<String> lPostedStrings = new HashSet<>();
    if(pPostedValues != null && pPostedValues.length > 0) {
      for(String lPostedValue : pPostedValues) {
        //Trim prefix off posted value
        lPostedValue = OptionFieldMgr.getOptionPostedValue(lPostedValue);
        lPostedStrings.add(lPostedValue);
      }
    }
    else if(pPostedValues == null || pPostedValues.length == 0) {
      //If nothing was sent back this is equivelant to null being sent back
      lPostedStrings.add(FieldValueMapping.NULL_VALUE);
    }

    boolean lChanged = false;

    //Shortcut out if the posted set exactly matches the sent set
    if(!lPostedStrings.equals(mSentValues)) {
      boolean lRemoved = removeDeselectedItems(pRequestContext, lPostedStrings, lTargetDOM);
      boolean lAdded = addSelectedItems(pRequestContext, lPostedStrings, lTargetDOM);

      lChanged = lRemoved || lAdded;
    }

    if(lChanged) {
      return createChangeActionContext(lTargetDOM);
    }
    else {
      return Collections.emptyList();
    }
  }

  private boolean removeDeselectedItems(ActionRequestContext pRequestContext, Set<String> pPostedStrings, DOM pTargetDOM) {
    boolean lChanged = false;

    //Determine explicitly deselected items
    Set<String> lSentButNotPosted = new HashSet<>(mSentValues);
    lSentButNotPosted.removeAll(pPostedStrings);

    //Remove items explicitly deselected by user
    for(String lSentString : lSentButNotPosted) {
      if(lSentString.startsWith(FieldValueMapping.UNRECOGNISED_PREFIX)) {
        //User has deselected an unrecognised option; work out its node ref and remove from the DOM
        String lDOMRef = OptionFieldMgr.getDOMRefFromUnrecognisedEntry(lSentString);
        DOM_LOOP:
        for(DOM lSelectedNode : pTargetDOM.getUL(mSelectorPath)) {
          if(lSelectedNode.getRef().equals(lDOMRef)) {
            lSelectedNode.remove();
            lChanged = true;
            break DOM_LOOP;
          }
        }
      }
      else if(FieldValueMapping.NULL_VALUE.equals(lSentString)) {
        //Null was sent but not received; doesn't matter
      }
      else {
        //User has deselected a previously selected value; remove from the DOM
        FVMOption lFVMOption = mFVM.getFVMOptionForRef(pRequestContext, pTargetDOM, lSentString);
        //Locate and remove child DOM
        DOM_LOOP:
        for(DOM lSelectedNode : pTargetDOM.getUL(mSelectorPath)) {
          if(lFVMOption.isEqualTo(lSelectedNode)) {
            lSelectedNode.remove();
            lChanged = true;
            break DOM_LOOP;
          }
        }
      }
    }
    return lChanged;
  }

  private boolean addSelectedItems(ActionRequestContext pRequestContext, Set<String> pPostedStrings, DOM pTargetDOM) {
    boolean lChanged = false;

    //Determine explicitly selected items
    Set<String> lPostedButNotSent = new HashSet<>(pPostedStrings);
    lPostedButNotSent.removeAll(mSentValues);

    //Add items explicitly selected by user
    for(String lPostedString : lPostedButNotSent) {
      if(lPostedString.startsWith(FieldValueMapping.UNRECOGNISED_PREFIX)) {
        //User submitted an unrecognised value but we didn't send it - shouldn't happen
        throw new ExInternal("Unrecognised value " + lPostedString + " was posted but not sent");
      }
      else if(FieldValueMapping.NULL_VALUE.equals(lPostedString)) {
        //Null was posted (either explict "None" option or just nothing was selected)
        if(pPostedStrings.size() > 1) {
          //Null was posted along with other values - this is currently possible (happened on legacy with multi-select selector+)
          //Tolerate for now by ignoring the null if other values have been posted
        }
        else {
          //Null was only thing posted, remove all selected elements
          pTargetDOM.getUL(mSelectorPath).removeFromDOMTree();
          lChanged = true;
        }
      }
      else {
        FVMOption lFVMOption = mFVM.getFVMOptionForRef(pRequestContext, pTargetDOM, lPostedString);
        //Search for a DOM node and create if not there (avoid creating dupes if it's already been added into the document by another thread)
        boolean lDOMAlreadyExists = false;
        DOM_LOOP:
        for(DOM lSelectedNode : pTargetDOM.getUL(mSelectorPath)) {
          if(lFVMOption.isEqualTo(lSelectedNode)) {
            lDOMAlreadyExists = true;
            break DOM_LOOP;
          }
        }

        if(!lDOMAlreadyExists) {
          //Not found in loop, create a new DOM child and set its content to the value of the FVM option
          lChanged = true;
          try {
            lFVMOption.applyToNode(pRequestContext, pTargetDOM.create1E(mSelectorPath));
          }
          catch (ExTooMany e) {}
        }
      }
    }

    return lChanged;
  }
}
