package net.foxopen.fox.thread.persistence;


import net.foxopen.fox.XFUtil;
import net.foxopen.fox.cache.CacheManager;
import net.foxopen.fox.cache.BuiltInCacheDefinition;
import net.foxopen.fox.cache.FoxCache;
import net.foxopen.fox.database.UCon;
import net.foxopen.fox.database.UConBindMap;
import net.foxopen.fox.database.UConStatementResult;
import net.foxopen.fox.database.parser.ParsedStatement;
import net.foxopen.fox.database.parser.StatementParser;
import net.foxopen.fox.dom.DOM;
import net.foxopen.fox.ex.ExDB;
import net.foxopen.fox.ex.ExDBTooFew;
import net.foxopen.fox.ex.ExInternal;
import net.foxopen.fox.sql.SQLManager;
import net.foxopen.fox.thread.RequestContext;
import net.foxopen.fox.track.Track;

import java.util.EnumMap;
import java.util.Iterator;
import java.util.Map;


/**
 * A SharedDOMManager which updates DOMs on the database.
 */
public class DatabaseSharedDOMManager
extends SharedDOMManager {

  private static final String DOM_COLUMN_NAME = "DOM";
  private static final String CHANGE_NUMBER_COLUMN_NAME = "CHANGE_NUMBER";

  private static final String DOM_BIND_NAME = ":" + DOM_COLUMN_NAME;
  private static final String CHANGE_NUMBER_BIND_NAME = ":" + CHANGE_NUMBER_COLUMN_NAME;
  private static final String ID_BIND_NAME = ":id";

  //If further DOM types are required it would be better to encapsulate these maps in strategy objects

  private static final String SESSION_DOM_SELECT_PARSED_STATEMENT =
    "SELECT\n" +
    "  session_dom " + DOM_COLUMN_NAME + "\n" +
    ", session_dom_change_number " + CHANGE_NUMBER_COLUMN_NAME +  "\n" +
    "FROM ${schema.fox}.fox_user_thread_sessions\n" +
    "WHERE id = :1";

  private static final String SESSION_DOM_UPDATE_PARSED_STATEMENT =
    "UPDATE ${schema.fox}.fox_user_thread_sessions\n" +
    "SET\n" +
    "  session_dom = " + DOM_BIND_NAME + "\n" +
    ", session_dom_change_number = " + CHANGE_NUMBER_BIND_NAME +  "\n" +
    "WHERE id = " + ID_BIND_NAME;

  private static final Map<SharedDOMType, ParsedStatement> SELECT_STATEMENT_MAP = new EnumMap<>(SharedDOMType.class);

  private static final Map<SharedDOMType, ParsedStatement> UPDATE_STATEMENT_MAP = new EnumMap<>(SharedDOMType.class);

  static {
    parseQueries();
  }

  public static void parseQueries() {
    String lSessionDOMSelect = SQLManager.replaceSQLSubstitutionVariables(SESSION_DOM_SELECT_PARSED_STATEMENT);
    SELECT_STATEMENT_MAP.put(SharedDOMType.SESSION, StatementParser.parseSafely(lSessionDOMSelect, "Session DOM Select"));

    String lSessionDOMUpdate = SQLManager.replaceSQLSubstitutionVariables(SESSION_DOM_UPDATE_PARSED_STATEMENT);
    UPDATE_STATEMENT_MAP.put(SharedDOMType.SESSION, StatementParser.parseSafely(lSessionDOMUpdate, "Session DOM Update"));
  }

  private static final Iterator<String> gChangeNumberIterator = XFUtil.getUniqueIterator();

  /**
   * Gets a SharedDOMManager from the static cache, creating one if it doesn't exist.
   * @param pDOMType DOMType to get/create a manager for.
   * @param pDOMId ID of DOM.
   * @return New or existing SharedDOMManager.
   */
  public static SharedDOMManager getOrCreateDOMManager(SharedDOMType pDOMType, String pDOMId) {
    FoxCache<String, SharedDOMManager> lFoxCache = CacheManager.getCache(BuiltInCacheDefinition.DATABASE_SHARED_DOMS);
    SharedDOMManager lDOMManager = lFoxCache.get(pDOMType + "/" + pDOMId);
    if(lDOMManager == null) {
      lDOMManager = new DatabaseSharedDOMManager(pDOMId, pDOMType);
      lFoxCache.put(pDOMType + "/" + pDOMId, lDOMManager);
    }

    return lDOMManager;
  }

  private DatabaseSharedDOMManager(String pDOMId, SharedDOMType pDOMType) {
    super(pDOMId, pDOMType);
  }

  @Override
  protected DOMReadResult readDOMOrNull(RequestContext pRequestContext, String pCurrentChangeNumber) {

    UCon lUCon = pRequestContext.getContextUCon().getUCon("Select shared DOM");
    try {
      UConStatementResult lResult = lUCon.querySingleRow(SELECT_STATEMENT_MAP.get(getDOMType()), getDOMId());
      final String lChangeNumber = lResult.getString(CHANGE_NUMBER_COLUMN_NAME);
      if(pCurrentChangeNumber.equals(lChangeNumber)) {
        //Change numbers match - no need to read the DOM
        Track.debug("ChangeNumberMatch", "Not reading DOM as latest version already in memory");
        return null;
      }
      else {
        //Change number mismatch - read the latest DOM
        Track.debug("ChangeNumberMismatch", "Reading latest DOM version into memory");

        //Read the DOM now - before the UCon is returned
        final DOM lDOM = lResult.getDOMFromSQLXML(DOM_COLUMN_NAME);

        return new DOMReadResult() {
          public String getChangeNumber() { return lChangeNumber; }
          public DOM getDOM() { return lDOM; }
        };
      }
    }
    catch (ExDBTooFew e) {
      //Return null if no DOM was persisted
      return null;
    }
    catch (ExDB e) {
      throw new ExInternal("Failed to select " + getDOMType() + " DOM", e);
    }
    finally {
      pRequestContext.getContextUCon().returnUCon(lUCon, "Select shared DOM");
    }
  }

  @Override
  protected String updateDOM(RequestContext pRequestContext, DOM pDOM) {
    //Increment the DOM change number
    String lChangeNumber = gChangeNumberIterator.next();

    UConBindMap lBindMap = new UConBindMap();
    lBindMap.defineBind(DOM_BIND_NAME, pDOM);
    lBindMap.defineBind(CHANGE_NUMBER_BIND_NAME, lChangeNumber);
    lBindMap.defineBind(ID_BIND_NAME, getDOMId());

    UCon lUCon = pRequestContext.getContextUCon().getUCon("Update shared DOM");
    try {
      lUCon.executeAPI(UPDATE_STATEMENT_MAP.get(getDOMType()), lBindMap);
    }
    catch (ExDB e) {
      throw new ExInternal("Failed to update " + getDOMType() + " DOM", e);
    }
    finally {
      pRequestContext.getContextUCon().returnUCon(lUCon, "Update shared DOM");
    }

    return lChangeNumber;
  }

  @Override
  public String toString() {
    return "DatabaseSharedDOMManager DOM Type = " + getDOMType() + ", DOM ID = " + getDOMId();
  }
}
