package net.foxopen.fox.module.serialiser.pdf.postprocessing;

import com.itextpdf.text.pdf.PdfStamper;

/**
 * An operation to be applied to the document after the first-pass rendering has been completed
 */
public interface PostProcessingOperation {
  /**
   * Apply the post processing operation
   * @param pStamper A stamper over the first-pass document output
   */
  public void process(PdfStamper pStamper);
}
