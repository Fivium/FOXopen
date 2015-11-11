package net.foxopen.fox.module.serialiser.widgets.pdf;

import com.itextpdf.text.Paragraph;
import com.itextpdf.text.Phrase;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.tool.xml.html.HTML;
import net.foxopen.fox.module.RenderTypeOption;
import net.foxopen.fox.module.datanode.EvaluatedNodeInfo;
import net.foxopen.fox.module.datanode.NodeAttribute;
import net.foxopen.fox.module.fieldset.fvm.FieldSelectOption;
import net.foxopen.fox.module.serialiser.FOXGridUtils;
import net.foxopen.fox.module.serialiser.SerialisationContext;
import net.foxopen.fox.module.serialiser.layout.GridLayoutManager;
import net.foxopen.fox.module.serialiser.layout.items.LayoutFieldValueMappingItemColumn;
import net.foxopen.fox.module.serialiser.layout.methods.FieldValueMappingLayout;
import net.foxopen.fox.module.serialiser.pdf.PDFSerialiser;
import net.foxopen.fox.module.serialiser.pdf.elementattributes.ElementAttributes;
import net.foxopen.fox.module.serialiser.pdf.elementcontainers.ElementContainerFactory;
import net.foxopen.fox.module.serialiser.widgets.OptionWidgetUtils;

import java.util.Arrays;
import java.util.List;


/**
 * Contains the layout code for radio buttons and tickboxes as they're pretty much the same thing
 */
public class MultiOptionSelectWidget extends WidgetBuilderPDFSerialiser<EvaluatedNodeInfo> {
  private static final String MULTI_OPTION_CELL_TAG = HTML.Tag.TD;
  private static final String MULTI_OPTION_CELL_CLASS = "multiOptionCell";
  private static final float FIELD_TEXT_SPACING = 3f;

  private final String mSelectedFieldText;
  private final String mDeselectedFieldText;

  protected MultiOptionSelectWidget(String pSelectedFieldText, String pDeselectedFieldText) {
    mSelectedFieldText = pSelectedFieldText;
    mDeselectedFieldText = pDeselectedFieldText;
  }

  @Override
  public void buildWidgetInternal(SerialisationContext pSerialisationContext, PDFSerialiser pSerialiser, EvaluatedNodeInfo pEvalNode) {
    int lItemsPerRow = OptionWidgetUtils.getItemsPerRow(pEvalNode);
    GridLayoutManager lMultiOptionLayout = new GridLayoutManager(lItemsPerRow, pSerialiser, pEvalNode, FieldValueMappingLayout.getInstance());
    boolean lHideField = getRenderType(pEvalNode) == RenderTypeOption.PRINT;

    if (lMultiOptionLayout.getLayoutItems().size() > 0) {
      boolean lIsSingleOption = lMultiOptionLayout.getFilledColumnCount() == 1;
      PdfPTable lOptionTable = pSerialiser.getElementFactory().getTable(FOXGridUtils.getMaxColumns());

      lMultiOptionLayout.getLayoutItems().forEach(lLayoutItem -> {
        switch (lLayoutItem.getItemType()) {
          case ROW_START:
          case ROW_END:
            break;
          case COLUMN:
            PdfPCell lFormCell = createColumnItem(pSerialiser, pEvalNode, (LayoutFieldValueMappingItemColumn) lLayoutItem, lIsSingleOption ? 1 : lItemsPerRow, lHideField);
            lOptionTable.addCell(lFormCell);
            break;
        }
      });

      if (!pEvalNode.isNested()) {
        pSerialiser.addTableSpacer();
      }

      pSerialiser.add(lOptionTable);
    }
  }

  private PdfPCell createColumnItem(PDFSerialiser pSerialiser, EvaluatedNodeInfo pEvalNode,
                                    LayoutFieldValueMappingItemColumn pColumnItem, int pItemsPerRow, boolean pHideField) {
    ElementAttributes lElementAttributes = pSerialiser.getInheritedElementAttributes();
    pSerialiser.getCSSResolver().resolveStyles(lElementAttributes, MULTI_OPTION_CELL_TAG,
                                               getColumnItemClasses(pEvalNode), getColumnItemStyles(pEvalNode));
    pSerialiser.pushElementAttributes(lElementAttributes);

    PdfPCell lFieldCell = pSerialiser.getElementFactory().getCell();
    Paragraph lCellParagraph = pSerialiser.getElementFactory().getParagraph();
    lFieldCell.setColspan(FOXGridUtils.calculateAdjustedColumnSpan(pColumnItem.getColSpan(), pItemsPerRow));

    pSerialiser.startContainer(ElementContainerFactory.getContainer(lFieldCell, lCellParagraph));
    addColumnItemContent(pSerialiser, pColumnItem, pHideField);
    pSerialiser.endContainer();

    pSerialiser.popElementAttributes();

    return lFieldCell;
  }

  private List<String> getColumnItemClasses(EvaluatedNodeInfo pEvalNode) {
    List<String> lClasses = Arrays.asList(MULTI_OPTION_CELL_CLASS);
    lClasses.addAll(pEvalNode.getStringAttributes(NodeAttribute.KEY_CLASS));

    return lClasses;
  }

  private List<String> getColumnItemStyles(EvaluatedNodeInfo pEvalNode) {
    return pEvalNode.getStringAttributes(NodeAttribute.KEY_STYLE);
  }

  private void addColumnItemContent(PDFSerialiser pSerialiser, LayoutFieldValueMappingItemColumn pColumnItem, boolean pHideField) {
    if (!pColumnItem.isFiller()) {
      Phrase lPhrase = pSerialiser.getElementFactory().getPhrase();
      FieldSelectOption lSelectOption = pColumnItem.getFieldSelectOption();

      pSerialiser.startContainer(ElementContainerFactory.getContainer(lPhrase));

      if (!pHideField) {
        String lFieldText = lSelectOption.isSelected() ? mSelectedFieldText : mDeselectedFieldText;
        pSerialiser.addText(lFieldText);
        lPhrase.getChunks().stream().findFirst().ifPresent(pFieldChunk -> pFieldChunk.setCharacterSpacing(FIELD_TEXT_SPACING));
      }

      pSerialiser.addText(lSelectOption.getDisplayKey());
      pSerialiser.endContainer();
      pSerialiser.add(lPhrase);
    }
  }
}
