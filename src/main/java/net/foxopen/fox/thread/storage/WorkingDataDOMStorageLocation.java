package net.foxopen.fox.thread.storage;

import net.foxopen.fox.ContextUElem;
import net.foxopen.fox.ex.ExInternal;
import net.foxopen.fox.ex.ExModule;
import net.foxopen.fox.module.Mod;
import net.foxopen.fox.module.SyncMode;
import net.foxopen.fox.track.Track;

public class WorkingDataDOMStorageLocation
extends WorkingStorageLocation<DataDOMStorageLocation> {

  private final SyncMode mSyncMode;

  /**
   * Evaluates the given StorageLocation and creates a new WorkingStorageLocation based on it.
   * @param pStorageLocation StorageLocation to evaluate.
   * @param pContextUElem Context for XPath evaluation.
   * @param pUniqueValue Value for "UNIQUE" bind type evaulation.
   * @param pEvaluateBinds If true, statement binds are evaluated. If false nothing is evaluated and this will be WSL
   * which cannot run any statements. It can still be used to determine if the WSL is temporary or read only.
   * @param pSyncMode SyncMode for the DOM retrieved by this WSL, if any.
   */
  public WorkingDataDOMStorageLocation(StorageLocation pStorageLocation, ContextUElem pContextUElem, String pUniqueValue, boolean pEvaluateBinds, SyncMode pSyncMode) {
    super(pStorageLocation, pContextUElem, pUniqueValue, pEvaluateBinds, pSyncMode != SyncMode.SYNCHRONISED);
    Track.debug("StorageLocationSyncMode", pSyncMode.toString());
    mSyncMode = pSyncMode;
  }

  @Override
  public DataDOMStorageLocation getStorageLocation(){
    try {
      if(mAppMnem != null && mModName != null){
        return Mod.getModuleForAppMnemAndModuleName(mAppMnem, mModName).getDataStorageLocation(mStorageLocationName);
      }
      else {
        return null;
      }
    }
    catch (ExModule e) {
      throw new ExInternal("Error getting store location for working store location", e);
    }
  }

  public SyncMode getSyncMode() {
    return mSyncMode;
  }

}
