/*

Copyright (c) 2012, UK DEPARTMENT OF ENERGY AND CLIMATE CHANGE - 
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

import net.foxopen.fox.ex.ExDOM;

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
    else {
      //Commented out to prevent dross in logs
      //PN TODO after NP's HTML changes, check to see if this is still a problem
      //Bug.sysLogWarn(pMethodName + " called on UnattachedDocControl");
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
  
  /**
   * @return False, because this DocControl is for unattached nodes.
   */
  @Override
  public boolean isAttachedDocControl(){
    return false;
  }  
}
