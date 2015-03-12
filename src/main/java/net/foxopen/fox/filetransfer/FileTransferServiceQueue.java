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
package net.foxopen.fox.filetransfer;

import java.util.HashMap;
import java.util.Map;

import net.foxopen.fox.App;
import net.foxopen.fox.XFUtil;
import net.foxopen.fox.ex.ExInternal;
import net.foxopen.fox.queue.ServiceQueue;
import net.foxopen.fox.queue.ServiceQueueHandler;
import net.foxopen.fox.queue.WorkItem;

public class FileTransferServiceQueue extends ServiceQueue {

  public static final Integer MAX_UPLOAD_DEFAULT = new Integer(1);
  public static final Integer MAX_DOWNLOAD_DEFAULT = new Integer(1);

  private final String mApp;
  private final ServiceQueueHandler mServiceQueueHandler;
  private int mMinByteThreshold;
  private int mMaxByteThreshold;

  private static String[] mWorkItemTypes = {ServiceQueueHandler.UPLOAD_WORKITEM_TYPE};
  private final Map mMaxThreadCounts = new HashMap();
  private final Map mCurrentThreadCounts = new HashMap();

  public FileTransferServiceQueue(String pName, App pApp, String pMaxUploadThreadCount, String pMaxDownloadThreadCount, String pMinByteThreshold, String pMaxByteThreshold, ServiceQueueHandler pServiceQueueHandler) {
    super(pName, pServiceQueueHandler);
    mApp = pApp.getMnemonicName();
    mServiceQueueHandler = pServiceQueueHandler;

    // Assign max values into max work item values map
    mMaxThreadCounts.put(mWorkItemTypes[0], XFUtil.isNull(pMaxUploadThreadCount) ? MAX_UPLOAD_DEFAULT : Integer.valueOf(pMaxUploadThreadCount)); // Uploads

    // Assign current values into max work item values map (Starting with 0)
    mCurrentThreadCounts.put(mWorkItemTypes[0], new Integer(0)); // Uploads

    mMinByteThreshold = XFUtil.isNull(pMinByteThreshold) ? 0 : Integer.valueOf(pMinByteThreshold).intValue(); // default to size 0
    mMaxByteThreshold = XFUtil.isNull(pMaxByteThreshold) ? 999999999 : Integer.valueOf(pMaxByteThreshold).intValue(); // default to high value
  }

  public synchronized boolean addNewWorkItem(WorkItem pWorkItem, String pWorkItemType) {

    // This Queue will only accept upload work items from the queues owning app
    if (!(pWorkItemType.equals(ServiceQueueHandler.UPLOAD_WORKITEM_TYPE)) || !(mApp.equals(((App)pWorkItem.getAttribute("OriginatingApp")).getMnemonicName()))) {
      return false;
    }

    // Cast the work item to upload flavour
    UploadWorkItem lWorkItem = (UploadWorkItem) pWorkItem;

    // For now sniff the content length of the request (difficult to identify the file size due to stream)
    int lRequestContentLength = ((Integer)lWorkItem.getAttribute("MultipartContentLength")).intValue();

    // If the request size is greater than or equal to min threshold and less than the max then it can be added to this queue
    if (lRequestContentLength >= mMinByteThreshold && lRequestContentLength < mMaxByteThreshold) {
      this.addWorkItemToPendingQueue(lWorkItem);
      return true;
    }

    return false;
  }

  protected synchronized boolean testCanRun(WorkItem pWorkItem) {
    // If we know about workItem type and the thread counts configured for this work item type are exceptable then return true
     return mCurrentThreadCounts.containsKey( pWorkItem.getWorkItemType())
      && (((Integer)mCurrentThreadCounts.get( pWorkItem.getWorkItemType())).intValue() < ((Integer)mMaxThreadCounts.get(pWorkItem.getWorkItemType())).intValue())
      && ((Integer)mServiceQueueHandler.getHandlerAttribute(pWorkItem.getWorkItemType()+"-CURRENT")).intValue() < ((Integer)mServiceQueueHandler.getHandlerAttribute(pWorkItem.getWorkItemType()+"-MAX")).intValue();
  }

  protected void onCheckOut(WorkItem pWorkItem) {
    mCurrentThreadCounts.put(pWorkItem.getWorkItemType(), new Integer(((Integer)mCurrentThreadCounts.get(pWorkItem.getWorkItemType())).intValue() + 1));
  }

  protected void onCheckIn(WorkItem pWorkItem) {

    if (!mCurrentThreadCounts.containsKey(pWorkItem.getWorkItemType())) {
      throw new ExInternal("Failed to decrease thread count when returning work item implementation \"" + pWorkItem.getClass().getName() + "\" to queue \"" + getName() + "\".");
    }

    // If work is of right class type then decrement current thread count and break out of the loop
    mCurrentThreadCounts.put( pWorkItem.getWorkItemType(), new Integer(((Integer)mCurrentThreadCounts.get(pWorkItem.getWorkItemType())).intValue() - 1));
  }
}
