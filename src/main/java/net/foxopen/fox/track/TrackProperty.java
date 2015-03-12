package net.foxopen.fox.track;

import net.foxopen.fox.XFUtil;

/**
 * Named properties which can be recorded in a TrackLogger for later retrieval by an interested consumer.
 */
public enum TrackProperty {
  THREAD_ID("thread_id"),
  ACTION_TYPE("action_type"),
  ACTION_NAME("action_name"),
  RESPONSE_TYPE("response_type"),
  AUTHENTICATED_USER_ID("wua_id"),
  AUTHENTICATED_SESSION_ID("wus_id"),
  MODULE_START_NAME("module_start_name"),
  STATE_START_NAME("state_start_name"),
  MODULE_END_NAME("module_end_name"),
  STATE_END_NAME("state_end_name");

  private final String mColumnName;

  private TrackProperty(String pColumnName) {
    mColumnName = pColumnName;
  }

  String getTrackSubject() {
    return XFUtil.initCap(toString()).replace(" ", "");
  }

  String getColumnName() {
    return mColumnName;
  }
}
