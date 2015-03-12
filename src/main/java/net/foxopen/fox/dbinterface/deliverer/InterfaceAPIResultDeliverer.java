package net.foxopen.fox.dbinterface.deliverer;

import java.sql.CallableStatement;
import java.sql.SQLException;

import net.foxopen.fox.XFUtil;
import net.foxopen.fox.database.sql.APIResultDeliverer;
import net.foxopen.fox.database.sql.ExecutableAPI;
import net.foxopen.fox.database.sql.out.CallableStatementAdaptor;
import net.foxopen.fox.database.sql.out.JDBCResultAdaptor;
import net.foxopen.fox.dbinterface.InterfaceAPI;
import net.foxopen.fox.dbinterface.InterfaceParameter;
import net.foxopen.fox.dom.DOM;
import net.foxopen.fox.ex.ExInternal;
import net.foxopen.fox.thread.ActionRequestContext;


/**
 * Deliverer for populating the matched node of an fm:run-api command with any output parameters (i.e OUT or IN_OUT).
 */
public class InterfaceAPIResultDeliverer
implements APIResultDeliverer {

  private final ActionRequestContext mRequestContext;
  private final InterfaceAPI mInterfaceAPI;
  private final DOM mMatchNode;

  public InterfaceAPIResultDeliverer(ActionRequestContext pRequestContext, InterfaceAPI pInterfaceAPI, DOM pMatchNode) {
    mRequestContext = pRequestContext;
    mInterfaceAPI = pInterfaceAPI;
    mMatchNode = pMatchNode;
  }

  @Override
  public void deliver(ExecutableAPI pAPI) {

    try {
      CallableStatement lCallableStatement = pAPI.getCallableStatement();
      JDBCResultAdaptor lResultAdaptor = new CallableStatementAdaptor(lCallableStatement);

      //Loop through all the binds in the parsed statement, retriving out binds into the DOM
      int lBindIdx = 1;
      for(String lAPIBindName : mInterfaceAPI.getParsedStatement().getBindNameList()) {
        InterfaceParameter lParam = mInterfaceAPI.getParamForBindNameOrNull(lAPIBindName);

        if(lParam == null) {
          throw new ExInternal("No bind definition found for bind " + lAPIBindName);
        }

        //If this parameter was outbound we need to populate the DOM with the result
        if(lParam.getBindDirection().isOutBind()) {

          String lOutBindName = lParam.getParamName();
          String lDestinationPath = lParam.getRelativeXPath();

          //Check user has specified a destination for the bind
          //Could default based on bind name here? (legacy didn't)
          if(XFUtil.isNull(lDestinationPath)) {
            throw new ExInternal("datadom-location must be specified for parameter " + lOutBindName + " in API " + mInterfaceAPI.getStatementName());
          }

          //Use the SQL type recorded at bind time when binding back out (it will not have changed)
          int lOutSQLType = pAPI.getOutBindSQLType(lBindIdx).getSqlTypeCode();

          //Convert the out bind and write to the DOM
          DelivererUtils.convertResultAndPopulateDOM(mRequestContext, lResultAdaptor, lBindIdx, lOutSQLType, mMatchNode, lDestinationPath,
                                                     lParam.getDOMDataType(), lOutBindName, false, lParam.isPurgeDOMContents());
        }

        lBindIdx++;
      }

    }
    catch (SQLException e) {
      throw new ExInternal("Error writing query results to DOM in API " + mInterfaceAPI.getStatementName(), e);
    }
  }

  @Override
  public boolean closeStatementAfterDelivery() {
    return true;
  }
}
