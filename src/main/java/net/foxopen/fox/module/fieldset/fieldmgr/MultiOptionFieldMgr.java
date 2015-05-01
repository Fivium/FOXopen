package net.foxopen.fox.module.fieldset.fieldmgr;

import net.foxopen.fox.XFUtil;
import net.foxopen.fox.dom.DOM;
import net.foxopen.fox.dom.DOMList;
import net.foxopen.fox.ex.ExCardinality;
import net.foxopen.fox.ex.ExInternal;
import net.foxopen.fox.module.datanode.EvaluatedNodeInfoItem;
import net.foxopen.fox.module.datanode.NodeAttribute;
import net.foxopen.fox.module.datanode.NodeVisibility;
import net.foxopen.fox.module.fieldset.FieldSelectConfig;
import net.foxopen.fox.module.fieldset.FieldSet;
import net.foxopen.fox.module.fieldset.fieldinfo.FieldInfo;
import net.foxopen.fox.module.fieldset.fieldinfo.MultiOptionFieldInfo;
import net.foxopen.fox.module.fieldset.fvm.FieldSelectOption;
import net.foxopen.fox.module.fieldset.fvm.FieldValueMapping;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;


public class MultiOptionFieldMgr
extends OptionFieldMgr {

  //TODO PN - this may contain nulls for unrecognised items - needs work
  /** Currently selected FVMOption ref */
  private final List<String> mSelectedFVMOptionRefs;
  //Small initial capacity for unrecognised options as typically there should not be any
  private final List<FieldSelectOption> mUnrecognisedOptions = new ArrayList<>(1);
  private final Set<String> mUnrecognisedSentStrings = new HashSet<>(1);

  /** Path to the repeating item of this FieldMgr relative to the value DOM */
  private final String mSelectorPath;

  public MultiOptionFieldMgr(EvaluatedNodeInfoItem pEvaluatedNodeInfo, FieldSet pFieldSet, FieldSelectConfig pFieldSelectConfig, String pFieldId, FieldValueMapping pFVM) {
    super(pEvaluatedNodeInfo, pFieldSet, pFieldSelectConfig, pFieldId, pFVM);

    //Resolve selector path attribute, which may be a wildcard ("*"), into a deterministic simple path
    //This needs to be done so the FieldInfo knows what elements to create.
    String lSelectorPath = pEvaluatedNodeInfo.getStringAttribute(NodeAttribute.SELECTOR);
    DOM lCurrentModelDOM = pEvaluatedNodeInfo.getNodeInfo().getModelDOMElem();
    DOM lSelectorModelDOM;
    try {
      lSelectorModelDOM = lCurrentModelDOM.get1E(lSelectorPath);
    }
    catch (ExCardinality e) {
      throw new ExInternal("Selector path " + lSelectorPath + " failed to locate target in schema for node " + pEvaluatedNodeInfo.getIdentityInformation(), e);
    }

    mSelectorPath = lCurrentModelDOM.getRelativeDownToOrNull(lSelectorModelDOM);
    DOMList lChildElements = getValueDOM().getUL(mSelectorPath);

    mSelectedFVMOptionRefs = new ArrayList<>(lChildElements.size());
    for(DOM lChildElement : lChildElements) {
      String lRef = mFVM.getFVMOptionRefForItem(this, lChildElement);
      mSelectedFVMOptionRefs.add(lRef);

      //If unrecognised, create the entry now so we don't have to work it out later
      if(lRef == null) {
        String lUnrecognisedDisplayKey = XFUtil.nvl(getEvaluatedNodeInfoItem().getStringAttribute(NodeAttribute.KEY_UNRECOGNISED), lChildElement.value());
        FieldSelectOption lUnrecognisedOption = mFVM.createFieldSelectOption(lUnrecognisedDisplayKey, true, false, getExternalValueForUnrecognisedEntry(lChildElement));
        mUnrecognisedOptions.add(lUnrecognisedOption);
        mUnrecognisedSentStrings.add(getSentValueForUnrecognisedEntry(lChildElement));
      }
    }
  }

  @Override
  protected boolean isNull() {
    //TODO check this logic is ok - unrecognised? (will be > 0 for unrecognised as will contain null entries)
    return mSelectedFVMOptionRefs.size() == 0;
  }

  @Override
  public boolean isRecognisedOptionSelected() {
    return mUnrecognisedOptions.size() == 0 && mSelectedFVMOptionRefs.size() != 0;
  }

  @Override
  public FieldInfo createFieldInfoOrNull() {

    Set<String> lSendingValues;
    if(isNull()) {
      lSendingValues = Collections.singleton(FieldValueMapping.NULL_VALUE);
    }
    else {
      lSendingValues = new HashSet<>();
      for(String lSelectedRef : mSelectedFVMOptionRefs) {
        //Record the sending of a recognised item (unrecognised dealt with seperately below)
        if(lSelectedRef != null) {
          lSendingValues.add(lSelectedRef);
        }
      }
    }

    //Augment unrecognised values into the sent set
    lSendingValues.addAll(mUnrecognisedSentStrings);

    return new MultiOptionFieldInfo(getExternalFieldName(), getValueDOM().getRef(), getEvaluatedNodeInfoItem().getChangeActionName(), lSendingValues, mFVM, mSelectorPath);
  }

  @Override
  public List<FieldSelectOption> getSelectOptions() {

    List<FieldSelectOption> lSelectOptions = mFVM.getSelectOptions(this, new HashSet<>(mSelectedFVMOptionRefs));

    boolean lSuppressUnselected = getEvaluatedNodeInfoItem().getBooleanAttribute(NodeAttribute.SUPPRESS_UNSELECTED_OPTIONS,false) && getEvaluatedNodeInfoItem().getVisibility().asInt() < NodeVisibility.EDIT.asInt();

    //Remove unselected options if required
    if(lSuppressUnselected) {
      Iterator<FieldSelectOption> lOptionIterator = lSelectOptions.iterator();
      while(lOptionIterator.hasNext()) {
        FieldSelectOption lOption = lOptionIterator.next();
        if(!lOption.isSelected()) {
          lOptionIterator.remove();
        }
      }
    }

    //if null augment null entry
    if(!lSuppressUnselected || isNull()) {
      augmentNullKeyIntoList(lSelectOptions, getEvaluatedNodeInfoItem());
    }

    //augment pre-computed unrecognised entries
    lSelectOptions.addAll(mUnrecognisedOptions);

    return lSelectOptions;
  }

  @Override
  protected boolean isFVMOptionRefSelected(String pRef) {
    return mSelectedFVMOptionRefs.contains(pRef);
  }
}
