package net.foxopen.fox.thread.persistence.data;

import net.foxopen.fox.auth.SecurityScope;
import net.foxopen.fox.thread.stack.ModuleXPathVariableManager;
import net.foxopen.fox.thread.stack.callback.CallbackHandler;
import net.foxopen.fox.thread.storage.WorkingDataDOMStorageLocation;

import java.util.List;
import java.util.Map;


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
  private final ModuleXPathVariableManager mXPathVariableManager;


  public ModuleCallPersistedData(String pCallId, int pStackPosition, String pAppMnem, String pModuleName, String pEntryThemeName,
                                 Map<String, WorkingDataDOMStorageLocation> pLabelToStorageLocationMap, List<CallbackHandler> pCallbackHandlerList, SecurityScope pSecurityScope,
                                 ModuleXPathVariableManager pXPathVariableManager) {
    mStackPosition = pStackPosition;
    mCallId = pCallId;
    mAppMnem = pAppMnem;
    mModuleName = pModuleName;
    mEntryThemeName = pEntryThemeName;
    mLabelToStorageLocationMap = pLabelToStorageLocationMap;
    mCallbackHandlerList = pCallbackHandlerList;
    mSecurityScope = pSecurityScope;
    mXPathVariableManager = pXPathVariableManager;
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

  /** Might be null if no XPath variables have been serialised */
  public ModuleXPathVariableManager getXPathVariableManager() {
    return mXPathVariableManager;
  }
}
