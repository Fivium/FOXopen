/*

Copyright (c) 2010, UK DEPARTMENT OF ENERGY AND CLIMATE CHANGE -
                    ENERGY DEVELOPMENT UNIT (IT UNIT)
All rights reserved.

Redistribution and use in source and binary forms, with or without modification,
are permitted provided that the following conditions are met:

    * Redistributions of source code must retain the above copyright notice,
      this list of conditions and the following disclaimer.
    * Redistributions in binary form must reproduce the above copyright notice,
      this list of conditions and the following disclaimer in the documentation
      and/or other materials provided with the distribution.
    * Neither the name of the DEPARTMENT OF ENERGY AND CLIMATE CHANGE nor the
      names of its contributors may be used to endorse or promote products
      derived from this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR
ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
(INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
(INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

$Id$

*/
package net.foxopen.fox.thread.storage;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.regex.Pattern;

import net.foxopen.fox.ContextUElem;
import net.foxopen.fox.database.UCon;
import net.foxopen.fox.database.sql.ExecutableAPI;
import net.foxopen.fox.database.sql.ExecutableQuery;
import net.foxopen.fox.database.sql.bind.BindDirection;
import net.foxopen.fox.database.sql.bind.BindObject;
import net.foxopen.fox.database.sql.bind.BindObjectProvider;
import net.foxopen.fox.database.sql.bind.BindSQLType;
import net.foxopen.fox.database.sql.bind.StringBindObject;
import net.foxopen.fox.database.storage.WorkDoc;
import net.foxopen.fox.database.storage.dom.XMLWorkDoc;
import net.foxopen.fox.ex.ExInternal;
import net.foxopen.fox.track.Track;


/**
 * An evaluated StorageLocation which contains evaluated bind variables for the string binds of the original StorageLocation's
 * DatabaseStatements. Some consumers may not require bind variables to be evaluated for temporary storage locations as this
 * indicates to the consumer that the WSL does not need to be used. In this case the bind map will be null and this object will
 * not be able to create any ExecutableStatements.<br><br>
 *
 * A WSL only holds the evaluated bind variables for an SL. To actually perform LOB access, you must create a WorkDoc.
 * For File WSLs this is done directly from the WSL. For XML WSLs you should use the static methods on {@link XMLWorkDoc}.
 */
public abstract class WorkingStorageLocation<SL extends StorageLocation> {

  private static final Pattern FOR_UPDATE_PATTERN = Pattern.compile(".*FOR[\\s]+UPDATE.*", Pattern.DOTALL | Pattern.CASE_INSENSITIVE);

  protected final String mStorageLocationName;
  protected final String mAppMnem;
  protected final String mModName;
  private final String mEvaluatedKeyString;
  private final boolean mReadOnly;

  //Binds to evaluated values
  protected Map<StatementType, Map<StorageLocationBind, String>> mStatementTypeToEvaluatedStringBindMap;

  /**
   * Evaluates the given StorageLocation and creates a new WorkingStorageLocation based on it.
   * @param pStorageLocation StorageLocation to evaluate.
   * @param pContextUElem Context for XPath evaluation.
   * @param pUniqueValue Value for "UNIQUE" bind type evaulation.
   * @param pEvaluateBinds If true, statement binds are evaluated. If false nothing is evaluated and this will be WSL
   * which cannot run any statements. It can still be used to determine if the WSL is temporary or read only.
   */
  protected WorkingStorageLocation(StorageLocation pStorageLocation, ContextUElem pContextUElem, String pUniqueValue, boolean pEvaluateBinds, boolean pReadOnly) {

    Track.pushInfo("CreateWSL");
    try {
      mStorageLocationName = pStorageLocation.getName();
      mAppMnem = pStorageLocation.getAppMnem();
      mModName = pStorageLocation.getModName();
      mReadOnly = pReadOnly;

      // Evaluate cache key string if defined (may not be for temp WSLs)
      mEvaluatedKeyString = evaluateCacheKey(pStorageLocation, pContextUElem, pUniqueValue);

      Track.debug("StorageLocationName", mStorageLocationName);
      Track.debug("StorageLocationCacheKey", mEvaluatedKeyString);

      if(pEvaluateBinds) {
        mStatementTypeToEvaluatedStringBindMap = new EnumMap<>(StatementType.class);
        //Establish which statements to evaluate - if this is RO we just need the RO select, otherwise we need select/update/delete etc
        EnumSet<StatementType> lEvalStatementTypes;
        if(mReadOnly) {
          lEvalStatementTypes = EnumSet.of(StatementType.READ_ONLY_QUERY);
        }
        else {
          lEvalStatementTypes = EnumSet.of(StatementType.QUERY, StatementType.UPDATE, StatementType.INSERT, StatementType.API);
        }

        //Evaluate the relevant statements
        for(StatementType lStatementType : lEvalStatementTypes) {
          //If the DatabaseStatement is defined in the SL, evaluate the binds for it.
          if(pStorageLocation.hasDatabaseStatement(lStatementType)) {
            //Evaluate binds
            Map<StorageLocationBind, String> lEvaluatedBinds = pStorageLocation.evaluateStringBinds(lStatementType, pContextUElem, pUniqueValue);

            //Store evaluated bind map against statement type
            mStatementTypeToEvaluatedStringBindMap.put(lStatementType, lEvaluatedBinds);

            //Do some validation to warn developers about dodgy statements
            trackStatementDebugInfo(lStatementType, pStorageLocation);
          }
        }
      }
      else {
        mStatementTypeToEvaluatedStringBindMap = null;
      }
    }
    finally {
      Track.pop("CreateWSL");
    }
  }

  private void trackStatementDebugInfo(StatementType pStatementType, StorageLocation pStoreLocation) {

    if(pStatementType == StatementType.QUERY) {
      String lQueryStatementString = pStoreLocation.getDatabaseStatement(StatementType.QUERY).getParsedStatement().getOriginalStatement().toUpperCase();
      boolean lForUpdateMatches = FOR_UPDATE_PATTERN.matcher(lQueryStatementString).matches();
      int lNoWaitIndex = lQueryStatementString.indexOf("NOWAIT");
      if(!lForUpdateMatches){
        Track.alert("BadQueryStatement", "Query statement for " + mStorageLocationName + " does not contain FOR UPDATE string");
      }
      if(lNoWaitIndex == -1){
        Track.alert("BadQueryStatement", "Query statement for " + mStorageLocationName + "  does not contain NOWAIT string");
      }
    }
    else if(pStatementType == StatementType.READ_ONLY_QUERY) {
      String lQueryStatementString = pStoreLocation.getDatabaseStatement(StatementType.READ_ONLY_QUERY).getParsedStatement().getOriginalStatement();
      //Log the ReadOnly query, which may have been automatically generated, so it is visible to developers
      Track.logDebugText("WSLReadOnlyQuery", lQueryStatementString);

      //Belt and braces check that the FOR UPDATE string doesn't appear in the RO query
      if(FOR_UPDATE_PATTERN.matcher(lQueryStatementString).matches()) {
        Track.alert("BadROQueryStatement", "Read only query statement for " + mStorageLocationName + "  appears to contain FOR UPDATE string");
      }
    }
  }

  /**
   * Gets the evaluated cache key appended with the SQL strings of the database statements used by this storage location.
   * Used for caching in an environment where SQL strings are likely to be frequently modified (i.e. in development), avoiding
   * the need to flush WorkDoc seperately to flushing application modules. This is experimental and may need to be removed
   * if it causes cache bloat.
   * @return Evaluated cache key which is dependent on the underlying storage location's definition.
   */
  public String getDefinitionDependentCacheKey() {

    StringBuilder lBuilder = new StringBuilder();
    lBuilder.append("Key: ");
    lBuilder.append(mEvaluatedKeyString);

    for(StatementType lStatementType : EnumSet.of(getQueryStatementType(), StatementType.UPDATE, StatementType.INSERT)) {
      DatabaseStatement lDBStatement = getStorageLocation().getDatabaseStatement(lStatementType);
      if(lDBStatement != null) {
        lBuilder.append(lStatementType + ": ");
        lBuilder.append(lDBStatement.getParsedStatement().getOriginalStatement());
      }
    }

    return lBuilder.toString();
  }

  protected String evaluateCacheKey(StorageLocation pStorageLocation, ContextUElem pContextUElem, String pUniqueValue) {
    if(pStorageLocation.getCacheKey() != null) {
      //VERY IMPORTANT: append the RO flag so WorkDoc can differentiate between RO and non RO WSLs for the same SL
      return getStorageLocation().evaluateCacheKey(pContextUElem, pUniqueValue) + (mReadOnly ? " /ReadOnly" : "");
    }
    else {
      return "";
    }
  }

  /**
   * Gets the evaluated cache key of this WSL. This is composed of the SL identity (app/mod/name), any key specified by
   * the module developer, and the RO-ness of the WSL.
   * @return
   */
  public String getCacheKey() {
    return mEvaluatedKeyString;
  }

  /**
   * Gets the original StoreLocation for this WorkingStoreLocation, or null if it cannot be resolved (i.e. if this is
   * an ad-hoc WorkingStoreLocation)
   * @return The original StoreLocation, or null.
   */
  public abstract SL getStorageLocation();

  /**
   * Gets the BindObject provider for this WSL. Overloaded by upload subclass to additionally provide file upload metadata
   * bind type.
   * @param pDatabaseStatement
   * @param pStatementType
   * @param pOptionalWorkDoc
   * @return
   */
  protected BindObjectProvider createBindObjectProvider(DatabaseStatement pDatabaseStatement, StatementType pStatementType, WorkDoc pOptionalWorkDoc) {
    return new WSLBindObjectProvider(pDatabaseStatement, pStatementType, pOptionalWorkDoc);
  }

  private void checkBindsEvaluated() {
    if(mStatementTypeToEvaluatedStringBindMap == null) {
      throw new ExInternal("Cannot generate an ExecutableStatement for WSL " + mStorageLocationName + " as no binds have been evaluated");
    }
  }

  protected ExecutableAPI getExecutableAPIOrNull(WorkDoc pWorkDoc, StatementType pStatementType) {
    checkBindsEvaluated();
    DatabaseStatement lDBStatement = getStorageLocation().getDatabaseStatement(pStatementType);
    if(lDBStatement == null) {
      return null;
    }

    BindObjectProvider lBindProvider = createBindObjectProvider(lDBStatement, pStatementType, pWorkDoc);
    return lDBStatement.getParsedStatement().createExecutableAPI(lBindProvider);
  }

  /**
   * Gets an ExecutableAPI representing this WSL's UPDATE statement. This will be null if it was not defined in the original
   * StorageLocation definition, or if this WSL is read only.
   * @param pWorkDoc Provider for the WSL DATA LOB bind.
   * @return Update statement or null.
   */
  public ExecutableAPI getExecutableUpdateStatementOrNull(WorkDoc pWorkDoc) {
    return getExecutableAPIOrNull(pWorkDoc, StatementType.UPDATE);
  }

  /**
   * Gets an ExecutableAPI representing this WSL's INSERT statement. This will be null if it was not defined in the original
   * StorageLocation definition, or if this WSL is read only.
   * @param pWorkDoc Provider for the WSL DATA LOB bind.
   * @return Insert statement or null.
   */
  public ExecutableAPI getExecutableInsertStatementOrNull(WorkDoc pWorkDoc) {
    return getExecutableAPIOrNull(pWorkDoc, StatementType.INSERT);
  }

  /**
   * Gets the ExecutableQuery representing this WSL's SELECT statement. For read only WSLs this will be the "read only"
   * version with any FOR UPDATE clause removed
   * @return Select statement.
   */
  public ExecutableQuery getExecutableSelectStatement() {
    checkBindsEvaluated();

    //Look up the DatabaseStatement which contains the underlying ParsedStatement
    DatabaseStatement lDBSelectStatement = getStorageLocation().getDatabaseStatement(getQueryStatementType());
    if(lDBSelectStatement == null) {
      throw new ExInternal("Failed to retrieve " + (mReadOnly ? "read-only" : "") + " select query from storage location " + mStorageLocationName);
    }

    //Create a bind provider for populating the executable query with binds
    BindObjectProvider lBindProvider = createBindObjectProvider(lDBSelectStatement, getQueryStatementType(), null);
    return lDBSelectStatement.getParsedStatement().createExecutableQuery(lBindProvider);
  }

  private StatementType getQueryStatementType() {
    return mReadOnly ? StatementType.READ_ONLY_QUERY : StatementType.QUERY;
  }

  public String getStorageLocationName() {
    return mStorageLocationName;
  }

  /**
   * Tests if this WSL is read only. If true, changes made to the data LOB should <b>not</b> be written back to the database,
   * and the update/insert statements should never be executed.
   * @return True if the data LOB for this WSL should not be written back to the database.
   */
  public final boolean isReadOnly() {
    return mReadOnly;
  }

  /**
   * Bind provider for the WorkingStorageLocation, used for creating ExecutableStatements. Provides String or data (LOB)
   * bind types as required.
   */
  protected class WSLBindObjectProvider
  implements BindObjectProvider {

    protected DatabaseStatement mDatabaseStatement;
    protected StatementType mStatementType;
    protected WorkDoc mWorkDoc;

    public WSLBindObjectProvider(DatabaseStatement pDatabaseStatement, StatementType pStatementType, WorkDoc pWorkDoc) {
      mDatabaseStatement = pDatabaseStatement;
      mStatementType = pStatementType;
      mWorkDoc = pWorkDoc;
    }

    @Override
    public boolean isNamedProvider() {
      return mDatabaseStatement.hasNamedBinds();
    }

    @Override
    public BindObject getBindObject(String pBindName, int pIndex) {
      //Get the bind definition (StoreLocationBind) for the given name/index from the database statement
      StorageLocationBind lBind = mDatabaseStatement.getBind(pBindName, pIndex);
      if(lBind.getUsingType().isString()) {
        //If the bind is a string, get the evaluated value from the map
        //Null pointers here could be caused by SL definition being changed (i.e. binds added/removed) after WSL was created
        String lEvaluatedBindString = mStatementTypeToEvaluatedStringBindMap.get(mStatementType).get(lBind);
        return new StringBindObject(lEvaluatedBindString, BindDirection.IN);
      }
      else if(lBind.getUsingType().isData()) {
        //Otherwise, bind WorkDoc data
        return new WSLWorkDocBindObject(mWorkDoc, lBind.getUsingType());
      }
      else {
        throw new ExInternal("Don't know how to bind a " + lBind.getUsingType() + " at index " + pIndex);
      }
    }
  }

  /**
   * BindObject for storage location data (i.e. a DOM/LOB).
   */
  private class WSLWorkDocBindObject
  implements BindObject {

    private final WorkDoc mWorkDoc;
    private final UsingType mUsingType;

    private WSLWorkDocBindObject(WorkDoc pWorkDoc, UsingType pUsingType) {
      mWorkDoc = pWorkDoc;
      mUsingType = pUsingType;
    }

    @Override
    public Object getObject(UCon pUCon) {
      if(mWorkDoc == null) {
        throw new ExInternal("WorkDoc would have been null");
      }
      return mWorkDoc.getLOBForBinding(pUCon, mUsingType.getSQLType());
    }

    @Override
    public String getObjectDebugString() {
      return "[" + mUsingType.toString() + "]";
    }

    @Override
    public BindSQLType getSQLType() {
      return mUsingType.getSQLType();
    }

    @Override
    public BindDirection getDirection() {
      return BindDirection.IN;
    }
  }
}
