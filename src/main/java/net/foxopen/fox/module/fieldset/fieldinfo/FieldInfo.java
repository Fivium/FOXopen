package net.foxopen.fox.module.fieldset.fieldinfo;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import net.foxopen.fox.XFUtil;
import net.foxopen.fox.dom.DOM;
import net.foxopen.fox.module.ChangeActionContext;
import net.foxopen.fox.module.fieldset.PostedValueProcessor;
import net.foxopen.fox.thread.ActionRequestContext;


/**
 * A FieldInfo contains the information used to apply a posted value to a target DOM, including information about any
 * inbound transformations which need to be applied. FieldInfos are stored in the FieldSet and are serialised at the end
 * of every churn. A FieldInfo should be simple to serialise and be able to apply changes to a DOM <i>as they would have
 * been applied immediately after the value was sent</i>. This means any transformations based on dynamic data (i.e. mapsets)
 * must save the relevant state of the dynamic data at send time. Furthermore, posted values should only be applied if the
 * user has modified a value, so the originally sent value must also be stored for comparison purposes. This means any changes
 * to the DOM which occur while the user is dealing with the page will be preserved unless the user makes a conflicting change.
 */
public abstract class FieldInfo
implements PostedValueProcessor {

  /** HTML name for this FieldInfo */
  private final String mExternalName;

  private final String mDOMRef;

  private final String mChangeActionName;

  protected FieldInfo(String pExternalName, String pDOMRef, String pChangeActionName) {
    mExternalName = pExternalName;
    mDOMRef = pDOMRef;
    mChangeActionName = pChangeActionName;
  }

  @Override
  public String getExternalName() {
    return mExternalName;
  }

  protected String getDOMRef() {
    return mDOMRef;
  }

  /**
   * Returns null of no change action defined.
   * @param pItemParentDOM
   * @return
   */
  private final ChangeActionContext getChangeActionContext(DOM pItemParentDOM) {
    if(!XFUtil.isNull(mChangeActionName)) {
      return new ChangeActionContext(mChangeActionName, pItemParentDOM.getRef(), mDOMRef);
    }
    else {
      return null;
    }
  }

  protected final DOM resolveTargetDOM(ActionRequestContext pRequestContext) {
    DOM lTargetDOM = pRequestContext.getContextUElem().getElemByRef(getDOMRef());
    return lTargetDOM;
  }

  /**
   * Resolves the item DOM for this FieldInfo and clears its children, ready for applying a new value to.
   * @param pRequestContext
   * @return
   */
  protected final DOM resolveAndClearTargetDOM(ActionRequestContext pRequestContext) {
    DOM lTargetDOM = pRequestContext.getContextUElem().getElemByRef(getDOMRef());
    lTargetDOM.removeAllChildren();
    return lTargetDOM;
  }

  protected final List<ChangeActionContext> createChangeActionContext(DOM pItemDOM) {
    ChangeActionContext lChangeActionContext = getChangeActionContext(pItemDOM.getParentOrSelf());
    if(lChangeActionContext != null) {
      return Arrays.asList(lChangeActionContext);
    }
    else {
      return Collections.emptyList();
    }
  }
}
