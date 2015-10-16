package net.foxopen.fox.module.serialiser.pdf.pages;

/**
 * The attributes of a page body
 */
public class BodyAttributes {
  private final float mMarginLeft;
  private final float mMarginRight;
  private final float mMarginTop;
  private final float mMarginBottom;

  /**
   * Create the attributes of a page body
   * @param pMarginLeft The margin to the left of the page body
   * @param pMarginRight The margin to the right of the page body
   * @param pMarginTop The margin to the top of the page body
   * @param pMarginBottom The margin to the bottom of the page body
   */
  public BodyAttributes(float pMarginLeft, float pMarginRight, float pMarginTop, float pMarginBottom) {
    mMarginLeft = pMarginLeft;
    mMarginRight = pMarginRight;
    mMarginTop = pMarginTop;
    mMarginBottom = pMarginBottom;
  }

  public float getMarginLeft() {
    return mMarginLeft;
  }

  public float getMarginRight() {
    return mMarginRight;
  }

  public float getMarginTop() {
    return mMarginTop;
  }

  public float getMarginBottom() {
    return mMarginBottom;
  }
}
