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
package net.foxopen.fox.renderer;

import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.Point2D;

import java.io.IOException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class Renderer {
  protected Map mRenderableObjectsMap = new HashMap();
  protected List mRootLevelRenderableObjects = new ArrayList();

  //Add a renderable object to the right place for ordered rendering
  public void addObject(String pParentRenderableObject, RenderableObject pRenderableObject){
    //Add to mapping of the key to the object
    mRenderableObjectsMap.put(pRenderableObject.mKey, pRenderableObject);

    if (pParentRenderableObject == null) {
      //If no parent object, note down it's a root level renderable object
      mRootLevelRenderableObjects.add(pRenderableObject.mKey);
    }
    else {
      //Find parent to add this renderable object to, or put it down as a root level element if no parent found
      if (mRenderableObjectsMap.containsKey(pParentRenderableObject)) {
        RenderableObject lParent = (RenderableObject)mRenderableObjectsMap.get(pParentRenderableObject);
        lParent.addChild(pRenderableObject);
      }
      else {
        mRootLevelRenderableObjects.add(pRenderableObject.mKey);
        //throw new ExInternal("Parent Missing. Cannot make '" + pRenderableObject.mKey + "' a child of '" + pParentRenderableObject + "'");
      }
    }
  } //addObject

  public RenderableObject getObject(String pParentRO){
    return (RenderableObject)mRenderableObjectsMap.get(pParentRO);
  } //getObject

  //Add funtions to be completed by the renderers
  abstract public Graphics2D getGraphics2D();
  abstract public byte[] generate() throws IOException;
  


  /**
   * Change the point of a marker to account for a text area's size
   * <pre>
   * +--------+--------+
   * |        UC       |
   * |   UL   |   UR   |
   * |        |        |
   * +CL ---- CC --- CR+
   * |        |        |
   * |   LL   |   LR   |
   * |        LC       |
   * +--------+--------+
   * </pre>
   * @param pOriginal Initial marker position
   * @param pPosition Layout of the text about the marker
   * @param pBounds Rectangle the size of the text to draw
   * @return Point of text's top left to start drawing at
   */
  protected Point2D getMarkerPoint (Point2D pOriginal, String pPosition, Rectangle pBounds) {
    if ("LR".equals(pPosition.toUpperCase())) {
      return pOriginal;
    }
    else if ("LL".equals(pPosition.toUpperCase())) {
      return new Point((int)pOriginal.getX() - pBounds.width, (int)pOriginal.getY());
    }
    else if ("CC".equals(pPosition.toUpperCase())) {
      return new Point((int)(pOriginal.getX() - (pBounds.width/2)), (int)pOriginal.getY() - (pBounds.height/2));
    }
    else if ("UR".equals(pPosition.toUpperCase())) {
      return new Point((int)pOriginal.getX(), (int)pOriginal.getY() - pBounds.height);
    }
    else if ("UL".equals(pPosition.toUpperCase())) {
      return new Point((int)pOriginal.getX() - pBounds.width, (int)pOriginal.getY() - pBounds.height);
    }
    else if ("LC".equals(pPosition.toUpperCase())) {
      return new Point((int)(pOriginal.getX() - (pBounds.width/2)), (int)pOriginal.getY());
    }
    else if ("UC".equals(pPosition.toUpperCase())) {
      return new Point((int)(pOriginal.getX() - (pBounds.width/2)), (int)pOriginal.getY() - pBounds.height);
    }
    else if ("CL".equals(pPosition.toUpperCase())) {
      return new Point((int)(pOriginal.getX() - pBounds.width), (int)pOriginal.getY() - (pBounds.height/2));
    }
    else if ("CR".equals(pPosition.toUpperCase())) {
      return new Point((int)pOriginal.getX(), (int)pOriginal.getY() - (pBounds.height/2));
    }
    return pOriginal;
  } //getMarkerPoint
}
