package net.foxopen.fox.download;

import java.io.IOException;
import java.io.OutputStream;

import net.foxopen.fox.database.UCon;


/**
 * Stateful object representing a file download. A DownloadParcel is created by action code and stored in a DownloadManager,
 * which manages URLs and access controls etc for the parcel. In this state a URL can be generated for it and served to
 * the user. When the user requests a download, the parcel should query the target file just-in-time and stream it back as
 * the response.<br/><br/>
 *
 * DownloadParcels may be serialised by their Manager, but typically this should only be allowed if the download has not
 * been served out, as the serialiser should not be recording any transient fields such as LOB locators, etc.
 */
public interface DownloadParcel {
  
  /**
   * Gets the unique ID of this DownloadParcel.
   * @return Parcel ID.
   */
  public String getParcelId();
  
  /**
   * Gets the filename of the file which this download parcel will serve out.
   * @return Filename.
   */
  public String getFilename();
  
  /**
   * Gets the MIME type of the file which this download parcel will serve out.
   * @return MIME type.
   */
  public String getContentType();
  
  /**
   * Gets the size of the file which this download parcel will serve out. Returns -1 for an unknown size. Implementors should
   * endeavour to report this if possible so the user is presented with a download progress indicator in their browser.
   * @return Positive integer, or -1 if size is not known.
   */
  public long getFileSizeBytes();
  
  /**
   * Gives this DownloadParcel an opportunity to perform any initialisation required before the download is started.
   * This method is called before the metadata getters so can be used to query in these (i.e. content type, size, etc).
   * @param pUCon UCon for querying.
   */
  public void prepareForDownload(UCon pUCon);
  
  /**
   * Streams
   * @param pUCon UCon for querying.
   * @param pOutputStream Destination of the file download.
   * @throws IOException If a transfer error occurs.
   */
  public void streamDownload(UCon pUCon, OutputStream pOutputStream)
  throws IOException;
  
  /**
   * Gives this DownloadParcel an opportunity to perform any cleanup required after the download is finished. (I.e.
   * closing streams, etc).
   * @param pUCon UCon for querying.
   */
  public void closeAfterDownload(UCon pUCon);
  
  /**
   * Checks if this DownloadParcel is in a state where it is allowed to be serialised. Typically, a parcel should not
   * be serialised after its associated LOB has been selected.
   * @return True if the parcel is allowed to be serialised.
   */
  public boolean isSerialiseAllowed();
  
}
