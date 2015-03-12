package net.foxopen.fox.dbinterface;

import net.foxopen.fox.XFUtil;
import net.foxopen.fox.dom.DOM;
import net.foxopen.fox.ex.ExModule;

/**
 * Query level configuration for Top-N pagination. This drives how the Top-N pager operates.
 */
public class TopNPaginationConfig {

  private final String mRowFromBindName;
  private final String mRowToBindName;
  private final String mSCNBindName;
  private final String mRowCountColumnName;

  private TopNPaginationConfig(String pRowFromBindName, String pRowToBindName, String pSCNBindName, String pRowCountColumnName) {
    mRowFromBindName = pRowFromBindName;
    mRowToBindName = pRowToBindName;
    mSCNBindName = pSCNBindName;
    mRowCountColumnName = pRowCountColumnName;
  }

  public static TopNPaginationConfig fromDOMOrNull(DOM pDefinitionElement) throws ExModule {

    String lRowFrom = pDefinitionElement.getAttrOrNull("row-from-bind-name");
    String lRowTo = pDefinitionElement.getAttrOrNull("row-to-bind-name");
    String lSCN = pDefinitionElement.getAttrOrNull("scn-bind-name");
    String lRowCountColumn = pDefinitionElement.getAttrOrNull("row-count-column-name");

    if(XFUtil.isNull(lRowFrom) || XFUtil.isNull(lRowTo)) {
      throw new ExModule("Both row-from-bind-name and row-to-bind-name attributes must be specified.");
    }

    lRowFrom = prependBindPrefix(lRowFrom);
    lRowTo = prependBindPrefix(lRowTo);
    if(lSCN != null) {
      lSCN = prependBindPrefix(lSCN);
    }

    return new TopNPaginationConfig(lRowFrom, lRowTo, lSCN, lRowCountColumn);
  }

  private static String prependBindPrefix(String pBindName) {
    if(pBindName.charAt(0) != ':') {
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
}
