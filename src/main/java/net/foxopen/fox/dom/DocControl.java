/*

Copyright (c) 2010, UK DEPARTMENT OF ENERGY AND CLIMATE CHANGE -
                    ENERGY DEVELOPMENT UNIT (IT UNIT)
All rights reserved.

Redistribution and use in source and binary forms, with or without modification,
are permitted provided that the following conditions are met:

    * Redistributions of source code must retain the above copyright notice,
      this list of conditions and the following disclaimer.
    * Redistributions in binary form must reproduce the above copyright notice,
      this list of conditions and the following disclaimer in the documentation
      and/or other materials provided with the distribution.
    * Neither the name of the DEPARTMENT OF ENERGY AND CLIMATE CHANGE nor the
      names of its contributors may be used to endorse or promote products
      derived from this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR
ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
(INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
(INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

$Id$

*/
package net.foxopen.fox.dom;

import net.foxopen.fox.XFUtil;
import net.foxopen.fox.dom.xpath.saxon.SaxonEnvironment;
import net.foxopen.fox.ex.ExInternal;
import net.foxopen.fox.track.Track;
import net.foxopen.fox.track.TrackFlag;
import net.sf.saxon.option.xom.XOMDocumentWrapper;
import nu.xom.Attribute;
import nu.xom.Document;
import nu.xom.Element;
import nu.xom.Elements;
import nu.xom.Node;

import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.WeakHashMap;


/**
 * A wrapping object used to control access to an underlying XOM document.
 */
public class DocControl {

  private static final String DEFAULT_BASE_URI = "";

  /**
   * Global lookup Map for resolving a DocControl from a Document
   */
  private static final Map<Document, DocControl> gDocumentToDocControl_SyncMe = new WeakHashMap<Document, DocControl>();
  protected static int gSeqCount = 1;

  /**
   * Special DocControl for unattached nodes. This allows operations to be performed on nodes via a ReadWriteActuator
   * if they are not attached to a document tree.
   */
  private static UnattachedDocControl gUnattachedNodeDocControl;

  static {
    gUnattachedNodeDocControl = new UnattachedDocControl();
  }

  /**
   * Weak reference to XOM Document. This must be weak otherwise GC never occures
   * because entries (not keys) are strong in gDocumentToDocControl_SyncMe Map
   */
  private Reference<Document> mDocumentRef;

  /**
   * Saxon XOM Document Wrapper. Used when wrapping XOM nodes for XPath evaluation.
   */
  private Reference<XOMDocumentWrapper> mDocumentWrapperRef;

  private final Map mRefIndexToWeakElement = new HashMap();
  private int mRefIndexFullRebuildModifyCount = Integer.MIN_VALUE;

  public Actuate mActuate;
  private Actuate mPreviousActuate;

  private int mDocumentModifiedCount = 0;

  public final int mCreateSequence;

  final Iterator mUniqueIterator = XFUtil.getUniqueIterator();

  /**
   * Does this Document require namespace aware XPath processing?
   */
  private boolean mNamespaceAware = false;

//  public boolean mTrackModifications = false;

  /**
   * Used by UnattachedDocControl.
   */
  protected DocControl(){
    mCreateSequence = gSeqCount++;
  }

  /**
   * Create a DocControl wrapper for the given XOM Document
   * @param pDocument The XOM Document to wrap.
   * @param pNamespaceAware Set to true if this Document has arbitrary non-Fox namespaces defined on it.
   */
  DocControl(Document pDocument, boolean pNamespaceAware) {

    // Initialise variables
    mCreateSequence = gSeqCount++;
    mDocumentRef = new WeakReference<Document>(pDocument);
    mActuate = new ActuateReadWrite("INITIALRW", this);
    mNamespaceAware = pNamespaceAware;

    // Register DocControl in hashtable used by getDocControl
    synchronized(gDocumentToDocControl_SyncMe) {
      gDocumentToDocControl_SyncMe.put(pDocument, this);
    }
  }

  /** Return summary info about document root element */
  public final String toString() {
    String lDesc;
    Document lDocument = mDocumentRef.get();

    if(lDocument!=null) {
      Element lElement = lDocument.getRootElement();
      if(lElement!=null) {
        lDesc = lElement.getQualifiedName();
      }
      else {
        lDesc = "XOM document node without root node";
      }
    }
    else {
      lDesc = "XOM document GC'ed";
    }

    return lDesc +" "+ mCreateSequence+" Doc="+lDocument + " hash " + this.hashCode();
  }

  public static final DocControl getDocControl(Node pNode) {
    DocControl lDocControl;

    if(pNode.getDocument() == null){
      //Special case for unattached nodes
      return gUnattachedNodeDocControl;
    }

    synchronized(gDocumentToDocControl_SyncMe) {
      lDocControl =  DocControl.gDocumentToDocControl_SyncMe.get(pNode.getDocument());
    }

    if(lDocControl==null) {
      throw new ExInternal("DocControl should always be in gDocumentToDocControl_SyncMe map for node " + pNode.toString());
    }
    return lDocControl;
  }

  static final void setDocumentModified(final Node pNode) {
    getDocControl(pNode).setDocumentModifiedCount();
  }

  public int getDocumentModifiedCount() {
    return mDocumentModifiedCount;
  }

  public void setDocumentModifiedCount() {
//    if(mTrackModifications) {
//       Track2.logDebugText("DOMModified", XFUtil.getJavaStackTraceInfo(new Exception()));
//    }
    mDocumentModifiedCount++;
  }

  public void setDocumentNoAccess(){

    // Short-cut actuate already set
    if(mActuate.getClass() == ActuateNoAccess.class) {
      return;
    }

    // Restore previous Actuate when of required type
    if(mPreviousActuate != null && mPreviousActuate.getClass() == ActuateNoAccess.class) {
      Actuate lastActuate = mActuate;
      mActuate = mPreviousActuate;
      mPreviousActuate = lastActuate;
    }

    // Assign new actuate
    else {
      mPreviousActuate = mActuate;
      mActuate = new ActuateNoAccess("**No Access Allowed**", this);
    }

  }

  public void setDocumentReadOnly(){
    // Short-cut actuate already set
    if(mActuate.getClass() == ActuateReadOnly.class) {
      return;
    }

    // Restore previous Actuate when of required type
    if(mPreviousActuate != null && mPreviousActuate.getClass() == ActuateReadOnly.class) {
      Actuate lastActuate = mActuate;
      mActuate = mPreviousActuate;
      mPreviousActuate = lastActuate;
    }

    // Assign new actuate
    else {
      mPreviousActuate = mActuate;
      mActuate = new ActuateReadOnly("**Read Only**", this);
    }

  }

  public void setDocumentReadWrite(){

    // Short-cut actuate already set
    if(mActuate.getClass() == ActuateReadWrite.class) {
      return;
    }

    // Restore previous Actuate when of required type
    if(mPreviousActuate != null && mPreviousActuate.getClass() == ActuateReadWrite.class) {
      Actuate lastActuate = mActuate;
      mActuate = mPreviousActuate;
      mPreviousActuate = lastActuate;
    }

    // Assign new actuate
    else {
      mPreviousActuate = mActuate;
      mActuate = new ActuateReadWrite("**Read/Write**", this);
    }

  }

  public void setDocumentReadWriteAutoIds(){
    // Short-cut actuate already set
    if(mActuate.getClass() == ActuateReadWriteAutoIds.class) {
      return;
    }

    // Restore previous Actuate when of required type (provided current is NoAccess)
    if(mPreviousActuate != null
    && mPreviousActuate.getClass() == ActuateReadWriteAutoIds.class
    && mActuate.getClass() == ActuateNoAccess.class) {
      Actuate lastActuate = mActuate;
      mActuate = mPreviousActuate;
      mPreviousActuate = lastActuate;
    }

    // Assign new actuate
    else {
      mPreviousActuate = mActuate;
      mActuate = new ActuateReadWriteAutoIds("**Read/Write**", this);
      refIndexAssignRecursive();
    }

  }

  /**
   * @return The root element of this DocControl's Document, as a DOM.
   */
  public DOM getRootDOM() {
    Document doc = mDocumentRef.get();
    if(doc != null) {
      return new DOM(doc.getRootElement());
    }
    return null;
  }

  public Document getDocumentNode() {
    Document doc = mDocumentRef.get();
    if(doc != null) {
      return doc;
    }
    else {
      throw new ExInternal("Document not found, must have been GCd");
    }
  }

  /**
   * @return The root Element of this DocControl's Document.
   */
  Element getRootElement() {
    Document doc = mDocumentRef.get();
    if(doc != null) {
      return doc.getRootElement();
    }
    return null;
  }

  public DOM getElemByRefOrNull(String pRef) {
    Node n = getNodeByRefOrNull(pRef, false);
    if(n != null) {
      return new DOM(n);
    }
    return null;
  }

  /**
   * Get or create a Saxon DocumentWrapper for this document. This is used to wrap XOM Nodes from this document
   * for the purposes of XPath execution (see {@link DOM#wrap}). Once created, the DocumentWrapper is stored on this
   * DocControl for reuse.
   * @return The DocumentWrapper.
   */
  public XOMDocumentWrapper getOrCreateDocumentWrapper(){
    XOMDocumentWrapper lDocumentWrapper = null;
    if(mDocumentWrapperRef != null){
      lDocumentWrapper = mDocumentWrapperRef.get();
    }
    if(lDocumentWrapper == null){
      if(mDocumentRef.get() == null){
        throw new ExInternal("mDocumentRef is null");
      }
      lDocumentWrapper = new XOMDocumentWrapper(mDocumentRef.get(), SaxonEnvironment.getSaxonConfiguration());
      mDocumentWrapperRef = new WeakReference<>(lDocumentWrapper);
   }

    return lDocumentWrapper;
  }

  /**
   * Refresh the reference index HashMap for this Document.
   * @return True if the refresh was performed, false if it was not necessary.
   */
  boolean refIndexRefresh() {
    // When index as up-to-date as it can be
    if(mRefIndexFullRebuildModifyCount == mDocumentModifiedCount) {
      return false;
    }

    Track.pushDebug("refIndexRefresh", "Document element " + getRootElement().getLocalName());
    try {
      // Rebuild index
      mRefIndexFullRebuildModifyCount = mDocumentModifiedCount;
      mRefIndexToWeakElement.clear();
      refIndexRefresh(getRootElement());
      return true;
    }
    finally {
      Track.pop("refIndexRefresh");
    }
  }

  /**
   * Recursively refreshes the reference index for this document, starting from pElement.
   * @param pElement The target Element.
   */
  private final void refIndexRefresh(Element pElement){

    if(pElement == null){
      throw new ExInternal("pElement is null");
    }

    // Locate reference and index for current node
    String lRef = getRef(pElement);
    if(lRef.length()!=0) {
      if(mRefIndexToWeakElement.containsKey(lRef)) {
        throw new ExInternal("Duplicate foxid ("+lRef+") found in XML data", getRootDOM());
      }
      mRefIndexToWeakElement.put(lRef, new WeakReference(pElement));
    }
    // Recurse for child elements only
    Elements lElements = pElement.getChildElements();
    for(int i = 0; i < lElements.size(); i++){
      refIndexRefresh(lElements.get(i));
    }

  } // refIndexRefresh


  void setRefIndex(String pRef, Node pNode) {
    mRefIndexToWeakElement.put(pRef, new WeakReference(pNode));
  }

  void removeRefIndex(String pRef, Node pNode) {
    mRefIndexToWeakElement.remove(pRef);
  }

  final Node getNodeByRefOrNull(String pRefString, boolean pAutoIds) {
    Reference lRef;
    Node found;

    // First seek element in existing index
    lRef = (Reference) mRefIndexToWeakElement.get(pRefString);
    if(lRef != null) {
      found = (Node) lRef.get();
      if(found != null) {
        return found;
      }
    }

    // Refresh index in case it fell out of date
    boolean lRefreshPerformed = refIndexRefresh();

    if(lRefreshPerformed){
      if(pAutoIds) {
        Track.alert("FoxSysLogError", "getElemByRefOrNull(AutoIds) needed to rebuild ref index for doc root element " + getRootDOM().getName() + "\n"
        + "This strongly indicates a bug in around ActuateReadWriteAutoIds methods");
      }
      else {
        Track.debug("FoxSysLogWarning", "getElemByRefOrNull needed to rebuild ref index for ref " + pRefString + ", doc root element " + getRootDOM().getName() + "\n"
        + "This should not be required if index is kept up-to-date by UElem methods - bug fix required", TrackFlag.FOX_SYS_LOG_WARNING);
      }
    }

    // Second seek element in index
    lRef = (Reference) mRefIndexToWeakElement.get(pRefString);
    if(lRef != null) {
      found = (Node) lRef.get();
      if(found != null) {
        return found;
      }
    }

    return null;
  }

  /**
   * @return True if this DocControl represents a namespace aware Document which contains arbitrary namespaces.
   */
  public boolean isNamespaceAware(){
    return mNamespaceAware;
  }

  /**
   * Convenience method for getting the FOXID of an Element.
   * @param pElement The Element to get the FOXID of.
   * @return FOXID String or empty string if not set.
   */
  public static String getRef(Element pElement){
    Attribute lAttr = pElement.getAttribute(Actuate.FOXID);
    if(lAttr == null){
      return "";
    }
    else {
      return lAttr.getValue();
    }
  }

  /**
   * Reassigns FOXIDs for pElement and any Elements below it if a FOXID exists on the Element. Also updates the reference
   * index for this document after a reassignment.
   * @param pElement The target element.
   */
  void refIndexReassignRecursive(Element pElement) {

    // Reassign reference and index for current node
    String lRef = getRef(pElement);
    if(lRef.length() != 0) {
      mRefIndexToWeakElement.remove(lRef);
      lRef = (String) mUniqueIterator.next();
      pElement.addAttribute(new Attribute(Actuate.FOXID, lRef));
      mRefIndexToWeakElement.put(lRef, new WeakReference(pElement));
    }

    // Recurse for child elements only
    Elements lElements = pElement.getChildElements();
    for(int i = 0; i < lElements.size(); i++){
      refIndexReassignRecursive(lElements.get(i));
    }

  } // end refIndexReassignRecursive

  /**
   * Recursively removes FOXID attributes for pElement and it children, and updates the reference index accordingly.
   * @param pElement The target Element.
   */
  void refIndexRemoveRecursive(Element pElement) {

    // Remove reference and index for current node
    Attribute lAttr = pElement.getAttribute(Actuate.FOXID);
    if(lAttr != null) {
      mRefIndexToWeakElement.remove(lAttr.getValue());
      pElement.removeAttribute(lAttr);
    }

    // Recurse for child elements only
    Elements lElements = pElement.getChildElements();
    for(int i = 0; i < lElements.size(); i++){
      refIndexRemoveRecursive(lElements.get(i));
    }
  }

  /**
   * Iterates over all Elements in this Document and assigns a FOXID attribute to those which are missing one.
   * Elements which already have a FOXID are unaffected. This method also completely refreshes the reference index.
   */
  synchronized void refIndexAssignRecursive() {
    mRefIndexToWeakElement.clear();
    Element node = getRootElement();
    if(node != null) {
      refIndexAssignRecursive(node);
    }
    mRefIndexFullRebuildModifyCount = mDocumentModifiedCount;
  }

  /**
   * Assigns a FOXID attribute to pElement if one is not defined on it, then repeats the process for pElement's children.
   * @param pElement The element to check.
   * @throws ExInternal
   */
  private final void refIndexAssignRecursive(Element pElement){

    // Locate reference and index for current node
    String lRef = getRef(pElement);
    if(lRef.length() == 0) {
      setDocumentModifiedCount();
      lRef = (String) mUniqueIterator.next();
      pElement.addAttribute(new Attribute(Actuate.FOXID, lRef));
      mRefIndexToWeakElement.put(lRef, new WeakReference(pElement));
    }
    else {
      if(mRefIndexToWeakElement.containsKey(lRef)) {
        throw new ExInternal("Duplicate foxid ("+lRef+") found in XML data", getRootDOM());
      }
      else {
        mRefIndexToWeakElement.put(lRef, new WeakReference(pElement));
      }
    }

    // Recurse for child elements only
    Elements lElements = pElement.getChildElements();
    for(int i = 0; i < lElements.size(); i++){
      refIndexAssignRecursive(lElements.get(i));
    }
  }

  /**
   * Indicates wheter this DocControl represents attached nodes or not.
   * @return True if this DocControl is not an UnattachedDocControl, false otherwise.
   */
  public boolean isAttachedDocControl(){
    return true;
  }

}
