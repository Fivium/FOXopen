package net.foxopen.fox.filetransfer;

import net.foxopen.fox.FoxRequest;
import net.foxopen.fox.ex.ExInternal;
import org.apache.commons.fileupload.FileItemIterator;
import org.apache.commons.fileupload.FileItemStream;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.fileupload.util.Streams;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * Object which handles the reading of a Multipart form request. When {@link #readFormData()} is invoked, the object
 * reads the request until it encounters a file item (or the end of the request). Consumers may then use {@link #getUploadFileItem()}
 * to retrieve the file to be uploaded and continue the processing from that point. This pattern allows one consumer to read
 * form fields while another consumer handles the actual file upload.
 */
public class MultipartUploadReader {

  private final ServletFileUpload mServletFileUploadUpload;
  private final FiletransferProgressListener mFiletransferProgressListener;
  private final FileItemIterator mItemIterator;

  private FileItemStream mUploadFileItem;

  private final Map<String, String> mFieldParamMap = new HashMap<>();

  /**
   * Creates a new Reader for reading the given HTTP request. This must be a multipart request.
   * @param pFoxRequest
   */
  public MultipartUploadReader(FoxRequest pFoxRequest) {
    try {
      // New file upload handler
      mServletFileUploadUpload = new ServletFileUpload();

      // Create a progress listener for this upload
      mFiletransferProgressListener = new FiletransferProgressListener();

      // Attach the progress listener to this upload
      mServletFileUploadUpload.setProgressListener(mFiletransferProgressListener);
      //mUploadInfo.setProgressListener(mFiletransferProgressListener);

      // Parse the upload request
      mItemIterator = mServletFileUploadUpload.getItemIterator(pFoxRequest.getHttpRequest());
    }
    catch (Throwable ex) {
      throw new ExInternal("Error encountered while trying to initialise a file upload work item.\nOriginal error: " + ex.getMessage());
    }
  }

  /**
   * Reads form values from the multipart request until a file is encountered. Field values are stored as strings for
   * retrieval using {@link #getFormFieldValue}.
   * @return  True if there is an upload file available to read via {@link #getUploadFileItem()}.
   */
  public boolean readFormData() {
    mUploadFileItem = null;
    try {
      while (mItemIterator.hasNext()) {

        FileItemStream lCurrentItem = mItemIterator.next();
        /**
         * NOTE: the InputStream here is read here in a blocking way. Long upload hangs have been observed on live
         * environments at this point due to network issues. It should be possible to convert the stream to a
         * non-blocking stream at this point if required.
         */
        InputStream lItemInputStream = lCurrentItem.openStream();

        if (lCurrentItem.isFormField()) {
          //Read form values into the map
          String lParamName = lCurrentItem.getFieldName();
          String lFieldValue = Streams.asString(lItemInputStream);

          mFieldParamMap.put(lParamName, lFieldValue);
        }
        else {
          //We've hit the file field, so stop the read for now
          mUploadFileItem = lCurrentItem;
          break;
        }

        lItemInputStream.close();
      }

    }
    catch (IOException | FileUploadException e) {
      throw new ExInternal("Failed to read form data for the multipart request", e);
    }

    return mUploadFileItem != null;
  }

  /**
   * Gets the value supplied in the upload form for the given field name. NOTE: fields must appear before the file in
   * the multipart request to be populated into the map.
   * @param pFieldName
   * @return
   */
  public String getFormFieldValue(String pFieldName) {
    return mFieldParamMap.get(pFieldName);
  }

  public FileItemStream getUploadFileItem() {
    return mUploadFileItem;
  }

  FiletransferProgressListener getFiletransferProgressListener() {
    return mFiletransferProgressListener;
  }

}
