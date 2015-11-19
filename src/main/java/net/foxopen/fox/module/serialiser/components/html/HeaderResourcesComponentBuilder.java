package net.foxopen.fox.module.serialiser.components.html;

import net.foxopen.fox.XFUtil;
import net.foxopen.fox.logging.BoomerangBangHandler;
import net.foxopen.fox.logging.RequestLogger;
import net.foxopen.fox.module.CSSListItem;
import net.foxopen.fox.module.parsetree.evaluatedpresentationnode.EvaluatedPresentationNode;
import net.foxopen.fox.module.serialiser.SerialisationContext;
import net.foxopen.fox.module.serialiser.components.ComponentBuilder;
import net.foxopen.fox.module.serialiser.html.HTMLSerialiser;
import net.foxopen.fox.module.serialiser.widgets.WidgetBuilderType;
import net.foxopen.fox.thread.devtoolbar.DevToolbarUtils;

import java.util.List;

/**
 *
 */
public class HeaderResourcesComponentBuilder
extends AbstractResourcesComponentBuilder {

  private static final ComponentBuilder<HTMLSerialiser, EvaluatedPresentationNode> INSTANCE = new HeaderResourcesComponentBuilder();

  public static ComponentBuilder<HTMLSerialiser, EvaluatedPresentationNode> getInstance() {
    return INSTANCE;
  }

  private HeaderResourcesComponentBuilder() {
  }

  @Override
  public void buildComponent(SerialisationContext pSerialisationContext, HTMLSerialiser pSerialiser, EvaluatedPresentationNode pEvalNode) {
    if (pSerialiser.isHeaderResourcesIncluded()) {
      return;
    }

    //TODO PN logging should be based on flag on request, or at least on engine config
    if(RequestLogger.LOG_USER_EXPERIENCE_TIMES) {
      javascript(pSerialiser, pSerialisationContext.getStaticResourceURI("js/boomerang.js"));

      pSerialiser.append("<script>\n" +
        "  BOOMR.init({\n" +
        "    beacon_url: '");
      pSerialiser.append(pSerialisationContext.createURIBuilder().buildBangHandlerURI(BoomerangBangHandler.instance()));
      pSerialiser.append("',\n" +
      "    log: null\n" +
      "  });\n" +
      "  BOOMR.addVar('");
      pSerialiser.append(BoomerangBangHandler.REQUEST_ID_PARAM_NAME);
      pSerialiser.append("','");
      pSerialiser.append(pSerialisationContext.getRequestLogId());
      pSerialiser.append("');\n");

      //For the dev toolbar, add a listener so the UX time can be displayed in the timing summary div when boomerang reports it
      if(DevToolbarUtils.isDevToolbarEnabled(pSerialisationContext)) {
        pSerialiser.append("  BOOMR.subscribe('before_beacon', function(o) {\n" +
                           "    DevToolbar.setUserExperienceTime(o.t_done)\n" +
                           "  });\n");
      }
      pSerialiser.append("</script>");
    }

    //JS first on desktop - may require a switch for mobile browsers
    //see http://stackoverflow.com/questions/9271276/is-the-recommendation-to-include-css-before-javascript-invalid

    javascript(pSerialiser, pSerialisationContext.getStaticResourceURI("js/core-header.js"));

    css(pSerialiser, pSerialisationContext.getContextResourceURI("/OpenSans/OpenSans.css"));
    css(pSerialiser, pSerialisationContext.getContextResourceURI("/icomoon/icomoon.css"));
    css(pSerialiser, pSerialisationContext.getContextResourceURI("/icomoon/icomoon-png.css"));

    if (pSerialisationContext.getImplicatedWidgets().contains(WidgetBuilderType.SEARCH_SELECTOR)) {
      css(pSerialiser, pSerialisationContext.getStaticResourceURI("css/tagger.css"));
    }

    if (pSerialisationContext.getImplicatedWidgets().contains(WidgetBuilderType.FILE)) {
      css(pSerialiser, pSerialisationContext.getStaticResourceURI("css/fileUpload.css"));
      css(pSerialiser, pSerialisationContext.getStaticResourceURI("css/fileUploadLTIE8.css"), "LT IE 8");
    }

    if (pSerialisationContext.getImplicatedWidgets().contains(WidgetBuilderType.CARTOGRAPHIC)) {
      css(pSerialiser, pSerialisationContext.getStaticResourceURI("css/spatial.css"));
    }

    if(pSerialisationContext.getDownloadLinks().size() > 0) {
      css(pSerialiser, pSerialisationContext.getStaticResourceURI("css/download.css"));
    }

    css(pSerialiser, pSerialisationContext.getStaticResourceURI("css/fox.css"));
    css(pSerialiser, pSerialisationContext.getStaticResourceURI("css/fox-ie8.css"), "IE 8");

    // Include CSS from module CSS List
    insertCSSItems(pSerialisationContext, pSerialiser);

    pSerialiser.setHeaderResourcesIncluded(true);
  }

  /**
   * Insert CSS links from CSS lists in modules and libraries
   *
   * @param pSerialisationContext
   * @param pSerialiser
   */
  private void insertCSSItems(SerialisationContext pSerialisationContext, HTMLSerialiser pSerialiser) {
    List<CSSListItem> lCSSItems = pSerialisationContext.getModule().getStyleSheets();

    for (CSSListItem lCSSItem : lCSSItems) {
      if (("accessible".equals(lCSSItem.getType()) && pSerialisationContext.isAccessibilityMode()) ||
          ("standard".equals(lCSSItem.getType()) && !pSerialisationContext.isAccessibilityMode()) ||
          "standard-and-accessible".equals(lCSSItem.getType())) {

        String lBrowserCondition = lCSSItem.getBrowserCondition();
        if(!XFUtil.isNull(lBrowserCondition)) {
          pSerialiser.append("<!--[if ");
          pSerialiser.append(lBrowserCondition);
          pSerialiser.append("]>");
        }

        css(pSerialiser, pSerialisationContext.getStaticResourceOrFixedURI(lCSSItem.getStyleSheetPath()));

        if(!XFUtil.isNull(lBrowserCondition)) {
          pSerialiser.append("<![endif]-->");
        }
      }
    }
  }
}
