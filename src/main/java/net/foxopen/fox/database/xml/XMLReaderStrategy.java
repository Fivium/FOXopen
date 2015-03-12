package net.foxopen.fox.database.xml;

import java.sql.SQLException;
import java.sql.SQLXML;

import net.foxopen.fox.dom.DOM;


public interface XMLReaderStrategy {
  
  /**
   * Reads from a SQLXML result object into a FOX DOM.
   * @param pSQLXML
   * @return
   */
  public DOM read(SQLXML pSQLXML)
  throws SQLException;
}
