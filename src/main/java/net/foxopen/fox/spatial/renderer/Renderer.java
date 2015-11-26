package net.foxopen.fox.spatial.renderer;

import java.awt.*;
import java.awt.geom.Point2D;
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

  //Add functions to be completed by the renderers
  abstract public Graphics2D getGraphics2D();

  abstract public void generate();



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
