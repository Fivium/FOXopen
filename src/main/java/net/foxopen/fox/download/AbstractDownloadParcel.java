package net.foxopen.fox.download;


public abstract class AbstractDownloadParcel
implements DownloadParcel {
  
  private final String mParcelId;
  private final String mFilename;
  private final String mContentType;
//  private final long mFileSizeBytes;

  protected AbstractDownloadParcel(String pParcelId, String pFilename, String pContentType/*, long pFileSizeBytes*/) {
    mParcelId = pParcelId;
    mFilename = pFilename;
    mContentType = pContentType;
//    mFileSizeBytes = pFileSizeBytes;
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

//  @Override
//  public long getFileSizeBytes() {
//    return mFileSizeBytes;
//  }
}
