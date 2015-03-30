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
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * Extension of a standard Java ThreadPoolExecutor which additionally captures errors and provides message reporting.
 * Individual work items should be submitted as FoxJobTasks. Subclasses control the ThreadPoolExecutor used to provide
 * the implementation.
 */
public abstract class FoxJobPool {

  private static final String JOB_NAME_PREFIX = "Fox-Job-";
  private static final int LAST_ERROR_QUEUE_SIZE = 10;
  private static final int LAST_MESSAGE_QUEUE_SIZE = 20;

  public static final String STATUS_DATE_FORMAT = "dd-MM HH:mm:ss";

  static {
    EngineStatus.instance().registerStatusProvider(new JobPoolStatusProvider());
  }

  private static final Map<String, FoxJobPool> gPoolNameToPoolMap = new HashMap<>();

  private final String mPoolName;
  private final Queue<ErrorInfo> mLastErrors = EvictingQueue.create(LAST_ERROR_QUEUE_SIZE);
  private final Queue<TaskCompletionMessage> mLastMessages = EvictingQueue.create(LAST_MESSAGE_QUEUE_SIZE);

  protected static void registerPool(FoxJobPool pNewJobPool) {

    String lPoolName = pNewJobPool.mPoolName;

    //Check pool name is valid
    if(gPoolNameToPoolMap.containsKey(lPoolName)) {
      throw new ExInternal("Job pool " + lPoolName + " already defined");
    }
    gPoolNameToPoolMap.put(lPoolName, pNewJobPool);

  }

  protected static ThreadFactory createThreadFactory(String pPoolName) {
    return new ThreadFactoryBuilder()
      .setNameFormat(JOB_NAME_PREFIX + pPoolName +  "-%d")
      .setDaemon(true)
      .build();
  }

  public static void shutdownAllPools() {
    FoxLogger.getLogger().info("Shutting down all job pools");
    synchronized (gPoolNameToPoolMap) {
      for (Map.Entry<String, FoxJobPool> lJobPoolEntry : gPoolNameToPoolMap.entrySet()) {
        lJobPoolEntry.getValue().getExecutor().shutdown();
        FoxLogger.getLogger().trace("Shutting down {} job pool", lJobPoolEntry.getKey());
      }
      gPoolNameToPoolMap.clear();
    }
  }

  protected FoxJobPool(String pPoolName) {
    mPoolName = pPoolName;
  }

  protected void registerError(Throwable pThrowable) {
    if(pThrowable != null) {
      mLastErrors.add(new ErrorInfo(pThrowable, new Date()));
    }
  }

  protected void registerMessage(TaskCompletionMessage pMessage) {
    mLastMessages.add(pMessage);
  }

  /**
   * Gets the underlying executor for running tasks.
   * @return
   */
  protected abstract ThreadPoolExecutor getExecutor();

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
              String lStatusMessageString = "(" + lMessage.getTaskDescription() + ") " + lMessage.getMessage() + " [completed in " + lMessage.getTimeTakenMS() + " ms]";
              StatusMessage lStatusMessage = new StatusMessage(lDateFormat.format(lMessage.getCompletionTime()), lStatusMessageString);
              lMessageCollection.addItem(lStatusMessage);
            }

            pRowDestination.addRow(lPool.mPoolName)
              .setColumn(lPool.mPoolName)
              .setColumn(Integer.toString(lPool.getExecutor().getQueue().size()))
              .setColumn(Long.toString(lPool.getExecutor().getCompletedTaskCount()))
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

  /**
   * Runnable for running in a job pool and handling error/message reporting.
   */
  protected class RunnableFoxJobTask
  implements Runnable {

    private final FoxJobTask mTask;

    RunnableFoxJobTask(FoxJobTask pTask) {
      mTask = pTask;
    }

    public void run() {
      try {
        long lStartTime = System.currentTimeMillis();
        TaskCompletionMessage lCompletionMessage = mTask.executeTask();
        lCompletionMessage.setTimeTakenMS(System.currentTimeMillis() - lStartTime);
        registerMessage(lCompletionMessage);
      }
      catch (Throwable th) {
        registerError(th);
      }
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
