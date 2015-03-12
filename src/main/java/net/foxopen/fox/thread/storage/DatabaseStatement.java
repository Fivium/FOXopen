package net.foxopen.fox.thread.storage;

import java.util.List;

import net.foxopen.fox.database.parser.ParsedStatement;
import net.foxopen.fox.database.parser.StatementParser;
import net.foxopen.fox.dom.DOM;
import net.foxopen.fox.ex.ExCardinality;
import net.foxopen.fox.ex.ExInternal;
import net.foxopen.fox.ex.ExModule;
import net.foxopen.fox.ex.ExParser;


/**
 * Wrapper for a parsed SQL statement and its {@link StorageLocationBind}s. Storage locations may have named or unnamed
 * binds externally, but binds will always have a name internally. Use this class to retrieve the correct StorageLocationBind
 * object for a bind name or index.
 */
public class DatabaseStatement {

  private final ParsedStatement mParsedStatement;
  private final List<StorageLocationBind> mBindList;
  private final boolean mNamedBinds;
  
  /**
   * Constructs a new DatabaseStatement from a definition DOM.
   * @param pDefinitionDOM
   * @param pSQLElementName
   * @return
   * @throws ExModule
   */
  static DatabaseStatement createFromDOM(DOM pDefinitionDOM, String pSQLElementName, StatementType pStatementType, String pStorageLocatioName) 
  throws ExModule {
    
    String lSQL;
    try {
      lSQL = pDefinitionDOM.get1S(pSQLElementName);
    }
    catch (ExCardinality e) {
      throw new ExModule("Invalid storage location definition: expected exactly one element called " + pSQLElementName, e);
    }
    
    //TODO named bind logic
    return new DatabaseStatement(lSQL, StorageLocationBind.createListFromDOMDefinition(pDefinitionDOM), false, pStatementType, pStorageLocatioName);
    
  }
  
  /**
   * Constructs a new DatabaseStatement from an internal SQL statement.
   * @param pSQL SQL statement.
   * @param pBindList List of binds for the statement.
   * @return
   */
  public static DatabaseStatement createInternalStatement(String pSQL, List<StorageLocationBind> pBindList, StatementType pStatementType, String pStorageLocatioName) {    
    return new DatabaseStatement(pSQL, pBindList, false, pStatementType, pStorageLocatioName);    
  }

  private DatabaseStatement(String pSQL, List<StorageLocationBind> pBindList, boolean pNamedBinds, StatementType pStatementType, String pStorageLocatioName) {
    try {
      mParsedStatement = StatementParser.parse(pSQL, "Storage Location " + pStorageLocatioName + " [" + pStatementType.toString().toLowerCase() + "]");
    }
    catch (ExParser e) {
      //TODO better exception (needs context)
      throw new ExInternal("Failed to parse SQL " + pSQL, e);
    }
    mBindList = pBindList;
    mNamedBinds = pNamedBinds;
  }

  public ParsedStatement getParsedStatement() {
    return mParsedStatement;
  }

  public List<StorageLocationBind> getBindList() {
    return mBindList;
  }
  
  /**
   * Gets the StorageLocationBind for the given bind name and/or index, depending on whether named binds are used by this
   * DatabaseStatement.
   * @param pName Bind name as it appears in the SQL statement. Can be null if the statement has no named binds.
   * @param pIndex Bind index in the SQL statement.
   * @return The StorageLocationBind for the given name/index.
   */
  public StorageLocationBind getBind(String pName, int pIndex) {
    if(hasNamedBinds()) {
      //TODO
      throw new UnsupportedOperationException("Need to implement named bind logic");
    }
    else {
      try {
        return mBindList.get(pIndex);  
      }
      catch (IndexOutOfBoundsException e) {
        throw new ExInternal("No bind variable defined for bind variable ':" + (pIndex + 1) + "'", e);
      }
    }
  }

  public boolean hasNamedBinds() {
    return mNamedBinds;
  }
}
