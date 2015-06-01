package net.foxopen.fox.database.storage.dom;

import net.foxopen.fox.XFUtil;
import net.foxopen.fox.database.UCon;
import net.foxopen.fox.database.sql.bind.BindSQLType;
import net.foxopen.fox.database.sql.out.SQLTypeConverter;
import net.foxopen.fox.dom.DOM;
import net.foxopen.fox.ex.ExInternal;
import net.foxopen.fox.track.Track;
import oracle.sql.CLOB;
import oracle.xdb.XMLType;

import java.sql.SQLException;


public class ClobDOMAccessor
implements XMLWorkDocDOMAccessor {

  private CLOB mFetchedCLOB = null;

  ClobDOMAccessor() {}

  @Override
  public DOM retrieveDOM(UCon pUCon) {
    return SQLTypeConverter.clobToDOM(mFetchedCLOB);
  }

  @Override
  public void prepareForDML(UCon pUCon, DOM pDOM) {

    if(mFetchedCLOB == null) {
      mFetchedCLOB = pUCon.getTemporaryClob();
    }

    try {
      Track.pushInfo("WriteDOMtoCLOB");
      //Remove any characters already in the CLOB
      mFetchedCLOB.truncate(0);
      //Write the DOM to a CHARACTER STREAM (not a byte stream). This is because Oracle is not aware at this point that
      //the eventual destination is an XMLtype so no character set interpration is performed on the bytes.
      pDOM.outputNodeToWriter(mFetchedCLOB.setCharacterStream(0L), false);
    }
    catch (SQLException e) {
      throw new ExInternal("Could not write DOM to CLOB XMLType", e);
    }
    finally {
      Track.pop("WriteDOMtoCLOB");
    }
  }

  @Override
  public String readChangeNumber(UCon pUCon) {

    if(mFetchedCLOB == null) {
      throw new IllegalStateException("Cannot read a change number when CLOB is null");
    }

    // Read first 2K of clob only
    String lBuffer;
    Track.pushDebug("SubstringChangeNumberRead");
    try {
      lBuffer = mFetchedCLOB.getSubString(1, 2048).trim();

      String lDBChangeNumber = null;
      // Extract change number
      int p1 = lBuffer.indexOf(XMLWorkDoc.CHANGE_NUMBER_ATTR_NAME+"=\"");
      if (p1!=-1) {
        int p2 = lBuffer.indexOf("\"", p1 + XMLWorkDoc.CHANGE_NUMBER_ATTR_NAME.length() + 2);
        lDBChangeNumber = lBuffer.substring(p1 + XMLWorkDoc.CHANGE_NUMBER_ATTR_NAME.length() + 2, p2);
      }

      //Be sure to return null if the change number attr is empty or undefined
      return XFUtil.nvl(lDBChangeNumber, null);
    }
    catch (SQLException x) {
      throw new ExInternal("Storage Location: Error reading first 2K from CLOB/XMLType", x);
    }
    finally {
      Track.pop("SubstringChangeNumberRead");
    }

  }

  @Override
  public void openLocator(UCon pUCon, Object pLOB) {

    //Close any existing locator
    closeLocator(pUCon);

    if(pLOB == null) {
      mFetchedCLOB = null;
    }
    else if(pLOB instanceof CLOB) {
      mFetchedCLOB = (CLOB) pLOB;
    }
    else if (pLOB instanceof XMLType) {
      Track.pushDebug("GetClobValFromXMLType");
      try {
        mFetchedCLOB = ((XMLType) pLOB).getClobVal();

        //Check this isn't a temporary clob - if it is, this is probably a binary XML based storage location and should be declared as such.
        if(mFetchedCLOB.isTemporary()){
          Track.alert("ClobXMLWorkDoc", "Fetched CLOB is temporary! Ensure storage location markup reflects XML storage type.");
        }
      }
      catch (SQLException e) {
        throw new ExInternal("Failed to get CLOB value from XMLType", e);
      }
      finally {
        Track.pop("GetClobValFromXMLType");
      }
    }
    else {
      throw new ExInternal("Do not know how to convert type " + pLOB.getClass().getName());
    }
  }

  @Override
  public Object getLOBForBinding(UCon pUCon, BindSQLType pBindTypeRequired, DOM pDOM) {
    if(pBindTypeRequired == BindSQLType.CLOB) {
      return mFetchedCLOB;
    }
    else if (pBindTypeRequired == BindSQLType.XML) {
      return pUCon.convertClobToSQLXML(mFetchedCLOB);
    }
    else {
      throw new ExInternal("Don't know how to bind XML LOB as a " + pBindTypeRequired + ")");
    }
  }

  @Override
  public boolean isLocatorEmpty(UCon pUCon) {
    if(mFetchedCLOB == null) {
      return false;
    }

    try {
      return mFetchedCLOB.length() == 0;
    }
    catch (SQLException e) {
      throw new ExInternal("Failed to determine if LOB was empty", e);
    }
  }

  @Override
  public boolean isLocatorNull(UCon pUCon) {
    return mFetchedCLOB == null;
  }

  @Override
  public void closeLocator(UCon pUCon) {
    if(mFetchedCLOB != null) {
      try {
        if(mFetchedCLOB.isOpen()){
          mFetchedCLOB.close();
        }
      }
      catch (SQLException e) {
        throw new ExInternal("Failed to close LOB locator", e);
      }
    }
    //Note this method only frees a CLOB if it is temporary
    pUCon.freeTemporaryClob(mFetchedCLOB);
    mFetchedCLOB = null;
  }
}
