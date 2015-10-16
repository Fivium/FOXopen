package net.foxopen.fox.module.serialiser.components.pdf.htmltags;

import com.itextpdf.text.List;
import net.foxopen.fox.module.parsetree.evaluatedpresentationnode.EvaluatedHtmlPresentationNode;
import net.foxopen.fox.module.serialiser.SerialisationContext;
import net.foxopen.fox.module.serialiser.components.ComponentBuilder;
import net.foxopen.fox.module.serialiser.pdf.PDFSerialiser;
import net.foxopen.fox.module.serialiser.pdf.elementcontainers.ElementContainerFactory;

/**
 * Serialises a ol or ul tag
 */
public class ListComponentBuilder extends ComponentBuilder<PDFSerialiser, EvaluatedHtmlPresentationNode> {
  public static enum ListType {
    ORDERED,
    UNORDERED
  }

  /**
   * Determines whether the list is numbered (i.e. is a ol as opposed to ul)
   */
  private final boolean mIsNumbered;

  /**
   * Creates a list component builder of the specified type
   * @param pListType The list type
   */
  protected ListComponentBuilder(ListType pListType) {
    mIsNumbered = pListType == ListType.ORDERED;
  }

  @Override
  public void buildComponent(SerialisationContext pSerialisationContext, PDFSerialiser pSerialiser, EvaluatedHtmlPresentationNode pEvalNode) {
    List lList = pSerialiser.getElementFactory().getList();
    lList.setNumbered(mIsNumbered);

    pSerialiser.startContainer(ElementContainerFactory.getContainer(lList));
    processChildren(pSerialisationContext, pSerialiser, pEvalNode);
    pSerialiser.endContainer();

    pSerialiser.add(lList);
  }
}
