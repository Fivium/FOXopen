package net.foxopen.fox.dom.paging;

import net.foxopen.fox.XFUtil;
import net.foxopen.fox.database.UCon;
import net.foxopen.fox.database.sql.bind.BindDirection;
import net.foxopen.fox.database.sql.bind.BindObject;
import net.foxopen.fox.database.sql.bind.BindSQLType;
import net.foxopen.fox.database.sql.bind.DecoratingBindObjectProvider;
import net.foxopen.fox.database.sql.bind.NumericBindObject;
import net.foxopen.fox.dbinterface.TopNPaginationConfig;

import java.sql.SQLException;

/**
 * Bind provider for a Top-N paginated query. Provides the "from" and "to" row number binds.
 */
public class TopNPaginationBindProvider
extends DecoratingBindObjectProvider {

  private final TopNPaginationConfig mTopNPaginationConfig;
  private final TopNDatabasePager mPager;

  public TopNPaginationBindProvider(TopNPaginationConfig pTopNPaginationConfig, TopNDatabasePager pPager) {
    mTopNPaginationConfig = pTopNPaginationConfig;
    mPager = pPager;
  }

  @Override
  public BindObject getBindObjectOrNull(String pBindName, int pIndex) {

    pBindName = XFUtil.nvl(pBindName, "");

    if(pBindName.equals(mTopNPaginationConfig.getRowFromBindName())) {
      //"row from" is inclusive of the first row number of the current page
      return new NumericBindObject(mPager.getQueryBindRowStart());
    }
    else if(pBindName.equals(mTopNPaginationConfig.getOffsetBindName())) {
      //"offset" is exclusive of the first row number of the current page
      return new NumericBindObject(mPager.getQueryBindRowStart() - 1);
    }
    else if(pBindName.equals(mTopNPaginationConfig.getRowToBindName())) {
      return new NumericBindObject(mPager.getQueryBindRowEnd());
    }
    else if(pBindName.equals(mTopNPaginationConfig.getPageSizeBindName())) {
      return new NumericBindObject(mPager.getPageSize() + mPager.getLookAheadRows());
    }
    else if(pBindName.equals(mTopNPaginationConfig.getSCNBindName())) {
      return new SCNBindObject();
    }
    else {
      return null;
    }
  }

  private class SCNBindObject
  implements BindObject {

    @Override
    public Object getObject(UCon pUCon) throws SQLException {
      return mPager.getAsOfSCN();
    }

    @Override
    public String getObjectDebugString() {
      return mPager.getAsOfSCN();
    }

    @Override
    public BindSQLType getSQLType() {
      return BindSQLType.STRING;
    }

    @Override
    public BindDirection getDirection() {
      return BindDirection.IN;
    }
  }
}
