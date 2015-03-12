package net.foxopen.fox.track;


/**
 * Predefined timer names for tracking the elapsed time of common operations. This enum provides consumers with a safe
 * way to share timer information. Timers do not have to be based on one of these enum values.
 */
public enum TrackTimer {
  MODULE_LOAD,
  THREAD_DESERIALISE,
  THREAD_LOCK,
  THREAD_RAMP_UP,
  ACTION_PROCESSING,
  OUTPUT_GENERATION,
  THREAD_RAMP_DOWN,
  THREAD_SERIALISE,
  THREAD_UNLOCK,
  THREAD_LOCK_MANAGEMENT(true),
  GET_CONNECTION(true),
  DATABASE_EXECUTION(true);

  private final boolean mReportInSummary;
  private final boolean mCumulative;

  private TrackTimer() {
    mReportInSummary = true;
    mCumulative = false;
  }

  private TrackTimer(boolean pCumulative) {
    mReportInSummary = true;
    mCumulative = pCumulative;
  }

  String getName() {
    return name();
  }

  /**
   * True if this property should be reported by the Track timer summary.
   * @return
   */
  boolean isReportInSummary() {
    return mReportInSummary;
  }

  public boolean isCumulative() {
    return mCumulative;
  }
}
