package net.foxopen.fox.database.sql.bind;

import java.sql.Types;


/**
 * Subset of types in java.sql.types which are currently supported for binding by FOX.
 */
public enum BindSQLType {
  
  BLOB(Types.BLOB),
  CLOB(Types.CLOB),
  NUMBER(Types.NUMERIC),
  TIMESTAMP(Types.TIMESTAMP),
  STRING(Types.VARCHAR),
  XML(Types.SQLXML);
  
  private final int mSqlTypeCode;
  
  private BindSQLType(int pSqlTypeCode) {
    mSqlTypeCode = pSqlTypeCode;
  }

  public int getSqlTypeCode() {
    return mSqlTypeCode;
  }
}
