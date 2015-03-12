package net.foxopen.fox.module.fieldset.fieldinfo;

import java.util.Collections;
import java.util.List;

import net.foxopen.fox.dom.DOM;
import net.foxopen.fox.module.ChangeActionContext;
import net.foxopen.fox.module.fieldset.transformer.FieldTransformer;
import net.foxopen.fox.thread.ActionRequestContext;


public class TextValueFieldInfo
extends SingleValueFieldInfo {

  /** Transformer used to send/receive value */
  private final FieldTransformer mFieldTransformer;

  public TextValueFieldInfo(String pExternalName, String pDOMRef, String pCurrentValue, String pChangeActionName, FieldTransformer pFieldTransformer) {
    super(pExternalName, pDOMRef, pChangeActionName, pCurrentValue);
    mFieldTransformer = pFieldTransformer;
  }

  @Override
  public List<ChangeActionContext> applyPostedValues(ActionRequestContext pRequestContext, String[] pPostedValues) {

    String lPostedValue = getSinglePostedValue(pPostedValues);

    lPostedValue = mFieldTransformer.applyInboundTransform(lPostedValue);

    if(!getSentValue().equals(lPostedValue)) {

      DOM lItemDOM = resolveAndClearTargetDOM(pRequestContext);

      if(!"".equals(lPostedValue)) {
        lItemDOM.setText(lPostedValue);
      }

      return createChangeActionContext(lItemDOM);
    }
    else {
      return Collections.emptyList();
    }
  }

}
