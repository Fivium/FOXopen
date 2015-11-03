package net.foxopen.fox.module.serialiser.widgets.pdf;

import com.itextpdf.text.Paragraph;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.tool.xml.html.HTML;
import net.foxopen.fox.module.datanode.EvaluatedNode;
import net.foxopen.fox.module.datanode.EvaluatedNodeInfo;
import net.foxopen.fox.module.datanode.EvaluatedNodeInfoList;
import net.foxopen.fox.module.datanode.NodeAttribute;
import net.foxopen.fox.module.datanode.NodeVisibility;
import net.foxopen.fox.module.serialiser.SerialisationContext;
import net.foxopen.fox.module.serialiser.pdf.PDFSerialiser;
import net.foxopen.fox.module.serialiser.pdf.elementattributes.ElementAttributes;
import net.foxopen.fox.module.serialiser.pdf.elementcontainers.ElementContainerFactory;
import net.foxopen.fox.module.serialiser.widgets.WidgetBuilder;
import net.foxopen.fox.module.serialiser.widgets.WidgetBuilderType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Serialises a list set-out
 */
public class ListWidgetBuilder extends WidgetBuilderPDFSerialiser<EvaluatedNodeInfoList> {
  private static final String LIST_TAG = HTML.Tag.TABLE;
  private static final String LIST_ROW_TAG = HTML.Tag.TR;
  private static final String LIST_CELL_TAG = HTML.Tag.TD;
  /**
   * The class applied to list cells when the list is a top-level list
   */
  private static final String LIST_CELL_CLASS = "listCell";
  /**
   * The class applied to list cells when the list is a nested list
   */
  private static final String NESTED_LIST_CELL_CLASS = "nestedListCell";
  /**
   * The class applied to list header cells in conjunction with the top-level or nested list cell class
   */
  private static final String LIST_HEADER_CELL_CLASS = "listHeaderCell";
  private static final WidgetBuilder<PDFSerialiser, EvaluatedNodeInfoList> INSTANCE = new ListWidgetBuilder();

  public static final WidgetBuilder<PDFSerialiser, EvaluatedNodeInfoList> getInstance() {
    return INSTANCE;
  }

  private ListWidgetBuilder() {
  }

  @Override
  public boolean hasPrompt(EvaluatedNodeInfoList pEvalNode) {
    // Nested lists may have a form prompt
    return pEvalNode.isNestedList() && pEvalNode.hasPrompt();
  }

  @Override
  public void buildWidget(SerialisationContext pSerialisationContext, PDFSerialiser pSerialiser, EvaluatedNodeInfoList pEvalNode) {
    buildWidgetInternal(pSerialisationContext, pSerialiser, pEvalNode);
  }

  @Override
  public void buildWidgetInternal(SerialisationContext pSerialisationContext, PDFSerialiser pSerialiser, EvaluatedNodeInfoList pEvalNode) {
    ElementAttributes lElementAttributes = pSerialiser.getInheritedElementAttributes();
    pSerialiser.getCSSResolver().resolveStyles(lElementAttributes, LIST_TAG, getListClasses(pEvalNode), getListStyles(pEvalNode));
    pSerialiser.pushElementAttributes(lElementAttributes);

    List<EvaluatedNodeInfo> lListColumns = pEvalNode.getColumns();
    // Ensure column count is 1 when no columns exist to allow the list header to come out instead of nothing
    int lListColumnCount = Math.max(lListColumns.size(), 1);
    PdfPTable lListTable = pSerialiser.getElementFactory().getTable(lListColumnCount);
    pSerialiser.startContainer(ElementContainerFactory.getContainer(lListTable));

    // Only add column headers if not nested within another list (in html output, the thead of a list in a list is
    // hidden by the css)
    if (!pEvalNode.checkAncestry(WidgetBuilderType.LIST)) {
      addColumnHeaders(pSerialiser, lListColumns);
    }

    addBodyCells(pSerialisationContext, pSerialiser, pEvalNode, lListColumns);

    pSerialiser.endContainer();
    if (!pEvalNode.isNested()) {
      pSerialiser.addTableSpacer();
    }
    pSerialiser.add(lListTable);
    pSerialiser.popElementAttributes();
  }

  /**
   * Serialises the column headers of the list
   * @param pSerialiser
   * @param pColumnNodes The list columns nodes
   */
  private void addColumnHeaders(PDFSerialiser pSerialiser, List<EvaluatedNodeInfo> pColumnNodes) {
    pColumnNodes.stream()
                .forEach(pHeaderNode -> addColumnHeader(pSerialiser, pHeaderNode));
  }

  /**
   * Serialises a column header
   * @param pSerialiser
   * @param pHeaderNode The column header node
   */
  private void addColumnHeader(PDFSerialiser pSerialiser, EvaluatedNodeInfo pHeaderNode) {
    ElementAttributes lElementAttributes = pSerialiser.getInheritedElementAttributes();
    pSerialiser.getCSSResolver().resolveStyles(lElementAttributes, LIST_CELL_TAG, getHeaderClasses(pHeaderNode), getHeaderStyles(pHeaderNode));
    pSerialiser.pushElementAttributes(lElementAttributes);

    PdfPCell lHeaderCell = pSerialiser.getElementFactory().getCell();

    // Add the header prompt to the cell if one exists
    Optional.ofNullable(pHeaderNode.getSummaryPrompt()).ifPresent(pEvaluatedPrompt -> {
      Paragraph lCellParagraph = pSerialiser.getElementFactory().getParagraph();
      pSerialiser.startContainer(ElementContainerFactory.getContainer(lHeaderCell, lCellParagraph));
      pSerialiser.addParagraphText(pSerialiser.getSafeStringAttribute(pEvaluatedPrompt));
      pSerialiser.endContainer();
    });

    pSerialiser.add(lHeaderCell);
    pSerialiser.popElementAttributes();
  }

  /**
   * Serialises the body cells of a list
   * @param pSerialisationContext
   * @param pSerialiser
   * @param pListNode The list node
   * @param pColumnNodes The list column nodes
   */
  private void addBodyCells(SerialisationContext pSerialisationContext, PDFSerialiser pSerialiser,
                            EvaluatedNodeInfoList pListNode, List<EvaluatedNodeInfo> pColumnNodes) {
    pListNode.getChildren()
             .stream()
             .forEach(pRowNode -> addBodyRowCells(pSerialisationContext, pSerialiser, pRowNode, pColumnNodes, pListNode.isNested()));
  }

  /**
   * Serialises the cells of a body row
   * @param pSerialisationContext
   * @param pSerialiser
   * @param pRowNode The body row
   * @param pColumnNodes The list columns
   * @param pIsNestedList Whether or not the list is nested, a different class is applied to nested list cells
   */
  private void addBodyRowCells(SerialisationContext pSerialisationContext, PDFSerialiser pSerialiser,
                               EvaluatedNodeInfo pRowNode, List<EvaluatedNodeInfo> pColumnNodes, boolean pIsNestedList) {
    // iText does not use explicit rows when adding cells to a table (a row is added internally when the number of cells
    // added reaches the column limit), so process node children after the styles on the row have been applied
    ElementAttributes lElementAttributes = pSerialiser.getInheritedElementAttributes();
    pSerialiser.getCSSResolver().resolveStyles(lElementAttributes, LIST_ROW_TAG, getRowClasses(pRowNode), getRowStyles(pRowNode));
    pSerialiser.pushElementAttributes(lElementAttributes);

    getRowItemNodes(pRowNode, pColumnNodes).stream()
                                           .forEach(pItemNode -> addBodyCell(pSerialisationContext, pSerialiser, pItemNode, pIsNestedList));

    pSerialiser.popElementAttributes();
  }

  /**
   * Serialises a list body cell
   * @param pSerialisationContext
   * @param pSerialiser
   * @param pItemNode The item node of the cell if the row and column of the cell has content
   * @param pIsNestedList Whether or not the list if nested, a different class is applied to nested list cells
   */
  private void addBodyCell(SerialisationContext pSerialisationContext, PDFSerialiser pSerialiser, Optional<EvaluatedNodeInfo> pItemNode, boolean pIsNestedList) {
    ElementAttributes lElementAttributes = pSerialiser.getInheritedElementAttributes();
    pSerialiser.getCSSResolver().resolveStyles(lElementAttributes, LIST_CELL_TAG, getCellClasses(pItemNode, pIsNestedList), getCellStyles(pItemNode));
    pSerialiser.pushElementAttributes(lElementAttributes);

    PdfPCell lCell = pSerialiser.getElementFactory().getCell();
    Paragraph lCellParagraph = pSerialiser.getElementFactory().getParagraph();

    if (pItemNode.isPresent() && pItemNode.get().getVisibility() != NodeVisibility.DENIED) {
      // Column was found for this row and it isn't denied visibility, add content
      pSerialiser.startContainer(ElementContainerFactory.getContainer(lCell, lCellParagraph));
      pSerialiser.getWidgetBuilder(pItemNode.get().getWidgetBuilderType()).buildWidget(pSerialisationContext, pSerialiser, pItemNode.get());
      pSerialiser.endContainer();
    }

    pSerialiser.add(lCell);
    pSerialiser.popElementAttributes();
  }

  /**
   * Returns an ordered list of the item nodes in the specified row node. Item nodes are optional as a row node may not
   * contain an item node for certain columns (i.e. the cell is empty for that row and column).
   * @param pRowNode The body row
   * @param pColumnNodes The list columns
   * @return An ordered list of the item nodes in the specified row node
   */
  private List<Optional<EvaluatedNodeInfo>> getRowItemNodes(EvaluatedNodeInfo pRowNode, List<EvaluatedNodeInfo> pColumnNodes) {
    // A row node contains a map from a column node to the item node for that column in the row, collect a list of the
    // item nodes for each column (this is optional for the case when no item exists for the individual column and row)
    return pColumnNodes.stream()
                       .map(pColumnNode -> Optional.ofNullable(pRowNode.getChildrenMap().get(pColumnNode.getNodeInfo())))
                       .collect(Collectors.toList());
  }

  /**
   * Returns a list of classes that should be applied when styling the list
   * @param pListNode The list node
   * @return A list of classes that should be applied when styling the list
   */
  private List<String> getListClasses(EvaluatedNode pListNode) {
    List<String> lClasses = pListNode.getStringAttributes(NodeAttribute.CLASS, NodeAttribute.LIST_CLASS);

    if (pListNode.isNested()) {
      lClasses.addAll(pListNode.getStringAttributes(NodeAttribute.NESTED_TABLE_CLASS));
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
      lStyles.addAll(pListNode.getStringAttributes(NodeAttribute.NESTED_TABLE_STYLE));
    }

    return lStyles;
  }

  /**
   * Returns a list of classes that should be applied when styling a header cell
   * @param pHeaderNode The header cell node
   * @return A list of classes that should be applied when styling a header cell
   */
  private List<String> getHeaderClasses(EvaluatedNode pHeaderNode) {
    // Headers get both the list cell and list header cell class
    List<String> lClasses = new ArrayList<>(Arrays.asList(LIST_CELL_CLASS, LIST_HEADER_CELL_CLASS));
    lClasses.addAll(pHeaderNode.getStringAttributes(NodeAttribute.LIST_TABLE_CLASS, NodeAttribute.LIST_CELL_CLASS, NodeAttribute.CELL_CLASS, NodeAttribute.PROMPT_CLASS));

    return lClasses;
  }

  /**
   * Returns a list of styles that should be applied when styling a header cell
   * @param pHeaderNode The header cell node
   * @return A list of styles that should be applied when styling a header cell
   */
  private List<String> getHeaderStyles(EvaluatedNode pHeaderNode) {
    return pHeaderNode.getStringAttributes(NodeAttribute.LIST_TABLE_STYLE, NodeAttribute.LIST_CELL_STYLE, NodeAttribute.CELL_STYLE, NodeAttribute.PROMPT_STYLE);
  }

  /**
   * Returns a list of classes that should be applied when styling the list body row
   * @param pRowNode The row node
   * @return A list of classes that should be applied when styling the list body row
   */
  private List<String> getRowClasses(EvaluatedNode pRowNode) {
    return pRowNode.getStringAttributes(NodeAttribute.CLASS, NodeAttribute.LIST_ROW_CLASS);
  }

  /**
   * Returns a list of styles that should be applied when styling the list body row
   * @param pRowNode The row node
   * @return A list of styles that should be applied when styling the list body row
   */
  private List<String> getRowStyles(EvaluatedNode pRowNode) {
    return pRowNode.getStringAttributes(NodeAttribute.STYLE, NodeAttribute.LIST_ROW_STYLE);
  }

  /**
   * Returns a list of classes that should be applied when styling the body cell. Nested list cells have a different
   * class to top-level list cells. Additional classes are resolved from the cell item node if applicable.
   * @param pItemNode The cell item node if the cell row and column has a content node
   * @param pIsNested Whether or not the list is nested (otherwise the list is treated as top-level)
   * @return A list of classes that should be applied when styling the body cell
   */
  private List<String> getCellClasses(Optional<? extends EvaluatedNode> pItemNode, boolean pIsNested) {
    // Nested and list cell classes are exclusive - a cell doesn't get both classes
    List<String> lClasses = new ArrayList<>(Arrays.asList(pIsNested ? NESTED_LIST_CELL_CLASS : LIST_CELL_CLASS));

    pItemNode.ifPresent(pPresentItemNode -> {
      lClasses.addAll(pPresentItemNode.getStringAttributes(NodeAttribute.LIST_TABLE_CLASS, NodeAttribute.LIST_CELL_CLASS, NodeAttribute.FIELD_CELL_CLASS, NodeAttribute.CELL_CLASS));
    });

    return lClasses;
  }

  /**
   * Returns a list of styles that should be applied when styling the body cell. Styles are resolved from the cell item
   * node if applicable.
   * @param pItemNode The cell item node if the cell row and column has a content node
   * @return A list of styles that should be applied when styling the body cell
   */
  private List<String> getCellStyles(Optional<? extends EvaluatedNode> pItemNode) {
    List<String> lStyles = new LinkedList<>();

    pItemNode.ifPresent(pPresentItemNode -> {
      lStyles.addAll(pPresentItemNode.getStringAttributes(NodeAttribute.LIST_TABLE_STYLE, NodeAttribute.LIST_CELL_STYLE, NodeAttribute.FIELD_CELL_STYLE, NodeAttribute.CELL_STYLE));
    });

    return lStyles;
  }
}
