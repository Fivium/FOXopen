package net.foxopen.fox.download;

import net.foxopen.fox.ContextUElem;
import net.foxopen.fox.XFUtil;
import net.foxopen.fox.database.sql.ExecutableQuery;
import net.foxopen.fox.dom.DOM;
import net.foxopen.fox.entrypoint.uri.RequestURIBuilder;
import net.foxopen.fox.ex.ExInternal;
import net.foxopen.fox.filetransfer.UploadInfo;
import net.foxopen.fox.module.UploadedFileInfo;
import net.foxopen.fox.thread.RequestContext;
import net.foxopen.fox.thread.StatefulXThread;
import net.foxopen.fox.thread.persistence.PersistenceMethod;
import net.foxopen.fox.thread.storage.FileStorageLocation;
import net.foxopen.fox.thread.storage.WorkingFileStorageLocation;
import net.foxopen.fox.track.Track;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;


/**
 * DownloadManager implementation for a StatefulXThread. This stores DownloadParcels on the database, keyed to the thread.
 */
public class ThreadDownloadManager
implements DownloadManager {

  private static final int PARCEL_CACHE_MAX_SIZE = 50;
  private static final Iterator<String> gUniqueIterator = XFUtil.getUniqueIterator();

  private final StatefulXThread mXThread;

  /** Cache of recently used download parcels. The limit must be imposed to avoid memory leaks.
   * This object can defer to the database to retrieve older parcels if needed. */
  private final Map<String, DownloadParcel> mDownloadParcels = new LinkedHashMap<String, DownloadParcel>() {
    protected boolean removeEldestEntry(Map.Entry<String, DownloadParcel> pEldest) {
      return size() > PARCEL_CACHE_MAX_SIZE;
    }
  };

  public ThreadDownloadManager(StatefulXThread pXThread) {
    mXThread = pXThread;
  }

  private String generateParcelId() {
    return gUniqueIterator.next();
  }

  @Override
  public String generateURL(RequestContext pRequestContext, DownloadParcel pDownloadParcel) {
    return DownloadServlet.buildParcelDownloadURI(pRequestContext.createURIBuilder(), mXThread.getThreadId(), pDownloadParcel.getParcelId(), pDownloadParcel.getFilename(), null);
  }

  @Override
  public String generateURL(RequestContext pRequestContext, DownloadParcel pDownloadParcel, DownloadMode pDownloadMode) {
    RequestURIBuilder lURIBuilder = pRequestContext.createURIBuilder();
    return DownloadServlet.buildParcelDownloadURI(lURIBuilder, mXThread.getThreadId(), pDownloadParcel.getParcelId(), pDownloadParcel.getFilename(), pDownloadMode);
  }

  public DownloadParcel getDownloadParcel(String pParcelId) {
    DownloadParcel lDownloadParcel = getDownloadParcelOrNull(pParcelId);
    if(lDownloadParcel == null) {
      throw new ExInternal("Cannot find parcel " + pParcelId);
    }
    return lDownloadParcel;
  }

  private DownloadParcel getDownloadParcelOrNull(String pParcelId) {
    Track.pushInfo("GetDownloadParcel", "Getting parcel " + pParcelId);
    try {
      DownloadParcel lDownloadParcel = mDownloadParcels.get(pParcelId);

      if(lDownloadParcel == null) {
        Track.info("Parcel not in cache; attempting database deserialise");

        lDownloadParcel = mXThread.getPersistenceContext().getDeserialiser().getDownloadParcel(pParcelId);
        if(lDownloadParcel != null) {
          //Re-add the deserialised parcel to local cache
          mDownloadParcels.put(pParcelId, lDownloadParcel);
        }
      }

      return lDownloadParcel;
    }
    finally {
      Track.pop("GetDownloadParcel");
    }
  }

  @Override
  public UploadedFileInfo addFileDownload(RequestContext pRequestContext, FileStorageLocation pFileStorageLocation, DOM pUploadTargetDOM, ContextUElem pContextUElem) {
    //For getting a link from a file widget
    WorkingFileStorageLocation lWorkingFSL = pFileStorageLocation.createWorkingStorageLocationForUploadDownload(pContextUElem, pUploadTargetDOM);
    UploadedFileInfo lUploadedFileInfo = UploadedFileInfo.createFromDOM(pUploadTargetDOM);
    getOrCreateFileDownloadParcel(pRequestContext, lWorkingFSL, lUploadedFileInfo);
    return lUploadedFileInfo;
  }

  @Override
  public UploadedFileInfo addFileDownload(RequestContext pRequestContext, WorkingFileStorageLocation pWFSL, String pUploadContainerDOMRef, UploadInfo pUploadInfo) {
    //For getting a link from a newly finished upload
    UploadedFileInfo lUploadedFileInfo = UploadedFileInfo.createFromUploadInfo(pUploadContainerDOMRef, pUploadInfo);
    getOrCreateFileDownloadParcel(pRequestContext, pWFSL, lUploadedFileInfo);
    return lUploadedFileInfo;
  }

  private DownloadParcel getOrCreateFileDownloadParcel(RequestContext pRequestContext, WorkingFileStorageLocation pWFSL, UploadedFileInfo pUploadedFileInfo) {
    //Work out the parcel id from the storage location cache key - if there's already a parcel, we don't want to generate another one
    //NOTE: this is depending on the cache key being unique enough to identify the same file download, if this is not true then it will not work properly.
    String lParcelId = UploadedFileDownloadParcel.generateParcelId(pWFSL, pUploadedFileInfo);
    DownloadParcel lParcel = getDownloadParcelOrNull(lParcelId);
    if(lParcel == null) {
      lParcel = UploadedFileDownloadParcel.createParcelFromUploadedFileInfo(pWFSL, pUploadedFileInfo);
      addDownloadParcel(lParcel);
    }

    //IMPORTANT: set the URL on the newly created uploaded file info
    pUploadedFileInfo.setDownloadURL(generateURL(pRequestContext, lParcel));

    return lParcel;
  }

  /**
   * Adds a new DownloadParcel to the map and marks it as requiring persitence.
   * @param pNewParcel
   */
  private void addDownloadParcel(DownloadParcel pNewParcel) {
    //Mark parcel as persistence required
    mXThread.getPersistenceContext().requiresPersisting(new DownloadParcelPersistenceHelper(pNewParcel), PersistenceMethod.CREATE);

    //Cache in local map for quick access when user hits download URL
    mDownloadParcels.put(pNewParcel.getParcelId(), pNewParcel);
  }

  @Override
  public DownloadParcel addDownload(WorkingFileStorageLocation pWFSL, String pFilename, String pContentType) {
    //Creates a new parcel for an ad-hoc WFSL
    WFSLDownloadParcel lNewParcel = new WFSLDownloadParcel(generateParcelId(), pWFSL, pFilename, pContentType);
    addDownloadParcel(lNewParcel);

    return lNewParcel;
  }

  @Override
  public DownloadParcel addQueryDownload(ExecutableQuery pExecutableQuery, String pFilename) {
    QueryDownloadParcel lNewParcel = new QueryDownloadParcel(generateParcelId(), pFilename, pExecutableQuery);
    addDownloadParcel(lNewParcel);
    return lNewParcel;
  }

  @Override
  public DownloadParcel addZipQueryDownload(List<ExecutableQuery> pExecutableQueryList, String pFilename, int pZipCompressionLevel) {
    ZipQueryDownloadParcel lNewParcel = new ZipQueryDownloadParcel(generateParcelId(), pFilename, pExecutableQueryList, pZipCompressionLevel);
    addDownloadParcel(lNewParcel);
    return lNewParcel;
  }
}
