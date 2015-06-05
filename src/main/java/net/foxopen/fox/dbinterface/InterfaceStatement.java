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
import net.foxopen.fox.FoxGbl;
import net.foxopen.fox.XFUtil;
import net.foxopen.fox.database.UCon;
import net.foxopen.fox.database.UConStatementResult;
import net.foxopen.fox.database.parser.ParsedStatement;
import net.foxopen.fox.database.parser.StatementParser;
import net.foxopen.fox.database.sql.ExecutableStatement;
import net.foxopen.fox.database.sql.ResultDeliverer;
import net.foxopen.fox.database.sql.bind.BindDirection;
import net.foxopen.fox.database.sql.bind.BindObject;
import net.foxopen.fox.database.sql.bind.BindObjectProvider;
import net.foxopen.fox.database.sql.bind.BindSQLType;
import net.foxopen.fox.database.sql.bind.ClobStringBindObject;
import net.foxopen.fox.database.sql.bind.DOMBindObject;
import net.foxopen.fox.database.sql.bind.DOMListBindObject;
import net.foxopen.fox.database.sql.bind.DecoratingBindObjectProvider;
import net.foxopen.fox.database.sql.bind.PreEvaluatedBindObjectProvider;
import net.foxopen.fox.database.sql.bind.StringBindObject;
import net.foxopen.fox.database.sql.bind.TimestampBindObject;
import net.foxopen.fox.dom.DOM;
import net.foxopen.fox.dom.DOMList;
import net.foxopen.fox.dom.xpath.XPathResult;
import net.foxopen.fox.ex.ExActionFailed;
import net.foxopen.fox.ex.ExCardinality;
import net.foxopen.fox.ex.ExDB;
import net.foxopen.fox.ex.ExDOMName;
import net.foxopen.fox.ex.ExInternal;
import net.foxopen.fox.ex.ExModule;
import net.foxopen.fox.ex.ExParser;
import net.foxopen.fox.module.Mod;
import net.foxopen.fox.module.datanode.NodeInfo;
import net.foxopen.fox.sql.SQLManager;
import net.foxopen.fox.thread.devtoolbar.DevToolbarContext;
import net.foxopen.fox.thread.ActionRequestContext;
import net.foxopen.fox.track.Track;

import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
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

  // The name of the statement
  private final String mStatementName;
  // The name of the parent db-interface
  private final String mDBInterfaceName;
  // Mod/app tuple which this statement is defined on
  private final String mModuleName;
  private final String mAppMnem;

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

    //Parse the statement
    try {
      mParsedStatement = StatementParser.parse(lStatementString, getQualifiedName(), pReplaceBindNames);
    }
    catch (ExParser e) {
      throw new ExModule("Failed to parse statement " + mStatementName, e);
    }

    //Process binds
    mNameToUsingParamMap = parseParameterList(pDbStatementXML.getUL("fm:using"), BindDirection.IN, false);

    //loop through bind definitions and check they're defined in the query (binds defined in query but not here will be picked up by binding process)
    Set<String> lQueryBindNames = new HashSet<>(mParsedStatement.getBindNameList());
    for (InterfaceParameter lParam : mNameToUsingParamMap.values()) {
      //Prefix the bind colon as internal bind param names don't have one
      if(!lQueryBindNames.contains(":" + lParam.getParamName())) {
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
        String lDOMDataType = lParamDOM.getAttrOrNull("datadom-type");
        String lRelativeXpath = lParamDOM.getAttrOrNull("datadom-location");
        String lDirection = lParamDOM.getAttrOrNull("direction");
        String lDOMMergeMode = lParamDOM.getAttrOrNull("dom-merge-mode");

        // If datadom-location is not specified, fallback to inspecting the text node
        // contained within the bind element (as per legacy db-interface behaviour)
        if (XFUtil.isNull(lRelativeXpath)) {
          lRelativeXpath = lParamDOM.value();
        }

        InterfaceParameter lParam;
        try {
          lParam = InterfaceParameter.create(lBindName, lSqlDataType, lDOMDataType, lDirection, lRelativeXpath, lDOMMergeMode, pDefaultDirection);
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
    executeStatement(pRequestContext, pMatchedNode, pUCon, null, pDeliverer);
  }

  /**
   * Executes this InterfaceStatement for the given match node, then delivers the results using the given ResultDeliverer.
   * @param pRequestContext Current request context.
   * @param pMatchedNode Relative node of the statement.
   * @param pUCon Connection for statement execution.
   * @param pOptionalBindProvider External bind provider for augmenting the default query binding mechanism. Can be null.
   * @param pDeliverer Destination for statement results.
   * @throws ExDB If statement execution or delivery fails.
   */
  public final void executeStatement(ActionRequestContext pRequestContext, DOM pMatchedNode, UCon pUCon, DecoratingBindObjectProvider pOptionalBindProvider,
                                     ResultDeliverer<? extends ExecutableStatement> pDeliverer)
  throws ExDB {

    boolean lEnableDBMSOutput = pRequestContext.getDevToolbarContext().isFlagOn(DevToolbarContext.Flag.DBMS_OUTPUT);
    if(lEnableDBMSOutput) {
      pUCon.executeAPI(ENABLE_DBMS_OUTPUT_STATEMENT);
    }

    try {
      //Create a bind provider for this statement and decorate using the external provider if necessary
      BindObjectProvider lBindProvider = new StatementBindProvider(pMatchedNode, pRequestContext.getContextUElem());
      if(pOptionalBindProvider != null) {
        lBindProvider = pOptionalBindProvider.decorate(lBindProvider);
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
   * Pre-evaluates the binds for this interface statement and returns a {@link PreEvaluatedBindObjectProvider} containing them,
   * which can be used to execute this query at a later stage. The pre-evaluation process executes all bind variable XPaths
   * and stores the typed results (i.e. String objects for string binds, DOM objects for XML binds). Care must be taken if
   * the binds are cached for a long time as they may hold live references to DOM objects.
   *
   * @param pRequestContext For XPath evaluation.
   * @param pMatchNode Match node of the query
   * @return BindObjectProvider containing binds which can be used to execute this query's ParsedStatement at a later stage.
   */
  public BindObjectProvider preEvaluateBinds(ActionRequestContext pRequestContext, DOM pMatchNode) {
    return PreEvaluatedBindObjectProvider.preEvaluateBinds(mNameToUsingParamMap.keySet(), new StatementBindProvider(pMatchNode, pRequestContext.getContextUElem()));
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

  /**
   * Bind provider logic for an InterfaceStatement.
   */
  protected class StatementBindProvider
  implements BindObjectProvider {

    private final DOM mRelativeNode;
    private final ContextUElem mContextUElem;

    protected StatementBindProvider(DOM pRelativeNode, ContextUElem pContextUElem) {
      mRelativeNode = pRelativeNode;
      mContextUElem = pContextUElem;
    }

    @Override
    public boolean isNamedProvider() {
      return true;
    }

    @Override
    public BindObject getBindObject(String pBindName, int pIndex) {

      //Resolve the parameter being bound
      InterfaceParameter lParam = getParamForBindNameOrNull(pBindName);
      if(lParam == null) {
        throw new ExInternal("No bind definition found for parameter " + pBindName);
      }

      //Run XPath to select bind node(s) or bind string
      try {
        //The path may not resolve a node (could be the result of an XPath function). Get the raw result and see if it's a valid DOMList or not.
        XPathResult lXPathResult = mContextUElem.extendedXPathResult(mRelativeNode, lParam.getRelativeXPath());
        if(lXPathResult.isValidDOMList()) {
          //XPath returned a node or node list; defer to DOM binding logic
          return bindNodes(lXPathResult.asDOMList(), pBindName, lParam);
        }
        else {
          //XPath does not target any nodes; use string binding logic
          return bindString(lXPathResult.asString(), pBindName, lParam);
        }
      }
      catch (ExActionFailed e) {
        throw new ExInternal("Failed to process bind '" + pBindName + "' in statement " +  getQualifiedName(), e);
      }
    }

    /**
     * Constructs a BindObject for binding a DOMList of 0 or more nodes, based on the established bind type for the parameter.
     * @param pNodesToBind
     * @param pBindName
     * @param pParam
     * @return
     */
    private BindObject bindNodes(DOMList pNodesToBind, String pBindName, InterfaceParameter pParam) {

      //Attempt to get a nodeinfo if we're binding a single node - this might be used to determine the bind sql type if the developer didn't declare it in the query definition
      NodeInfo lNodeInfo = null;
      if(pNodesToBind.size() == 1) {
        //Exactly one node found - attempt to get a node info for it
        lNodeInfo = Mod.getModuleForAppMnemAndModuleName(mAppMnem, mModuleName).getNodeInfo(pNodesToBind.get(0));
      }
      else if(pParam.getBindDirection().isOutBind() || pNodesToBind.size() == 0) {
        //If this is an out bind there might not be a target node yet - work out what the absoulte path WOULD be and use that to get the node info
        //Likewise an in bind might be null but we still need to know what node info it WOULD have had for consistency when the query is run with an actual bind value

        String lTargetNodeAbsolutePath;
        try {
          lTargetNodeAbsolutePath = mContextUElem.getAbsolutePathForCreateableXPath(mRelativeNode, pParam.getRelativeXPath());
        }
        catch (ExActionFailed | ExCardinality | ExDOMName e) {
          //Ignore errors here; they can be dealth with by the deliverer if they're actually a problem
          lTargetNodeAbsolutePath = null;
        }

        //A path could be determined - look up the NodeInfo
        if(lTargetNodeAbsolutePath != null) {
          lNodeInfo = Mod.getModuleForAppMnemAndModuleName(mAppMnem, mModuleName).getNodeInfo(lTargetNodeAbsolutePath);
        }
      }

      //Get the bind type for this parameter
      BindSQLType lParamBindSQLType = pParam.getBindSQLType(lNodeInfo);

      if(pParam.getBindDirection() == BindDirection.OUT) {
        //If this is outbound only, we don't need to bind an object in
        return createEmptyBindObjectForOutBind(lParamBindSQLType);
      }
      else {
        //Create a bind object based on the target SQL type
        switch(lParamBindSQLType) {
          case XML:
            return createDOMBindObject(pNodesToBind, pParam);
          case STRING:
            return createCharacterBindObject(pNodesToBind, pParam, false);
          case CLOB:
            return createCharacterBindObject(pNodesToBind, pParam, true);
          case TIMESTAMP:
            return createTimestampBindObject(pNodesToBind, pParam);
          default:
            throw new ExInternal("Failed to process bind '" + pBindName + "' in statement " +  getQualifiedName() + ": cannot bind to SQL Type " + lParamBindSQLType);
        }
      }
    }

    /**
     * Constructs a BindObject for binding a String value, based on the established bind type for the parameter.
     * @param pStringToBind
     * @param pBindName
     * @param pParam
     * @return
     */
    private BindObject bindString(String pStringToBind, String pBindName, InterfaceParameter pParam) {

      //Do some basic validation here - can't bind out to a none-node path
      if(pParam.getBindDirection().isOutBind()) {
        throw new ExInternal("Cannot create bind object for parameter " + pBindName + " - OUT or IN_OUT binds must resolve a node");
      }

      //We have no NodeInfo to use to get the SQL Type, but we can still read off the parameter definition
      BindSQLType lParamBindSQLType = pParam.getBindSQLType(null);

      switch(lParamBindSQLType) {
        case STRING:
          return new StringBindObject(pStringToBind, pParam.getBindDirection());
        case CLOB:
          return new ClobStringBindObject(pStringToBind, pParam.getBindDirection());
        case TIMESTAMP:
          return createTimestampBindObject(pStringToBind, pParam);
        case XML: //Don't allow non-node results to be bound as XML - makes no sense
        default:
          throw new ExInternal("Failed to process bind '" + pBindName + "' in statement " +  getQualifiedName() + ": cannot bind to SQL Type " + lParamBindSQLType);
      }
    }
  }

  /**
   * Creates an OUT-only BindObject for the given type.
   * @param pBindType
   * @return
   */
  private static BindObject createEmptyBindObjectForOutBind(BindSQLType pBindType) {
    switch(pBindType) {
      case XML:
        return new DOMBindObject(null, BindDirection.OUT);
      case STRING:
        return new StringBindObject(null, BindDirection.OUT);
      case CLOB:
        return new ClobStringBindObject(null, BindDirection.OUT);
      case TIMESTAMP:
        return new TimestampBindObject(null, BindDirection.OUT);
      default:
        throw new ExInternal("Don't know how to make an empty out bind object for " + pBindType);
    }
  }

  /**
   * Creates a DOM bind object based on the selected nodes and parameter definition. If the parameter definition requests
   * a string, the text nodes are extracted from the node list. Otherwise the nodes themselves are bound. For empty lists
   * a dummy DOM element is generated to circumvent the issues which arise from having null XMLType binds.
   * @param pSelectedNodes
   * @param pParam
   * @return
   */
  private static BindObject createDOMBindObject(DOMList pSelectedNodes, InterfaceParameter pParam) {
    //As an XMLType has been bound, assume the datadom type is DOM if not specified
    DOMDataType lDOMDataType = XFUtil.nvl(pParam.getDOMDataType(), DOMDataType.DOM);

    DOMList lSelectedNodes;
    if(lDOMDataType == DOMDataType.STRING) {
      //TODO PN is logic ok?
      lSelectedNodes = pSelectedNodes.allChildTextNodesAsDOMList(false);
    }
    else if (lDOMDataType == DOMDataType.DOM) {
      lSelectedNodes = pSelectedNodes;
    }
    else {
      //Default action - throw exception to satisfy compiler
      throw new ExInternal("Can't bind a " + lDOMDataType + " to an xmltype");
    }

    if(lSelectedNodes.size() == 0) {
      //TODO PN assert that this is OK
      Track.alert("NullXMLTypeBind", "XMLType bind was null for parameter " + pParam.getParamName() + " - binding workaround value instead");
      return new DOMBindObject(DOM.createDocument("FOX_NULL_XML_BIND_WORKAROUND"), pParam.getBindDirection());
    }
    else if(lSelectedNodes.size() == 1 && lSelectedNodes.get(0).isElement()) {
      //XPath selected a single element - create a simple DOMBindObject
      return new DOMBindObject(lSelectedNodes.get(0), pParam.getBindDirection());
    }
    else {
      //Multiple nodes or a single non-element node - use DOMListBindObject to handle binding fragments
      return new DOMListBindObject(lSelectedNodes, pParam.getBindDirection());
    }
  }

  /**
   * Generates a string value for the given nodes, based on the parameter definition. If the parameter has a DOMDataType
   * of DOM, the nodes are serialised to an XML string. Otherwise the shallow value is extracted from the first node. If more
   * than one node is in the list, only the first node is used (with a warning logged to the dev toolbar). Empty nodelists return an empty string.
   * @param pSelectedNodes
   * @param pParam
   * @return
   */
  private static String getBindString(DOMList pSelectedNodes, InterfaceParameter pParam) {

    String lBindString;
    if(pParam.getDOMDataType() == DOMDataType.DOM) {
      //If datadom-type = dom, get the XML string value of the selected node(s)
      lBindString = pSelectedNodes.outputNodesToString();
    }
    else {
      //If datadom-type == xs:string/xs:date/xs:dateTime, then bind node contents

      if(pSelectedNodes.size() > 1) {
        //Warn developer about multiple nodes (legacy beahviour was to pick the first node)
        Track.alert("MultiNodeBind", "Query param " + pParam.getParamName() + " resolved " + pSelectedNodes.size() + " nodes - only first node will be used");
      }

      if(pSelectedNodes.size() == 0) {
        //No nodes selected, bind empty string (null)
        lBindString = "";
      }
      else {
        //SHALLOW node value - shallow is important; avoids getting sub-node text like fox-errors etc
        lBindString = pSelectedNodes.get(0).value();
      }
    }

    return lBindString;
  }

  /**
   * Creates a string or CLOB bind object for binding a string. The shallow value of the first node in the nodelist is used.
   * @param pSelectedNodes
   * @param pParam
   * @param pIsClob
   * @return
   */
  private static BindObject createCharacterBindObject(DOMList pSelectedNodes, InterfaceParameter pParam, boolean pIsClob) {

    String lBindString = getBindString(pSelectedNodes, pParam);

    if(pIsClob) {
      return new ClobStringBindObject(lBindString, pParam.getBindDirection());
    }
    else {
      return new StringBindObject(lBindString, pParam.getBindDirection());
    }
  }

  private static BindObject createTimestampBindObject(DOMList pSelectedNodes, InterfaceParameter pParam) {
    //Don't allow DOMs to be bound as a date - makes no sense
    if(pParam.getDOMDataType() == DOMDataType.DOM) {
      throw new ExInternal("Cannot bind DOM for bind " + pParam.getParamName() + " as a date");
    }

    String lBindString = getBindString(pSelectedNodes, pParam).trim();

    return createTimestampBindObject(lBindString, pParam);
  }

  /**
   * Creates a timestamp bind object for the given string, which should be the string representation of either an xs:date
   * or an xs:datetime. The length of the string is used to determine the format mask which should be applied to it.
   * @param pBindString
   * @param pParam
   * @return
   */
  private static BindObject createTimestampBindObject(String pBindString, InterfaceParameter pParam) {

    DateFormat lDateFormat = null;
    try {
      //Deal with nulls
      if(XFUtil.isNull(pBindString)) {
        return new TimestampBindObject(null, pParam.getBindDirection());
      }

      if (pBindString.length() > 11) {
        // It must have a time component so select the appropriate format
        lDateFormat = new SimpleDateFormat(FoxGbl.FOX_JAVA_DATE_TIME_FORMAT);
      }
      else {
        // its a date with no time component
        lDateFormat = new SimpleDateFormat(FoxGbl.FOX_DATE_FORMAT);
      }
      Date lDate = lDateFormat.parse(pBindString);
      Timestamp lTimeStamp = new Timestamp(lDate.getTime());
      return new TimestampBindObject(lTimeStamp, pParam.getBindDirection());
    }
    catch (ParseException e) {
      throw new ExInternal("Cannot convert FOX date to SQL timestamp: " + pBindString, e);
    }
  }
}
