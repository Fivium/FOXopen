package net.foxopen.fox.module.serialiser.widgets.pdf;

import com.itextpdf.text.Paragraph;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.tool.xml.html.HTML;
import net.foxopen.fox.module.datanode.EvaluatedNode;
import net.foxopen.fox.module.datanode.EvaluatedNodeInfo;
import net.foxopen.fox.module.datanode.EvaluatedNodeInfoList;
import net.foxopen.fox.module.datanode.NodeAttribute;
import net.foxopen.fox.module.serialiser.SerialisationContext;
import net.foxopen.fox.module.serialiser.pdf.PDFSerialiser;
import net.foxopen.fox.module.serialiser.pdf.elementattributes.ElementAttributes;
import net.foxopen.fox.module.serialiser.pdf.elementcontainers.ElementContainerFactory;
import net.foxopen.fox.module.serialiser.widgets.WidgetBuilder;

import java.util.List;

public class FormListWidgetBuilder extends WidgetBuilderPDFSerialiser<EvaluatedNodeInfoList> {
  private static final String LIST_TAG = HTML.Tag.TABLE;
  private static final String LIST_ROW_TAG = HTML.Tag.TR;

  private static final WidgetBuilder<PDFSerialiser, EvaluatedNodeInfoList> INSTANCE = new FormListWidgetBuilder();

  public static final WidgetBuilder<PDFSerialiser, EvaluatedNodeInfoList> getInstance() {
    return INSTANCE;
  }

  private FormListWidgetBuilder() {
  }

  @Override
  public boolean hasPrompt(EvaluatedNodeInfoList pEvalNode) {
    return pEvalNode.isNestedList() && pEvalNode.hasPrompt();
  }

  @Override
  public void buildWidget(SerialisationContext pSerialisationContext, PDFSerialiser pSerialiser, EvaluatedNodeInfoList pEvalNode) {
    buildWidgetInternal(pSerialisationContext, pSerialiser, pEvalNode);
  }

  @Override
  public void buildWidgetInternal(SerialisationContext pSerialisationContext, PDFSerialiser pSerialiser, EvaluatedNodeInfoList pEvalNode) {
    if (pEvalNode.getChildren().size() > 0) {
      ElementAttributes lElementAttributes = pSerialiser.getInheritedElementAttributes();
      pSerialiser.getCSSResolver().resolveStyles(lElementAttributes, LIST_TAG, getListClasses(pEvalNode), getListStyles(pEvalNode));
      pSerialiser.pushElementAttributes(lElementAttributes);

      // The list of forms is generated as a table with one column, with a form set-out per row
      PdfPTable lListTable = pSerialiser.getElementFactory().getTable(1);
      pSerialiser.startContainer(ElementContainerFactory.getContainer(lListTable));

      addFormRows(pSerialisationContext, pSerialiser, pEvalNode);

      pSerialiser.endContainer();
      if (!pEvalNode.isNested()) {
        pSerialiser.addTableSpacer();
      }
      pSerialiser.add(lListTable);
      pSerialiser.popElementAttributes();
    }
  }

  /**
   * Serialises the form set-out rows
   * @param pSerialisationContext
   * @param pSerialiser
   * @param pEvalNode The list node
   */
  private void addFormRows(SerialisationContext pSerialisationContext, PDFSerialiser pSerialiser, EvaluatedNodeInfoList pEvalNode) {
    pEvalNode.getChildren().forEach(pRowNode -> addFormRow(pSerialisationContext, pSerialiser, pRowNode));
  }

  /**
   * Serialises a form row
   * @param pSerialisationContext
   * @param pSerialiser
   * @param pRowNode The form node
   */
  private void addFormRow(SerialisationContext pSerialisationContext, PDFSerialiser pSerialiser, EvaluatedNodeInfo pRowNode) {
    ElementAttributes lElementAttributes = pSerialiser.getInheritedElementAttributes();
    pSerialiser.getCSSResolver().resolveStyles(lElementAttributes, LIST_ROW_TAG, getRowClasses(pRowNode), getRowStyles(pRowNode));
    pSerialiser.pushElementAttributes(lElementAttributes);

    PdfPCell lCell = pSerialiser.getElementFactory().getCell();
    Paragraph lCellParagraph = pSerialiser.getElementFactory().getParagraph();

    pSerialiser.startContainer(ElementContainerFactory.getContainer(lCell, lCellParagraph));
    pSerialiser.getWidgetBuilder(pRowNode.getWidgetBuilderType()).buildWidget(pSerialisationContext, pSerialiser, pRowNode);
    pSerialiser.endContainer();
    pSerialiser.add(lCell);

    pSerialiser.popElementAttributes();
  }

  /**
   * Returns a list of classes that should be applied when styling the list
   * @param pListNode The list node
   * @return A list of classes that should be applied when styling the list
   */
  private List<String> getListClasses(EvaluatedNode pListNode) {
    List<String> lClasses = pListNode.getStringAttributes(NodeAttribute.CLASS, NodeAttribute.LIST_CLASS);

    if (pListNode.isNested()) {
      lClasses.addAll(pListNode.getStringAttributes(NodeAttribute.NESTED_CLASS, NodeAttribute.NESTED_TABLE_CLASS, NodeAttribute.NESTED_LIST_CLASS));
    }

    return lClasses;
  }

  /**
   * Returns a list of styles that should be applied when styling the list
   * @param pListNode The list node
   * @return A list of styles that should be applied when styling the list
   */
  private List<String> getListStyles(EvaluatedNode pListNode) {
    List<String> lStyles = pListNode.getStringAttributes(NodeAttribute.STYLE, NodeAttribute.LIST_STYLE);

    if (pListNode.isNested()) {
      lStyles.addAll(pListNode.getStringAttributes(NodeAttribute.NESTED_STYLE, NodeAttribute.NESTED_TABLE_STYLE, NodeAttribute.NESTED_LIST_STYLE));
    }

    return lStyles;
  }

  /**
   * Returns a list of classes that should be applied when styling the list form row
   * @param pRowNode The row node
   * @return A list of classes that should be applied when styling the list form row
   */
  private List<String> getRowClasses(EvaluatedNode pRowNode) {
    return pRowNode.getStringAttributes(NodeAttribute.CLASS, NodeAttribute.LIST_ROW_CLASS);
  }

  /**
   * Returns a list of styles that should be applied when styling the list form row
   * @param pRowNode The row node
   * @return A list of styles that should be applied when styling the list form row
   */
  private List<String> getRowStyles(EvaluatedNode pRowNode) {
    return pRowNode.getStringAttributes(NodeAttribute.STYLE, NodeAttribute.LIST_ROW_STYLE);
  }
}
