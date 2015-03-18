package net.foxopen.fox;

import net.foxopen.fox.dom.DOM;
import net.foxopen.fox.ex.ExApp;
import net.foxopen.fox.ex.ExInternal;
import net.foxopen.fox.ex.ExTooFew;
import net.foxopen.fox.ex.ExTooMany;

public class VirusScannerDefinition {

  private static final String VIRUS_NAME_ELEMENT_NAME = "name";
  private static final String VIRUS_HOST_ELEMENT_NAME = "host";
  private static final String VIRUS_PORT_ELEMENT_NAME = "port";
  private static final String VIRUS_TIMEOUT_SECONDS_ELEMENT_NAME = "timeout-seconds";

  private final String mName;
  private final String mHost;
  private final int mPort;
  private final int mTimeoutSeconds;

  public static VirusScannerDefinition fromDOM(DOM pVirusScannerDOM) throws ExApp {
    try {
      String lName = pVirusScannerDOM.get1S(VIRUS_NAME_ELEMENT_NAME);
      String lHost = pVirusScannerDOM.get1S(VIRUS_HOST_ELEMENT_NAME);
      int lPort = Integer.parseInt(pVirusScannerDOM.get1S(VIRUS_PORT_ELEMENT_NAME));
      int lTimeoutSeconds = Integer.parseInt(pVirusScannerDOM.get1S(VIRUS_TIMEOUT_SECONDS_ELEMENT_NAME));

      if(!"CLAMD".equals(lName)){
        throw new ExInternal("Only CLAMD VirusScanner supported at this time.");
      }
      else {
        return new VirusScannerDefinition(lName, lHost, lPort, lTimeoutSeconds);
      }
    }
    catch (NumberFormatException e) {
      throw new ExApp("When creating a virus scanner the port or timeout seconds was not a number.", e);
    }
    catch (ExTooMany | ExTooFew e) {
      throw new ExApp("When creating a virus scanner an element was not found or there were more than one.", e);
    }
  }

  private VirusScannerDefinition(String pName, String pHost, int pPort, int pTimeoutSeconds) {
    mName = pName;
    mHost = pHost;
    mPort = pPort;
    mTimeoutSeconds = pTimeoutSeconds;
  }

  public String getName() {
    return mName;
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
