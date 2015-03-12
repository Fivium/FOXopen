package net.foxopen.fox.download;


import com.google.common.base.Charsets;
import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;

import net.foxopen.fox.module.UploadedFileInfo;
import net.foxopen.fox.thread.storage.WorkingFileStorageLocation;


public class UploadedFileDownloadParcel
extends WFSLDownloadParcel {

  private static final HashFunction gHashFunction = Hashing.goodFastHash(128);

  static String generateParcelId(WorkingFileStorageLocation pWFSL) {
//    return pWFSL.getCacheKey().replace("/","_").replace(" ", "_"); //Temporarily exposed to help with debugging
    //Hash the WFSL cache key to obfuscate any developer-introduced oddities/information leakage
    return gHashFunction.newHasher().putString(pWFSL.getCacheKey(), Charsets.UTF_8).hash().toString();
  }

  private UploadedFileDownloadParcel(String pParcelId, WorkingFileStorageLocation pWFSL, String pFilename, String pContentType) {
    super(pParcelId, pWFSL, pFilename, pContentType);
  }

  static UploadedFileDownloadParcel createParcelFromUploadedFileInfo(WorkingFileStorageLocation pWFSL, UploadedFileInfo pUploadedFileInfo ) {
    return new UploadedFileDownloadParcel(generateParcelId(pWFSL), pWFSL, pUploadedFileInfo.getFilename(), pUploadedFileInfo.getBrowserContentType());
  }
}
