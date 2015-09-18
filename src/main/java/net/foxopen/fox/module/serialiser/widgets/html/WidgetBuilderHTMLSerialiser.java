package net.foxopen.fox.module.serialiser.widgets.html;

import com.google.common.base.Joiner;
import net.foxopen.fox.StringUtil;
import net.foxopen.fox.XFUtil;
import net.foxopen.fox.download.DownloadMode;
import net.foxopen.fox.download.DownloadServlet;
import net.foxopen.fox.entrypoint.uri.RequestURIBuilder;
import net.foxopen.fox.module.ActionType;
import net.foxopen.fox.module.MandatoryDisplayOption;
import net.foxopen.fox.module.datanode.EvaluatedNode;
import net.foxopen.fox.module.datanode.NodeAttribute;
import net.foxopen.fox.module.datanode.NodeVisibility;
import net.foxopen.fox.module.fieldset.fieldmgr.FieldMgr;
import net.foxopen.fox.module.parsetree.evaluatedpresentationnode.EvaluatedPresentationNode;
import net.foxopen.fox.module.parsetree.presentationnode.PresentationNode;
import net.foxopen.fox.module.serialiser.SerialisationContext;
import net.foxopen.fox.module.serialiser.TempSerialiser;
import net.foxopen.fox.module.serialiser.fragmentbuilder.MustacheFragmentBuilder;
import net.foxopen.fox.module.serialiser.html.HTMLSerialiser;
import net.foxopen.fox.module.serialiser.widgets.WidgetBuilder;
import net.foxopen.fox.module.serialiser.widgets.WidgetBuilderType;
import org.apache.commons.lang3.StringEscapeUtils;
import org.json.simple.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public abstract class WidgetBuilderHTMLSerialiser<EN extends EvaluatedNode>
extends WidgetBuilder<HTMLSerialiser, EN>  {

  protected WidgetBuilderHTMLSerialiser() {}



  @Override
  public void buildPrompt(SerialisationContext pSerialisationContext, HTMLSerialiser pSerialiser, EN pEvalNode) {
    if (hasPrompt(pEvalNode)) {
      Map<String, Object> lTemplateVars = new HashMap<>();
      lTemplateVars.put("FieldName", pEvalNode.getExternalFieldName());

      setPromptOrPromptBufferTemplateVariable(pSerialisationContext, pSerialiser, pEvalNode, lTemplateVars);

      List<String> lDefaultClasses = new ArrayList<>();
      lDefaultClasses.add("prompt");
      lDefaultClasses.add(pEvalNode.getStringAttribute(NodeAttribute.PROMPT_LAYOUT, "west"));
      if (lTemplateVars.get("PromptBuffer") != null) {
        lDefaultClasses.add("promptbuffer");
      }

      lTemplateVars.put("Class", Joiner.on(" ").skipNulls().join(lDefaultClasses));
      lTemplateVars.put("MandatoryClass", pEvalNode.getStringAttribute(NodeAttribute.MANDATORY_CLASS));
      lTemplateVars.put("MandatoryStyle", pEvalNode.getStringAttribute(NodeAttribute.MANDATORY_STYLE));
      if (pEvalNode.isMandatory() && !(MandatoryDisplayOption.OPTIONAL == pEvalNode.getMandatoryDisplay() || MandatoryDisplayOption.NONE == pEvalNode.getMandatoryDisplay())) {
        // If the node is mandatory and it's not meant to be showing optional/no mandatory hints
        lTemplateVars.put("Mandatory", pEvalNode.getStringAttribute(NodeAttribute.MANDATORY_TEXT, "*"));
      }
      else if (!pEvalNode.isMandatory() && !(MandatoryDisplayOption.MANDATORY == pEvalNode.getMandatoryDisplay() || MandatoryDisplayOption.NONE == pEvalNode.getMandatoryDisplay())) {
        // If the node is optional and it's not meant to be showing mandatory/no mandatory hints
        lTemplateVars.put("Optional", pEvalNode.getStringAttribute(NodeAttribute.OPTIONAL_TEXT, "optional"));
      }

      MustacheFragmentBuilder.applyMapToTemplate("html/Prompt.mustache", lTemplateVars, pSerialiser.getWriter());
    }
  }

  /**
   * Use this method if you need to merge action JSON with other JSON.
   *
   * @param pActionName
   * @param pContextRef
   * @return
   */
  protected JSONObject getActionJSON(String pActionName, String pContextRef) {
    JSONObject lActionObject = new JSONObject();
    lActionObject.put("ref", pActionName);
    lActionObject.put("ctxt", pContextRef);

    return lActionObject;
  }

  /**
   * Use this method to build a JSON string directly.
   *
   * @param pActionName
   * @param pContextRef
   * @param pOptionalConfirm
   * @return
   */
  protected String getActionSubmitJSONString(String pActionName, String pContextRef, String pOptionalConfirm) {
    JSONObject lActionJSON = new JSONObject();
    lActionJSON.put("ref", pActionName);
    lActionJSON.put("ctxt", pContextRef);
    if(XFUtil.exists(pOptionalConfirm)) {
      lActionJSON.put("confirm", pOptionalConfirm.replaceAll("\\\\n", "##SAFE_ESCAPE_LINEBREAK##"));
    }

    return lActionJSON.toString().replaceAll("##SAFE_ESCAPE_LINEBREAK##", "\\\\n");
  }

  private void addActionTemplateVars(SerialisationContext pSerialisationContext, EN pEvalNode, Map<String, Object> pTemplateVars) {

    ActionType lActionType = ActionType.fromExternalString(pEvalNode.getStringAttribute(NodeAttribute.ACTION_TYPE));

    if(lActionType == ActionType.DOWNLOAD) {
      //If this is a download action type, generate the download URL and serve it out directly as the link HREF
      RequestURIBuilder lURIBuilder = pSerialisationContext.createURIBuilder();
      String lFilename = pEvalNode.getStringAttribute(NodeAttribute.DOWNLOAD_FILENAME, "file");

      DownloadMode lDownloadMode = pEvalNode.getBooleanAttribute(NodeAttribute.DOWNLOAD_AS_ATTACHMENT, true) ? DownloadMode.ATTACHMENT : DownloadMode.INLINE;

      String lURI = DownloadServlet.buildActionDownloadURI(lURIBuilder, pSerialisationContext.getThreadInfoProvider().getThreadId(), pEvalNode.getActionName(), pEvalNode.getActionContextRef(), lFilename, lDownloadMode);
      pTemplateVars.put("ActionHref", lURI);
    }
    else {
      pTemplateVars.put("AttributeSafeActionJS", StringEscapeUtils.escapeHtml4(getActionSubmitString(pEvalNode)));
      pTemplateVars.put("RawActionJS", getActionSubmitString(pEvalNode));
    }
  }

  protected String getActionSubmitString(EN pEvaluatedNode) {
    String lSubmitSection = "FOXjs.action(" + getActionSubmitJSONString(pEvaluatedNode.getActionName(), pEvaluatedNode.getActionContextRef(),  pEvaluatedNode.getConfirmMessage()) + ");";

    // If submit section has been resolved to null then set string to void(0)
    return XFUtil.nvl(lSubmitSection, "void(0);");
  }

  /**
   * Get the generic template vars that most mustache templates for widgets have such as:
   * <ul>
   *   <li>FieldName - external field name/ID</li>
   *   <li>PromptBuffer - Raw HTML from an evaluated buffer</li>
   *   <li>PromptText - Prompt text</li>
   *   <li>AccessiblePromptText - Prompt text for aria tags</li>
   *   <li>Class - CSS Classes</li>
   *   <li>Style - Styles on the element</li>
   *   <li>Rows - Size of the widget, set by fieldHeight</li>
   *   <li>Cols - Size of the widget, set by fieldWidth</li>
   *   <li>Readonly - If the field is in view-mode</li>
   *   <li>Mandatory - If the field is marked up as mandatory</li>
   *   <li>AttributeSafeActionJS - Action JS that is safe to include in attributes using {{{}}}</li>
   *   <li>RawActionJS - Unescaped Action JS that is not to be put in attributes, only raw JS blocks using {{{}}}</li>
   *   <li>HintID - Hint icon ID to trigger hints on focus if turned on</li>
   * </ul>
   *
   *
   * @param pSerialisationContext
   * @param pEvalNode
   * @return Map of variables for widget mustache templates
   */
  protected Map<String, Object> getGenericTemplateVars(SerialisationContext pSerialisationContext, HTMLSerialiser pSerialiser, EN pEvalNode) {
    FieldMgr lFieldMgr = pEvalNode.getFieldMgr();

    Map<String, Object> lTemplateVars = new HashMap<>();
    lTemplateVars.put("FieldName", lFieldMgr.getExternalFieldName());

    setPromptOrPromptBufferTemplateVariable(pSerialisationContext, pSerialiser, pEvalNode, lTemplateVars);

    lTemplateVars.put("AccessiblePromptText", pEvalNode.getStringAttribute(NodeAttribute.ACCESSIBLE_PROMPT));
    lTemplateVars.put("PlaceholderText", pEvalNode.getStringAttribute(NodeAttribute.PLACEHOLDER));

    List<String> lDefaultClasses = pEvalNode.getStringAttributes(NodeAttribute.CLASS, NodeAttribute.FIELD_CLASS);
    List<String> lDefaultStyles = pEvalNode.getStringAttributes(NodeAttribute.STYLE, NodeAttribute.FIELD_STYLE);
    String lTightWidgets = pEvalNode.getStringAttribute(NodeAttribute.TIGHT_WIDGETS);
    if (pEvalNode.getBooleanAttribute(NodeAttribute.TIGHT_FIELD, false)) {
      lDefaultClasses.add("tight-field");
      lTemplateVars.put("TightField", true);
    }
    else if (!XFUtil.isNull(lTightWidgets)) {
      for (String lTightWidgetName : StringUtil.commaDelimitedListToSet(lTightWidgets)) {
        if (WidgetBuilderType.fromString(lTightWidgetName, pEvalNode, false) == pEvalNode.getWidgetBuilderType()) {
          lDefaultClasses.add("tight-field");
          lTemplateVars.put("TightField", true);
          break;
        }
      }
    }
    if (lTemplateVars.get("PromptText") == null && lTemplateVars.get("PromptBuffer") == null) {
      lDefaultClasses.add("has-blank-prompt");
    }
    if (lTemplateVars.get("PromptBuffer") != null) {
      lDefaultClasses.add("promptbuffer");
    }
    if (pEvalNode.getWidgetBuilderType().isAction()) {
      lDefaultClasses.add(pEvalNode.getStringAttribute(NodeAttribute.ACTION_CLASS));
      lDefaultStyles.add(pEvalNode.getStringAttribute(NodeAttribute.ACTION_STYLE));
    }
    lTemplateVars.put("Class", Joiner.on(" ").skipNulls().join(lDefaultClasses));

    lTemplateVars.put("Style", Joiner.on(" ").skipNulls().join(lDefaultStyles));
    lTemplateVars.put("Cols", pEvalNode.getFieldWidth());
    lTemplateVars.put("Rows", pEvalNode.getFieldHeight());

    lTemplateVars.put("Readonly", lFieldMgr.getVisibility() == NodeVisibility.VIEW);

    if (pEvalNode.isMandatory()) {
      lTemplateVars.put("Mandatory", true);
    }

    if (lFieldMgr.isRunnable()) {
      addActionTemplateVars(pSerialisationContext, pEvalNode, lTemplateVars);
    }

    if (pEvalNode.hasHint() && pEvalNode.isEnableFocusHintDisplay()) {
      lTemplateVars.put("HintID", pEvalNode.getHint().getHintID());
    }

    return lTemplateVars;
  }

  /**
   * Add PromptBuffer or PromptText value to the pTemplateVars map
   *
   * @param pSerialisationContext
   * @param pSerialiser
   * @param pEvalNode
   * @param pTemplateVars
   */
  private void setPromptOrPromptBufferTemplateVariable(SerialisationContext pSerialisationContext, HTMLSerialiser pSerialiser, EN pEvalNode, Map<String, Object> pTemplateVars) {
    EvaluatedPresentationNode<? extends PresentationNode> lEvaluatedPresentationNode = pEvalNode.getPromptBuffer();
    if (lEvaluatedPresentationNode != null) {
      TempSerialiser lTempSerialiser = pSerialiser.getTempSerialiser();
      lEvaluatedPresentationNode.render(pSerialisationContext, lTempSerialiser);
      pTemplateVars.put("PromptBuffer", lTempSerialiser.getOutput());
    }
    else {
      pTemplateVars.put("PromptText", pSerialiser.getSafeStringAttribute(pEvalNode.getPrompt()));
    }
  }
}
