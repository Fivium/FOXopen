package net.foxopen.fox.plugin.api.database;

import java.sql.ResultSet;

public interface FxpUConResultSet {
  /**
   * Gets the JDBC ResultSet from this UConResultSet.
   * @return ResultSet for reading rows/columns.
   */
  ResultSet getResultSet();

  /**
   * Closes this statement, including its binds and result set.
   */
  void close();
}
