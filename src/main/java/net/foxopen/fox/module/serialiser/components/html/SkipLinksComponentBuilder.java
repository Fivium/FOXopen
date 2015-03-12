package net.foxopen.fox.module.serialiser.components.html;

import java.util.List;

import net.foxopen.fox.module.parsetree.evaluatedpresentationnode.EvaluatedBufferPresentationNode;
import net.foxopen.fox.module.parsetree.evaluatedpresentationnode.EvaluatedPresentationNode;
import net.foxopen.fox.module.serialiser.SerialisationContext;
import net.foxopen.fox.module.serialiser.components.ComponentBuilder;
import net.foxopen.fox.module.serialiser.html.HTMLSerialiser;


public class SkipLinksComponentBuilder extends ComponentBuilder<HTMLSerialiser, EvaluatedPresentationNode> {
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
      pSerialiser.append("<div id=\"skiplinks\">");
      pSerialiser.append("<p>Skip To:</p>");
      pSerialiser.append("<ul>");
      for (EvaluatedBufferPresentationNode lBuffer : lBufferRegions) {
        pSerialiser.append("<li><a href=\"#" + lBuffer.getSkipLinkID() + "\">" + lBuffer.getRegionTitle() + "</a></li>");
      }
      pSerialiser.append("</ul>");
      pSerialiser.append("</div>");
    }
  }
}
