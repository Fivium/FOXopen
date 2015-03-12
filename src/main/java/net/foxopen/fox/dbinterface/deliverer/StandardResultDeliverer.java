package net.foxopen.fox.dbinterface.deliverer;

import java.sql.ResultSet;

import net.foxopen.fox.dbinterface.InterfaceQuery;
import net.foxopen.fox.thread.ActionRequestContext;

/**
 * An InterfaceQueryResultDeliverer which performs no additional processing before or after delivery.
 */
class StandardResultDeliverer
extends InterfaceQueryResultDeliverer {

  public StandardResultDeliverer(ActionRequestContext pRequestContext, InterfaceQuery pInterfaceQuery, QueryRowProvider pRowProvider) {
    super(pRequestContext, pInterfaceQuery, pRowProvider);
  }

  @Override
  protected void performPreDeliveryProcessing() {
  }

  @Override
  protected void performPostDeliveryProcessing(int pFinalRowCount, ResultSet pResultSet) {
  }
}
