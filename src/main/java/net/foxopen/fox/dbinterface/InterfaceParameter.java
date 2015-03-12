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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import net.foxopen.fox.XFUtil;
import net.foxopen.fox.database.sql.bind.BindDirection;
import net.foxopen.fox.database.sql.bind.BindSQLType;
import net.foxopen.fox.ex.ExInternal;
import net.foxopen.fox.ex.ExModule;
import net.foxopen.fox.module.datanode.NodeInfo;


/**
 * Tuple containing data items found in an fm:using or fm:into clause of a database interface statement.
 */
public class InterfaceParameter {

  public static final String SQLBIND_STRING = "varchar";
  public static final String SQLBIND_XML = "xmltype";
  public static final String SQLBIND_CLOB = "clob";
  public static final String SQLBIND_DATE = "date";

  /**  Supported SQL Bind Types (subset of BindSQLTypes) */
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
  /** XPath for this parameter relative to the evaluation attach point, i.e. the statement's matched node for inputs
   * or the target path row node for outputs. */
  private final String mRelativeXPath;
  /** Index of this bind in the definition list (not necessarily the statement */

  private final boolean mPurgeDOMContents;

  static InterfaceParameter create(String pParamName, String pSqlDataType, String pDOMDataType, String pBindDirection, String pRelativeXPath, String pDOMMergeMode, BindDirection pDefaultDirection)
  throws ExModule {

    //Validate pParamName
    if (XFUtil.isNull(pParamName)) {
      throw new ExModule("New InterfaceParameter requires a Param Name and null was passed.");
    }

    //If a SQL type was specified, look it up in the map (otherwise keep it null and let the binding process work it out later)
    BindSQLType lBindSqlType = null;
    if(!XFUtil.isNull(pSqlDataType)) {
      lBindSqlType = gExternalStringToBindTypeMap.get(pSqlDataType);
      //Check external string was specified correctly
      if(lBindSqlType == null) {
        throw new ExModule("Unknown SQL data type '" + pSqlDataType + "'");
      }
    }

    //If a DOM type was specified, look it up in the map (otherwise keep it null and let the binding process work it out later)
    DOMDataType lBindDOMType = null;
    if(!XFUtil.isNull(pDOMDataType)) {
      lBindDOMType = DOMDataType.fromExternalString(pDOMDataType);
      //Check external string was specified correctly
      if(lBindDOMType == null) {
        throw new ExModule("Unknown DOM data type '" + pDOMDataType + "'");
      }
    }

    //Use the provided default bind direction unless explicitly specified on the element
    BindDirection lBindDirection;
    if(XFUtil.isNull(pBindDirection)) {
      lBindDirection = pDefaultDirection;
    }
    else {
      //TODO fix this - map lookup
      lBindDirection = BindDirection.valueOf(pBindDirection.toUpperCase().replace(' ', '_'));
      if(lBindDirection == null) {
        throw new ExModule("Unknown bind direction '" + lBindDirection + "'");
      }
    }

    //Ensure relative path is null if not specified
    String lRelativeXPath = XFUtil.nvl(pRelativeXPath, null);

    //DOM Merge Mode controls whether XMLTypes are augmented or purged when written into an existing complex node
    boolean lPurgeDOMContents;
    if(!XFUtil.isNull(pDOMMergeMode)) {
      //Validate that correct values have been specified
      if("add-to".equals(pDOMMergeMode)) {
        lPurgeDOMContents = false;
      }
      else if("purge".equals(pDOMMergeMode)){
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

    return new InterfaceParameter(pParamName, lBindSqlType, lBindDOMType, lBindDirection, lRelativeXPath, lPurgeDOMContents);
  }

  private InterfaceParameter(String pParamName, BindSQLType pSqlDataType, DOMDataType pDOMDataType, BindDirection pBindDirection, String pRelativeXpath, boolean pPurgeDOMContents) {
    mParamName = pParamName;
    mSqlDataType = pSqlDataType;
    mBindDirection = pBindDirection;
    mDOMDataType = pDOMDataType;
    mRelativeXPath = pRelativeXpath;
    mPurgeDOMContents = pPurgeDOMContents;
  }

  /**
   * Get default BindSQLType given a FOX datadom-type. Defaults are as follows:
   * <ul>
   * <li>STRING/null -> STRING</lI>
   * <li>DOM -> XML</lI>
   * <li>DATE/DATETIME -> TIMESTAMP</lI>
   * </ul>
   *
   * @param pDOMDataType The input FOX Datatype
   */
  private static BindSQLType getBindSQLTypeForDOMDataType(DOMDataType pDOMDataType) {

    if (pDOMDataType == null) {
      return BindSQLType.STRING; // If FOX type is null default to String
    }

    switch(pDOMDataType) {
      case STRING:
        return BindSQLType.STRING;
      case DOM:
        return BindSQLType.XML;
      case DATE:
      case DATETIME:
      case TIME:
        return BindSQLType.TIMESTAMP;
      default:
        throw new ExInternal("Can't map " + pDOMDataType + " to a BindSQLType");
    }
  }

  /**
   * Return the bind name as a String, excluding the colon prefix.
   * @return bind name
   */
  public String getParamName() {
    return mParamName;
  }

  /**
   * Gets the BindSQLType for this interface parameter, using the following precedence:
   * <ol>
   * <li>If the user explicitly specified a SQL type on this parameter's definition, returns that.</li>
   * <li>If the user explicitly specified a DOMDataTypeon this parameter's definition, returns the default BindSQLType
   * for that DOMDataType (see {@link #getBindSQLTypeForDOMDataType}) </li>
   * <li>If a node info is available, gets the corresponding DOMDataType for the node's xs: type, and returns the BindSQLType
   * for that as in (2).</li>
   * <li>Otherwise, defaults to STRING.</li>
   * </ol>
   * @param pOptionalNodeInfo Optional NodeInfo for the parameter's target node, to help determine the default BindSQLType
   * if one was not specified on the parameter definition.
   * @return BindSQLType for this parameter, possibly contextual based on the target node.
   */
  public BindSQLType getBindSQLType(NodeInfo pOptionalNodeInfo) {

    //If the user explicitly declared a SQL data type, use that
    if(mSqlDataType != null) {
      return mSqlDataType;
    }
    else if(mDOMDataType != null) {
      //User explicitly declared a data DOM type, use that to determine the SQL type
      return getBindSQLTypeForDOMDataType(mDOMDataType);
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
          return getBindSQLTypeForDOMDataType(lDOMDataType);
        }
      }
      else {
        return BindSQLType.STRING;
      }

    }
  }

  /**
   * Gets the DOMDataType as specified on the parameter XML. This can be null if nothing was explicitly specified.
   * @return
   */
  public DOMDataType getDOMDataType() {
    return mDOMDataType;
  }

  /**
   * Gets the XPath of this parameter, defined either as the datadom-location attribute or the text value of the fm:using/fm:into
   * element. The XPath may contain :{context}s. If the path is a relative XPath, the statement's match node should be treated
   * as the relative node. If the user did not specify a value, this will return null and a default should be assumed.
   * @return Relative XPath or null.
   */
  public String getRelativeXPath() {
    return mRelativeXPath;
  }

  public BindDirection getBindDirection() {
    return mBindDirection;
  }

  /**
   * Returns true if DOM columns should have their existing contents purged before being selected into. Only relevant
   * for into parameters.
   * @return
   */
  public boolean isPurgeDOMContents() {
    return mPurgeDOMContents;
  }
}
