package net.foxopen.fox.module.fieldset.fieldmgr;

import net.foxopen.fox.XFUtil;
import net.foxopen.fox.dom.DOM;
import net.foxopen.fox.dom.DOMList;
import net.foxopen.fox.ex.ExCardinality;
import net.foxopen.fox.ex.ExInternal;
import net.foxopen.fox.module.datanode.EvaluatedNodeInfo;
import net.foxopen.fox.module.datanode.EvaluatedNodeInfoItem;
import net.foxopen.fox.module.datanode.NodeAttribute;
import net.foxopen.fox.module.datanode.NodeEvaluationContext;
import net.foxopen.fox.module.datanode.NodeInfo;
import net.foxopen.fox.module.evaluatedattributeresult.StringAttributeResult;
import net.foxopen.fox.module.fieldset.FieldSelectConfig;
import net.foxopen.fox.module.fieldset.FieldSet;
import net.foxopen.fox.module.fieldset.fieldinfo.FieldInfo;
import net.foxopen.fox.module.fieldset.fieldinfo.MultiOptionFieldInfo;
import net.foxopen.fox.module.fieldset.fvm.FieldSelectOption;
import net.foxopen.fox.module.fieldset.fvm.FieldValueMapping;
import net.foxopen.fox.module.fieldset.fvm.NullOptionType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


public class MultiOptionFieldMgr
extends OptionFieldMgr {

  /** Currently selected FVMOption refs */
  private final List<String> mSelectedFVMOptionRefs;
  //Small initial capacity for unrecognised options as typically there should not be any
  private final List<FieldSelectOption> mUnrecognisedOptions = new ArrayList<>(1);
  private final Set<String> mUnrecognisedSentStrings = new HashSet<>(1);

  /** Path to the repeating item of this FieldMgr relative to the value DOM */
  private final String mSelectorPath;

  /**
   * Convenience method for getting the <tt>selector</tt> path element's model DOM node (i.e. the repeating item of a multi-selector) for
   * the given ENI. Returns null if the given EvaluatedNodeInfo is not a multi selector.
   *
   * @param pEvaluatedNodeInfo EvaluatedNodeInfo to examine.
   * @return Model DOM node for the repeating item of a multi select node, or null if the node is not a multi selector.
   */
  public static DOM getSelectorModelDOMNodeOrNull(EvaluatedNodeInfo pEvaluatedNodeInfo) {
    return getSelectorModelDOMNodeOrNull(pEvaluatedNodeInfo.getNodeEvaluationContext(), pEvaluatedNodeInfo.getNodeInfo());
  }

  /**
   * Convenience method for getting the <tt>selector</tt> path element's model DOM node (i.e. the repeating item of a multi-selector) for
   * a given parent NodeInfo. Returns null if the given NodeInfo is not a multi selector.
   *
   * @param pNodeEvaluationContext For retrieving <tt>selector</tt> attribute.
   * @param pContainerNodeInfo NodeInfo to examine.
   * @return Model DOM node for the repeating item of a multi select node, or null if the node is not a multi selector.
   */
  public static DOM getSelectorModelDOMNodeOrNull(NodeEvaluationContext pNodeEvaluationContext, NodeInfo pContainerNodeInfo) {

    StringAttributeResult lSelectorPath = pNodeEvaluationContext.getStringAttributeOrNull(NodeAttribute.SELECTOR);
    if(lSelectorPath != null) {
      DOM lCurrentModelDOM = pContainerNodeInfo.getModelDOMElem();
      try {
        return lCurrentModelDOM.get1E(lSelectorPath.getString());
      }
      catch (ExCardinality e) {
        throw new ExInternal("Selector path " + lSelectorPath + " failed to locate target in schema for node " + pContainerNodeInfo.getAbsolutePath(), e);
      }
    }
    else {
      return null;
    }
  }

  public MultiOptionFieldMgr(EvaluatedNodeInfoItem pEvaluatedNodeInfo, FieldSet pFieldSet, FieldSelectConfig pFieldSelectConfig, String pFieldId, FieldValueMapping pFVM) {
    super(pEvaluatedNodeInfo, pFieldSet, pFieldSelectConfig, pFieldId, pFVM);

    //Resolve selector path attribute, which may be a wildcard ("*"), into a deterministic simple path
    //This needs to be done so the FieldInfo knows what elements to create.
    DOM lCurrentModelDOM = pEvaluatedNodeInfo.getNodeInfo().getModelDOMElem();
    DOM lSelectorModelDOM = getSelectorModelDOMNodeOrNull(pEvaluatedNodeInfo);

    mSelectorPath = lCurrentModelDOM.getRelativeDownToOrNull(lSelectorModelDOM);
    DOMList lChildElements = getValueDOM().getUL(mSelectorPath);

    mSelectedFVMOptionRefs = new ArrayList<>(lChildElements.size());
    for(DOM lChildElement : lChildElements) {
      String lRef = mFVM.getFVMOptionRefForItem(this, lChildElement);

      if(lRef == null) {
        //If unrecognised, create the entry now so we don't have to work it out later
        String lUnrecognisedDisplayKey = XFUtil.nvl(getEvaluatedNodeInfoItem().getStringAttribute(NodeAttribute.KEY_UNRECOGNISED), lChildElement.value());
        FieldSelectOption lUnrecognisedOption = mFVM.createFieldSelectOption(lUnrecognisedDisplayKey, true, NullOptionType.NOT_NULL, getExternalValueForUnrecognisedEntry(lChildElement));
        mUnrecognisedOptions.add(lUnrecognisedOption);
        mUnrecognisedSentStrings.add(getSentValueForUnrecognisedEntry(lChildElement));
      }
      else {
        //Recognised option - record the FVM option ref
        mSelectedFVMOptionRefs.add(lRef);
      }
    }
  }

  @Override
  protected boolean isNull() {
    //Test that no recognised OR unrecognised options are selected
    return mSelectedFVMOptionRefs.size() == 0 && mUnrecognisedOptions.size() == 0;
  }

  @Override
  public boolean isRecognisedNotNullOptionSelected() {
    return mSelectedFVMOptionRefs.size() > 0;
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
        lSendingValues.add(lSelectedRef);
      }
    }

    //Augment unrecognised values into the sent set
    lSendingValues.addAll(mUnrecognisedSentStrings);

    return new MultiOptionFieldInfo(getExternalFieldName(), getValueDOM().getRef(), getEvaluatedNodeInfoItem().getChangeActionName(), lSendingValues, mFVM, mSelectorPath);
  }

  @Override
  public List<FieldSelectOption> getSelectOptions() {

    List<FieldSelectOption> lSelectOptions = mFVM.getSelectOptions(this, new HashSet<>(mSelectedFVMOptionRefs));

    //Remove unselected options if required
    if (isSuppressUnselected()) {
      removeUnselectedOptions(lSelectOptions);
    }

    //if null augment null entry
    if(!isSuppressUnselected() || isNull()) {
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
