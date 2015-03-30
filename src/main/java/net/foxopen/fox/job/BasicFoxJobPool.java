package net.foxopen.fox.job;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * JobPool for executing tasks in an ad-hoc, unscheduled manner.
 */
public class BasicFoxJobPool
extends FoxJobPool {

  private final ThreadPoolExecutor mExecutor;

  public static BasicFoxJobPool createSingleThreadedPool(String pPoolName) {
    BasicFoxJobPool lNewJobPool = new BasicFoxJobPool(pPoolName);
    registerPool(lNewJobPool);
    return lNewJobPool;
  }

  private BasicFoxJobPool(String pPoolName) {

    super(pPoolName);

    //Create a new ThreadPoolExecutor with the afterExecute method overriden so we can log errors.
    mExecutor = new ThreadPoolExecutor(1, 1, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<Runnable>(), createThreadFactory(pPoolName));
  }

  @Override
  protected ThreadPoolExecutor getExecutor() {
    return mExecutor;
  }

  public void submitTask(final FoxJobTask pTask) {
    getExecutor().execute(new RunnableFoxJobTask(pTask));
  }

}