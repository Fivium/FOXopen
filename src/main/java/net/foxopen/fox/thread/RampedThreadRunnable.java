package net.foxopen.fox.thread;

import net.foxopen.fox.ex.ExUserRequest;

/**
 * Processing to be performed when the thread is fully ramped up.
 */
public interface RampedThreadRunnable {

  /**
   * Performs arbitrary processing against a ramped XThread.
   * @param pRequestContext ActionRequestContext from the ramped thread.
   * @throws ExUserRequest If the request is invalid.
   */
  void run(ActionRequestContext pRequestContext) throws ExUserRequest;

  /**
   * Determines if this Runnable should allow CallStackTransformations, which may be caused by opening the thread or
   * running an action. If true, CSTs will be processed and the thread's callstack will be modified as a result.
   * If false, CSTs are treated as errors. The default is false - this should only be set to true if the XThread is being
   * invoked in a way in which the CST can be immediately presented to the user (i.e. during a screen churn).
   * @return True if CSTs are allowed to be caused by processing this RampedThreadRunnable.
   */
  default boolean allowCallStackTransformation() {
    return false;
  }
}
