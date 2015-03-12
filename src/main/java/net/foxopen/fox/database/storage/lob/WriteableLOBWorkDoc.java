package net.foxopen.fox.database.storage.lob;

import net.foxopen.fox.database.UCon;
import net.foxopen.fox.database.sql.ExecutableAPI;
import net.foxopen.fox.database.storage.WaitingRowSelector;
import net.foxopen.fox.ex.ExDB;
import net.foxopen.fox.ex.ExDBTimeout;
import net.foxopen.fox.ex.ExInternal;
import net.foxopen.fox.thread.storage.WorkingFileStorageLocation;

public class WriteableLOBWorkDoc<T>
extends LOBWorkDoc<T> {

  private static final int LOB_WAIT_TIMEOUT_SECS = 7;
  private static final WaitingRowSelector gRowSelector = new WaitingRowSelector(LOB_WAIT_TIMEOUT_SECS);

  public WriteableLOBWorkDoc(Class<T> pLOBClass, WorkingFileStorageLocation<T> pWorkingStorageLocation) {
    super(pLOBClass, pWorkingStorageLocation);
  }

  @Override
  protected void openInternal(UCon pUCon) {

    //Attempt to select the row
    boolean lRowExists = selectAndLockRow(pUCon);
    if(!lRowExists) {
      insertRow(pUCon);
    }

    //At this point we should have a LOB accessor determined by the select statement
    getLOBAccessor().prepareLOBForWrite();
  }

  private boolean selectAndLockRow(UCon pUCon) {

    WaitingRowSelector.SelectAction lSelectAction = new WaitingRowSelector.SelectAction() {
      public boolean attemptSelect(UCon pUCon) throws ExDBTimeout {
        return selectRow(pUCon);
      }
    };

    return gRowSelector.selectRow(pUCon, lSelectAction, getWorkingStorageLocation().getStorageLocationName());
  }

  private void insertRow(UCon pUCon) {
    ExecutableAPI lInsertStatement = getWorkingStorageLocation().getExecutableInsertStatementOrNull(this);
    if(lInsertStatement == null) {
      throw new ExInternal("No row selected and insert statement not defined for " + getWorkingStorageLocation().getStorageLocationName());
    }

    try {
      lInsertStatement.executeAndClose(pUCon);
    }
    catch (ExDB e) {
      throw new ExInternal("Failed to run insert statement for storage location " + getWorkingStorageLocation().getStorageLocationName(), e);
    }

    //Try to select the row we just inserted
    try {
      boolean lRowExists = selectRow(pUCon);
      if(!lRowExists) {
        throw new ExInternal("Insert/select statement mistmatch: row inserted could not be selected by corresponding statement for " + getWorkingStorageLocation().getStorageLocationName());
      }
    }
    catch (ExDB e) {
      throw new ExInternal("Failed to select row after insert statement for storage location " + getWorkingStorageLocation().getStorageLocationName(), e);
    }
  }

  @Override
  protected void closeInternal(UCon pUCon) {
    //Run update statement if defined
    ExecutableAPI lUpdateStatement = getWorkingStorageLocation().getExecutableUpdateStatementOrNull(this);
    if(lUpdateStatement != null) {
      try {
        lUpdateStatement.executeAndClose(pUCon);
      }
      catch (ExDB e) {
        throw new ExInternal("Failed to run insert statement for storage location " +  getWorkingStorageLocation().getStorageLocationName(), e);
      }
    }
  }
}
