package net.foxopen.fox.module.serialiser.components.html;


import net.foxopen.fox.download.DownloadLinkXDoResult;
import net.foxopen.fox.entrypoint.servlets.FoxMainServlet;
import net.foxopen.fox.ex.ExInternal;
import net.foxopen.fox.module.datadefinition.EvaluatedDataDefinition;
import net.foxopen.fox.module.evaluatedattributeresult.StringAttributeResult;
import net.foxopen.fox.module.fieldset.InternalHiddenField;
import net.foxopen.fox.module.parsetree.EvaluatedModalPopover;
import net.foxopen.fox.module.parsetree.evaluatedpresentationnode.EvaluatedHtmlPresentationNode;
import net.foxopen.fox.module.parsetree.presentationnode.HtmlPresentationNode;
import net.foxopen.fox.module.serialiser.SerialisationContext;
import net.foxopen.fox.module.serialiser.components.ComponentBuilder;
import net.foxopen.fox.module.serialiser.components.ComponentBuilderType;
import net.foxopen.fox.module.serialiser.fragmentbuilder.MustacheFragmentBuilder;
import net.foxopen.fox.module.serialiser.html.HTMLAlertSerialiser;
import net.foxopen.fox.module.serialiser.html.HTMLSerialiser;
import net.foxopen.fox.module.serialiser.widgets.html.FileWidgetBuilder;
import net.foxopen.fox.thread.FocusResult;
import net.foxopen.fox.thread.PopupXDoResult;
import net.foxopen.fox.thread.devtoolbar.DevToolbarUtils;
import net.foxopen.fox.thread.stack.transform.ModelessCall.ModelessPopup;
import net.foxopen.fox.track.Track;
import net.foxopen.fox.track.TrackFlag;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * Build HTML elements for the pages.
 * This class also contains the logic for inserting JS/CSS at the right moments and building up for the form
 *
 * TODO - NP - Really need to get some mustache up in here now it's out of the serialiser
 */
public class HTMLComponentBuilder extends ComponentBuilder<HTMLSerialiser, EvaluatedHtmlPresentationNode> {
  private static final ComponentBuilder<HTMLSerialiser, EvaluatedHtmlPresentationNode> INSTANCE = new HTMLComponentBuilder();

  private static final String HIDDEN_FORM_ELEMENTS_TEMPLATE = "html/HiddenFormElements.mustache";
  public static final String MODAL_POPOVER_TEMPLATE = "html/ModalPopover.mustache";

  /**
   * List of Void Elements, html tags to force leaving open, no self close and no closing tag needed. These should throw
   * an error if they have children also.
   *
   * @see <a href="http://www.w3.org/TR/html-markup/syntax.html#void-element">http://www.w3.org/TR/html-markup/syntax.html#void-element</a>
   */
  private static final List<String> VOID_ELEMENTS = new ArrayList<>(Arrays.asList("br", "hr", "img", "input", "link", "meta", "area", "base", "col", "command", "embed", "keygen",
                                                                                  "param", "source", "track", "wbr"));

  private static final List<String> IGNORE_ATTRIBUTES = new ArrayList<>(Arrays.asList(HtmlPresentationNode.FORCE_SELF_CLOSE_TAG_NAME));

  private static final String DEFAULT_WINDOW_OPTIONS = "default";

  public static final ComponentBuilder<HTMLSerialiser, EvaluatedHtmlPresentationNode> getInstance() {
    return INSTANCE;
  }

  private HTMLComponentBuilder() {
  }

  @Override
  public void buildComponent(SerialisationContext pSerialisationContext, HTMLSerialiser pSerialiser, EvaluatedHtmlPresentationNode pEvalNode) {

    String lTagName = pEvalNode.getTagName();
    int lChildCount = pEvalNode.getChildren().size();
    boolean lVoidElement = VOID_ELEMENTS.contains(lTagName);
    if (lVoidElement && lChildCount > 0) {
      throw new ExInternal("Found a void element with content: " + lTagName);
    }
    if (pEvalNode.isForceSelfCloseTag() && lVoidElement) {
      Track.info("BadMarkup", "Found a forceSelfCloseTag attribute on an element that HTML5 says is a Void Element: " + lTagName, TrackFlag.BAD_MARKUP);
    }

    // Output HTML Opening tag
    pSerialiser.append("<");
    pSerialiser.append(lTagName);

    // add attribute for the element
    Map<String, StringAttributeResult> lAttributeMap = pEvalNode.getAttributeMap();
    ATTRIBUTE_LOOP:
    for (Map.Entry<String, StringAttributeResult> lEntry : lAttributeMap.entrySet()) {
      if (IGNORE_ATTRIBUTES.contains(lEntry.getKey())) {
        continue ATTRIBUTE_LOOP;
      }

      pSerialiser.append(" ");
      pSerialiser.append(lEntry.getKey());
      pSerialiser.append("=\"");
      pSerialiser.append(pSerialiser.getSafeStringAttribute(lEntry.getValue()));
      pSerialiser.append("\"");
    } // end ATTRIBUTE_LOOP

    if ((lChildCount == 0 && pEvalNode.isForceSelfCloseTag()) && !lVoidElement) {
      // Make this tag self-closing if it has no content and a developer asked for it and the element isn't a void element
      pSerialiser.append("/");
    }

    pSerialiser.append(">");

    // If we just opened the body tag...
    if ("body".equals(lTagName)) {
      // Set this flag in a hacky way so that debug info knows it can show up finally (inlude container debug shows up in title otherwise)
      pSerialiser.setInBody(true);

      // Include the dev toolbar for dev/support users
      if(DevToolbarUtils.isDevToolbarEnabled(pSerialisationContext)) {
        pSerialiser.getComponentBuilder(ComponentBuilderType.DEV_TOOLBAR).buildComponent(pSerialisationContext, pSerialiser, null);
      }

      // Include the skiplinks HTML
      pSerialiser.getComponentBuilder(ComponentBuilderType.SKIP_LINKS).buildComponent(pSerialisationContext, pSerialiser, null);

      // Include blocking overlay iframe
      //    Works around IE rendering issues with select widgets
      pSerialiser.append("<iframe id=\"iframe-wrapper\" src=\"javascript:'';\" style=\"border: none; overflow: hidden; visibility: hidden; display: none;\">&nbsp;</iframe>");

      //Large dropzone for single upload field
      FileWidgetBuilder.insertSingleDropzone(pSerialisationContext, pSerialiser);

      // Open the general FOX form
      // TODO - NP - Dose of mustache needed
      pSerialiser.append("<form method=\"post\" name=\"mainForm\" action=\"");
      pSerialiser.append(FoxMainServlet.buildFormPostDestinationURI(pSerialisationContext.createURIBuilder(), pSerialisationContext.getApp().getAppMnem()));
      pSerialiser.append("\" onSubmit=\"javascript: return false;\"");
      // Set autocomplete off if set with a attribute at RM/State level
      String lAutoComplete = pSerialisationContext.getState().getStateAttributes().get("form-autocomplete");
      if (lAutoComplete != null && "OFF".equals(lAutoComplete.toUpperCase())) {
        pSerialiser.append(" autocomplete=\"off\"");
      }
      pSerialiser.append(" accept-charset=\"UTF-8\">");

      // Include hidden fields
      insertHiddenFormElements(pSerialiser, pSerialisationContext);

      //Insert modal popover container div if a popover is active
      insertModalPopover(pSerialiser, pSerialisationContext);
    }

    processChildren(pSerialisationContext, pSerialiser, pEvalNode);

    if ("head".equals(lTagName)) {
      // If closing a head tag, insert scripts and styles first
      pSerialiser.getComponentBuilder(ComponentBuilderType.HEADER_RESOURCES).buildComponent(pSerialisationContext, pSerialiser, pEvalNode);
    }
    else if ("body".equals(lTagName)) {
      pSerialiser.append("</form>");

      //JS files etc which go at the bottom of the page to speed up rendering
      pSerialiser.getComponentBuilder(ComponentBuilderType.FOOTER_RESOURCES).buildComponent(pSerialisationContext, pSerialiser, pEvalNode);

      //Insert hidden alert buffers so they can be referenced by alert JS
      HTMLAlertSerialiser.insertAlertBuffers(pSerialiser, pSerialisationContext);

      // Add JS, process onload, alerts, downloads, modals...
      insertJavascript(pSerialiser, pSerialisationContext);
    }

    // Close the tag if the element had children or if it wasn't self closing and it wasn't a void element (see: http://www.w3.org/TR/html-markup/syntax.html#void-element)
    if ((lChildCount > 0 || !pEvalNode.isForceSelfCloseTag()) && !lVoidElement) {
      pSerialiser.append("</");
      pSerialiser.append(lTagName);
      pSerialiser.append(">");
    }
  }



  /**
   * Include Javascript that was buffered up on the evaluated parse tree up to this point. This method should be called
   * at the end of a page, when closing the body for example. If called earlier it might not contain all the JS the page
   * needs yet.
   *
   * @param pSerialiser
   * @param pSerialisationContext
   */
  private void insertJavascript(HTMLSerialiser pSerialiser, SerialisationContext pSerialisationContext) {
    // TODO - NP - Dose of mustache needed
    pSerialiser.append("<script type=\"text/javascript\">");
    pSerialiser.append("$(document).ready( function(){ FOXjs.init(function(){");
    // If init succeeds run this block
    HTMLAlertSerialiser.insertAlerts(pSerialiser, pSerialisationContext);
    insertModelessPopups(pSerialiser, pSerialisationContext);
    insertDownloadLinks(pSerialiser, pSerialisationContext);
    insertPopupLinks(pSerialiser, pSerialisationContext);
    insertFocusJS(pSerialiser, pSerialisationContext);
    insertImplicatedDataDefinitions(pSerialiser, pSerialisationContext);
    for (String lJS : pSerialisationContext.getConditionalLoadJavascript()) {
      pSerialiser.append(lJS);
      pSerialiser.append("\n");
    }
    pSerialiser.append("});"); // End init success function and init call
    // Run code on document ready that doesn't rely on init being successful
    for (String lJS : pSerialisationContext.getUnconditionalLoadJavascript()) {
      pSerialiser.append(lJS + "\n");
    }
    pSerialiser.append("});"); // End document ready function

    pSerialiser.append("</script>");
  }
  private void insertDownloadLinks(HTMLSerialiser pSerialiser, SerialisationContext pSerialisationContext) {

    //TODO PN - download JS needs improving

    List<DownloadLinkXDoResult> lDownloadLinks = pSerialisationContext.getDownloadLinks();

    if (lDownloadLinks.size() > 0) {
      pSerialiser.append("var lPopUpURLList = new Array();\n");
    }

    final String WINDOW_TYPE = "FILE";

    for(DownloadLinkXDoResult lLink : lDownloadLinks) {
      pSerialiser.append("listSize = lPopUpURLList.length;\n")
        .append("lPopUpURLList[listSize] = new Array(3);\n")
        .append("lPopUpURLList[listSize][1] = \"" + WINDOW_TYPE + "\";\n")
        .append("lPopUpURLList[listSize][2] = \"" + lLink.getDownloadURL() + "\";\n")
        .append("lPopUpURLList[listSize][3] = \"" + lLink.getFilename() + "\";\n")
        .append("lPopUpURLList[listSize][4] = " + ("\"\"") + ";\n") //window features
        .append("lPopUpURLList[listSize][5] = " + ("\"\"") + ";\n"); //window type
    }

    if (lDownloadLinks.size() > 0) {
      pSerialiser.append("downloadDoDownloads(lPopUpURLList);\n");
    }
  }

  /**
   * Inserts JS to do window popups.
   * @param pSerialiser
   * @param pSerialisationContext
   */
  private void insertPopupLinks(HTMLSerialiser pSerialiser, SerialisationContext pSerialisationContext) {

    List<PopupXDoResult> lPopups = pSerialisationContext.getPopups();
    for(PopupXDoResult lPopup : lPopups) {
      pSerialiser.append(HTMLSerialiser.buildFOXjsOpenWinJSON(lPopup.getURI(), lPopup.getWindowName(), DEFAULT_WINDOW_OPTIONS, lPopup.getWindowFeatures()));
    }
  }

  /**
   * Get modeless popup window requests from the EvaluatedParseTree and serialise them for FOXjs to open
   *
   * @param pSerialiser
   * @param pSerialisationContext
   */
  private void insertModelessPopups(HTMLSerialiser pSerialiser, SerialisationContext pSerialisationContext) {

    List<ModelessPopup> lPopups = pSerialisationContext.getModelessPopups();
    for(ModelessPopup lPopup : lPopups) {
      pSerialiser.append(HTMLSerialiser.buildFOXjsOpenWinJSON(lPopup.getEntryURI(pSerialisationContext.createURIBuilder()), lPopup.getWindowName(), DEFAULT_WINDOW_OPTIONS, lPopup.getWindowProperties()));
    }
  }

  private void insertFocusJS(HTMLSerialiser pSerialiser, SerialisationContext pSerialisationContext) {
    FocusResult lFocusResult = pSerialisationContext.getFocusResult();
    if(lFocusResult != null) {
      String lExternalFoxId = pSerialisationContext.getFieldSet().getExternalFoxId(lFocusResult.getNodeRef());
      pSerialiser.append("FOXjs.focus('" + lExternalFoxId + "', " + lFocusResult.getScrollYOffset() + ");");
    }
  }

  private void insertImplicatedDataDefinitions(HTMLSerialiser pSerialiser, SerialisationContext pSerialisationContext) {
    for (EvaluatedDataDefinition edd : pSerialisationContext.getEvaluatedDataDefinitions()) {
      pSerialiser.append("FOXdata.registerFoxData(\"");
      pSerialiser.append(edd.getDataDefinitionName());
      pSerialiser.append("\", ");

      Map<String, JSONArray> lTransformedDataMap = edd.getTransformedData();
      if (lTransformedDataMap.size() == 1 && lTransformedDataMap.containsKey(EvaluatedDataDefinition.DEFAULT_INTERNAL_FOX_DATA_KEY)) {
        try {
          lTransformedDataMap.get(EvaluatedDataDefinition.DEFAULT_INTERNAL_FOX_DATA_KEY).writeJSONString(pSerialiser.getWriter());
        }
        catch (IOException e) {
          throw new ExInternal("Failed to write out data JSON", e);
        }
      }
      else {
        // If there were multiple entries register them as an object with key/data properties
        JSONObject lDataTuple = new JSONObject();
        for (Map.Entry<String, JSONArray> lTransformedData : lTransformedDataMap.entrySet()) {
          lDataTuple.put(lTransformedData.getKey(), lTransformedData.getValue());
        }

        try {
          lDataTuple.writeJSONString(pSerialiser.getWriter());
        }
        catch (IOException e) {
          throw new ExInternal("Failed to write out data JSON", e);
        }
      }

      pSerialiser.append(");");
    }
  }

  /**
   * Insert the hidden form elements that FOX uses to keep state
   *
   * @param pSerialiser
   * @param pSerialisationContext
   */
  private void insertHiddenFormElements(HTMLSerialiser pSerialiser, SerialisationContext pSerialisationContext) {
    Map<String, Object> lTemplateVars = new HashMap<>();
    lTemplateVars.put("ThreadID", pSerialisationContext.getThreadInfoProvider().getThreadId());
    lTemplateVars.put("AppMnem", pSerialisationContext.getThreadInfoProvider().getThreadAppMnem());
    lTemplateVars.put("FieldSetLabel", pSerialisationContext.getFieldSet().getOutwardFieldSetLabel());
    lTemplateVars.put("ScrollPosition", pSerialisationContext.getThreadInfoProvider().getScrollPosition());
    lTemplateVars.put("CurrentCallID", pSerialisationContext.getThreadInfoProvider().getCurrentCallId());

    MustacheFragmentBuilder.applyMapToTemplate(HIDDEN_FORM_ELEMENTS_TEMPLATE, lTemplateVars, pSerialiser.getWriter());
  }

  /**
   * Inserts the modal popover div, including its rendered contents, if a modal popover is active. This must be as soon as possible
   * during HTML generation so the page is rendered with the popover content and greyout div from the start.
   * @param pSerialiser Current Serialiser.
   * @param pSerialisationContext Current SerialisationContext.
   */
  private void insertModalPopover(HTMLSerialiser pSerialiser, SerialisationContext pSerialisationContext) {

    Map<String, Object> lTemplateMap = new HashMap<>();

    EvaluatedModalPopover lEvaluatedPopover = pSerialisationContext.getCurrentModalPopoverOrNull();

    if(lEvaluatedPopover != null) {
      //Render the popover's buffer into a temp serialiser
      HTMLSerialiser.HTMLTempSerialiser lTempSerialiser = pSerialiser.getTempSerialiser();
      lEvaluatedPopover.render(pSerialisationContext, lTempSerialiser);

      lTemplateMap.put("content", lTempSerialiser.getOutput());
      lTemplateMap.put("scroll-position", lEvaluatedPopover.getModalPopover().getScrollPosition());

      //Get additional template variables from the modal popover
      lTemplateMap.putAll(lEvaluatedPopover.getModalPopover().getModalPopoverOptions().asMustacheTemplatePropertyMap());

      //Write input element for the hidden modal scroll position field (note the field's sending value should equal the current scroll position)
      InternalHiddenField lHiddenField = lEvaluatedPopover.getScrollPositionHiddenField();
      pSerialiser.append("<input type=\"hidden\" name=\"" + lHiddenField.getExternalFieldName() + "\" value=\"" + lHiddenField.getSendingValue() + "\"/>");

      //Insert the popover divs
      MustacheFragmentBuilder.applyMapToTemplate(MODAL_POPOVER_TEMPLATE, lTemplateMap, pSerialiser.getWriter());
    }
  }
}
