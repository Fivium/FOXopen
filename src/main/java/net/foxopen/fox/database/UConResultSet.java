package net.foxopen.fox.database;

import java.sql.ResultSet;
import java.sql.SQLException;

import net.foxopen.fox.database.parser.ParsedStatement;
import net.foxopen.fox.database.sql.ExecutableQuery;
import net.foxopen.fox.ex.ExInternal;
import net.foxopen.fox.plugin.api.database.FxpUConResultSet;


/**
 * Wrapper for a JDBC ResultSet as returned by a UCon statement execution method. Consumers must ensure they call {@link #close}
 * when they are done with the ResultSet. It is not sufficient to just close the JDBC ResultSet as there may be other resources
 * associated with the statement.
 * See {@link UCon#queryResultSet(ParsedStatement, UConBindMap)} for usage details.
 */
public class UConResultSet implements FxpUConResultSet {

  private final ExecutableQuery mExecutableQuery;

  UConResultSet(ExecutableQuery pExecutableQuery) {
    mExecutableQuery = pExecutableQuery;
  }
  
  /**
   * Gets the JDBC ResultSet from this UConResultSet.
   * @return ResultSet for reading rows/columns.
   */
  public ResultSet getResultSet() {
    return mExecutableQuery.getResultSet();
  }

  /**
   * Closes this statement, including its binds and result set.
   */
  public void close() {
    try {
      mExecutableQuery.close();
    }
    catch (SQLException e) {
      throw new ExInternal("Error closing UConResultSet", mExecutableQuery.convertError(e));
    }
  }
}
