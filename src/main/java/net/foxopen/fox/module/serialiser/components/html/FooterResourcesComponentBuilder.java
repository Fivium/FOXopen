package net.foxopen.fox.module.serialiser.components.html;

import net.foxopen.fox.module.parsetree.evaluatedpresentationnode.EvaluatedPresentationNode;
import net.foxopen.fox.module.serialiser.SerialisationContext;
import net.foxopen.fox.module.serialiser.components.ComponentBuilder;
import net.foxopen.fox.module.serialiser.html.HTMLSerialiser;
import net.foxopen.fox.module.serialiser.widgets.WidgetBuilderType;

/**
 * CSS and JS includes which go at the end of an HTML page, just before the "body" close tag. It is preferred for scripts
 * to be loaded at this location so the page can render more quickly.
 */
public class FooterResourcesComponentBuilder
extends AbstractResourcesComponentBuilder {

  private static final ComponentBuilder<HTMLSerialiser, EvaluatedPresentationNode> INSTANCE = new FooterResourcesComponentBuilder();

  public static ComponentBuilder<HTMLSerialiser, EvaluatedPresentationNode> getInstance() {
    return INSTANCE;
  }

  private FooterResourcesComponentBuilder() {
  }

  @Override
  public void buildComponent(SerialisationContext pSerialisationContext, HTMLSerialiser pSerialiser, EvaluatedPresentationNode pEvalNode) {

    //JS common to every page
    javascript(pSerialiser, pSerialisationContext.getStaticResourceURI("js/core-footer.js"));

    if (pSerialisationContext.getImplicatedWidgets().contains(WidgetBuilderType.SEARCH_SELECTOR)) {
      javascript(pSerialiser, pSerialisationContext.getStaticResourceURI("js/tagger.js"));
    }

    if (pSerialisationContext.getImplicatedWidgets().contains(WidgetBuilderType.HTML)) {
      javascript(pSerialiser, pSerialisationContext.getContextResourceURI("/tiny_mce/tinymce.min.js"));
    }

    if (pSerialisationContext.getImplicatedWidgets().contains(WidgetBuilderType.FILE)) {
      javascript(pSerialiser, pSerialisationContext.getStaticResourceURI("js/fileUpload.js"));
    }

    if(pSerialisationContext.getDownloadLinks().size() > 0) {
      javascript(pSerialiser, pSerialisationContext.getStaticResourceURI("js/download.js"));
    }

    if(pSerialisationContext.getClientVisibilityRuleCount() > 0) {
      javascript(pSerialiser, pSerialisationContext.getStaticResourceURI("js/clientVisibility.js"));
    }

    if (pSerialisationContext.getEvaluatedDataDefinitions().size() > 0) {
      javascript(pSerialiser, pSerialisationContext.getStaticResourceURI("js/foxData.js"));
    }
  }
}
