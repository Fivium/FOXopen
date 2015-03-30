package net.foxopen.fox.job;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * JobPool which runs jobs according to a fixed schedule. Currently only a singleton instance is required which only
 * executes its jobs daily at midnight.
 */
public class ScheduledFoxJobPool
extends FoxJobPool {

  private final ScheduledThreadPoolExecutor mExecutor;

  private static final ScheduledFoxJobPool INSTANCE = createPool("ScheduledFoxJobPoolInstance");
  public static ScheduledFoxJobPool instance() {
    return INSTANCE;
  }

  private static ScheduledFoxJobPool createPool(String pPoolName) {
    ScheduledFoxJobPool lNewJobPool = new ScheduledFoxJobPool(pPoolName);
    registerPool(lNewJobPool);
    return lNewJobPool;
  }

  private ScheduledFoxJobPool(String pPoolName) {
    super(pPoolName);
    mExecutor = new ScheduledThreadPoolExecutor(1, createThreadFactory(pPoolName));
  }

  /**
   * Schedule a task to run every day at midnight.
   * TODO - make this more flexible
   * @param pTask Task to repeatedly run.
   */
  public void scheduleTask(final FoxJobTask pTask) {

    //Create calendar set to midnight today
    Calendar lMidnightTomorrow = new GregorianCalendar();
    lMidnightTomorrow.set(Calendar.HOUR_OF_DAY, 0);
    lMidnightTomorrow.set(Calendar.MINUTE, 0);
    lMidnightTomorrow.set(Calendar.SECOND, 0);
    lMidnightTomorrow.set(Calendar.MILLISECOND, 0);

    //Add one day to get midnight tomorrow
    lMidnightTomorrow.add(Calendar.DAY_OF_MONTH, 1);

    long lDelay = lMidnightTomorrow.getTimeInMillis() - System.currentTimeMillis();

    String lFirstRunTime = new SimpleDateFormat(FoxJobPool.STATUS_DATE_FORMAT).format(lMidnightTomorrow.getTime());
    registerMessage(new TaskCompletionMessage(pTask, "Next run scheduled for " + lFirstRunTime + " (in " + TimeUnit.MILLISECONDS.toMinutes(lDelay) + " mins)"));

    //Schedule task to run daily
    mExecutor.scheduleAtFixedRate(new RunnableFoxJobTask(pTask), lDelay, TimeUnit.DAYS.toMillis(1), TimeUnit.MILLISECONDS);
  }

  @Override
  protected ThreadPoolExecutor getExecutor() {
    return mExecutor;
  }
}
