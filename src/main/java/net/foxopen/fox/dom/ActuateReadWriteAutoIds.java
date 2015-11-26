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
