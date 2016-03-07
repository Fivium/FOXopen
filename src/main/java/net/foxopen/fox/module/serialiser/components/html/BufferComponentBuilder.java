package net.foxopen.fox.module.serialiser.components.html;

import net.foxopen.fox.XFUtil;
import net.foxopen.fox.module.parsetree.evaluatedpresentationnode.EvaluatedBufferPresentationNode;
import net.foxopen.fox.module.serialiser.SerialisationContext;
import net.foxopen.fox.module.serialiser.components.ComponentBuilder;
import net.foxopen.fox.module.serialiser.fragmentbuilder.MustacheFragmentBuilder;
import net.foxopen.fox.module.serialiser.html.HTMLSerialiser;

import java.util.HashMap;
import java.util.Map;


public class BufferComponentBuilder extends ComponentBuilder<HTMLSerialiser, EvaluatedBufferPresentationNode> {
  private static final String SKIPLINK_TARGET_MUSTACHE_TEMPLATE = "html/SkipLinkTarget.mustache";

  private static final ComponentBuilder<HTMLSerialiser, EvaluatedBufferPresentationNode> INSTANCE = new BufferComponentBuilder();

  public static final ComponentBuilder<HTMLSerialiser, EvaluatedBufferPresentationNode> getInstance() {
    return INSTANCE;
  }

  private BufferComponentBuilder() {
  }

  @Override
  public void buildComponent(SerialisationContext pSerialisationContext, HTMLSerialiser pSerialiser, EvaluatedBufferPresentationNode pEvalBufferNode) {

    if(pEvalBufferNode.getEvalClientVisibilityRule() != null) {
      //Wrapping div for contents hiding
      //TODO PN make this better (shouldn't really wrap arbitrary stuff in a div, might break the page)
      pSerialiser.append("<div data-xfid=\"");
      pSerialiser.append(pEvalBufferNode.getSkipLinkID());
      pSerialiser.append("\" class=\"");
      pSerialiser.append(pEvalBufferNode.getEvalClientVisibilityRule().getInitialCSSClass());
      pSerialiser.append("\">");
    }

    if (!XFUtil.isNull(pEvalBufferNode.getRegionTitle())) {
      Map<String, Object> lTemplateVars = new HashMap<>(2);
      lTemplateVars.put("SkipLinkID", pEvalBufferNode.getSkipLinkID());
      lTemplateVars.put("RegionTitle", pEvalBufferNode.getRegionTitle());

      MustacheFragmentBuilder.applyMapToTemplate(SKIPLINK_TARGET_MUSTACHE_TEMPLATE, lTemplateVars, pSerialiser.getWriter());
    }

    processChildren(pSerialisationContext, pSerialiser, pEvalBufferNode);

    if(pEvalBufferNode.getEvalClientVisibilityRule() != null) {
      //Close the wrapping div
      pSerialiser.append("</div>");
    }
  }
}
