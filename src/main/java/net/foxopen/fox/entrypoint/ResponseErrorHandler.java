package net.foxopen.fox.entrypoint;

import net.foxopen.fox.FoxRequest;

/**
 * An object which can perform specialised error handling for a request.
 * See {@link net.foxopen.fox.entrypoint.servlets.ErrorServlet#setResponseErrorHandlerForRequest}.
 */
public interface ResponseErrorHandler {

  /**
   * Handles the given error. Although a FoxRequest is provided, consumers may be required to have a reference to the
   * response writer if the response has already begun to be sent.
   * @param pFoxRequest Request on which the error occurred.
   * @param pError Error that occurred.
   * @param pErrorRef Reference for the error assigned by the error logger.
   */
  void handleError(FoxRequest pFoxRequest, Throwable pError, String pErrorRef);

}
