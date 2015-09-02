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
package net.foxopen.fox.dbinterface;

import net.foxopen.fox.ContextUElem;
import net.foxopen.fox.XFUtil;
import net.foxopen.fox.database.UCon;
import net.foxopen.fox.database.UConStatementResult;
import net.foxopen.fox.database.parser.ParsedStatement;
import net.foxopen.fox.database.parser.StatementParser;
import net.foxopen.fox.database.sql.ExecutableStatement;
import net.foxopen.fox.database.sql.ResultDeliverer;
import net.foxopen.fox.database.sql.bind.BindDirection;
import net.foxopen.fox.database.sql.bind.BindObjectProvider;
import net.foxopen.fox.database.sql.bind.DecoratingBindObjectProvider;
import net.foxopen.fox.database.sql.bind.CachedBindObjectProvider;
import net.foxopen.fox.database.sql.bind.template.MustacheVariableConverter;
import net.foxopen.fox.dom.DOM;
import net.foxopen.fox.dom.DOMList;
import net.foxopen.fox.ex.ExBadPath;
import net.foxopen.fox.ex.ExCardinality;
import net.foxopen.fox.ex.ExDB;
import net.foxopen.fox.ex.ExInternal;
import net.foxopen.fox.ex.ExModule;
import net.foxopen.fox.ex.ExParser;
import net.foxopen.fox.module.Mod;
import net.foxopen.fox.module.ModuleProxyNodeInfoProvider;
import net.foxopen.fox.sql.SQLManager;
import net.foxopen.fox.thread.ActionRequestContext;
import net.foxopen.fox.thread.devtoolbar.DevToolbarContext;
import net.foxopen.fox.track.Track;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;


/**
 * A parsed statement (query or API) defined within a database interface.
 */
public abstract class InterfaceStatement {

  private static final ParsedStatement ENABLE_DBMS_OUTPUT_STATEMENT = StatementParser.parseSafely("BEGIN dbms_output.enable(null); END;", "DBMS Output Enable");
  private static final ParsedStatement DISABLE_DBMS_OUTPUT_STATEMENT = StatementParser.parseSafely("BEGIN dbms_output.disable(); END;", "DBMS Output Disable");

  private static final String READ_DBMS_OUTPUT_FILENAME = "ReadDBMSOutput.sql";
  private static final String TEMPLATE_TYPE_NONE = "none";
  private static final String TEMPLATE_TYPE_MUSTACHE = "mustache";

  // The name of the statement
  private final String mStatementName;
  // The name of the parent db-interface
  private final String mDBInterfaceName;
  // Mod/app tuple which this statement is defined on
  private final String mModuleName;
  private final String mAppMnem;

  private final String mTemplateType;

  private final ParsedStatement mParsedStatement;

  /** Map of bind names to their fm:using definitions. Unnamed binds are assigned ordinal numbers as names. */
  private final Map<String, InterfaceParameter> mNameToUsingParamMap;

  protected InterfaceStatement(DOM pDbStatementXML, String pDbInterfaceName, String pStatementContainerElementName, boolean pReplaceBindNames, Mod pMod)
  throws ExModule {

    mDBInterfaceName = pDbInterfaceName;
    mModuleName = pMod.getName();
    mAppMnem = pMod.getApp().getMnemonicName();

    // Get Statement Name
    mStatementName = pDbStatementXML.getAttr("name");
    if(XFUtil.isNull(mStatementName)) {
      throw new ExModule("Missing name attribute for query/API in db-interface " + pDbInterfaceName);
    }

    // Get RAW statement from definition xml
    String lStatementString;
    try {
      lStatementString = pDbStatementXML.get1S(pStatementContainerElementName);
    }
    catch (ExCardinality e) {
      throw new ExModule("Error getting statement SQL text", e);
    }

    mTemplateType = XFUtil.nvl(pDbStatementXML.getAttr("template-type"), TEMPLATE_TYPE_NONE);
    if( !TEMPLATE_TYPE_NONE.equals(mTemplateType) && !TEMPLATE_TYPE_MUSTACHE.equals(mTemplateType)) {
      throw new ExModule("Unrecognised template type: " + mTemplateType);
    }

    //Parse the statement
    try {
      mParsedStatement = StatementParser.parse(lStatementString, getQualifiedName(), pReplaceBindNames, TEMPLATE_TYPE_MUSTACHE.equals(mTemplateType));
    }
    catch (ExParser e) {
      throw new ExModule("Failed to parse statement " + mStatementName, e);
    }

    //Process binds
    try {
      mNameToUsingParamMap = parseParameterList(pDbStatementXML.xpathUL("fm:using | fm:template-variable"), BindDirection.IN, false);
    }
    catch (ExBadPath e) {
      throw new ExInternal("Failed to parse parameter list", e);
    }

    //loop through bind definitions and check they're defined in the query (binds defined in query but not here will be picked up by binding process)
    //Ignore template only binds as we don't know if they are used
    Set<String> lQueryBindNames = new HashSet<>(mParsedStatement.getBindNameList());
    for (InterfaceParameter lParam : mNameToUsingParamMap.values()) {
      //Prefix the bind colon as internal bind param names don't have one
      if(!lQueryBindNames.contains(":" + lParam.getParamName()) && !lParam.isTemplateOnlyParameter()) {
        throw new ExModule("Bad statement definition for " + mStatementName + ": bind parameter " + lParam.getParamName() + " is not used in the SQL statement");
      }
    }
  }

  /**
   * Gets the full name (interface name concatenated with statement name) of this statement, for debug purposes.
   * @return
   */
  public String getQualifiedName() {
    return  mDBInterfaceName + "/" + mStatementName;
  }

  protected Map<String, InterfaceParameter> parseParameterList(DOMList pParameterXML, BindDirection pDefaultDirection, boolean pNamesRequired)
  throws ExModule {

    // Check Fox binds match all statement binds found above and insert additional data
    Map<String, InterfaceParameter> lResultMap = new HashMap<>(pParameterXML.size());

    for (int i = 0; i < pParameterXML.getLength(); i++) {

      DOM lParamDOM = pParameterXML.item(i);

      String lBindName = lParamDOM.getAttrOrNull("name");
      //new behaviour introduces default name based on index
      if(XFUtil.isNull(lBindName)) {
        if(!pNamesRequired) {
          lBindName = Integer.toString(i + 1);
        }
        else {
          throw new ExModule(lParamDOM.getName() + " requires name attribute and none specified for parameter at index " + i);
        }
      }

      //Ensure bind name does not have leading colon
      lBindName = lBindName.replace(":", "").trim();

      if (lResultMap.containsKey(lBindName)) {
        throw new ExModule("Bind variable " + lBindName + " defined multiple times in " + getQualifiedName());
      }
      else {
        String lSqlDataType = lParamDOM.getAttrOrNull("sql-type");
        String lDOMDataTypeAttr = lParamDOM.getAttrOrNull("datadom-type");
        String lRelativeXPath = lParamDOM.getAttrOrNull("datadom-location");
        String lDirection = lParamDOM.getAttrOrNull("direction");
        String lDOMMergeMode = lParamDOM.getAttrOrNull("dom-merge-mode");

        // If datadom-location is not specified, fallback to inspecting the text node
        // contained within the bind element (as per legacy db-interface behaviour)
        if (XFUtil.isNull(lRelativeXPath)) {
          lRelativeXPath = lParamDOM.value();
        }

        //If a DOM type was specified, look it up in the map (otherwise keep it null and let the binding process work it out later)
        DOMDataType lDOMDataType = null;
        if (!XFUtil.isNull(lDOMDataTypeAttr)) {
          lDOMDataType = DOMDataType.fromExternalString(lDOMDataTypeAttr);
          //Check external string was specified correctly
          if (lDOMDataType == null) {
            throw new ExModule("Unknown DOM data type '" + lDOMDataTypeAttr + "' in bind " + lBindName);
          }
        }

        InterfaceParameter lParam;
        try {
          //Determine type of parameter to create
          if("template-variable".equals(lParamDOM.getLocalName())) {
            //Only create template variable params if this is a templated query
            if(!TEMPLATE_TYPE_NONE.equals(mTemplateType)) {
              lParam = TemplateVariableInterfaceParameter.create(lBindName, lRelativeXPath, lDOMDataType);
            }
            else {
              throw new ExModule("fm:template-variable binds should only be specified on queries where a template-type is specified");
            }
          }
          else {
            lParam = BindVariableInterfaceParameter.create(lBindName, lSqlDataType, lDOMDataType, lDirection, lRelativeXPath, lDOMMergeMode, pDefaultDirection);
          }
        }
        catch (ExModule e) {
          throw new ExModule("Failed to parse parameter definition in " + getQualifiedName(), e);
        }

        lResultMap.put(lBindName, lParam);
      }
    }

    return Collections.unmodifiableMap(lResultMap);
  }

  /**
   * Gets the InterfaceParameter definition corresponding to the given external bind name for this statement, or null
   * if no parameter of that name exists.
   * @param pExternalParamName External parameter name (potentially with the ":" prefix for binds).
   * @return
   */
  public InterfaceParameter getParamForBindNameOrNull(String pExternalParamName) {
    return mNameToUsingParamMap.get(pExternalParamName.replaceFirst(":", ""));
  }

  /**
   * Gets all the InterfaceParameters defined for this statement. No order is guaranteed.
   * @return
   */
  public Collection<InterfaceParameter> getAllInterfaceParameters() {
    return mNameToUsingParamMap.values();
  }

  /**
   * Returns the statement name as a String.
   * @return statement name
   */
  public String getStatementName() {
    return mStatementName;
  }

  /**
   * Returns the parent DBInterface name as a String.
   * @return db interface name
   */
  public String getDBInterfaceName() {
    return mDBInterfaceName;
  }

  public ParsedStatement getParsedStatement() {
    return mParsedStatement;
  }

  /**
   * Gets this statement's map of parameter names to using parameters.
   * @return
   */
  protected final Map<String, InterfaceParameter> getUsingParamMap() {
    return mNameToUsingParamMap;
  }

  /**
   * Executes this InterfaceStatement for the given match node, then delivers the results using the given ResultDeliverer.
   * @param pRequestContext Current request context.
   * @param pMatchedNode Relative node of the statement.
   * @param pUCon Connection for statement execution.
   * @param pDeliverer Destination for statement results.
   * @throws ExDB If statement execution or delivery fails.
   */
  public final void executeStatement(ActionRequestContext pRequestContext, DOM pMatchedNode, UCon pUCon, ResultDeliverer<? extends ExecutableStatement> pDeliverer)
  throws ExDB {
    executeStatement(pRequestContext, pMatchedNode, pUCon, new StatementExecutionBindOptions(){}, pDeliverer);
  }

  /**
   * Executes this InterfaceStatement for the given match node, then delivers the results using the given ResultDeliverer.
   * @param pRequestContext Current request context.
   * @param pMatchedNode Relative node of the statement.
   * @param pUCon Connection for statement execution.
   * @param pOptionalBindProvider External bind provider for augmenting the default query binding mechanism. Can be null.
   * @param pDeliverer Destination for statement results.
   * @throws ExDB If statement execution or delivery fails.
   * @return TODO
   */
  public final StatementExecutionResult executeStatement(ActionRequestContext pRequestContext, DOM pMatchedNode, UCon pUCon, DecoratingBindObjectProvider pOptionalBindProvider,
                                                         ResultDeliverer<? extends ExecutableStatement> pDeliverer)
  throws ExDB {
    StatementExecutionBindOptions lBindOptions = new StatementExecutionBindOptions() {
      @Override public DecoratingBindObjectProvider getDecoratingBindObjectProvider() { return pOptionalBindProvider; }
    };

    return executeStatement(pRequestContext, pMatchedNode, pUCon, lBindOptions, pDeliverer);
  }

  /**
   * Executes this InterfaceStatement for the given match node, then delivers the results using the given ResultDeliverer.
   * @param pRequestContext Current request context.
   * @param pMatchedNode Relative node of the statement.
   * @param pUCon Connection for statement execution.
   * @param pBindOptions Parameter object to modify how binds are cached or decorated.
   * @param pDeliverer Destination for statement results.
   * @throws ExDB If statement execution or delivery fails.
   * @return StatementExecutionResult which may contain cached bind variables, based on the BindOptions parameter.
   */
  public final StatementExecutionResult executeStatement(ActionRequestContext pRequestContext, DOM pMatchedNode, UCon pUCon, StatementExecutionBindOptions pBindOptions,
                                                         ResultDeliverer<? extends ExecutableStatement> pDeliverer)
  throws ExDB {

    //Validate params
    if(pBindOptions.cacheBinds() && pBindOptions.getCachedBindObjectProvider() != null) {
      throw new ExInternal("Cannot cache binds if cached binds are already provided for query " + getQualifiedName());
    }

    //Enable DBMS_OUTPUT if it's on in the dev toolbar
    boolean lEnableDBMSOutput = pRequestContext.getDevToolbarContext().isFlagOn(DevToolbarContext.Flag.DBMS_OUTPUT);
    if(lEnableDBMSOutput) {
      pUCon.executeAPI(ENABLE_DBMS_OUTPUT_STATEMENT);
    }

    StatementBindProvider lOriginalStatementBindProvider = null;
    try {
      //If the consumer has cached binds, use them, otherwise create a bind provider for this statement
      BindObjectProvider lBindProvider = pBindOptions.getCachedBindObjectProvider();
      if(lBindProvider == null) {
        //We may need the provider to store the binds as it is executed if the consumer wants to receive the cached binds
        lOriginalStatementBindProvider = createBindProvider(pMatchedNode, pRequestContext.getContextUElem(), pBindOptions.cacheBinds());
        lBindProvider = lOriginalStatementBindProvider;
      }

      //Decorate the default bind provider if the consumer provided a decorator
      DecoratingBindObjectProvider lDecoratingProvider = pBindOptions.getDecoratingBindObjectProvider();
      if(lDecoratingProvider != null) {
        lBindProvider = lDecoratingProvider.decorate(lBindProvider);
      }

      //Run the statement
      executeStatementInternal(pUCon, lBindProvider, pDeliverer);

      //Read DBMS_OUTPUT if necessary
      if(lEnableDBMSOutput) {
        UConStatementResult lReadResult = pUCon.executeAPI(SQLManager.instance().getStatement(READ_DBMS_OUTPUT_FILENAME, getClass()), UCon.bindOutClob());
        String lOutputClobValue = lReadResult.getStringFromClob(":1");
        if(lOutputClobValue.length() > 0) {
          //If DBMS_OUTPUT produced anything, store the string as an XDoResult
          DBMSOutputResult lDBMSOutputResult = new DBMSOutputResult(getQualifiedName(), pMatchedNode.getRef(), lOutputClobValue);
          pRequestContext.addXDoResult(lDBMSOutputResult);
        }
      }

      //If the consumer asked for the cached binds to be returned, retrieve them from the BindProvider
      if(lOriginalStatementBindProvider != null && pBindOptions.cacheBinds()) {
        return StatementExecutionResult.createCachedBindResult(lOriginalStatementBindProvider.createCachedBindObjectProvider());
      }
      else {
        return StatementExecutionResult.defaultEmptyResult();
      }
    }
    finally {
      try {
        if(lEnableDBMSOutput) {
          //Disable DBMS_OUTPUT until it's needed again
          pUCon.executeAPI(DISABLE_DBMS_OUTPUT_STATEMENT);
        }
      }
      catch (Throwable th) {
        Track.recordSuppressedException("Suppressed exception caused by DBMS_OUTPUT disable", th);
      }
    }
  }

  /**
   * Pre-evaluates the binds for this interface statement and returns a {@link CachedBindObjectProvider} containing them,
   * which can be used to execute this query at a later stage. The pre-evaluation process executes all bind variable XPaths
   * and stores the typed results (i.e. String objects for string binds, DOM objects for XML binds). Bound XML values are
   * duplicated to prevent memory leaks.
   *
   * @param pRequestContext For XPath evaluation.
   * @param pMatchNode Match node of the query
   * @return CachedBindObjectProvider which can be used to execute this query at a later stage. It is safe to serialise the provider.
   */
  public CachedBindObjectProvider preEvaluateBinds(ActionRequestContext pRequestContext, DOM pMatchNode) {

    //Create a BindProvider which records all the variables it is asked for and can create a CachedBindObjectProvider
    StatementBindProvider lStatementBindProvider = createBindProvider(pMatchNode, pRequestContext.getContextUElem(), true);

    //Evalauate standard bind variables
    for (String lBindName : mNameToUsingParamMap.keySet()) {
      lStatementBindProvider.getBindObject(lBindName, -1);
    }

    //Evaluate template variables
    mParsedStatement.getAllTemplateVariableNames().forEach(lStatementBindProvider::getObjectForTemplateVariable);

    return lStatementBindProvider.createCachedBindObjectProvider();
  }

  /**
   * Executes this InterfaceStatement for the given match node, then delivers the results using the given ResultDeliverer.
   * @param pUCon Connection for statement execution.
   * @param pBindProvider Bind provider (should be either from the statement directly or from the statement and decorated).
   * @param pDeliverer Destination for statement results.
   * @throws ExDB If statement execution or delivery fails.
   */
  protected abstract void executeStatementInternal(UCon pUCon, BindObjectProvider pBindProvider, ResultDeliverer<? extends ExecutableStatement> pDeliverer)
  throws ExDB;

  protected StatementBindProvider createBindProvider(DOM pRelativeNode, ContextUElem pContextUElem, boolean pCacheBinds) {
    //Log debug message so it's obvious in track that binds have been cached
    if(pCacheBinds) {
      Track.info("CacheBindsEnabled", "Query execution of " + getQualifiedName() + " will create cacheable bind provider");
    }

    return new StatementBindProvider(pRelativeNode, pContextUElem, this::getParamForBindNameOrNull, ModuleProxyNodeInfoProvider.create(mAppMnem, mModuleName),
                                     getQualifiedName(), MustacheVariableConverter.INSTANCE, pCacheBinds);
  }
}
