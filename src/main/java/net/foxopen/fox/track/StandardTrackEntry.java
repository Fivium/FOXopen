package net.foxopen.fox.track;

import java.util.ArrayList;
import java.util.List;

class StandardTrackEntry implements TrackEntry {

  private final String mSubject;
  private final String mInfo;
  private final Track.SeverityLevel mSeverity;
  private final long mInTime;
  private final TrackEntry mParent;
  private final TrackFlag[] mFlags;
  private long mOutTime;
  private final List<TrackEntry> mChildren = new ArrayList<>();
  private TrackEntryType mType = TrackEntryType.DEFAULT;

  StandardTrackEntry(String pSubject, String pInfo, Track.SeverityLevel pSeverity, TrackEntry pParent, TrackFlag[] pFlags) {
    mSubject = pSubject;
    mInfo = pInfo;
    mSeverity = pSeverity;
    mInTime = System.currentTimeMillis();
    mParent = pParent;
    mOutTime = mInTime;
    mFlags = pFlags;
  }

  public String getSubject() {
    return mSubject;
  }

  public String getInfo() {
    return mInfo;
  }

  public Track.SeverityLevel getSeverity() {
    return mSeverity;
  }

  public long getInTime() {
    return mInTime;
  }

  public TrackEntry getParent() {
    return mParent;
  }

  public void setOutTime(long pOutTime) {
    mOutTime = pOutTime;
  }

  public long getOutTime() {
    return mOutTime;
  }

  @Override
  public void setType(TrackEntryType pType) {
    mType = pType;
  }

  @Override
  public TrackEntryType getType() {
    return mType;
  }

  public void addChildEntry(TrackEntry pChild) {
    mChildren.add(pChild);
  }

  public List<TrackEntry> getChildEntryList() {
    return mChildren;
  }

  @Override
  public boolean isVisible() {
    return true;
  }
}
