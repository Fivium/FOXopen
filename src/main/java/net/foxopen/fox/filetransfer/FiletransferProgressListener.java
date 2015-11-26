package net.foxopen.fox.filetransfer;

import org.apache.commons.fileupload.ProgressListener;

public class FiletransferProgressListener
implements ProgressListener
{

  public FiletransferProgressListener() {
  }

  private int mTransmissionProgress = 0;
  private long mBytesRead = 0;

  public int getTransmissionProgress () {
    return mTransmissionProgress;
  }

  public long getBytesRead () {
    return mBytesRead;
  }

  public void update (long pBytesRead, long pContentLength, int pItems) {
    mBytesRead = pBytesRead;
    mTransmissionProgress = (int)(((float)pBytesRead / (float)pContentLength) * 100);
  }

}
