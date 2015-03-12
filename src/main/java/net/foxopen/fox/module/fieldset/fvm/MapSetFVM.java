package net.foxopen.fox.module.fieldset.fvm;

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
 */
public class MapSetFVM
extends FieldValueMapping {

  private final String mMapSetName;
  private final DOM mMapSetAttach;

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
  }

  @Override
  public List<FieldSelectOption> getSelectOptions(OptionFieldMgr pFieldMgr, Set<Integer> pSelectedIndexes) {

    EvaluatedNodeInfoItem lEvaluatedNodeInfo = pFieldMgr.getEvaluatedNodeInfoItem();
    List<FieldSelectOption> lOptionList = new ArrayList<>();

    int i = 0;
    for(MapSetEntry lMapSetItem : lEvaluatedNodeInfo.getMapSet().getEntryList()) {
      lOptionList.add(new MapSetSelectOption(lMapSetItem, pSelectedIndexes.contains(i), pFieldMgr.getExternalValueForOption(i)));
      i++;
    }

    return lOptionList;
  }

  @Override
  public int getIndexForItem(DataFieldMgr pFieldMgr, DOM pItemDOM) {
    return pFieldMgr.getEvaluatedNodeInfoItem().getMapSet().indexOf(pItemDOM);
  }

  @Override
  public List<FVMOption> getFVMOptionList(ActionRequestContext pRequestContext, DOM pTargetDOM) {
    MapSet lMapSet = pRequestContext.resolveMapSet(mMapSetName, pTargetDOM, mMapSetAttach);
    return lMapSet.getFVMOptionList();
  }
}
