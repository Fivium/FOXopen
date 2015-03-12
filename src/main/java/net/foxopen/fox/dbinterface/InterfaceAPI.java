package net.foxopen.fox.dbinterface;

import net.foxopen.fox.XFUtil;
import net.foxopen.fox.database.UCon;
import net.foxopen.fox.database.sql.ExecutableAPI;
import net.foxopen.fox.database.sql.ExecutableStatement;
import net.foxopen.fox.database.sql.ResultDeliverer;
import net.foxopen.fox.database.sql.bind.BindObjectProvider;
import net.foxopen.fox.dom.DOM;
import net.foxopen.fox.ex.ExDB;
import net.foxopen.fox.ex.ExModule;
import net.foxopen.fox.module.Mod;

/**
 * Representation of an API definition within a database interface definition.
 */
public class InterfaceAPI
extends InterfaceStatement {

  static final String DBINT_ELEMENT_NAME = "api";

  public InterfaceAPI(DOM pDbStatementXML, String pDbInterfaceName, Mod pMod)
  throws ExModule {
    super(pDbStatementXML, pDbInterfaceName, "fm:statement", true, pMod);

    //Validate out params have paths (note this could get annoying on legacy modules - used to assume a value of "." and bind out to match node)"
    for(InterfaceParameter lParam : getUsingParamMap().values()) {
      if(lParam.getBindDirection().isOutBind() && XFUtil.isNull(lParam.getRelativeXPath())) {
        throw new ExModule("Invalid definition for " + getQualifiedName() + " - out bind " + lParam.getParamName() + " misssing datadom-location attribute");
      }
    }
  }

  @Override
  protected void executeStatementInternal(UCon pUCon, BindObjectProvider pBindProvider, ResultDeliverer<? extends ExecutableStatement> pDeliverer)
  throws ExDB {
    ExecutableAPI lExecutableAPI = getParsedStatement().createExecutableAPI(pBindProvider);
    lExecutableAPI.executeAndDeliver(pUCon, pDeliverer);
  }
}
