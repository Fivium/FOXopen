package net.foxopen.fox.database.sql.out;

import java.io.InputStream;

import java.sql.SQLException;
import java.sql.SQLXML;


public class SQLXMLAdaptor
extends LOBAdaptor {
  
  private final SQLXML mSQLXML;

  SQLXMLAdaptor(SQLXML pSQLXML) {
    mSQLXML = pSQLXML;
  }

  @Override
  public InputStream getInputStream() throws SQLException {
    return mSQLXML.getBinaryStream();
  }

  @Override
  public long getLength() {
    throw new UnsupportedOperationException("Needs implementing: getLength for SQLXML");
    //TODO need to loop through binary stream and manually count
  }
}
