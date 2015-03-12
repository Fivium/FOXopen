package net.foxopen.fox.module.serialiser.widgets.html;

import net.foxopen.fox.XFUtil;
import net.foxopen.fox.module.datanode.EvaluatedNode;
import net.foxopen.fox.module.serialiser.SerialisationContext;
import net.foxopen.fox.module.serialiser.fragmentbuilder.MustacheFragmentBuilder;
import net.foxopen.fox.module.serialiser.html.HTMLSerialiser;
import net.foxopen.fox.module.serialiser.widgets.WidgetBuilder;
import org.apache.commons.lang3.StringEscapeUtils;

import java.util.Map;


public class TextWidgetBuilder extends WidgetBuilderHTMLSerialiser<EvaluatedNode> {
  private static final WidgetBuilder<HTMLSerialiser, EvaluatedNode> INSTANCE = new TextWidgetBuilder();
  private static final String TEXT_MUSTACHE_TEMPLATE = "html/TextWidget.mustache";

  public static final WidgetBuilder<HTMLSerialiser, EvaluatedNode> getInstance() {
    return INSTANCE;
  }

  private TextWidgetBuilder () {
  }

  @Override
  public boolean hasPrompt(EvaluatedNode pEvalNode) {
    return false;
  }

  @Override
  public void buildPrompt(HTMLSerialiser pSerialiser, EvaluatedNode pEvalNode) {
  }

  @Override
  public void buildWidgetInternal(SerialisationContext pSerialisationContext, HTMLSerialiser pSerialiser, EvaluatedNode pEvalNode) {
    Map<String, Object> lTemplateVars = super.getGenericTemplateVars(pSerialiser, pEvalNode);
    lTemplateVars.put("Class", "text-widget " + XFUtil.nvl(lTemplateVars.get("Class"), ""));

    lTemplateVars.put("UnescapedValue", StringEscapeUtils.escapeHtml4(pEvalNode.getFieldMgr().getSingleTextValue()).replaceAll("\r?\n","<br>"));

    MustacheFragmentBuilder.applyMapToTemplate(TEXT_MUSTACHE_TEMPLATE, lTemplateVars, pSerialiser.getWriter());
  }
}
