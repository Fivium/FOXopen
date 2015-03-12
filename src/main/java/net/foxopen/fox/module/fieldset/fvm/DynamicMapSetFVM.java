package net.foxopen.fox.module.fieldset.fvm;

import net.foxopen.fox.dom.DOM;
import net.foxopen.fox.thread.ActionRequestContext;

import java.util.List;

/**
 * A MapSetFVM which stores references to all the FVMOptions provided by the mapset. This is required for dynamic mapsets
 * which may change their content between churns. The FieldSet requires an exact copy of the mapset as it appeared when it
 * was sent to the user, as data application is based on item indexes which can change within a dynamic mapset.<br><br>
 *
 * For example: Say the page is sent to the user containing options "1", "2" and "3". When the user posts the page, the
 * dynamic mapset is re-evaluated and the latest version only contains options "1" and "3". If the user selected option "2"
 * on their page, option "3" will actually be selected. By storing the options as they were sent out, this risk is mitigated.
 * This incurs an overhead in serialising additional mapset data which developers should be aware of, and minimise their use
 * of dynamic mapsets accordingly.
 */
public class DynamicMapSetFVM
extends MapSetFVM {

  /** List of FVM options - should only be populated for dynamic mapsets. */
  private final List<FVMOption> mFVMOptionList;

  protected DynamicMapSetFVM(String pMapSetName, DOM pMapSetAttach, List<FVMOption> pFVMOptionList) {
    super(pMapSetName, pMapSetAttach);
    mFVMOptionList = pFVMOptionList;
  }

  @Override
  public List<FVMOption> getFVMOptionList(ActionRequestContext pRequestContext, DOM pTargetDOM) {
    return mFVMOptionList;
  }

}
