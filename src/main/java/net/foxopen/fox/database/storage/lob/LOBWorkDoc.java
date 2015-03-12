package net.foxopen.fox.database.storage.lob;

import net.foxopen.fox.database.UCon;
import net.foxopen.fox.database.sql.ExecutableQuery;
import net.foxopen.fox.database.sql.SingleRowResultDeliverer;
import net.foxopen.fox.database.sql.bind.BindSQLType;
import net.foxopen.fox.database.storage.WorkDoc;
import net.foxopen.fox.ex.ExDB;
import net.foxopen.fox.ex.ExDBTimeout;
import net.foxopen.fox.ex.ExDBTooFew;
import net.foxopen.fox.ex.ExDBTooMany;
import net.foxopen.fox.ex.ExInternal;
import net.foxopen.fox.thread.storage.WorkingFileStorageLocation;
import net.foxopen.fox.track.Track;

import java.util.Map;


/**
 * WorkDoc for accessing a LOB defined in a WorkingFileStorageLocation. These should be constructed from a WFSL.
 * @param <T> Type of LOB being accessed (e.g. BLOB, CLOB, etc). The special value {@link AnyLOBType} can be used by a
 * consumer which is willing to retrieve any LOB regardless of type (in this case, only the LOB's InputStream will be acces
 */
public abstract class LOBWorkDoc<T>
implements WorkDoc {

  private final Class<T> mLOBClass;
  private final WorkingFileStorageLocation<T> mWorkingStorageLocation;

  private boolean mOpen = false;
  private LOBAccessor<T> mLOBAccessor;

  protected LOBWorkDoc(Class<T> pLOBClass, WorkingFileStorageLocation<T> pWorkingStorageLocation) {
    mLOBClass = pLOBClass;
    mWorkingStorageLocation = pWorkingStorageLocation;
  }

  @Override
  public T getLOBForBinding(UCon pUCon, BindSQLType pBindTypeRequired) {
    return getLOB();
  }

  public void open(UCon pUCon) {
    if(!mOpen) {
      openInternal(pUCon);
      mOpen = true;
    }
    else {
      Track.alert("Tried to open a LOBWorkDoc which is already open");
    }
  }

  protected abstract void openInternal(UCon pUCon);

  /**
   * Selects exactly one LOB (of the target type) from exactly one query row.
   * @param pUCon
   * @return True if a valid row was selected, false otherwise.
   * @throws ExDBTimeout
   */
  protected boolean selectRow(UCon pUCon)
  throws ExDBTimeout {

    ExecutableQuery lSelectQuery = mWorkingStorageLocation.getExecutableSelectStatement();
    try {
      SingleRowResultDeliverer lDeliverer = new SingleRowResultDeliverer();
      lSelectQuery.executeAndDeliver(pUCon, lDeliverer);

      Map<String, Object> lColumnMap = lDeliverer.getColumnMap();
      LOBAccessor<T> lLOBAccessor = null;
      int lLOBsFound = 0;
      COL_LOOP:
      for(Map.Entry<String, Object> lColumn : lColumnMap.entrySet()) {
        Object lColObject = lColumn.getValue();

        //If we don't know what LOB type we're looking for, try and get an accessor for every object, otherwise only
        //attempt to get an accessor for columns of the desired type.
        if(lColObject != null && (mLOBClass == AnyLOBType.class || mLOBClass.isInstance(lColObject))) {
          LOBAccessor<T> lLOBAccessorOrNull = LOBAccessor.getAccessorForLOBOrNull(lColObject, mLOBClass);
          if(lLOBAccessorOrNull != null) {
            lLOBsFound++;
            lLOBAccessor = lLOBAccessorOrNull;
          }
        }
      }

      //Looped all columns without finding exactly one compatible LOB - this is an error
      if(lLOBsFound != 1) {
        throw new ExInternal("Select query for " + mWorkingStorageLocation.getStorageLocationName() + " failed to locate exactly one non-null " +
                             mLOBClass.getSimpleName() + " column (got " + lLOBsFound + ")");
      }
      else {
        mLOBAccessor = lLOBAccessor;
      }

      return true;
    }
    catch (ExDBTooFew e) {
      return false;
    }
    catch (ExDBTooMany e) {
      throw new ExInternal("Select query for " + mWorkingStorageLocation.getStorageLocationName() + " returned more than 1 row", e);
    }
    catch (ExDBTimeout e) {
      throw e;
    }
    catch (ExDB e) {
      throw new ExInternal("Error running select query for " + mWorkingStorageLocation.getStorageLocationName(), e);
    }
  }

  public T getLOB() {
    if(mLOBAccessor == null) {
      return null;
    }
    else {
      return mLOBAccessor.getLOB();
    }
  }

  public void close(UCon pUCon) {
    if(mOpen) {
      closeInternal(pUCon);

      if(mLOBAccessor != null) {
        mLOBAccessor.closeLOB();
        mLOBAccessor = null;
      }
      mOpen = false;
    }
    else {
      Track.alert("Tried to close a LOBWorkDoc which is not open");
    }
  }

  protected abstract void closeInternal(UCon pUCon);

  public void closeOnError() {
    if(mLOBAccessor != null) {
      mLOBAccessor.closeLOB();
    }
  }

  protected WorkingFileStorageLocation<T> getWorkingStorageLocation() {
    return mWorkingStorageLocation;
  }

  protected LOBAccessor<T> getLOBAccessor() {
    return mLOBAccessor;
  }
}
