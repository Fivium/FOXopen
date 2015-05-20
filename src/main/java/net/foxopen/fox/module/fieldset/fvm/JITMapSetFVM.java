package net.foxopen.fox.module.fieldset.fvm;

import net.foxopen.fox.XFUtil;
import net.foxopen.fox.dom.DOM;
import net.foxopen.fox.ex.ExInternal;
import net.foxopen.fox.ex.ExTooFew;
import net.foxopen.fox.ex.ExTooMany;
import net.foxopen.fox.module.datanode.EvaluatedNodeInfoItem;
import net.foxopen.fox.module.datanode.NodeAttribute;
import net.foxopen.fox.module.evaluatedattributeresult.DOMAttributeResult;
import net.foxopen.fox.module.fieldset.fieldmgr.DataFieldMgr;
import net.foxopen.fox.module.fieldset.fieldmgr.OptionFieldMgr;
import net.foxopen.fox.module.mapset.AJAXQueryDefinition;
import net.foxopen.fox.module.mapset.JITMapSet;
import net.foxopen.fox.module.mapset.MapSet;
import net.foxopen.fox.module.mapset.MapSetEntry;
import net.foxopen.fox.thread.ActionRequestContext;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * FieldValueMapping for a JITMapSet where the MapSet itself stores no values and queries the keys and data in just in
 * time.
 */
public class JITMapSetFVM
extends FieldValueMapping {

  /**
   * Hard reference to the MapSet. Cached here for performance. Will be null if this FVM has been deserialised.
   * Important: do NOT allow this to be serialised. Instead the MapSet name is serialised and can be used to re-resolve
   * the MapSet object JIT.
   */
  transient private final MapSet mMapSet;

  /** Store the MapSet and its name so the MapSet can be retrieved at FieldSet apply time */
  private final String mMapSetName;

  /**
   * Tests that the given ENI contains a JitMapSet which can be used to construct this JITMapSet FVM.
   *
   * @param pEvaluatedNodeInfoItem
   * @return true if pEvaluatedNodeInfoItem has a mapset defined and it is a JITMapSet
   */
  public static boolean validateENI(EvaluatedNodeInfoItem pEvaluatedNodeInfoItem) {
    return pEvaluatedNodeInfoItem.getMapSet() != null && pEvaluatedNodeInfoItem.getMapSet() instanceof JITMapSet;
  }

  public static JITMapSetFVM createMapSetFVM(EvaluatedNodeInfoItem pEvaluatedNodeInfoItem) {
    MapSet lMapSet = pEvaluatedNodeInfoItem.getMapSet();
    //Validate that a mapset definition is available
    if(!validateENI(pEvaluatedNodeInfoItem)) {
      throw new ExInternal("Cannot create JITMapSetFVM for " + pEvaluatedNodeInfoItem.getIdentityInformation() + " as no valid map-set could be found");
    }

    DOM lMsAttach = null;
    DOMAttributeResult lMapsetAttachDOMResult = pEvaluatedNodeInfoItem.getDOMAttributeOrNull(NodeAttribute.MAPSET_ATTACH);
    if (lMapsetAttachDOMResult != null) {
      lMsAttach = lMapsetAttachDOMResult.getDOM();
    }

    return new JITMapSetFVM(lMapSet, lMsAttach);
  }

  protected JITMapSetFVM(MapSet pMapSet, DOM pMapSetAttach) {
    mMapSet = pMapSet;
    mMapSetName = pMapSet.getMapSetName();
    // TODO - AJMS - No mapset attach yet
  }

  @Override
  public List<FieldSelectOption> getSelectOptions(OptionFieldMgr pFieldMgr, Set<String> pSelectedRefs) {
    List<FieldSelectOption> lOptionList = new ArrayList<>();

    for (String lRef : pSelectedRefs) {
      ActionRequestContext lRequestContext = pFieldMgr.getEvaluatedNodeInfoItem().getNodeEvaluationContext().getEvaluatedParseTree().getRequestContext();
      MapSetEntry lMapSetEntry = MapSetEntry.createFromMapSetKey(mMapSet.getKeyForDataString(lRequestContext, pFieldMgr.getEvaluatedNodeInfoItem().getDataItem(), lRef));
      lOptionList.add(new MapSetSelectOption(lMapSetEntry, true, lRef));
    }

    return lOptionList;
  }

  @Override
  public String getFVMOptionRefForItem(DataFieldMgr pFieldMgr, DOM pItemDOM) {
    // Do val or path on definition in complex
    AJAXQueryDefinition lMapSetDefinition = (AJAXQueryDefinition)mMapSet.getMapSetDefinition();
    if (XFUtil.isNull(lMapSetDefinition.getRefPath())) {
      return pItemDOM.value();
    }
    else {
      try {
        return pItemDOM.get1S(lMapSetDefinition.getRefPath());
      }
      catch (ExTooFew | ExTooMany e) {
        throw new ExInternal("Failed to find a ref string in mapset data value using ref-path: " + lMapSetDefinition.getRefPath(), e);
      }
    }
}

  @Override
  public FVMOption getFVMOptionForRef(ActionRequestContext pRequestContext, DOM pTargetDOM, String pRef) {
    // TODO - AJMS - No mapset attach yet
    return new JITFVMOption(getMapSet(pRequestContext, pTargetDOM, null), pRef);
  }

  /**
   * When this object is de-serialised the transient mMapSet will be null and the code needs to re-resolve it from the
   * non-transient mMapSetName
   *
   * @param pRequestContext Context to resolve the MapSet from
   * @param pTargetDOM MapSet Item DOM
   * @param pMapSetAttach MapSet Attach DOM
   * @return MapSet object re-resolved from mMapSetName
   */
  private MapSet getMapSet(ActionRequestContext pRequestContext, DOM pTargetDOM, DOM pMapSetAttach) {
    if (mMapSet != null) {
      return mMapSet;
    }

    return pRequestContext.resolveMapSet(mMapSetName, pTargetDOM, pMapSetAttach);
  }
}
