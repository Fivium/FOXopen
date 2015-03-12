package net.foxopen.fox.dbinterface.deliverer;

import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Types;

import java.util.List;

import net.foxopen.fox.XFUtil;
import net.foxopen.fox.database.sql.out.JDBCResultAdaptor;
import net.foxopen.fox.database.sql.out.SQLTypeConverter;
import net.foxopen.fox.dbinterface.InterfaceQuery;
import net.foxopen.fox.dom.DOM;
import net.foxopen.fox.ex.ExBadPath;
import net.foxopen.fox.ex.ExInternal;
import net.foxopen.fox.ex.ExTooFew;
import net.foxopen.fox.ex.ExTooMany;


/**
 * Row provider for AUGMENT query mode. This attempts to locate an existing row based on the key defined on the interface
 * query, and only creates new rows when an existing row cannot be found.
 */
class AugmentRowProvider
implements QueryRowProvider {

  private final InterfaceQuery mInterfaceQuery;
  private final DOM mMatchNode;

  private int[] mColumnPositionsForKeyElements;

  public AugmentRowProvider(InterfaceQuery pInterfaceQuery, DOM pMatchNode) {
    //Validate that the associated query has primary key elements defined
    if(pInterfaceQuery.getKeyElementNames() == null) {
      throw new ExInternal("Cannot create a key-based result deliverer for " + pInterfaceQuery.getQualifiedName() + " as it has no primary keys defined");
    }

    mInterfaceQuery = pInterfaceQuery;
    mMatchNode = pMatchNode;
  }

  @Override
  public void prepareForDelivery(ResultSetMetaData pResultSetMeta)
  throws SQLException {
    List<String> lKeyElementNames = mInterfaceQuery.getKeyElementNames();
    mColumnPositionsForKeyElements = new int[lKeyElementNames.size()];
    KEY_LOOP:
    for(int lKeyIdx = 0; lKeyIdx < lKeyElementNames.size(); lKeyIdx++) {
      //Search the metadata for the key column and record its index for future lookups
      for(int lColIdx = 1; lColIdx <= pResultSetMeta.getColumnCount(); lColIdx++) {
        if(pResultSetMeta.getColumnName(lColIdx).equals(lKeyElementNames.get(lKeyIdx))) {
          mColumnPositionsForKeyElements[lKeyIdx] = lColIdx;
          continue KEY_LOOP;
        }
      }
      //Search completed without finding the column
      throw new ExInternal("Query " + mInterfaceQuery.getQualifiedName() + " does not return column for key " + lKeyElementNames.get(lKeyIdx));
    }
  }

  @Override
  public DOM getTargetRow(JDBCResultAdaptor pResultSet) {

    DOM lTargetRowContainer;
    String lTargetPath = mInterfaceQuery.getTargetPath();
    try {
      StringBuilder lSeekRecordXPath = new StringBuilder(lTargetPath);
      List<String> lKeyElementNames = mInterfaceQuery.getKeyElementNames();

      //Construct seek path based on target path and predicates of keys
      for(int i = 0; i < lKeyElementNames.size(); i++) {

        String lKeyColumnName = lKeyElementNames.get(i);

        int lColIdxForKey = mColumnPositionsForKeyElements[i];
        //Result set should be in the correct position
        String lColKeyValue;
        try {
          lColKeyValue = SQLTypeConverter.getValueAsString(pResultSet, lColIdxForKey, Types.VARCHAR);
        }
        catch (SQLException e) {
          throw new ExInternal("Failed to convert key column " + lKeyColumnName + " to a string", e);
        }

        //Escape single quotes in the key value string ('' is an XPath 2.0 escape sequence)
        lColKeyValue.replace("'", "''");

        lSeekRecordXPath.append("[" + lKeyColumnName + " = '" + lColKeyValue + "']");
      }

      lTargetRowContainer = mMatchNode.xpath1E(lSeekRecordXPath.toString());
    }
    catch (ExBadPath e) {
      throw new ExInternal("Bad key XPath search ", e);
    }
    catch (ExTooFew e) {
      // Create new record node when missing
      StringBuffer lSB = new StringBuffer(lTargetPath);
      String lElementName = XFUtil.pathPopTail(lSB);
      if(lSB.length() != 0) {
        try {
          lTargetRowContainer = mMatchNode.getCreateXpath1E(lSB.toString());
        }
        catch (ExTooMany x) {
          throw x.toUnexpected("Error creating target record: "+lTargetPath);
        }
        catch (ExBadPath x) {
          throw x.toUnexpected("Error creating target record: "+lTargetPath);
        }
      }
      else {
        lTargetRowContainer = mMatchNode;
      }
      lTargetRowContainer = lTargetRowContainer.addElem(lElementName);
    }
    catch (ExTooMany e) {
      throw new ExInternal("Duplicate DOM matches found for interface key ", e);
    }

    return lTargetRowContainer;
  }

  @Override
  public void finaliseRow(int pRowNumber, DOM pRow) {
  }
}
