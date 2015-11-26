package net.foxopen.fox;

import net.foxopen.fox.ex.ExInternal;

import java.io.IOException;


/**
 * A URI/URL-based resource reference.
 */
public class URIResourceReference extends FoxResponse {

  private final String mURIString;

  public URIResourceReference(String uri) {
    mURIString = uri;
  }

  public String getURI() {
    return mURIString;
  }

  public void respond(FoxRequest request) throws ExInternal {
    try {
      request.getHttpResponse().sendRedirect(mURIString);
    }
    catch (IOException ex) {
      throw new ExInternal("Unexpected error redirecting client to uri=\"" +getURI()+ "\"", ex);
    }
  }
}
