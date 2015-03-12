package net.foxopen.fox.module.fieldset.fieldinfo;

import net.foxopen.fox.dom.DOM;
import net.foxopen.fox.ex.ExInternal;
import net.foxopen.fox.module.ChangeActionContext;
import net.foxopen.fox.module.fieldset.transformer.html.DefaultHTMLValueTransformer;
import net.foxopen.fox.module.fieldset.transformer.html.HTMLWidgetConfig;
import net.foxopen.fox.thread.ActionRequestContext;

import java.util.Collections;
import java.util.List;


public class HTMLValueFieldInfo
extends SingleValueFieldInfo {

  private final String mHTMLWidgetConfigName;

  public HTMLValueFieldInfo(String pExternalName, String pDOMRef, String pCurrentValue, String pChangeActionName, String pHTMLWidgetConfigName) {
    super(pExternalName, pDOMRef, pChangeActionName, pCurrentValue);
    mHTMLWidgetConfigName = pHTMLWidgetConfigName;
  }

  @Override
  public List<ChangeActionContext> applyPostedValues(ActionRequestContext pRequestContext, String[] pPostedValues) {

    String lPostedValue = getSinglePostedValue(pPostedValues);

    HTMLWidgetConfig lHTMLWidgetConfig = pRequestContext.getModuleApp().getHTMLWidgetConfig(mHTMLWidgetConfigName);
    if(lHTMLWidgetConfig == null) {
      throw new ExInternal("Failed to locate HTML widget config called " + mHTMLWidgetConfigName);
    }

    //Convert the posted string into an HTML DOM and serialise it to compare it against the sent value
    DOM lConvertedHTML = DefaultHTMLValueTransformer.instance().transformPostedString(lPostedValue, lHTMLWidgetConfig.getHTMLTransformConfig());
    String lConvertedHTMLString = lConvertedHTML.outputNodeContentsToString(false);

    if(!getSentValue().equals(lConvertedHTMLString)) {

      DOM lItemDOM = resolveAndClearTargetDOM(pRequestContext);

      if(!"".equals(lPostedValue)) {
        lItemDOM.setPreserveWhitespace(true);
        lConvertedHTML.copyContentsTo(lItemDOM);
      }

      return createChangeActionContext(lItemDOM);
    }
    else {
      return Collections.emptyList();
    }
  }
}
