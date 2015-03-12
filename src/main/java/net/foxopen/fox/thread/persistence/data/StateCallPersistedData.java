package net.foxopen.fox.thread.persistence.data;

import java.util.Collection;

import net.foxopen.fox.ContextUElem;


public class StateCallPersistedData
implements PersistedData {
  
  private final String mCallId;
  private final int mStackPosition;
  private final String mStateName;
  private final int mScrollPosition;
  private final Collection<ContextUElem.SerialisedLabel> mContextualLabels;
  
  public StateCallPersistedData(String pCallId, int pStackPosition, String pStateName, int pScrollPosition, Collection<ContextUElem.SerialisedLabel> pContextualLabels) {        
    mCallId = pCallId;
    mStackPosition = pStackPosition;
    mStateName = pStateName;
    mScrollPosition = pScrollPosition;
    mContextualLabels = pContextualLabels;
  }

  public String getCallId() {
    return mCallId;
  }

  public int getStackPosition() {
    return mStackPosition;
  }

  public String getStateName() {
    return mStateName;
  }

  public int getScrollPosition() {
    return mScrollPosition;
  }

  public Collection<ContextUElem.SerialisedLabel> getContextLabels() {
    return mContextualLabels;
  }
}
