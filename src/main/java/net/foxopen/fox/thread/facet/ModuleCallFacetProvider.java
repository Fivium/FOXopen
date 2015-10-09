package net.foxopen.fox.thread.facet;

import net.foxopen.fox.module.facet.ModuleFacet;
import net.foxopen.fox.module.facet.ModuleFacetProvider;
import net.foxopen.fox.module.facet.ModuleFacetType;
import net.foxopen.fox.thread.persistence.PersistenceContext;
import net.foxopen.fox.thread.persistence.PersistenceMethod;
import net.foxopen.fox.thread.persistence.xstream.XStreamManager;
import net.foxopen.fox.thread.stack.ModuleCall;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * A ModuleFacetProvider which serialises its contents against its owning ModuleCall using the ModuleCall's PersistenceContext.
 * <br><br>
 *
 * This implementation stores a map of ModuleFacets against their facet keys. Implementors should delegate to this map
 * to store their facets. This map should be restored when the Provider is deserialised.<br><br>
 *
 * A Provider is created from an associated {@link Builder} instance. The Builder instances should be used by a new or
 * deserialised ModuleCall to bootstrap/recreate the ModuleCall's set of Providers.
 *
 * @param <T> ModuleFacet type which the implemtor provides.
 */
public abstract class ModuleCallFacetProvider<T extends ModuleFacet>
implements ModuleFacetProvider {

  /** All Provider Builders which need to be used when a ModuleCall is created */
  private static final Set<Builder<?>> BUILDERS = new HashSet<>();

  private final PersistenceContext mPersistenceContext;
  private final ModuleCall mModuleCall;
  private final Map<String, T> mFacetKeyToFacetMap;

  static {
    //Explicitly register all the provider builders. This must be done in a "pull" style, otherwise builder classes
    //are not created by the class loader.
    BUILDERS.add(ModuleCallModalPopoverProvider.getBuilder());
    BUILDERS.add(ModuleCallTabGroupProvider.getBuilder());
    BUILDERS.add(ModuleCallPagerProvider.getBuilder());
  }

  /**
   * Gets all the Provider Builders that have been registered on this class.
   * @return
   */
  public static Collection<Builder<?>> getAllBuilders() {
    return Collections.unmodifiableCollection(BUILDERS);
  }

  protected ModuleCallFacetProvider(PersistenceContext pPersistenceContext, ModuleCall pModuleCall, Map<String, T> pFacetKeyToFacetMap) {
    mPersistenceContext = pPersistenceContext;
    mModuleCall = pModuleCall;
    mFacetKeyToFacetMap = pFacetKeyToFacetMap;
  }

  @Override
  public Collection<T> getAllFacets() {
    return Collections.unmodifiableCollection(mFacetKeyToFacetMap.values());
  }

  /**
   * Gets a Facet for the given key from the Facet map. Returns null if the Facet does not exist.
   * @param pFacetKey
   * @return
   */
  protected T getFacetByKey(String pFacetKey) {
    return mFacetKeyToFacetMap.get(pFacetKey);
  }

  /**
   * Registers a new Facet in the map and marks it as requiring serialisation.
   * @param pNewFacet The newly created facet.
   */
  protected void registerNewFacet(T pNewFacet) {
    mFacetKeyToFacetMap.put(pNewFacet.getFacetKey(), pNewFacet);
    mPersistenceContext.requiresPersisting(pNewFacet, PersistenceMethod.CREATE);
  }

  /**
   * Marks the given facet as requiring an update. Note that a new object may be created which represents an existing facet -
   * this should be considered an update as there should already be a serialised entity.
   * @param pFacet Facet which has been updated.
   */
  protected void updateExistingFacet(T pFacet) {
    //Put into the map again, in case the caller has actaully created a new object
    mFacetKeyToFacetMap.put(pFacet.getFacetKey(), pFacet);
    mPersistenceContext.requiresPersisting(pFacet, PersistenceMethod.UPDATE);
  }

  /**
   * Removes the given facet from this provider and marks it as requiring a delete.
   * @param pFacet Facet to delete.
   */
  protected void deleteFacet(T pFacet) {
    mFacetKeyToFacetMap.remove(pFacet.getFacetKey());
    mPersistenceContext.requiresPersisting(pFacet, PersistenceMethod.DELETE);
  }

  protected ModuleCall getModuleCall() {
    return mModuleCall;
  }

  protected PersistenceContext getPersistenceContext() {
    return mPersistenceContext;
  }

  public String debugOutput() {
    return XStreamManager.serialiseObjectToXMLString(mFacetKeyToFacetMap);
  }

  /**
   * ModuleCallFacetProviders should extend this Builder and register an instance statically on this class. The Builder
   * is used to create a Provider for a new module call or deserialise a provider for an existing module call.
   * @param <T> Type of Provider being created by this builder.
   */
  public static abstract class Builder<T extends ModuleFacetProvider> {

    private final Class<T> mFacetProviderClass;

    protected Builder(Class<T> pFacetProviderClass) {
      mFacetProviderClass = pFacetProviderClass;
    }

    /**
     * Deserialises a map of Facet keys to Facets from the given PersistenceContext.
     * @param pPersistenceContext For deserialising the Facets.
     * @param pModuleCall ModuleCall to deserialise the Facets for.
     * @param pFacetType Type of Facet to be deserialised.
     * @param pFacetClass Marker class for providing type safety. Deserialised Facets should be castable to this class.
     * @param <T> Facet type to be returned.
     * @return Map of keys to Facets.
     */
    protected static <T extends ModuleFacet> Map<String, T> deserialiseFacets(PersistenceContext pPersistenceContext, ModuleCall pModuleCall, ModuleFacetType pFacetType, Class<T> pFacetClass) {

      Collection<T> lModuleCallFacets = pPersistenceContext.getDeserialiser().getModuleCallFacets(pModuleCall.getCallId(), pFacetType, pFacetClass);

      Map lFacetMap = new HashMap<String, T>();
      for(ModuleFacet lFacet : lModuleCallFacets) {
        lFacetMap.put(lFacet.getFacetKey(), lFacet);
      }

      return lFacetMap;
    }

    /**
     * Creates a ModuleCallFacetProvider for the given module call.
     * @param pPersistenceContext Current PersistenceContext.
     * @param pModuleCall ModuleCall which will own the new Provider.
     * @return
     */
    public abstract T createNewProvider(PersistenceContext pPersistenceContext, ModuleCall pModuleCall);

    /**
     * Deserialises an existing Provider, including its Facet map.
     * @param pPersistenceContext Current PersistenceContext.
     * @param pModuleCall ModuleCall to deserialise the Facets for.
     * @return
     */
    public abstract T deserialiseExistingProvider(PersistenceContext pPersistenceContext, ModuleCall pModuleCall);

    /**
     * Gets the Provider type this Builder will create.
     * @return
     */
    public Class<T> getFacetProviderClass() {
      return mFacetProviderClass;
    }
  }
}
