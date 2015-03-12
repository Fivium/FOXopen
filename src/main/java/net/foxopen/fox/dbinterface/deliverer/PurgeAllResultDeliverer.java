package net.foxopen.fox.dbinterface.deliverer;

import java.sql.ResultSet;

import net.foxopen.fox.dbinterface.InterfaceQuery;
import net.foxopen.fox.dom.DOM;
import net.foxopen.fox.ex.ExActionFailed;
import net.foxopen.fox.thread.ActionRequestContext;
import net.foxopen.fox.track.Track;
import net.foxopen.fox.track.TrackFlag;

/**
 * A QueryResultDeliverer which clears out all the children of the match node before the query runs. This is used by
 * the PURGE-ALL query mode.
 */
class PurgeAllResultDeliverer
extends InterfaceQueryResultDeliverer {

  private final DOM mMatchNode;

  protected PurgeAllResultDeliverer(ActionRequestContext pRequestContext, InterfaceQuery pInterfaceQuery, QueryRowProvider pRowProvider, DOM pMatchNode) {
    super(pRequestContext, pInterfaceQuery, pRowProvider);
    mMatchNode = pMatchNode;
  }

  @Override
  protected void performPreDeliveryProcessing() {
    try {
      String lTargetPath = mInterfaceQuery.getTargetPath();
      //For single row queries purge-all should remove contents of the match node.
      //For multi row queries row containers should be removed.
      if(".".equals(lTargetPath)){
        mMatchNode.removeAllChildren();
        Track.info("SingleRowPurgeAll", "Query executed in purge-all mode without a target-path specified - purging match node children in lieu of target path nodes", TrackFlag.REGRESSION_CHANGE);
      }
      else {
        mRequestContext.getContextUElem().extendedXPathUL(mMatchNode, lTargetPath).removeFromDOMTree();
      }

    }
    catch (ExActionFailed ex) {
      // It's valid for the target to not exist so continue
    }
  }

  @Override
  protected void performPostDeliveryProcessing(int pFinalRowCount, ResultSet pResultSet) {
  }
}
