package net.foxopen.fox.module.serialiser.pdf.pages.pagenumbers;

/**
 * A placeholder for a page number that sets the page number based on the current page number or total page count
 */
public interface PageNumberPlaceholder {
  /**
   * Set the placeholder to either the current page number of total page count
   * @param pCurrentPageNumber The current page number
   * @param pTotalPageCount The total page count
   */
  public void setPageNumber(int pCurrentPageNumber, int pTotalPageCount);
}
