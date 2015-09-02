package net.foxopen.fox.dbinterface;

import net.foxopen.fox.database.sql.bind.BindDirection;
import net.foxopen.fox.database.sql.bind.BindSQLType;
import net.foxopen.fox.ex.ExInternal;
import net.foxopen.fox.module.datanode.NodeInfo;

/**
 * Encapsulation of fm:using, fm:template-variable and fm:into definition markup in query and API definitions.
 */
public interface InterfaceParameter {

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
  static BindSQLType getBindSQLTypeForDOMDataType(DOMDataType pDOMDataType) {

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
  String getParamName();

  /**
   * Gets the BindSQLType for this interface parameter, using the following precedence:
   * <ol>
   * <li>If the user explicitly specified a SQL type on this parameter's definition, returns that.</li>
   * <li>If the user explicitly specified a DOMDataType on this parameter's definition, returns the default BindSQLType
   * for that DOMDataType (see {@link BindVariableInterfaceParameter#getBindSQLTypeForDOMDataType}) </li>
   * <li>If a node info is available, gets the corresponding DOMDataType for the node's xs: type, and returns the BindSQLType
   * for that as in (2).</li>
   * <li>Otherwise, defaults to STRING.</li>
   * </ol>
   * @param pOptionalNodeInfo Optional NodeInfo for the parameter's target node, to help determine the default BindSQLType
   * if one was not specified on the parameter definition.
   * @return BindSQLType for this parameter, possibly contextual based on the target node.
   */
  BindSQLType getBindSQLType(NodeInfo pOptionalNodeInfo);

  /**
   * Gets the DOMDataType as specified on the parameter XML. This can be null if nothing was explicitly specified.
   * @return
   */
  DOMDataType getDOMDataType();

  /**
   * Gets the XPath of this parameter, defined either as the datadom-location attribute or the text value of the fm:using/fm:into
   * element. The XPath may contain :{context}s. If the path is a relative XPath, the statement's match node should be treated
   * as the relative node. If the user did not specify a value, this will return null and a default should be assumed.
   * @return Relative XPath or null.
   */
  String getRelativeXPath();

  /**
   * Gets the BindDirection of this paramater (IN, OUT or IN_OUT).
   * @return Non null BindDirection.
   */
  BindDirection getBindDirection();

  /**
   * Returns true if DOM columns should have their existing contents purged before being selected into. Only relevant
   * for into parameters.
   * @return
   */
  boolean isPurgeDOMContents();

  /**
   * If true, indicates this parameter is marked up only as a template variable parameter and should not be used as a query
   * bind variable.
   * @return True if this parameter is only for use in template markup.
   */
  boolean isTemplateOnlyParameter();
}
