package net.foxopen.fox.download;

import net.foxopen.fox.thread.persistence.Persistable;
import net.foxopen.fox.thread.persistence.PersistableType;
import net.foxopen.fox.thread.persistence.PersistenceContext;
import net.foxopen.fox.thread.persistence.PersistenceMethod;
import net.foxopen.fox.thread.persistence.PersistenceResult;

import java.util.Collection;
import java.util.Collections;


/**
 * Persistable wrapper for DownloadParcels to save DownloadParcel implementations from having to duplicate this code.
 */
public class DownloadParcelPersistenceHelper
implements Persistable {

  private final DownloadParcel mDownloadParcel;

  public DownloadParcelPersistenceHelper(DownloadParcel pDownloadParcel) {
    mDownloadParcel = pDownloadParcel;
  }

  @Override
  public Collection<PersistenceResult> create(PersistenceContext pPersistenceContext) {
    //Important: only allow the serialisation of parcels which haven't yet been executed (otherwise we might accidentally serialise the statement result)
    if(mDownloadParcel.isSerialiseAllowed()) {
      pPersistenceContext.getSerialiser().createDownloadParcel(mDownloadParcel);
      return Collections.singleton(new PersistenceResult(this, PersistenceMethod.CREATE));
    }
    else {
      return Collections.emptySet();
    }
  }

  @Override
  public Collection<PersistenceResult> update(PersistenceContext pPersistenceContext) {
    return Collections.emptySet();
  }

  @Override
  public Collection<PersistenceResult> delete(PersistenceContext pPersistenceContext) {
    return Collections.emptySet();
  }

  @Override
  public PersistableType getPersistableType() {
    return PersistableType.DOWNLOAD_PARCEL;
  }


  /*
   * Override hashCode and equals to allow comparison of DownloadParcelPersistenceHelper objects based on the download parcel id
   * Used in DownloadParcelPersistenceHelper.endPersistenceCycle() to check if this object has already been serialised - see FOXRD-880
   */
  @Override
  public int hashCode() {
    return getParcelId().hashCode();
  }

  @Override
  public boolean equals(Object comparisonObject) {
    if (!(comparisonObject instanceof DownloadParcelPersistenceHelper)) {
      return false;
    }
    else {
      return getParcelId().equals(((DownloadParcelPersistenceHelper) comparisonObject).getParcelId());
    }
  }

  protected String getParcelId() {
    return mDownloadParcel.getParcelId();
  }

}
