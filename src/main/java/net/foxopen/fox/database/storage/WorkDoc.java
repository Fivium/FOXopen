package net.foxopen.fox.database.storage;

import net.foxopen.fox.database.UCon;
import net.foxopen.fox.database.sql.bind.BindSQLType;

/**
 * A WorkDoc is an interface for accessing a LOB on the database, typically using a WorkingStorageLocation to control
 * access. Implementors may hold LOB locators for the duration of a transaction, or cache data to avoid unnecessary
 * database access.
 */
public interface WorkDoc {
  
  /**
   * Retrieves the WorkDoc's referenced LOB for it to be bound into an ExecutableStatement.
   * @param pUCon UCon to be used for LOB retrieval if required.
   * @param pBindTypeRequired SQL datatype the consumer is expecting.
   * @return LOB object for binding.
   */
  public Object getLOBForBinding(UCon pUCon, BindSQLType pBindTypeRequired);
  
}
