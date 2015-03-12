package net.foxopen.fox.entrypoint.ws;

import net.foxopen.fox.FoxRequest;
import net.foxopen.fox.FoxResponse;
import net.foxopen.fox.FoxResponseByteStream;
import net.foxopen.fox.command.util.OutputStreamGenerator;
import net.foxopen.fox.ex.ExInternal;

import java.io.IOException;

/**
 * Response wrapper for a WebService which returns binary data.
 */
public class BinaryWebServiceResponse
extends WebServiceResponse {

  private final String mContentType;
  private final OutputStreamGenerator mStreamGenerator;
  private final long mCacheTimeMS;

  public BinaryWebServiceResponse(String pContentType, long pCacheTimeMS, OutputStreamGenerator pStreamGenerator) {
    mContentType = pContentType;
    mCacheTimeMS = pCacheTimeMS;
    mStreamGenerator = pStreamGenerator;
  }

  @Override
  boolean isTypeSupported(Type pType) {
    //TODO PN - fix
    return true;
  }

  @Override
  FoxResponse generateResponse(FoxRequest pFoxRequest, Type pType) {
    FoxResponseByteStream lResponseByteStream = new FoxResponseByteStream(mContentType, pFoxRequest, mCacheTimeMS);
    try {
      mStreamGenerator.writeOutput(lResponseByteStream.getHttpServletOutputStream());
    }
    catch (IOException e) {
      throw new ExInternal("Failed to stream binary web service response", e);
    }

    return lResponseByteStream;
  }
}
