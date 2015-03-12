package net.foxopen.fox.module.mapset;

import java.util.Map;

import net.foxopen.fox.command.XDoCommandList;
import net.foxopen.fox.database.UCon;
import net.foxopen.fox.database.sql.ExecutableQuery;
import net.foxopen.fox.database.sql.ScalarResultDeliverer;
import net.foxopen.fox.database.sql.ScalarResultType;
import net.foxopen.fox.database.sql.bind.BindObject;
import net.foxopen.fox.database.sql.bind.BindObjectProvider;
import net.foxopen.fox.database.sql.bind.StringBindObject;
import net.foxopen.fox.dom.DOM;
import net.foxopen.fox.ex.ExDB;
import net.foxopen.fox.ex.ExInternal;
import net.foxopen.fox.ex.ExModule;
import net.foxopen.fox.module.Mod;
import net.foxopen.fox.module.mapset.MapSetDefinitionFactory.DefinitionBuilder;
import net.foxopen.fox.thread.ActionRequestContext;
import net.foxopen.fox.thread.storage.CacheKey;
import net.foxopen.fox.thread.storage.StatementType;
import net.foxopen.fox.thread.storage.StorageLocation;
import net.foxopen.fox.thread.storage.StorageLocationBind;


/**
 * A MapSet which is constructed from a storage location's SELECT query. This is a legacy feature.
 */
public class SLQueryDefinition
extends MapSetDefinition {

  static class Builder
  extends DefinitionBuilder {

    private final StorageLocation mStorageLocation;

    Builder(StorageLocation pStorageLocation)
    throws ExModule {
      mStorageLocation = pStorageLocation;
    }

    @Override
    protected CacheKey getAutoCacheKeyOrNull() {
      throw new ExInternal("Auto cache key should never be requested for SL definitions");
    }

    @Override
    protected Long getDefaultTimeoutMinsOrNull() {
      //Force user to specify
      return null;
    }

    @Override
    protected MapSetDefinition buildInternal(String pLocalName, CacheKey pCacheKey, XDoCommandList pXDo, long pRefreshTimeoutMins, Mod pModule) throws ExModule {
      return new SLQueryDefinition(pLocalName, pCacheKey, pXDo, pRefreshTimeoutMins, pModule, mStorageLocation);
    }
  }

  private final StorageLocation mStorageLocation;

  private SLQueryDefinition(String pLocalName, CacheKey pCacheKey, XDoCommandList pXDo, long pRefreshTimeoutMins, Mod pModule, StorageLocation pStorageLocation) {
    super(pLocalName, pCacheKey, pXDo, pRefreshTimeoutMins, pModule);
    mStorageLocation = pStorageLocation;
  }

  @Override
  protected DOM createMapSetDOM(ActionRequestContext pRequestContext, DOM pItemDOM, String pEvaluatedCacheKey, String pUniqueValue) {

    Map<StorageLocationBind, String> lBinds = mStorageLocation.evaluateStringBinds(StatementType.QUERY, pRequestContext.getContextUElem(), pUniqueValue);
    BindObjectProvider lBindProvider = new SLQueryBindProvider(lBinds);

    ExecutableQuery lExecutableQuery = mStorageLocation.getDatabaseStatement(StatementType.QUERY).getParsedStatement().createExecutableQuery(lBindProvider);

    DOM lMapSetDOM;
    UCon lUCon = pRequestContext.getContextUCon().getUCon("Create SL MapSet");
    try {
      ScalarResultDeliverer<DOM> lDeliverer = ScalarResultType.DOM.getResultDeliverer();
      lExecutableQuery.executeAndDeliver(lUCon, lDeliverer);
      lMapSetDOM = lDeliverer.getResult();
    }
    catch (ExDB e) {
      throw new ExInternal("Error running mapset storage location query", e);
    }
    finally {
      pRequestContext.getContextUCon().returnUCon(lUCon, "Create SL MapSet");
    }

    return lMapSetDOM;
  }

  private class SLQueryBindProvider
  implements BindObjectProvider {

    private final Map<StorageLocationBind, String> mBinds;

    public SLQueryBindProvider(Map<StorageLocationBind, String> pBinds) {
      mBinds = pBinds;
    }

    @Override
    public boolean isNamedProvider() {
      return false;
    }

    @Override
    public BindObject getBindObject(String pBindName, int pIndex) {
      //Look up the bind definition for this name/index
      StorageLocationBind lBindDefn = mStorageLocation.getDatabaseStatement(StatementType.QUERY).getBind(pBindName, pIndex);
      //Get the bind value from the map
      return new StringBindObject(mBinds.get(lBindDefn));
    }
  }
}
