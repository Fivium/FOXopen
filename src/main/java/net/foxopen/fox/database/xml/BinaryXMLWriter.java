package net.foxopen.fox.database.xml;

import java.sql.SQLException;
import java.sql.SQLXML;

import net.foxopen.fox.database.UCon;
import net.foxopen.fox.dom.DOM;
import net.foxopen.fox.ex.ExInternal;

import nu.xom.converters.SAXConverter;

import oracle.jdbc.OracleConnection;

import oracle.xdb.XMLType;

import oracle.xml.binxml.BinXMLEncoder;
import oracle.xml.binxml.BinXMLException;
import oracle.xml.binxml.BinXMLStream;

import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;


public class BinaryXMLWriter
implements XMLWriterStrategy {
  
  private static final BinaryXMLWriter INSTANCE = new BinaryXMLWriter();
  
  public static XMLWriterStrategy instance() {
    return INSTANCE;
  }
   
  private BinaryXMLWriter() {}

  @Override
  public SQLXML writeToObject(UCon pUCon, DOM pDOM) 
  throws SQLException {
  
    //Create an empty XML type and declare as binary XML
    XMLType lXMLType = XMLType.createXML(pUCon.getJDBCConnection().unwrap(OracleConnection.class), (String) null);    
    lXMLType.setBinaryXML();
    
    try {
      //Create a new binary stream
      BinXMLStream lBinXMLStream = pUCon.getBinXMLProcessor().createBinXMLStream(lXMLType.setBinaryStream());
      BinXMLEncoder lBinXMLEncoder = lBinXMLStream.getEncoder();
      ContentHandler lContentHandler = lBinXMLEncoder.getContentHandler();    
      
      //Pass SAX events to the stream from XOM
      SAXConverter lSAXConverter = new SAXConverter(lContentHandler);
      lSAXConverter.convert(pDOM.getDocControl().getDocumentNode());
      
      return lXMLType;
    }
    catch (BinXMLException e) {
      throw new ExInternal("Error creating binary XML stream", e);
    }
    catch (SAXException e) {
      throw new ExInternal("Error converting DOM to binary XML", e);
    }

  }

}
