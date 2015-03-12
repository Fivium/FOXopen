package net.foxopen.fox.database.sql.bind;

import java.sql.Timestamp;

import java.util.Date;

import net.foxopen.fox.database.UCon;


/**
 * Wrapper for a JDBC timestamp object for binding into an ExecutableStatement.
 */
public class TimestampBindObject
implements BindObject {
  
  private final BindDirection mBindDirection;
  private final Timestamp mTimestamp; //Can be null
  
  /**
   * Converts a java.util.Date into a SQL timestamp for binding as such.
   * @param pDate Date to convert.
   */
  public TimestampBindObject(Date pDate) {
    mBindDirection = BindDirection.IN;
    mTimestamp = new Timestamp(pDate.getTime());
  }
  
  /**
   * Converts a java.util.Date into a SQL timestamp for binding as such.
   * @param pDate Date to convert.
   * @param pBindDirection
   */
  public TimestampBindObject(Date pDate, BindDirection pBindDirection) {
    mBindDirection = pBindDirection;
    mTimestamp = new Timestamp(pDate.getTime());
  }

  public TimestampBindObject(Timestamp pTimestamp) {
    mBindDirection = BindDirection.IN;
    mTimestamp = pTimestamp;
  }

  public TimestampBindObject(Timestamp pTimestamp, BindDirection pBindDirection) {
    mBindDirection = pBindDirection;
    mTimestamp = pTimestamp;
  }

  @Override
  public Object getObject(UCon pUCon) {
    return mTimestamp;
  }

  @Override
  public String getObjectDebugString() {
    return mTimestamp == null ? null : mTimestamp.toString();
  }  

  @Override
  public BindSQLType getSQLType() {
    return BindSQLType.TIMESTAMP;
  }

  @Override
  public BindDirection getDirection() {
    return mBindDirection;
  }
  
}
