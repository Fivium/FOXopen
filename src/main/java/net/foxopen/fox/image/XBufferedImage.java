package net.foxopen.fox.image;

import com.sun.media.jai.codec.ImageCodec;
import com.sun.media.jai.codec.ImageEncoder;
import com.sun.media.jai.codec.PNGEncodeParam;

import java.awt.image.BufferedImage;

import java.io.IOException;
import java.io.OutputStream;


public class XBufferedImage  extends BufferedImage
{
  private final int mDPI;

  /**
   * Create a new Buffered image with height/width specified in pixels
   * This specifies it's 72 DPI however
   *
   * @param pWidth Width in Pixels of desired image
   * @param pHeight Height in Pixels of desired image
   * @param pImageType BufferedImage.TYP_INT.....
   */
  public XBufferedImage(int pWidth, int pHeight, int pImageType) {
    super(pWidth, pHeight, pImageType);
    mDPI = 72;
  }

  /**
   * Create a new Buffered image with height/width specified in pUnits.
   * pUnits can be Inches, or px
   * You can also specifiy a higher DPI for better resolution when intending to
   * print the resultant image, remember to add in a transform to any graphics
   * operations like:<br />
   * <pre>  float lDPITransformScale = (1.0f/72.0f) * lCanvas.getDPI();
   *   AffineTransform lDPITransform = lG2D.getTransform().getScaleInstance(lDPITransformScale, lDPITransformScale);
   *   lG2D.transform(lDPITransform);</pre>
   *
   * @param pWidth Width in pUnits of desired image
   * @param pHeight Height in pUnits of desired image
   * @param pImageType BufferedImage.TYP_INT.....
   * @param pDPI DPI of image when saved, probably 72, 96, 300, 600...
   * @param pUnits Unit of measurement of supplies width/height (inches or pixels currently)
   */
  public XBufferedImage(int pWidth, int pHeight, int pImageType, int pDPI, String pUnits) {
    super(("inches".equals(pUnits)?pDPI*pWidth:pWidth), ("inches".equals(pUnits)?pDPI*pHeight:pHeight), pImageType);
    mDPI = pDPI;
  }

  //Accessor for DPI
  public final int getDPI() {
    return mDPI;
  }

  /**
   * Save buffered image to PNG, with optional scaling/DPI setting
   *
   * @param pOut
   * @throws IOException
   */
  public void convertToPNG(OutputStream pOut)
    throws IOException {
    //Set up DPI settings
    int lDotsPerMetre = (int)(mDPI / 0.0254);
    PNGEncodeParam params = PNGEncodeParam.getDefaultEncodeParam(this);
    params.setPhysicalDimension(lDotsPerMetre, lDotsPerMetre, 1);

    //Encode to PNG
    ImageEncoder encoder = ImageCodec.createImageEncoder("PNG", pOut, params);
    encoder.encode(this);

    pOut.close();
  }
}
