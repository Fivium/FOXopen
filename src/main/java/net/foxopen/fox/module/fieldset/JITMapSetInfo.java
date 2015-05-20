package net.foxopen.fox.module.fieldset;

import net.foxopen.fox.ex.ExInternal;
import net.foxopen.fox.module.datanode.EvaluatedNodeInfoItem;
import net.foxopen.fox.module.mapset.JITMapSet;
import net.foxopen.fox.module.mapset.MapSet;

public class JITMapSetInfo {
  private final String mJITMapSetName;
  private final String mDomRef;

  public JITMapSetInfo(EvaluatedNodeInfoItem pEvaluatedNodeInfoItem) {
    MapSet lMapSet = pEvaluatedNodeInfoItem.getMapSet();
    if (lMapSet instanceof JITMapSet) {
      mJITMapSetName = lMapSet.getMapSetName();
      mDomRef = pEvaluatedNodeInfoItem.getDataItem().getRef();
    }
    else {
      throw new ExInternal("Cannot create a JITMapSetInfo as MapSet defined is not a JITMapSet: " + pEvaluatedNodeInfoItem.getIdentityInformation());
    }
  }

  public String getJITMapSetName() {
    return mJITMapSetName;
  }

  public String getDOMRef() {
    return mDomRef;
  }
}
