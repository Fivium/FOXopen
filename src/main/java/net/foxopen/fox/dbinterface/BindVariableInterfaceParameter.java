package net.foxopen.fox.dbinterface;

import net.foxopen.fox.XFUtil;
import net.foxopen.fox.database.sql.bind.BindDirection;
import net.foxopen.fox.database.sql.bind.BindSQLType;
import net.foxopen.fox.ex.ExModule;
import net.foxopen.fox.module.datanode.NodeInfo;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;


/**
 * Tuple containing data items found in an fm:using or fm:into clause of a database interface statement.
 */
public class BindVariableInterfaceParameter
implements InterfaceParameter {

  public static final String SQLBIND_STRING = "varchar";
  public static final String SQLBIND_XML = "xmltype";
  public static final String SQLBIND_CLOB = "clob";
  public static final String SQLBIND_DATE = "date";

  /**
   * Supported SQL Bind Types (subset of BindSQLTypes)
   */
  private static final Map<String, BindSQLType> gExternalStringToBindTypeMap;

  static {
    Map<String, BindSQLType> lMap = new HashMap<>();
    lMap.put(SQLBIND_STRING, BindSQLType.STRING);
    lMap.put(SQLBIND_XML, BindSQLType.XML);
    lMap.put(SQLBIND_CLOB, BindSQLType.CLOB);
    lMap.put(SQLBIND_DATE, BindSQLType.TIMESTAMP);
    gExternalStringToBindTypeMap = Collections.unmodifiableMap(lMap);
  }

  private final String mParamName;
  //Can be null
  private final BindSQLType mSqlDataType;
  private final BindDirection mBindDirection;
  //Can be null
  private final DOMDataType mDOMDataType;
  /**
   * XPath for this parameter relative to the evaluation attach point, i.e. the statement's matched node for inputs
   * or the target path row node for outputs.
   */
  private final String mRelativeXPath;

  private final boolean mPurgeDOMContents;

  static InterfaceParameter create(String pParamName, String pSqlDataType, DOMDataType pDOMDataType, String pBindDirection, String pRelativeXPath, String pDOMMergeMode, BindDirection pDefaultDirection)
  throws ExModule {

    //Validate pParamName
    if (XFUtil.isNull(pParamName)) {
      throw new ExModule("New InterfaceParameter requires a Param Name and null was passed.");
    }

    //If a SQL type was specified, look it up in the map (otherwise keep it null and let the binding process work it out later)
    BindSQLType lBindSqlType = null;
    if (!XFUtil.isNull(pSqlDataType)) {
      lBindSqlType = gExternalStringToBindTypeMap.get(pSqlDataType);
      //Check external string was specified correctly
      if (lBindSqlType == null) {
        throw new ExModule("Unknown SQL data type '" + pSqlDataType + "'");
      }
    }

    //Use the provided default bind direction unless explicitly specified on the element
    BindDirection lBindDirection;
    if (XFUtil.isNull(pBindDirection)) {
      lBindDirection = pDefaultDirection;
    }
    else {
      //TODO fix this - map lookup
      lBindDirection = BindDirection.valueOf(pBindDirection.toUpperCase().replace(' ', '_'));
      if (lBindDirection == null) {
        throw new ExModule("Unknown bind direction '" + pBindDirection + "'");
      }
    }

    //Ensure relative path is null if not specified
    String lRelativeXPath = XFUtil.nvl(pRelativeXPath, null);

    //DOM Merge Mode controls whether XMLTypes are augmented or purged when written into an existing complex node
    boolean lPurgeDOMContents;
    if (!XFUtil.isNull(pDOMMergeMode)) {
      //Validate that correct values have been specified
      if ("add-to".equals(pDOMMergeMode)) {
        lPurgeDOMContents = false;
      }
      else if ("purge".equals(pDOMMergeMode)) {
        lPurgeDOMContents = true;
      }
      else {
        throw new ExModule("Unknown DOM Merge mode '" + pDOMMergeMode + "'");
      }
    }
    else {
      //Legacy behaviour - always merge selected DOMs
      lPurgeDOMContents = false;
    }

    return new BindVariableInterfaceParameter(pParamName, lBindSqlType, pDOMDataType, lBindDirection, lRelativeXPath, lPurgeDOMContents);
  }

  private BindVariableInterfaceParameter(String pParamName, BindSQLType pSqlDataType, DOMDataType pDOMDataType, BindDirection pBindDirection, String pRelativeXpath, boolean pPurgeDOMContents) {
    mParamName = pParamName;
    mSqlDataType = pSqlDataType;
    mBindDirection = pBindDirection;
    mDOMDataType = pDOMDataType;
    mRelativeXPath = pRelativeXpath;
    mPurgeDOMContents = pPurgeDOMContents;
  }

  @Override
  public String getParamName() {
    return mParamName;
  }

  @Override
  public BindSQLType getBindSQLType(NodeInfo pOptionalNodeInfo) {

    //If the user explicitly declared a SQL data type, use that
    if(mSqlDataType != null) {
      return mSqlDataType;
    }
    else if(mDOMDataType != null) {
      //User explicitly declared a data DOM type, use that to determine the SQL type
      return InterfaceParameter.getBindSQLTypeForDOMDataType(mDOMDataType);
    }
    else {

      if(pOptionalNodeInfo != null) {
        if (!pOptionalNodeInfo.getIsItem()) {
          //Not an item (a collection or list - default to DOM)
          return BindSQLType.XML;
        }
        else {
          //If we have an item node info we can use that to determine the DOMDataType and subsequently the SQL Type.
          //Otherwise rely on the default SQL type (xs:string) by leaving lDOMDataType null
          DOMDataType lDOMDataType = DOMDataType.fromExternalString(pOptionalNodeInfo.getDataType());
          return InterfaceParameter.getBindSQLTypeForDOMDataType(lDOMDataType);
        }
      }
      else {
        return BindSQLType.STRING;
      }

    }
  }

  @Override
  public DOMDataType getDOMDataType() {
    return mDOMDataType;
  }

  @Override
  public String getRelativeXPath() {
    return mRelativeXPath;
  }

  @Override
  public BindDirection getBindDirection() {
    return mBindDirection;
  }

  @Override
  public boolean isPurgeDOMContents() {
    return mPurgeDOMContents;
  }

  @Override
  public boolean isTemplateOnlyParameter() {
    return false;
  }
}
