package net.foxopen.fox.download;

import net.foxopen.fox.command.XDoResult;

/**
 * XDoResult for a file download/popup to be displayed to the user when a page is generated.
 */
public class DownloadLinkXDoResult
implements XDoResult {

  private final String mParcelId;
  private final String mDownloadURL;
  private final String mFilename;

  /**
   * Create a new DownloadLink which will be sent to the user when the page generates and result in a download popup.
   * @param pParcelId    Stream parcel ID for the download.
   * @param pDownloadURL Relative download URL.
   * @param pFilename Filename to be displayed in the link on the page. This should typically correspond to the filename
   * of the target download parcel, but it does not have to.
   */
  public DownloadLinkXDoResult(String pParcelId, String pDownloadURL, String pFilename) {
    mParcelId = pParcelId;
    mDownloadURL = pDownloadURL;
    mFilename = pFilename;
  }

  public String getParcelId() {
    return mParcelId;
  }

  public String getDownloadURL() {
    return mDownloadURL;
  }

  public String getFilename() {
    return mFilename;
  }
}
