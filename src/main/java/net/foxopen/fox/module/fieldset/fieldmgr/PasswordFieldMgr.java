package net.foxopen.fox.module.fieldset.fieldmgr;

import net.foxopen.fox.module.datanode.EvaluatedNodeInfoItem;
import net.foxopen.fox.module.fieldset.FieldSet;
import net.foxopen.fox.module.fieldset.fieldinfo.FieldInfo;
import net.foxopen.fox.module.fieldset.transformer.PasswordTransformer;

public class PasswordFieldMgr
extends TextValueFieldMgr {

  public PasswordFieldMgr(EvaluatedNodeInfoItem pEvaluatedNodeInfo, FieldSet pFieldSet, String pExternalFieldName) {
    super(pEvaluatedNodeInfo, pFieldSet, pExternalFieldName, PasswordTransformer.instance());
  }

  public FieldInfo createFieldInfoOrNull() {
    //Record the fact that we have sent obfuscated spaces so no change is applied if the user doesn't change the password
    return createFieldInfo(getFieldTransformer().applyOutboundTransform(getValueDOM()));
  }
}
