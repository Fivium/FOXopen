package net.foxopen.fox.thread;

import net.foxopen.fox.FoxRequest;
import net.foxopen.fox.entrypoint.ResponseErrorHandler;
import net.foxopen.fox.module.fieldset.FieldSet;
import net.foxopen.fox.module.serialiser.html.HTMLSerialiser;

/**
 * ResponseErrorHandler which holds a reference to a Streaming HTMLOutputSerialiser, and injects a JS redirect into the
 * HTML which in turn sends the user to a full error page. Redirection in this way appears to work for most cases.
 */
public class ThreadStreamingResponseErrorHandler
implements ResponseErrorHandler {

  private final String mThreadId;
  private final String mTrackId;
  private final String mRestoreFieldSetLabel;
  private final HTMLSerialiser mOutputSerialiser;

  /**
   * Creates a new ResponseErrorHandler to handle any error which may occur for a single response.
   * @param pThreadId Thread ID of the request.
   * @param pTrackId Track ID of the request.
   * @param pPreviousFieldSet FieldSet from the previous churn. This is used to restore the FieldSet cookie to a valid value
   *                          on the error screen.
   * @param pHTMLSerialiser The serialiser being used to generate the response. This must be used to send the error redirect
   *                        as it has a reference to the response's Writer.
   * @return New ResponseErrorHandler.
   */
  static ResponseErrorHandler createForRequest(String pThreadId, String pTrackId, FieldSet pPreviousFieldSet, HTMLSerialiser pHTMLSerialiser) {

    String lPreviousFieldSetLabel = null;
    if (pPreviousFieldSet != null) {
      lPreviousFieldSetLabel = pPreviousFieldSet.getOutwardFieldSetLabel();
    }

    return new ThreadStreamingResponseErrorHandler(pThreadId, pTrackId, lPreviousFieldSetLabel, pHTMLSerialiser);
  }

  private ThreadStreamingResponseErrorHandler(String pThreadId, String pTrackId, String pRestoreFieldSetLabel, HTMLSerialiser pOutputSerialiser) {
    mThreadId = pThreadId;
    mTrackId = pTrackId;
    mRestoreFieldSetLabel = pRestoreFieldSetLabel;
    mOutputSerialiser = pOutputSerialiser;
  }

  @Override
  public void handleError(FoxRequest pFoxRequest, Throwable pError, String pErrorRef) {
    //Delegate to the implicated OutputSerialiser to write a JS error redirect into the stream
    mOutputSerialiser.handleStreamingError(pError, pErrorRef, mThreadId, mTrackId, mRestoreFieldSetLabel);
  }
}
