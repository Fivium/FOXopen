package net.foxopen.fox.database.sql.out;

import java.io.Reader;

import java.sql.Blob;
import java.sql.Clob;
import java.sql.SQLException;
import java.sql.SQLXML;
import java.sql.Timestamp;


/**
 * Adaptor to allow JDBC ResultSets and CallableStatements to share the same interface for {@link SQLTypeConverter} conversion.
 */
public interface JDBCResultAdaptor {

  Object getObject(int pIndex) throws SQLException;

  SQLXML getSQLXML(int pIndex) throws SQLException;

  Timestamp getTimestamp(int pIndex) throws SQLException;

  Reader getCharacterStream(int pIndex) throws SQLException;

  String getString(int pIndex) throws SQLException;

  Clob getClob(int pIndex) throws SQLException;

  Blob getBlob(int pIndex) throws SQLException;

  Double getDouble(int pIndex) throws SQLException;

  Integer getInteger(int pIndex) throws SQLException;
}
