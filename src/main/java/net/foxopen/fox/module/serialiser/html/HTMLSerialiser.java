package net.foxopen.fox.module.serialiser.html;

import net.foxopen.fox.XFUtil;
import net.foxopen.fox.entrypoint.FoxGlobals;
import net.foxopen.fox.entrypoint.servlets.ErrorServlet;
import net.foxopen.fox.entrypoint.uri.RequestURIBuilder;
import net.foxopen.fox.ex.ExInternal;
import net.foxopen.fox.module.OutputHint;
import net.foxopen.fox.module.datanode.EvaluatedNode;
import net.foxopen.fox.module.datanode.NodeVisibility;
import net.foxopen.fox.module.evaluatedattributeresult.StringAttributeResult;
import net.foxopen.fox.module.fieldset.action.InternalActionContext;
import net.foxopen.fox.module.parsetree.EvaluatedParseTree;
import net.foxopen.fox.module.parsetree.evaluatedpresentationnode.EvaluatedPresentationNode;
import net.foxopen.fox.module.serialiser.HtmlDoctype;
import net.foxopen.fox.module.serialiser.TempSerialiser;
import net.foxopen.fox.module.serialiser.WriterOutputSerialiser;
import net.foxopen.fox.module.serialiser.components.ComponentBuilder;
import net.foxopen.fox.module.serialiser.components.ComponentBuilderType;
import net.foxopen.fox.module.serialiser.components.html.ActionOutComponentBuilder;
import net.foxopen.fox.module.serialiser.components.html.BufferComponentBuilder;
import net.foxopen.fox.module.serialiser.components.html.ContainerComponentBuilder;
import net.foxopen.fox.module.serialiser.components.html.DevToolbarComponentBuilder;
import net.foxopen.fox.module.serialiser.components.html.ExprOutComponentBuilder;
import net.foxopen.fox.module.serialiser.components.html.ExternalURLComponentBuilder;
import net.foxopen.fox.module.serialiser.components.html.GridCellComponentBuilder;
import net.foxopen.fox.module.serialiser.components.html.GridComponentBuilder;
import net.foxopen.fox.module.serialiser.components.html.GridRowComponentBuilder;
import net.foxopen.fox.module.serialiser.components.html.HTMLComponentBuilder;
import net.foxopen.fox.module.serialiser.components.html.HeaderResourcesComponentBuilder;
import net.foxopen.fox.module.serialiser.components.html.HeadingComponentBuilder;
import net.foxopen.fox.module.serialiser.components.html.HintOutComponentBuilder;
import net.foxopen.fox.module.serialiser.components.html.InfoBoxComponentBuilder;
import net.foxopen.fox.module.serialiser.components.html.MailToComponentBuilder;
import net.foxopen.fox.module.serialiser.components.html.MenuOutComponentBuilder;
import net.foxopen.fox.module.serialiser.components.html.PagerControlComponentBuilder;
import net.foxopen.fox.module.serialiser.components.html.SetOutComponentBuilder;
import net.foxopen.fox.module.serialiser.components.html.SkipLinksComponentBuilder;
import net.foxopen.fox.module.serialiser.components.html.TabGroupComponentBuilder;
import net.foxopen.fox.module.serialiser.components.html.TextComponentBuilder;
import net.foxopen.fox.module.serialiser.components.html.UnimplementedComponentBuilder;
import net.foxopen.fox.module.serialiser.components.html.WidgetOutComponentBuilder;
import net.foxopen.fox.module.serialiser.widgets.WidgetBuilder;
import net.foxopen.fox.module.serialiser.widgets.WidgetBuilderType;
import net.foxopen.fox.module.serialiser.widgets.html.ButtonWidgetBuilder;
import net.foxopen.fox.module.serialiser.widgets.html.CaptchaWidgetBuilder;
import net.foxopen.fox.module.serialiser.widgets.html.CellmateWidgetBuilder;
import net.foxopen.fox.module.serialiser.widgets.html.DateWidgetBuilder;
import net.foxopen.fox.module.serialiser.widgets.html.ErrorRefWidgetBuilder;
import net.foxopen.fox.module.serialiser.widgets.html.FileWidgetBuilder;
import net.foxopen.fox.module.serialiser.widgets.html.FormWidgetBuilder;
import net.foxopen.fox.module.serialiser.widgets.html.HTMLWidgetBuilder;
import net.foxopen.fox.module.serialiser.widgets.html.ImageWidgetBuilder;
import net.foxopen.fox.module.serialiser.widgets.html.InputWidgetBuilder;
import net.foxopen.fox.module.serialiser.widgets.html.LinkWidgetBuilder;
import net.foxopen.fox.module.serialiser.widgets.html.ListWidgetBuilder;
import net.foxopen.fox.module.serialiser.widgets.html.MailToWidgetBuilder;
import net.foxopen.fox.module.serialiser.widgets.html.PasswordWidgetBuilder;
import net.foxopen.fox.module.serialiser.widgets.html.PhantomBufferWidgetBuilder;
import net.foxopen.fox.module.serialiser.widgets.html.PhantomMenuWidgetBuilder;
import net.foxopen.fox.module.serialiser.widgets.html.PrintWidgetBuilder;
import net.foxopen.fox.module.serialiser.widgets.html.RadioWidgetBuilder;
import net.foxopen.fox.module.serialiser.widgets.html.SearchSelectorWidgetBuilder;
import net.foxopen.fox.module.serialiser.widgets.html.SelectorWidgetBuilder;
import net.foxopen.fox.module.serialiser.widgets.html.StaticTextWidgetBuilder;
import net.foxopen.fox.module.serialiser.widgets.html.SubmitWidgetBuilder;
import net.foxopen.fox.module.serialiser.widgets.html.TextWidgetBuilder;
import net.foxopen.fox.module.serialiser.widgets.html.TickboxWidgetBuilder;
import net.foxopen.fox.module.serialiser.widgets.html.TimerWidgetBuilder;
import net.foxopen.fox.module.serialiser.widgets.html.URLWidgetBuilder;
import net.foxopen.fox.module.serialiser.widgets.html.UnimplementedWidgetBuilder;
import net.foxopen.fox.track.Track;
import org.apache.commons.lang3.StringEscapeUtils;
import org.json.simple.JSONObject;

import java.io.StringWriter;
import java.io.Writer;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;


//TODO - NP - Script includes at the end to speed up perception of load times
//            Widget Builders
//
//
//

public class HTMLSerialiser
extends WriterOutputSerialiser {

  static private final Map<WidgetBuilderType, WidgetBuilder<HTMLSerialiser, ? extends EvaluatedNode>> HTML_WIDGET_MAP = new EnumMap<>(WidgetBuilderType.class);
  static private final Map<ComponentBuilderType, ComponentBuilder<HTMLSerialiser, ? extends EvaluatedPresentationNode>> HTML_PAGE_COMPONENT_MAP = new EnumMap<>(ComponentBuilderType.class);
  static {
    HTML_WIDGET_MAP.put(WidgetBuilderType.FORM, FormWidgetBuilder.getInstance());
    HTML_WIDGET_MAP.put(WidgetBuilderType.LIST, ListWidgetBuilder.getInstance());
    HTML_WIDGET_MAP.put(WidgetBuilderType.PHANTOM_BUFFER, PhantomBufferWidgetBuilder.getInstance());
    HTML_WIDGET_MAP.put(WidgetBuilderType.PHANTOM_MENU, PhantomMenuWidgetBuilder.getInstance());
    HTML_WIDGET_MAP.put(WidgetBuilderType.INPUT, InputWidgetBuilder.getInstance());
    HTML_WIDGET_MAP.put(WidgetBuilderType.PASSWORD, PasswordWidgetBuilder.getInstance());
    HTML_WIDGET_MAP.put(WidgetBuilderType.SELECTOR, SelectorWidgetBuilder.getInstance());
    HTML_WIDGET_MAP.put(WidgetBuilderType.SEARCH_SELECTOR, SearchSelectorWidgetBuilder.getInstance());
    HTML_WIDGET_MAP.put(WidgetBuilderType.TICKBOX, TickboxWidgetBuilder.getInstance());
    HTML_WIDGET_MAP.put(WidgetBuilderType.RADIO, RadioWidgetBuilder.getInstance());
    HTML_WIDGET_MAP.put(WidgetBuilderType.BUTTON, ButtonWidgetBuilder.getInstance());
    HTML_WIDGET_MAP.put(WidgetBuilderType.SUBMIT, SubmitWidgetBuilder.getInstance());
    HTML_WIDGET_MAP.put(WidgetBuilderType.LINK, LinkWidgetBuilder.getInstance());
    HTML_WIDGET_MAP.put(WidgetBuilderType.URL, URLWidgetBuilder.getInstance());
    HTML_WIDGET_MAP.put(WidgetBuilderType.DATE, DateWidgetBuilder.getInstance());
    HTML_WIDGET_MAP.put(WidgetBuilderType.DATE_TIME, DateWidgetBuilder.getInstance());
    HTML_WIDGET_MAP.put(WidgetBuilderType.MAILTO, MailToWidgetBuilder.getInstance());
    HTML_WIDGET_MAP.put(WidgetBuilderType.TEXT, TextWidgetBuilder.getInstance());
    HTML_WIDGET_MAP.put(WidgetBuilderType.STATIC_TEXT, StaticTextWidgetBuilder.getInstance());
    HTML_WIDGET_MAP.put(WidgetBuilderType.CAPTCHA, CaptchaWidgetBuilder.getInstance());
    HTML_WIDGET_MAP.put(WidgetBuilderType.PRINT, PrintWidgetBuilder.getInstance());
    HTML_WIDGET_MAP.put(WidgetBuilderType.TIMER, TimerWidgetBuilder.getInstance());
    HTML_WIDGET_MAP.put(WidgetBuilderType.HTML, HTMLWidgetBuilder.getInstance());
    HTML_WIDGET_MAP.put(WidgetBuilderType.FILE, FileWidgetBuilder.getInstance());
    HTML_WIDGET_MAP.put(WidgetBuilderType.IMAGE, ImageWidgetBuilder.getInstance());
    HTML_WIDGET_MAP.put(WidgetBuilderType.CELLMATES, CellmateWidgetBuilder.getInstance());
    HTML_WIDGET_MAP.put(WidgetBuilderType.ERROR_REF, ErrorRefWidgetBuilder.getInstance());

    HTML_PAGE_COMPONENT_MAP.put(ComponentBuilderType.DEV_TOOLBAR, DevToolbarComponentBuilder.getInstance());
    HTML_PAGE_COMPONENT_MAP.put(ComponentBuilderType.SKIP_LINKS, SkipLinksComponentBuilder.getInstance());
    HTML_PAGE_COMPONENT_MAP.put(ComponentBuilderType.HEADER_RESOURCES, HeaderResourcesComponentBuilder.getInstance());
    HTML_PAGE_COMPONENT_MAP.put(ComponentBuilderType.HTML_TAG, HTMLComponentBuilder.getInstance());
    HTML_PAGE_COMPONENT_MAP.put(ComponentBuilderType.TEXT, TextComponentBuilder.getInstance());
    HTML_PAGE_COMPONENT_MAP.put(ComponentBuilderType.COMMENT, TextComponentBuilder.getInstance());
    HTML_PAGE_COMPONENT_MAP.put(ComponentBuilderType.EXPR_OUT, ExprOutComponentBuilder.getInstance());
    HTML_PAGE_COMPONENT_MAP.put(ComponentBuilderType.BUFFER, BufferComponentBuilder.getInstance());
    HTML_PAGE_COMPONENT_MAP.put(ComponentBuilderType.ACTION_OUT, ActionOutComponentBuilder.getInstance());
    HTML_PAGE_COMPONENT_MAP.put(ComponentBuilderType.MENU_OUT, MenuOutComponentBuilder.getInstance());
    HTML_PAGE_COMPONENT_MAP.put(ComponentBuilderType.SET_OUT, SetOutComponentBuilder.getInstance());
    HTML_PAGE_COMPONENT_MAP.put(ComponentBuilderType.NODE_CONTAINER, ContainerComponentBuilder.getInstance());
    HTML_PAGE_COMPONENT_MAP.put(ComponentBuilderType.HINT_OUT, HintOutComponentBuilder.getInstance());
    HTML_PAGE_COMPONENT_MAP.put(ComponentBuilderType.EXTERNAL_URL, ExternalURLComponentBuilder.getInstance());
    HTML_PAGE_COMPONENT_MAP.put(ComponentBuilderType.HEADING, HeadingComponentBuilder.getInstance());
    HTML_PAGE_COMPONENT_MAP.put(ComponentBuilderType.MAILTO, MailToComponentBuilder.getInstance());
    HTML_PAGE_COMPONENT_MAP.put(ComponentBuilderType.GRID, GridComponentBuilder.getInstance());
    HTML_PAGE_COMPONENT_MAP.put(ComponentBuilderType.GRID_ROW, GridRowComponentBuilder.getInstance());
    HTML_PAGE_COMPONENT_MAP.put(ComponentBuilderType.GRID_CELL, GridCellComponentBuilder.getInstance());
    HTML_PAGE_COMPONENT_MAP.put(ComponentBuilderType.INFO_BOX, InfoBoxComponentBuilder.getInstance());
    HTML_PAGE_COMPONENT_MAP.put(ComponentBuilderType.WIDGET_OUT, WidgetOutComponentBuilder.getInstance());
    HTML_PAGE_COMPONENT_MAP.put(ComponentBuilderType.PAGER_CONTROL, PagerControlComponentBuilder.getInstance());
    HTML_PAGE_COMPONENT_MAP.put(ComponentBuilderType.TAB_GROUP, TabGroupComponentBuilder.getGroupBuilderInstance());
    HTML_PAGE_COMPONENT_MAP.put(ComponentBuilderType.TAB_PROMPT, TabGroupComponentBuilder.getPromptBuilderInstance());
  }

  private static final HtmlDoctype DEFAULT_DOCTYPE = HtmlDoctype.HTML5;

  private final EvaluatedParseTree mEvalParseTree;

  private boolean mInBody = false;
  private boolean mHeaderResourcesIncluded = false;
  private int mDebugCount = 0;

  public HTMLSerialiser(EvaluatedParseTree pEvalParseTree) {
    mEvalParseTree = pEvalParseTree;
  }

  public void serialise(Writer pWriter) {
    Track.pushInfo("HTMLOutputSerialiser", "Serialising HTML output");

    try {
      if(pWriter == null) {
        throw new ExInternal("Cannot pass in a null or closed writer");
      }

      setWriter(pWriter);

      // Output doctype
      HtmlDoctype lDoctypeDeclaration = XFUtil.nvl(mEvalParseTree.getState().getDocumentType(), XFUtil.nvl(mEvalParseTree.getModule().getDocumentType(), XFUtil.nvl(mEvalParseTree.getModule().getApp().getDefaultDocumentType(), DEFAULT_DOCTYPE)));
      append(lDoctypeDeclaration.getDoctypeDeclaration());

      // Recursive serialisation
      mEvalParseTree.getRootBuffer().render(mEvalParseTree, this);
    }
    finally {
      Track.pop("HTMLOutputSerialiser");
    }
  }

  @Override
  public WidgetBuilder<HTMLSerialiser, EvaluatedNode> getWidgetBuilder(WidgetBuilderType pWidgetBuilderType) {
    WidgetBuilder lMapResult = HTML_WIDGET_MAP.get(pWidgetBuilderType);
    if (lMapResult != null) {
      return lMapResult;
    }

    return UnimplementedWidgetBuilder.getInstance();
  }

  @Override
  public ComponentBuilder<HTMLSerialiser, EvaluatedPresentationNode> getComponentBuilder(ComponentBuilderType pComponentEnum) {
    ComponentBuilder lMapResult = HTML_PAGE_COMPONENT_MAP.get(pComponentEnum);
    if (lMapResult != null) {
      return lMapResult;
    }

    return UnimplementedComponentBuilder.getInstance();
  }

  @Override
  public void addHint(OutputHint pHint, String pTargetID, boolean pAddIcon) {
    if (pHint != null) {
      String lHintContent = getSafeStringAttribute(pHint.getContent());
      String lHintDescription = getSafeStringAttribute(pHint.getDescription());
      if (!XFUtil.isNull(lHintDescription)) {
        // TODO - NP/PN - Add in the hint/description concatenator again possibly? Though this perhaps is best left handled by the hint-code
        lHintContent += "<br/><br/>" + lHintDescription;
      }
      String lHintTitle = getSafeStringAttribute(pHint.getTitle());

      if (pAddIcon) {
        if (XFUtil.exists(pHint.getHintURL())) {
          append("<a href=\"");
          append(StringEscapeUtils.escapeHtml4(pHint.getHintURL()));
          append("\">");
        }

        append("<div  id=\"");
        append(pHint.getHintID());
        append("\" title=\"");
        append(StringEscapeUtils.escapeHtml4(lHintContent));
        append("\" aria-label=\"");
        append(StringEscapeUtils.escapeHtml4(lHintContent));
        if(!XFUtil.isNull(lHintTitle)) {
          append("\" data-tooltip-title=\"");
          append(StringEscapeUtils.escapeHtml4(lHintTitle));
        }
        append("\" class=\"hint icon-info\"></div>");

        if (XFUtil.exists(pHint.getHintURL())) {
          append("</a>");
        }
      }
      else {
        JSONObject lTooltipJSON = new JSONObject();
        if (!XFUtil.isNull(lHintTitle)) {
          lTooltipJSON.put("content", "<h4>" + lHintTitle + "</h4>" + lHintContent);
        }
        else {
          lTooltipJSON.put("content", lHintContent);
        }
        mEvalParseTree.addConditionalLoadJavascript("$('#" + pTargetID + "').tooltipster(" + lTooltipJSON.toJSONString() + ");");
      }
    }
  }

  @Override
  public void addDescription(EvaluatedNode pEvaluatedNode) {
    if(pEvaluatedNode.getFieldMgr().getVisibility().asInt() >= NodeVisibility.VIEW.asInt() && pEvaluatedNode.hasDescription()) {
      String lDescription = getSafeStringAttribute(pEvaluatedNode.getDescription());
      if(!XFUtil.isNull(lDescription)) {
        append("<div class=\"fieldDescription\">");
        append(lDescription);
        append("</div>");
      }
    }
  }

  @Override
  public void addHint(OutputHint pHint) {
    addHint(pHint, null, true);
  }

  /**
   * Set flag so that debug information is only set out if the serialiser is serialising in the body tag
   *
   * @param pInBody
   */
  public void setInBody(boolean pInBody) {
    mInBody = pInBody;
  }

  /**
   * Check flag so that debug information is only set out if the serialiser is serialising in the body tag
   *
   * @return true if serialiser has serialising elements somewhere inside the body tag
   */
  public boolean isInBody() {
    return mInBody;
  }

  public void setHeaderResourcesIncluded(boolean pHeaderResourcesIncluded) {
    mHeaderResourcesIncluded = pHeaderResourcesIncluded;
  }

  public boolean isHeaderResourcesIncluded() {
    return mHeaderResourcesIncluded;
  }

  @Override
  public HTMLTempSerialiser getTempSerialiser() {
    return new HTMLTempSerialiser(mEvalParseTree);
  }

  /**
   * Temporary HTMLSerialiser serialiser
   */
  public static class HTMLTempSerialiser
  extends HTMLSerialiser
  implements TempSerialiser {

    StringWriter mTemp = new StringWriter();
    public HTMLTempSerialiser(EvaluatedParseTree pEvalParseTree) {
      super(pEvalParseTree);

      setWriter(mTemp);
    }

    @Override
    public String getOutput() {
      return mTemp.toString();
    }
  }

  @Override
  public String getInternalActionSubmitString(InternalActionContext pActionContext) {
    return getInternalActionSubmitString(pActionContext, Collections.<String, String>emptyMap());
  }

  @Override
  public String getInternalActionSubmitString(InternalActionContext pActionContext, Map<String, String> pParamMap) {

    String lParamJSON = "";
    if(pParamMap.size() > 0) {
      //Write the param map to a JSON string, ensure double quotes are replace to single quotes as this is going in an HTML attribute
      lParamJSON = ", params:" + JSONObject.toJSONString(pParamMap).replace("\"", "'");
    }

    return "FOXjs.action({ref:'" + pActionContext.generateActionRef() + "'" + lParamJSON +  "})";
  }

  //TODO PN move to interface?
  public static void serialiseDataAttributes(HTMLSerialiser pSerialiser, EvaluatedNode pEvalNode) {
    for(Map.Entry<String, String> lInternalAttr : pEvalNode.getCellInternalAttributes().entrySet()) {
      pSerialiser.append(" data-" + lInternalAttr.getKey() + "=\"" + lInternalAttr.getValue() + "\"");
    }
  }

  public void handleStreamingError(Throwable pThrowable, long pErrorRef, String pThreadID, String pPreviousFieldSetLabel) {

    RequestURIBuilder lURIBuilder = mEvalParseTree.createURIBuilder();

    lURIBuilder
      .setParam(ErrorServlet.ERROR_REF_ATTRIBUTE, Long.toString(pErrorRef))
      .setParam(ErrorServlet.THREAD_ID_ATTRIBUTE, pThreadID)
      .setParam(ErrorServlet.FIELDSET_ID_ATTRIBUTE, pPreviousFieldSetLabel);

    if(FoxGlobals.getInstance().canShowStackTracesOnError()) {
      // No need to add Track ID unless they can see track on the error page
      lURIBuilder.setParam(ErrorServlet.TRACK_ID_ATTRIBUTE, Track.currentTrackId());
    }

    String lErrorURL = lURIBuilder.buildServletURI(ErrorServlet.SERVLET_PATH);

    // Send out basic redirect
    append("<script>window.onload = function() {window.location.assign('");
    append(lErrorURL);
    append("');};</script>");

    // Send out error text
    append("<div class=\"streamedError\"><h1>Application Error</h1><p>The application has experienced an unexpected error. You should be redirected to an error page containing more details. <a href=\"");
    append(lErrorURL);
    append("\">Click here if you were not redirected</a>.</p>");

    // Display stack trace here if we can, to help in case of serious mishap
    if (FoxGlobals.getInstance().canShowStackTracesOnError()) {
      append("<pre>");
      append(StringEscapeUtils.escapeHtml4(XFUtil.getJavaStackTraceInfo(pThrowable)));
      append("</pre>");
    }

    append("</div>");
  }


  /**
   * Output serialisers should define a function to sanitise strings for safe output. This is then used when getting
   * attributes, such as prompts/hints, to make sure they're safe for output.
   * The implementation should call pEvalNode.getStringAttributeResultOrNull() and test lAttributeResult.isEscapingRequired()
   * to find out when application developers have requested for no safely escaped strings. By default it should be safely
   * escaped.
   *
   * @param pStringAttributeResult Attribute result with a getString() method on it that might require escaping
   * @return Evaluated value of pNodeAttribute, safely escaped for output if required
   */
  @Override
  public String getSafeStringAttribute(StringAttributeResult pStringAttributeResult) {
    if (pStringAttributeResult == null) {
      return null;
    }

    if (pStringAttributeResult.isEscapingRequired()) {
      return StringEscapeUtils.escapeHtml4(pStringAttributeResult.getString());
    }
    else {
      return pStringAttributeResult.getString();
    }
  }

  /**
   * Add some debug information to the output.
   * HTML Serialiser wraps the debug information in a span with a debug class
   *
   * @param pDebugInformation
   */
  @Override
  public void addDebugInformation(String pDebugInformation) {
    mDebugCount++;
    append("<div class=\"debug-icon icon-eye\" data-debug-id=\"debug");
    append(String.valueOf(mDebugCount));
    append("\"></div><div id=\"debug");
    append(String.valueOf(mDebugCount));
    append("\" class=\"debug-wrapper\"><div class=\"debug-container\">");
    append(pDebugInformation);
    append("</div></div>");
  }

  public static String buildFOXjsOpenWinJSON(String pURL, String pWindowOptions) {
    return buildFOXjsOpenWinJSON(pURL, pWindowOptions, null, null);
  }

  public static String buildFOXjsOpenWinJSON(String pURL, String pWindowOptions, String pWindowProperties, String pWindowName) {
    JSONObject lCallProperties = new JSONObject();
    lCallProperties.put("url", pURL);

    String lWindowOptions = pWindowOptions;
    if(!XFUtil.isNull(pWindowProperties) && !"default".equals(pWindowProperties)) {
      lWindowOptions = "custom";
      lCallProperties.put("windowProperties", pWindowProperties);
    }
    lCallProperties.put("windowOptions", lWindowOptions);

    if(!XFUtil.isNull(pWindowName)) {
      lCallProperties.put("windowName", pWindowName);
    }

    return "FOXjs.openwin(" + lCallProperties.toJSONString() + ");";
  }
}
