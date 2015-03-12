package net.foxopen.fox.dbinterface.deliverer;

import net.foxopen.fox.database.sql.out.JDBCResultAdaptor;
import net.foxopen.fox.dbinterface.InterfaceQuery;
import net.foxopen.fox.dom.DOM;


/**
 * Modifier for the AUGMENT row provider which clears out the contents of existing rows before allowing them to be populated.
 */
class PurgeSelectedRowProvider
extends AugmentRowProvider {

  public PurgeSelectedRowProvider(InterfaceQuery pInterfaceQuery, DOM pMatchNode) {
    super(pInterfaceQuery, pMatchNode);
  }

  @Override
  public DOM getTargetRow(JDBCResultAdaptor pResultSet) {
    DOM lRow = super.getTargetRow(pResultSet);
    //Purge selected row's child nodes
    lRow.removeAllChildren();
    return lRow;
  }

}
