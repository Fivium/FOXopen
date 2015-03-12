package net.foxopen.fox.database.sql.bind;

import java.sql.SQLException;

import net.foxopen.fox.database.UCon;


/**
 * Generic String BindObject for binding strings into statements.
 */
public class StringBindObject 
implements BindObject {

  private final BindDirection mBindDirection;
  private final String mEvaluatedString;

  public StringBindObject(String pEvaluatedString) {
    mBindDirection = BindDirection.IN;
    mEvaluatedString = pEvaluatedString;
  }

  public StringBindObject(String pEvaluatedString, BindDirection pBindDirection) {
    mBindDirection = pBindDirection;
    mEvaluatedString = pEvaluatedString;
  }

  @Override
  public Object getObject(UCon pUCon)
  throws SQLException {
    return mEvaluatedString;
  }

  @Override
  public String getObjectDebugString() {
    return mEvaluatedString;
  }
  
  protected String getEvaluatedString() {
    return mEvaluatedString;
  }

  @Override
  public BindSQLType getSQLType() {
    return BindSQLType.STRING;
  }

  @Override
  public BindDirection getDirection() {
    return mBindDirection;
  }
}
