package net.foxopen.fox.entrypoint.ws;

import java.util.Collection;

/**
 * A WebService is a collection of EndPoints with shared configuration options.
 */
public interface WebService {

  /**
   * Gets the name of this WebService, as it will appear in a URI.
   * @return
   */
  public String getName();

  /**
   * Gets the authentication settings required by all the EndPoints within this WebService.
   * @return
   */
  public WebServiceAuthDescriptor getAuthDescriptor();

  /**
   * Gets the database connect pool required by any of this WebService's EndPoints when connecting to the database. This
   * is used to set up the ContextUCon at the start of the request. If it is null, the engine's default is used. Implementors
   * should rely on the default unless they have a specific requirement to override it.
   * @return Connect key corresponding to an existing ConnnectionPool, or null.
   */
  public String getRequiredConnectionPoolName();

  /**
   * Gets all the EndPoints provided by this WebService.
   */
  public Collection<? extends EndPoint> getAllEndPoints();

}
