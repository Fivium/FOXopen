package net.foxopen.fox.dbinterface.deliverer;

import java.sql.ResultSetMetaData;

import net.foxopen.fox.database.sql.out.JDBCResultAdaptor;
import net.foxopen.fox.dbinterface.InterfaceQuery;
import net.foxopen.fox.dom.DOM;
import net.foxopen.fox.ex.ExTooMany;


/**
 * Row provider for the ADD-TO query mode. This always creates a new row element for every row.
 */
public class AddToRowProvider
implements QueryRowProvider {

  private final InterfaceQuery mInterfaceQuery;
  private final DOM mMatchNode;

  public AddToRowProvider(InterfaceQuery pInterfaceQuery, DOM pMatchNode) {
    mInterfaceQuery = pInterfaceQuery;
    mMatchNode = pMatchNode;
  }

  @Override
  public DOM getTargetRow(JDBCResultAdaptor pResultSet) throws ExTooMany {
    return getTargetRow();
  }

  /**
   * Gets a target row without requiring a result set.
   * @return
   * @throws ExTooMany
   */
  public DOM getTargetRow() throws ExTooMany {
    return mMatchNode.create1E(mInterfaceQuery.getTargetPath());
  }

  @Override
  public void prepareForDelivery(ResultSetMetaData pResultSetMeta) {
  }

  @Override
  public void finaliseRow(int pRowNumber, DOM pRow) {
  }
}
