package net.foxopen.fox.module.mapset;

import net.foxopen.fox.ContextUElem;
import net.foxopen.fox.command.XDoCommandList;
import net.foxopen.fox.database.UCon;
import net.foxopen.fox.database.sql.ScalarResultDeliverer;
import net.foxopen.fox.database.sql.ScalarResultType;
import net.foxopen.fox.dbinterface.InterfaceStatement;
import net.foxopen.fox.dom.DOM;
import net.foxopen.fox.ex.ExDB;
import net.foxopen.fox.ex.ExInternal;
import net.foxopen.fox.ex.ExModule;
import net.foxopen.fox.module.Mod;
import net.foxopen.fox.thread.ActionRequestContext;
import net.foxopen.fox.thread.storage.CacheKey;

/**
 * A MapSet which is constructed from a database interface query which returns a scalar row containing the MapSet XML.
 */
public class DOMQueryDefinition
extends InterfaceQueryDefinition {

  protected DOMQueryDefinition(String pLocalName, CacheKey pCacheKey, XDoCommandList pXDo, long pRefreshTimeoutMins, Mod pModule, InterfaceStatement pDOMQueryStatement, String pQueryMatchXPath)
  throws ExModule {
    super(pLocalName, pCacheKey, pXDo, pRefreshTimeoutMins, pModule, pDOMQueryStatement, pQueryMatchXPath);
  }

  @Override
  protected DOM createMapSetDOM(ActionRequestContext pRequestContext, DOM pItemDOM, String pEvaluatedCacheKey, String pUniqueValue) {

    ContextUElem lContextUElem = pRequestContext.getContextUElem();
    //Determine match node
    DOM lMatchNode = establishMatchNode(lContextUElem);

    DOM lMapSetDOM;
    UCon lUCon = pRequestContext.getContextUCon().getUCon("Create DOM Query MapSet");
    try {
      ScalarResultDeliverer<DOM> lDeliverer = ScalarResultType.DOM.getResultDeliverer();
      mDOMQueryStatement.executeStatement(pRequestContext, lMatchNode, lUCon, lDeliverer);
      lMapSetDOM = lDeliverer.getResult();
    }
    catch (ExDB e) {
      throw new ExInternal("Error running mapset storage location query", e);
    }
    finally {
      pRequestContext.getContextUCon().returnUCon(lUCon, "Create DOM Query MapSet");
    }

    return lMapSetDOM;
  }
}
