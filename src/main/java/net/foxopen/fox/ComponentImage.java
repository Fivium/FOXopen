package net.foxopen.fox;

import net.foxopen.fox.ex.*;
import net.foxopen.fox.track.Track;
import net.foxopen.fox.track.TrackFlag;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;


public final class ComponentImage
extends FoxComponent
{

  private final String mName;
  private final FoxResponseBytes mFoxResponse;
  private final ImageInfo mImageInfo;

  public ComponentImage(
    String pName
  , byte[] pImageBytes
  , String pExpectedMimeType
  , long pBrowserCacheMilliSec
  , String pOptionalHash
  )
  throws ExApp // when unsupported image format
  {
    super(pOptionalHash);
    // Record name
    mName = pName;

    // Determine Image Properties (size, Mime type etc)
    ByteArrayInputStream lByteArrayInputStream = new ByteArrayInputStream(pImageBytes);
    mImageInfo = new ImageInfo();
    mImageInfo.setInput(lByteArrayInputStream); // in can be InputStream or RandomAccessFile
    if (!mImageInfo.check()) {
      throw new ExApp("Not a supported image file format: "+mName);
    }
    try {
      lByteArrayInputStream.close();
    }
    catch(IOException x) {
      // no action
    }

    // Validate MimeType (warning only)
    String lMimeType=mImageInfo.getMimeType();
    String lExpectedMimeType = XFUtil.nvl(pExpectedMimeType, "image");
    if(!lExpectedMimeType.equals("image") && !lExpectedMimeType.equals(lMimeType)) {
      Track.debug("FoxSysLogWarning", "Image " + pName + " is stored on database as type " + pExpectedMimeType
      + " but is actually of type " + lMimeType, TrackFlag.FOX_SYS_LOG_WARNING);
    }

    // Build standard binary response
    mFoxResponse = new FoxResponseBytes(lMimeType, pImageBytes, pBrowserCacheMilliSec);

  }

  public String getName()
  {
    return mName;
  }

  public FoxResponse processResponse(
    FoxRequest pRequest
  , StringBuffer pURLTail
  )
  throws
    ExInternal
  , ExSecurity            // when browser attempting to access thread owned by another session
  , ExUserRequest
  , ExModule
  , ExServiceUnavailable
  {
    return mFoxResponse;
  }

  public FoxResponseBytes getFoxResponseBytes()
  {
    return mFoxResponse;
  }

  public int getWidth () {
    return mImageInfo.getWidth();
  }

  public int getHeight () {
    return mImageInfo.getHeight();
  }

  public final String getType() {
    return mImageInfo.getMimeType();
  }

  public final byte[] getByteArray () {
    return mFoxResponse.getBytes();
  }

  @Override
  public InputStream getInputStream() {
    return new ByteArrayInputStream(mFoxResponse.getBytes());
  }

  @Override
  public Reader getReader() {
    return null;
  }
}
