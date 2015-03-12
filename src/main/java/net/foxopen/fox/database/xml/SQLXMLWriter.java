package net.foxopen.fox.database.xml;

import java.sql.SQLException;
import java.sql.SQLXML;

import net.foxopen.fox.database.UCon;
import net.foxopen.fox.dom.DOM;


public class SQLXMLWriter
implements XMLWriterStrategy {

  private static final SQLXMLWriter INSTANCE = new SQLXMLWriter();

  public static SQLXMLWriter instance() {
    return INSTANCE;
  }

  private SQLXMLWriter() {}

  @Override
  public SQLXML writeToObject(UCon pUCon, DOM pDOM)
  throws SQLException {

    SQLXML lSQLXML = pUCon.getJDBCConnection().createSQLXML();
    //Write to the BINARY stream so the database can perform XML-aware character set translation if it needs to.
    pDOM.outputNodeToOutputStream(lSQLXML.setBinaryStream(), false, false);

    return lSQLXML;
  }
}
