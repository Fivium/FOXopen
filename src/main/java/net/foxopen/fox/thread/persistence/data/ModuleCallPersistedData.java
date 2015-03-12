package net.foxopen.fox.thread.persistence.data;

import java.util.List;
import java.util.Map;

import net.foxopen.fox.auth.SecurityScope;
import net.foxopen.fox.thread.stack.callback.CallbackHandler;
import net.foxopen.fox.thread.storage.WorkingDataDOMStorageLocation;


public class ModuleCallPersistedData
implements PersistedData {

  private final String mCallId;

  private final String mAppMnem;
  private final String mModuleName;
  private final String mEntryThemeName;

  private final int mStackPosition;

  private final Map<String, WorkingDataDOMStorageLocation> mLabelToStorageLocationMap;
  private final List<CallbackHandler> mCallbackHandlerList;
  private final SecurityScope mSecurityScope;


  public ModuleCallPersistedData(String pCallId, int pStackPosition, String pAppMnem, String pModuleName, String pEntryThemeName,
                                 Map<String, WorkingDataDOMStorageLocation> pLabelToStorageLocationMap, List<CallbackHandler> pCallbackHandlerList, SecurityScope pSecurityScope) {
    mStackPosition = pStackPosition;
    mCallId = pCallId;
    mAppMnem = pAppMnem;
    mModuleName = pModuleName;
    mEntryThemeName = pEntryThemeName;
    mLabelToStorageLocationMap = pLabelToStorageLocationMap;
    mCallbackHandlerList = pCallbackHandlerList;
    mSecurityScope = pSecurityScope;
  }

  public String getCallId() {
    return mCallId;
  }

  public String getAppMnem() {
    return mAppMnem;
  }

  public String getModuleName() {
    return mModuleName;
  }

  public String getEntryThemeName() {
    return mEntryThemeName;
  }

  public int getStackPosition() {
    return mStackPosition;
  }

  public List<CallbackHandler> getCallbackHandlerList() {
    return mCallbackHandlerList;
  }

  public SecurityScope getSecurityScope() {
    return mSecurityScope;
  }

  public Map<String, WorkingDataDOMStorageLocation> getLabelToStorageLocationMap() {
    return mLabelToStorageLocationMap;
  }
}
