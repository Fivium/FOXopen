package net.foxopen.fox.spatial.renderer;

import java.awt.Rectangle;
import java.awt.geom.Point2D;

public class Marker {

  public Point2D mPoint;
  public String mText;
  public int mFontSize;
  public Rectangle mBounds;
  public String mParentObjectKey;

  public Marker(Point2D pPoint, String pText, Rectangle pTextBounds, int pFontSize, String pParentObjectKey) {
    mPoint = pPoint;
    mText = pText;
    mBounds = pTextBounds;
    mFontSize = pFontSize;
    mParentObjectKey = pParentObjectKey;
  }

  public Rectangle getBounds() {
    return mBounds;
  }
}
