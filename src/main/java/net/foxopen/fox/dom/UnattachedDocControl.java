package net.foxopen.fox.dom;

import net.foxopen.fox.dom.xpath.saxon.SaxonEnvironment;
import net.foxopen.fox.ex.ExDOM;

import net.foxopen.fox.track.Track;
import net.sf.saxon.option.xom.XOMDocumentWrapper;
import nu.xom.Element;
import nu.xom.Node;


/**
 * Workaround DocControl class for dealing with Nodes which do not have a containing document. This will usually be
 * because they have been removed from their original DOM tree. If consuming code does not expect this, references
 * to unattached nodes will persist, and attempts to resolve their DocControl will result in null-pointer related
 * errors. Furthermore, it may be desirable in some cases to detach a node from its parent and still continue to
 * perform operations on it. Using this class provides a transparent solution for allowing these usage patterns.
 * <br/><br/>
 *
 * The UnattachedDocControl always has a ReadWrite Actuator, so all normal operations will be allowed on unattached
 * nodes.<br/><br/>
 *
 * This class should only be instantiated once and reused as a singleton. Therefore all operations which could cause
 * thread safety issues or corrupt the state of the DocControl are disallowed. <br/><br/>
 *
 * N.B. This is an issue introduced by XOM - in the W3C DOM model, a node always belonged to a document whether it
 * was attached to a parent or not. <br/><br/>
 */
final class UnattachedDocControl
extends DocControl {

  /**
   * Set to true to force hard errors when this DocControl is used inappropriately.
   */
  private static final boolean STRICT = false;

  UnattachedDocControl() {
    mActuate = new ActuateReadWrite("INITIALRW", this);
  }

  private void throwError(String pMethodName){
    if(STRICT){
      throw new ExDOM("Cannot " + pMethodName + " on an UnattachedDocControl. " +
        "This error is caused by attempting to manipulate a node which does not belong to a DOM tree.");
    }
  }

  @Override
  public void setDocumentModifiedCount() {
    throwError("setDocumentModifiedCount");
  }

  @Override
  public void setDocumentNoAccess() {
    throwError("setDocumentNoAccess");
  }

  @Override
  public void setDocumentReadOnly() {
    throwError("setDocumentReadOnly");
  }

  @Override
  public void setDocumentReadWrite() {
    throwError("setDocumentReadWrite");
  }

  @Override
  public void setDocumentReadWriteAutoIds() {
    throwError("setDocumentReadWriteAutoIds");
  }

  @Override
  synchronized void refIndexAssignRecursive() {
    throwError("refIndexAssignRecursive");
  }

  @Override
  void refIndexReassignRecursive(Element pElement) {
    throwError("refIndexReassignRecursive");
  }

  @Override
  boolean refIndexRefresh() {
    throwError("refIndexRefresh");
    return false;
  }

  @Override
  void refIndexRemoveRecursive(Element pElement) {
    throwError("refIndexRemoveRecursive");
  }

  @Override
  void removeRefIndex(String pRef, Node pNode) {
    throwError("removeRefIndex");
  }

  @Override
  void setRefIndex(String pRef, Node pNode) {
    throwError("setRefIndex");
  }

  @Override
  public DOM getElemByRefOrNull(String pRef) {
    return null;
  }

  @Override
  public DOM getRootDOM() {
    throw new ExDOM("Cannot getRootDOM on an UnattachedDocControl"); //Always error - this is a serious problem
  }

  @Override
  Element getRootElement() {
    throw new ExDOM("Cannot getRootElement on an UnattachedDocControl"); //Always error - this is a serious problem
  }

  @Override
  public XOMDocumentWrapper getOrCreateDocumentWrapper(Node pNode) {
    //Previously threw an exception but now tolerated for XPath variable support (element variables are unattached)
    //XPath processing should still check for node attachment and prevent XPath execution on unattached nodes in inappropriate circumstances
    Track.debug("WrapUnattachedNode", "Creating JIT document wrapper for unattached node " + (pNode instanceof Element ? ((Element) pNode).getLocalName() : ""));
    return new XOMDocumentWrapper(pNode, SaxonEnvironment.getSaxonConfiguration());
  }

  /**
   * @return False, because this DocControl is for unattached nodes.
   */
  @Override
  public boolean isAttachedDocControl(){
    return false;
  }
}
