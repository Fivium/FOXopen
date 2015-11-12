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

import com.google.common.base.Charsets;
import com.google.common.hash.Hashing;
import net.foxopen.fox.dom.DOM;
import net.foxopen.fox.ex.ExApp;
import net.foxopen.fox.ex.ExInternal;
import net.foxopen.fox.ex.ExModule;
import net.foxopen.fox.ex.ExSecurity;
import net.foxopen.fox.ex.ExServiceUnavailable;
import net.foxopen.fox.ex.ExUserRequest;
import net.foxopen.fox.io.IOUtil;
import net.foxopen.fox.module.Mod;
import net.foxopen.fox.track.Track;
import net.foxopen.fox.track.TrackTimer;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringWriter;


public abstract class FoxComponent {

  private final String mOptionalHash;

  protected FoxComponent(String pOptionalHash) {
    mOptionalHash = pOptionalHash;
  }

  /**
   * @return Gets the hash of this component if one was generated at construction time, or null if not.
   */
  public String getHashOrNull() {
    return mOptionalHash;
  }

  public abstract String getName();

  public abstract String getType();

  public abstract Reader getReader();

  public abstract InputStream getInputStream();

  // TODO - NP - Make this all StringBuildery
  @Deprecated
  public abstract FoxResponse processResponse(
    FoxRequest pRequest
  , StringBuffer pURLTail
  )
  throws
    ExInternal
  , ExSecurity            // when browser attempting to access thread owned by another session
  , ExUserRequest
  , ExModule
  , ExServiceUnavailable
  , Exception
  ;

  public static final FoxComponent createComponent(
    String pName
  , String pType
  , Reader pReader
  , InputStream pInputStream
  , App pApp
  , long pBrowserCacheMilliSec
  , boolean pGenerateHash
  )
  throws
    ExModule
  , ExServiceUnavailable
  , ExApp
  {

    Track.pushInfo("LoadFoxComponent");
    Track.addAttribute("name", pName);
    Track.addAttribute("type", pType);
    try {
      FoxComponent lFoxComponent;

      // Load Fox Module
      if("module".equals(pType)) {
        // Load Fox Module
        Track.pushInfo("ConstructModule", pName, TrackTimer.MODULE_LOAD);
        if (pReader == null) {
          // Belt and races check
          throw new ExInternal("Failed to create module: " + pName + ", no CLOB data found and reader ended up null");
        }
        try {
          // Load fox module source
          StringWriter lStringWriter = new StringWriter();
          try {
            IOUtil.transfer(pReader, lStringWriter);
          }
          catch (IOException e) {
            throw new ExInternal("Error reading module clob for module " + pName, e);
          }
          // Parse module to DOM
          DOM lUElem;
          try {
            lUElem = Mod.parseModuleDOM(lStringWriter.getBuffer(), pName, false);
          }
          catch (Throwable th) {
            throw new ExInternal("Error parsing DOM for module " + pName, th);
          }
          // Construct fox module

          try {
            Mod mod = new Mod(pName, lUElem, pApp);
            mod.validate(mod);
            lFoxComponent = mod;
          }
          catch (Throwable th) {
            throw new ExModule("Failed to parse module " + pName, th);
          }
        }
        finally {
          Track.pop("ConstructModule", TrackTimer.MODULE_LOAD);
        }
      }

      // Load CSS
      else if("text/css".equals(pType)) {
        if (pReader == null) {
          // Belt and races check
          throw new ExInternal("Failed to create CSS component: " + pName + ", no CLOB data found and reader ended up null");
        }
        StringBuffer lStringBuffer = XFUtil.toStringBuffer(pReader, 2048, -1);
        String lHash = pGenerateHash ? Hashing.md5().hashString(lStringBuffer, Charsets.UTF_16LE).toString() : null;
        lFoxComponent = new ComponentCSS(pName, lStringBuffer, pBrowserCacheMilliSec, lHash);
      }

      // Load image formats (various)
      else if("image".equals(pType) || pType.startsWith("image/")) {
        if (pInputStream == null) {
          // Belt and races check
          throw new ExInternal("Failed to create image component: " + pName + ", no BLOB data found and input stream ended up null");
        }
        byte[] lByteArray = XFUtil.toByteArray(pInputStream, 2048, -1);
        String lHash = pGenerateHash ? Hashing.md5().hashBytes(lByteArray).toString() : null;
        lFoxComponent = new ComponentImage(pName, lByteArray, pType, pBrowserCacheMilliSec, lHash);
      }

      // Load image formats (various)
      else if(pType.startsWith("text/")) {
        if (pReader == null) {
          // Belt and races check
          throw new ExInternal("Failed to create text component: " + pName + ", no CLOB data found and reader ended up null");
        }
        StringBuffer lStringBuffer = XFUtil.toStringBuffer(pReader, 2048, -1);
        String lHash = pGenerateHash ? Hashing.md5().hashString(lStringBuffer, Charsets.UTF_16LE).toString() : null;
        lFoxComponent = new ComponentText(pName, pType, lStringBuffer, pBrowserCacheMilliSec, lHash);
      }

      // Unknown object type
      else {
        throw new ExApp("Application " + pName + " Content Type not known: " + pType);
      }

      return lFoxComponent;
    }
    finally {
      Track.pop("LoadFoxComponent");
    }
  }

}
