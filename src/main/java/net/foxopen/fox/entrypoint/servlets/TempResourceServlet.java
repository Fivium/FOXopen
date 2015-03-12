package net.foxopen.fox.entrypoint.servlets;

import net.foxopen.fox.FoxResponseByteStream;
import net.foxopen.fox.XFUtil;
import net.foxopen.fox.entrypoint.FoxSession;
import net.foxopen.fox.entrypoint.UnauthenticatedFoxSession;
import net.foxopen.fox.ex.ExInternal;
import net.foxopen.fox.thread.RequestContext;
import net.foxopen.fox.thread.storage.StatefulThreadTempResourceProvider;
import net.foxopen.fox.thread.storage.TempResourceGenerator;

import java.io.IOException;

public class TempResourceServlet
extends EntryPointServlet {

  public static final String SERVLET_PATH = "temp";

  @Override
  protected String getContextUConInitialConnectionName() {
    return "TEMP_RESOURCE";
  }

  @Override
  protected String getTrackElementName(RequestContext pRequestContext) {
    return "TempResource";
  }

  @Override
  public FoxSession establishFoxSession(RequestContext pRequestContext) {
    return UnauthenticatedFoxSession.create();
  }

  @Override
  public void processGet(RequestContext pRequestContext) {
    StringBuilder lRequestURI = new StringBuilder(pRequestContext.getFoxRequest().getRequestURI());
    String lFilePath = XFUtil.pathPopTail(lRequestURI);
    String lFileId = XFUtil.pathPopTail(lRequestURI);

    //TODO get temp resource provider name from request
    TempResourceGenerator lTempResourceGenerator = new StatefulThreadTempResourceProvider().getExistingResourceGenerator(pRequestContext, lFileId);

    FoxResponseByteStream lFoxResponse = new FoxResponseByteStream(lTempResourceGenerator.getContentType(), pRequestContext.getFoxRequest(), lTempResourceGenerator.getCacheTimeMS());
    try {
      lTempResourceGenerator.streamOutput(lFoxResponse.getHttpServletOutputStream());
      lFoxResponse.getHttpServletOutputStream().close();
    }
    catch (IOException e) {
      throw new ExInternal("Failed to stream temp resource", e);
    }
  }
}
