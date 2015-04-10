package net.foxopen.fox.module.fieldset.fieldmgr;

import net.foxopen.fox.XFUtil;
import net.foxopen.fox.module.fieldset.FieldSet;
import net.foxopen.fox.module.fieldset.clientaction.DeleteUploadedFileClientAction;
import net.foxopen.fox.module.fieldset.fieldinfo.FieldInfo;
import net.foxopen.fox.module.fieldset.fvm.FieldSelectOption;
import net.foxopen.fox.module.datanode.EvaluatedNodeInfo;
import net.foxopen.fox.module.datanode.EvaluatedNodeInfoFileItem;
import net.foxopen.fox.module.datanode.NodeVisibility;

import java.util.List;


public class UploadFieldMgr extends FieldMgr {

  private final EvaluatedNodeInfoFileItem mEvaluatedNode;

  public UploadFieldMgr(EvaluatedNodeInfo pEvaluatedNode, FieldSet pFieldSet, String pFieldId) {
    super(pEvaluatedNode, pFieldSet, pFieldId);
    mEvaluatedNode = (EvaluatedNodeInfoFileItem) pEvaluatedNode;
  }

  @Override
  public String getExternalFieldName() {
    return getFieldId();
  }

  @Override
  public List<FieldSelectOption> getSelectOptions() {
    return null;
  }

  @Override
  public String getSingleTextValue() {
    throw new UnsupportedOperationException("An UploadFieldMgr cannot provide a single text value.");
  }

  @Override
  public FieldInfo createFieldInfoOrNull() {
    return null;
  }

  @Override
  public boolean isRunnable() {
    return false;
  }

  @Override
  public void prepareForSetOut() {

    if(super.getVisibility() == NodeVisibility.EDIT) {
      String lItemContextRef = mEvaluatedNode.getDataItem().getRef();

      //Register the upload on the fieldset so it can be validated when the upload starts
      boolean lAddAllowed = super.getOwningFieldSet().addUploadTarget(lItemContextRef);
      if(!lAddAllowed) {
        //If add wasn't allowed, knock this upload field down to RO (only one editable instance allowed per page)
        super.setVisibility(NodeVisibility.VIEW);
      }

      //Register success/fail actions as runnable actions for modal uploads
      String lSuccessAction = mEvaluatedNode.getSuccessAction();
      if(!XFUtil.isNull(lSuccessAction)) {
        super.getOwningFieldSet().registerExternalRunnableAction(lSuccessAction, lItemContextRef);
      }

      String lFailAction = mEvaluatedNode.getFailAction();
      if(!XFUtil.isNull(lFailAction)) {
        super.getOwningFieldSet().registerExternalRunnableAction(lFailAction, lItemContextRef);
      }

      //Register deletion client action
      super.getOwningFieldSet().registerClientAction(new DeleteUploadedFileClientAction(lItemContextRef, mEvaluatedNode.getMaxFilesAllowed() > 1));
    }
  }

  protected EvaluatedNodeInfoFileItem getEvaluatedNode() {
    return mEvaluatedNode;
  }
}
