package net.foxopen.fox.download;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import net.foxopen.fox.database.UCon;
import net.foxopen.fox.thread.storage.WorkingFileStorageLocation;

import org.apache.commons.io.IOUtils;


public class WFSLDownloadParcel
implements DownloadParcel {

  private final String mParcelId;
  private final WorkingFileStorageLocation mWorkingStorageLocation;
  private final String mFilename;
  private final String mContentType;

  private LOBWorkDocAccessor mWorkDocAccessor;
  private long mLOBSize = -1;

  WFSLDownloadParcel(String pParcelId, WorkingFileStorageLocation pWFSL, String pFilename, String pContentType) {
    mParcelId = pParcelId;
    mWorkingStorageLocation = pWFSL;
    mFilename = pFilename;
    mContentType = pContentType;
  }

  @Override
  public String getParcelId() {
    return mParcelId;
  }

  @Override
  public String getFilename() {
    return mFilename;
  }

  @Override
  public String getContentType() {
    return mContentType;
  }

  @Override
  public long getFileSizeBytes() {
    return mLOBSize;
  }

  @Override
  public void prepareForDownload(UCon pUCon) {
    mWorkDocAccessor = new LOBWorkDocAccessor(pUCon, mWorkingStorageLocation);
    mLOBSize = mWorkDocAccessor.getLOBSize(pUCon);
  }

  @Override
  public void streamDownload(UCon pUCon, OutputStream pOutputStream)
  throws IOException {
    InputStream lInputStream = mWorkDocAccessor.getInputStream();
    IOUtils.copy(lInputStream, pOutputStream);
    lInputStream.close();
  }

  @Override
  public void closeAfterDownload(UCon pUCon) {
    if(mWorkDocAccessor != null) {
      mWorkDocAccessor.close(pUCon);
    }
  }

  @Override
  public boolean isSerialiseAllowed() {
    return mWorkDocAccessor == null;
  }
}
