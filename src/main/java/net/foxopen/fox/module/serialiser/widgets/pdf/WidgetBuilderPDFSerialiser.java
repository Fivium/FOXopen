package net.foxopen.fox.module.serialiser.widgets.pdf;

import com.itextpdf.text.Paragraph;
import com.itextpdf.tool.xml.html.HTML;
import net.foxopen.fox.StringUtil;
import net.foxopen.fox.dom.DOM;
import net.foxopen.fox.module.LayoutDirection;
import net.foxopen.fox.module.RenderTypeOption;
import net.foxopen.fox.module.datanode.EvaluatedNode;
import net.foxopen.fox.module.datanode.NodeAttribute;
import net.foxopen.fox.module.datanode.NodeVisibility;
import net.foxopen.fox.module.parsetree.ParseTree;
import net.foxopen.fox.module.parsetree.evaluatedpresentationnode.EvaluatedPresentationNode;
import net.foxopen.fox.module.parsetree.presentationnode.PresentationNode;
import net.foxopen.fox.module.serialiser.OutputSerialiser;
import net.foxopen.fox.module.serialiser.SerialisationContext;
import net.foxopen.fox.module.serialiser.pdf.PDFSerialiser;
import net.foxopen.fox.module.serialiser.pdf.elementattributes.ElementAttributes;
import net.foxopen.fox.module.serialiser.pdf.elementcontainers.ElementContainerFactory;
import net.foxopen.fox.module.serialiser.widgets.WidgetBuilder;
import net.foxopen.fox.module.serialiser.widgets.WidgetBuilderType;

import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;


public abstract class WidgetBuilderPDFSerialiser<EN extends EvaluatedNode> extends WidgetBuilder<PDFSerialiser, EN>  {
  private static final String PROMPT_TAG = HTML.Tag.SPAN;
  private static final String PROMPT_CLASS = "prompt";
  private static final LayoutDirection DEFAULT_PROMPT_LAYOUT_DIRECTION = LayoutDirection.WEST;
  private static final Map<LayoutDirection, String> PROMPT_LAYOUT_CLASSES = new EnumMap<>(LayoutDirection.class);
  private static final RenderTypeOption DEFAULT_RENDER_TYPE = RenderTypeOption.FORM;

  static {
    PROMPT_LAYOUT_CLASSES.put(LayoutDirection.WEST, "promptWest");
    PROMPT_LAYOUT_CLASSES.put(LayoutDirection.NORTH, "promptNorth");
  }

  protected WidgetBuilderPDFSerialiser() {
  }

  @Override
  public void buildPrompt(SerialisationContext pSerialisationContext, PDFSerialiser pSerialiser, EN pEvalNode) {
    if (hasPrompt(pEvalNode)) {
      ElementAttributes lElementAttributes = pSerialiser.getInheritedElementAttributes();
      pSerialiser.getCSSResolver().resolveStyles(lElementAttributes, PROMPT_TAG, getPromptClasses(pEvalNode), Collections.emptyList());
      pSerialiser.pushElementAttributes(lElementAttributes);

      Paragraph lParagraph = pSerialiser.getElementFactory().getParagraph();
      pSerialiser.startContainer(ElementContainerFactory.getContainer(lParagraph));
      renderXMLStringContent(pSerialisationContext, pSerialiser, pEvalNode.getPrompt().getString());
      pSerialiser.endContainer();
      pSerialiser.add(lParagraph);

      pSerialiser.popElementAttributes();
    }
  }

  protected static boolean isVisible(EvaluatedNode pEvalNode) {
    return Arrays.asList(NodeVisibility.VIEW, NodeVisibility.EDIT).contains(pEvalNode.getFieldMgr().getVisibility());
  }

  /**
   * Returns the layout direction of the node prompt, either specified via the prompt layout attribute or as the default
   * prompt layout direction for the PDF serialiser
   * @param pEvalNode The node with a prompt
   * @return The layout direction of the node prompt
   */
  protected static LayoutDirection getPromptLayoutDirection(EvaluatedNode pEvalNode) {
    return Optional.ofNullable(pEvalNode.getStringAttribute(NodeAttribute.PROMPT_LAYOUT))
                   .map(LayoutDirection::fromString)
                   .flatMap(Optional::ofNullable)
                   .orElse(DEFAULT_PROMPT_LAYOUT_DIRECTION);
  }

  /**
   * Returns whether the widget node is a tight field, specified either via the tight field attribute or because the
   * node widget type is one of the types specified in the tight widgets CSV attribute
   * @param pEvalNode The widget node
   * @return True if the widget node is a tight field, false otherwise
   */
  protected static boolean isTightField(EvaluatedNode pEvalNode) {
    return pEvalNode.getBooleanAttribute(NodeAttribute.TIGHT_FIELD, false) || isTightWidgetType(pEvalNode);
  }

  /**
   * Returns the render type specified on the node, or the default if not specified or the node attribute was invalid
   * @param pEvalNode The node to get the widget render type attribute from
   * @return The render type specified on the node, or the default if not specified or the node attribute was invalid
   */
  protected static RenderTypeOption getRenderType(EvaluatedNode pEvalNode) {
    return RenderTypeOption.fromString(pEvalNode.getStringAttribute(NodeAttribute.RENDER_TYPE))
                           .orElse(DEFAULT_RENDER_TYPE);
  }

  /**
   * Returns the class name of the prompt layout on the node, to be applied to style the prompt. The prompt layout is
   * specified via the prompt layout attribute or as the default layout for the PDF serialiser.
   * @param pEvalNode The node with a prompt
   * @return The class name of the prompt layout specified on the node
   */
  private static String getPromptLayoutClass(EvaluatedNode pEvalNode) {
    return PROMPT_LAYOUT_CLASSES.get(getPromptLayoutDirection(pEvalNode));
  }

  /**
   * Returns the classes to be applied to the prompt of the node
   * @param pEvalNode The node with a prompt
   * @return The classes to be applied to the prompt of the node
   */
  private static List<String> getPromptClasses(EvaluatedNode pEvalNode) {
    return Arrays.asList(PROMPT_CLASS, getPromptLayoutClass(pEvalNode));
  }

  /**
   * Returns whether the given node is of a tight widget type based on the tight widgets CSV attribute
   * @param pEvalNode The widget node
   * @return True if the given node is of a tight widget type, false otherwise
   */
  private static boolean isTightWidgetType(EvaluatedNode pEvalNode) {
    return Optional.ofNullable(pEvalNode.getStringAttribute(NodeAttribute.TIGHT_WIDGETS))
                   .map(StringUtil::commaDelimitedListToSet)
                   .map(pTightWidgets -> pTightWidgets.stream().anyMatch(pTightWidgetName -> isWidgetType(pEvalNode, pTightWidgetName)))
                   .orElse(false);
  }

  /**
   * Returns whether the given node widget builder type is equal to the specified widget builder type name
   * @param pEvalNode The widget node
   * @param pWidgetTypeName The name of the widget builder type to test
   * @return True if the given node widget builder type is equal to the specified widget builder type name, false otherwise
   */
  private static boolean isWidgetType(EvaluatedNode pEvalNode, String pWidgetTypeName) {
    return pEvalNode.getWidgetBuilderType() == WidgetBuilderType.fromString(pWidgetTypeName, pEvalNode, false);
  }

  /**
   * Renders XML string content, that may include text nodes, comments, html nodes etc.
   * @param pSerialisationContext The serialisation context to use during rendering
   * @param pSerialiser The serialiser to use to render
   * @param pXMLStringContent The string of XML to be rendered, does not have to have a parent node
   */
  private static void renderXMLStringContent(SerialisationContext pSerialisationContext, OutputSerialiser pSerialiser, String pXMLStringContent) {
    DOM lWrapperDOM = DOM.createDocumentFromXMLString("<span>" + pXMLStringContent + "</span>");
    PresentationNode lWrapperNode = ParseTree.parseDOMNode(lWrapperDOM);

    EvaluatedPresentationNode lEvaluatedWrapperNode = pSerialisationContext.evaluateNode(null, lWrapperNode, lWrapperDOM);
    lEvaluatedWrapperNode.render(pSerialisationContext, pSerialiser);
  }
}
