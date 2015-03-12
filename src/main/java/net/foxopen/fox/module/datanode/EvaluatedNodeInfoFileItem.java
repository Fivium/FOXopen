package net.foxopen.fox.module.datanode;

import net.foxopen.fox.FileUploadType;
import net.foxopen.fox.XFUtil;
import net.foxopen.fox.dom.DOM;
import net.foxopen.fox.download.DownloadManager;
import net.foxopen.fox.download.DownloadMode;
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

  EvaluatedNodeInfoFileItem(EvaluatedNode pParent, GenericAttributesEvaluatedPresentationNode<? extends GenericAttributesPresentationNode> pEvaluatedPresentationNode, NodeEvaluationContext pNodeEvaluationContext, NodeVisibility pNodeVisibility, NodeInfo pNodeInfo) {
    super(pParent, pEvaluatedPresentationNode, pNodeEvaluationContext, pNodeVisibility, pNodeInfo);

    //Validate the schema definition - lists must allow more than 1 item otherwise cardinality logic which relies on this value being > 1 will get confused
    if(getNodeInfo().isListContainer() && getNodeInfo().getListMaxCardinality() <= 1) {
      throw new ExInternal("Invalid multi file upload definition: maximum list cardinality must be greater than 1 for node " + getIdentityInformation());
    }

    DOM lDataItem = getDataItem();
    if(lDataItem.getChildElements().size() > 0) {

      String lFSLAttr = pNodeInfo.getFoxNamespaceAttribute(NodeAttribute.FILE_STORAGE_LOCATION);
      FileStorageLocation lFSL = getEvaluatedParseTree().getModule().getFileStorageLocation(lFSLAttr);
      DownloadManager lDownloadManager = getEvaluatedParseTree().getDownloadManager();

      //If this is a multi upload widget, loop through the child nodes and create an UploadedFileInfo for each one.
      //Otherwise just create one for the current node.
      if(getMaxFilesAllowed() > 1) {
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

    if(!lDescriptionDefined || !lHintDefined) {
      FileUploadType lFileUploadType = getEvaluatedParseTree().getApp().getFileUploadType(pNodeInfo.getFoxNamespaceAttribute(NodeAttribute.UPLOAD_FILE_TYPE));

      //Establish the default description attribute if not override by user
      if(!lDescriptionDefined) {
        setDescription(lFileUploadType.getReadableSummaryDescription());
      }

      if(!lHintDefined) {
        lDefaultHint = lFileUploadType.getReadableExtensionRestrictionList();
      }
    }

    mDefaultHint = lDefaultHint;

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

    private UploadWidgetOptions() {

      mCompleteAction = getStringAttribute(NodeAttribute.UPLOAD_COMPLETE_ACTION);
      mSuccessAction = getStringAttribute(NodeAttribute.UPLOAD_SUCCESS_ACTION);
      mFailAction = getStringAttribute(NodeAttribute.UPLOAD_FAIL_ACTION);

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

  public int getMaxFilesAllowed() {
    if(getNodeInfo().isListContainer()) {
      return getNodeInfo().getListMaxCardinality();
    }
    else {
      return 1;
    }
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
    return getEvaluatedParseTree().getDownloadManager().getDownloadModeParamName() + "=" + lDownloadMode.getHttpParameterValue();
  }

  protected StringAttributeResult getDefaultHint() {
    if(mDefaultHint != null) {
      return new FixedStringAttributeResult(mDefaultHint);
    }
    return super.getDefaultHint();
  }
}
