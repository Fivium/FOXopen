package net.foxopen.fox.module;

import java.util.HashMap;
import java.util.Map;

import net.foxopen.fox.ex.ExModule;


/**
 * Options to determine how and when storage location XML is serialised to the database.
 */
public enum SyncMode {
  SYNCHRONISED("synchronised"),
  UNSYNCHRONISED("unsynchronised"),
  READ_ONLY("read-only");
  
  private static final Map<String, SyncMode> gExternalStringToSyncMode = new HashMap<>(3);
  
  static {
    for(SyncMode lMode : values()) {
      gExternalStringToSyncMode.put(lMode.mExternalString, lMode);
    }
  }
  
  public static SyncMode fromExternalString(String pExternalString)
  throws ExModule {
    
    SyncMode lSyncMode = gExternalStringToSyncMode.get(pExternalString);

    if(lSyncMode == null) {
      throw new ExModule("Unrecognised value for synchronisation-mode " + pExternalString);
    }
    
    return lSyncMode;
  }
  
  private final String mExternalString;
          
  private SyncMode(String pExternalString) {
    mExternalString = pExternalString;      
  }
}