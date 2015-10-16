package net.foxopen.fox.module.serialiser.widgets.pdf;

import net.foxopen.fox.module.datanode.EvaluatedNode;
import net.foxopen.fox.module.serialiser.SerialisationContext;
import net.foxopen.fox.module.serialiser.pdf.PDFSerialiser;

/**
 * Contains methods for serialising input field content
 * @param <EN> The {@link EvaluatedNode} type of the widget
 */
@FunctionalInterface
public interface InputFieldContent<EN extends EvaluatedNode> {
  /**
   * Serialises input field content to be added to an input field div wrapper
   * @param pSerialisationContext
   * @param pSerialiser
   * @param pEvalNode
   */
  public void addContent(SerialisationContext pSerialisationContext, PDFSerialiser pSerialiser, EN pEvalNode);
}
