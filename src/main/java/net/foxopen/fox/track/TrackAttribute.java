package net.foxopen.fox.track;

import java.util.Collections;
import java.util.List;

import net.foxopen.fox.ex.ExInternal;

class TrackAttribute implements TrackEntry {

  private final String mAttributeName;
  private final String mAttributeValue;

  TrackAttribute(String pAttributeName, String pAttributeValue) {
    mAttributeName = pAttributeName;
    mAttributeValue = pAttributeValue;
  }

  @Override
  public String getSubject() {
    return mAttributeName;
  }

  @Override
  public String getInfo() {
    return mAttributeValue;
  }

  @Override
  public Track.SeverityLevel getSeverity() {
    return null;
  }

  @Override
  public long getInTime() {
    return 0L;
  }

  @Override
  public TrackEntry getParent() {
    return null;
  }

  @Override
  public void setOutTime(long pOutTime) {
  }

  @Override
  public long getOutTime() {
    return 0L;
  }

  @Override
  public void addChildEntry(TrackEntry pChild) {
    throw new ExInternal("Cannot add a child to an attribute");
  }

  @Override
  public List<TrackEntry> getChildEntryList() {
    return Collections.emptyList();
  }

  @Override
  public boolean isVisible() {
    return true;
  }

  @Override
  public TrackEntryType getType() {
    return TrackEntryType.ATTRIBUTE;
  }

  @Override
  public void setType(TrackEntryType pType) {
    throw new ExInternal("Cannot set type of an attribute");
  }
}
