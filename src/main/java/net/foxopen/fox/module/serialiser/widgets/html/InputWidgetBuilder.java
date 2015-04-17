package net.foxopen.fox.module.serialiser.widgets.html;

import net.foxopen.fox.module.datanode.EvaluatedNode;
import net.foxopen.fox.module.datanode.NodeAttribute;
import net.foxopen.fox.module.datanode.NodeVisibility;
import net.foxopen.fox.module.serialiser.SerialisationContext;
import net.foxopen.fox.module.serialiser.fragmentbuilder.MustacheFragmentBuilder;
import net.foxopen.fox.module.serialiser.html.HTMLSerialiser;
import net.foxopen.fox.module.serialiser.widgets.WidgetBuilder;
import net.foxopen.fox.module.serialiser.widgets.WidgetBuilderType;
import org.apache.commons.lang3.StringEscapeUtils;

import java.util.Map;


public class InputWidgetBuilder extends WidgetBuilderHTMLSerialiser<EvaluatedNode> {
  private static final WidgetBuilder<HTMLSerialiser, EvaluatedNode> INSTANCE = new InputWidgetBuilder("text");
  private static final String INPUT_MUSTACHE_TEMPLATE = "html/InputWidget.mustache";
  private static final String TEXTAREA_MUSTACHE_TEMPLATE = "html/TextareaWidget.mustache";

  private final String mInputType;

  public static final WidgetBuilder<HTMLSerialiser, EvaluatedNode> getInstance() {
    return INSTANCE;
  }

  protected InputWidgetBuilder(String pInputType) {
    mInputType = pInputType;
  }

  @Override
  public void buildWidgetInternal(SerialisationContext pSerialisationContext, HTMLSerialiser pSerialiser, EvaluatedNode pEvalNode) {

    if (!pEvalNode.isPlusWidget() && pEvalNode.getFieldMgr().getVisibility() == NodeVisibility.VIEW) {
      pSerialiser.getWidgetBuilder(WidgetBuilderType.TEXT).buildWidget(pSerialisationContext, pSerialiser, pEvalNode);
    }
    else {
      Map<String, Object> lTemplateVars = super.getGenericTemplateVars(pSerialisationContext, pSerialiser, pEvalNode);
      lTemplateVars.put("InputType", mInputType);

      String lTextValue = pEvalNode.getFieldMgr().getSingleTextValue();
      lTemplateVars.put("Value", lTextValue);

      if (pEvalNode.getMaxDataLength() != Integer.MAX_VALUE) {
        lTemplateVars.put("MaxLength", pEvalNode.getMaxDataLength());
      }

      if (pEvalNode.isWidgetAutoResize()) {
        lTemplateVars.put("AutoResize", true);

        String lAutoResizeMaxHeight = pEvalNode.getStringAttribute(NodeAttribute.AUTO_RESIZE_MAX_HEIGHT);
        if (lAutoResizeMaxHeight != null) {
          lTemplateVars.put("Style", "max-height: " + lAutoResizeMaxHeight + "px;" + lTemplateVars.get("Style"));
        }
      }

      if (("1".equals(pEvalNode.getFieldHeight()) || "password".equals(mInputType)) && !pEvalNode.isWidgetAutoResize()) {
        MustacheFragmentBuilder.applyMapToTemplate(INPUT_MUSTACHE_TEMPLATE, lTemplateVars, pSerialiser.getWriter());
      }
      else {
        //Manually escape for now as mustache auto-escape corrupts these entities (see use of {{{x}}} in textarea template instead of {{x}})
        lTextValue = StringEscapeUtils.escapeHtml4(lTextValue);
        //Convert newlines to HTML entities for standards conformance
        lTextValue = lTextValue.replaceAll("\r?\n", "&#13;&#10;");
        lTemplateVars.put("Value", lTextValue);

        MustacheFragmentBuilder.applyMapToTemplate(TEXTAREA_MUSTACHE_TEMPLATE, lTemplateVars, pSerialiser.getWriter());
      }
    }
  }
}
