package net.foxopen.fox.database.storage.dom;

import net.foxopen.fox.database.UCon;
import net.foxopen.fox.database.sql.bind.BindSQLType;
import net.foxopen.fox.dom.DOM;

/**
 * Strategy for WorkDoc DOM access. Implementations should use the optimal approach for DOM access based on their target
 * XML storage type.
 */
interface XMLWorkDocDOMAccessor {  
  
  /**
   * Reads the DOM from the Database into memory.
   * @param pUCon Current UCon.
   * @return
   */
  DOM retrieveDOM(UCon pUCon);
  
  /**
   * This method is called before an insert or update is performed. Implementors may perform any preparation they require here.
   * @param pUCon Current UCon.
   */
  void prepareForDML(UCon pUCon, DOM pDOM);
  
  /**
   * Returns the change number defined in the change number attribute, or null if no change number is defined on the XML 
   * document. This method is only called when {@link #isLocatorEmpty} returns false.
   * @return Change number string or null.
   */
  String readChangeNumber(UCon pUCon);
  
  /**
   * Opens the locator for the selected XML LOB. Implementations must handle the LOB being null (in the case that a row
   * is available but the LOB column is null).
   */
  void openLocator(UCon pUCon, Object pLOB);  
  
  /**
   * Returns true if and only if the LOB locator is not null and empty. Returns false if the locator is null.
   * @param pUCon
   * @return
   */
  boolean isLocatorEmpty(UCon pUCon);
  
  /**
   * Returns true if the LOB locator column is null. If the locator is not null but empty this method should return false.
   * @param pUCon
   * @return
   */
  boolean isLocatorNull(UCon pUCon);
  
  /**
   * This method is called when the WorkDoc is closed to allow implementors to perform any clean up, release temporary
   * resources, etc.
   * @param pUCon
   */
  void closeLocator(UCon pUCon);
  
  /**
   * Gets an XML LOB object for binding into an insert/update statement. Implementors may use an existing cached object
   * if available, or create a new one based on pDOM.
   * @param pUCon Current UCon.
   * @param pBindTypeRequired Target LOB type.
   * @param pDOM The current WorkDoc DOM.
   * @return XML LOB (CLOB, BLOB or XMLType).
   */
  Object getLOBForBinding(UCon pUCon, BindSQLType pBindTypeRequired, DOM pDOM);
  
}
