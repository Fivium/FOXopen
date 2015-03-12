package net.foxopen.fox.database.sql.out;

import java.io.InputStream;

import java.sql.Clob;
import java.sql.SQLException;


public class ClobAdaptor 
extends LOBAdaptor  {
  
  private final Clob mClob;

  ClobAdaptor(Clob pClob) {
    mClob = pClob;
  }

  @Override
  public InputStream getInputStream() throws SQLException {
    return mClob.getAsciiStream();
  }

  @Override
  public long getLength() throws SQLException {
    return mClob.length();
  }
}
