package net.foxopen.fox.module.serialiser.widgets.html;

import net.foxopen.fox.XFUtil;
import net.foxopen.fox.dom.DOM;
import net.foxopen.fox.dom.DOMList;
import net.foxopen.fox.ex.ExBadPath;
import net.foxopen.fox.ex.ExInternal;
import net.foxopen.fox.module.fieldset.fieldmgr.FieldMgr;
import net.foxopen.fox.module.fieldset.transformer.html.HTMLWidgetConfig;
import net.foxopen.fox.module.datanode.EvaluatedNode;
import net.foxopen.fox.module.datanode.NodeAttribute;
import net.foxopen.fox.module.datanode.NodeVisibility;
import net.foxopen.fox.module.serialiser.SerialisationContext;
import net.foxopen.fox.module.serialiser.fragmentbuilder.MustacheFragmentBuilder;
import net.foxopen.fox.module.serialiser.html.HTMLSerialiser;
import net.foxopen.fox.module.serialiser.widgets.WidgetBuilder;

import java.util.Map;


public class HTMLWidgetBuilder extends WidgetBuilderHTMLSerialiser<EvaluatedNode> {
  private static final WidgetBuilder<HTMLSerialiser, EvaluatedNode> INSTANCE = new HTMLWidgetBuilder();
  private static final String MUSTACHE_TEMPLATE = "html/HTMLWidget.mustache";
  private static final String JS_MUSTACHE_TEMPLATE = "html/TinyMCEJS.mustache";

  public static final WidgetBuilder<HTMLSerialiser, EvaluatedNode> getInstance() {
    return INSTANCE;
  }

  private HTMLWidgetBuilder() {
  }

  private static void convertMailMergeTagsToSpans(DOM pValueDOM) {

    try {
      // Convert MM to style wrapped [[TEXT]]
      DOMList mm = pValueDOM.xpathUL("//MM[not(attribute::for)]", null);
      int j = mm.getLength();
      DOM item;
      for(int i=0; i<j; i++) {
        item = mm.item(i);
        String name = item.value();
        item.setAttr("style", "color: red; background-color: #FFFF66; font-size: 7pt").setText("[["+name+"]]").rename("span");
      }
    }
    catch (ExBadPath e) {
      throw new ExInternal("Failed to convert MM tags to span containers", e);
    }

  }

  @Override
  public void buildWidgetInternal(SerialisationContext pSerialisationContext, HTMLSerialiser pSerialiser, EvaluatedNode pEvalNode) {
    FieldMgr lFieldMgr = pEvalNode.getFieldMgr();

    if (!pEvalNode.isPlusWidget() && lFieldMgr.getVisibility() == NodeVisibility.VIEW) {
      // Dump out raw HTML if readonly and not a plus widget
      pSerialiser.append(lFieldMgr.getSingleTextValue());
    }
    else {
      HTMLWidgetConfig lHTMLWidgetConfig = pSerialisationContext.getApp().getHTMLWidgetConfig(pEvalNode.getStringAttribute(NodeAttribute.HTML_WIDGET_CONFIG));

      DOM lSendingXML = lFieldMgr.getSingleXMLValue();
      if(lHTMLWidgetConfig.getHTMLTransformConfig().isMailMergeTranslationEnabled()) {
        convertMailMergeTagsToSpans(lSendingXML);
      }

      Map<String, Object> lTemplateVars = super.getGenericTemplateVars(pSerialisationContext, pSerialiser, pEvalNode);
      lTemplateVars.put("Value", lSendingXML.outputNodeContentsToString(false));
      lTemplateVars.put("Class", "htmltextarea clv-ignore " + XFUtil.nvl(lTemplateVars.get("Class"), ""));
      lTemplateVars.put("Style", "visibility: hidden;" + XFUtil.nvl(lTemplateVars.get("Style"), ""));

      MustacheFragmentBuilder.applyMapToTemplate(MUSTACHE_TEMPLATE, lTemplateVars, pSerialiser.getWriter());


      // TODO - NP - Does this need to call Generic vars again? Maybe it should expand upon the previous template vars
      //             or perhaps it could clone the original result of the call to getGenericTemplateVars()?
      Map<String, Object> lJSTemplateVars = super.getGenericTemplateVars(pSerialisationContext, pSerialiser, pEvalNode);

      // TODO - NP - Re-enable this if we reimplement spellcheck
//      if (lFieldMgr.isSpellEnabled() && lFieldMgr.isSpellIcon()) {
//        lJSTemplateVars.put("SpellCheckIcon", false);
//      }
      boolean lSpellcheckerIconEnabled = false;//(lFieldMgr.isSpellEnabled() && lFieldMgr.isSpellIcon());

      String lToolbarConfig = lHTMLWidgetConfig.getToolbarConfig();
      if(lSpellcheckerIconEnabled) {
        lToolbarConfig = lToolbarConfig.replace("#spellcheck#", "| foxopenspellchecker");
      }
      else {
        lToolbarConfig = lToolbarConfig.replace("#spellcheck#", "");
      }
      lJSTemplateVars.put("ToolbarConfig", lToolbarConfig);

      lJSTemplateVars.put("AdditionalPlugins", lHTMLWidgetConfig.getAdditionalPlugins());
      lJSTemplateVars.put("AdditionalConfig", lHTMLWidgetConfig.getAdditionalConfig());

      if (pEvalNode.getBooleanAttribute(NodeAttribute.AUTO_RESIZE, false)) {
        lJSTemplateVars.put("AutoResize", true);

        String lAutoResizeMaxHeight = pEvalNode.getStringAttribute(NodeAttribute.AUTO_RESIZE_MAX_HEIGHT);
        if (lAutoResizeMaxHeight != null) {
          lJSTemplateVars.put("AutoResizeMaxHeight", lAutoResizeMaxHeight);
        }
      }

      MustacheFragmentBuilder.applyMapToTemplate(JS_MUSTACHE_TEMPLATE, lJSTemplateVars, pSerialiser.getWriter());
    }
  }
}
