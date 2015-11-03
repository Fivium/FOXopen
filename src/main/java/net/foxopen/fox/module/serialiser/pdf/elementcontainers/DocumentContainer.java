package net.foxopen.fox.module.serialiser.pdf.elementcontainers;

import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Element;
import net.foxopen.fox.ex.ExInternal;

/**
 * A container for elements to be added to a document.
 */
public class DocumentContainer  implements ElementContainer {
  private final Document mDocument;

  /**
   * Create a container for elements to be added to a document
   * @param pDocument The containing document
   */
  public DocumentContainer(Document pDocument) {
    mDocument = pDocument;
  }

  @Override
  public boolean isSuppressNewPageTemplates() {
    return false;
  }

  /**
   * Adds an element to the document
   * @param pChildElement The element to be added
   * @throws ExInternal If the document is unable to add the element, for example if the document is not open
   */
  @Override
  public void addChildElement(Element pChildElement) throws ExInternal {
    try {
      mDocument.add(pChildElement);
    }
    catch (DocumentException e) {
      throw new ExInternal("Failed to add element " + pChildElement.toString() + " to document", e);
    }
  }
}
