package net.foxopen.fox.filetransfer;

import net.foxopen.fox.ex.ExInternal;
import net.foxopen.fox.image.ImageUtils;
import net.foxopen.fox.thread.storage.WorkingFileStorageLocation;
import net.foxopen.fox.thread.storage.WorkingStorageLocation;

import java.awt.*;


public class ImageUploadInfo extends UploadInfo {

  private Dimension mDisplayDim;
  private int mPresentationRotation;
  private String mFoxServletMnem;
  private boolean mImageProcessingComplete;
  private String mContextRef;

  public ImageUploadInfo(String pContextRef, String pGroupingRef, String pCallId, Boolean pEnableInterrupt, WorkingStorageLocation pDataWSL, WorkingFileStorageLocation pFileWSL, Dimension pDisplayDim, String pFoxServletMnem) {

    //TODO PN IMAGE WIDGET
    super(null, null, null, null, null, null, null, null, null);

    if (pDisplayDim == null) {
      throw new ExInternal("Unable to construct image upload information object due to a missing display dimensions.");
    }
    mDisplayDim = pDisplayDim;

    mFoxServletMnem = pFoxServletMnem;

    // Write the constructor params to member variables ensuring that they are all none null
    if (pContextRef == null) {
      throw new ExInternal("Unable to construct upload information object due to a missing fox id for the file upload element.");
    }
    mContextRef = pContextRef;
  }

  public int getWidth() {
    return (int)mDisplayDim.getWidth();
  }

  public int getHeight() {
    return (int)mDisplayDim.getHeight();
  }

  public String getFoxServletMnem() {
    return mFoxServletMnem;
  }

  public String imageCacheKey() {
    return imageCacheKey(getFileId(), (int)mDisplayDim.getWidth(), (int)mDisplayDim.getHeight());
  }

  public String imageCacheKey(int pWidth, int pHeight) {
    return imageCacheKey(getFileId(), pWidth, pHeight);
  }

  public static String imageCacheKey(String pImageId, int pWidth, int pHeight) {
    return "?fileid=" + pImageId
                  + "&width=" + pWidth
                  + "&height=" + pHeight;
  }

  public static String imageCacheKey(String pImageId, int pWidth, int pHeight, int pRotation) {
    return "?fileid=" + pImageId
                  + "&width=" + pWidth
                  + "&height=" + pHeight
                  + "&rotation=" + ImageUtils.getNormalisedRotation(pRotation);
  }

  public String imageCacheKey(int pWidth, int pHeight, int pRotation) {
    return imageCacheKey(getFileId(), pWidth, pHeight, pRotation);
  }

  public void setImageProcessingComplete(boolean pComplete) {
    mImageProcessingComplete = pComplete;
  }
}
