package net.foxopen.fox.database.sql.bind;

import java.sql.Clob;
import java.sql.SQLException;

import net.foxopen.fox.database.UCon;


/**
 * BindObject for binding an existing Clob, which may be a temporary CLOB or a LOB locator to a permanent CLOB.
 */
public class ClobBindObject 
implements CloseableBindObject {
  
  private final BindDirection mBindDirection;
  private final Clob mClob;
  
  public ClobBindObject(Clob pClob) {
    mBindDirection = BindDirection.IN;
    mClob = pClob;
  }

  public ClobBindObject(Clob pClob, BindDirection pBindDirection) {
    mBindDirection = pBindDirection;
    mClob = pClob;
  }

  @Override
  public Object getObject(UCon pUCon) {
    return mClob;
  }

  @Override
  public String getObjectDebugString() {
    return mClob == null ? null : "[CLOB]"; 
  }

  @Override
  public BindSQLType getSQLType() {
    return BindSQLType.CLOB;
  }

  @Override
  public BindDirection getDirection() {
    return mBindDirection;
  }

  @Override
  public void close() 
  throws SQLException {
    if(mClob != null) {
      mClob.free();
    }
  }

}
