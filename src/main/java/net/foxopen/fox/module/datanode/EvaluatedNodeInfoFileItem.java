package net.foxopen.fox.module.datanode;

import net.foxopen.fox.ContextUElem;
import net.foxopen.fox.FileUploadType;
import net.foxopen.fox.XFUtil;
import net.foxopen.fox.dom.DOM;
import net.foxopen.fox.download.DownloadManager;
import net.foxopen.fox.download.DownloadMode;
import net.foxopen.fox.download.DownloadServlet;
import net.foxopen.fox.ex.ExActionFailed;
import net.foxopen.fox.ex.ExInternal;
import net.foxopen.fox.module.evaluatedattributeresult.FixedStringAttributeResult;
import net.foxopen.fox.module.evaluatedattributeresult.StringAttributeResult;
import net.foxopen.fox.module.UploadedFileInfo;
import net.foxopen.fox.module.parsetree.evaluatedpresentationnode.GenericAttributesEvaluatedPresentationNode;
import net.foxopen.fox.module.parsetree.presentationnode.GenericAttributesPresentationNode;
import net.foxopen.fox.thread.storage.FileStorageLocation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


public class EvaluatedNodeInfoFileItem
extends EvaluatedNodeInfoItem {

  private final List<UploadedFileInfo> mUploadedFileInfoList;
  private final UploadWidgetOptions mUploadWidgetOptions;

  private final String mDefaultHint;
  private final String mDefaultDescription;

  /** Cached JIT to avoid multiple XPath evaluation */
  private Integer mMaxFilesAllowed = null;

  EvaluatedNodeInfoFileItem(EvaluatedNode pParent, GenericAttributesEvaluatedPresentationNode<? extends GenericAttributesPresentationNode> pEvaluatedPresentationNode, NodeEvaluationContext pNodeEvaluationContext, NodeVisibility pNodeVisibility, NodeInfo pNodeInfo) {
    super(pParent, pEvaluatedPresentationNode, pNodeEvaluationContext, pNodeVisibility, pNodeInfo);

    DOM lDataItem = getDataItem();
    if(lDataItem.getChildElements().size() > 0) {

      String lFSLAttr = pNodeInfo.getFoxNamespaceAttribute(NodeAttribute.FILE_STORAGE_LOCATION);
      FileStorageLocation lFSL = getEvaluatedParseTree().getModule().getFileStorageLocation(lFSLAttr);
      DownloadManager lDownloadManager = getEvaluatedParseTree().getDownloadManager();

      //If this is a multi upload widget, loop through the child nodes and create an UploadedFileInfo for each one.
      //Otherwise just create one for the current node.
      if(isMultiUploadTarget()) {
        mUploadedFileInfoList = new ArrayList<>(lDataItem.getChildElements().size());
        for(DOM lUploadContainer : lDataItem.getChildElements()) {
          mUploadedFileInfoList.add(lDownloadManager.addFileDownload(getEvaluatedParseTree().getRequestContext(), lFSL, lUploadContainer, getEvaluatedParseTree().getContextUElem()));
        }
      }
      else {
        mUploadedFileInfoList = Collections.singletonList(lDownloadManager.addFileDownload(getEvaluatedParseTree().getRequestContext(), lFSL, lDataItem, getEvaluatedParseTree().getContextUElem()));
      }
    }
    else {
      mUploadedFileInfoList = Collections.emptyList();
    }

    boolean lDescriptionDefined = isAttributeDefined(NodeAttribute.DESCRIPTION);
    boolean lHintDefined = isAttributeDefined(NodeAttribute.HINT);
    String lDefaultHint = null;
    String lDefaultDescription = null;

    if(!lDescriptionDefined || !lHintDefined) {
      FileUploadType lFileUploadType = getEvaluatedParseTree().getApp().getFileUploadType(getStringAttribute(NodeAttribute.UPLOAD_FILE_TYPE));

      //Establish the default description attribute if not override by user
      if(!lDescriptionDefined) {
        lDefaultDescription = lFileUploadType.getReadableSummaryDescription();
      }

      if(!lHintDefined) {
        lDefaultHint = lFileUploadType.getReadableExtensionRestrictionList();
      }
    }

    mDefaultHint = lDefaultHint;
    mDefaultDescription = lDefaultDescription;

    mUploadWidgetOptions = new UploadWidgetOptions();
  }

  private enum UploadWidgetMode {
    INLINE, MODAL, MODELESS;
  }

  private class UploadWidgetOptions {

    private final UploadWidgetMode mWidgetMode;
    private final String mCompleteAction;
    private final String mSuccessAction;
    private final String mFailAction;
    private final String mUploadChoosePrompt;

    private UploadWidgetOptions() {

      mCompleteAction = getStringAttribute(NodeAttribute.UPLOAD_COMPLETE_ACTION);
      mSuccessAction = getStringAttribute(NodeAttribute.UPLOAD_SUCCESS_ACTION);
      mFailAction = getStringAttribute(NodeAttribute.UPLOAD_FAIL_ACTION);
      mUploadChoosePrompt = getStringAttribute(NodeAttribute.UPLOAD_CHOOSE_PROMPT);

      if(!XFUtil.isNull(mCompleteAction) && (!XFUtil.isNull(mSuccessAction) || !XFUtil.isNull(mFailAction))) {
        throw new ExInternal("Error with node " + getIdentityInformation() + " - cannot specify a fail or success action if a complete action is also specified");
      }

      if(!XFUtil.isNull(mCompleteAction) || !XFUtil.isNull(mSuccessAction) || !XFUtil.isNull(mFailAction)) {
        mWidgetMode = UploadWidgetMode.MODAL;
      }
      else {
        mWidgetMode = UploadWidgetMode.INLINE;
      }
    }
  }

  public String getUploadWidgetMode() {
    return mUploadWidgetOptions.mWidgetMode.toString().toLowerCase();
  }

  public String getSuccessAction() {
    return XFUtil.nvl(mUploadWidgetOptions.mCompleteAction, mUploadWidgetOptions.mSuccessAction);
  }

  public String getFailAction() {
    return XFUtil.nvl(mUploadWidgetOptions.mCompleteAction, mUploadWidgetOptions.mFailAction);
  }

  /**
   * Tests if this ENI represents a multi file upload target.
   * @return True if a multi upload target, false if a single upload target.
   */
  public boolean isMultiUploadTarget() {
    return isMultiUploadTarget(getNodeInfo());
  }

  /**
   * Tests if the given node info represents a multi file upload target.
   * @return True if a multi upload target, false if a single upload target.
   */
  public static boolean isMultiUploadTarget(NodeInfo pNodeInfo) {
    return pNodeInfo.isListContainer();
  }

  public int getMaxFilesAllowed() {
    if(mMaxFilesAllowed == null) {
      mMaxFilesAllowed = maxFilesAllowed(getContextUElem(), getNodeInfo(), getDataItem());
    }
    return mMaxFilesAllowed;
  }

  /**
   * Shared functionality for establishing the maximum number of files allowed in an upload. Both ENI and UploadProcessing
   * should use the same method to ensure consistency (although this results in some XPath evaluation logic duplication on the ENI side). <br><br>
   *
   * For list containers, if the <tt>fox:maxUploadFiles</tt> attribute is specified, this takes precedence. Otherwise the list
   * cardinality is used. For non-list containers (i.e. single uploads) the maximum files is always 1.
   *
   * @param pContextUElem For running max file XPath if specified.
   * @param pContainerNodeInfo NodeInfo of the upload container (i.e. list container for multi uploads).
   * @param pContainerDOM DOM corresponding to the given NodeInfo.
   * @return Maximum number of files allowed to be uploaded to the given node. Note this does not reflect how many files
   * are currently uploaded.
   */
  public static int maxFilesAllowed(ContextUElem pContextUElem, NodeInfo pContainerNodeInfo, DOM pContainerDOM) {

    if(pContainerNodeInfo.isListContainer()) {

      String lMaxFileAttr = pContainerNodeInfo.getFoxNamespaceAttribute(NodeAttribute.UPLOAD_MAX_FILES);

      if(!XFUtil.isNull(lMaxFileAttr)) {
        try {
          return Integer.parseInt(pContextUElem.extendedStringOrXPathString(NodeEvaluationContext.establishEvaluateContextRuleNode(pContainerDOM, pContainerNodeInfo), lMaxFileAttr));
        }
        catch (ExActionFailed | NumberFormatException e) {
          throw new ExInternal("Failed to evaluate uploadMaxFiles attribute", e);
        }
      }
      else {
        return pContainerNodeInfo.getListMaxCardinality();
      }
    }
    else {
      return 1;
    }
  }

  public String getUploadChoosePrompt() {
    return XFUtil.nvl(mUploadWidgetOptions.mUploadChoosePrompt, "Choose file...");
  }

  /**
   * Gets a list of 0 or more UploadedFileInfos corresponding to uploads contained in this node.
   * @return
   */
  public List<UploadedFileInfo> getUploadedFileInfoList() {
    return mUploadedFileInfoList;
  }

  public String getDownloadModeParameter() {
    DownloadMode lDownloadMode = getBooleanAttribute(NodeAttribute.DOWNLOAD_AS_ATTACHMENT, true) ? DownloadMode.ATTACHMENT : DownloadMode.INLINE;
    if(lDownloadMode != DownloadServlet.DEFAULT_DOWNLOAD_MODE) {
      return DownloadServlet.DOWNLOAD_MODE_PARAM_NAME + "=" + lDownloadMode.getHttpParameterValue();
    }
    else {
      return "";
    }
  }

  protected StringAttributeResult getDefaultHint() {
    if(mDefaultHint != null) {
      return new FixedStringAttributeResult(mDefaultHint);
    }
    return super.getDefaultHint();
  }

  protected StringAttributeResult getDefaultDescription() {
    if(mDefaultDescription != null) {
      return new FixedStringAttributeResult(mDefaultDescription);
    }
    return super.getDefaultDescription();
  }
}
