package net.foxopen.fox.module;

import net.foxopen.fox.XFUtil;
import net.foxopen.fox.dom.DOM;
import net.foxopen.fox.ex.ExInternal;
import net.foxopen.fox.filetransfer.UploadInfo;
import org.apache.commons.io.FileUtils;
import org.json.simple.JSONObject;


/**
 * Immutable information about a COMPLETED file upload. This is different from an UploadInfo, which is mutable
 */
public class UploadedFileInfo {

  private static final String FILE_ID_NODE_NAME = "file-id";
  private static final String FILENAME_NODE_NAME = "filename";
  private static final String CONTENT_TYPE_NODE_NAME = "content-type";
  private static final String FILE_LOCATION_NODE_NAME = "original-file-location";
  private static final String ERROR_MESSAGE_NAME = "readable-error-message";
  private static final String SIZE_NODE_NAME = "size";

  public static final String DOM_REF_KEY_NAME = "uploadDomRef";
  private static final String FILE_ID_KEY_NAME = "fileId";
  private static final String FILENAME_KEY_NAME = "filename";
  private static final String CONTENT_TYPE_KEY_NAME = "contentType";
  public static final String ERROR_MESSAGE_KEY_NAME = "errorMessage";
//  private static final String FILE_LOCATION_KEY_NAME = "original-file-location";
  private static final String SIZE_KEY_NAME = "fileSize";
  private static final String DOWNLOAD_URL_KEY_NAME = "downloadUrl";

  /** FOXID of the element containing the file upload metadata */
  private final String mUploadDOMRef;
  private final String mFileId;
  private final String mFilename;
  private final String mBrowserContentType;
  private final String mOriginalFileLocation;
  private final String mErrorMessage;
  private final long mFileSizeBytes;

  //Can't be known at construction time; should be set by creator as soon as possible
  private String mDownloadURL = null;

  public static UploadedFileInfo createFromDOM(DOM pFileElem) {

    String lFileId = pFileElem.get1SNoEx(FILE_ID_NODE_NAME);
    String lFilename = pFileElem.get1SNoEx(FILENAME_NODE_NAME);
    String lBrowserContentType = pFileElem.get1SNoEx(CONTENT_TYPE_NODE_NAME);
    String lOriginalFileLocation = pFileElem.get1SNoEx(FILE_LOCATION_NODE_NAME);
    String lErrorMessage = pFileElem.get1SNoEx("diagnostic-info/" + ERROR_MESSAGE_NAME);
    String lFileBytesString = pFileElem.get1SNoEx(SIZE_NODE_NAME);
    long lFileBytes = Long.valueOf(XFUtil.nvl(lFileBytesString.equals("") ? null : lFileBytesString, 0)).longValue();

    return new UploadedFileInfo(pFileElem.getRef(), lFileId, lFilename, lBrowserContentType, lOriginalFileLocation, lErrorMessage, lFileBytes);
  }

  public static UploadedFileInfo createFromUploadInfo(String pUploadContainerDOMRef, UploadInfo pUploadInfo) {
    return new UploadedFileInfo(pUploadContainerDOMRef, pUploadInfo.getFileId(), pUploadInfo.getFilename(), pUploadInfo.getBrowserContentType(), pUploadInfo.getOriginalFileLocation(), "", pUploadInfo.getFileSize());
  }

  private UploadedFileInfo(String pUploadDOMRef, String pFileId, String pFilename, String pBrowserContentType, String pOriginalFileLocation, String pErrorMessage, long pFileBytes) {
    mUploadDOMRef = pUploadDOMRef;
    mFileId = pFileId;
    mFilename = pFilename;
    mBrowserContentType = pBrowserContentType;
    mOriginalFileLocation = pOriginalFileLocation;
    mErrorMessage = pErrorMessage;
    mFileSizeBytes = pFileBytes;
  }

  public JSONObject asJSONObject() {
    JSONObject lJSONObject = new JSONObject();
    lJSONObject.put(DOM_REF_KEY_NAME, mUploadDOMRef);
    lJSONObject.put(FILE_ID_KEY_NAME, mFileId);
    lJSONObject.put(FILENAME_KEY_NAME, mFilename);
    lJSONObject.put(CONTENT_TYPE_KEY_NAME, mBrowserContentType);
    lJSONObject.put(SIZE_KEY_NAME, FileUtils.byteCountToDisplaySize(mFileSizeBytes));
    lJSONObject.put(DOWNLOAD_URL_KEY_NAME, mDownloadURL);

    if(!XFUtil.isNull(mErrorMessage)) {
      lJSONObject.put(ERROR_MESSAGE_KEY_NAME, mErrorMessage);
    }

    return lJSONObject;
  }

  public String getFilename() {
    return mFilename;
  }

  public String getFileId() {
    return mFileId;
  }

  public String getBrowserContentType() {
    return mBrowserContentType;
  }

  public long getFileSizeBytes() {
    return mFileSizeBytes;
  }

  public void setDownloadURL(String pDownloadURL) {
    if(mDownloadURL == null) {
      mDownloadURL = pDownloadURL;
    }
    else {
      throw new ExInternal("Cannot modify download URL after it is set");
    }
  }

  public String getDownloadURL() {
    return mDownloadURL;
  }
}
