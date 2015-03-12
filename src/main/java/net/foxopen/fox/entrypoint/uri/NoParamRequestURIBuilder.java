package net.foxopen.fox.entrypoint.uri;

import net.foxopen.fox.ex.ExInternal;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

/**
 * RequestURIBuilder which does not allow params to be set. This should be used for shared/reusable URIBuilders where
 * it does not make sense to allow state modification.
 */
public class NoParamRequestURIBuilder
extends RequestURIBuilderImpl {

  protected NoParamRequestURIBuilder(HttpServletRequest pHttpServletRequest, String pAppMnem) {
    super(pHttpServletRequest, pAppMnem);
  }

  @Override
  public RequestURIBuilderImpl setParam(String pParamName, String pParamValue) {
    throw new ExInternal("You cannot set params on this URIBuilder");
  }

  @Override
  public RequestURIBuilder setParams(Map<String, String> pParamMap) {
    throw new ExInternal("You cannot set params on this URIBuilder");
  }
}
