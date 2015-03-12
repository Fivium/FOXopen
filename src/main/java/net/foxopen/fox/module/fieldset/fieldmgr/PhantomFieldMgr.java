package net.foxopen.fox.module.fieldset.fieldmgr;

import net.foxopen.fox.module.datanode.EvaluatedNode;
import net.foxopen.fox.module.fieldset.FieldSet;
import net.foxopen.fox.module.fieldset.fieldinfo.FieldInfo;
import net.foxopen.fox.module.fieldset.fvm.FieldSelectOption;
import net.foxopen.fox.track.Track;

import java.util.Collections;
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
    //Don't blow up here, there may be a phantom data xpath with an invalid target
    Track.alert("PhantomGetSelectOptions","Cannot return select options for a phantom - check phantom-data-xpath definition on " + mEvaluatedNode.getIdentityInformation());
    return Collections.emptyList();
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
