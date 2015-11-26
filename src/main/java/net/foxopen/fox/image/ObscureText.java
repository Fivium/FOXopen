package net.foxopen.fox.image;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Date;
import java.util.Random;

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
