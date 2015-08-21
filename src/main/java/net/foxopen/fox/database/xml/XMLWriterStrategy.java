package net.foxopen.fox.database.xml;

import net.foxopen.fox.database.UCon;
import net.foxopen.fox.dom.DOM;
import net.foxopen.fox.entrypoint.FoxGlobals;

import java.sql.SQLException;
import java.sql.SQLXML;


/**
 * Strategy for controlling how a DOM is serialised to the database.
 */
public interface XMLWriterStrategy {

  /** Gets the default XMLWriterStrategy for this engine, based on the environment configuration */
  static XMLWriterStrategy engineDefaultInstance() {
    if(FoxGlobals.getInstance().getFoxEnvironment().getDatabaseProperties().isUseBinaryXMLWriter()) {
      return BinaryXMLWriter.instance();
    }
    else {
      return SQLXMLWriter.instance();
    }
  }

  SQLXML writeToObject(UCon pUCon, DOM pDOM)
  throws SQLException;

}
