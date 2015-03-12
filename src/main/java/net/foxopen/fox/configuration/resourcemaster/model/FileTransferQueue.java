package net.foxopen.fox.configuration.resourcemaster.model;

import net.foxopen.fox.XFUtil;
import net.foxopen.fox.dom.DOM;
import net.foxopen.fox.filetransfer.FileTransferServiceQueue;

public class FileTransferQueue {
  private final String mName;
  private final int mMinFileBytes;
  private final int mMaxFileBytes;
  private final int mMaxUploadChannels;
  private final int mMaxDownloadChannels;

  public static FileTransferQueue createFileTransferQueue(DOM pFileTransferQueueDOM) {
    FileTransferQueue lFileTransferQueue = new FileTransferQueue(pFileTransferQueueDOM);
    return lFileTransferQueue;
  }

  public FileTransferQueue(DOM pFileTransferQueueDOM) {
    mName = pFileTransferQueueDOM.get1SNoEx("name");

    if (!XFUtil.isNull(pFileTransferQueueDOM.get1SNoEx("min-file-bytes"))) {
      mMinFileBytes =  Integer.parseInt(pFileTransferQueueDOM.get1SNoEx("min-file-bytes"));
    } else {
      mMinFileBytes = 0;
    }

    if (!XFUtil.isNull(pFileTransferQueueDOM.get1SNoEx("max-file-bytes"))) {
      mMaxFileBytes =  Integer.parseInt(pFileTransferQueueDOM.get1SNoEx("max-file-bytes"));
    } else {
      mMaxFileBytes = 999999999;
    }

    if (!XFUtil.isNull(pFileTransferQueueDOM.get1SNoEx("max-upload-channels"))) {
      mMaxUploadChannels = Integer.parseInt(pFileTransferQueueDOM.get1SNoEx("max-upload-channels"));
    } else {
      mMaxUploadChannels = FileTransferServiceQueue.MAX_UPLOAD_DEFAULT;
    }

    if (!XFUtil.isNull(pFileTransferQueueDOM.get1SNoEx("max-download-channels"))) {
      mMaxDownloadChannels =  Integer.parseInt(pFileTransferQueueDOM.get1SNoEx("max-download-channels"));
    } else {
      mMaxDownloadChannels = FileTransferServiceQueue.MAX_DOWNLOAD_DEFAULT;
    }
  }

  public String getName() {
    return mName;
  }

  public int getMinFileBytes() {
    return mMinFileBytes;
  }

  public int getMaxFileBytes() {
    return mMaxFileBytes;
  }

  public int getMaxUploadChannels() {
    return mMaxUploadChannels;
  }

  public int getMaxDownloadChannels() {
    return mMaxDownloadChannels;
  }
}
