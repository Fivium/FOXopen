package net.foxopen.fox.module.mapset;

import net.foxopen.fox.ContextUElem;
import net.foxopen.fox.XFUtil;
import net.foxopen.fox.command.XDoCommandList;
import net.foxopen.fox.database.sql.bind.BindSQLType;
import net.foxopen.fox.dbinterface.InterfaceParameter;
import net.foxopen.fox.dbinterface.InterfaceStatement;
import net.foxopen.fox.dom.DOM;
import net.foxopen.fox.ex.ExActionFailed;
import net.foxopen.fox.ex.ExCardinality;
import net.foxopen.fox.ex.ExInternal;
import net.foxopen.fox.ex.ExModule;
import net.foxopen.fox.module.Mod;
import net.foxopen.fox.module.mapset.MapSetDefinitionFactory.DefinitionBuilder;
import net.foxopen.fox.thread.storage.CacheKey;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;


/**
 * Encapsulation of shared functionality between DOMQueryDefinitions and RecordQueryDefinitions.
 */
public abstract class InterfaceQueryDefinition
extends MapSetDefinition {

  static class Builder
  extends DefinitionBuilder {

    private final InterfaceStatement mQueryStatement;
    private final String mQueryMatchXPath;
    private final DOM mDefinitionElement;

    Builder(Mod pModule, DOM pDefinitionElement)
    throws ExModule {

      String lDBInterfaceName = pDefinitionElement.getAttr("interface");
      String lQueryName = pDefinitionElement.getAttr("query");

      if(XFUtil.isNull(lDBInterfaceName)) {
        throw new ExModule("Bad definition - missing mandatory 'interface' attribute");
      }
      else if(XFUtil.isNull(lQueryName)) {
        throw new ExModule("Bad definition - missing mandatory 'query' attribute");
      }

      mQueryStatement = pModule.getDatabaseInterface(lDBInterfaceName).getInterfaceQuery(lQueryName);
      mQueryMatchXPath = pDefinitionElement.getAttr("match");

      mDefinitionElement = pDefinitionElement;
    }

    @Override
    protected CacheKey getAutoCacheKeyOrNull() {
      //Only allow auto cache key generation if no match is specified
      //(match implies query binds are relative to an arbitrary node, which won't be generally known about when resolving the mapset)
      if(XFUtil.isNull(mQueryMatchXPath)) {
        return createAutoCacheKey(mQueryStatement);
      }
      else {
        throw new ExInternal("Bad definition for query-based mapset - if a 'match' attribute is specified a cache key definition must be explicitly provided");
      }
    }

    @Override
    protected Long getDefaultTimeoutMinsOrNull() {
      //Force user to decide
      return null;
    }

    @Override
    protected MapSetDefinition buildInternal(String pLocalName, CacheKey pCacheKey, XDoCommandList pXDo, long pRefreshTimeoutMins, Mod pModule)
    throws ExModule {

      if (MapSetDefinitionFactory.DefinitionTag.DOM_QUERY.toString().equals(mDefinitionElement.getLocalName())) {
        return new DOMQueryDefinition(pLocalName, pCacheKey, pXDo, pRefreshTimeoutMins, pModule, mQueryStatement, mQueryMatchXPath);
      }
      else if (MapSetDefinitionFactory.DefinitionTag.RECORD_QUERY.toString().equals(mDefinitionElement.getLocalName())) {
        return new RecordQueryDefinition(pLocalName, pCacheKey, pXDo, pRefreshTimeoutMins, pModule, mQueryStatement, mQueryMatchXPath,
                                         mDefinitionElement.getAttr(RecordQueryDefinition.KEY_COLUMN_ATTR), mDefinitionElement.getAttr(RecordQueryDefinition.DATA_COLUMN_ATTR));
      }
      else {
        throw new ExModule("Unknown query type " + mDefinitionElement.getLocalName());
      }
    }
  }

  protected final InterfaceStatement mDOMQueryStatement;
  private final String mQueryMatchXPath;

  protected InterfaceQueryDefinition(String pLocalName, CacheKey pCacheKey, XDoCommandList pXDo, long pRefreshTimeoutMins, Mod pModule, InterfaceStatement pDOMQueryStatement, String pQueryMatchXPath )
  throws ExModule {
    super(pLocalName, pCacheKey, pXDo, pRefreshTimeoutMins, pModule);
    mDOMQueryStatement = pDOMQueryStatement;
    mQueryMatchXPath = pQueryMatchXPath;
  }

  protected DOM establishMatchNode(ContextUElem pContextUElem) {
    DOM lMatchNode;
    if(!XFUtil.isNull(mQueryMatchXPath)) {
      try {
        lMatchNode = pContextUElem.extendedXPath1E(mQueryMatchXPath);
      }
      catch (ExActionFailed | ExCardinality e) {
        throw new ExInternal("Failed to run match XPath for query based mapset", e);
      }
    }
    else {
      //Default to state attach if not specified - this is so any binds from the query which go into the cache key are always evaluated from state attach
      //(at both cache key evaluate time and query run time)
      lMatchNode = pContextUElem.attachDOM();
    }

    return lMatchNode;
  }


  /**
   * Constructs an auto cache key based on the target query's bind variables.
   * @param pDOMQueryStatement
   * @return
   */
  private static CacheKey createAutoCacheKey(InterfaceStatement pDOMQueryStatement) {

    Collection<InterfaceParameter> lInterfaceParams = pDOMQueryStatement.getAllInterfaceParameters();
    List<String> lBindVariableUsingXPaths = new ArrayList<>(lInterfaceParams.size());
    for(InterfaceParameter lParam : lInterfaceParams) {
      //Check here that the query does not contain a DOM bind
      //This is not airtight as a query does not need this markup on it (bind types can be dynamically established at run time) but it'll do
      if(lParam.getBindSQLType(null) == BindSQLType.XML) {
        throw new ExInternal("Cannot create an auto cache key for a query with a DOM bind - specify an overridden cache key");
      }

      lBindVariableUsingXPaths.add(lParam.getRelativeXPath());
    }

    return CacheKey.createFromXPathList(lBindVariableUsingXPaths);
  }
}
