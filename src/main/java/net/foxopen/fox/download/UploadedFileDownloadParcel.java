package net.foxopen.fox.download;


import com.google.common.base.Charsets;
import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;

import net.foxopen.fox.module.UploadedFileInfo;
import net.foxopen.fox.thread.storage.WorkingFileStorageLocation;


public class UploadedFileDownloadParcel
extends WFSLDownloadParcel {

  private static final HashFunction gHashFunction = Hashing.goodFastHash(128);

  static String generateParcelId(WorkingFileStorageLocation pWFSL, UploadedFileInfo pUploadedFileInfo) {
    //Hash the WFSL cache key to obfuscate any developer-introduced oddities/information leakage
    //Always include the file ID in the hash so repeated uploads to a WFSL with the same cache key get a new download parcel (to refresh filename etc)
    return gHashFunction
      .newHasher()
      .putString(pUploadedFileInfo.getFileId(), Charsets.UTF_8)
      .putString(pWFSL.getCacheKey(), Charsets.UTF_8)
      .hash()
      .toString();
  }

  private UploadedFileDownloadParcel(String pParcelId, WorkingFileStorageLocation pWFSL, String pFilename, String pContentType) {
    super(pParcelId, pWFSL, pFilename, pContentType);
  }

  static UploadedFileDownloadParcel createParcelFromUploadedFileInfo(WorkingFileStorageLocation pWFSL, UploadedFileInfo pUploadedFileInfo ) {
    return new UploadedFileDownloadParcel(generateParcelId(pWFSL, pUploadedFileInfo), pWFSL, pUploadedFileInfo.getFilename(), pUploadedFileInfo.getBrowserContentType());
  }
}
