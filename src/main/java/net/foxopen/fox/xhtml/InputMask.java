package net.foxopen.fox.xhtml;

import net.foxopen.fox.ex.ExInternal;

import java.util.HashMap;
import java.util.Map;

@Deprecated
public abstract class InputMask {

  protected String mFieldSetMaskName = null;

  private static final Map mMaskNameToInputMask;

  public final static InputMask INPUT_MASK_COMMAS = new InputMaskCommas();

  static {
    mMaskNameToInputMask = new HashMap();
    mMaskNameToInputMask.put("commas", INPUT_MASK_COMMAS);
  }

  public static final InputMask getInputMaskByName(final String pName) {
    if(pName==null) {
      return null;
    } else if(!mMaskNameToInputMask.containsKey(pName)) {
      throw new ExInternal("Input mask '"+pName+"' not found")  ;
    }
    return (InputMask)mMaskNameToInputMask.get(pName);
  }

  public abstract String applyMask(String pTextValue);
  public abstract String removeMask(String pMaskedValue);

  public final String getFieldSetMaskName() {
    return mFieldSetMaskName;
  }

}
