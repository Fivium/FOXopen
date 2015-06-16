package net.foxopen.fox.thread.persistence;

import net.foxopen.fox.thread.RequestContext;
import net.foxopen.fox.thread.persistence.SharedDOMManager.SharedDOMType;

public interface PersistenceContext {

  void startPersistenceCycle(RequestContext pRequestContext);

  void registerListeningPersistable(ListeningPersistable pPersistable);

  /**
   * Marks the given Persistable object as requiring some sort of persistence operation to be performed on it at the end
   * of the current persistence cycle.
   * @param pPersistable Object to be persisted.
   * @param pMethod The PersistenceMethod which the given Persistable requires. If the object is marked multiple times within
   *                the current cycle, only the operation with the highest precedence will be performed.
   * @param pFacetsToMark 0 or more PersistenceFacets to mark as requiring an update as part of this operation. These should
   *                      be related to the Persistable being marked.
   */
  void requiresPersisting(Persistable pPersistable, PersistenceMethod pMethod, PersistenceFacet... pFacetsToMark);

  void endPersistenceCycle(RequestContext pRequestContext);

  String getThreadId();

  Serialiser getSerialiser();

  Deserialiser setupDeserialiser(RequestContext pRequestContext);

  Deserialiser getDeserialiser();

  SharedDOMManager getSharedDOMManager(SharedDOMType pDOMType, String pDOMId);

  /**
   * Tests if the given {@link PersistenceFacet} has been marked as requiring an update as part of the current persistnce cycle.
   * @param pFacet PersistenceFacet to test.
   * @return True if the facet has been marked during this persistence cycle.
   */
  boolean isFacetMarked(PersistenceFacet pFacet);

}
