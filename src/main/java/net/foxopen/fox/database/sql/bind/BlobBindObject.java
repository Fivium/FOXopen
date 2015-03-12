package net.foxopen.fox.database.sql.bind;

import java.sql.Blob;
import java.sql.SQLException;

import net.foxopen.fox.database.UCon;


public class BlobBindObject 
implements CloseableBindObject {
  
  private final BindDirection mBindDirection;
  private final Blob mBlob;
  
  public BlobBindObject(Blob pBlob) {
    mBindDirection = BindDirection.IN;
    mBlob = pBlob;
  }

  public BlobBindObject(Blob pBlob, BindDirection pBindDirection) {
    mBindDirection = pBindDirection;
    mBlob = pBlob;
  }

  @Override
  public Object getObject(UCon pUCon) {
    return mBlob;
  }

  @Override
  public String getObjectDebugString() {
    return mBlob == null ? null : "[BLOB]"; 
  }

  @Override
  public BindSQLType getSQLType() {
    return BindSQLType.BLOB;
  }

  @Override
  public BindDirection getDirection() {
    return mBindDirection;
  }

  @Override
  public void close() 
  throws SQLException {
    if(mBlob != null) {
      mBlob.free();
    }
  }

}
