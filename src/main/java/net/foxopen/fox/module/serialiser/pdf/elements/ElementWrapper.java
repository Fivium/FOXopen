package net.foxopen.fox.module.serialiser.pdf.elements;

import com.itextpdf.text.Chunk;
import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Element;
import com.itextpdf.text.ElementListener;
import com.itextpdf.text.api.WriterOperation;
import com.itextpdf.text.pdf.PdfWriter;

import java.util.List;

/**
 * Wraps an element, with the {@link #type} overriden to {@link Element#WRITABLE_DIRECT}. This is used so that elements
 * with types that iText treats specially can be added with this special processing avoided. For example, when
 * {@link com.itextpdf.text.Phrase} elements are added iText will extract the chunks from the phrase and discard the
 * phrase itself. Wrapping the element allows the phrase to be added as an encapsulated array of chunks. The
 * {@link WriterOperation} interface is implemented with a {@link WriterOperation#write} method stub as iText may do an
 * unsafe cast of an element with type {@link Element#WRITABLE_DIRECT} to {@link WriterOperation}.
 */
public class ElementWrapper implements Element, WriterOperation {
  private final Element mWrappedElement;

  /**
   * Create a wrapped element
   * @param pWrappedElement The element to be wrapped
   */
  public ElementWrapper(Element pWrappedElement) {
    mWrappedElement = pWrappedElement;
  }

  @Override
  public boolean process(ElementListener pListener) {
    return mWrappedElement.process(pListener);
  }

  /**
   * Gets the type of the element. A wrapped element always return the type {@link Element#WRITABLE_DIRECT}. iText does
   * not treat a {@link Element#WRITABLE_DIRECT} type in any special way, so the element is added to its container
   * without any special processing that may break the encapsulation.
   * @return {@link Element#WRITABLE_DIRECT}
   */
  @Override
  public int type() {
    return Element.WRITABLE_DIRECT;
  }

  @Override
  public boolean isContent() {
    return mWrappedElement.isContent();
  }

  @Override
  public boolean isNestable() {
    return mWrappedElement.isNestable();
  }

  @Override
  public List<Chunk> getChunks() {
    return mWrappedElement.getChunks();
  }

  // Implement WriterOperation method with a stub, as iText may check the element type, and do an unsafe cast of the
  // wrapper to a WriterOperation and call write
  @Override
  public void write(PdfWriter writer, Document doc) throws DocumentException {
  }
}
