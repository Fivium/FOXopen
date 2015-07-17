package net.foxopen.fox.database.sql.out;

import java.io.Reader;

import java.sql.Blob;
import java.sql.Clob;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLXML;
import java.sql.Timestamp;


/**
 * Adaptor for a JDBC ResultSet.
 */
public class ResultSetAdaptor
implements JDBCResultAdaptor {

  private final ResultSet mResultSet;

  public ResultSetAdaptor(ResultSet pResultSet) {
    mResultSet = pResultSet;
  }

  @Override
  public Object getObject(int pIndex) throws SQLException {
    return mResultSet.getObject(pIndex);
  }

  @Override
  public SQLXML getSQLXML(int pIndex) throws SQLException {
    return mResultSet.getSQLXML(pIndex);
  }

  @Override
  public Timestamp getTimestamp(int pIndex) throws SQLException {
    return mResultSet.getTimestamp(pIndex);
  }

  @Override
  public Reader getCharacterStream(int pIndex) throws SQLException {
    return mResultSet.getCharacterStream(pIndex);
  }

  @Override
  public String getString(int pIndex) throws SQLException {
    return mResultSet.getString(pIndex);
  }

  @Override
  public Clob getClob(int pIndex) throws SQLException {
    return mResultSet.getClob(pIndex);
  }

  @Override
  public Blob getBlob(int pIndex) throws SQLException {
    return mResultSet.getBlob(pIndex);
  }

  @Override
  public Double getDouble(int pIndex) throws SQLException {
    return mResultSet.getDouble(pIndex);
  }

  @Override
  public Integer getInteger(int pIndex) throws SQLException {
    return mResultSet.getInt(pIndex);
  }
}
