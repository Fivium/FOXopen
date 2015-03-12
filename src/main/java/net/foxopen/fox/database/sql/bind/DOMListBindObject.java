package net.foxopen.fox.database.sql.bind;

import java.io.IOException;
import java.io.Writer;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLXML;

import net.foxopen.fox.database.UCon;
import net.foxopen.fox.dom.DOMList;
import net.foxopen.fox.ex.ExInternal;

import oracle.sql.CLOB;


/**
 * Wrapper for binding an arbitrary XML fragment into a SQLXML instance. This class ensures the initial XML string sent
 * to the database to be parsed as an XMLType is well-formed, but when the object is bound into a statement it will be
 * correctly bound as a fragment. The nodes in the DOMList can be of any node type. The purpose of this class is to work
 * around the fact that the XML must initially be well-formed to be parsed by the database.
 */
public class DOMListBindObject 
implements CloseableBindObject {
  
  private static final String CLOB_TO_XML_FRAGMENT_CONVERSION_SQL = "SELECT EXTRACT(XMLTYPE(?), '/*/node()') FROM dual";
  
  private final DOMList mSelectedNodes;
  private final BindDirection mBindDirection;
  
  private SQLXML mSQLXML;  
  private ResultSet mFragmentResultSet;

  public DOMListBindObject(DOMList pSelectedNodes, BindDirection pBindDirection) {
    mSelectedNodes = pSelectedNodes;
    mBindDirection = pBindDirection;
  }

  @Override
  public Object getObject(UCon pUCon) throws SQLException {
    
    //As this is an XML fragment there is no direct way to bind it using the JDBC APIs, which all expect a SQLXML
    //object to be a valid XML document. Workaround is to use the Oracle EXTRACT function on a wrapped version of the 
    //fragment, to create a SQLXML object which represents the fragment.
    
    CLOB lClob = pUCon.getTemporaryClob();
    try {
      Writer lClobWriter = lClob.setCharacterStream(0);
      //Add a parent "root" node to wrap the fragment 
      lClobWriter.write("<root>" + mSelectedNodes.outputNodesToString() + "</root>");
      lClobWriter.close();
    }
    catch (IOException | SQLException e) {
        throw new ExInternal("Error Writing to temporary clob for xmltype fragment", e);
    } 
    
    // send the clob to the dataase and extact the xml root 
    PreparedStatement lExtractStatement = pUCon.getJDBCConnection().prepareStatement(CLOB_TO_XML_FRAGMENT_CONVERSION_SQL);
    lExtractStatement.setClob(1, lClob);
    
    //Execute the query and increment the result set
    mFragmentResultSet = lExtractStatement.executeQuery();    
    mFragmentResultSet.next();
    
    //Now the object returned should be an xml fragment
    mSQLXML = mFragmentResultSet.getSQLXML(1);
    
    lClob.freeTemporary();
    lExtractStatement.close();
    
    return mSQLXML;
  }


  @Override
  public String getObjectDebugString() {
    return mSelectedNodes == null ? null : mSelectedNodes.outputNodesToString();
  }
  
  @Override 
  public void close()
  throws SQLException {
    //Close both the result set and the SQLXML object
    mFragmentResultSet.close();
    if (mSQLXML != null) {
      mSQLXML.free();
    }
  }

  @Override
  public BindSQLType getSQLType() {
    return BindSQLType.XML;
  }

  @Override
  public BindDirection getDirection() {
    return mBindDirection;
  }
}
