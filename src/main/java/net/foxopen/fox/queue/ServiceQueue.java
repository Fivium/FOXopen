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

import net.foxopen.fox.ex.ExInternal;

import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Set;


public abstract class ServiceQueue {

  private final LinkedList mPendingWorkQueue = new LinkedList();
  private final Set mProcessingWorkQueue = new HashSet();
  private int mCurrentThreadCount = 0;
  private int mMaxServicingThreads = 7; // default to one thread per queue - Constructor of implementation should set this
  private String mName;
  private boolean mDestroyFlag = false;
  private final ServiceQueueHandler mOwningQueueHandler;

  public abstract boolean addNewWorkItem(WorkItem pWorkItem, String pWorkItemType);

  public ServiceQueue(String pName, ServiceQueueHandler pOwningHandler) {
    mName = pName;
    mOwningQueueHandler = pOwningHandler;
  }

  public final synchronized WorkItem checkOut() {

    WorkItem lWorkItem = null;

    if (mCurrentThreadCount >= mMaxServicingThreads) {
      return null;
    }

    Iterator lPendingWorkIterator = mPendingWorkQueue.iterator();
    while(lPendingWorkIterator.hasNext()) {
      // get reference to next work item
      lWorkItem = (WorkItem)lPendingWorkIterator.next();

      // check to see if the work item is an instance of the requested class
      if(testCanRun(lWorkItem)) {
        lPendingWorkIterator.remove();
        mProcessingWorkQueue.add(lWorkItem);
        mCurrentThreadCount++;
        onCheckOut(lWorkItem);
        break; // break out from loop when work found of correct class type
      }
      else {
        lWorkItem = null;
      }
    }

    return lWorkItem;
  }

  /**
   * This method should be used in check in implementations.  It returns a work item to the
   * pending list of this queue given that the work item has not completed.  If the work item
   * has completed then don't add it back to the pending list and pass control to original thread.
   * Finally the queues thread count is then decremented by 1.
   *
   * @param pWorkItem - The work item which you wish to return to this queue
   */
  public final synchronized void checkIn(WorkItem pWorkItem) {
    // ensure that the processing queue contains the work item

    if (!mProcessingWorkQueue.remove(pWorkItem)) {
      throw new ExInternal("Tryed to check work item into queue \"" + mName + "\" but failed to match work item to known work in processing array.");
    }

    mCurrentThreadCount--; // decrease the thread count
    onCheckIn(pWorkItem);

    // If work is not complete and not failed then add back into queue
    if (!pWorkItem.isComplete() && !pWorkItem.isFailed()) {
      // add work item to the end of queue
      mPendingWorkQueue.addLast(pWorkItem);
    }
    else {
      //If queue was marked for destroy and its last WorkItem has finished, remove the Queue From the QueueHandler's list
      if(mDestroyFlag && mPendingWorkQueue.size() == 0 && mProcessingWorkQueue.size() == 0)
        mOwningQueueHandler.removeServiceQueue(this);

      pWorkItem.wakeUpCaller(); // Pass control back to original thread
    }

  }

  protected final synchronized void addWorkItemToPendingQueue (WorkItem pWorkItem) {
    mPendingWorkQueue.addLast(pWorkItem);
    // give the work item a reference back to this service queue
    pWorkItem.setOwningServiceQueue(this);
  }

  public final String getName () {
    return mName;
  }

  protected final synchronized int getPendingWorkQueueSize () {
    return mPendingWorkQueue.size();
  }

  protected final synchronized int getMaxTreadCount () {
    return mMaxServicingThreads;
  }

  protected final synchronized void setMaxThreadCount (int pMaxThreadCount) {
    mMaxServicingThreads = pMaxThreadCount;
  }

  /**
   *
   * @return true if remove was immediate, false if still pending
   */
  protected synchronized boolean destroy(){
    mDestroyFlag = true;
    //Might be able to destroy now if not working
    if(mPendingWorkQueue.size() == 0 && mProcessingWorkQueue.size() == 0){
      mOwningQueueHandler.removeServiceQueue(this);
      return true;
    } else {
      return false;
    }
  }

  // Override to provide a method of checking to see whether work items should be processed
  protected abstract boolean testCanRun(WorkItem pWorkItem);

  // Override for additional activity on check out
  protected void onCheckOut(WorkItem pWorkItem) {
  }

  // Override for additional activity on check in
  protected void onCheckIn(WorkItem pWorkItem) {
  }

}
