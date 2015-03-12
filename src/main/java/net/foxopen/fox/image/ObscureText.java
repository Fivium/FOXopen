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
package net.foxopen.fox.image;

import java.awt.image.BufferedImage;
import java.awt.Graphics2D;
import java.awt.Color;
import java.awt.Font;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Date;
import java.util.Random;
import javax.imageio.ImageIO;

public class ObscureText
{
  private final static String IMAGE_FORMAT = "png";
  private final static double MIN_SKEW = 0.01;
  private final static double MAX_SKEW = 0.03;
  private final static double NOISE_PERCENTAGE_COVER = 0.15;
  
  // returns a value between 0.04 and 0.07
  private static double getSkewRand () {
    Random r = new Random(new Date().getTime());
    return Math.max(Math.min((r.nextDouble() - r.nextDouble())/6, MAX_SKEW), MIN_SKEW);
  }

  public static byte[] getObscureTextAsByteArray(int aWidth, int aHeight, String aString)
  throws
    IOException
  {
    BufferedImage lBufferedImage = new BufferedImage(aWidth, aHeight, BufferedImage.TYPE_INT_RGB);
    Graphics2D g2 = lBufferedImage.createGraphics();
    byte[] bytes = null;
    
    Random r = new Random(new Date().getTime());
    double rndx = getSkewRand();
    double rndy = getSkewRand();
    
    // get a sensible font size
    int fontsize = Math.min(aHeight/2, aWidth/aString.length());

    // set colours
    g2.setColor(Color.WHITE);
    g2.fillRect(0, 0, aWidth, aHeight);
    g2.setColor(Color.BLACK);
    
    // create border
    g2.drawRect(0, 0, aWidth-1, aHeight-1);
    
    // set up font
    Font thisFont = new Font("SansSerif", Font.BOLD, fontsize);
    g2.setFont(thisFont);
   
    // add noise
    for (int i = 0; i < (aWidth * aHeight) * NOISE_PERCENTAGE_COVER; i++) {
      int x = r.nextInt(aWidth);
      int y = r.nextInt(aHeight);
      g2.drawLine(x, y, x, y);
    }

    // loop through and place each letter
    for (int i = 0; i < aString.length(); i++) 
    { 
      // skew image pseudorandomly
      int sign = (0 - r.nextInt(1));
      g2.shear(sign + rndx, 0);
    
      // draw a horizontal and vertical line
      g2.drawLine(0, 2 + r.nextInt(aHeight - 2), aWidth, 2 + r.nextInt(aHeight - 2));
      g2.drawLine(2 + r.nextInt(aWidth - 2), 0, r.nextInt(aWidth - 2), aHeight);
      
      // output a character of the string
      double rand_y = (0 - r.nextInt(1)) + (r.nextDouble() + r.nextDouble());
      g2.drawString(aString.substring(i, i+1), aWidth/(aString.length()+3) * (i+2), (int)((aHeight/(1+rand_y))));
    }

    // convert to byte array and return    
    ByteArrayOutputStream lByteArrayOutputStream = new ByteArrayOutputStream();
    ImageIO.write(lBufferedImage, IMAGE_FORMAT, lByteArrayOutputStream);
    bytes = lByteArrayOutputStream.toByteArray();
    return bytes;
  }
}
