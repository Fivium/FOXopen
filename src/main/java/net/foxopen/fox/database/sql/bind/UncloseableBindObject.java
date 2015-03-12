package net.foxopen.fox.database.sql.bind;

import java.sql.SQLException;

import net.foxopen.fox.database.UCon;


/**
 * Decorator for a BindObject which would normally be closeable but should not be closed in some circumstances, i.e. a LOB
 * locator which is reused in multiple queries.
 */
public class UncloseableBindObject
implements BindObject {
  
  private final BindObject mWrappedBindObject;

  public UncloseableBindObject(BindObject pWrappedBindObject) {
    mWrappedBindObject = pWrappedBindObject;
  }

  @Override
  public Object getObject(UCon pUCon) 
  throws SQLException {
    return mWrappedBindObject.getObject(pUCon);
  }

  @Override
  public String getObjectDebugString() {
    return mWrappedBindObject.getObjectDebugString();
  }

  @Override
  public BindSQLType getSQLType() {
    return mWrappedBindObject.getSQLType();
  }

  @Override
  public BindDirection getDirection() {
    return mWrappedBindObject.getDirection();
  }
}
