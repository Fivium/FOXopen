package net.foxopen.fox.database.sql.out;

import java.io.Reader;

import java.sql.Blob;
import java.sql.CallableStatement;
import java.sql.Clob;
import java.sql.SQLException;
import java.sql.SQLXML;
import java.sql.Timestamp;


/**
 * Adaptor for a JDBC CallableStatement.
 */
public class CallableStatementAdaptor
implements JDBCResultAdaptor {

  private final CallableStatement mCallableStatement;

  public CallableStatementAdaptor(CallableStatement pCallableStatement) {
    mCallableStatement = pCallableStatement;
  }

  @Override
  public Object getObject(int pIndex) throws SQLException {
    return mCallableStatement.getObject(pIndex);
  }

  @Override
  public SQLXML getSQLXML(int pIndex) throws SQLException {
    return mCallableStatement.getSQLXML(pIndex);
  }

  @Override
  public Timestamp getTimestamp(int pIndex) throws SQLException {
    return mCallableStatement.getTimestamp(pIndex);
  }

  @Override
  public Reader getCharacterStream(int pIndex) throws SQLException {
    return mCallableStatement.getCharacterStream(pIndex);
  }

  @Override
  public String getString(int pIndex) throws SQLException {
    return mCallableStatement.getString(pIndex);
  }

  @Override
  public Clob getClob(int pIndex) throws SQLException {
    return mCallableStatement.getClob(pIndex);
  }

  @Override
  public Blob getBlob(int pIndex) throws SQLException {
    return mCallableStatement.getBlob(pIndex);
  }

  @Override
  public Double getDouble(int pIndex) throws SQLException {
    return mCallableStatement.getDouble(pIndex);
  }

  @Override
  public Integer getInteger(int pIndex) throws SQLException {
    return mCallableStatement.getInt(pIndex);
  }

}
