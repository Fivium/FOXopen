package net.foxopen.fox.dbinterface;

import net.foxopen.fox.XFUtil;
import net.foxopen.fox.dom.DOM;
import net.foxopen.fox.ex.ExModule;

/**
 * Query level configuration for Top-N pagination. This drives how the Top-N pager operates.
 */
public class TopNPaginationConfig {

  /* Row from/to binds are designed for use in a "row_number BETWEEN x AND y" clause.
     Offset and page size binds are designed for use in a 12c style "OFFSET x ROWS FETCH NEXT y ROWS ONLY" Top-N query. */

  private final String mRowFromBindName;
  private final String mRowToBindName;
  private final String mOffsetBindName;
  private final String mPageSizeBindName;
  private final String mSCNBindName;
  private final String mRowCountColumnName;

  private TopNPaginationConfig(String pRowFromBindName, String pRowToBindName, String pOffsetBindName, String pPageSizeBindName, String pSCNBindName, String pRowCountColumnName) {
    mRowFromBindName = pRowFromBindName;
    mRowToBindName = pRowToBindName;
    mOffsetBindName = pOffsetBindName;
    mPageSizeBindName = pPageSizeBindName;
    mSCNBindName = pSCNBindName;
    mRowCountColumnName = pRowCountColumnName;
  }

  public static TopNPaginationConfig fromDOMOrNull(DOM pDefinitionElement) throws ExModule {

    String lRowFrom = prependBindPrefix(pDefinitionElement.getAttrOrNull("row-from-bind-name"));
    String lRowTo = prependBindPrefix(pDefinitionElement.getAttrOrNull("row-to-bind-name"));
    String lOffset = prependBindPrefix(pDefinitionElement.getAttrOrNull("offset-bind-name"));
    String lPageSize = prependBindPrefix(pDefinitionElement.getAttrOrNull("page-size-bind-name"));
    String lSCN =  prependBindPrefix(pDefinitionElement.getAttrOrNull("scn-bind-name"));
    String lRowCountColumn = pDefinitionElement.getAttrOrNull("row-count-column-name");

    if(XFUtil.isNull(lRowFrom) && XFUtil.isNull(lOffset)) {
      throw new ExModule("Either row-from-bind-name attribute or offset-bind-name attribute must be specified.");
    }

    if(XFUtil.isNull(lRowTo) && XFUtil.isNull(lPageSize)) {
      throw new ExModule("Either row-to-bind-name attribute or page-size-bind-name attribute must be specified.");
    }

    return new TopNPaginationConfig(lRowFrom, lRowTo, lOffset, lPageSize, lSCN, lRowCountColumn);
  }

  private static String prependBindPrefix(String pBindName) {
    if(pBindName != null && pBindName.charAt(0) != ':') {
      return ":" + pBindName;
    }
    else {
      return pBindName;
    }
  }

  /**
   * Bind name for the "row number from" bind variable, including ":" prefix.
   * @return
   */
  public String getRowFromBindName() {
    return mRowFromBindName;
  }

  /**
   * Bind name for the "row number to" bind variable, including ":" prefix.
   * @return
   */
  public String getRowToBindName() {
    return mRowToBindName;
  }

  /**
   * The column name which will report the rowcount. Can be null.
   * @return
   */
  public String getRowCountColumnName() {
    return mRowCountColumnName;
  }

  /**
   * Bind name for the SCN bind variable, including ":" prefix.
   * @return
   */
  public String getSCNBindName() {
    return mSCNBindName;
  }

  /**
   * Bind name for the offset variable, including ":" prefix.
   * @return
   */
  public String getOffsetBindName() {
    return mOffsetBindName;
  }

  /**
   * Bind name for the page size bind variable, including ":" prefix.
   * @return
   */
  public String getPageSizeBindName() {
    return mPageSizeBindName;
  }
}
