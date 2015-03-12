package net.foxopen.fox.database.storage.lob;

import java.sql.SQLException;

import net.foxopen.fox.ex.ExInternal;

import oracle.sql.BLOB;


public class OracleBlobAccessor
extends LOBAccessor<BLOB> {

  private final BLOB mBlob;

  OracleBlobAccessor(BLOB pBlob) {
    mBlob = pBlob;
  }

  @Override
  public BLOB getLOB() {
    return mBlob;
  }

  @Override
  public void prepareLOBForWrite() {
    try {
      if(mBlob.isTemporary()) {
        throw new ExInternal("LOB validation failed: uploads to temporary BLOBs are not allowed");
      }
    }
    catch (SQLException e) {
      throw new ExInternal("Failed to determine if BLOB is temporary", e);
    }

    try {
      if(!mBlob.isOpen()) {
        mBlob.open(BLOB.MODE_READWRITE);
      }
    }
    catch (SQLException e) {
      throw new ExInternal("Failed to open BLOB in read/write mode - ensure LOB is selected FOR UPDATE", e);
    }
  }

  @Override
  public void closeLOB() {
    try {
      if(mBlob.isOpen()) {
        mBlob.close();
      }
    }
    catch (SQLException e) {
      throw new ExInternal("Failed to close BLOB", e);
    }
  }
}
