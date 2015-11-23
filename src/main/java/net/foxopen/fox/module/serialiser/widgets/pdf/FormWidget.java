package net.foxopen.fox.module.serialiser.widgets.pdf;

import com.google.common.collect.Multimap;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.tool.xml.html.HTML;
import net.foxopen.fox.module.LayoutDirection;
import net.foxopen.fox.module.datanode.EvaluatedNode;
import net.foxopen.fox.module.datanode.EvaluatedNodeInfo;
import net.foxopen.fox.module.datanode.NodeAttribute;
import net.foxopen.fox.module.serialiser.FOXGridUtils;
import net.foxopen.fox.module.serialiser.SerialisationContext;
import net.foxopen.fox.module.serialiser.layout.GridLayoutManager;
import net.foxopen.fox.module.serialiser.layout.items.LayoutItem;
import net.foxopen.fox.module.serialiser.layout.items.LayoutWidgetItemColumn;
import net.foxopen.fox.module.serialiser.pdf.PDFSerialiser;
import net.foxopen.fox.module.serialiser.pdf.elementattributes.ElementAttributes;
import net.foxopen.fox.module.serialiser.pdf.elementcontainers.ElementContainerFactory;
import net.foxopen.fox.module.serialiser.widgets.FormWidgetUtils;
import net.foxopen.fox.module.serialiser.widgets.WidgetBuilderType;

import java.util.EnumMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Serialises a form or cellmate widget
 */
public class FormWidget extends WidgetBuilderPDFSerialiser<EvaluatedNodeInfo> {
  /**
   * The possible parts of a form set-out
   */
  public enum FormPart {
    FORM,
    TOP_LEVEL_FORM,
    NESTED_FORM,
    FIELD_CELL,
    PROMPT_CELL,
  }

  private static final String FORM_TAG = HTML.Tag.TABLE;
  private static final String FORM_CELL_TAG = HTML.Tag.TD;
  /**
   * The class applied to all prompt cells in the form
   */
  private static final String FORM_PROMPT_CELL_CLASS = "formPromptCell";
  /**
   * The class applied to all non-prompt, non-filler cells
   */
  private static final String FORM_CELL_CLASS = "formCell";
  /**
   * Additional classes may be applied to the form cell based on the widget type of the nested content
   */
  private static final Map<WidgetBuilderType, String> FORM_CELL_NESTED_CONTENT_CLASSES = new EnumMap<>(WidgetBuilderType.class);
  static {
    FORM_CELL_NESTED_CONTENT_CLASSES.put(WidgetBuilderType.FORM, "nestedForm");
    FORM_CELL_NESTED_CONTENT_CLASSES.put(WidgetBuilderType.LIST, "nestedList");
    FORM_CELL_NESTED_CONTENT_CLASSES.put(WidgetBuilderType.FORM_LIST, "nestedList");
    FORM_CELL_NESTED_CONTENT_CLASSES.put(WidgetBuilderType.CELLMATES, "nestedCellmates");
    FORM_CELL_NESTED_CONTENT_CLASSES.put(WidgetBuilderType.RADIO, "nestedRadioGroup");
    FORM_CELL_NESTED_CONTENT_CLASSES.put(WidgetBuilderType.TICKBOX, "nestedTickboxGroup");
  }

  private final Multimap<FormPart, NodeAttribute> mFormNodeClassAttributes;
  private final Multimap<FormPart, NodeAttribute> mFormNodeStyleAttributes;

  /**
   * Creates a form widget that takes classes and styles using the specified node attributes for each part of the form
   * @param pFormNodeClassAttributes The node attributes to take classes from for each part of the form set-out
   * @param pFormNodeStyleAttributes The node attributes to take styles from for each part of the form set-out
   */
  public FormWidget(Multimap<FormPart, NodeAttribute> pFormNodeClassAttributes, Multimap<FormPart, NodeAttribute> pFormNodeStyleAttributes) {
    mFormNodeClassAttributes = pFormNodeClassAttributes;
    mFormNodeStyleAttributes = pFormNodeStyleAttributes;
  }

  @Override
  public boolean hasPrompt(EvaluatedNodeInfo pEvalNode) {
    EvaluatedNode lFormParent = pEvalNode.getParent();
    boolean lIsNestedForm = lFormParent != null && lFormParent.getWidgetBuilderType() == WidgetBuilderType.FORM;

    // Nested forms may have a prompt
    return lIsNestedForm && pEvalNode.hasPrompt();
  }

  @Override
  public void buildWidget(SerialisationContext pSerialisationContext, PDFSerialiser pSerialiser, EvaluatedNodeInfo pEvalNode) {
    buildWidgetInternal(pSerialisationContext, pSerialiser, pEvalNode);
  }

  @Override
  public void buildWidgetInternal(SerialisationContext pSerialisationContext, PDFSerialiser pSerialiser, EvaluatedNodeInfo pEvalNode) {
    // Do not set-out anything if an empty form
    if (!pEvalNode.hasChildren()) {
      return;
    }

    ElementAttributes lElementAttributes = pSerialiser.getInheritedElementAttributes();
    pSerialiser.getCSSResolver().resolveStyles(lElementAttributes, FORM_TAG, getFormClasses(pEvalNode), getFormStyles(pEvalNode));
    pSerialiser.pushElementAttributes(lElementAttributes);

    // Form tables are created with the maximum grid column, cell spans are adjusted based on this
    PdfPTable lFormTable = pSerialiser.getElementFactory().getTable(FOXGridUtils.getMaxColumns());
    pSerialiser.startContainer(ElementContainerFactory.getContainer(lFormTable));

    int lFormColumns = FormWidgetUtils.getFormColumns(pEvalNode);
    GridLayoutManager lFormLayout = new GridLayoutManager(lFormColumns, pSerialiser, pEvalNode);

    Set<Integer> lNorthPromptRowIndexes = new HashSet<>();
    boolean lLastRowHasNorthPromptFields = false;

    for (LayoutItem lLayoutItem : lFormLayout.getLayoutItems()) {
      switch (lLayoutItem.getItemType()) {
        case ROW_START:
          if (lLastRowHasNorthPromptFields) {
            // Add a dummy row that isn't kept-together so that the prompt north and field can be kept-together, but
            // still split from further rows. For example, if there is a form with 4 rows: prompt, field, prompt, field,
            // setting all of them to be keep-together means they can't split at all, so the dummy row in between the
            // first and second pair of prompt/fields allows them to split
            PdfPCell lCell = new PdfPCell();
            lCell.setColspan(lFormTable.getNumberOfColumns());
            lCell.setBorder(PdfPCell.NO_BORDER);
            lFormTable.addCell(lCell);

            lLastRowHasNorthPromptFields = false;
          }
          break;
        case ROW_END:
          break;
        case COLUMN:
          LayoutWidgetItemColumn lLayoutWidgetItemColumn = (LayoutWidgetItemColumn) lLayoutItem;
          boolean lIsField = !lLayoutWidgetItemColumn.isFiller() && !lLayoutWidgetItemColumn.isPrompt();

          if (lIsField && getPromptLayoutDirection(lLayoutWidgetItemColumn.getItemNode()) == LayoutDirection.NORTH) {
            // Record the last completed row index as one with prompt norths in it
            // Check that this isn't the very top row, where the last completed index would be -1
            if (lFormTable.getLastCompletedRowIndex() >= 0) {
              lNorthPromptRowIndexes.add(lFormTable.getLastCompletedRowIndex());
            }

            // Set flag so that a dummy row is added below the prompt north field, which allows the prompt and field to
            // be set as stay together but still split from other north prompt/field pairs
            lLastRowHasNorthPromptFields = true;
          }

          addColumnItem(pSerialisationContext, pSerialiser, lLayoutWidgetItemColumn, lFormColumns);
          break;
      }
    }

    setKeepNorthPromptFieldsTogether(lFormTable, lNorthPromptRowIndexes);

    pSerialiser.endContainer();
    if (!pEvalNode.isNested()) {
      pSerialiser.addTableSpacer();
    }
    pSerialiser.add(lFormTable);
    pSerialiser.popElementAttributes();
  }

  /**
   * Serialises a table cell of the column item content with cell styles applied
   * @param pSerialisationContext
   * @param pSerialiser
   * @param pColumnItem The column item to create a cell for
   * @param pFormColumns The number of columns in the form, used to determine field column spans
   */
  private void addColumnItem(SerialisationContext pSerialisationContext, PDFSerialiser pSerialiser, LayoutWidgetItemColumn pColumnItem, int pFormColumns) {
    ElementAttributes lElementAttributes = pSerialiser.getInheritedElementAttributes();
    pSerialiser.getCSSResolver().resolveStyles(lElementAttributes, FORM_CELL_TAG, getColumnItemCellClasses(pColumnItem), getColumnItemCellStyles(pColumnItem));
    pSerialiser.pushElementAttributes(lElementAttributes);

    PdfPCell lFormCell = pSerialiser.getElementFactory().getCell();
    Paragraph lCellParagraph = pSerialiser.getElementFactory().getParagraph();
    lFormCell.setColspan(FOXGridUtils.calculateAdjustedColumnSpan(pColumnItem.getColSpan(), pFormColumns));

    pSerialiser.startContainer(ElementContainerFactory.getContainer(lFormCell, lCellParagraph));
    addColumnItemContent(pSerialisationContext, pSerialiser, pColumnItem);
    pSerialiser.endContainer();

    pSerialiser.add(lFormCell);
    pSerialiser.popElementAttributes();
  }

  /**
   * Serialises the content of the column item, building a prompt or widget depending on the column item type
   * @param pSerialisationContext
   * @param pSerialiser
   * @param pColumnItem The column item to serialise content for
   */
  private void addColumnItemContent(SerialisationContext pSerialisationContext, PDFSerialiser pSerialiser, LayoutWidgetItemColumn pColumnItem) {
    // Do not add the item if it is not set to be initially displayed (i.e. a client visibility rule has hidden it)
    if (!pColumnItem.isFiller() && pColumnItem.getItemNode().isInitiallyDisplayed()) {
      if (pColumnItem.isPrompt()) {
        pColumnItem.getWidgetBuilder().buildPrompt(pSerialisationContext, pSerialiser, pColumnItem.getItemNode());
      }
      else {
        pColumnItem.getWidgetBuilder().buildWidget(pSerialisationContext, pSerialiser, pColumnItem.getItemNode());
      }
    }
  }

  /**
   * Sets the table so that if possible, prompt norths are not split from their corresponding field on the row below
   * @param pTable The form table
   * @param pNorthPromptRowIndexes The row indexes that contain prompt norths
   */
  private void setKeepNorthPromptFieldsTogether(PdfPTable pTable, Set<Integer> pNorthPromptRowIndexes) {
    // The row index of the field is the row below the prompt - if there is a row below the prompt, set it so that the
    // table shouldn't split the prompt and field row
    pNorthPromptRowIndexes.stream()
                          .filter(pPromptRowIndex -> pPromptRowIndex < pTable.size() - 1)
                          .forEach(pPromptRowIndex -> pTable.keepRowsTogether(pPromptRowIndex, pPromptRowIndex + 2));
  }

  /**
   * Returns the classes that should be applied to the form, individual classes are applied based on whether the form is
   * nested or top level
   * @param pEvalNode The form node
   * @return The classes that should be applied to the form
   */
  private List<String> getFormClasses(EvaluatedNode pEvalNode) {
    List<String> lClasses = getFormPartClasses(pEvalNode, FormPart.FORM);

    // Get nested or top level form attributes
    FormPart lFormPart = pEvalNode.isNested() ? FormPart.NESTED_FORM : FormPart.TOP_LEVEL_FORM;
    lClasses.addAll(getFormPartClasses(pEvalNode, lFormPart));

    return lClasses;
  }

  /**
   * Returns the styles that should be applied to the form, individual styles are applied based on whether the form is
   * nested or top level
   * @param pEvalNode The form node
   * @return The styles that should be applied to the form
   */
  private List<String> getFormStyles(EvaluatedNode pEvalNode) {
    List<String> lStyles = getFormPartStyles(pEvalNode, FormPart.FORM);

    // Get nested or top level form attributes
    FormPart lFormPart = pEvalNode.isNested() ? FormPart.NESTED_FORM : FormPart.TOP_LEVEL_FORM;
    lStyles.addAll(getFormPartStyles(pEvalNode, lFormPart));

    return lStyles;
  }

  /**
   * Returns the classes that should be applied when resolving the column item styles
   * @param pColumnItem The column item
   * @return The classes that should be applied when resolving the column item styles
   */
  private List<String> getColumnItemCellClasses(LayoutWidgetItemColumn pColumnItem) {
    List<String> lClasses = new LinkedList<>();

    if (pColumnItem.isPrompt()) {
      lClasses.addAll(getFormPartClasses(pColumnItem.getItemNode(), FormPart.PROMPT_CELL));
      lClasses.add(FORM_PROMPT_CELL_CLASS);
    }
    else if (!pColumnItem.isFiller()) {
      lClasses.addAll(getFormPartClasses(pColumnItem.getItemNode(), FormPart.FIELD_CELL));
      lClasses.add(FORM_CELL_CLASS);

      // A class may be applied to the cell based on the widget type of the nested content
      WidgetBuilderType lCellContentWidgetType = pColumnItem.getItemNode().getWidgetBuilderType();
      Optional.ofNullable(FORM_CELL_NESTED_CONTENT_CLASSES.get(lCellContentWidgetType)).ifPresent(lClasses::add);
    }

    return lClasses;
  }

  /**
   * Returns the styles that should be applied when resolving the column item styles
   * @param pColumnItem The column item
   * @return The styles that should be applied when resolving the column item styles
   */
  private List<String> getColumnItemCellStyles(LayoutWidgetItemColumn pColumnItem) {
    List<String> lStyles = new LinkedList<>();

    if (pColumnItem.isPrompt()) {
      lStyles.addAll(getFormPartStyles(pColumnItem.getItemNode(), FormPart.PROMPT_CELL));
    }
    else if (!pColumnItem.isFiller()) {
      lStyles.addAll(getFormPartStyles(pColumnItem.getItemNode(), FormPart.FIELD_CELL));
    }

    return lStyles;
  }

  /**
   * Returns a list of classes that should be applied when resolving styles for the form part. The classes are taken
   * from the node attributes that correspond to classes for the form part. For example, if the form part is the prompt,
   * the node attributes that can specify prompt classes will be returned.
   * @param pEvalNode The form node
   * @param pFormPart The part of the form set-out that the classes are being applied to
   * @return A list of classes that should be applied when resolving styles for the form part
   */
  private List<String> getFormPartClasses(EvaluatedNode pEvalNode, FormPart pFormPart) {
    return getFormPartAttributeValues(pEvalNode, pFormPart, mFormNodeClassAttributes);
  }

  /**
   * Returns a list of styles that should be applied when resolving styles for the form part. The styles are taken
   * from the node attributes that correspond to styles for the form part. For example, if the form part is the prompt,
   * the node attributes that can specify prompt styles will be returned.
   * @param pEvalNode The form node
   * @param pFormPart The part of the form set-out that the classes are being applied to
   * @return A list of styles that should be applied when resolving styles for the form part
   */
  private List<String> getFormPartStyles(EvaluatedNode pEvalNode, FormPart pFormPart) {
    return getFormPartAttributeValues(pEvalNode, pFormPart, mFormNodeStyleAttributes);
  }

  /**
   * Returns a list of resolved string attributes based on the node attributes specified for the form part
   * @param pEvalNode The form node
   * @param pFormPart The part of the form set-out the node attributes are being resolved for
   * @param pFormPartAttributes A map of form parts to the node attributes that should be resolved for that part
   * @return A list of resolved string attributes based on the node attributes specified for the form part
   */
  private List<String> getFormPartAttributeValues(EvaluatedNode pEvalNode, FormPart pFormPart, Multimap<FormPart, NodeAttribute> pFormPartAttributes) {
    List<String> lResolvedAttributes = new LinkedList<>();

    pFormPartAttributes.get(pFormPart)
                       .stream()
                       .map(pEvalNode::getStringAttribute)
                       .filter(Objects::nonNull)
                       .forEach(lResolvedAttributes::add);

    return lResolvedAttributes;
  }
}
