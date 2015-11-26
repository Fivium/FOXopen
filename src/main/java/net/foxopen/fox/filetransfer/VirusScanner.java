package net.foxopen.fox.filetransfer;

import net.foxopen.fox.ex.ExVirusScan;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public abstract class VirusScanner
implements Runnable{

  protected InputStream mInputStream = null;
  protected OutputStream mOutputStream = null;

  protected final String mHost;
  protected final int mPort;
  protected final int mTimeout;

  private Thread mRunThread;
  private boolean mComplete;

  private ExVirusScan mException = null;

  private static final byte[] EICAR_VIRUS = {'X','5','O','!','P','%','@','A','P','[','4','\\','P','Z','X','5','4','(','P','^',')','7','C','C',')','7','}','$','E','I','C','A','R','-','S','T','A','N','D','A','R','D','-','A','N','T','I','V','I','R','U','S','-','T','E','S','T','-','F','I','L','E','!','$','H','+','H','*'};

  public static VirusScanner createVirusScanner(VirusScannerDefinition pDefinition){

    //TODO this will need to be dynamic at some point. For now just support Clam (we've already checked the definition is type=CLAMD)
    return new ClamdVirusScanner(pDefinition.getHost(), pDefinition.getPort(), pDefinition.getTimeoutSeconds());
  }

  protected VirusScanner(String pHost, int pPort, int pTimeout) {
    mHost = pHost;
    mPort = pPort;
    mTimeout = pTimeout;
  }

  public void setInputStream(InputStream pInputStream){
    mInputStream = pInputStream;
  }

  public void setOutputStream(OutputStream pOutputStream){
    mOutputStream = pOutputStream;
  }

  /**
   * Get the OutputStream to write to in the main thread - it will then be
   * passed to the virus scanner on this scanner's associated InputStream.
   * Do NOT write to this stream from the VirusScanner thread (i.e. in the scan()
   * method) - this will cause a deadlock.
   * @return an OutputStream
   */
  public OutputStream getOutputStream(){
    return mOutputStream;
  }

  public InputStream getInputStream(){
    return mInputStream;
  }

  public abstract String getType();

  public void run(){
    mRunThread = Thread.currentThread();
    if(mInputStream == null || mOutputStream == null){
      handleError(new ExVirusScan("InputStream and/or OutputStream cannot be null"));
      return;
    }

    initialiseVirusScanner();

    try{
      scan(mInputStream);
    } catch(ExVirusScan ex){
      handleError(ex);
    } finally {
      //Close the input stream to stop the main thread hanging infinitely on
      //write attempts.
      if(mInputStream != null){
        try {
          mInputStream.close();
        } catch (IOException ignore) {}
      }
    }
    mComplete = true;
    //Thread will now die
  }

  /**
   * Attempts to connect to the VirusScanner using the supplied connection info.
   * A test virus is then supplied to the Scanner. If it is not detected the scanner
   * is deemed unfunctional.
   * @return true on successful connection and detection, false otherwise.
   */
  public boolean testConnectionAndScanner(){
    initialiseVirusScanner();
    ByteArrayInputStream lVirusBAIS = new ByteArrayInputStream(EICAR_VIRUS);
    try{
      scan(lVirusBAIS);
    } catch(ExVirusScan ex){
      return false;
    }
    return isVirusFound();
  }

  /**
   *
   * @return The Thread which is executing this object's run() method.
   */
  public Thread getCurrentThread(){
    return mRunThread;
  }

  private void handleError(ExVirusScan pEx){
    mException = pEx;
    mComplete = true;
  }

  public boolean isError(){
    return mException != null;
  }

  public String getErrorMessage(){
    if(mException!=null)
      return mException.getMessage();
    else
      return null;
  }

  public String getHost() {
    return mHost;
  }

  /**
   *
   * @return Amount of time to wait for scanner to complete (i.e. after the
   * upload is complete) in seconds.
   */
  public int getTimeoutSecs(){
    return mTimeout;
  }

  /**
   * Scans given input stream, waiting until the scan completes or an error occurs.
   * @param pInputStream
   * @throws ExVirusScan
   */
  public abstract void scan(InputStream pInputStream) throws ExVirusScan;

  /**
   *
   * @return Detail message from scanner, if available.
   */
  public abstract String getScanResultString();

  /**
   *
   * @return true if VirusScanner has found a virus, false otherwise. To detect
   * errors use isError().
   */
  public abstract boolean isVirusFound();

  /**
   *
   * @return True if VirusScanner has completed its scan, regardless of its result,
   * or if an error occurred. False if scan is not started or not complete.
   */
  public boolean isComplete(){
    return mComplete;
  }

  /**
   * Set up the VirusScanner in preparation for a call to scan().
   */
  public abstract void initialiseVirusScanner();

}
