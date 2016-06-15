package net.foxopen.fox.module.serialiser.widgets.html;

import net.foxopen.fox.XFUtil;
import net.foxopen.fox.module.datanode.EvaluatedNode;
import net.foxopen.fox.module.datanode.NodeAttribute;
import net.foxopen.fox.module.serialiser.SerialisationContext;
import net.foxopen.fox.module.serialiser.components.html.ExternalURLComponentBuilder;
import net.foxopen.fox.module.serialiser.fragmentbuilder.MustacheFragmentBuilder;
import net.foxopen.fox.module.serialiser.html.HTMLSerialiser;
import net.foxopen.fox.module.serialiser.widgets.WidgetBuilder;
import org.apache.commons.lang3.StringEscapeUtils;

import java.util.Map;


public class URLWidgetBuilder extends WidgetBuilderHTMLSerialiser<EvaluatedNode> {
  private static final WidgetBuilder<HTMLSerialiser, EvaluatedNode> INSTANCE= new URLWidgetBuilder();
  private static final String MUSTACHE_TEMPLATE = "html/LinkWidget.mustache";

  public static final WidgetBuilder<HTMLSerialiser, EvaluatedNode> getInstance() {
    return INSTANCE;
  }

  private URLWidgetBuilder () {
  }

  @Override
  public boolean hasPrompt(EvaluatedNode pEvalNode) {
    return false;
  }

  @Override
  public void buildPrompt(SerialisationContext pSerialisationContext, HTMLSerialiser pSerialiser, EvaluatedNode pEvalNode) {
  }

  @Override
  public void buildWidgetInternal(SerialisationContext pSerialisationContext, HTMLSerialiser pSerialiser, EvaluatedNode pEvalNode) {
    String lURL = pEvalNode.getStringAttribute(NodeAttribute.HREF, null);
    if (!XFUtil.isNull(lURL)) {
      lURL = pSerialisationContext.getStaticResourceOrFixedURI(lURL);

      Map<String, Object> lTemplateVars = super.getGenericTemplateVars(pSerialisationContext, pSerialiser, pEvalNode);

      // Choose between a non-js link and a FOXjs.openwin() windowOptions string
      String lLinkType = pEvalNode.getStringAttribute(NodeAttribute.LINK_TYPE, ExternalURLComponentBuilder.NON_JS_LINK_TYPE);
      if (ExternalURLComponentBuilder.NON_JS_LINK_TYPE.equals(lLinkType)) {
        lTemplateVars.put("ActionHref", StringEscapeUtils.escapeHtml4(lURL));
        lTemplateVars.put("TargetBlank", true);
      }
      else {
        lTemplateVars.put("AttributeSafeActionJS", StringEscapeUtils.escapeHtml4(HTMLSerialiser.buildFOXjsOpenWinJSON(lURL, lLinkType)));
      }

      lTemplateVars.put("PromptText", XFUtil.nvl(lTemplateVars.get("PromptText"), lURL));
      lTemplateVars.put("LinkTitle", XFUtil.nvl(XFUtil.nvl(pEvalNode.getStringAttribute(NodeAttribute.LINK_TITLE), lTemplateVars.get("PromptText")), lURL));

      MustacheFragmentBuilder.applyMapToTemplate(MUSTACHE_TEMPLATE, lTemplateVars, pSerialiser.getWriter());
    }
  }
}
