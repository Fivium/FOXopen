package net.foxopen.fox.module.serialiser.widgets.html;

import net.foxopen.fox.XFUtil;
import net.foxopen.fox.filetransfer.UploadServlet;
import net.foxopen.fox.module.UploadedFileInfo;
import net.foxopen.fox.module.datanode.EvaluatedNode;
import net.foxopen.fox.module.datanode.EvaluatedNodeInfoFileItem;
import net.foxopen.fox.module.datanode.NodeAttribute;
import net.foxopen.fox.module.datanode.NodeVisibility;
import net.foxopen.fox.module.fieldset.clientaction.DeleteUploadedFileClientAction;
import net.foxopen.fox.module.fieldset.fieldmgr.FieldMgr;
import net.foxopen.fox.module.serialiser.SerialisationContext;
import net.foxopen.fox.module.serialiser.html.HTMLSerialiser;
import net.foxopen.fox.module.serialiser.widgets.WidgetBuilder;
import net.foxopen.fox.module.serialiser.widgets.WidgetBuilderType;
import org.apache.commons.lang3.StringUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.util.Collection;
import java.util.List;


public class FileWidgetBuilder
extends WidgetBuilderHTMLSerialiser<EvaluatedNodeInfoFileItem> {

  private static final WidgetBuilder<HTMLSerialiser, EvaluatedNodeInfoFileItem> INSTANCE = new FileWidgetBuilder();
  private static final String MUSTACHE_TEMPLATE = "html/FileWidget.mustache"; //TODO PN proper widget template

  public static final WidgetBuilder<HTMLSerialiser, EvaluatedNodeInfoFileItem> getInstance() {
    return INSTANCE;
  }

  private static void insertDropzoneDiv(HTMLSerialiser pSerialiser, String pContainerId) {
    pSerialiser.append("<div class=\"dropzone\" data-dropzone-id=\"" + pContainerId + "\" style=\"display:none;\"><div class=\"dropzone-text-container\"><div class=\"dropzone-text icon-download\">Drop files here<div class=\"dropzone-max-files-text\"></div></div></div></div>");
  }

  private static String getFieldId(FieldMgr pFieldMgr) {
    return pFieldMgr.getExternalFieldName();
  }

  private static String getContainerId(FieldMgr pFieldMgr) {
    return "fileUploadContainer-"+getFieldId(pFieldMgr);
  }

  private static boolean singleDropzoneRequired(SerialisationContext pSerialisationContext) {
    Collection<? extends EvaluatedNode> lFileENIs = pSerialisationContext.getEvaluatedNodesByWidgetBuilderType(WidgetBuilderType.FILE);
    return lFileENIs.size() == 1 && lFileENIs.iterator().next().getBooleanAttribute(NodeAttribute.UPLOAD_WHOLE_PAGE_DROPZONE, true);
  }

  public static void insertSingleDropzone(SerialisationContext pSerialisationContext, HTMLSerialiser pSerialiser){
    //If there's exactly 1 file widget on the page, and it's marked up to allow a whole page dropzone, add the dropzone now (in the body tag)
    if(singleDropzoneRequired(pSerialisationContext)) {
      EvaluatedNode lSingleFileENI = pSerialisationContext.getEvaluatedNodesByWidgetBuilderType(WidgetBuilderType.FILE).iterator().next();
      insertDropzoneDiv(pSerialiser, getContainerId(lSingleFileENI.getFieldMgr()));
    }
  }

  private FileWidgetBuilder() { }

  @Override
  public void buildWidgetInternal(SerialisationContext pSerialisationContext, HTMLSerialiser pSerialiser, EvaluatedNodeInfoFileItem pEvalNode) {

    FieldMgr lFieldMgr = pEvalNode.getFieldMgr();
    String lFieldId = getFieldId(lFieldMgr);
    String lContainerId = getContainerId(lFieldMgr);

    String lWidgetMode =  pEvalNode.getUploadWidgetMode();
    pSerialiser.append("<div class=\"fileUpload " + lWidgetMode + "\" id=\"" + lContainerId + "\">");

    pSerialiser.append("<ul class=\"fileList\"></ul>");

    //Add the dropzone within the file upload container if it's not a whole page dropzone
    if(!singleDropzoneRequired(pSerialisationContext)) {
      insertDropzoneDiv(pSerialiser, lContainerId);
    }

    pSerialiser.append("<div class=\"fileUploadInputContainer\" role=\"menu\">");

    //TODO PN need to handle all attributes (like generic template vars for other widgets) e.g. tightField, etc

    List<String> lClasses = pEvalNode.getStringAttributes(NodeAttribute.CLASS, NodeAttribute.FIELD_CLASS);
    List<String> lDefaultStyles = pEvalNode.getStringAttributes(NodeAttribute.STYLE, NodeAttribute.FIELD_STYLE);

    if(lFieldMgr.getVisibility() == NodeVisibility.EDIT) {
      pSerialiser.append("<a href=\"#\" role=\"menuitem\" class=\"fileUploadLink " + StringUtils.join(lClasses, " ") + "\" style=\"" +  StringUtils.join(lDefaultStyles, " ") + "\" aria-label=\"" + pEvalNode.getPrompt().getString() + ": " + pEvalNode.getUploadChoosePrompt() + "\">" + pEvalNode.getUploadChoosePrompt() + "</a>");

      pSerialiser.append("<input type=\"file\" tabindex=\"-1\" " + (pEvalNode.getMaxFilesAllowed() > 1 ? "multiple" : "") + " id=\"" + lFieldId + "\" name=\"" + lFieldId + "\" " +
        "class=\"uploadControl fileUploadInput\">");
    }

    pSerialiser.append("</div>");

    List<UploadedFileInfo> lFileInfoList = pEvalNode.getUploadedFileInfoList();
    //Make the JSON array null if no files have been uploaded
    String lFileInfoJSON = "null";
    if(lFileInfoList.size() > 0) {
      JSONArray lJSONArray = new JSONArray();
      for(UploadedFileInfo lFileInfo : lFileInfoList) {
        lJSONArray.add(lFileInfo.asJSONObject());
      }

      lFileInfoJSON = lJSONArray.toJSONString();
    }

    String lURLBase = pSerialisationContext.createURIBuilder().buildServletURI(UploadServlet.UPLOAD_SERVLET_PATH);

    String lThreadId = pSerialisationContext.getThreadInfoProvider().getThreadId();
    String lCallId = pSerialisationContext.getThreadInfoProvider().getCurrentCallId();
    String lThreadAppMnem = pSerialisationContext.getThreadInfoProvider().getThreadAppMnem();
    String lItemRef = pEvalNode.getDataItem().getRef();

    JSONObject lStartURLParams = new JSONObject();
    lStartURLParams.put("thread_id", lThreadId);
    lStartURLParams.put("call_id", lCallId);
    lStartURLParams.put("app_mnem", lThreadAppMnem);
    lStartURLParams.put("context_ref", lItemRef);


    String lOptionJSON = getWidgetOptionJSONString(pEvalNode, pEvalNode.getActionContextRef(), lFieldMgr.getVisibility().asInt() <  NodeVisibility.EDIT.asInt());
    String lJS = "<script>\n" +
    "$(document).ready(function() {" +
    "  new FileUpload($('#" + lContainerId + "'), '" + lURLBase +  "', " + lStartURLParams.toJSONString() + ", " + lFileInfoJSON  + ", " + lOptionJSON + ");\n" +
    "});\n" +
    "</script>";

    pSerialiser.append(lJS);
    pSerialiser.append("</div>");

    //MustacheFragmentBuilder.applyMapToTemplate(MUSTACHE_TEMPLATE, null, pSerialiser.getWriter());
  }

  private String getWidgetOptionJSONString(EvaluatedNodeInfoFileItem pEvalNode, String pActionContextRef, boolean pIsReadOnly) {

    JSONObject lWidgetOptions = new JSONObject();

    String lSuccessAction = pEvalNode.getSuccessAction();
    if(!XFUtil.isNull(lSuccessAction)) {
      lWidgetOptions.put("successAction", getActionJSON(lSuccessAction, pActionContextRef));
    }

    String lFailAction = pEvalNode.getFailAction();
    if(!XFUtil.isNull(lFailAction)) {
      lWidgetOptions.put("failAction", getActionJSON(lFailAction, pActionContextRef));
    }

    lWidgetOptions.put("widgetMode", pEvalNode.getUploadWidgetMode());

    lWidgetOptions.put("downloadModeParam", pEvalNode.getDownloadModeParameter());

    lWidgetOptions.put("readOnly", pIsReadOnly);

    lWidgetOptions.put("maxFiles", pEvalNode.getMaxFilesAllowed());

    lWidgetOptions.put("deleteActionKey", DeleteUploadedFileClientAction.generateActionKey(pEvalNode.getDataItem().getRef()));

    String lConfirm = pEvalNode.getStringAttribute(NodeAttribute.CONFIRM);
    if(!XFUtil.isNull(lConfirm)) {
      lWidgetOptions.put("deleteConfirmText", lConfirm);
    }

    return lWidgetOptions.toString();
  }
}
