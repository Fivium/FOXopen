package net.foxopen.fox.module.serialiser.components.pdf.htmltags;

import com.google.common.collect.Iterables;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.tool.xml.html.HTML;
import net.foxopen.fox.ex.ExInternal;
import net.foxopen.fox.module.parsetree.evaluatedpresentationnode.EvaluatedHtmlPresentationNode;
import net.foxopen.fox.module.serialiser.SerialisationContext;
import net.foxopen.fox.module.serialiser.components.ComponentBuilder;
import net.foxopen.fox.module.serialiser.pdf.PDFSerialiser;
import net.foxopen.fox.module.serialiser.pdf.PDFTempSerialiser;
import net.foxopen.fox.module.serialiser.pdf.PDFTempSerialiserOutput;
import net.foxopen.fox.module.serialiser.pdf.elementcontainers.ElementContainerFactory;

import java.util.Arrays;
import java.util.List;

/**
 * Serialises a table tag
 */
public class TableComponentBuilder extends ComponentBuilder<PDFSerialiser, EvaluatedHtmlPresentationNode> {
  private static final ComponentBuilder<PDFSerialiser, EvaluatedHtmlPresentationNode> INSTANCE = new TableComponentBuilder();

  public static final ComponentBuilder<PDFSerialiser, EvaluatedHtmlPresentationNode> getInstance() {
    return INSTANCE;
  }

  private TableComponentBuilder() {
  }

  @Override
  public void buildComponent(SerialisationContext pSerialisationContext, PDFSerialiser pSerialiser, EvaluatedHtmlPresentationNode pEvalNode) {
    // Number of columns is required to construct a table, however this is not known until the table contents have been
    // processed. Serialise to the temp serialiser, and use the number of columns added in the first row of the table
    // as the number of columns to construct the table
    PDFTempSerialiser lTempSerialiser = pSerialiser.getTempSerialiser();
    processChildren(pSerialisationContext, lTempSerialiser, pEvalNode);
    PDFTempSerialiserOutput lTempSerialiserOutput = lTempSerialiser.getOutput();

    int lColumnCount = getColumnCount(lTempSerialiserOutput.getSerialisedHTMLTags());
    if (lColumnCount == 0) {
      throw new ExInternal("No table cells found under row, cannot create a table with 0 columns");
    }

    PdfPTable lTable = pSerialiser.getElementFactory().getTable(lColumnCount);
    pSerialiser.startContainer(ElementContainerFactory.getContainer(lTable));
    processChildren(pSerialisationContext, pSerialiser, pEvalNode);
    pSerialiser.endContainer();

    pSerialiser.addTableSpacer();
    pSerialiser.add(lTable);
  }

  /**
   * Return the number of table columns, determined by the number of td/th tags in the first encountered tr tag. It is
   * assumed the table is well formed.
   * @param lTags A list of the HTML tags that are children of the table
   * @return The number of table columns
   */
  private int getColumnCount(List<String> lTags) {
    int lFirstTr = lTags.indexOf(HTML.Tag.TR);
    if (lFirstTr == -1 || lFirstTr == lTags.size()) {
      // There's no table row or a table row starts and there's no cells after it, and therefore no columns
      return 0;
    }

    // Find the next tag from the first tr that isn't a td/th
    List<String> lTagsFromFirstTr = lTags.subList(lFirstTr + 1, lTags.size());
    int lNextNonCell = Iterables.indexOf(lTagsFromFirstTr, pTag -> !Arrays.asList(HTML.Tag.TH, HTML.Tag.TD).contains(pTag));

    // The row ends at the non-cell tag, or the end of the list if all remaining tags are cells
    int lRowEnd = lNextNonCell != -1 ? lNextNonCell : lTagsFromFirstTr.size();

    return lTagsFromFirstTr.subList(0, lRowEnd).size();
  }
}
