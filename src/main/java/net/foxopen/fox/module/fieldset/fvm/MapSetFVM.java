package net.foxopen.fox.module.fieldset.fvm;

import net.foxopen.fox.XFUtil;
import net.foxopen.fox.dom.DOM;
import net.foxopen.fox.ex.ExInternal;
import net.foxopen.fox.module.datanode.EvaluatedNodeInfoItem;
import net.foxopen.fox.module.datanode.NodeAttribute;
import net.foxopen.fox.module.evaluatedattributeresult.DOMAttributeResult;
import net.foxopen.fox.module.fieldset.fieldmgr.DataFieldMgr;
import net.foxopen.fox.module.fieldset.fieldmgr.OptionFieldMgr;
import net.foxopen.fox.module.mapset.MapSet;
import net.foxopen.fox.module.mapset.MapSetEntry;
import net.foxopen.fox.thread.ActionRequestContext;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;


/**
 * FieldValueMapping for a MapSet. For static MapSets it is sufficient to just store the MapSet name and attach point -
 * the MapSet can be retrieved at FieldSet apply time to determine the actual option selected. For dynamic MapSets it is
 * necessary to store all the sent values in case the MapSet has changed by the time the form is posted back - see the
 * DynamicMapSetFVM subclass.
 *
 * Note: FVMOption refs for this class are integer values corresponding to the 0-based index of the option within the mapset.
 */
public class MapSetFVM
extends FieldValueMapping {

  /** Name of the mapset being used to provide the options for this FVM. */
  private final String mMapSetName;
  /** Optional FOXID of the map-set-attach element, if one was specified. */
  private final String mMapSetAttachRef;

  /**
   * Hard reference to the mapset attach DOM. Cached here for performance. Will be null if no map-set-attach was specified
   * or if this FVM has been deserialised.
   * Important: do NOT allow this to be serialised. Instead the DOM ref is serialised and can be used to re-resolve the node JIT.
   */
  private transient final DOM mMapSetAttach;

  /**
   * Tests that the given ENI contains a valid mapset definition which can be used to construct a mapset FVM.
   * @param pEvaluatedNodeInfoItem
   * @return
   */
  public static boolean validateENI(EvaluatedNodeInfoItem pEvaluatedNodeInfoItem) {
    return pEvaluatedNodeInfoItem.getMapSet() != null;
  }

  public static MapSetFVM createMapSetFVM(EvaluatedNodeInfoItem pEvaluatedNodeInfoItem) {

    MapSet lMapSet = pEvaluatedNodeInfoItem.getMapSet();
    //Validate that a mapset definition is available
    if(lMapSet == null) {
      throw new ExInternal("Cannot create MapSetFVM for " + pEvaluatedNodeInfoItem.getIdentityInformation() + " as no map-set definition could be found");
    }

    String lMsName = lMapSet.getMapSetName();
    DOM lMsAttach = null;
    DOMAttributeResult lMapsetAttachDOMResult = pEvaluatedNodeInfoItem.getDOMAttributeOrNull(NodeAttribute.MAPSET_ATTACH);
    if (lMapsetAttachDOMResult != null) {
      lMsAttach = lMapsetAttachDOMResult.getDOM();
    }

    if(lMapSet.isDynamic()) {
      return new DynamicMapSetFVM(lMsName, lMsAttach, lMapSet.getFVMOptionList());
    }
    else {
      return new MapSetFVM(lMsName, lMsAttach);
    }

  }

  protected MapSetFVM(String pMapSetName, DOM pMapSetAttach) {
    mMapSetName = pMapSetName;
    mMapSetAttach = pMapSetAttach;
    mMapSetAttachRef = pMapSetAttach != null ? pMapSetAttach.getFoxId() : null;
  }

  @Override
  public List<FieldSelectOption> getSelectOptions(OptionFieldMgr pFieldMgr, Set<String> pSelectedRefs) {

    EvaluatedNodeInfoItem lEvaluatedNodeInfo = pFieldMgr.getEvaluatedNodeInfoItem();
    List<FieldSelectOption> lOptionList = new ArrayList<>();

    int i = 0;
    for(MapSetEntry lMapSetItem : lEvaluatedNodeInfo.getMapSet().getEntryList()) {
      String lIdxAsString = Integer.toString(i);
      lOptionList.add(new MapSetSelectOption(lMapSetItem, pSelectedRefs.contains(lIdxAsString), pFieldMgr.getExternalValueForOptionRef(lIdxAsString)));
      i++;
    }

    return lOptionList;
  }

  @Override
  public String getFVMOptionRefForItem(DataFieldMgr pFieldMgr, DOM pItemDOM) {
    int lMSIndex = pFieldMgr.getEvaluatedNodeInfoItem().getMapSet().indexOf(pItemDOM);
    if(lMSIndex == -1) {
      return null;
    }
    else {
      return Integer.toString(lMSIndex);
    }
  }

  @Override
  public FVMOption getFVMOptionForRef(ActionRequestContext pRequestContext, DOM pTargetDOM, String pRef) {

    //Attempt to resolve the original map-set-attach node
    DOM lMapsetAttach = null;
    if(mMapSetAttach != null) {
      //If a direct DOM ref is already available on this object, use it
      lMapsetAttach = mMapSetAttach;
    }
    else if(!XFUtil.isNull(mMapSetAttachRef)) {
      //If a FOXID is known use it to re-resolve the attach DOM
      lMapsetAttach = pRequestContext.getContextUElem().getElemByRef(mMapSetAttachRef);
    }

    MapSet lMapSet = pRequestContext.resolveMapSet(mMapSetName, pTargetDOM, lMapsetAttach);
    return lMapSet.getFVMOptionList().get(Integer.valueOf(pRef));
  }
}
