package net.foxopen.fox.module.mapset;

import net.foxopen.fox.ContextUCon;
import net.foxopen.fox.XFUtil;
import net.foxopen.fox.command.XDoCommandList;
import net.foxopen.fox.database.UCon;
import net.foxopen.fox.database.parser.ParsedStatement;
import net.foxopen.fox.database.parser.StatementParser;
import net.foxopen.fox.database.sql.ScalarResultType;
import net.foxopen.fox.dom.DOM;
import net.foxopen.fox.ex.ExDB;
import net.foxopen.fox.ex.ExDBTooFew;
import net.foxopen.fox.ex.ExInternal;
import net.foxopen.fox.ex.ExModule;
import net.foxopen.fox.module.Mod;
import net.foxopen.fox.module.mapset.MapSetDefinitionFactory.DefinitionBuilder;
import net.foxopen.fox.thread.ActionRequestContext;
import net.foxopen.fox.thread.storage.CacheKey;

/**
 * A MapSet definition stored on the database (typically in the env_mapsets table).
 */
public class DatabaseDefinition
extends MapSetDefinition {

  private final String mTableName;
  private final String mDomain;

  static class Builder
  extends DefinitionBuilder {

    private final String mTableName;
    private final String mDomain;

    Builder(Mod pModule, DOM pDefinitionDOM)
    throws ExModule {
      mDomain = pDefinitionDOM.getAttrOrNull("domain");

      if(XFUtil.isNull(mDomain)) {
        throw new ExModule("Bad definition - missing mandatory 'domain' attribute");
      }

      mTableName = pModule.getApp().getMapSetTableName();
    }

    @Override
    protected CacheKey getAutoCacheKeyOrNull() {
      return CacheKey.createStaticCacheKey();
    }

    @Override
    protected Long getDefaultTimeoutMinsOrNull() {
      return MapSetDefinitionFactory.NEVER_REFRESH_TIMEOUT_MINS;
    }

    @Override
    protected MapSetDefinition buildInternal(String pLocalName, CacheKey pCacheKey, XDoCommandList pXDo, long pRefreshTimeoutMins, Mod pModule) {
      return new DatabaseDefinition(pLocalName, pCacheKey, pXDo, pRefreshTimeoutMins, pModule, mTableName, mDomain);
    }
  }

  private DatabaseDefinition(String pLocalName, CacheKey pCacheKey, XDoCommandList pXDo, long pRefreshTimeoutMins, Mod pModule, String pTableName, String pDomain) {
    super(pLocalName, pCacheKey, pXDo, pRefreshTimeoutMins, pModule);
    mTableName = pTableName;
    mDomain = pDomain;
  }

  @Override
  protected DOM createMapSetDOM(ActionRequestContext pRequestContext, DOM pItemDOM, String pEvaluatedCacheKey, String pUniqueValue) {

    ParsedStatement lParsedStatement = StatementParser.parseSafely("SELECT data FROM " + mTableName + " WHERE domain = :domain", "DatabaseMapSet " + mDomain);

    ContextUCon lContextUCon = pRequestContext.getContextUCon();
    DOM lMapSetDOM;

    UCon lUCon = lContextUCon.getUCon("Select DatabaseMapSet");
    try {
      lMapSetDOM = lUCon.queryScalarResult(lParsedStatement, ScalarResultType.DOM, mDomain);
    }
    catch (ExDBTooFew e) {
      throw new ExInternal("Mapset named '" + mDomain + "' could not be located in table " + mTableName, e);
    }
    catch (ExDB e) {
      throw new ExInternal("Failed to retrieve mapset '" + mDomain + "' from table " + mTableName, e);
    }
    finally {
      lContextUCon.returnUCon(lUCon, "Select DatabaseMapSet");
    }

    return lMapSetDOM;
  }

  /**
   * Overloaded method so all mapset definitions based on the same underlying mapset will be refreshed at the same time.
   * @return Key scoped to underlying database mapset.
   */
  String getDefinitionKey() {
    return mTableName + "/" + mDomain;
  }
}
