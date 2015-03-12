package net.foxopen.fox.module.fieldset.fieldmgr;

import java.util.List;

import net.foxopen.fox.module.fieldset.FieldSet;
import net.foxopen.fox.module.fieldset.fieldinfo.FieldInfo;
import net.foxopen.fox.module.fieldset.fvm.FieldSelectOption;
import net.foxopen.fox.module.datanode.EvaluatedNode;


public class ActionFieldMgr extends FieldMgr {
  private final EvaluatedNode mEvaluatedNode;

  ActionFieldMgr(EvaluatedNode pEvaluatedNode, FieldSet pFieldSet, String pFieldId) {
    super(pEvaluatedNode, pFieldSet, pFieldId);
    mEvaluatedNode = pEvaluatedNode;
  }

  @Override
  public void prepareForSetOut() {
    if(isRunnable()) {
      super.getOwningFieldSet().registerExternalRunnableAction(mEvaluatedNode);
    }
  }

  @Override
  public boolean isRunnable() {
    return mEvaluatedNode.isRunnable();
  }

  @Override
  public String getExternalFieldName() {
    return getFieldId();
  }

  @Override
  public List<FieldSelectOption> getSelectOptions() {
    throw new UnsupportedOperationException(getClass().getName() + " cannot provide Select Options - only applicable to data items");
  }

  @Override
  public String getSingleTextValue() {
    throw new UnsupportedOperationException(getClass().getName() + " cannot provide a Text Value - only applicable to data items");
  }

  @Override
  public FieldInfo createFieldInfoOrNull() {
    throw new UnsupportedOperationException(getClass().getName() + " cannot provide Field Info - only applicable to data items");
  }
}
