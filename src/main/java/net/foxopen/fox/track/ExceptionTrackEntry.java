package net.foxopen.fox.track;

import java.util.Collections;
import java.util.List;

import net.foxopen.fox.XFUtil;
import net.foxopen.fox.ex.ExInternal;

public class ExceptionTrackEntry
implements TrackEntry {

  private final TrackEntry mParent;
  private final long mCreatedTime;
  private final String mErrorId;

  /** This will only be populated if this Entry should be serialised. This is determined by the TrackLogger but will typically
   * because this exception was implicated in a hard failure. We don't store a reference to every Exception to avoid using too much memory. */
  private Throwable mSerialiseException;

  public ExceptionTrackEntry(TrackEntry pParent, String pErrorId) {
    mParent = pParent;
    mCreatedTime = System.currentTimeMillis();
    mErrorId = pErrorId;
  }

  @Override
  public String getSubject() {
    return "Exception";
  }

  @Override
  public String getInfo() {
    return mSerialiseException.getMessage() + "\n\n" + XFUtil.getJavaStackTraceInfo(mSerialiseException);
  }

  @Override
  public Track.SeverityLevel getSeverity() {
    return Track.SeverityLevel.ALERT;
  }

  @Override
  public long getInTime() {
    return mCreatedTime;
  }

  @Override
  public TrackEntry getParent() {
    return mParent;
  }

  @Override
  public void setOutTime(long pOutTime) {
  }

  @Override
  public long getOutTime() {
    return mCreatedTime;
  }

  @Override
  public TrackEntryType getType() {
    return TrackEntryType.TEXT;
  }

  @Override
  public void setType(TrackEntryType pType) {
    throw new ExInternal("Cannot set type of an exception");
  }

  @Override
  public void addChildEntry(TrackEntry pChild) {
  }

  @Override
  public List<TrackEntry> getChildEntryList() {
    return Collections.emptyList();
  }

  @Override
  public boolean isVisible() {
    return mSerialiseException != null;
  }

  public void setSerialiseException(Throwable pSerialiseException) {
    mSerialiseException = pSerialiseException;
  }
}
