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
package net.foxopen.fox.spatial.renderer;

import net.foxopen.fox.dom.DOM;

import java.awt.*;
import java.awt.geom.Area;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class RenderableObject {
  public List mChildren = new ArrayList();
  public final String mKey;
  public final Object mObject;
  public final DOM mStyle;

  public RenderableObject(String pKey, Object pObject, DOM pRenderingStyle) {
    mKey = pKey;
    mObject = pObject;
    mStyle = pRenderingStyle;
  }

  public void addChild(RenderableObject lChild) {
    mChildren.add(lChild);
  }

  public Rectangle getBounds() {
    //If there are children, the bounds encompass them
    if (mChildren.size() > 0) {
      Rectangle lTempBounds;
      Rectangle lSumBounds = null;
      Iterator lI = mChildren.iterator();
      while (lI.hasNext()) {
        RenderableObject lRO = (RenderableObject)lI.next();
        lTempBounds = lRO.getBounds();

        if (lSumBounds == null) {
          lSumBounds = lTempBounds;
          continue;
        }

        lSumBounds.add(lSumBounds);
      }
    }
    else {
      if (mObject instanceof Marker) {
        Marker lMarker = (Marker)mObject;
        return lMarker.getBounds();
      }
      else if (mObject instanceof Area) {
        Area lPolygon = (Area)mObject;
        return lPolygon.getBounds();
      }
      else if (mObject instanceof Point2D) {
        Point2D lPoint = (Point2D)mObject;
        return new Rectangle(new Double(lPoint.getX()).intValue()-2, new Double(lPoint.getY()).intValue()-2, 5, 5);
      }
      else if (mObject instanceof PolyLine) {
        PolyLine lPolyLine = (PolyLine)mObject;
        return lPolyLine.getBounds();
      }
    }

    return new Rectangle(0,0,0,0);
  }
}
