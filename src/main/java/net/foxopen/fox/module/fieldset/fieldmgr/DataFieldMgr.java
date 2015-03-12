package net.foxopen.fox.module.fieldset.fieldmgr;

import net.foxopen.fox.dom.DOM;
import net.foxopen.fox.module.datanode.EvaluatedNodeInfoItem;
import net.foxopen.fox.module.datanode.NodeVisibility;
import net.foxopen.fox.module.fieldset.FieldSet;


public abstract class DataFieldMgr
extends FieldMgr {

  private final EvaluatedNodeInfoItem mEvaluatedNodeInfoItem;
  private final DOM mValueDOM;

  protected DataFieldMgr(EvaluatedNodeInfoItem pEvaluatedNodeInfoItem, FieldSet pFieldSet, String pFieldId) {
    super(pEvaluatedNodeInfoItem, pFieldSet, pFieldId);
    mEvaluatedNodeInfoItem = pEvaluatedNodeInfoItem;
    mValueDOM = mEvaluatedNodeInfoItem.getDataItem();
  }

  protected DOM getValueDOM() {
    return mValueDOM;
  }

  public EvaluatedNodeInfoItem getEvaluatedNodeInfoItem() {
    return mEvaluatedNodeInfoItem;
  }

  @Override
  public void prepareForSetOut() {
    if(super.getVisibility() == NodeVisibility.EDIT) {
      boolean lAddAllowed = super.getOwningFieldSet().addFieldInfo(createFieldInfoOrNull(), mValueDOM, mEvaluatedNodeInfoItem.getContextUElem());
      if(!lAddAllowed) {
        super.setVisibility(NodeVisibility.VIEW);
      }
    }

    if(isRunnable()) {
      super.getOwningFieldSet().registerExternalRunnableAction(mEvaluatedNodeInfoItem);
    }
  }

  @Override
  public boolean isRunnable() {
    return mEvaluatedNodeInfoItem.isRunnable();
  }
}
