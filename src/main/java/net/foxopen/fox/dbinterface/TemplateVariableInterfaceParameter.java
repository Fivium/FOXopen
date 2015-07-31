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

  private TemplateVariableInterfaceParameter(String pParamName, String pRelativeXPath) {
    mParamName = pParamName;
    mRelativeXPath = pRelativeXPath;
  }

  /**
   * Creates a new TemplateVariableInterfaceParameter for the given parameter name.
   * @param pParamName Bind variable name.
   * @param pRelativeXPath XPath to be used to resolve the variable.
   * @return New TemplateVariableInterfaceParameter.
   */
  static InterfaceParameter create(String pParamName, String pRelativeXPath) {
    return new TemplateVariableInterfaceParameter(pParamName, pRelativeXPath);
  }

  @Override
  public String getParamName() {
    return mParamName;
  }

  @Override
  public BindSQLType getBindSQLType(NodeInfo pOptionalNodeInfo) {
    return null;
  }

  @Override
  public DOMDataType getDOMDataType() {
    return null;
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
