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
}
