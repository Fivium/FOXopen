package net.foxopen.fox.dbinterface.deliverer;

import java.sql.ResultSetMetaData;
import java.sql.SQLException;

import net.foxopen.fox.database.sql.out.JDBCResultAdaptor;
import net.foxopen.fox.dom.DOM;
import net.foxopen.fox.ex.ExTooMany;


/**
 * RowProviders provide DOM complex types (i.e. rows) for population from the query result's rows.
 */
public interface QueryRowProvider {

  /**
   * Prepares this RowProvider for generating target rows. Any required initialisation should be done in this method.
   * @param pResultSetMeta Metadata of the newly executed query.
   * @throws SQLException
   */
  public void prepareForDelivery(ResultSetMetaData pResultSetMeta) throws SQLException;

  /**
   * Gets or creates a new target row container for the query to populate. Key-based row providers may read the ResultSet
   * to determine the key values, but MUST NOT alter the state of the ResultSet.
   * @return Row container DOM.
   * @throws ExTooMany If a path problem occurs.
   */
  public DOM getTargetRow(JDBCResultAdaptor pResultSet) throws ExTooMany;

  /**
   * Performs any required processing on a fully populated row DOM.
   * @param pRowNumber Query row number of the row (1-based).
   * @param pRow Populated DOM containing row contents, as read from the query.
   */
  public void finaliseRow(int pRowNumber, DOM pRow);
}
