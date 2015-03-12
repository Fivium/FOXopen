package net.foxopen.fox.database.sql.out;

import java.io.InputStream;

import java.sql.Blob;
import java.sql.Clob;
import java.sql.SQLException;
import java.sql.SQLXML;

import net.foxopen.fox.ex.ExInternal;


/**
 * Adaptor so JDBC LOB types can share a common interface. Consumers can retrieve binary streams for underlying locators.
 * This supercedes the Oracle-specific "Datum" class which was a shared superclass of BLOBs and CLOBs.
 */
public abstract class LOBAdaptor {  
  
  public static LOBAdaptor getAdaptor(Object pLOB) {
    if(pLOB instanceof Blob) {
      return new BlobAdaptor((Blob) pLOB);
    }
    else if(pLOB instanceof Clob) {
      return new ClobAdaptor((Clob) pLOB);
    }
    else if(pLOB instanceof SQLXML) {
      return new SQLXMLAdaptor((SQLXML) pLOB);
    }
    else {
      throw new ExInternal("Don't have an appropriate adaptor for a " + pLOB.getClass().getName());
    }
  }
  
  public abstract InputStream getInputStream() throws SQLException;
  
  public abstract long getLength() throws SQLException;
}
