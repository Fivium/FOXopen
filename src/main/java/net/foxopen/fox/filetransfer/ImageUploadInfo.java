/*

Copyright (c) 2010, UK DEPARTMENT OF ENERGY AND CLIMATE CHANGE - 
                    ENERGY DEVELOPMENT UNIT (IT UNIT)
All rights reserved.

Redistribution and use in source and binary forms, with or without modification,
are permitted provided that the following conditions are met:

    * Redistributions of source code must retain the above copyright notice, 
      this list of conditions and the following disclaimer.
    * Redistributions in binary form must reproduce the above copyright notice, 
      this list of conditions and the following disclaimer in the documentation 
      and/or other materials provided with the distribution.
    * Neither the name of the DEPARTMENT OF ENERGY AND CLIMATE CHANGE nor the
      names of its contributors may be used to endorse or promote products
      derived from this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND 
ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED 
WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE 
DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR
ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES 
(INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; 
LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON 
ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT 
(INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS 
SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

$Id$

*/
package net.foxopen.fox.filetransfer;

import java.awt.Dimension;

import net.foxopen.fox.ex.ExInternal;
import net.foxopen.fox.image.ImageUtils;
import net.foxopen.fox.thread.storage.WorkingFileStorageLocation;
import net.foxopen.fox.thread.storage.WorkingStorageLocation;


public class ImageUploadInfo extends UploadInfo {

  private Dimension mDisplayDim;
  private int mPresentationRotation;
  private String mFoxServletMnem;
  private boolean mImageProcessingComplete;
  private String mContextRef;
  
  public ImageUploadInfo(String pContextRef, String pGroupingRef, String pCallId, Boolean pEnableInterrupt, WorkingStorageLocation pDataWSL, WorkingFileStorageLocation pFileWSL, Dimension pDisplayDim, String pFoxServletMnem) {
    
    //TODO PN IMAGE WIDGET
    super("", pCallId, pContextRef, null, null);

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
