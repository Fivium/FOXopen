package net.foxopen.fox.thread;

import net.foxopen.fox.FoxResponse;
import net.foxopen.fox.auth.AuthenticationContext;
import net.foxopen.fox.entrypoint.uri.RequestURIBuilder;
import net.foxopen.fox.thread.stack.ModuleCall;

import java.util.Map;


public interface XThreadInterface {

  public FoxResponse startThread(RequestContext pRequestContext, ModuleCall.Builder pInitialModuleCallBuilder, boolean pGenerateResponse);

  public FoxResponse processAction(RequestContext pRequestContext, String pActionName, String pActionRef, Map<String, String[]> pPostedFormValuesMap);

  public String getThreadRef();

  public String getEntryURI(RequestURIBuilder pURIBuilder);

  public AuthenticationContext getAuthenticationContext();

}
