package net.foxopen.fox.module.serialiser.widgets.html;


import net.foxopen.fox.XFUtil;
import net.foxopen.fox.module.datanode.EvaluatedNode;
import net.foxopen.fox.module.datanode.NodeVisibility;
import net.foxopen.fox.module.serialiser.SerialisationContext;
import net.foxopen.fox.module.serialiser.fragmentbuilder.MustacheFragmentBuilder;
import net.foxopen.fox.module.serialiser.html.HTMLSerialiser;
import net.foxopen.fox.module.serialiser.widgets.WidgetBuilder;
import net.foxopen.fox.module.serialiser.widgets.WidgetBuilderType;

import java.util.Map;


public class DateWidgetBuilder extends WidgetBuilderHTMLSerialiser<EvaluatedNode> {
  private static final WidgetBuilder<HTMLSerialiser, EvaluatedNode> INSTANCE = new DateWidgetBuilder();
  private static final String MUSTACHE_TEMPLATE = "html/DateWidget.mustache";

  public static final WidgetBuilder<HTMLSerialiser, EvaluatedNode> getInstance() {
    return INSTANCE;
  }

  private DateWidgetBuilder () {
  }

  @Override
  public void buildWidgetInternal(SerialisationContext pSerialisationContext, HTMLSerialiser pSerialiser, EvaluatedNode pEvalNode) {
    if (!pEvalNode.isPlusWidget() && pEvalNode.getFieldMgr().getVisibility() == NodeVisibility.VIEW) {
      pSerialiser.getWidgetBuilder(WidgetBuilderType.TEXT).buildWidget(pSerialisationContext, pSerialiser, pEvalNode);
    }
    else {
      Map<String, Object> lTemplateVars = super.getGenericTemplateVars(pSerialisationContext, pSerialiser, pEvalNode);
      lTemplateVars.put("InputType", "text");
      lTemplateVars.put("Value", pEvalNode.getFieldMgr().getSingleTextValue());
      if (pEvalNode.getWidgetBuilderType() == WidgetBuilderType.DATE_TIME) {
        lTemplateVars.put("Class", "date-input date-time-input " + XFUtil.nvl(lTemplateVars.get("Class"), ""));
        lTemplateVars.put("Cols", "17");
      }
      else {
        lTemplateVars.put("Class", "date-input " + XFUtil.nvl(lTemplateVars.get("Class"), ""));
        lTemplateVars.put("Cols", "11");
      }

      MustacheFragmentBuilder.applyMapToTemplate(MUSTACHE_TEMPLATE, lTemplateVars, pSerialiser.getWriter());
    }
  }
}
