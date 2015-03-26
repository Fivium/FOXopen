package net.foxopen.fox.module.serialiser.components.html;

import net.foxopen.fox.XFUtil;
import net.foxopen.fox.banghandler.BangHandlerServlet;
import net.foxopen.fox.banghandler.FlushBangHandler;
import net.foxopen.fox.dbinterface.DBMSOutputResult;
import net.foxopen.fox.entrypoint.uri.RequestURIBuilder;
import net.foxopen.fox.module.fieldset.action.InternalActionContext;
import net.foxopen.fox.module.fieldset.action.RefreshAction;
import net.foxopen.fox.module.parsetree.evaluatedpresentationnode.EvaluatedPresentationNode;
import net.foxopen.fox.module.serialiser.SerialisationContext;
import net.foxopen.fox.module.serialiser.components.ComponentBuilder;
import net.foxopen.fox.module.serialiser.html.HTMLSerialiser;
import net.foxopen.fox.thread.devtoolbar.DebugPage;
import net.foxopen.fox.thread.devtoolbar.DebugPageBangHandler;
import net.foxopen.fox.thread.devtoolbar.DevToolbarContext;
import net.foxopen.fox.thread.devtoolbar.DevToolbarUtils;
import net.foxopen.fox.thread.devtoolbar.ViewDOMBangHandler;
import net.foxopen.fox.thread.devtoolbar.XPathBangHandler;
import net.foxopen.fox.track.LatestTrackBangHandler;
import net.foxopen.fox.track.Track;
import net.foxopen.fox.track.TrackUtils;
import org.apache.commons.lang3.StringEscapeUtils;

import java.util.List;
import java.util.Map;


public class DevToolbarComponentBuilder extends ComponentBuilder<HTMLSerialiser, EvaluatedPresentationNode> {
  private static final ComponentBuilder<HTMLSerialiser, EvaluatedPresentationNode> INSTANCE = new DevToolbarComponentBuilder();

  //Debug pages which we send out links for
  private static final DebugPage[] DEBUG_PAGE_LINKS = new DebugPage[] {DebugPage.MODULE, DebugPage.MOD_MERGER, DebugPage.THREAD, DebugPage.FIELD_SET };

  public static final ComponentBuilder<HTMLSerialiser, EvaluatedPresentationNode> getInstance() {
    return INSTANCE;
  }

  private DevToolbarComponentBuilder() {
  }

  @Override
  public void buildComponent(SerialisationContext pSerialisationContext, HTMLSerialiser pSerialiser, EvaluatedPresentationNode pEvalNode) {
    //TODO PN - this whole thing needs tidying up/templating.

    DevToolbarContext lDevToolbarContext = pSerialisationContext.getDevToolbarContext();
    List<DBMSOutputResult> lDBMSOutputList = pSerialisationContext.getXDoResultList(DBMSOutputResult.class);

    String lFlushURI =  pSerialisationContext.createURIBuilder().buildBangHandlerURI(FlushBangHandler.instance());

    String lThreadId = pSerialisationContext.getThreadInfoProvider().getThreadId();

    pSerialiser.append("<div id=\"dev-toolbar\" class=\"short-dev-toolbar\">\n" +
    "<form id=\"devToolbarForm\">\n" +
    "<span id=\"dev-toolbar-icon\"></span>\n" +
    "<ul id=\"dev-toolbar-main-actions\">\n");

    // Flush Link
    pSerialiser.append("<li id=\"dev-toolbar-flush\"><a href=\"");
    pSerialiser.append(lFlushURI);
    pSerialiser.append("\" class=\"icon-bin\">Flush</a></li>\n");

    // Refresh Link
    InternalActionContext lActionContext = pSerialisationContext.getFieldSet().addInternalAction(RefreshAction.instance());
    pSerialiser.append("<li id=\"dev-toolbar-refresh\"><a href=\"#\" class=\"icon-spinner11\" onclick=\"");
    pSerialiser.append(pSerialiser.getInternalActionSubmitString(lActionContext));
    pSerialiser.append(";return false;\">Refresh</a></li>\n");

    // Entry point Link
    pSerialiser.append("<li id=\"dev-toolbar-entrypoint\"><a href=\"");
    pSerialiser.append(lDevToolbarContext.getEntryPointURI(pSerialisationContext.createURIBuilder()));
    pSerialiser.append("\" class=\"icon-enter\">Entry Point</a></li>\n");

    // Contexts Link
    pSerialiser.append("<li id=\"dev-toolbar-contexts\"><a href=\"#\" class=\"icon-link\">Contexts</a></li> \n");

    // XPath Runner
    pSerialiser.append("<li id=\"dev-toolbar-run-xpath\"><a href=\"#\" class=\"icon-play2\" onclick=\"FOXjs.openwin({url:'");
    pSerialiser.append(pSerialisationContext.createURIBuilder().setParam(XPathBangHandler.THREAD_ID_PARAM, lThreadId).buildBangHandlerURI(XPathBangHandler.instance()));
    pSerialiser.append("',windowOptions:'refwin'});return false;\">Run XPath</a></li>\n");

    pSerialiser.append("</ul>\n" +
      "<ul id=\"dev-toolbar-doms\">\n");

    RequestURIBuilder lDOMViewURIBuilder = pSerialisationContext.createURIBuilder();
    lDOMViewURIBuilder.setParam(ViewDOMBangHandler.THREAD_ID_PARAM, lThreadId);
//    lDOMViewURIBuilder.setParam(ViewDOMBangHandler.APP_MNEM_PARAM, pSerialisationContext.getThreadInfoProvider().getThreadAppMnem());

    for (String lDomName : lDevToolbarContext.getDocumentContextLabels()) {
      lDOMViewURIBuilder.setParam(ViewDOMBangHandler.DOM_NAME_PARAM, lDomName);
      String lDOMViewURI = lDOMViewURIBuilder.buildBangHandlerURI(ViewDOMBangHandler.instance());
      pSerialiser.append("<li><a href=\"#\" onclick=\"FOXjs.openwin({url:'");
      pSerialiser.append(StringEscapeUtils.escapeHtml4(lDOMViewURI));
      pSerialiser.append("',windowOptions:'appwin'});return false;\" id=\"");
      pSerialiser.append(StringEscapeUtils.escapeHtml4(lDomName));
      pSerialiser.append( "-dom-link\">:{");
      pSerialiser.append(StringEscapeUtils.escapeHtml4(lDomName));
      pSerialiser.append("}</a></li>\n");
    }
    pSerialiser.append("</ul>\n" +
      "<ul id=\"dev-toolbar-view\">\n");

    RequestURIBuilder lDebugPageURIBuilder = pSerialisationContext.createURIBuilder();
    lDebugPageURIBuilder.setParam(DebugPageBangHandler.THREAD_ID_PARAM, lThreadId);

    //General debug page links
    for (DebugPage lDebugPage : DEBUG_PAGE_LINKS) {
      lDebugPageURIBuilder.setParam(DebugPageBangHandler.DEBUG_PAGE_TYPE_PARAM_NAME, lDebugPage.toString());
      String lDebugURI = lDebugPageURIBuilder.buildBangHandlerURI(DebugPageBangHandler.instance());
      pSerialiser.append("<li><a href=\"#\" onclick=\"FOXjs.openwin({url:'");
      pSerialiser.append(StringEscapeUtils.escapeHtml4(lDebugURI));
      pSerialiser.append("',windowOptions:'appwin'});return false;\">");
      pSerialiser.append(StringEscapeUtils.escapeHtml4(XFUtil.initCap(lDebugPage.toString())));
      pSerialiser.append("</a></li>\n");
    }

    //Last track links
    pSerialiser.append("<li>");
    int lMaxTrackDisplay = Math.min(TrackUtils.MAX_RECENT_TRACKS, 3); // TODO - PN - Hacked to 3 from TrackUtils.MAX_RECENT_TRACKS for display, change if UI changes
    RequestURIBuilder lLatestTrackURIBuilder = pSerialisationContext.createURIBuilder();
    for(int i = 1; i <= lMaxTrackDisplay; i++) {

      String lTrackURI = lLatestTrackURIBuilder.setParam(LatestTrackBangHandler.LATEST_TRACK_INDEX_PARAM_NAME, Integer.toString(i)).buildBangHandlerURI(LatestTrackBangHandler.instance());

      pSerialiser.append("<a href=\"#\" onclick=\"FOXjs.openwin({url:'");
      pSerialiser.append(lTrackURI);
      pSerialiser.append("',windowOptions:'track'});return false;\">T");
      pSerialiser.append(Integer.toString(i));
      pSerialiser.append("</a>");
      if (i < lMaxTrackDisplay) {
        if (i%3 == 0) {
          pSerialiser.append("<br />");
        }
        else {
          pSerialiser.append(", ");
        }
      }
    }
    pSerialiser.append("</li>\n");
    if(lDBMSOutputList.size() > 0) {
      pSerialiser.append("<li><a href=\"#\" id=\"dbmsOutputInfo\">DBMS_OUTPUT</a></li>\n");
    }
    pSerialiser.append("</ul>\n" +
    "<div id=\"dev-toolbar-toggle-mode\" class=\"icon-circle-down\"></div>\n" +
    "<div id=\"dev-toolbar-toggle-pin\" class=\"icon-pushpin\"></div>\n" +
    "<div id=\"dev-toolbar-advanced\" class=\"icon-cog\">\n" +
    "  <ul id=\"dev-toolbar-advanced-tooltip\">\n");

    //Ouput checkboxes for dev flags
    for(DevToolbarContext.Flag lDevFlag : DevToolbarContext.Flag.values()) {
      pSerialiser.append("<li><input type=\"checkbox\" name=\"");
      pSerialiser.append(DevToolbarUtils.DEV_FLAG_FORM_NAME);
      pSerialiser.append("\" value=\"");
      pSerialiser.append(lDevFlag.toString());
      pSerialiser.append("\" id=\"devflag_");
      pSerialiser.append(lDevFlag.toString());
      pSerialiser.append("\"");
      pSerialiser.append((lDevToolbarContext.isFlagOn(lDevFlag) ? " checked=\"checked\"" : ""));
      pSerialiser.append("/><label for=\"devflag_");
      pSerialiser.append(lDevFlag.toString());
      pSerialiser.append("\">");
      pSerialiser.append(lDevFlag.getDisplayKey());
      pSerialiser.append("</label>");

      if(lDevFlag == DevToolbarContext.Flag.TRACK_UNATTACHED_LABEL) {
        pSerialiser.append("<input type=\"text\" id=\"");
        pSerialiser.append(DevToolbarUtils.TRACK_UNATTACHED_LABEL_NAME);
        pSerialiser.append("\" name=\"");
        pSerialiser.append(DevToolbarUtils.TRACK_UNATTACHED_LABEL_NAME);
        pSerialiser.append("\" size=\"10\" value=\"");
        pSerialiser.append(XFUtil.nvl(lDevToolbarContext.getTrackedContextLabelOrNull(), ""));
        pSerialiser.append("\" />\n");
      }

      pSerialiser.append("</li>\n");
    }


    pSerialiser.append("</ul>\n" +
    "    </div>\n" +
    "    <div id=\"dev-toolbar-timing\">\n" +
    "    </div>\n" +
    "    <div id=\"dev-toolbar-messages\">\n" +
    "      <div id=\"dev-toolbar-messages-tooltip\"></div>\n" +
    "    </div>\n" +
    "  </form>\n" +
    "</div>\n" +
    "<div id=\"dev-toolbar-spacer\" class=\"short-dev-toolbar-spacer\"></div>");



    //Dump out label info so it can be picked up by JS and placed in a popover
    pSerialiser.append("<div id=\"contextLabelData\" style=\"display:none;\"><ul>");
    for(Map.Entry<String, String> lEntry : lDevToolbarContext.getContextLabelToPathMap().entrySet()) {
      pSerialiser.append("<li><strong>:{");
      pSerialiser.append(lEntry.getKey());
      pSerialiser.append("}</strong> - ");
      pSerialiser.append(lEntry.getValue());
      pSerialiser.append("</li>");
    }
    pSerialiser.append("</ul></div>");

    //Dump out DBMS OUTPUT info so it can be picked up by JS and placed in a popover
    if(lDBMSOutputList.size() > 0) {
      pSerialiser.append("<div id=\"dbmsOutputData\" style=\"display:none;\">");
      for(DBMSOutputResult lDBMSOutput : lDBMSOutputList) {
        pSerialiser.append("<strong>");
        pSerialiser.append(lDBMSOutput.getStatementName());
        pSerialiser.append("</strong><br/><span style=\"font-size: 0.8em\">Match @");
        pSerialiser.append(lDBMSOutput.getMatchRef());
        pSerialiser.append("</span>");
        pSerialiser.append("<pre style=\"font-family: monospace; margin-top: 0.5em;\">");
        pSerialiser.append(lDBMSOutput.getOutputString());
        pSerialiser.append("</pre>");
      }
      pSerialiser.append("</div>");
    }

    //Append devtoolbar JS
    pSerialiser.append("<script type=\"text/javascript\" src=\"");
    pSerialiser.append(pSerialisationContext.getStaticResourceURI("js/jquery-hotkeys.js"));
    pSerialiser.append("\"></script>");

    pSerialiser.append("<script type=\"text/javascript\" src=\"");
    pSerialiser.append(pSerialisationContext.getStaticResourceURI("js/dev-toolbar.js"));
    pSerialiser.append("\"></script>");

    pSerialiser.append("<link rel=\"stylesheet\" type=\"text/css\" href=\"");
    pSerialiser.append(pSerialisationContext.getStaticResourceURI("css/dev-toolbar.css"));
    pSerialiser.append("\">");

    pSerialisationContext.addConditionalLoadJavascript("DevToolbar.gTrackId = '" +  Track.currentTrackId() + "';\n" +
      "DevToolbar.gBangUrlPrefix = '" + pSerialisationContext.createURIBuilder().buildServletURI(BangHandlerServlet.getServletPath()) + "/';\n" +
      "DevToolbar.processOnLoad();\n" +
      "DevToolbar.setContextLabelInfo($('#contextLabelData > *'));\n" +
      "DevToolbar.setDbmsOutputInfo($('#dbmsOutputData *'));\n");

    // No children to process
  }
}
