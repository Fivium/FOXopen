package net.foxopen.fox.queue;

import net.foxopen.fox.ex.ExInternal;

import java.util.HashMap;
import java.util.Map;

public abstract class WorkItem
{

  private final Map mAttributeMap = new HashMap();
  private final String mIdentifier;
  private ServiceQueue mOwningServiceQueue;
  protected final String mWorkItemType;

  public WorkItem(String pWorkItemType, String pIdentifier) {
    mWorkItemType = pWorkItemType;
    mIdentifier = pIdentifier;
  }

  public String getIdentifier(){
    return mIdentifier;
  }

  public abstract void execute() throws Throwable;

  public abstract boolean isComplete();

  public abstract boolean isFailed();

  public abstract void finaliseOnSuccess();

  public abstract void finaliseOnError(Throwable pEx);

  public final String getWorkItemType() {
    return mWorkItemType;
  }

  public final synchronized void setAttribute(String pKey, Object pData){
    mAttributeMap.put(pKey, pData);
  }

  public final synchronized Object getAttribute(String pKey){
    return mAttributeMap.get(pKey);
  }

  public final synchronized void wakeUpCaller() {
    // Only the original thread should be waiting on this objects monitor
    // hence safe to use notify
    this.notify();
  }

  public final synchronized void setOwningServiceQueue(ServiceQueue pServiceQueue) {
    // ensure that we only set the service queue once
    if (mOwningServiceQueue != null) {
      throw new ExInternal("Tried to set the work items owning service queue but the member variable already contained a reference to a service queue.");
    }

    // set the reference back to the owning service queue
    mOwningServiceQueue = pServiceQueue;
  }

  public final synchronized ServiceQueue getOwningServiceQueue() {
    return mOwningServiceQueue;
  }

}
