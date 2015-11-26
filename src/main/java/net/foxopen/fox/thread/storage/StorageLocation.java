package net.foxopen.fox.thread.storage;


import net.foxopen.fox.ContextUElem;
import net.foxopen.fox.command.XDoCommandList;
import net.foxopen.fox.dom.DOM;
import net.foxopen.fox.ex.ExDoSyntax;
import net.foxopen.fox.ex.ExInternal;
import net.foxopen.fox.ex.ExModule;
import net.foxopen.fox.ex.ExTooFew;
import net.foxopen.fox.ex.ExTooMany;
import net.foxopen.fox.module.Mod;
import net.foxopen.fox.module.Validatable;
import net.foxopen.fox.track.Track;
import net.foxopen.fox.track.TrackFlag;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;


/**
 * A StorageLocation provides information about database statements which may be used to perform CRUD operations on
 * arbitrary database records. Database queries are stored as {@link DatabaseStatement}s and are accessed from this class
 * using the {@link #getDatabaseStatement} method. StorageLocations also provide an optional cache key, defined by a module
 * developer, which can be used by consuming code to cache query results.<br/><br/>
 *
 * TODO PN review below doc
 * Note: a StorageLocation without any statements explicitly defined is considered to be a temporary storage location.
 * A temporary SL is automatically populated with CRUD statements referring to the FOX temporary SL table, but these
 * may not be required by a consumer which needs a WorkingStorageLocation. You must ensure you pass the correct parameter
 * to {@link #createWorkingStorageLocation(ContextUElem, String, boolean)} to prevent the created WSL evaluating the binds
 * for these statements.
 */
public abstract class StorageLocation
implements Validatable {

  private static final Pattern FOR_UPDATE_PATTERN = Pattern.compile("FOR[\\s]+UPDATE([\\s]+OF[\\s]+[^\\s]+)?([\\s]+NOWAIT)?", Pattern.MULTILINE | Pattern.CASE_INSENSITIVE);

  private final String mStorageLocationName;
  private final String mAppMnem;
  private final String mModName;

  /** Optional. */
  private final CacheKey mCacheKey;

  private final Map<StatementType, DatabaseStatement> mStatementMap = new HashMap<>();

  private final XDoCommandList mValidationCommands;

  /**
   * Creates a new storage location definition.
   * @param pModule Module where the storage location is defined.
   * @param pStorageLocationDOM Definition XML from module DOM.
   * @param pAutoGenerateCacheKey If true, a cache key is automatically generated for this SL based on the binds of the
   * QUERY statement, regardless of whether a cache key definition was provided by the user. If false, a cache key is
   * only created if specified by the user.
   * @throws ExModule If syntax is invalid.
   */
  protected StorageLocation(Mod pModule, DOM pStorageLocationDOM, boolean pAutoGenerateCacheKey)
  throws ExModule {

    mAppMnem = pModule.getApp().getAppMnem();
    mModName = pModule.getName();

    // Set store location name
    mStorageLocationName = pStorageLocationDOM.getAttr("name");
    if(mStorageLocationName.length()==0) {
      throw new ExModule("Bad storage-location name", pStorageLocationDOM);
    }

    // Check to see if store-location has database clause
    DOM lDatabaseClause = null;
    try {
      lDatabaseClause= pStorageLocationDOM.get1E("fm:database");
    }
    catch (ExTooMany x) {
      throw new ExModule("Bad storage-location database", pStorageLocationDOM, x);
    }
    catch (ExTooFew x) {} // database does not have to be specified

    // Parse statements when database clause specified
    if(lDatabaseClause!=null) {
      populateStatementMap(lDatabaseClause);
    }

    //Determine the cache key for this storage location
    mCacheKey = createCacheKey(pStorageLocationDOM, pAutoGenerateCacheKey);

    DOM lValidateBlock = pStorageLocationDOM.get1EOrNull("fm:validation");
    if(lValidateBlock != null) {
      try {
        mValidationCommands = new XDoCommandList(pModule, lValidateBlock);
      }
      catch (ExDoSyntax e) {
        throw new ExModule("Invalid command syntax in storage-location validation block", e);
      }
    }
    else {
      mValidationCommands = XDoCommandList.emptyCommandList();
    }
  }

  protected StorageLocation(String pName, Map<StatementType, DatabaseStatement> pStatementMap) {

    mStorageLocationName = pName;
    mAppMnem = "";
    mModName = "";
    mCacheKey = null;
    mValidationCommands = XDoCommandList.emptyCommandList();

    mStatementMap.putAll(pStatementMap);
  }

  private void populateStatementMap(DOM lDatabaseClause)
  throws ExModule {

    STMT_LOOP:
    for(DOM lStmtDOM : lDatabaseClause.getChildElements()) {

      // Validate statement element
      String lName = lStmtDOM.getLocalName();
      //Skip deprecated "lock" statements
      if("lock".equals(lName)) {
        Track.alert("LockStatementDeprecated", "Deprecated 'lock' statement found in storage location " + mStorageLocationName, TrackFlag.BAD_MARKUP);
        continue STMT_LOOP;
      }
      StatementType lStatementType = StatementType.valueOf(lName.toUpperCase().replaceAll("-", "_"));

      if(lStatementType == null) {
        throw new ExModule("Unrecognised storage location database statement: "+lName);
      }

      if(mStatementMap.containsKey(lStatementType)) {
        throw new ExModule("Duplicated storage location database statement: "+lStatementType);
      }

      // Process statement
      DatabaseStatement lDatabaseStatement = DatabaseStatement.createFromDOM(lStmtDOM, "fm:sql", lStatementType, mStorageLocationName);
      mStatementMap.put(lStatementType, lDatabaseStatement);

      //For Query type statements we may have an additional read only query defined
      if(lStatementType == StatementType.QUERY) {
        DatabaseStatement lROQueryStatement;
        if(lStmtDOM.get1EOrNull("fm:read-only-sql") != null) {
          lROQueryStatement = DatabaseStatement.createFromDOM(lStmtDOM, "fm:read-only-sql", StatementType.READ_ONLY_QUERY, mStorageLocationName);
        }
        else {
          //If not defined, attempt to automatically generate one
          lROQueryStatement = DatabaseStatement.createInternalStatement(removeForUpdateClause(lStmtDOM.get1SNoEx("fm:sql")), lDatabaseStatement.getBindList(), StatementType.READ_ONLY_QUERY, mStorageLocationName);
        }
        mStatementMap.put(StatementType.READ_ONLY_QUERY, lROQueryStatement);
      }

      // Parse insert statement fm:do (legacy) - throw an error if defined
      if(lStatementType == StatementType.INSERT) {
        DOM lDoDOM = lStmtDOM.get1EOrNull("fm:do");
        if(lDoDOM!=null) {
          throw new ExModule("Invalid syntax; insert fm:do no longer supported in " + mStorageLocationName);
        }
      }
    }
  }

  private CacheKey createCacheKey(DOM pStoreLocationDOM, boolean pAutoGenerateCacheKey)
  throws ExModule {

    CacheKey lCacheKey;
    if(!pAutoGenerateCacheKey) {
      try {
        lCacheKey = CacheKey.createFromDefinitionDOM(pStoreLocationDOM.get1E("fm:cache-key"), false);
      }
      catch (ExTooFew e) {
        lCacheKey = null;
      }
      catch (ExTooMany e) {
        throw new ExModule("Storage location " + mStorageLocationName + " missing cache-key definition", pStoreLocationDOM, e);
      }
      catch (ExModule e) {
        throw new ExModule("Storage location " + mStorageLocationName + " has invalid cache-key definition", pStoreLocationDOM, e);
      }
    }
    else if (mStatementMap.containsKey(StatementType.QUERY)) {
      //Subclass requested an auto generated cache key - NOTE: this should override any explicitly defined cache key.
      //(FOX4 -> FOX5 migration - old FSL cache keys are not fit for purpose)

      //Use the select statement to generate a list of binds for the cache key
      List<StorageLocationBind> lCacheKeyBinds = new ArrayList<>();
      DatabaseStatement lQueryStatement = mStatementMap.get(StatementType.QUERY);
      for (StorageLocationBind lBind : lQueryStatement.getBindList()) {
        if(lBind.getUsingType().isAllowedInCacheKey()) {
          lCacheKeyBinds.add(lBind);
        }
        else {
          //Track the fact that an 'odd' bind is found in the select statement
          Track.debug("AutoCacheKeyBinds", "Found a bind of type " + lBind.getUsingType().toString() + " in query statement for " + mStorageLocationName);
        }
      }
      lCacheKey = CacheKey.createFromBindList(lCacheKeyBinds);
    }
    else {
      throw new ExModule("Cache key could not be determined for " + mStorageLocationName);
    }

    return lCacheKey;
  }

  static final String removeForUpdateClause(String pSQLStatement) {
    return FOR_UPDATE_PATTERN.matcher(pSQLStatement).replaceFirst("");
  }

  public final String getName() {
    return mStorageLocationName;
  }

  /**
   * Evaluates the cache key using the given ContextUElem and unique constant by replacing the bind variable placeholders
   * in the original string with their evaluated values. Also prepends the current app mnemonic, module and storage location name
   * to guarantee that the keys will never clash across different storage locations (i.e. when 2 storage locations use the same
   * cache key string).
   * @param pContextUElem Context for XPath evaluation.
   * @param pUniqueConstant Unique constant for UNIQUE UsingType evaluation.
   * @return The evaluated cache key.
   */
  String evaluateCacheKey(ContextUElem pContextUElem, String pUniqueConstant) {
    //It is possible to have a storage location without a cache key, but consumers should not be attempting to evaluate
    //the cache key for these.
    if(mCacheKey == null) {
      throw new ExInternal("Cannot evaluate cache key for " + mStorageLocationName + " as no definition is available.");
    }

    //Prepend the storage location name to guarantee uniqueness in the case of multiple storage locations with the same user cache-key.
    //(note: this used to be tolerated but is now considered to be a markup bug)
    //Also prepend the app mnem to prevent caching conflicts with different apps.
    String lCacheKeyPrefix =  mAppMnem + "/" + mModName + "/" + mStorageLocationName + "/";

    return mCacheKey.evaluate(lCacheKeyPrefix, pContextUElem, pUniqueConstant);
  }

  /**
   * Retrieves the DatabaseStatement of the given StatementType, or null if it does not exist.
   * @param pType Type of statement to retrieve (insert, update, etc)
   * @return DatabaseStatement or null.
   */
  public DatabaseStatement getDatabaseStatement(StatementType pType) {
    return mStatementMap.get(pType);
  }

  /**
   * Tests if this StorageLocation has a statement of the given type defined on it.
   * @param pType
   * @return True if there is a corresponding DatabaseStatement for the StatementType.
   */
  public boolean hasDatabaseStatement(StatementType pType) {
    return mStatementMap.containsKey(pType);
  }

  /**
   * Tests if this StorageLocation has a query statement defined on it. If not, it should be treated as a temporary
   * storage location and handled appropriately.
   * @return
   */
  public boolean hasQueryStatement() {
    return mStatementMap.containsKey(StatementType.QUERY) || mStatementMap.containsKey(StatementType.READ_ONLY_QUERY);
  }

  protected void addDatabaseStatement(StatementType pType, DatabaseStatement pStatement) {
    mStatementMap.put(pType, pStatement);
  }

  /**
   * Evaluates String bind variables for the DatabaseStatement of the given StatementType, returning a map of bind property
   * tuples to the evaluated values.
   * @param pStatementType
   * @param pContextUElem
   * @param pUniqueValue
   * @return
   */
  public Map<StorageLocationBind, String> evaluateStringBinds(StatementType pStatementType, ContextUElem pContextUElem, String pUniqueValue) {

    DatabaseStatement lDatabaseStatement = mStatementMap.get(pStatementType);
    if (lDatabaseStatement == null) {
      throw new ExInternal("Statement type not defined on this storage location: " + pStatementType);
    }
    else {
      return evaluateStringBindsInternal(lDatabaseStatement.getBindList(), pContextUElem, pUniqueValue);
    }
  }

  private Map<StorageLocationBind, String> evaluateStringBindsInternal(List<StorageLocationBind> pBindList, ContextUElem pContextUElem, String pUniqueValue) {
    //Evaluate XPath, Static and Unique binds in the standard way
    Map<StorageLocationBind, String> lResult = StorageLocationBind.evaluateStringBinds(pBindList, pContextUElem, pUniqueValue);

    //Augment in CACHE_KEY binds for special cases
    for(StorageLocationBind lSLBind : pBindList) {
      if(lSLBind.getUsingType() == UsingType.CACHE_KEY) {
        lResult.put(lSLBind, evaluateCacheKey(pContextUElem, pUniqueValue));
      }
    }

    return lResult;
  }

  public void validate(Mod pModule) {
    mValidationCommands.validate(pModule);
  }

  public String getAppMnem() {
    return mAppMnem;
  }

  public String getModName() {
    return mModName;
  }

  /**
   * Gets the CacheKey definition for this StorageLocation. This may be null if the storage location does not require
   * a cache key definition.
   * @return Cache key.
   */
  public CacheKey getCacheKey() {
    return mCacheKey;
  }

  public XDoCommandList getValidationCommands() {
    return mValidationCommands;
  }
}
