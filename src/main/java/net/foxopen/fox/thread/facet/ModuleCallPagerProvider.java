package net.foxopen.fox.thread.facet;

import net.foxopen.fox.XFUtil;
import net.foxopen.fox.dbinterface.InterfaceQuery;
import net.foxopen.fox.dom.paging.DOMPager;
import net.foxopen.fox.dom.paging.DatabasePager;
import net.foxopen.fox.dom.paging.EvaluatedPagerSetup;
import net.foxopen.fox.dom.paging.Pager;
import net.foxopen.fox.dom.paging.PagerProvider;
import net.foxopen.fox.ex.ExInternal;
import net.foxopen.fox.module.facet.ModuleFacetType;
import net.foxopen.fox.thread.persistence.PersistenceContext;
import net.foxopen.fox.thread.stack.ModuleCall;

import java.util.HashMap;
import java.util.Map;

/**
 * PagerProvider for a ModuleCall which can serialise the Pagers to a PersistenceContext as ModuleFacets.
 */
public class ModuleCallPagerProvider
extends ModuleCallFacetProvider<Pager>
implements PagerProvider {

  public static ModuleCallFacetProvider.Builder getBuilder() {
    return new Builder();
  }

  private ModuleCallPagerProvider(PersistenceContext pPersistenceContext, ModuleCall pModuleCall, Map pFacetKeyToFacetMap) {
    super(pPersistenceContext, pModuleCall, pFacetKeyToFacetMap);
  }

  private String generatePagerKey(String pInvokeName, String pMatchId) {
    return pInvokeName + "/" + XFUtil.nvl(pMatchId, "");
  }

  private Pager getPagerInternal(String lPagerKey) {

    Pager lPager = getFacetByKey(lPagerKey);
    if(lPager == null) {
      throw new ExInternal("Pager not found for key " + lPagerKey);
    }
    return lPager;
  }

  @Override
  public Pager getPager(String pInvokeName, String pOptionalMatchId) {
    return getPagerInternal(generatePagerKey(pInvokeName, pOptionalMatchId));
  }

  @Override
  public Pager getPagerOrNull(String pInvokeName, String pOptionalMatchId) {
    return getFacetByKey(generatePagerKey(pInvokeName, pOptionalMatchId));
  }

  @Override
  public Pager getPagerByKey(String pPagerKey) {
    return getPagerInternal(pPagerKey);
  }

  @Override
  public DOMPager getOrCreateDOMPager(EvaluatedPagerSetup pPagerSetup) {
    String lPagerKey = generatePagerKey(pPagerSetup.getInvokeName(), pPagerSetup.getMatchId());

    Pager lPager = getFacetByKey(lPagerKey);
    if(lPager == null) {
      lPager = new DOMPager(lPagerKey, getModuleCall().getCallId(), pPagerSetup);
      registerNewFacet(lPager);
    }

    if(!(lPager instanceof DOMPager)) {
      throw new ExInternal("Pager for key " + lPagerKey + " was expected to be a DOMPager but was a " + lPager.getClass().getSimpleName());
    }

    return (DOMPager) lPager;
  }

  @Override
  public DatabasePager getOrCreateDatabasePager(EvaluatedPagerSetup pPagerSetup, InterfaceQuery pInterfaceQuery) {

    String lPagerKey = generatePagerKey(pPagerSetup.getInvokeName(), pPagerSetup.getMatchId());

    boolean lPagerAlreadyExists = getFacetByKey(lPagerKey) != null;

    //Always construct a new pager object to reflect potential changes to the pagination definition
    Pager lPager = DatabasePager.createDatabasePager(lPagerKey, getModuleCall().getCallId(), pPagerSetup, pInterfaceQuery, getModuleCall().getModule());

    //Serialise the new pager correctly
    if(!lPagerAlreadyExists) {
      registerNewFacet(lPager);
    }
    else {
      //Even though we've created a new object, the entity which was already serialised merely requires an update as it has the same key
      updateExistingFacet(lPager);
    }

    if(!(lPager instanceof DatabasePager)) {
      throw new ExInternal("Pager for key " + lPagerKey + " was expected to be a DatabasePager but was a " + lPager.getClass().getSimpleName());
    }

    return (DatabasePager) lPager;
  }

  private static final class Builder extends ModuleCallFacetProvider.Builder<PagerProvider> {

    private Builder() {
      super(PagerProvider.class);
    }

    @Override
    public PagerProvider createNewProvider(PersistenceContext pPersistenceContext, ModuleCall pModuleCall) {
      return new ModuleCallPagerProvider(pPersistenceContext, pModuleCall, new HashMap<String, Pager>());
    }

    @Override
    public PagerProvider deserialiseExistingProvider(PersistenceContext pPersistenceContext, ModuleCall pModuleCall) {
      return new ModuleCallPagerProvider(pPersistenceContext, pModuleCall, deserialiseFacets(pPersistenceContext, pModuleCall, ModuleFacetType.PAGER, Pager.class));
    }
  }
}
