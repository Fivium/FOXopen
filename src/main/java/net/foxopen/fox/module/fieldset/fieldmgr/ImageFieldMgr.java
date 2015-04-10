package net.foxopen.fox.module.fieldset.fieldmgr;

import net.foxopen.fox.module.datanode.EvaluatedNodeInfo;
import net.foxopen.fox.module.fieldset.FieldSet;

/**
 * FieldMgr for an image widget. Currently treated like a read-only upload. This widget type is runnable if it has an
 * action defined on it, so the prepareForSetOut behaviour is overridden.
 */
public class ImageFieldMgr
extends UploadFieldMgr {

  ImageFieldMgr(EvaluatedNodeInfo pEvaluatedNode, FieldSet pFieldSet, String pFieldId) {
    super(pEvaluatedNode, pFieldSet, pFieldId);
  }

  @Override
  public boolean isRunnable() {
    return getEvaluatedNode().isRunnable();
  }

  @Override
  public void prepareForSetOut() {
    if(isRunnable()) {
      super.getOwningFieldSet().registerExternalRunnableAction(getEvaluatedNode());
    }
  }
}
