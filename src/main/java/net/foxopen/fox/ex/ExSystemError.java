package net.foxopen.fox.ex;

import net.foxopen.fox.App;
import net.foxopen.fox.module.Mod;
import net.foxopen.fox.module.State;
import net.foxopen.fox.module.entrytheme.EntryTheme;


/**
 * A top-level exception that wraps other exceptions caught at the upper levels of
 * an main entry call to Fox. This exception can be used to record properties
 * concerning the error or exception that it wraps.
 *
 * <p>If this exception is thrown, we are unable to recover from some unexpected
 * system error and the client will be sent to a 'System Busy or Encountered an
 * unexpected error' page.
 */
public class ExSystemError extends RuntimeException
{
  private transient ErrorContext mErrorContext = new ErrorContext();

  public ExSystemError() {
    mErrorContext.setErrorID(Long.toString(System.currentTimeMillis()));
  }

  public String getErrorId() {
    return Long.toString(System.currentTimeMillis());
  }

  public final ErrorContext getErrorProperties() {
    return mErrorContext;
  }

  public final void setMessage(String pMessage) {
    mErrorContext.setMessage(pMessage);
  }

  public final void setException(Throwable pException) {
    mErrorContext.setException(pException);
  }

  public final void setState(State pState) {
    mErrorContext.setState(pState);
  }

  public final void setTheme(EntryTheme pTheme) {
    mErrorContext.setTheme(pTheme);
  }

  public final void setModule(Mod pModule) {
    mErrorContext.setModule(pModule);
  }

  public final void setApp(App pApp) {
    mErrorContext.setApp(pApp);
  }

  public final void setErrorID(String pErrorID) {
    mErrorContext.setErrorID(pErrorID);
  }

  public String getMessage () {
    // Get the context exception
    Throwable lThrowable = mErrorContext.getException();

    // If it's another ExSystemError, message will be null, so we need to get to the real exception
    // Recurse while lThrowable is still an ExSystemError and while we have a readable property
    while (lThrowable instanceof ExSystemError && ((ExSystemError)lThrowable).getErrorProperties().getException() != null) {
      lThrowable = ((ExSystemError)lThrowable).getErrorProperties().getException();
    }

    return lThrowable.getMessage();
  }
}
