package net.foxopen.fox.dbinterface;

import net.foxopen.fox.database.sql.bind.BindDirection;
import net.foxopen.fox.database.sql.bind.BindSQLType;
import net.foxopen.fox.module.datanode.NodeInfo;

/**
 * Markup defined in an fm:template-variable element, which can only provide a parameter name and value XPath.
 */
class TemplateVariableInterfaceParameter
implements InterfaceParameter {

  private final String mParamName;
  private final String mRelativeXPath;
  private final DOMDataType mDOMDataType;

  private TemplateVariableInterfaceParameter(String pParamName, String pRelativeXPath, DOMDataType pDOMDataType) {
    mParamName = pParamName;
    mRelativeXPath = pRelativeXPath;
    mDOMDataType = pDOMDataType;
  }

  /**
   * Creates a new TemplateVariableInterfaceParameter for the given parameter name.
   * @param pParamName Bind variable name.
   * @param pRelativeXPath XPath to be used to resolve the variable.
   * @param pDOMDataType
   * @return New TemplateVariableInterfaceParameter.
   */
  static InterfaceParameter create(String pParamName, String pRelativeXPath, DOMDataType pDOMDataType) {
    return new TemplateVariableInterfaceParameter(pParamName, pRelativeXPath, pDOMDataType);
  }

  @Override
  public String getParamName() {
    return mParamName;
  }

  @Override
  public BindSQLType getBindSQLType(NodeInfo pOptionalNodeInfo) {

    if(mDOMDataType != null) {
      //User explicitly declared a data DOM type, use that to determine the SQL type
      return InterfaceParameter.getBindSQLTypeForDOMDataType(mDOMDataType);
    }
    else if (pOptionalNodeInfo != null && !pOptionalNodeInfo.getIsItem()) {
      //Not an item (a collection or list - default to DOM)
      return BindSQLType.XML;
    }
    else {
      //In all other cases treat the template variable as a string
      return BindSQLType.STRING;
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
    return BindDirection.IN;
  }

  @Override
  public boolean isPurgeDOMContents() {
    return false;
  }

  @Override
  public boolean isTemplateOnlyParameter() {
    return true;
  }
}
