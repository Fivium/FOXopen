package net.foxopen.fox.database.storage.lob;

import java.sql.Clob;
import java.sql.SQLException;

import net.foxopen.fox.ex.ExInternal;


public class ClobAccessor
extends LOBAccessor<Clob> {

  private final Clob mClob;

  ClobAccessor(Clob pClob) {
    mClob = pClob;
  }

  @Override
  public Clob getLOB() {
    return mClob;
  }

  @Override
  public void prepareLOBForWrite() {

  }

  @Override
  public void closeLOB() {
    try {
      mClob.free();
    }
    catch (SQLException e) {
      throw new ExInternal("Failed to close Clob", e);
    }
  }
}
