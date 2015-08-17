package net.foxopen.fox.database.sql.bind;

import net.foxopen.fox.database.UCon;
import net.foxopen.fox.database.xml.SQLXMLWriter;
import net.foxopen.fox.database.xml.XMLWriterStrategy;
import net.foxopen.fox.dom.DOM;

import java.sql.SQLException;
import java.sql.SQLXML;


/**
 * Wrapper for binding an XML document (contained in a DOM) into a SQLXML instance. The DOM must be an element or
 * document node otherwise the database driver will fail due to a malformed XML string exception. To bind fragments or
 * non-element nodes, use {@link net.foxopen.fox.database.sql.bind.DOMListBindObject}.<br/><br/>
 *
 * When creating a DOMBindObject you may specify an XMLWriterStrategy to use, which may improve performance depending
 * on the underlying XML storage type. {@link SQLXMLWriter} is the default if none is specified.
 */
public class DOMBindObject
implements CloseableBindObject {

  private final BindDirection mBindDirection;
  private final DOM mSelectedElement;
  private final XMLWriterStrategy mWriterStrategy;
  private SQLXML mSQLXML = null;

  /**
   * Binds a DOM  in to/out from a statement. Writes are performed using the given XMLWriter.
   * @param pSelectedNode
   * @param pBindDirection
   * @param pWriterStrategy
   */
  public DOMBindObject(DOM pSelectedNode, BindDirection pBindDirection, XMLWriterStrategy pWriterStrategy) {
    mBindDirection = pBindDirection;
    mSelectedElement = pSelectedNode;
    mWriterStrategy = pWriterStrategy;
  }

  /**
   * Binds a DOM in to/out from a statement. Writes are performed using the default XMLWriter.
   * @param pSelectedNode
   * @param pBindDirection
   */
  public DOMBindObject(DOM pSelectedNode, BindDirection pBindDirection) {
    mBindDirection = pBindDirection;
    mSelectedElement = pSelectedNode;
    mWriterStrategy = SQLXMLWriter.instance();
  }

  /**
   * Binds a DOM in to a statement using the default XMLWriter.
   * @param pSelectedNode
   */
  public DOMBindObject(DOM pSelectedNode) {
    mBindDirection = BindDirection.IN;
    mSelectedElement = pSelectedNode;
    mWriterStrategy = SQLXMLWriter.instance();
  }

  @Override
  public Object getObject(UCon pUCon) throws SQLException {
    mSQLXML = mWriterStrategy.writeToObject(pUCon, mSelectedElement);
    return mSQLXML;
  }

  @Override
  public String getObjectDebugString() {
    return mSelectedElement == null ? null : mSelectedElement.outputNodeToString(true);
  }

  @Override
  public BindSQLType getSQLType() {
    return BindSQLType.XML;
  }

  @Override
  public BindDirection getDirection() {
    return mBindDirection;
  }

  @Override
  public void close() throws SQLException {
    if (mSQLXML != null) {
      mSQLXML.free();
    }
  }

  public static class Builder extends BindObjectBuilder<DOMBindObject> {

    private final DOM mBindDOM;

    public Builder(DOM pBindDOM, BindDirection pBindDirection) {
      super(pBindDirection);
      mBindDOM = pBindDOM;
    }

    @Override
    public DOMBindObject build() {
      return new DOMBindObject(mBindDOM, getBindDirection());
    }
  }
}
