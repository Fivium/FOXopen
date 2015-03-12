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

import java.awt.Rectangle;
import net.foxopen.fox.ex.ExInternal;

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
