package net.foxopen.fox.thread;

/**
 * Used by the XThread rampAndRun methods to determine an object to return after action processing.
 * @param <T> Type of object to be returned by the ResultGenerator.
 */
public interface ThreadActionResultGenerator<T> {

  /**
   * Invoked after the thread has been ramped and a {@link RampedThreadRunnable} run against it.
   * @param pRequestContext ActionRequestContext for the ramped thread.
   * @return Arbitrary object of the desired return type, or null if no result is needed.
   */
  T establishResult(ActionRequestContext pRequestContext);
}
