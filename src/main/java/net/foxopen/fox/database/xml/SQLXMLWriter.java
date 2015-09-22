package net.foxopen.fox.database.xml;

import net.foxopen.fox.database.UCon;
import net.foxopen.fox.dom.DOM;
import net.foxopen.fox.entrypoint.FoxGlobals;

import java.sql.SQLException;
import java.sql.SQLXML;


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

    //We may need to send the XML as character data if the database is unable to handle byte stream conversion (this is the case on non-UTF databases).
    //However byte stream writing should be preferred as it is more efficient and 'correct'.

    if(FoxGlobals.getInstance().getFoxEnvironment().getDatabaseProperties().isSendBytesToStandardXMLWriter()) {
      //Write to the BINARY stream so the database can perform XML-aware character set translation if it needs to.
      pDOM.outputNodeToOutputStream(lSQLXML.setBinaryStream(), false, false);
    }
    else {
      //Convert XML bytes to encoded character data before sending it to the database if configured to
      pDOM.outputNodeToWriter(lSQLXML.setCharacterStream(), false);
    }

    return lSQLXML;
  }
}
