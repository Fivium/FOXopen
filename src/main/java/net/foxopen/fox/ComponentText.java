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
package net.foxopen.fox;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.Reader;

import net.foxopen.fox.ex.ExInternal;
import net.foxopen.fox.ex.ExModule;
import net.foxopen.fox.ex.ExSecurity;
import net.foxopen.fox.ex.ExServiceUnavailable;
import net.foxopen.fox.ex.ExUserRequest;

public final class ComponentText
extends FoxComponent
{

  private final String mName;
  private final String mType;
  private final FoxResponse mFoxResponse;
  private StringBuffer mText;

  public ComponentText(String pName, String pType, StringBuffer pStringBuffer, long pBrowserCacheMilliSec)
  {
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
