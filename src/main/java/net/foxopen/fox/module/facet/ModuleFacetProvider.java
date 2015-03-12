package net.foxopen.fox.module.facet;

import java.util.Collection;

/**
 * An object which can maintain a list of ModuleFacets and probably create new ones as they are required. Due to the nature
 * of ModuleFacets, this interface does not describe how the facets should be retrieved or created. The owner of a Provider
 * should provide consumers with a type-safe method for retrieving the Provider cast to its appropriate subclass.<br><br>
 *
 * A ModuleFacetProvider should typically be scoped to a module call.
 */
public interface ModuleFacetProvider {

  /**
   * Gets a read-only collection of all the Facets this Provider knows about.
   * @return
   */
  public Collection<? extends ModuleFacet> getAllFacets();

  /**
   * Outputs the contents of this provider to a human readable string for debugging purposes.
   * @return
   */
  public String debugOutput();

}
