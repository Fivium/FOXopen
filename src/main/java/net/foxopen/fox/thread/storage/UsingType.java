package net.foxopen.fox.thread.storage;

import java.util.HashMap;
import java.util.Map;

import net.foxopen.fox.database.sql.bind.BindSQLType;


/**
 * Enum of bind variable using types which can be bound into storage location DatabaseStatements.
 */
public enum UsingType {
  
  /** An XPath to an element in a data DOM. */
  XPATH("XPATH", BindSQLType.STRING, true),
  /** A string which is unique to the current module call (not the current storage location or database statement). */
  UNIQUE("UNIQUE", BindSQLType.STRING, true),
  /** An unmodifiable string. */
  STATIC("STATIC", BindSQLType.STRING, true),
  /** The evaluated cache key of a working storage location. */
  CACHE_KEY("*not allowed in markup*", BindSQLType.STRING, false),
  /** The LOB to be written as a BLOB. */
  BLOB("DATA-BLOB", BindSQLType.BLOB, false),
  /** The LOB to be written as a CLOB. */
  CLOB("DATA-CLOB", BindSQLType.CLOB, false),
  /** The LOB to be written as SQLXML. */
  XMLTYPE("DATA-XMLTYPE", BindSQLType.XML, false),
  /** Metadata for a file upload (file storage locations only). */
  FILE_METADATA("FILE-METADATA-XMLTYPE", BindSQLType.XML, false);
  
  private final String mExternalName;
  private final BindSQLType mSQLType;
  private final boolean mAllowedInCacheKey;
  
  private static final Map<String, UsingType> gExternalNameToType = new HashMap<>();
  static {
    for(UsingType lType : values()) {
      gExternalNameToType.put(lType.mExternalName, lType);
    }
  }
  
  public static UsingType fromExternalString(String pExternalName) {
    return gExternalNameToType.get(pExternalName);
  }
  
  private UsingType(String pExternalName, BindSQLType pBindSQLType, boolean pAllowedInCacheKey) {
    mExternalName = pExternalName;
    mSQLType = pBindSQLType;
    mAllowedInCacheKey = pAllowedInCacheKey;
  }

  public BindSQLType getSQLType() {
    return mSQLType;
  }
  
  public boolean isString() {
    return this == XPATH || this == UNIQUE || this == STATIC || this == CACHE_KEY;
  }
  
  public boolean isAllowedInCacheKey() {
    return mAllowedInCacheKey;
  }
  
  public boolean isData() {
    return this == BLOB || this == CLOB || this == XMLTYPE;
  }

  public String getExternalName() {
    return mExternalName;
  }
}
