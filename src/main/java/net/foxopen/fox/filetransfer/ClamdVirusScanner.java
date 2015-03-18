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

import net.foxopen.fox.XFUtil;
import net.foxopen.fox.ex.ExVirusScan;
import net.foxopen.fox.logging.FoxLogger;
import net.taldius.clamav.ClamAVScanner;
import net.taldius.clamav.ClamAVScannerFactory;

import java.io.InputStream;

public class ClamdVirusScanner
extends VirusScanner{

  private ClamAVScanner mScanner;
  private String mMessage = null;
  private static final String STREAM_OK = "stream: OK";

  public ClamdVirusScanner(String pName, String pHost, int pPort, int pTimeout) {
    super(pName, pHost, pPort, pTimeout);
  }

  public void initialiseVirusScanner(){
    ClamAVScannerFactory.setClamdHost(mHost);
    ClamAVScannerFactory.setClamdPort(mPort);
    mScanner = ClamAVScannerFactory.getScanner();
  }

  public void scan(InputStream pInputStream)
  throws ExVirusScan{
    try {
      mScanner.performScan(pInputStream);
    }
    catch (Throwable ex) {
      FoxLogger.getLogger().error("Error performing virus scan", ex);
      mMessage = ex.getMessage();
      throw new ExVirusScan("Error performing virus scan: ", ex);
    }
    mMessage = mScanner.getMessage();
  }

  public boolean isVirusFound(){
    //null message means no virus found (yet)
    return !STREAM_OK.equals(XFUtil.nvl(mMessage, STREAM_OK));
  }

  public String getScanResultString(){
    return mMessage;
  }

}
