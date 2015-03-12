package net.foxopen.fox.thread.persistence;

import net.foxopen.fox.download.DownloadParcel;
import net.foxopen.fox.module.facet.ModuleFacet;
import net.foxopen.fox.module.facet.ModuleFacetType;
import net.foxopen.fox.thread.persistence.data.InternalDOMPersistedData;
import net.foxopen.fox.thread.persistence.data.ModuleCallPersistedData;
import net.foxopen.fox.thread.persistence.data.StateCallPersistedData;
import net.foxopen.fox.thread.persistence.data.StatefulXThreadPersistedData;

import java.util.Collection;
import java.util.List;


public interface Deserialiser {

  public StatefulXThreadPersistedData getXThreadPersistedData(String pThreadId);

  public List<ModuleCallPersistedData> getModuleCallPersistedData(String pThreadId);

  public List<StateCallPersistedData> getStateCallPersistedData(String pModuleCallId);

  /**
   * Gets the PersistedData for an internal DOM (:{theme}, :{env}, etc). The result may be null if the DOM was not modified
   * before the module call was serialised.
   * @param pModuleCallId Module call the DOM belongs to.
   * @param pDocumentName Name of the document (not necessarily the DOM's root element name).
   * @return PersistedData or null.
   */
  public InternalDOMPersistedData getInternalDOMPersistedData(String pModuleCallId, String pDocumentName);

  /**
   * Deserialises the download parcel of the given ID for the current thread. Returns null if the parcel is not serialised.
   * @param pDownloadParcelId
   * @return DownloadParcel or null.
   */
  public DownloadParcel getDownloadParcel(String pDownloadParcelId);

  /**
   * Gets a collection of all ModuleFacets of the given type for the given module call ID. If the serialised objects
   * cannot be cast to the given class, an exception is raised.
   * @param pModuleCallId Module call ID to retrieve Facets for.
   * @param pFacetType Type of Facet to retrieve.
   * @param pFacetClass Marker class indicating expected Facet type. Used for type safety. This may be an interface or
   *                    abstract (returned objects are guaranteed to be a subclass).
   * @param <T> Expected type for returned Facets.
   * @return Collection of Facets which may be empty if none are found.
   */
  public <T extends ModuleFacet> Collection<T> getModuleCallFacets(String pModuleCallId, ModuleFacetType pFacetType, Class<T> pFacetClass);
}
