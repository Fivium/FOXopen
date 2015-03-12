package net.foxopen.fox.module.facet;

import net.foxopen.fox.thread.persistence.Persistable;

/**
 * A stateful, persisted object which is scoped to and serailised against a module call. Typically these will contain
 * information about the state of some client side feature which needs to be regularly updated without incurring a DOM
 * write. ModuleFacets are accessed via a dedicated {@link ModuleFacetProvider} corresponding to their type.<br><br>
 *
 * A ModuleFacet's key uniquely identifies a facet of its type within its owning module call. Therefore, different facets
 * may have the same facet key, so long as they are of a different facet type or belong to a different module call. <br><br>
 *
 * ModuleFacets are automatically serialised using the Kryo library. Care must be taken to ensure that transient data
 * or complex object graphs are not serialised. The non-static fields of a ModuleFacet should typically be simple Java types
 * to minimise the risk of an overly complex serialisation. Facets which require additional initialisation after deserialisation
 * should implement {@link net.foxopen.fox.thread.persistence.DeserialisationHandler}.
 */
public interface ModuleFacet
extends Persistable {

  /**
   * Gets a key which uniquely identifies this ModuleFacet within its owning module call and facet type.
   * @return
   */
  public String getFacetKey();

  /**
   * Gets the module call id of the module call which owns this facet.
   * @return
   */
  public String getModuleCallId();

  /**
   * Gets the type of this facet for serialisation purposes.
   * @return
   */
  public ModuleFacetType getFacetType();

}
