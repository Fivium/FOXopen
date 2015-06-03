package net.foxopen.fox.database.storage.dom;

import net.foxopen.fox.XFUtil;
import net.foxopen.fox.database.UCon;
import net.foxopen.fox.database.sql.bind.BindSQLType;
import net.foxopen.fox.database.xml.BinaryXMLWriter;
import net.foxopen.fox.database.xml.OracleBinaryXMLReader;
import net.foxopen.fox.dom.DOM;
import net.foxopen.fox.ex.ExInternal;
import net.foxopen.fox.track.Track;
import oracle.xdb.XMLType;
import org.w3c.dom.Document;

import java.sql.SQLException;


/**
 * XMLWorkDoc implementation for accessing an Oracle Binary XML LOB.
 * <br/><br/>
 * <b>Performance notes:</b>
 * <br/><br/>
 * There is currently an overhead for the {@link #readChangeNumber} method compared with traditional CLOB-based storage.
 * This is because the initial access to the underlying XML involves a fetch of the binary XML token metadata. Subsequent reads
 * of the same XML on the same connection are not subject to this overhead as the metadata is cached on the connection.
 * It may be possible to reduce this overhead by pre-caching metadata but further investigation is required.
 */
public class BinaryDOMAccessor
implements XMLWorkDocDOMAccessor {

  private XMLType mXMLType;

  BinaryDOMAccessor() {}

  @Override
  public DOM retrieveDOM(UCon pUCon) {
    try {
      //Read the XML into a DOM using the Oracle binary XML API
      return OracleBinaryXMLReader.instance().read(mXMLType);
    }
    catch (SQLException e) {
      throw new ExInternal("Failed to read XML into DOM", e);
    }
  }

  @Override
  public void prepareForDML(UCon pUCon, DOM pDOM) {
    //Previous this set the "xml:space" attribute to "preserve" to stop whitespace being interfered with
    //Since switching reads to use the Oracle Binary XML API this behaviour has been disabled
  }

  @Override
  public String readChangeNumber(UCon pUCon) {

    if(mXMLType == null) {
      throw new IllegalStateException("Cannot read a change number when SQLXML is null");
    }

    Track.pushDebug("DOMChangeNumberRead");
    try {
      //Use the XMLType DOM API to check the change number. This reads the XML on demand and does not load the whole
      //document into memory, unlike other methods (i.e. using the SQLXML Stax API). Note the performance overhead
      //of the binary metadata loading invoked by the getDocument method (see class Javadoc).
      Document lDocument = mXMLType.getDocument();
      String lAttributeVal = lDocument.getDocumentElement().getAttribute(XMLWorkDoc.CHANGE_NUMBER_ATTR_NAME);
      return XFUtil.nvl(lAttributeVal, "UNDEFINED");
    }
    catch (SQLException e) {
      throw new ExInternal("Failed to read change number using DOM API", e);
    }
    finally {
      Track.pop("DOMChangeNumberRead");
    }
  }

  @Override
  public void openLocator(UCon pUCon, Object pLOB) {

    //Close any existing locator
    closeLocator(pUCon);

    if(pLOB == null) {
      mXMLType = null;
    }
    else if(!(pLOB instanceof XMLType)) {
      //Get scalar XMLType object from query
      throw new ExInternal("LOB selected for binary XML work doc must be an XMLType column");
    }
    else {
      mXMLType = (XMLType) pLOB;
    }
  }

  @Override
  public Object getLOBForBinding(UCon pUCon, BindSQLType pBindTypeRequired, DOM pDOM) {

    if(pBindTypeRequired != BindSQLType.XML) {
      throw new ExInternal("Binary XML WorkDoc can only bind XMLTYPEs (cannot bind " + pBindTypeRequired + ")");
    }

    //Create a new XMLType using a BinaryXMLWriter
    Track.pushInfo("WriteDOMToBinXML");
    try {
      return BinaryXMLWriter.instance().writeToObject(pUCon, pDOM);
    }
    catch (SQLException e) {
      throw new ExInternal("Failed to bind as binary XML", e);
    }
    finally {
      Track.pop("WriteDOMToBinXML");
    }
  }

  @Override
  public boolean isLocatorEmpty(UCon pUCon) {
    //There is no concept of empty binary XML
    return false;
  }

  @Override
  public boolean isLocatorNull(UCon pUCon) {
    return mXMLType == null;
  }

  @Override
  public void closeLocator(UCon pUCon) {
    //Close the XMLType pointer and free resources
    if(mXMLType != null) {
      mXMLType.close();
      mXMLType = null;
    }
  }

  @Override
  public void abort() {
    //Safe to call with a null UCon as we know we don't need it in this class
    closeLocator(null);
  }
}
