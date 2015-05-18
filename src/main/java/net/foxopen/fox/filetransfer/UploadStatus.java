package net.foxopen.fox.filetransfer;

/**
 * Valid statuses for an UploadInfo to have.
 */
public enum UploadStatus {

  NOT_STARTED("not-started", "Not started", false),
  STARTED("started", "Started file upload", true),
  RECEIVING("receiving", "Uploading [[filename]]...", true),
  CONTENT_CHECK("content-check", "Checking content of [[filename]]...", true),
  VIRUS_CHECK("virus-check", "Virus scanning [[filename]]...", true),
  SIGNATURE_VERIFY("signature-verification", "Verifying digital signatures on [[filename]]...", true),
  STORING("storing", "Storing [[filename]]...", true),
  COMPLETE("complete", "Upload complete", false),
  FAILED("failed", "Upload failed", false),
  CONTENT_CHECK_FAILED("failed", "Upload failed", false),
  VIRUS_CHECK_FAILED("failed", "Upload failed", false);

  private final String mStringRepresentation;
  private final String mReadableMessage;
  private final boolean mInProgress;

  public static UploadStatus fromString(String pStringRepresentation) {
    for(UploadStatus lStatus : values()) {
      if(lStatus.getStringRepresentation().equals(pStringRepresentation)) {
        return lStatus;
      }
    }

    return null;
  }

  UploadStatus(String pStringRepresentation, String pReadableMessage, boolean pInProgress) {
    mStringRepresentation = pStringRepresentation;
    mReadableMessage = pReadableMessage;
    mInProgress = pInProgress;
  }

  public String getStringRepresentation() {
    return mStringRepresentation;
  }

  public String getReadableMessage(String pFilename) {
    return mReadableMessage.replaceAll("\\[\\[filename\\]\\]", pFilename);
  }

  public boolean isInProgress() {
    return mInProgress;
  }

}
