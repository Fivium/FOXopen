package net.foxopen.fox.filetransfer;

import net.foxopen.fox.dom.DOM;
import net.foxopen.fox.ex.ExApp;
import net.foxopen.fox.ex.ExInternal;
import net.foxopen.fox.ex.ExTooFew;
import net.foxopen.fox.ex.ExTooMany;

public class VirusScannerDefinition {

  public static final String VIRUS_SCANNER_DEFINITION_TYPE = "type";
  private static final String VIRUS_SCANNER_DEFINITION_HOST = "host";
  private static final String VIRUS_SCANNER_DEFINITION_PORT = "port";
  private static final String VIRUS_SCANNER_DEFINITION_TIMEOUT_SECS = "timeout-seconds";

  private final String mType;
  private final String mHost;
  private final int mPort;
  private final int mTimeoutSeconds;

  public static VirusScannerDefinition fromDOM(DOM pVirusScannerDOM) throws ExApp {
    try {
      String lType = pVirusScannerDOM.get1S(VIRUS_SCANNER_DEFINITION_TYPE);

      if(!ClamdVirusScanner.SCANNER_TYPE.equals(lType)){
        throw new ExInternal("Only CLAMD VirusScanner supported at this time (invalid type: " + lType + ")");
      }
      else {
        String lHost = pVirusScannerDOM.get1S(VIRUS_SCANNER_DEFINITION_HOST);
        int lPort = Integer.parseInt(pVirusScannerDOM.get1S(VIRUS_SCANNER_DEFINITION_PORT));
        int lTimeoutSeconds = Integer.parseInt(pVirusScannerDOM.get1S(VIRUS_SCANNER_DEFINITION_TIMEOUT_SECS));

        return new VirusScannerDefinition(lType, lHost, lPort, lTimeoutSeconds);
      }
    }
    catch (NumberFormatException e) {
      throw new ExApp("When creating a virus scanner the port or timeout seconds was not a number.", e);
    }
    catch (ExTooMany | ExTooFew e) {
      throw new ExApp("When creating a virus scanner an element was not found or there were more than one.", e);
    }
  }

  private VirusScannerDefinition(String pType, String pHost, int pPort, int pTimeoutSeconds) {
    mType = pType;
    mHost = pHost;
    mPort = pPort;
    mTimeoutSeconds = pTimeoutSeconds;
  }

  public String getType() {
    return mType;
  }

  public String getHost() {
    return mHost;
  }

  public int getPort() {
    return mPort;
  }

  public int getTimeoutSeconds() {
    return mTimeoutSeconds;
  }
}
