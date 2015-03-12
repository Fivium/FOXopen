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
    pSerialiser.append("<li id=\"dev-toolbar-flush\"><a href=\"" + lFlushURI + "\" class=\"icon-bin\">Flush</a></li>\n");

    // Refresh Link
    InternalActionContext lActionContext = pSerialisationContext.getFieldSet().addInternalAction(RefreshAction.instance());
    pSerialiser.append("<li id=\"dev-toolbar-refresh\"><a href=\"#\" class=\"icon-spinner11\" onclick=\"" + pSerialiser.getInternalActionSubmitString(lActionContext)  + ";return false;\">Refresh</a></li>\n");

    // Entry point Link
    pSerialiser.append("<li id=\"dev-toolbar-entrypoint\"><a href=\"" + lDevToolbarContext.getEntryPointURI(pSerialisationContext.createURIBuilder()) + "\" class=\"icon-enter\">Entry Point</a></li>\n");

    // Contexts Link
    pSerialiser.append("<li id=\"dev-toolbar-contexts\"><a href=\"#\" class=\"icon-link\">Contexts</a></li> \n");

    // XPath Runner
    String lXPathURI = pSerialisationContext.createURIBuilder().setParam(XPathBangHandler.THREAD_ID_PARAM, lThreadId).buildBangHandlerURI(XPathBangHandler.instance());
    pSerialiser.append("<li id=\"dev-toolbar-run-xpath\"><a href=\"#\" class=\"icon-play2\" onclick=\"FOXjs.openwin({url:'" + lXPathURI +"',windowOptions:'refwin'});return false;\">Run XPath</a></li>\n");

    pSerialiser.append("</ul>\n" +
      "<ul id=\"dev-toolbar-doms\">\n");

    RequestURIBuilder lDOMViewURIBuilder = pSerialisationContext.createURIBuilder();
    lDOMViewURIBuilder.setParam(ViewDOMBangHandler.THREAD_ID_PARAM, lThreadId);
//    lDOMViewURIBuilder.setParam(ViewDOMBangHandler.APP_MNEM_PARAM, pSerialisationContext.getThreadInfoProvider().getThreadAppMnem());

    for (String lDomName : lDevToolbarContext.getDocumentContextLabels()) {
      lDOMViewURIBuilder.setParam(ViewDOMBangHandler.DOM_NAME_PARAM, lDomName);
      String lDOMViewURI = lDOMViewURIBuilder.buildBangHandlerURI(ViewDOMBangHandler.instance());
      pSerialiser.append("<li><a href=\"#\" onclick=\"FOXjs.openwin({url:'" + lDOMViewURI + "',windowOptions:'appwin'});return false;\" id=\"" + lDomName + "-dom-link\">:{" + lDomName + "}</a></li>\n");
    }
    pSerialiser.append("</ul>\n" +
      "<ul id=\"dev-toolbar-view\">\n");

    RequestURIBuilder lDebugPageURIBuilder = pSerialisationContext.createURIBuilder();
    lDebugPageURIBuilder.setParam(DebugPageBangHandler.THREAD_ID_PARAM, lThreadId);

    //General debug page links
    for (DebugPage lDebugPage : DEBUG_PAGE_LINKS) {
      lDebugPageURIBuilder.setParam(DebugPageBangHandler.DEBUG_PAGE_TYPE_PARAM_NAME, lDebugPage.toString());
      String lDebugURI = lDebugPageURIBuilder.buildBangHandlerURI(DebugPageBangHandler.instance());
      pSerialiser.append("<li><a href=\"#\" onclick=\"FOXjs.openwin({url:'" + lDebugURI + "',windowOptions:'appwin'});return false;\">" + XFUtil.initCap(lDebugPage.toString()) + "</a></li>\n");
    }

    //Last track links
    pSerialiser.append("<li>");
    int lMaxTrackDisplay = Math.min(TrackUtils.MAX_RECENT_TRACKS, 3); // TODO - PN - Hacked to 3 from TrackUtils.MAX_RECENT_TRACKS for display, change if UI changes
    RequestURIBuilder lLatestTrackURIBuilder = pSerialisationContext.createURIBuilder();
    for(int i=1; i <= lMaxTrackDisplay; i++) {

      String lTrackURI = lLatestTrackURIBuilder.setParam(LatestTrackBangHandler.LATEST_TRACK_INDEX_PARAM_NAME, Integer.toString(i)).buildBangHandlerURI(LatestTrackBangHandler.instance());

      pSerialiser.append("<a href=\"#\" onclick=\"FOXjs.openwin({url:'" + lTrackURI + "',windowOptions:'track'});return false;\">T" + i + "</a>");
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
      pSerialiser.append("<li><input type=\"checkbox\" name=\"" + DevToolbarUtils.DEV_FLAG_FORM_NAME + "\" value=\"" + lDevFlag.toString() + "\" id=\"devflag_" + lDevFlag.toString() + "\"" +
        " " + (lDevToolbarContext.isFlagOn(lDevFlag) ? "checked=\"checked\"" : "") + "/><label for=\"devflag_" + lDevFlag.toString() + "\">" + lDevFlag.getDisplayKey() + "</label>");

      if(lDevFlag == DevToolbarContext.Flag.TRACK_UNATTACHED_LABEL) {
        pSerialiser.append("<input type=\"text\" id=\"" + DevToolbarUtils.TRACK_UNATTACHED_LABEL_NAME  + "\" name=\"" + DevToolbarUtils.TRACK_UNATTACHED_LABEL_NAME + "\" size=\"10\" value=\"" +  XFUtil.nvl(lDevToolbarContext.getTrackedContextLabelOrNull(), "") + "\" />\n");
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
      pSerialiser.append("<li><strong>:{" + lEntry.getKey() + "}</strong> - " + lEntry.getValue() + "</li>");
    }
    pSerialiser.append("</ul></div>");

    //Dump out DBMS OUTPUT info so it can be picked up by JS and placed in a popover
    if(lDBMSOutputList.size() > 0) {
      pSerialiser.append("<div id=\"dbmsOutputData\" style=\"display:none;\">");
      for(DBMSOutputResult lDBMSOutput : lDBMSOutputList) {
        pSerialiser.append("<strong>" + lDBMSOutput.getStatementName() + "</strong><br/><span style=\"font-size: 0.8em\">Match @" + lDBMSOutput.getMatchRef() + "</span>");
        pSerialiser.append("<pre style=\"font-family: monospace; margin-top: 0.5em;\">" + lDBMSOutput.getOutputString() + "</pre>");
      }
      pSerialiser.append("</div>");
    }

    //Append devtoolbar JS
    pSerialiser.append("<script type=\"text/javascript\" src=\"" + pSerialisationContext.getStaticResourceURI("js/jquery-hotkeys.js") +"\"></script>");
    pSerialiser.append("<script type=\"text/javascript\" src=\"" + pSerialisationContext.getStaticResourceURI("js/dev-toolbar.js") +"\"></script>");
    pSerialiser.append("<link rel=\"stylesheet\" type=\"text/css\" href=\"" + pSerialisationContext.getStaticResourceURI("css/dev-toolbar.css") + "\">");
    pSerialisationContext.addConditionalLoadJavascript("DevToolbar.gTrackId = '" +  Track.currentTrackId() + "';\n" +
      "DevToolbar.gBangUrlPrefix = '" + pSerialisationContext.createURIBuilder().buildServletURI(BangHandlerServlet.getServletPath()) + "/';\n" +
      "DevToolbar.processOnLoad();\n" +
      "DevToolbar.setContextLabelInfo($('#contextLabelData > *'));\n" +
      "DevToolbar.setDbmsOutputInfo($('#dbmsOutputData *'));\n");

    // No children to process
  }
}
