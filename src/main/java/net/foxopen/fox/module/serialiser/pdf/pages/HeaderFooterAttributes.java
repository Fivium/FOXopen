package net.foxopen.fox.module.serialiser.pdf.pages;

/**
 * The attributes of a header or footer
 */
public class HeaderFooterAttributes {
  private final float mHeight;

  /**
   * Create header/footer attributes
   * @param pHeight The height of the header or footer
   */
  public HeaderFooterAttributes(float pHeight) {
    mHeight = pHeight;
  }

  public float getHeight() {
    return mHeight;
  }
}
