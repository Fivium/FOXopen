package net.foxopen.fox.dbinterface;

import net.foxopen.fox.ContextUElem;
import net.foxopen.fox.XFUtil;
import net.foxopen.fox.command.XDoCommandList;
import net.foxopen.fox.database.UCon;
import net.foxopen.fox.database.sql.ExecutableQuery;
import net.foxopen.fox.database.sql.ExecutableStatement;
import net.foxopen.fox.database.sql.ResultDeliverer;
import net.foxopen.fox.database.sql.bind.BindDirection;
import net.foxopen.fox.database.sql.bind.BindObjectProvider;
import net.foxopen.fox.dom.DOM;
import net.foxopen.fox.dom.DOMList;
import net.foxopen.fox.ex.ExDB;
import net.foxopen.fox.ex.ExDoSyntax;
import net.foxopen.fox.ex.ExModule;
import net.foxopen.fox.module.Mod;
import net.foxopen.fox.track.Track;
import net.foxopen.fox.track.TrackFlag;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;


/**
 * Representation of a query definition within a database interface definition.
 */
public class InterfaceQuery
extends InterfaceStatement {

  static final String DBINT_ELEMENT_NAME = "query";

  // The target path of the statement
  private final String mTargetPath;

  private final String mPaginationDefinitionName;

  private final Map<String, InterfaceParameter> mIntoParams;

  /** Can be null if not on definition. */
  private final QueryMode mQueryMode;

  /** Can be null if list not defined. */
  private final List<String> mKeyElementNames;

  /** Can be null if list not defined. */
  private final XDoCommandList mForEachRowCommandList;

  /** Can be null if list not defined. */
  private final TopNPaginationConfig mTopNPaginationConfig;

  public InterfaceQuery(DOM pDbStatementXML, String pDbInterfaceName, Mod pMod)
  throws ExModule {

    //Note: bind names are NOT rewritten for queries - see note on StatementParser class JavaDoc
    super(pDbStatementXML, pDbInterfaceName, "fm:select", false, pMod);

    //Target path
    String lTargetPath =  pDbStatementXML.xpath1SNoEx("fm:target-path/@match");
    if(XFUtil.isNull(lTargetPath)) {
      mTargetPath = ".";
    }
    else {
      mTargetPath = lTargetPath;
    }

    //Target paths should be relative to the query's match node
    if(mTargetPath.charAt(0) == '/') {
      Track.alert("AbsoluteTargetPath", getQualifiedName() + ": fm:target-path should be a relative path (was " + mTargetPath + ")", TrackFlag.BAD_MARKUP);
    }


    //Pagination definition
    mPaginationDefinitionName = pDbStatementXML.get1SNoEx("fm:pagination-definition");

    //Into params
    mIntoParams = parseParameterList(pDbStatementXML.getUL("fm:into"), BindDirection.OUT, true);

    //Default query mode
    String lQueryModeString = pDbStatementXML.getAttrOrNull("mode");
    if(lQueryModeString != null) {
      mQueryMode = QueryMode.fromExternalString(lQueryModeString);
    }
    else {
      mQueryMode = null;
    }

    DOM lPaginationConfig = pDbStatementXML.get1EOrNull("fm:pagination-config");
    if(lPaginationConfig != null) {
      mTopNPaginationConfig = TopNPaginationConfig.fromDOMOrNull(lPaginationConfig);
    }
    else {
      mTopNPaginationConfig = null;
    }

    //Primary key
    DOM lPrimaryList = pDbStatementXML.get1EOrNull("fm:primary");
    if(lPrimaryList != null) {
      mKeyElementNames = parsePrimaryKeys(lPrimaryList);
    }
    else {
      mKeyElementNames = null;
    }

    //For each fetch action
    DOM lForEachRow = pDbStatementXML.get1EOrNull("fm:for-each-fetch");
    if(lForEachRow != null) {
      try {
        mForEachRowCommandList = new XDoCommandList(pMod, lForEachRow);
        mForEachRowCommandList.validate(pMod);
      }
      catch (ExDoSyntax e) {
        throw new ExModule("Invalid command syntax in definition for " + getQualifiedName(), e);
      }
    }
    else {
      mForEachRowCommandList = null;
    }
  }

  private List<String> parsePrimaryKeys(DOM pPrimaryKeyDefinitionDOM)
  throws ExModule {

    DOMList lKeyDOMList = pPrimaryKeyDefinitionDOM.getUL("fm:key");
    if(lKeyDOMList.size() == 0) {
      throw new ExModule("fm:primary expected to contain at least one fm:key in " + getQualifiedName());
    }

    List<String> lKeyList = new ArrayList<String>(lKeyDOMList.size());

    for(DOM lKey : lKeyDOMList) {
      String lKeyName = lKey.value().trim();
      if(XFUtil.isNull(lKeyName)) {
        throw new ExModule("fm:key definition cannot be empty in " + getQualifiedName());
      }

      lKeyList.add(lKeyName);
    }

    return Collections.unmodifiableList(lKeyList);
  }

  /**
   * Creates an executable version of this query (with evaluated binds) but does not execute it. This should be used to
   * serialise the ExecutableQuery for execution elsewhere. If you need to execute it immediately, use executeAndDeliver.
   * <br/><br/>
   *
   * Note: if the query is serialised, it is the consumer's responsibility to ensure that all associated BindObjects are
   * appropriate to serialise.
   *
   * @param pMatchedNode Query match node.
   * @param pContextUElem Context for bind evaluation.
   * @return
   */
  public ExecutableQuery createExecutableQuery(DOM pMatchedNode, ContextUElem pContextUElem) {
    BindObjectProvider lBindProvider = createBindProvider(pMatchedNode, pContextUElem, false);
    return getParsedStatement().createExecutableQuery(lBindProvider);
  }


  @Override
  protected void executeStatementInternal(UCon pUCon, BindObjectProvider pBindProvider, ResultDeliverer<? extends ExecutableStatement> pDeliverer)
  throws ExDB {
    ExecutableQuery lExecutableQuery = getParsedStatement().createExecutableQuery(pBindProvider);
    lExecutableQuery.executeAndDeliver(pUCon, pDeliverer);
  }

  /**
   * Gets the target-path attribute for this query. This specifies the path to create for each row returned by the query,
   * relative to the match node. A value of "." indicates that the query is expected to return at most one row and the
   * result columns should be written directly into the match node.
   * @return Target path. Should not be null or empty.
   */
  public String getTargetPath() {
    return mTargetPath;
  }

  public QueryMode getQueryMode() {
    return mQueryMode;
  }

  public String getPaginationDefnitionName() {
    return mPaginationDefinitionName;
  }

  /**
   * Gets a map of column names to their corresponding fm:into parameters as defined for this query.
   * @return
   */
  public Map<String, InterfaceParameter> getIntoParams() {
    return mIntoParams;
  }

  /**
   * Gets the list of column names comprising the key definition for this query. The list can be null if no fm:primary
   * definition exists for this query. The column names should correspond to columns selected by the query.
   * @return Key column name list.
   */
  public List<String> getKeyElementNames() {
    return mKeyElementNames;
  }

  /**
   * Gets the optional Top-N pagination configuration options specified on this query definition. This may be null if
   * no Top-N options are defined.
   * @return TopNPaginationConfig, or null.
   */
  public TopNPaginationConfig getTopNPaginationConfig() {
    return mTopNPaginationConfig;
  }

  public XDoCommandList getForEachRowCommandList() {
    return mForEachRowCommandList;
  }
}
