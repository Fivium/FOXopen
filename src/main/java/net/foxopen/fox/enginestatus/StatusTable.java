package net.foxopen.fox.enginestatus;

import net.foxopen.fox.banghandler.BangHandler;
import net.foxopen.fox.ex.ExInternal;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class StatusTable
implements ContainerStatusItem, NamedStatusItem {

  private final String mTableName;
  private final List<String> mColumnNames;
  private RowProvider mRowProvider = null;

  public StatusTable(String pTableName, String... pColumnNames) {
    mTableName = pTableName;
    mColumnNames = Arrays.asList(pColumnNames);
  }

  public StatusTable setRowProvider(RowProvider pRowProvider) {
    mRowProvider = pRowProvider;
    return this;
  }

  @Override
  public String getMnem() {
    return EngineStatus.promptToMnem(mTableName);
  }

  @Override
  public MessageLevel getMaxMessageSeverity() {
    return EngineStatus.getMaxChildMessageSeverity(getRows());
  }

//  public Row addRow() {
//    Row lRow = new Row("ROW_" + mRows.size());
//    mRows.add(lRow);
//    return lRow;
//  }
//
//  public Row addRow(String pRowMnem) {
//    Row lRow = new Row(pRowMnem);
//    mRows.add(lRow);
//    return lRow;
//  }

  private List<Row> getRows() {
    if(mRowProvider == null) {
      throw new ExInternal("No row provider available for this table (ensure setRowProvider has been invoked)");
    }
    RowDestinationImpl lRowDestination = new RowDestinationImpl();
    mRowProvider.generateRows(lRowDestination);
    return lRowDestination.mRows;
  }

  @Override
  public StatusItem getNestedItem(String pItemMnem) {
    return EngineStatus.getNestedItem(getRows(), pItemMnem);
  }

  @Override
  public void serialiseHTML(Writer pWriter, StatusSerialisationContext pSerialisationContext)
  throws IOException {

    List<Row> lRows = getRows();

    pSerialisationContext.pushNestedStatus(getMnem());
    try {
      //Work out all column names
      Set<String> lColumnNameSet = new LinkedHashSet<>(mColumnNames);
      for(Row lRow : lRows) {
        for(String lColName : lRow.mColumns.keySet()) {
          lColumnNameSet.add(lColName);
        }
      }

      pWriter.append("<h2>" + mTableName + "</h2>");

      pWriter.append("<table width=\"100%\" class=\"table table-striped\"><thead><tr>");

      for(String lColName : lColumnNameSet) {
        pWriter.append("<th>" + lColName + "</th>");
      }

      pWriter.append("</tr></thead><tbody>");

      for(Row lRow : lRows) {

        pSerialisationContext.pushNestedStatus(lRow.mRowMnem);
        try {
          pWriter.append("<tr>");
          for(String lColName : lColumnNameSet) {

            StatusItem lColValue = lRow.mColumns.get(lColName);
            pWriter.append("<td valign=\"top\">");
            if(lColValue != null) {
              //If the column contains multiple items (i.e. a list of actions) we need to make sure the container mnem is included in any paths which are generated
              if(lColValue instanceof ContainerStatusItem) {
                pSerialisationContext.pushNestedStatus(lColValue.getMnem());
              }
              try {
                lColValue.serialiseHTML(pWriter, pSerialisationContext);
              }
              finally {
                if(lColValue instanceof ContainerStatusItem) {
                  pSerialisationContext.popNestedStatus(lColValue.getMnem());
                }
              }
            }
            else {
              pWriter.append("(null)");
            }
            pWriter.append("</td>");

          }
          pWriter.append("</tr>");
        }
        finally {
          pSerialisationContext.popNestedStatus(lRow.mRowMnem);
        }
      }
    }
    finally {
      pSerialisationContext.popNestedStatus(getMnem());
    }

    pWriter.append("</tbody></table>");
  }

  @Override
  public String getItemName() {
    return mTableName;
  }

  public interface RowProvider {
    void generateRows(RowDestination pRowDestination);
  }

  public interface RowDestination {
    Row addRow();
    Row addRow(String pRowMnem);
  }

  private class RowDestinationImpl
  implements RowDestination {

    private final List<Row> mRows = new ArrayList<>();

    @Override
    public Row addRow() {
      Row lRow = new Row("ROW_" + mRows.size());
      mRows.add(lRow);
      return lRow;
    }

    @Override
    public Row addRow(String pRowMnem) {
      Row lRow = new Row(pRowMnem);
      mRows.add(lRow);
      return lRow;
    }
  }

  public class Row
  implements ContainerStatusItem {

    private final String mRowMnem;
    private int mCurrentColumnIndex = 0;

    private final Map<String, StatusItem> mColumns = new HashMap<>();

    public Row(String pRowMnem) {
      mRowMnem = pRowMnem;
    }

    @Override
    public StatusItem getNestedItem(String pItemMnem) {
      return EngineStatus.getNestedItem(mColumns.values(), pItemMnem);
    }

    @Override
    public String getMnem() {
      return mRowMnem;
    }

    @Override
    public MessageLevel getMaxMessageSeverity() {
      return EngineStatus.getMaxChildMessageSeverity(mColumns.values());
    }

    @Override
    public void serialiseHTML(Writer pWriter, StatusSerialisationContext pSerialisationContext)
    throws IOException {
      //TODO move from above to here
    }

    /**
     * Sets the column value sequentially.
     * @param pValue
     * @return
     */
    public Row setColumn(String pValue) {
      mColumns.put(mColumnNames.get(mCurrentColumnIndex++), new StatusText(pValue));
      return this;
    }

    public Row setDetailColumn(String pTitle, String pDetailText) {
      mColumns.put(mColumnNames.get(mCurrentColumnIndex++), new StatusDetail(pTitle, pDetailText));
      return this;
    }

    public Row setDetailColumn(String pTitle, StatusDetail.Provider pProvider) {
      mColumns.put(mColumnNames.get(mCurrentColumnIndex++), new StatusDetail(pTitle, pProvider));
      return this;
    }

    public Row setColumn(StatusItem pStatusItem) {
      mColumns.put(mColumnNames.get(mCurrentColumnIndex++), pStatusItem);
      return this;
    }

    public Row setColumn(String pColumnName, String pValue) {
      mColumns.put(pColumnName, new StatusText(pValue));
      return this;
    }
    public Row setColumn(String pColumnName, String pValue, MessageLevel pMessageLevel) {
      mColumns.put(pColumnName, new StatusText(pValue, pMessageLevel));
      return this;
    }

    public Row setActionColumn(String pActionPrompt, BangHandler pBangHandler, Map<String, String> pParamMap) {
      mColumns.put(mColumnNames.get(mCurrentColumnIndex++), new StatusAction(pActionPrompt, pBangHandler, pParamMap));
      return this;
    }

    public Row setColumn(String pColumnName, StatusItem pStatusItem) {
      mColumns.put(pColumnName, pStatusItem);
      return this;
    }
  }
}
