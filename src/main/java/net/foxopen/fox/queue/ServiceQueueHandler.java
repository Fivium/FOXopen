/*

Copyright (c) 2010, UK DEPARTMENT OF ENERGY AND CLIMATE CHANGE -
                    ENERGY DEVELOPMENT UNIT (IT UNIT)
All rights reserved.

Redistribution and use in source and binary forms, with or without modification,
are permitted provided that the following conditions are met:

    * Redistributions of source code must retain the above copyright notice,
      this list of conditions and the following disclaimer.
    * Redistributions in binary form must reproduce the above copyright notice,
      this list of conditions and the following disclaimer in the documentation
      and/or other materials provided with the distribution.
    * Neither the name of the DEPARTMENT OF ENERGY AND CLIMATE CHANGE nor the
      names of its contributors may be used to endorse or promote products
      derived from this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR
ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
(INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
(INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

$Id$

*/
package net.foxopen.fox.queue;

import net.foxopen.fox.XFUtil;
import net.foxopen.fox.ex.ExInternal;
import net.foxopen.fox.logging.FoxLogger;
import net.foxopen.fox.track.Track;

import java.util.*;

/**
 * NOTE: Sync on this class for operations involving gQueueHandlerMap:
 *
 *   * destroyAllQueueHandlers
 *   * getQueueHandlerByName
 *   * <init>
 *
 * This prevents flushing issues where the map is empty and a request for a
 * QueueHandler is made before it is populated. It is not good enough to sync
 * on the map itself as the map object is recreated when a ServiceQueueHandler
 * is destroyed.
 */
public class ServiceQueueHandler {

  public static final String STANDARD_WORKITEM_TYPE = "STANDARD_WORKITEM";
  public static final String UPLOAD_WORKITEM_TYPE =  "UPLOAD_WORKITEM";
  public static final String DOWNLOAD_WORKITEM_TYPE =  "DOWNLOAD_WORKITEM";
  private static final int NUM_OF_THREADS_DEFAULT = 1;
  //How frequently to 'bounce' (close and open, forcing a database write) worker tracks
  private static final int TRACK_BOUNCE_TIME_MS = 20 * 1000;

  private static Map gQueueHandlerMap = new HashMap();

  private final int mWorkerSleepTime;
  private final LinkedList mNextServiceQueueList = new LinkedList();
  private final Map mServiceQueueNameToServiceQueueMap = new HashMap();

  private final QueueWorker[] mThreads;
  private final String mQueueHandlerName;

  private boolean mDestroyFlag = false;
  private List mThreadsWithPendingAdds = new LinkedList();

  private Map mAttributeSet = new HashMap();

  public ServiceQueueHandler(int pNumberOfThreads, String pQueueHandlerName, int pWorkerSleepTime) {
    mQueueHandlerName = pQueueHandlerName;
    mWorkerSleepTime = pWorkerSleepTime;

    // Place this handler into the static handler map
    synchronized (gQueueHandlerMap) {
      gQueueHandlerMap.put(pQueueHandlerName, this);
    }

    // default the number of threads if the provided value was null
    int lNumberOfThreads = XFUtil.isNull(new Integer(pNumberOfThreads)) ? NUM_OF_THREADS_DEFAULT : pNumberOfThreads;

    // Create an array to hold pool worker threads
    mThreads = new QueueWorker[lNumberOfThreads];

    ThreadGroup lThreadGroup = new ThreadGroup("QueueWorkerThreads" + mQueueHandlerName);

    for (int i = 0; i < lNumberOfThreads; i++) {
      mThreads[i] = new QueueWorker(this, lThreadGroup, i+1);
      mThreads[i].start(); // set the thread on its way
    }
  }

  public final synchronized void assignServiceQueue(ServiceQueue pQueue) {
    if(mDestroyFlag){
      throw new ExInternal("Attempted to assign ServiceQueue (" + pQueue.getName() + ") to a stale ServiceQueueHandler.");
    }
    mNextServiceQueueList.addLast(pQueue); // maintain a linked list for managing queue service order
    mServiceQueueNameToServiceQueueMap.put(pQueue.getName(), pQueue); // maintain a map for name operations
  }

  /**
   * Loops through this Handler's list and marks each Queue for destruction. Doing so
   * will either:
   *
   * a) immediately remove the Queue from the list and associated map
   * b) wait for the Queue to complete any work it is doing and then remove the
   *    Queue from the list and the map.
   *
   * In either case the Queue will eventually call removeServiceQueue, which will destroy
   * the QueueWorkers when no Queues are left.
   */
  public void destroy() {

    synchronized(this){
    mDestroyFlag = true;
    }

   //Other threads may have indicated that they intend to add work to this object
   //in the near future. We wait a reasonable amount of time for this work to be added
   //(when it is added, this thread is notified). Otherwise work may be added to
   //this queue when it is in a destroyed state.
    synchronized(mThreadsWithPendingAdds){
      while(!mThreadsWithPendingAdds.isEmpty()){
        try {
          //make a record of the thread we're waiting for
          Thread lWaitingOnThread = (Thread) mThreadsWithPendingAdds.get(0);
          mThreadsWithPendingAdds.wait(2500);
          //forcibly remove the thread from the queue (it took too long to add work)
          mThreadsWithPendingAdds.remove(lWaitingOnThread);
        } catch (InterruptedException e) {
          mThreadsWithPendingAdds.clear(); //Shouldn't happen but if it does, break the loop
        }
      }
    }

    synchronized(this){

    int lQueueSize = mNextServiceQueueList.size();
    int lRemoved = 0, lRemaining = 0, i = 0;

    //queue size will be 0 if this handler was created but not provided with any queues
    //so call removeServiceQueue to force destruction of threads
    if(lQueueSize == 0){
      removeServiceQueue(null);
    }

    //need a little cleverness here as list size is volatile during this loop
    while(lRemoved + lRemaining != lQueueSize){
       if(((ServiceQueue)mNextServiceQueueList.get(i)).destroy())
         lRemoved++;
       else{
         lRemaining++;
         i++;
       }
    }
  }
  }

   /**
   * Remove a stale queue from the list and map maintained by this Handler
   * @param pQueue - can be null
   */
  public final synchronized void removeServiceQueue(ServiceQueue pQueue) {

    if(pQueue != null){
      if(!mNextServiceQueueList.remove(pQueue)){
        throw new ExInternal("Attempted to remove a queue that was not in list");
      }
      mServiceQueueNameToServiceQueueMap.remove(pQueue.getName());
    }

    if(mNextServiceQueueList.size() == 0){
      //All queues have been destroyed. No work exists in any Queue known by this
      //Handler. It is therefore safe to destroy the QueueWorker Threads.
      for(int i=0; i<mThreads.length; i++){
        mThreads[i].interrupt();
      }
    }

  }

  // TODO TO REMOVE IN FUTURE AFTER APP CHANGES
  public final synchronized boolean queueNameExists(String pName) {
    return mServiceQueueNameToServiceQueueMap.containsKey(pName);
  }

  public static ServiceQueueHandler getQueueHandlerByName(String pName) {
    return getQueueHandlerByName(pName, false);
  }

  public static ServiceQueueHandler getQueueHandlerByName(String pName, boolean pPrepareForWork) {
    synchronized(gQueueHandlerMap){
      ServiceQueueHandler lServiceQueue = (ServiceQueueHandler)gQueueHandlerMap.get(pName);
      if(pPrepareForWork){
        synchronized(lServiceQueue.mThreadsWithPendingAdds){
          if(!lServiceQueue.mThreadsWithPendingAdds.contains(Thread.currentThread()))
            lServiceQueue.mThreadsWithPendingAdds.add(Thread.currentThread());
          }
      }
      return lServiceQueue;
    }
  }

  /**
   * Destroy all the ServiceQueueHandlers this class knows about, and create a fresh
   * global map to store new ones, preventing any consumers from adding to stale queues.
   */
  public static void destroyAllQueueHandlers() {
    FoxLogger.getLogger().info("Shutting down upload service queues");
    synchronized(gQueueHandlerMap){
      for(Iterator it = gQueueHandlerMap.keySet().iterator(); it.hasNext();){
        ((ServiceQueueHandler)gQueueHandlerMap.get(it.next())).destroy();
      }
      gQueueHandlerMap = new HashMap();
    }
  }

  public final synchronized void setHandlerAttribute(String pKey, Object pValue) {
      mAttributeSet.put(pKey,pValue);
  }

  public final synchronized Object getHandlerAttribute(String pKey) {
    return mAttributeSet.get(pKey);
  }

  // TODO Remove the work item type param as it is already held within the work item object
  public void addItemToQueue(WorkItem pWorkItem, String pWorkItemType) //called from consumer
  throws InterruptedException {

    // Initial sync is required here while suspending the current thread as we want the monitor of the work item to be held so a
    // queue worker can pass control back at a later time using notify on the work item used in this method.  Must be performed to ensure
    // that worker doesnt try and return control to original thread before the thread starts to wait.  Subtle so be careful when editing.
    ITEM_SYNC: synchronized (pWorkItem) {
      try {

        // Sync on this since we are accessing the handlers private queues
        QUEUE_HANDLER_SYNC: synchronized (this) {

          // loop through all queues this handler knows about
          Iterator lQueueIterator;
          //belt and braces check to stop anyone adding items to this ServiceQueueHandler if it has been marked for destroy
          DESTROY_ADD_SYNC: synchronized (mThreadsWithPendingAdds) {

            if(mDestroyFlag && mThreadsWithPendingAdds.remove(Thread.currentThread())){

              mThreadsWithPendingAdds.notify();
              //A thread wanting to destroy this QueueHandler will now be able to attempt to
              //after we leave the DESTROY_ADD_SYNC block
            } else if(mDestroyFlag){
              throw new ExInternal("Attempted to add a work item via a stale ServiceQueueHandler");
            }

            lQueueIterator = mServiceQueueNameToServiceQueueMap.values().iterator();

          // Can't add an item to a queue if you havnt added a queue to the system
          if (!lQueueIterator.hasNext()) {
            throw new ExInternal("Failed to add work item to queue using Queue Handler: \"" + mQueueHandlerName + "\".  No queues have been added to this handlers queue listing.");
          }

          // Hunt for a queue that this handler knows about who will accept the work we are trying to process
          boolean lQueueHit = false;
          QUEUE_ITERATOR: while(lQueueIterator.hasNext()) {
            ServiceQueue lTempQueue = (ServiceQueue)lQueueIterator.next();
            // Try to add workItem to queue and break from loop if successful
            if(lTempQueue.addNewWorkItem(pWorkItem, pWorkItemType)) {
              lQueueHit = true;
              break QUEUE_ITERATOR;
            }
          } // QUEUE_ITERATOR
          if(!lQueueHit) {
            throw new ExInternal("Failed to find a service queue willing to accept the current work item for \"" + mQueueHandlerName + "\" queue handler.");
          }

          }//DESTROY_ADD_SYNC

          // arbitrarily wake up one of the worker threads waiting on this object (Work has arrived)
          this.notify();

        } // QUEUE_HANDLER_SYNC (On coming out of this sync a worker thread will begin to process work items)

        // wait until notified that thread can resume after work item complete
        pWorkItem.wait();

      }
      catch(InterruptedException e) {
        // if the original thread is interrupted from wait then throw e
        throw e;
      } finally {
        //Just in case - will prevent destroy() being caught in an infinite loop
        synchronized(mThreadsWithPendingAdds){
          mThreadsWithPendingAdds.remove(Thread.currentThread());
        }
      }

    } // ITEM_SYNC

  }

  private final WorkItem checkOutWorkItemOrWait(QueueWorker pQueueWorker)  // called from QueueWorker
  throws InterruptedException {
    WorkItem lWorkItem = null;
    ServiceQueue lServiceQueue = null;

    FOREVER_LOOP: while(true) {

      SYNC_SERVICE_QUEUE: synchronized(this) {
        // Loop through queue collection and return any work that needs processing
        // Ensure that its picking up from a different queue each time... don't want to be bias towards particular queue or workItem
        HUNT_LOOP: for (int i = 0; i < mNextServiceQueueList.size(); i++) {
          // Remove the first Queue from the collection
          lServiceQueue = (ServiceQueue)mNextServiceQueueList.removeFirst();
          // Try to check out work from the current queue
          lWorkItem = lServiceQueue.checkOut();
          // Place this queue to the back of the linked list so it has to wait for more service
          mNextServiceQueueList.addLast(lServiceQueue);
          // Have we been given some pending work
          if (lWorkItem != null) break HUNT_LOOP;
        } // HUNT_LOOP

        // belt and braces check to ensure that linked list size matches map size
        if (mNextServiceQueueList.size() != mServiceQueueNameToServiceQueueMap.size()) {
          throw new ExInternal("Failed to match the size of queue linked list with queue map in queue handler \""+ mQueueHandlerName +"\".  Ensure check out work item is working correctly.");
        }

        // Validate and return work item
        if(lWorkItem != null) {
          if(lWorkItem.isFailed()) {
//            Track.trackAddElementChild("WorkToProcess", "THIS SHOULD NEVER HAPPEN: Found work item in queue that has been failed with class type \""+lWorkItem.getWorkItemType()+"\".  Failed work items should be removed from service queues on check in.");
            throw new ExInternal("Found work item in queue that has been failed with class type \""+lWorkItem.getWorkItemType()+"\".  Failed work items should be removed from service queues on check in.");
          }
          return lWorkItem;
        }

        // No work - goto sleep
        try {
          // This thread has found no work so return to thread pool
          this.wait();

          //reset track time after waking up - prevents the QueueWorker bouncing
          //its track unnecessarily.
          pQueueWorker.resetTrackTime();
        }
        catch (InterruptedException e) {
          // InterruptedException only thrown by wait() if this thread is explicitly interrupted.
          // On a notify(), wait() just returns and flow continues as normal.
          throw e;
        }
      } // SYNC_SERVICE_QUEUE

    } // FOREVER_LOOP

  }

  private final synchronized void checkInOrSurrenderWorkItem(WorkItem pWorkItem) {

    ServiceQueue lServiceQueue;

    // get queue from mWorkItemSourceMap
    lServiceQueue = (ServiceQueue)(pWorkItem.getOwningServiceQueue());

    if (lServiceQueue == null) {
      throw new ExInternal("Failed to find a queue to return work item to.  Perhaps work item didnt have a reference to its owning queue.");
    }

    // Check work item back in
    lServiceQueue.checkIn(pWorkItem);
  }


  /**
   * Sub Class QueueWorker objects are threads which call to thier owning ServiceQueueHandler
   * attempting to receive a portion of work to process.  The QueueWorker knows nothing
   * of the implementation of a WorkItem and simply gets the current thread to run the
   * WorkItem's execute method.  If there is no work then workers will wait for notification
   * that new work has arrived.
   */
  private class QueueWorker
  extends Thread{

    ServiceQueueHandler mOwningServiceQueueHandler;
    private long mLastTrackTime;

    public QueueWorker(ServiceQueueHandler pServiceQueueHandler, ThreadGroup pWorkerThreadGroup, int pThreadNo){
      super(pWorkerThreadGroup, "QueueWorkerThread" + pThreadNo);
      this.mOwningServiceQueueHandler = pServiceQueueHandler;
    }

    public void resetTrackTime(){
      mLastTrackTime = System.currentTimeMillis();
    }

    public void run() {

      // loop forever
      RUN_LOOP: while(true) {

        WorkItem lWorkItem = null;

        try {
          lWorkItem = checkOutWorkItemOrWait(this); // wait() happens in here when no work exists

          synchronized(lWorkItem) {

            // Pass the thread to the workItems execute method
            try {
              lWorkItem.execute();
            }
            catch (Throwable ex) {
              // Need to tidy up any workItems that have resulted in an error here so they don't execute again
              lWorkItem.finaliseOnError(ex);

              Track.recordSuppressedException("ErrorExecutingWorkItem", ex);
            }
            finally {
              try {
                checkInOrSurrenderWorkItem(lWorkItem); // Attempt returning to the queue if not complete and remove if in error
              }
              catch(Throwable ex) {
                Track.recordSuppressedException("ErrorCheckingInWorkItem", ex);
              }

            }
          }
          lWorkItem = null; //null out so work item can be GC'd - otherwise held in memory at checkOutWorkItemOrWait() line
          //Sleep so other Threads get a look-in on CPU
          Thread.sleep(mWorkerSleepTime);

        }
        catch (InterruptedException ex1) {
          FoxLogger.getLogger().trace("Exiting QueueWorker thread due to InterruptedException");
          break RUN_LOOP;
        }
        catch (Throwable ex2) {
          // Log any errors to the threads track
          Track.recordSuppressedException("ErrorWithQueueWorkerThread", ex2);
        }

      } // RUN_LOOP
    } // run ()
  } // class QueueWorker
}
