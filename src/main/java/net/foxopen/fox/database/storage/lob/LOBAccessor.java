package net.foxopen.fox.database.storage.lob;

import oracle.sql.BLOB;

import java.sql.Clob;


/**
 * Access strategy for a LOB selected by a LOBWorkDoc.
 * @param <T>
 */
public abstract class LOBAccessor<T extends Object> {

  public static <T> LOBAccessor<T> getAccessorForLOBOrNull(Object pLOB, Class<T> pLOBClass) {
    if(pLOB instanceof BLOB) {
      return (LOBAccessor<T>) new OracleBlobAccessor((BLOB) pLOB);
    }
    else if(pLOB instanceof Clob) {
      return (LOBAccessor<T>) new ClobAccessor((Clob) pLOB);
    }
    else {
      return null;
    }
  }

  public abstract T getLOB();

  /**
   * Performs any validation or setup on the LOB prior to a write operation.
   */
  public abstract void prepareLOBForWrite();

  /**
   * Closes the LOB locator on the database.
   */
  public abstract void closeLOB();
}
