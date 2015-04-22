package net.foxopen.fox.module.fieldset.fieldmgr;

import net.foxopen.fox.module.datanode.EvaluatedNode;
import net.foxopen.fox.module.fieldset.FieldSet;
import net.foxopen.fox.module.fieldset.fieldinfo.FieldInfo;
import net.foxopen.fox.module.fieldset.fvm.FieldSelectOption;

import java.util.List;

/**
 * FieldMgr to be used by phantoms which have their own Evaluated Node Info objects, such as phantom buffers and phantom menus
 */
public class PhantomFieldMgr extends FieldMgr {
  private final EvaluatedNode mEvaluatedNode;

  PhantomFieldMgr(EvaluatedNode pEvaluatedNode, FieldSet pFieldSet, String pFieldId) {
    super(pEvaluatedNode, pFieldSet, pFieldId);
    mEvaluatedNode = pEvaluatedNode;
  }

  @Override
  public void prepareForSetOut() {
    // Do nothing
  }

  @Override
  public boolean isRunnable() {
    return false;
  }

  @Override
  public String getExternalFieldName() {
    return getFieldId();
  }

  @Override
  public List<FieldSelectOption> getSelectOptions() {
    throw new UnsupportedOperationException(getClass().getName() + " cannot provide a select options for node " + mEvaluatedNode.getIdentityInformation() + " - only applicable to data items");
  }

  @Override
  public String getSingleTextValue() {
    throw new UnsupportedOperationException(getClass().getName() + " cannot provide a Text Value for node " + mEvaluatedNode.getIdentityInformation() + " - only applicable to data items");
  }

  @Override
  public FieldInfo createFieldInfoOrNull() {
    throw new UnsupportedOperationException(getClass().getName() + " cannot provide Field Info for node " + mEvaluatedNode.getIdentityInformation() + " - only applicable to data items");
  }
}
