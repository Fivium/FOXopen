package net.foxopen.fox.database.sql.out;

import java.io.InputStream;

import java.sql.Blob;
import java.sql.SQLException;


public class BlobAdaptor
extends LOBAdaptor {
 
  private final Blob mBlob;

  BlobAdaptor(Blob pBlob) {
    mBlob = pBlob;
  }

  @Override
  public InputStream getInputStream() throws SQLException {
    return mBlob.getBinaryStream();
  }

  @Override
  public long getLength() throws SQLException {
    return mBlob.length();
  }
}
