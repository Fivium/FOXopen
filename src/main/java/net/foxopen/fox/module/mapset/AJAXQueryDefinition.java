package net.foxopen.fox.module.mapset;

import net.foxopen.fox.XFUtil;
import net.foxopen.fox.command.XDoCommandList;
import net.foxopen.fox.database.sql.bind.BindObject;
import net.foxopen.fox.database.sql.bind.DecoratingBindObjectProvider;
import net.foxopen.fox.database.sql.bind.StringBindObject;
import net.foxopen.fox.dbinterface.InterfaceQuery;
import net.foxopen.fox.dom.DOM;
import net.foxopen.fox.ex.ExModule;
import net.foxopen.fox.module.Mod;
import net.foxopen.fox.module.mapset.MapSetDefinitionFactory.DefinitionBuilder;
import net.foxopen.fox.thread.ActionRequestContext;
import net.foxopen.fox.thread.storage.CacheKey;


public class AJAXQueryDefinition
  extends MapSetDefinition {

  static class Builder
    extends DefinitionBuilder {

    private final String mDBInterfaceName;

    private final String mSearchQueryStatementName;
    private final int mSearchQueryResultLimit;

    private final String mRefQueryStatementName;
    private final String mRefPath;

    private final DOM mDefinitionElement;

    Builder(Mod pModule, DOM pDefinitionElement)
      throws ExModule {

      String lDBInterfaceName = pDefinitionElement.getAttr("interface");
      String lSearchQueryName = pDefinitionElement.getAttr("search-query");
      String lSearchQueryResultLimit = XFUtil.nvl(pDefinitionElement.getAttr("search-query-result-limit"), "100");
      String lRefQueryName = pDefinitionElement.getAttr("ref-query");
      String lRefPath = pDefinitionElement.getAttr("ref-path");

      if(XFUtil.isNull(lDBInterfaceName)) {
        throw new ExModule("Bad definition - missing mandatory 'interface' attribute");
      }
      else if(XFUtil.isNull(lSearchQueryName)) {
        throw new ExModule("Bad definition - missing mandatory 'search-query' attribute");
      }
      else if(XFUtil.isNull(lRefQueryName)) {
        throw new ExModule("Bad definition - missing mandatory 'ref-query' attribute");
      }

      mDBInterfaceName = lDBInterfaceName;
      mSearchQueryStatementName = lSearchQueryName;
      mSearchQueryResultLimit = Integer.parseInt(lSearchQueryResultLimit);
      mRefQueryStatementName = lRefQueryName;
      mRefPath = lRefPath;

      mDefinitionElement = pDefinitionElement;
    }

    @Override
    protected CacheKey getAutoCacheKeyOrNull() {
      return null;
    }

    @Override
    protected Long getDefaultTimeoutMinsOrNull() {
      //Force user to decide
      return null;
    }

    @Override
    protected MapSetDefinition buildInternal(String pLocalName, CacheKey pCacheKey, XDoCommandList pXDo, long pRefreshTimeoutMins, Mod pModule)
      throws ExModule {

      if (MapSetDefinitionFactory.DefinitionTag.AJAX_DEFINITION.toString().equals(mDefinitionElement.getLocalName())) {
        return new AJAXQueryDefinition(pLocalName, pCacheKey, pXDo, pRefreshTimeoutMins, pModule,
          mDBInterfaceName, mSearchQueryStatementName, mSearchQueryResultLimit, mRefQueryStatementName, mRefPath);
      }
      else {
        throw new ExModule("Unknown query type " + mDefinitionElement.getLocalName());
      }
    }
  }

  /** Name of bind automatically supplied to the search query */
  private static final String SEARCH_BIND = ":search_term";
  /** Name of optional column the search query can return */
  private static final String SEARCH_QUERY_REF_COLUMN = "reference";

  /** Name of bind automatically supplied to the ref query */
  private static final String REF_BIND = ":ref";

  private final String mDBInterfaceName;

  private final String mSearchQueryStatementName;
  private final int mSearchQueryResultLimit;

  private final String mRefQueryStatementName;
  private final String mRefPath;

  protected AJAXQueryDefinition(String pLocalName, CacheKey pCacheKey, XDoCommandList pXDo, long pRefreshTimeoutMins, Mod pModule,
                                String pDBInterfaceName, String pSearchQueryStatementName, int pSearchQueryResultLimit, String pRefQueryStatementName, String pRefPath)
    throws ExModule {
    super(pLocalName, pCacheKey, pXDo, pRefreshTimeoutMins, pModule);
    mDBInterfaceName = pDBInterfaceName;

    mSearchQueryStatementName = pSearchQueryStatementName;
    mSearchQueryResultLimit = pSearchQueryResultLimit;

    mRefQueryStatementName = pRefQueryStatementName;
    mRefPath = pRefPath;
  }

  /**
   * Constructs a new mapset DOM according to the definition. This should be in the format /map-set-list/map-set/rec etc.
   *
   * @param pRequestContext    Current RequestContext.
   * @param pItemDOM           The element the MapSet is being constructed for. This may be null depending on the MapSet specificity
   *                           (i.e. if it is dependent on :{itemrec}).
   * @param pEvaluatedCacheKey The cache key that has been evaluated for this mapset.
   * @param pUniqueValue       The value which would have been used for the UNIQUE cache key bind.
   * @return A new DOM containing the evaluated mapset.
   */
  @Override
  protected DOM createMapSetDOM(ActionRequestContext pRequestContext, DOM pItemDOM, String pEvaluatedCacheKey, String pUniqueValue) {
    return createDefaultContainerDOM();
  }

  @Override
  protected MapSet createMapSet(ActionRequestContext pRequestContext, DOM pItemDOM, String pEvaluatedCacheKey, String pUniqueValue) {
    return new JITMapSet(this, pEvaluatedCacheKey);
  }

  public String getRefPath() {
    return mRefPath;
  }

  public InterfaceQuery getSearchQueryStatement(ActionRequestContext pRequestContext) {
    return pRequestContext.resolveInterfaceQuery(mDBInterfaceName, mSearchQueryStatementName);
  }

  public InterfaceQuery getRefQueryStatement(ActionRequestContext pRequestContext) {
    return pRequestContext.resolveInterfaceQuery(mDBInterfaceName, mRefQueryStatementName);
  }

  /**
   * Gets the name of the database interface this definition's queries are stored in.
   * @return Search and ref query db interface name.
   */
  public String getDBInterfaceName() {
    return mDBInterfaceName;
  }

  /**
   * Gets the name of the search query for this AJAX definition. Prefer {@link #getSearchQueryStatement} if a RequestContext
   * is available.
   * @return Search query name.
   */
  public String getSearchQueryStatementName() {
    return mSearchQueryStatementName;
  }

  /**
   * Gets the maximum number of rows which should be retrieved when executing this definition's search query. The
   * user can be notified if this limit is exceeded.
   * @return Maximum results for search query to return.
   */
  public int getSearchQueryResultLimit() {
    return mSearchQueryResultLimit;
  }

  /**
   * Name of optional column the search query can return
   *
   * @return ref column name in the search query
   */
  public static String getSearchQueryRefColumn() {
    return SEARCH_QUERY_REF_COLUMN;
  }

  /**
   * Get a DecoratingBindObjectProvider with the ref bind applied for running the reference query on an AJAXQueryDefinition
   *
   * @param pRef Reference to bind in to return one row from the reference query
   * @return
   */
  public static DecoratingBindObjectProvider getRefBindObjectProvider(String pRef) {
    return new DecoratingBindObjectProvider() {
      @Override
      protected BindObject getBindObjectOrNull(String pBindName, int pIndex) {
        if (REF_BIND.equals(pBindName)) {
          return new StringBindObject(pRef);
        }
        return null;
      }
    };
  }

  /**
   * Get a DecoratingBindObjectProvider with the search bind applied for running the search query on an AJAXQueryDefinition
   *
   * @param pSearchTerm Search term to bind in to the search query
   * @return
   */
  public static DecoratingBindObjectProvider getSearchBindObjectProvider(String pSearchTerm) {
    return new DecoratingBindObjectProvider() {
      @Override
      protected BindObject getBindObjectOrNull(String pBindName, int pIndex) {
        if (SEARCH_BIND.equals(pBindName)) {
          return new StringBindObject(pSearchTerm);
        }
        return null;
      }
    };
  }
}
