package net.foxopen.fox.module.serialiser.widgets.html;

import net.foxopen.fox.module.fieldset.fieldmgr.FieldMgr;
import net.foxopen.fox.module.datanode.EvaluatedNode;
import net.foxopen.fox.module.serialiser.SerialisationContext;
import net.foxopen.fox.module.serialiser.fragmentbuilder.MustacheFragmentBuilder;
import net.foxopen.fox.module.serialiser.html.HTMLSerialiser;
import net.foxopen.fox.module.serialiser.widgets.WidgetBuilder;

import java.io.StringWriter;
import java.util.Map;


public class TimerWidgetBuilder extends WidgetBuilderHTMLSerialiser<EvaluatedNode> {
  private static final WidgetBuilder<HTMLSerialiser, EvaluatedNode> INSTANCE = new TimerWidgetBuilder();
  private static final String INPUT_MUSTACHE_TEMPLATE = "html/InputWidget.mustache";
  private static final String TEXTAREA_MUSTACHE_TEMPLATE = "html/TextareaWidget.mustache";
  private static final String JS_MUSTACHE_TEMPLATE = "html/TimerJS.mustache";

  public static final WidgetBuilder<HTMLSerialiser, EvaluatedNode> getInstance() {
    return INSTANCE;
  }

  private TimerWidgetBuilder() {
  }

  @Override
  public void buildWidgetInternal(SerialisationContext pSerialisationContext, HTMLSerialiser pSerialiser, EvaluatedNode pEvalNode) {
    // TODO - NP - This could be prettified a lot in the future, left as an input for now though
    FieldMgr lFieldMgr = pEvalNode.getFieldMgr();

    Map<String, Object> lTemplateVars = super.getGenericTemplateVars(pSerialisationContext, pSerialiser, pEvalNode);
    lTemplateVars.put("InputType", "text");
    lTemplateVars.put("Value", lFieldMgr.getSingleTextValue());
    lTemplateVars.put("Readonly", true);

    if ("1".equals(pEvalNode.getFieldHeight()) || "password".equals(lTemplateVars.get("InputType"))) {
      MustacheFragmentBuilder.applyMapToTemplate(INPUT_MUSTACHE_TEMPLATE, lTemplateVars, pSerialiser.getWriter());
    }
    else {
      MustacheFragmentBuilder.applyMapToTemplate(TEXTAREA_MUSTACHE_TEMPLATE, lTemplateVars, pSerialiser.getWriter());
    }

    if (pEvalNode.isRunnable()) {
      StringWriter lJSWriter = new StringWriter();
      MustacheFragmentBuilder.applyMapToTemplate(JS_MUSTACHE_TEMPLATE, lTemplateVars, lJSWriter);
      pSerialisationContext.addConditionalLoadJavascript(lJSWriter.toString());
    }
  }
}
