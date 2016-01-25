package net.foxopen.fox.boot;

import net.foxopen.fox.ex.ExInternal;

/**
 * Records the result of an engine initialisation attempt from the {@link EngineInitialisationController}, including
 * error information for failed attempts. Initialisation attempts can fail either because of bad engine configuration,
 * or because the EngineInitialisationController refuses the request.
 */
public class InitialisationResult {

  /** True if the engine was correctly initialised.  */
  private final boolean mEngineInitialised;

  /** If the initialisation attempt failed, the reason for the failure - otherwise null. */
  private final String mFailReason;
  /** If the initialisation attempt failed, the last recorded initialisation exception - otherwise null. */
  private final Throwable mLastInitialisationException;

  /**
   * @return An InitialisationResult representing a successful initialisation attempt.
   */
  static InitialisationResult successfulInitialisation() {
    return new InitialisationResult();
  }

  /**
   * Creates an InitialisationResult representing a failed initialisation attempt.
   * @param pFailReason Human readable reason for the failure, to be reported in any eventual error message.
   * @param pLastInitialisationException Last recorded initialisation exception for the engine (this may not be the
   *                                     reason for the failure)
   * @return New InitialisationResult.
   */
  static InitialisationResult failedInitialisation(String pFailReason, Throwable pLastInitialisationException) {
    return new InitialisationResult(pFailReason, pLastInitialisationException);
  }

  private InitialisationResult() {
    mEngineInitialised = true;
    mFailReason = null;
    mLastInitialisationException = null;
  }

  private InitialisationResult(String pFailReason, Throwable pLastInitialisationException) {
    mEngineInitialised = false;
    mFailReason = pFailReason;
    mLastInitialisationException = pLastInitialisationException;
  }

  /**
   * @return True if the engine was successfully initialised, false otherwise.
   */
  public boolean isEngineInitialised() {
    return mEngineInitialised;
  }

  /**
   * Creates an exception to report the failure reason for a failed initialisation. The last engine initialisation exception
   * is reported as the cause of the created exception. If this InitialisationResult represents a successful initialisation,
   * this method should not be called. Important: this method does not throw the exception, it just creates it.
   * @return New unthrown exception for a failed initialisation.
   */
  public ExInternal asException() {

    if (mEngineInitialised) {
      throw new ExInternal("Cannot convert a successful InitialisationResult to an exception");
    }
    else {

      String lMessageText = "Failed to initialise FOX engine (" + mFailReason + ")";

      if(mLastInitialisationException != null) {
        return new ExInternal(lMessageText + ", last recorded initialisation error is below:", mLastInitialisationException);
      }
      else {
        return new ExInternal(lMessageText + ", no last recorded initialisation error is available");
      }
    }
  }
}
