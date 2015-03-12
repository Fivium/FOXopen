package net.foxopen.fox.database.xml;

import net.foxopen.fox.dom.DOM;
import net.foxopen.fox.entrypoint.FoxGlobals;
import net.foxopen.fox.ex.ExInternal;
import nu.xom.Builder;
import nu.xom.ParsingException;
import oracle.xdb.XMLType;
import oracle.xml.binxml.BinXMLDecoder;
import oracle.xml.binxml.BinXMLException;
import org.xml.sax.ContentHandler;
import org.xml.sax.DTDHandler;
import org.xml.sax.EntityResolver;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.SAXNotSupportedException;
import org.xml.sax.XMLReader;
import org.xml.sax.ext.DeclHandler;
import org.xml.sax.ext.LexicalHandler;

import java.io.IOException;
import java.sql.SQLException;
import java.sql.SQLXML;

/**
 * XMLReader which is specialised for reading Oracle Binary XML. This class is required because as of JDBC 11.2.0.4 the
 * SQLXML API for binary XML columns introduces extra whitespace which could cause problems for module developers (i.e.
 * extra text nodes appearing where not expected). The workaround is to use the Oracle Binary XML API for Java to read
 * SAX events directly from the underlying XMLType in order to construct a DOM. This does not appear to introduce extra
 * whitespace.
 */
public class OracleBinaryXMLReader
implements XMLReaderStrategy {

  private static final OracleBinaryXMLReader INSTANCE = new OracleBinaryXMLReader();

  /**
   * Gets the singleton instance of an OracleBinaryXMLReader. Note: this method may return a DefaultSQLXMLReader if
   * binary XML reading is disabled in the resource master (some versions of Oracle do not support this read method).
   * @return
   */
  public static XMLReaderStrategy instance() {
    if(FoxGlobals.getInstance().getFoxEnvironment().getDatabaseProperties().isUseBinaryXMLReader()) {
      return INSTANCE;
    }
    else {
      return DefaultSQLXMLReader.instance();
    }
  }

  private OracleBinaryXMLReader() {
  }

  @Override
  public DOM read(SQLXML pSQLXML) throws SQLException {

    XMLType lXMLType = (XMLType) pSQLXML;
    Builder lBuilder;
    try {
      //Construct a new XOM builder with our ersatz parser implementation
      lBuilder = new Builder(new XOMBinaryXMLParser(lXMLType.getBinXMLStream().getDecoder()));
    }
    catch (BinXMLException e) {
      throw new ExInternal("Failed to read Binary XML", e);
    }

    try {
      //Invoke build with an empty systemId - this isn't used so it doesn't matter. XOM will then invoke parse() on the parser.
      return DOM.createDocumentFromNode(lBuilder.build(""));
    }
    catch (IOException | ParsingException e) {
      throw new ExInternal("Error decoding binary XML", e);
    }
  }

  /**
   * Bridge between an Oracle BinXMLDecoder and the XOM parser. We want to use the XOM ContentHandler to read XML because
   * it is known to be safe. This class allows us to retrieve the XOM ContentHandler (which is set when a XOM Builder is
   * constructed) and pass it off to the BinXMLDecoder.<br/><br/>
   *
   * This is ultimately a hack; we are effectively tricking XOM into giving us its ContentHandler. This is dependent
   * on the current implementation of XOM so may break if XOM is ever upgraded.
   */
  private static class XOMBinaryXMLParser
  implements XMLReader {

    private final BinXMLDecoder mDecoder;
    //This MUST be a XOMContentHandler, which also implements the other required Handler interfaces
    private ContentHandler mContentHandler;
    private ErrorHandler mErrorHandler;

    public XOMBinaryXMLParser(BinXMLDecoder pDecoder) {
      mDecoder = pDecoder;
    }

    @Override
    public boolean getFeature(String pName) throws SAXNotRecognizedException, SAXNotSupportedException {
      //Not used by XOM
      return false;
    }

    @Override
    public void setFeature(String pName, boolean pValue) throws SAXNotRecognizedException, SAXNotSupportedException {
    }

    @Override
    public Object getProperty(String pName) throws SAXNotRecognizedException, SAXNotSupportedException {
      //Not used by XOM
      return null;
    }

    @Override
    public void setProperty(String pName, Object pValue) throws SAXNotRecognizedException, SAXNotSupportedException {
      //Used by XOM to set lexical handler - we do this manually in setContentHandler
    }

    @Override
    public void setEntityResolver(EntityResolver pResolver) {
      //Not used by XOM
    }

    @Override
    public EntityResolver getEntityResolver() {
      return null;
    }

    @Override
    public void setDTDHandler(DTDHandler pHandler) {
      mDecoder.setDTDHandler((DTDHandler) mContentHandler);
    }

    @Override
    public DTDHandler getDTDHandler() {
      return (DTDHandler) mContentHandler;
    }

    @Override
    public void setContentHandler(ContentHandler pHandler) {
      //Called by XOM during builder construction - grab a reference to the handler so we can pass it to the decoder
      mContentHandler = pHandler;
      mDecoder.setLexicalHandler((LexicalHandler) mContentHandler);
      mDecoder.setDeclHandler((DeclHandler) mContentHandler);
    }

    @Override
    public ContentHandler getContentHandler() {
      return mContentHandler;
    }

    @Override
    public void setErrorHandler(ErrorHandler pHandler) {
      mErrorHandler = pHandler;
    }

    @Override
    public ErrorHandler getErrorHandler() {
      return mErrorHandler;
    }

    private void decode() {
      try {
        mDecoder.decode(mContentHandler, mErrorHandler);
      }
      catch (BinXMLException e) {
        throw new ExInternal("Error decoding binary XML", e);
      }
    }

    @Override
    public void parse(InputSource pInput) throws IOException, SAXException {
      decode();
    }

    @Override
    public void parse(String pSystemId) throws IOException, SAXException {
      decode();
    }
  }
}
