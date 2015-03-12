package net.foxopen.fox.module.fieldset.fieldmgr;

import net.foxopen.fox.module.datanode.EvaluatedNodeInfoItem;
import net.foxopen.fox.module.fieldset.FieldSet;
import net.foxopen.fox.module.fieldset.fieldinfo.FieldInfo;
import net.foxopen.fox.module.fieldset.fieldinfo.TextValueFieldInfo;
import net.foxopen.fox.module.fieldset.fvm.FieldSelectOption;
import net.foxopen.fox.module.fieldset.transformer.FieldTransformer;
import net.foxopen.fox.module.fieldset.transformer.FieldTransformerType;

import java.util.List;


public class TextValueFieldMgr
extends DataFieldMgr {

  private final FieldTransformer mFieldTransformer;

  TextValueFieldMgr(EvaluatedNodeInfoItem pEvaluatedNodeInfo, FieldSet pFieldSet, String pFieldId) {
    super(pEvaluatedNodeInfo, pFieldSet, pFieldId);
    mFieldTransformer = FieldTransformerType.getTransformerForNode(pEvaluatedNodeInfo);
  }

  protected TextValueFieldMgr(EvaluatedNodeInfoItem pEvaluatedNodeInfo, FieldSet pFieldSet, String pFieldId, FieldTransformer pFieldTransformer) {
    super(pEvaluatedNodeInfo, pFieldSet, pFieldId);
    mFieldTransformer = pFieldTransformer;
  }

  @Override
  public String getSingleTextValue() {
    return mFieldTransformer.applyOutboundTransform(getValueDOM());
  }

  @Override
  public String getExternalFieldName() {
    return getFieldId();
  }

  protected FieldInfo createFieldInfo(String pDOMValue) {
    return new TextValueFieldInfo(getExternalFieldName(), getValueDOM().getRef(), pDOMValue, getEvaluatedNodeInfoItem().getChangeActionName(), mFieldTransformer);
  }

  @Override
  public FieldInfo createFieldInfoOrNull() {
    //PN changed 19/5/14 - added trim() to mitigate oracle binary xml deserialiser adding extra whitespace to the DOM
    return createFieldInfo(getValueDOM().value().trim());
  }

  protected final FieldTransformer getFieldTransformer() {
    return mFieldTransformer;
  }

  @Override
  public List<FieldSelectOption> getSelectOptions() {
    return null;
  }
}
