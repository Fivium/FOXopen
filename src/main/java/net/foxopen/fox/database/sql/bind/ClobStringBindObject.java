package net.foxopen.fox.database.sql.bind;

import java.sql.Clob;
import java.sql.SQLException;

import net.foxopen.fox.database.UCon;


/**
 * BindObject for binding a Java String into an ExecutableStatement as a CLOB. A temporary clob is used to contain the
 * bound string and will be closed after the query is executed.
 */
public class ClobStringBindObject 
extends StringBindObject
implements CloseableBindObject {
  
  private Clob mTempClob = null;
  
  public ClobStringBindObject(String pEvaluatedString) {
    super(pEvaluatedString, BindDirection.IN);
  }

  public ClobStringBindObject(String pEvaluatedString, BindDirection pBindDirection) {
    super(pEvaluatedString, pBindDirection);
  }

  @Override
  public Object getObject(UCon pUCon) 
  throws SQLException {
    //Create a tempoary CLOB and set its contents to the evaluated string
    mTempClob = pUCon.getTemporaryClob();
    mTempClob.setString(1, getEvaluatedString());
    return mTempClob;
  }

  @Override
  public BindSQLType getSQLType() {
    return BindSQLType.CLOB;
  }

  @Override
  public void close() 
  throws SQLException {
    if(mTempClob != null) {
      mTempClob.free();
    }
  }
}
