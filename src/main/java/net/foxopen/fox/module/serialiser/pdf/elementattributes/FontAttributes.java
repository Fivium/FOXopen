package net.foxopen.fox.module.serialiser.pdf.elementattributes;

import com.itextpdf.text.BaseColor;

/**
 * Contains the font attributes that are used when adding text to the document
 */
public class FontAttributes {
  private float mSize;
  private int mStyle;
  private BaseColor mColor;
  private float mMultipliedLeading;

  /**
   * Creates a set of font attributes
   * @param pSize The font size in points
   * @param pStyle The font style, see the style constants defined in {@link com.itextpdf.text.Font}
   * @param pColor The font colour
   * @param pMultipliedLeading The multiplied leading, this number is multiplied by the font size to get the total leading
   */
  public FontAttributes(float pSize, int pStyle, BaseColor pColor, float pMultipliedLeading) {
    mSize = pSize;
    mStyle = pStyle;
    mColor = pColor;
    mMultipliedLeading = pMultipliedLeading;
  }

  /**
   * Creates a deep copy of the given font attributes
   * @param pFontAttributes The font attributes to copy
   */
  public FontAttributes(FontAttributes pFontAttributes) {
    mSize = pFontAttributes.getSize();
    mStyle = pFontAttributes.getStyle();
    mColor = new BaseColor(pFontAttributes.getColor().getRGB());
    mMultipliedLeading = pFontAttributes.getMultipliedLeading();
  }

  public float getSize() {
    return mSize;
  }

  public void setSize(float pSize) {
    mSize = pSize;
  }

  public int getStyle() {
    return mStyle;
  }

  public void setStyle(int pStyle) {
    mStyle = pStyle;
  }

  public BaseColor getColor() {
    return mColor;
  }

  public void setColor(BaseColor pColor) {
    mColor = pColor;
  }

  public float getMultipliedLeading() {
    return mMultipliedLeading;
  }

  public void setMultipliedLeading(float pMultipliedLeading) {
    mMultipliedLeading = pMultipliedLeading;
  }
}
