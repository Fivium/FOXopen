package net.foxopen.fox.module.fieldset.fvm;

import net.foxopen.fox.XFUtil;
import net.foxopen.fox.dom.DOM;
import net.foxopen.fox.ex.ExInternal;
import net.foxopen.fox.ex.ExTooFew;
import net.foxopen.fox.ex.ExTooMany;
import net.foxopen.fox.module.mapset.AJAXQueryDefinition;
import net.foxopen.fox.module.mapset.JITMapSet;
import net.foxopen.fox.module.mapset.MapSet;
import net.foxopen.fox.thread.ActionRequestContext;

/**
 * FVMOption for applying a simple string value to a DOM. This is used by simple mapsets and other FVM types (booleans,
 * schema enums, etc).
 */
public class JITFVMOption
implements FVMOption {
  private final JITMapSet mMapSet;
  private final String mRef;

  public JITFVMOption(MapSet pMapSet, String pRef) {
    mMapSet = (JITMapSet)pMapSet;
    mRef = pRef;
  }

  @Override
  public void applyToNode(ActionRequestContext pRequestContext, DOM pTargetNode) {
    // Look up on mapset def and get data for ref
    mMapSet.applyDataToNode(pRequestContext, mRef, pTargetNode);
  }

  @Override
  public boolean isEqualTo(DOM pTargetNode) {
    String lRefPath = ((AJAXQueryDefinition)mMapSet.getMapSetDefinition()).getRefPath();
    if (XFUtil.isNull(lRefPath)) {
      return pTargetNode.value().equals(mRef);
    }
    else {
      try {
        return pTargetNode.get1S(lRefPath).equals(mRef);
      }
      catch (ExTooFew | ExTooMany e) {
        throw new ExInternal("Failed to find a ref string in mapset data value using ref-path: " + lRefPath, e);
      }
    }
  }
}
