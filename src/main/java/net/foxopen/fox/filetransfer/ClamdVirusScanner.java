package net.foxopen.fox.filetransfer;

import net.foxopen.fox.XFUtil;
import net.foxopen.fox.ex.ExVirusScan;
import net.foxopen.fox.logging.FoxLogger;
import net.taldius.clamav.ClamAVScanner;
import net.taldius.clamav.impl.NetworkScanner;

import java.io.InputStream;

public class ClamdVirusScanner
extends VirusScanner{

  public static final String SCANNER_TYPE = "CLAMD";

  //Value taken from ClamAVScannerFactory (note: factory not used as it is not thread-safe)
  private static final int CLAM_NETWORK_CONNECTION_TIMEOUT = 90;

  private ClamAVScanner mScanner;
  private String mMessage = null;
  private static final String STREAM_OK = "stream: OK";

  public ClamdVirusScanner(String pHost, int pPort, int pTimeout) {
    super(pHost, pPort, pTimeout);
  }

  public void initialiseVirusScanner(){
    //Manually create a Clam scanner
    //ClamAVScannerFactory is not used as it is not thread-safe
    NetworkScanner lNetworkScanner = new NetworkScanner();
    lNetworkScanner.setClamdHost(mHost);
    lNetworkScanner.setClamdPort(mPort);
    lNetworkScanner.setConnectionTimeout(CLAM_NETWORK_CONNECTION_TIMEOUT);

    mScanner = lNetworkScanner;
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

  @Override
  public String getType() {
    return SCANNER_TYPE;
  }
}
