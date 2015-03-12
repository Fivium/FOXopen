package net.foxopen.fox.entrypoint.ws;

import java.util.Collection;

/**
 * A WebServiceCategory is the top level point of delegation for WebService requests. WebService categories are registered
 * on the {@link WebServiceServlet} and provide WebServices to it, which in turn provide EndPoints.
 */
public interface WebServiceCategory {

  /**
   * Gets the name of this category as it will appear in a URI.
   * @return
   */
  public String getName();

  /**
   * Gets all the WebServices which this category can provide.
   * @return
   */
  public Collection<? extends WebService> getAllWebServices();

}
