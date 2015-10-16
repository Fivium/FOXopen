package net.foxopen.fox.module.serialiser.pdf.postprocessing;

import com.itextpdf.text.DocumentException;
import com.itextpdf.text.pdf.PdfReader;
import com.itextpdf.text.pdf.PdfStamper;
import net.foxopen.fox.ex.ExInternal;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.LinkedList;
import java.util.List;

/**
 * Manipulates a document after the first pass has been completed, to apply post processing operations
 */
public class PostProcessingManager {
  private final List<PostProcessingOperation> mPostProcessingOperations = new LinkedList<>();

  /**
   * Adds an operation that will be carried out during post processing. Post processing operations occur in the order
   * they are added.
   * @param pPostProcessingOperation The operation that should be carried out
   */
  public void addPostProcessingOperation(PostProcessingOperation pPostProcessingOperation) {
    mPostProcessingOperations.add(pPostProcessingOperation);
  }

  /**
   * Process the first-pass source, applying any post processing operations that have been added, and stream to the
   * output.
   * @param pOutput The post processing output
   * @param pSource The source document bytes
   */
  public void process(OutputStream pOutput, ByteArrayOutputStream pSource) {
    PdfStamper lStamper = getStamper(pOutput, getReader(pSource));
    mPostProcessingOperations.forEach(pOperation -> pOperation.process(lStamper));
    closeStamper(lStamper);
  }

  /**
   * Return a reader for the PDF bytes provided in source
   * @param pSource The PDF output stream
   * @return A reader for the source PDF
   * @throws ExInternal If the source output stream could not be read
   */
  private PdfReader getReader(ByteArrayOutputStream pSource) throws ExInternal {
    try {
      return new PdfReader(pSource.toByteArray());
    }
    catch (IOException e) {
      throw new ExInternal("Failed to read source output stream for post processing", e);
    }
  }

  /**
   * Return a stamper for the source reader document that outputs to the provided output stream
   * @param pOutputStream The stamper output stream
   * @param pSourceReader The source document reader
   * @return A stamper that allows the source to be manipulated, output to the provided output stream
   * @throws ExInternal If the stamper could not be opened using the provided reader
   */
  private PdfStamper getStamper(OutputStream pOutputStream, PdfReader pSourceReader) throws ExInternal {
    try {
      return new PdfStamper(pSourceReader, pOutputStream);
    }
    catch (IOException | DocumentException e) {
      throw new ExInternal("Failed to create post processing stamper", e);
    }
  }

  /**
   * Closes the stamper and its associated reader
   * @param pStamper The stamper to close
   * @throws ExInternal If the stamper could not be closed
   */
  private void closeStamper(PdfStamper pStamper) throws ExInternal {
    try {
      pStamper.close();
    }
    catch (IOException | DocumentException e) {
      throw new ExInternal("Failed to close post processing stamper", e);
    }
  }
}
