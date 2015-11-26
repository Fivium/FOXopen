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
