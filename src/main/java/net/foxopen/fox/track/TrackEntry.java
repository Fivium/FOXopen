package net.foxopen.fox.track;

import java.util.List;

public interface TrackEntry {
  String getSubject();

  String getInfo();

  Track.SeverityLevel getSeverity();

  long getInTime();

  TrackEntry getParent();

  void setOutTime(long pOutTime);

  long getOutTime();

  TrackEntryType getType();

  void setType(TrackEntryType pType);

  void addChildEntry(TrackEntry pChild);

  List<TrackEntry> getChildEntryList();

  boolean isVisible();

  boolean hasFlag(TrackFlag pFlag);
}
