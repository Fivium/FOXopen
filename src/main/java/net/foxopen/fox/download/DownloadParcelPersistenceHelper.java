package net.foxopen.fox.download;

import java.util.Collection;
import java.util.Collections;

import net.foxopen.fox.thread.persistence.Persistable;
import net.foxopen.fox.thread.persistence.PersistableType;
import net.foxopen.fox.thread.persistence.PersistenceContext;
import net.foxopen.fox.thread.persistence.PersistenceMethod;
import net.foxopen.fox.thread.persistence.PersistenceResult;


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
}
