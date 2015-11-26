package net.foxopen.fox.spatial.renderer;

import net.foxopen.fox.ex.ExInternal;

import java.awt.*;

public class PolyLine {
  public int[] mXPoints;
  public int[] mYPoints;
  public String mAnnotation;

  public PolyLine(int[] pX, int[] pY, String pAnnotation) {
    if (pX.length != pY.length) {
      throw new ExInternal("Array of X points do not correspond to array of Y points");
    }

    mXPoints = pX;
    mYPoints = pY;

    mAnnotation = pAnnotation;
  }

  public PolyLine(int[] pX, int[] pY) {
    this(pX, pY, null);
  }

  public Rectangle getBounds() {
    int lMinX = -1, lMinY = -1, lMaxX = -1, lMaxY = -1;
    for ( int i = 0; i < mXPoints.length; ++i) {
      if (lMinX > mXPoints[i] || lMinX == -1) {
        lMinX = mXPoints[i];
      }
      if (lMinY > mYPoints[i] || lMinY == -1) {
        lMinY = mYPoints[i];
      }
      if (lMaxX < mXPoints[i]) {
        lMaxX = mXPoints[i];
      }
      if (lMaxY < mYPoints[i]) {
        lMaxY = mYPoints[i];
      }
    }

    return new Rectangle(lMinX, lMinY, (lMaxX - lMinX), (lMaxY - lMinY));
  }
}
