package net.foxopen.fox.job;

import com.google.common.collect.EvictingQueue;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import net.foxopen.fox.enginestatus.EngineStatus;
import net.foxopen.fox.enginestatus.MessageLevel;
import net.foxopen.fox.enginestatus.StatusCollection;
import net.foxopen.fox.enginestatus.StatusDestination;
import net.foxopen.fox.enginestatus.StatusDetail;
import net.foxopen.fox.enginestatus.StatusMessage;
import net.foxopen.fox.enginestatus.StatusProvider;
import net.foxopen.fox.enginestatus.StatusTable;
import net.foxopen.fox.enginestatus.StatusText;
import net.foxopen.fox.ex.ExInternal;
import net.foxopen.fox.logging.FoxLogger;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.ListIterator;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Extension of a standard Java ThreadPoolExecutor which additionally captures errors and provides message reporting.
 * Individual work items should be submitted as FoxJobTasks.
 */
public class FoxJobPool {

  private static final String JOB_NAME_PREFIX = "Fox-Job-";
  private static final int LAST_ERROR_QUEUE_SIZE = 10;
  private static final int LAST_MESSAGE_QUEUE_SIZE = 20;

  public static final String STATUS_DATE_FORMAT = "dd-MM HH:mm:ss";

  static {
    EngineStatus.instance().registerStatusProvider(new JobPoolStatusProvider());
  }

  private static final Map<String, FoxJobPool> gPoolNameToPoolMap = new HashMap<>();

  private final String mPoolName;
  private final ThreadPoolExecutor mExecutor;
  private final Queue<ErrorInfo> mLastErrors = EvictingQueue.create(LAST_ERROR_QUEUE_SIZE);
  private final Queue<TaskCompletionMessage> mLastMessages = EvictingQueue.create(LAST_MESSAGE_QUEUE_SIZE);

  private static ThreadFactory createThreadFactory(String pPoolName) {
    return new ThreadFactoryBuilder()
      .setNameFormat(JOB_NAME_PREFIX + pPoolName +  "-%d")
      .setDaemon(true)
      .build();
  }

  public static FoxJobPool createSingleThreadedPool(String pPoolName) {
    FoxJobPool lNewJobPool = new FoxJobPool(pPoolName);

    //Check pool name is valid
    if(gPoolNameToPoolMap.containsKey(pPoolName)) {
      throw new ExInternal("Job pool " + pPoolName + " already defined");
    }
    gPoolNameToPoolMap.put(pPoolName, lNewJobPool);

    return lNewJobPool;
  }

  public static void shutdownAllPools() {
    FoxLogger.getLogger().info("Shutting down all job pools");
    synchronized (gPoolNameToPoolMap) {
      for (Map.Entry<String, FoxJobPool> lJobPoolEntry : gPoolNameToPoolMap.entrySet()) {
        lJobPoolEntry.getValue().mExecutor.shutdown();
        FoxLogger.getLogger().trace("Shutting down {} job pool", lJobPoolEntry.getKey());
      }
      gPoolNameToPoolMap.clear();
    }
  }

  private FoxJobPool(String pPoolName) {
    mPoolName = pPoolName;

    //Create a new ThreadPoolExecutor with the afterExecute method overriden so we can log errors.
    ThreadPoolExecutor lExecutor = new ThreadPoolExecutor(1, 1, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<Runnable>(), createThreadFactory(pPoolName)) {
      @Override
      protected void afterExecute(Runnable pRunnable, Throwable pThrowable) {
        super.afterExecute(pRunnable, pThrowable);
        if(pThrowable != null) {
          mLastErrors.add(new ErrorInfo(pThrowable, new Date()));
        }
      }
    };

    mExecutor = lExecutor;
  }

  public void submitTask(final FoxJobTask pTask) {
    mExecutor.execute(new Runnable() {
      public void run() {
        long lStartTime = System.currentTimeMillis();
        TaskCompletionMessage lCompletionMessage = pTask.executeTask();
        lCompletionMessage.setTimeTakenMS(System.currentTimeMillis() - lStartTime);
        mLastMessages.add(lCompletionMessage);
      }
    });
  }

  private static class JobPoolStatusProvider
  implements StatusProvider {

    @Override
    public void refreshStatus(StatusDestination pDestination) {

      StatusTable lTable = pDestination.addTable("Job Pool List", "Pool Name", "Current queue size",  "Total executions", "Last messages", "Last errors");
      lTable.setRowProvider(new StatusTable.RowProvider() {
        @Override
        public void generateRows(StatusTable.RowDestination pRowDestination) {
          DateFormat lDateFormat = new SimpleDateFormat(STATUS_DATE_FORMAT);

          for(FoxJobPool lPool : gPoolNameToPoolMap.values()) {
            //Create a list of recent errors from the pool
            StatusCollection lPoolErrors = new StatusCollection("lastErrors");
            //Reverse through the error list so most recent exceptions are up top
            ListIterator<ErrorInfo> lErrorIter = new ArrayList<>(lPool.mLastErrors).listIterator(lPool.mLastErrors.size());
            while(lErrorIter.hasPrevious()) {
              ErrorInfo lError = lErrorIter.previous();
              lPoolErrors.addItem(new StatusDetail(lDateFormat.format(lError.mTime), new StatusText(lError.mError.getMessage(), MessageLevel.ERROR)));
            }

            //Compile last messages into a list
            StatusCollection lMessageCollection = new StatusCollection("lastMessages");
            ListIterator<TaskCompletionMessage> lMessageIter = new ArrayList<>(lPool.mLastMessages).listIterator(lPool.mLastMessages.size());
            while(lMessageIter.hasPrevious()) {
              TaskCompletionMessage lMessage = lMessageIter.previous();
              lMessageCollection.addItem(new StatusMessage(lDateFormat.format(lMessage.getCompletionTime()), lMessage.getMessage() + " [completed in " + lMessage.getTimeTakenMS() + " ms]"));
            }

            pRowDestination.addRow(lPool.mPoolName)
              .setColumn(lPool.mPoolName)
              .setColumn(Integer.toString(lPool.mExecutor.getQueue().size()))
              .setColumn(Long.toString(lPool.mExecutor.getCompletedTaskCount()))
              .setColumn(new StatusDetail("Last Messages", lMessageCollection))
              .setColumn(lPoolErrors);
          }
        }
      });
    }

    @Override
    public String getCategoryTitle() {
      return "Job Pools";
    }

    @Override
    public String getCategoryMnemonic() {
      return "jobPools";
    }

    @Override
    public boolean isCategoryExpandedByDefault() {
      return true;
    }
  }

  private static class ErrorInfo {
    private final Throwable mError;
    private final Date mTime;

    private ErrorInfo(Throwable pError, Date pTime) {
      mError = pError;
      mTime = pTime;
    }
  }
}
