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

import net.foxopen.fox.ex.ExInternal;

import net.foxopen.fox.track.Track;
import nu.xom.Attribute;
import nu.xom.Element;
import nu.xom.Node;


public class ActuateReadWriteAutoIds
extends ActuateReadWrite
{
  ActuateReadWriteAutoIds(String pAccessViolationInfo, DocControl pDocControl)
  {
   super(pAccessViolationInfo, pDocControl);
  }

  protected Element _internalCreateElementNoChangeInc(Node pNode, String pName)
  {

    // Create element
    Element lElement = super._internalCreateElementNoChangeInc(pNode, pName);

    // Assign foxid to element
    String lRef = (String) mUniqueIterator.next();
    lElement.addAttribute(new Attribute(FOXID, lRef));

    // Register foxid in index
    mDocControl.setRefIndex(lRef, lElement);

    // Return element
    return lElement;

  }

  public String getRef(Node pNode)
  throws ExInternal
  {

    // Return foxid off element which should always be present for this actuate
    String lRef = DocControl.getRef((Element) pNode);
    if(lRef.length()==0) {
      // TODO: WORKAROUND - should throw error but a bit harsh
      Track.alert("FoxSysLogError", "ActuateReadWriteAutoIds.getRef should never find a node without foxid: " + (new DOM(pNode)).toString());
      return super.getRef(pNode);
    }
    return lRef;

  }

  public void copyContentsTo(Node pNode, Node pNewParent, boolean pResetRefs)
  throws ExInternal
  {
    super.copyContentsTo(pNode, pNewParent, pResetRefs);
    assignAllRefs(null); //PN Note: this needs looking it, however to avoid confusion: assignAllRefs does nothing with the node argument, removed here for clarity
  }

  public Node removeRefsRecursive(Node pNode) // Must override actuateReadWrite implementation!
  throws ExInternal
  {
    throwAccessViolation(pNode);
    return null;
  }

  protected final void refreshRefIndex(Node pNode, DocControl pDocControl)
  throws ExInternal
  {
    throw new ExInternal("refreshRefIndex should never be needed for ActuateReadWriteAutoIds");
  }

  public Node getElemByRef(Node pNode, String pRefString)
  throws ExInternal
  {
    Node found = mDocControl.getNodeByRefOrNull(pRefString, true);
    if(found == null) {
      throw new ExInternal("GetElemByRef failed to locate ref ("+pRefString+") in"
      , new DOM(getRootElement(pNode))
      );
    }
    return found;
  }

} // end class ActuateReadWriteAutoIds
