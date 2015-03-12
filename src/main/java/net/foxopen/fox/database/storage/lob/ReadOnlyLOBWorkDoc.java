package net.foxopen.fox.database.storage.lob;

import net.foxopen.fox.database.UCon;
import net.foxopen.fox.ex.ExDBTimeout;
import net.foxopen.fox.ex.ExInternal;
import net.foxopen.fox.thread.storage.WorkingFileStorageLocation;

public class ReadOnlyLOBWorkDoc<T>
extends LOBWorkDoc<T> {

  public ReadOnlyLOBWorkDoc(Class<T> pLOBClass, WorkingFileStorageLocation<T> pWorkingStorageLocation) {
    super(pLOBClass, pWorkingStorageLocation);
  }

  @Override
  protected void openInternal(UCon pUCon) {
    try {
      boolean lRowExists = selectRow(pUCon);

      if(!lRowExists) {
        throw new ExInternal("Failed to select a single row for LOB read only storage location " + getWorkingStorageLocation().getStorageLocationName());
      }
    }
    catch (ExDBTimeout e) {
      throw new ExInternal("Unexpected timeout caught when selecting RO LOB; select statement should not attempt a lock", e);
    }
  }

  @Override
  protected void closeInternal(UCon pUCon) {
  }
}
