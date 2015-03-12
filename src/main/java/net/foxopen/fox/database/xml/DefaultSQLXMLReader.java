package net.foxopen.fox.database.xml;

import net.foxopen.fox.dom.DOM;

import java.sql.SQLException;
import java.sql.SQLXML;


/**
 * A SQLXMLReader which reads the XMLType's serialised character stream, then passes it to the DOM constructor.
 */
public class DefaultSQLXMLReader
implements XMLReaderStrategy {

  private static final DefaultSQLXMLReader INSTANCE = new DefaultSQLXMLReader();

  public static XMLReaderStrategy instance() {
    return INSTANCE;
  }

  private DefaultSQLXMLReader() {}

  @Override
  public DOM read(SQLXML pSQLXML)
  throws SQLException {
    //We have to use the character stream value here, as the XMLType does not report the binary stream correctly
    return DOM.createDocument(pSQLXML.getCharacterStream());
  }
}
