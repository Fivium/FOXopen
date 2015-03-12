package net.foxopen.fox.database.sql.bind;

import net.foxopen.fox.database.UCon;

public class NumericBindObject
implements BindObject {
  
  private final BindDirection mBindDirection;
  private final Number mBindNumber;

  public NumericBindObject(Number pBindNumber, BindDirection pBindDirection) {
    mBindDirection = pBindDirection;
    mBindNumber = pBindNumber;
  }
  
  public NumericBindObject(Number pBindNumber) {
    mBindDirection = BindDirection.IN;
    mBindNumber = pBindNumber;
  }

  @Override
  public Object getObject(UCon pPUCon) {
    return mBindNumber;
  }

  @Override
  public String getObjectDebugString() {
    return mBindNumber != null ? mBindNumber.toString() : null;
  }

  @Override
  public BindSQLType getSQLType() {
    return BindSQLType.NUMBER;
  }

  @Override
  public BindDirection getDirection() {
    return mBindDirection;
  }
}
