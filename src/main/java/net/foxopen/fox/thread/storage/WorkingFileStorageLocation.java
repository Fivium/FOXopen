package net.foxopen.fox.thread.storage;

import net.foxopen.fox.ContextUElem;
import net.foxopen.fox.database.storage.lob.LOBWorkDoc;
import net.foxopen.fox.database.storage.lob.ReadOnlyLOBWorkDoc;
import net.foxopen.fox.database.storage.lob.WriteableLOBWorkDoc;
import net.foxopen.fox.ex.ExInternal;
import net.foxopen.fox.module.Mod;

/**
 * WSL for accessing an arbitrary file (not XML) which is stored as a LOB on the database. This could be an upload target,
 * a generation destination, or a download target.
 * @param <T> LOB type (Blob or Clob) which this WFSL will be used to retrieve. Leave as Object if not known at evaluate time.
 */
public class WorkingFileStorageLocation<T>
extends WorkingStorageLocation<FileStorageLocation>  {

  protected final Class<T> mLOBClass;

  public WorkingFileStorageLocation(Class<T> pLOBClass, FileStorageLocation pStorageLocation, ContextUElem pContextUElem, boolean pReadOnly) {
    super(pStorageLocation, pContextUElem, null, true, pReadOnly);
    mLOBClass = pLOBClass;
  }

  protected WorkingFileStorageLocation(Class<T> pLOBClass, FileStorageLocation pStorageLocation, String pUniqueValue, boolean pReadOnly) {
    super(pStorageLocation, new ContextUElem(), pUniqueValue, true, pReadOnly);
    mLOBClass = pLOBClass;
  }

  //Overloaded to provide a FILE StorageLocation
  @Override
  public FileStorageLocation getStorageLocation() {
    if(mAppMnem != null && mModName != null){
      return Mod.getModuleForAppMnemAndModuleName(mAppMnem, mModName).getFileStorageLocation(mStorageLocationName);
    }
    else {
      return null;
    }
  }

  /**
   * Creates a new WorkDoc for this WSL. This must be opened, manipulated and closed using the standard LOBWorkDoc API.
   * @param pReadOnly If true, the WorkDoc will be read only. If false it will be writable. Note that if this WSL is
   * not writable, it will not be able to create a writable WorkDoc. Note this is specified separately to the writablity
   * of the WSL because you may want to create a read-only "view" of a writable WSL (this should be preferred unless writing
   * is definitely needed).
   * @return New LOBWorkDoc.
   */
  public LOBWorkDoc<T> createWorkDoc(boolean pReadOnly) {
    if(pReadOnly) {
      return new ReadOnlyLOBWorkDoc<>(mLOBClass, this);
    }
    else {
      if(isReadOnly()) {
        throw new ExInternal("Cannot create a Writable WorkDoc for a read only WorkingFileStorageLocation");
      }
      return new WriteableLOBWorkDoc<>(mLOBClass, this);
    }
  }
}
