package net.foxopen.fox.module.serialiser.components.html;

import net.foxopen.fox.module.parsetree.evaluatedpresentationnode.EvaluatedBufferPresentationNode;
import net.foxopen.fox.module.parsetree.evaluatedpresentationnode.EvaluatedPresentationNode;
import net.foxopen.fox.module.serialiser.SerialisationContext;
import net.foxopen.fox.module.serialiser.components.ComponentBuilder;
import net.foxopen.fox.module.serialiser.fragmentbuilder.MustacheFragmentBuilder;
import net.foxopen.fox.module.serialiser.html.HTMLSerialiser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class SkipLinksComponentBuilder extends ComponentBuilder<HTMLSerialiser, EvaluatedPresentationNode> {
  private static final String SKIPLINK_MUSTACHE_TEMPLATE = "html/SkipLinkComponent.mustache";

  private static final ComponentBuilder<HTMLSerialiser, EvaluatedPresentationNode> INSTANCE = new SkipLinksComponentBuilder();

  public static final ComponentBuilder<HTMLSerialiser, EvaluatedPresentationNode> getInstance() {
    return INSTANCE;
  }

  private SkipLinksComponentBuilder() {
  }

  @Override
  public void buildComponent(SerialisationContext pSerialisationContext, HTMLSerialiser pSerialiser, EvaluatedPresentationNode pEvalNode) {
    List<EvaluatedBufferPresentationNode> lBufferRegions = pSerialisationContext.getBufferRegions();
    if (lBufferRegions.size() > 0) {
      Map<String, Object> lTemplateVars = new HashMap<>(1);


      List<Map<String, String>> lLinks = new ArrayList<>();
      for (EvaluatedBufferPresentationNode lBuffer : lBufferRegions) {
        Map<String, String> lLinkTemplateVars = new HashMap<>(2);
        lLinkTemplateVars.put("SkipLinkID", lBuffer.getSkipLinkID());
        lLinkTemplateVars.put("RegionTitle", lBuffer.getRegionTitle());
        lLinks.add(lLinkTemplateVars);
      }
      lTemplateVars.put("Links", lLinks);

      MustacheFragmentBuilder.applyMapToTemplate(SKIPLINK_MUSTACHE_TEMPLATE, lTemplateVars, pSerialiser.getWriter());
    }
  }
}
