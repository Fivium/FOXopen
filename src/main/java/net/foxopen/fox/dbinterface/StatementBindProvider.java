package net.foxopen.fox.dbinterface;

import net.foxopen.fox.ContextUElem;
import net.foxopen.fox.FoxGbl;
import net.foxopen.fox.XFUtil;
import net.foxopen.fox.database.sql.bind.BindDirection;
import net.foxopen.fox.database.sql.bind.BindObject;
import net.foxopen.fox.database.sql.bind.BindObjectBuilder;
import net.foxopen.fox.database.sql.bind.BindSQLType;
import net.foxopen.fox.database.sql.bind.CachedBindObjectProvider;
import net.foxopen.fox.database.sql.bind.ClobStringBindObject;
import net.foxopen.fox.database.sql.bind.DOMBindObject;
import net.foxopen.fox.database.sql.bind.DOMListBindObject;
import net.foxopen.fox.database.sql.bind.StringBindObject;
import net.foxopen.fox.database.sql.bind.TimestampBindObject;
import net.foxopen.fox.database.sql.bind.template.TemplateVariableConverter;
import net.foxopen.fox.database.sql.bind.template.TemplateVariableObjectProvider;
import net.foxopen.fox.dom.DOM;
import net.foxopen.fox.dom.DOMList;
import net.foxopen.fox.dom.xpath.XPathResult;
import net.foxopen.fox.ex.ExActionFailed;
import net.foxopen.fox.ex.ExCardinality;
import net.foxopen.fox.ex.ExDOMName;
import net.foxopen.fox.ex.ExInternal;
import net.foxopen.fox.module.NodeInfoProvider;
import net.foxopen.fox.module.datanode.NodeInfo;
import net.foxopen.fox.track.Track;

import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Bind provider logic for an InterfaceStatement. This is capable of producing a CachedBindObjectProvider so binds can be
 * stored externally. In caching mode, bound DOM objects are cloned, which may result in a performance hit.
 */
class StatementBindProvider
implements TemplateVariableObjectProvider {

  /**
   * A method which can provide InterfaceParameter definitions to this BindProvider, typically from the InterfaceStatement
   * definition.
   */
  @FunctionalInterface
  interface InterfaceParameterProvider {
    InterfaceParameter getParamForBindNameOrNull(String pBindName);
  }

  private final DOM mRelativeNode;
  private final ContextUElem mContextUElem;
  private final InterfaceParameterProvider mInterfaceParameterProvider;
  private final NodeInfoProvider mNodeInfoProvider;
  private final String mStatementQualifiedName;
  private final TemplateVariableConverter mTemplateVariableConverter;
  private final boolean mCacheBinds;
  private final StatementBindObjectFactory mStatementBindObjectFactory;

  StatementBindProvider(DOM pRelativeNode, ContextUElem pContextUElem, InterfaceParameterProvider pInterfaceParameterProvider,
                        NodeInfoProvider pNodeInfoProvider, String pStatementQualifiedName, TemplateVariableConverter pTemplateVariableConverter,
                        boolean pCacheBinds) {
    mRelativeNode = pRelativeNode;
    mContextUElem = pContextUElem;
    mInterfaceParameterProvider = pInterfaceParameterProvider;
    mNodeInfoProvider = pNodeInfoProvider;
    mStatementQualifiedName = pStatementQualifiedName;
    mTemplateVariableConverter = pTemplateVariableConverter;
    mCacheBinds = pCacheBinds;
    mStatementBindObjectFactory = pCacheBinds ? new BuilderCachingBindObjectFactory() : new BasicBindObjectFactory();
  }

  @Override
  public boolean isNamedProvider() {
    return true;
  }

  @Override
  public BindObject getBindObject(String pBindName, int pIndex) {

    //Resolve the parameter being bound
    InterfaceParameter lParam = mInterfaceParameterProvider.getParamForBindNameOrNull(pBindName);
    if(lParam == null) {
      throw new ExInternal("No bind definition found for parameter " + pBindName);
    }

    //Run XPath to select bind node(s) or bind string
    XPathResult lXPathResult = getXPathResult(lParam);

    //The path may not resolve a node (could be the result of an XPath function). Get the raw result and see if it's a valid DOMList or not.
    if(lXPathResult.isValidDOMList()) {
      //XPath returned a node or node list; defer to DOM binding logic
      return bindNodes(lXPathResult.asDOMList(), pBindName, lParam);
    }
    else {
      //XPath does not target any nodes; use string binding logic
      return bindString(lXPathResult.asString(), pBindName, lParam);
    }
  }

  @Override
  public boolean isTemplateVariableDefined(String pVariableName) {
    return mInterfaceParameterProvider.getParamForBindNameOrNull(pVariableName) != null;
  }

  @Override
  public Object getObjectForTemplateVariable(String pVariableName) {
    return mStatementBindObjectFactory.createTemplateVariable(pVariableName);
  }

  private Object getObjectForTemplateVariableInternal(String pVariableName) {
    InterfaceParameter lParam = mInterfaceParameterProvider.getParamForBindNameOrNull(pVariableName);
    if(lParam == null) {
      return null;
    }
    else {
      return mTemplateVariableConverter.convertVariableObject(pVariableName, getXPathResult(lParam));
    }
  }

  private XPathResult getXPathResult(InterfaceParameter lParam) {
    try {
      return mContextUElem.extendedXPathResult(mRelativeNode, lParam.getRelativeXPath());
    }
    catch (ExActionFailed e) {
      throw new ExInternal("Failed to process bind '" + lParam.getParamName() + "' in statement " +  mStatementQualifiedName, e);
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
      lNodeInfo = mNodeInfoProvider.getNodeInfo(pNodesToBind.get(0));
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
        lNodeInfo = mNodeInfoProvider.getNodeInfo(lTargetNodeAbsolutePath);
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
          throw new ExInternal("Failed to process bind '" + pBindName + "' in statement " +  mStatementQualifiedName + ": cannot bind to SQL Type " + lParamBindSQLType);
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
      case CLOB:
        return mStatementBindObjectFactory.createStringBindObject(pStringToBind, pParam, lParamBindSQLType == BindSQLType.CLOB);
      case TIMESTAMP:
        return createTimestampBindObject(pStringToBind, pParam);
      case XML: //Don't allow non-node results to be bound as XML - makes no sense
      default:
        throw new ExInternal("Failed to process bind '" + pBindName + "' in statement " +  mStatementQualifiedName + ": cannot bind to SQL Type " + lParamBindSQLType);
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
  private BindObject createDOMBindObject(DOMList pSelectedNodes, InterfaceParameter pParam) {
    //As an XMLType has been bound, assume the datadom type is DOM if not specified
    DOMDataType lDOMDataType = XFUtil.nvl(pParam.getDOMDataType(), DOMDataType.DOM);

    DOMList lSelectedNodes;
    if(lDOMDataType == DOMDataType.STRING) {
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
      Track.alert("NullXMLTypeBind", "XMLType bind was null for parameter " + pParam.getParamName() + " - binding workaround value instead");
      return mStatementBindObjectFactory.createDOMBindObject(DOM.createDocument("FOX_NULL_XML_BIND_WORKAROUND"), pParam);
    }
    else if(lSelectedNodes.size() == 1 && lSelectedNodes.get(0).isElement()) {
      //XPath selected a single element - create a simple DOMBindObject
      return mStatementBindObjectFactory.createDOMBindObject(lSelectedNodes.get(0), pParam);
    }
    else {
      //Multiple nodes or a single non-element node - use DOMListBindObject to handle binding fragments
      return mStatementBindObjectFactory.createDOMListBindObject(lSelectedNodes, pParam);
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
  private BindObject createCharacterBindObject(DOMList pSelectedNodes, InterfaceParameter pParam, boolean pIsClob) {

    String lBindString = getBindString(pSelectedNodes, pParam);
    return mStatementBindObjectFactory.createStringBindObject(lBindString, pParam, pIsClob);
  }

  private BindObject createTimestampBindObject(DOMList pSelectedNodes, InterfaceParameter pParam) {
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
  private BindObject createTimestampBindObject(String pBindString, InterfaceParameter pParam) {

    DateFormat lDateFormat;
    try {
      //Deal with nulls
      if(XFUtil.isNull(pBindString)) {
        return mStatementBindObjectFactory.createTimestampBindObject(null, pParam);
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
      return mStatementBindObjectFactory.createTimestampBindObject(lTimeStamp, pParam);
    }
    catch (ParseException e) {
      throw new ExInternal("Cannot convert FOX date to SQL timestamp: " + pBindString, e);
    }
  }

  /**
   * Creates a CachedBindObjectProvider containing BindObjectBuilders and template variables resolved by this provider.
   * This must be called after all bind variables for the associated statement have been requested from this provider.
   * If the provider was instantiated with the cached binds argument set to false, this method throws an exception.
   * @return CachedBindObjectProvider containing evaluated binds and template variables.
   */
  CachedBindObjectProvider createCachedBindObjectProvider() {
    if(!mCacheBinds) {
      throw new ExInternal("This StatementBindProvider has not cached any binds");
    }

    return new StatementCachedBindProvider(new HashMap<>(mStatementBindObjectFactory.getBindObjectBuilderMap()), new HashMap<>(mStatementBindObjectFactory.getVariableObjectMap()));
  }

  /**
   * Internal interface which will either directly created BindObjects or create and cache BindObjectBuilders for later
   * retrieval as a CachedBindObjectProvider. Also caches template variables in the same way.
   */
  private interface StatementBindObjectFactory {

    BindObject createStringBindObject(String pString, InterfaceParameter pParam, boolean pBindAsClob);

    BindObject createTimestampBindObject(Timestamp pTimestamp, InterfaceParameter pParam);

    BindObject createDOMBindObject(DOM pDOM, InterfaceParameter pParam);

    BindObject createDOMListBindObject(DOMList pDOMList, InterfaceParameter pParam);

    Object createTemplateVariable(String pVariableName);

    Map<String, BindObjectBuilder> getBindObjectBuilderMap();

    Map<String, Object> getVariableObjectMap();
  }

  /**
   * Factory which creates BindObjects and does no caching.
   */
  private class BasicBindObjectFactory implements StatementBindObjectFactory {

    @Override
    public BindObject createStringBindObject(String pString, InterfaceParameter pParam, boolean pBindAsClob) {
      return pBindAsClob ? new ClobStringBindObject(pString) : new StringBindObject(pString, pParam.getBindDirection());
    }

    @Override
    public BindObject createTimestampBindObject(Timestamp pTimestamp, InterfaceParameter pParam) {
      return new TimestampBindObject(pTimestamp, pParam.getBindDirection());
    }

    @Override
    public BindObject createDOMBindObject(DOM pDOM, InterfaceParameter pParam) {
      return new DOMBindObject(pDOM, pParam.getBindDirection());
    }

    @Override
    public BindObject createDOMListBindObject(DOMList pDOMList, InterfaceParameter pParam) {
      return new DOMListBindObject(pDOMList, pParam.getBindDirection());
    }

    @Override
    public Object createTemplateVariable(String pVariableName) {
      return getObjectForTemplateVariableInternal(pVariableName);
    }

    @Override
    public Map<String, BindObjectBuilder> getBindObjectBuilderMap() {
      return Collections.emptyMap();
    }

    @Override
    public Map<String, Object> getVariableObjectMap() {
      return Collections.emptyMap();
    }
  }

  /**
   * Factory which creates and caches BindObjectBuilders, using the builders to also instantiate BindObjects.
   */
  private class BuilderCachingBindObjectFactory implements StatementBindObjectFactory {

    private final Map<String, BindObjectBuilder> mBuilderMap = new HashMap<>();
    private final Map<String, Object> mTemplateVariableMap = new HashMap<>();

    private BindObject addToMapAndBuild(BindObjectBuilder<?> pNewBuilder, InterfaceParameter pParam) {
      mBuilderMap.put(pParam.getParamName(), pNewBuilder);
      return pNewBuilder.build();
    }

    @Override
    public BindObject createStringBindObject(String pString, InterfaceParameter pParam, boolean pBindAsClob) {
      return addToMapAndBuild(pBindAsClob ? new ClobStringBindObject.Builder(pString, pParam.getBindDirection()) : new StringBindObject.Builder(pString, pParam.getBindDirection()), pParam);
    }

    @Override
    public BindObject createTimestampBindObject(Timestamp pTimestamp, InterfaceParameter pParam) {
      return addToMapAndBuild(new TimestampBindObject.Builder(pTimestamp, pParam.getBindDirection()), pParam);
    }

    @Override
    public BindObject createDOMBindObject(DOM pDOM, InterfaceParameter pParam) {
      return addToMapAndBuild(new DOMBindObject.Builder(pDOM.createDocument(), pParam.getBindDirection()), pParam);
    }

    @Override
    public BindObject createDOMListBindObject(DOMList pDOMList, InterfaceParameter pParam) {
      return addToMapAndBuild(new DOMListBindObject.Builder(pDOMList.cloneDocuments(), pParam.getBindDirection()), pParam);
    }

    @Override
    public Object createTemplateVariable(String pVariableName) {
      Object lVariableObject = getObjectForTemplateVariableInternal(pVariableName);
      mTemplateVariableMap.put(pVariableName, lVariableObject);
      return lVariableObject;
    }

    @Override
    public Map<String, BindObjectBuilder> getBindObjectBuilderMap() {
      return mBuilderMap;
    }

    @Override
    public Map<String, Object> getVariableObjectMap() {
      return mTemplateVariableMap;
    }
  }
}
