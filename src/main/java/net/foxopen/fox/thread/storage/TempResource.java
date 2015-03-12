package net.foxopen.fox.thread.storage;


/**
 * Special type of WSL which provides access to a temporary resource. Temporary resources are only expected to have short
 * life spans (i.e. minutes or hours) so should only be used to hold short term data, i.e. for a download preview. However
 * they are access in the same way as a user-defined storage location for interface consistency.
 * @param <T> LOB type to be returned by this any WorkDoc created from this TempResource. Typically Blob or Clob.
 */
public class TempResource<T>
extends WorkingFileStorageLocation<T> {

  private final String mTempResourceId;
  private final TempResourceProvider mTempResourceProvider;

  public TempResource(Class<T> pLOBClass, String pTempResourceId, TempResourceProvider pTempResourceProvider) {
    super(pLOBClass, pTempResourceProvider.getTempStorageLocationForLOBType(pLOBClass), pTempResourceId, false);
    mTempResourceId = pTempResourceId;
    mTempResourceProvider = pTempResourceProvider;
  }

  /**
   * Gets the storage location definition used to create this TempResource. Overrid of existing method to deal with the
   * StorageLocation not being defined an an app/mod tuple as it usually is.
   * @return
   */
  @Override
  public FileStorageLocation getStorageLocation() {
    return mTempResourceProvider.getTempStorageLocationForLOBType(mLOBClass);
  }

  public String getTempResourceId() {
    return mTempResourceId;
  }
}
