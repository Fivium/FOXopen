package net.foxopen.fox.thread.persistence;

/**
 * A known set of facets which can be marked as individually requiring persistence. These are used to prevent unnecessary
 * serialisation of large/complex objects. For instance, instead of updating the XThread property map every time an XThread is
 * updated, the THREAD_PROPERTIES facet can be marked on the {@link PersistenceContext} only when the map changes, so the
 * {@link DatabaseSerialiser} knows the write can be skipped at the end of the cycle if necessary.
 */
public enum PersistenceFacet {

  THREAD_PROPERTIES,
  MODULE_CALL_SECURITY_SCOPE,
  MODULE_CALL_XPATH_VARIABLES

}
