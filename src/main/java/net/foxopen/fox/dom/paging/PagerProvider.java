package net.foxopen.fox.dom.paging;

import net.foxopen.fox.dbinterface.InterfaceQuery;
import net.foxopen.fox.module.facet.ModuleFacetProvider;

/**
 * An object which maintains a list of existing Pagers and is able to create new Pagers when needed.
 */
public interface PagerProvider
extends ModuleFacetProvider {

  /**
   * Gets a pager with the given invoke name and optional match id for database pagers. This will error if a matching
   * pager does not exist.
   * @param pInvokeName
   * @param pOptionalMatchId
   * @return
   */
  public Pager getPager(String pInvokeName, String pOptionalMatchId);

  /**
   * Gets a pager with the given invoke name and optional match id for database pagers, or null if one cannot be found.
   * @param pInvokeName
   * @param pOptionalMatchId
   * @return
   */
  public Pager getPagerOrNull(String pInvokeName, String pOptionalMatchId);

  /**
   * Resolves a pager from its pager key. This will error if a matching pager does not exist.
   * @param pPagerKey
   * @return The resolved pager.
   */
  public Pager getPagerByKey(String pPagerKey);

  /**
   * Gets a DOM pager according to parameters in the provided setup object. If the pager does not exist, it is created.
   * @param pPagerSetup Evaluated pager setup info.
   * @return New or existing DOM pager.
   */
  public DOMPager getOrCreateDOMPager(EvaluatedPagerSetup pPagerSetup);

  /**
   * Gets a database pager according to parameters in the provided setup object. If the pager does not exist, it is created.
   * @param pPagerSetup Evaluated pager setup info.
   * @param pInterfaceQuery Query being executed in a paged mode.
   * @return New or existing database pager.
   */
  public DatabasePager getOrCreateDatabasePager(EvaluatedPagerSetup pPagerSetup, InterfaceQuery pInterfaceQuery);

}
