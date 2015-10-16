package net.foxopen.fox.module.serialiser.pdf.elementattributes;

import com.itextpdf.text.BaseColor;

/**
 * Contains cell attributes that should be applied when creating table cells
 */
public class CellAttributes {
  private boolean mAdjustFirstLine;
  private int mBorder;
  private float mPaddingTop;
  private float mPaddingRight;
  private float mPaddingBottom;
  private float mPaddingLeft;
  private BaseColor mBackgroundColor;

  /**
   * Create a set of cell attributes initialised with the given attributes
   * @param pAdjustFirstLine Whether or not to adjust the first line of a cell's ColumnText, i.e. take into account
   *                         leading
   * @param pBorder Which border sides should be enabled, see {@link com.itextpdf.text.pdf.PdfPCell#setBorder}
   * @param pPadding The padding to be applied to all sides - individual padding can be set after
   * @param pBackgroundColor The cell background color, set to null for no background
   */
  public CellAttributes(boolean pAdjustFirstLine, int pBorder, float pPadding, BaseColor pBackgroundColor) {
    mAdjustFirstLine = pAdjustFirstLine;
    mBorder = pBorder;
    setPadding(pPadding);
    mBackgroundColor = pBackgroundColor;
  }

  /**
   * Creates a deep copy of the given cell attributes
   * @param pCellAttributes The cell attributes to copy
   */
  public CellAttributes(CellAttributes pCellAttributes) {
    mAdjustFirstLine = pCellAttributes.isAdjustFirstLine();
    mBorder = pCellAttributes.getBorder();
    mPaddingTop = pCellAttributes.getPaddingTop();
    mPaddingRight = pCellAttributes.getPaddingRight();
    mPaddingBottom = pCellAttributes.getPaddingBottom();
    mPaddingLeft = pCellAttributes.getPaddingLeft();
    mBackgroundColor = pCellAttributes.getBackgroundColor();
  }

  public boolean isAdjustFirstLine() {
    return mAdjustFirstLine;
  }

  public void setAdjustFirstLine(boolean pAdjustFirstLine) {
    mAdjustFirstLine = pAdjustFirstLine;
  }

  public int getBorder() {
    return mBorder;
  }

  public void setBorder(int pBorder) {
    mBorder = pBorder;
  }

  public float getPaddingTop() {
    return mPaddingTop;
  }

  public void setPaddingTop(float pPaddingTop) {
    mPaddingTop = pPaddingTop;
  }

  public float getPaddingRight() {
    return mPaddingRight;
  }

  public void setPaddingRight(float pPaddingRight) {
    mPaddingRight = pPaddingRight;
  }

  public float getPaddingBottom() {
    return mPaddingBottom;
  }

  public void setPaddingBottom(float pPaddingBottom) {
    mPaddingBottom = pPaddingBottom;
  }

  public float getPaddingLeft() {
    return mPaddingLeft;
  }

  public void setPaddingLeft(float pPaddingLeft) {
    mPaddingLeft = pPaddingLeft;
  }

  /**
   * Set the padding for all sides (top, right, bottom, left)
   * @param pPadding The padding to be applied to all sides of the cell
   */
  public void setPadding(float pPadding) {
    setPaddingTop(pPadding);
    setPaddingRight(pPadding);
    setPaddingBottom(pPadding);
    setPaddingLeft(pPadding);
  }

  public BaseColor getBackgroundColor() {
    return mBackgroundColor;
  }

  public void setBackgroundColor(BaseColor pBackgroundColor) {
    mBackgroundColor = pBackgroundColor;
  }
}
