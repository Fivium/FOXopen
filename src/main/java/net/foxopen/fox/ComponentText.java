package net.foxopen.fox;

import net.foxopen.fox.ex.ExInternal;
import net.foxopen.fox.ex.ExModule;
import net.foxopen.fox.ex.ExSecurity;
import net.foxopen.fox.ex.ExServiceUnavailable;
import net.foxopen.fox.ex.ExUserRequest;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.Reader;

public final class ComponentText
extends FoxComponent
{

  private final String mName;
  private final String mType;
  private final FoxResponse mFoxResponse;
  private StringBuffer mText;

  public ComponentText(String pName, String pType, StringBuffer pStringBuffer, long pBrowserCacheMilliSec, String pOptionalHash) {
    super(pOptionalHash);
    mName = pName;
    mType = pType;
    mText = pStringBuffer;
    mFoxResponse = new FoxResponseCHAR(pType, pStringBuffer, pBrowserCacheMilliSec);
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

  public final String getType() {
    return mType;
  }

  public final StringBuffer getText()
  {
     return mText;
  }

  @Override
  public Reader getReader() {
    return null;
  }

  @Override
  public InputStream getInputStream() {
    return new ByteArrayInputStream(mText.toString().getBytes());
  }
}
