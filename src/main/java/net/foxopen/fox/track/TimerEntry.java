package net.foxopen.fox.track;


class TimerEntry {
  final long mStartTime = System.currentTimeMillis();
  long mEndTime = -1;
  int mUsageCount = 1;
}
