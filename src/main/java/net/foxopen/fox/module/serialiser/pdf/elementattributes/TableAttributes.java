package net.foxopen.fox.module.serialiser.pdf.elementattributes;

import net.foxopen.fox.ex.ExInternal;

import java.util.Optional;

/**
 * Contains the set of attributes used when adding a table to the document
 */
public class TableAttributes {
  private Optional<Float> mWidthPercentage;
  private Optional<Float> mFixedWidth;
  private int mHorizontalAlignment;
  private float mSpacingBefore;
  private float mSpacingAfter;
  private boolean mKeepTogether;

  /**
   * Create a set of table attributes
   * @param pWidthPercentage The percentage width the table should be relative to the content area, this or {@code
   *                         pFixedWidth} must be specified
   * @param pFixedWidth The fixed width in points the table should be, this or {@code pWidthPercentage} must be specified
   * @param pHorizontalAlignment The horizontal alignment of the table, see {@link com.itextpdf.text.pdf.PdfPTable#setHorizontalAlignment}
   * @param pSpacingBefore Spacing to be added before the table, see {@link com.itextpdf.text.pdf.PdfPTable#setSpacingBefore}
   * @param pSpacingAfter Spacing to be added before the table, see {@link com.itextpdf.text.pdf.PdfPTable#setSpacingAfter}
   * @param pKeepTogether If true the table will be kept on one page if it fits, by forcing a new page if it doesn't fit
   *                      on the current page. When false a table can split over multiple pages.
   * @throws ExInternal If neither or both of pWidthPercent or pFixedWidth are specified
   */
  public TableAttributes(Optional<Float> pWidthPercentage, Optional<Float> pFixedWidth, int pHorizontalAlignment,
                         float pSpacingBefore, float pSpacingAfter, boolean pKeepTogether) throws ExInternal {
    if (!pWidthPercentage.isPresent() && !pFixedWidth.isPresent()) {
      throw new ExInternal("Either a width percentage or a fixed width must be specified on a table", new IllegalArgumentException());
    }

    if (pWidthPercentage.isPresent() && pFixedWidth.isPresent()) {
      throw new ExInternal("Only one of a width percentage or a fixed width can be specified on a table, not both", new IllegalArgumentException());
    }

    mWidthPercentage = pWidthPercentage;
    mFixedWidth = pFixedWidth;
    mHorizontalAlignment = pHorizontalAlignment;
    mSpacingBefore = pSpacingBefore;
    mSpacingAfter = pSpacingAfter;
    mKeepTogether = pKeepTogether;
  }

  /**
   * Create a deep copy of the table attributes
   * @param pTableAttributes The table attributes to copy
   */
  public TableAttributes(TableAttributes pTableAttributes) {
    mWidthPercentage = pTableAttributes.getWidthPercentage();
    mFixedWidth = pTableAttributes.getFixedWidth();
    mHorizontalAlignment = pTableAttributes.getHorizontalAlignment();
    mSpacingBefore = pTableAttributes.getSpacingBefore();
    mSpacingAfter = pTableAttributes.getSpacingAfter();
    mKeepTogether = pTableAttributes.isKeepTogether();
  }

  public Optional<Float> getWidthPercentage() {
    return mWidthPercentage;
  }

  public void setWidthPercentage(float pWidthPercentage) {
    // Width percentage was last set so takes priority over fixed width
    mFixedWidth = Optional.empty();
    mWidthPercentage = Optional.of(pWidthPercentage);
  }

  public Optional<Float> getFixedWidth() {
    return mFixedWidth;
  }

  public void setFixedWidth(float pFixedWidth) {
    // Fixed width was last set so takes priority over width percentage
    mWidthPercentage = Optional.empty();
    mFixedWidth = Optional.of(pFixedWidth);
  }

  public float getSpacingBefore() {
    return mSpacingBefore;
  }

  public void setSpacingBefore(float pSpacingBefore) {
    mSpacingBefore = pSpacingBefore;
  }

  public float getSpacingAfter() {
    return mSpacingAfter;
  }

  public void setSpacingAfter(float pSpacingAfter) {
    mSpacingAfter = pSpacingAfter;
  }

  public boolean isKeepTogether() {
    return mKeepTogether;
  }

  public void setKeepTogether(boolean pKeepTogether) {
    mKeepTogether = pKeepTogether;
  }

  public int getHorizontalAlignment() {
    return mHorizontalAlignment;
  }

  public void setHorizontalAlignment(int pHorizontalAlignment) {
    mHorizontalAlignment = pHorizontalAlignment;
  }
}
