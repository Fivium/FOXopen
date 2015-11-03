package net.foxopen.fox.module.serialiser.components.pdf;


import com.itextpdf.tool.xml.html.HTML;
import net.foxopen.fox.ex.ExInternal;
import net.foxopen.fox.module.parsetree.evaluatedpresentationnode.EvaluatedHtmlPresentationNode;
import net.foxopen.fox.module.serialiser.SerialisationContext;
import net.foxopen.fox.module.serialiser.components.ComponentBuilder;
import net.foxopen.fox.module.serialiser.components.HTMLComponentUtils;
import net.foxopen.fox.module.serialiser.components.pdf.htmltags.BreakComponentBuilder;
import net.foxopen.fox.module.serialiser.components.pdf.htmltags.CellComponentBuilder;
import net.foxopen.fox.module.serialiser.components.pdf.htmltags.DivComponentBuilder;
import net.foxopen.fox.module.serialiser.components.pdf.htmltags.ForwarderComponentBuilder;
import net.foxopen.fox.module.serialiser.components.pdf.htmltags.HorizontalRuleComponentBuilder;
import net.foxopen.fox.module.serialiser.components.pdf.htmltags.ImageComponentBuilder;
import net.foxopen.fox.module.serialiser.components.pdf.htmltags.ListItemComponentBuilder;
import net.foxopen.fox.module.serialiser.components.pdf.htmltags.OrderedListComponentBuilder;
import net.foxopen.fox.module.serialiser.components.pdf.htmltags.SpanComponentBuilder;
import net.foxopen.fox.module.serialiser.components.pdf.htmltags.SubComponentBuilder;
import net.foxopen.fox.module.serialiser.components.pdf.htmltags.SupComponentBuilder;
import net.foxopen.fox.module.serialiser.components.pdf.htmltags.TableComponentBuilder;
import net.foxopen.fox.module.serialiser.components.pdf.htmltags.UnorderedListComponentBuilder;
import net.foxopen.fox.module.serialiser.pdf.PDFSerialiser;
import net.foxopen.fox.module.serialiser.pdf.elementattributes.ElementAttributes;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Serialises a HTML tag. The following HTML tags are supported:
 *
 * <ul>
 *   <li>div</li>
 *   <li>p</li>
 *   <li>h1</li>
 *   <li>h2</li>
 *   <li>h3</li>
 *   <li>h4</li>
 *   <li>h5</li>
 *   <li>h6</li>
 *   <li>span</li>
 *   <li>strong</li>
 *   <li>b</li>
 *   <li>em</li>
 *   <li>i</li>
 *   <li>sup</li>
 *   <li>sub</li>
 *   <li>table</li>
 *   <li>thead</li>
 *   <li>tbody</li>
 *   <li>tr</li>
 *   <li>th</li>
 *   <li>td</li>
 *   <li>ol</li>
 *   <li>ul</li>
 *   <li>li</li>
 *   <li>img</li>
 *   <li>hr</li>
 *   <li>br</li>
 * </ul>
 *
 * Unsupported tags will throw an exception.
 */
public class HTMLComponentBuilder extends ComponentBuilder<PDFSerialiser, EvaluatedHtmlPresentationNode> {
  /**
   * A map of HTML tags to their component builders
   */
  private static final Map<String,  ComponentBuilder<PDFSerialiser, EvaluatedHtmlPresentationNode>> TAG_COMPONENT_BUILDERS = new HashMap<>();
  private static final ComponentBuilder<PDFSerialiser, EvaluatedHtmlPresentationNode> INSTANCE = new HTMLComponentBuilder();

  static {
    TAG_COMPONENT_BUILDERS.put(HTML.Tag.DIV, DivComponentBuilder.getInstance());
    TAG_COMPONENT_BUILDERS.put(HTML.Tag.P, DivComponentBuilder.getInstance());
    TAG_COMPONENT_BUILDERS.put(HTML.Tag.H1, DivComponentBuilder.getInstance());
    TAG_COMPONENT_BUILDERS.put(HTML.Tag.H2, DivComponentBuilder.getInstance());
    TAG_COMPONENT_BUILDERS.put(HTML.Tag.H3, DivComponentBuilder.getInstance());
    TAG_COMPONENT_BUILDERS.put(HTML.Tag.H4, DivComponentBuilder.getInstance());
    TAG_COMPONENT_BUILDERS.put(HTML.Tag.H5, DivComponentBuilder.getInstance());
    TAG_COMPONENT_BUILDERS.put(HTML.Tag.H6, DivComponentBuilder.getInstance());

    TAG_COMPONENT_BUILDERS.put(HTML.Tag.SPAN, SpanComponentBuilder.getInstance());
    TAG_COMPONENT_BUILDERS.put(HTML.Tag.STRONG, SpanComponentBuilder.getInstance());
    TAG_COMPONENT_BUILDERS.put(HTML.Tag.B, SpanComponentBuilder.getInstance());
    TAG_COMPONENT_BUILDERS.put(HTML.Tag.EM, SpanComponentBuilder.getInstance());
    TAG_COMPONENT_BUILDERS.put(HTML.Tag.I, SpanComponentBuilder.getInstance());
    TAG_COMPONENT_BUILDERS.put(HTML.Tag.SUP, SupComponentBuilder.getInstance());
    TAG_COMPONENT_BUILDERS.put(HTML.Tag.SUB, SubComponentBuilder.getInstance());

    TAG_COMPONENT_BUILDERS.put(HTML.Tag.TABLE, TableComponentBuilder.getInstance());
    TAG_COMPONENT_BUILDERS.put(HTML.Tag.THEAD, ForwarderComponentBuilder.getInstance());
    TAG_COMPONENT_BUILDERS.put(HTML.Tag.TBODY, ForwarderComponentBuilder.getInstance());
    TAG_COMPONENT_BUILDERS.put(HTML.Tag.TR, ForwarderComponentBuilder.getInstance());
    TAG_COMPONENT_BUILDERS.put(HTML.Tag.TH, CellComponentBuilder.getInstance());
    TAG_COMPONENT_BUILDERS.put(HTML.Tag.TD, CellComponentBuilder.getInstance());

    TAG_COMPONENT_BUILDERS.put(HTML.Tag.OL, OrderedListComponentBuilder.getInstance());
    TAG_COMPONENT_BUILDERS.put(HTML.Tag.UL, UnorderedListComponentBuilder.getInstance());
    TAG_COMPONENT_BUILDERS.put(HTML.Tag.LI, ListItemComponentBuilder.getInstance());

    TAG_COMPONENT_BUILDERS.put(HTML.Tag.IMG, ImageComponentBuilder.getInstance());
    TAG_COMPONENT_BUILDERS.put(HTML.Tag.HR, HorizontalRuleComponentBuilder.getInstance());
    TAG_COMPONENT_BUILDERS.put(HTML.Tag.BR, BreakComponentBuilder.getInstance());
  }

  public static final ComponentBuilder<PDFSerialiser, EvaluatedHtmlPresentationNode> getInstance() {
    return INSTANCE;
  }

  private HTMLComponentBuilder() {
  }

  @Override
  public void buildComponent(SerialisationContext pSerialisationContext, PDFSerialiser pSerialiser, EvaluatedHtmlPresentationNode pEvalNode) {
    String lTagName = pEvalNode.getTagName();

    // A void html element must not have content, throw an exception if it does
    if (HTMLComponentUtils.isVoidElement(lTagName) && pEvalNode.getChildren().size() > 0) {
      throw new ExInternal("Found a void element with content: '" + lTagName + "'");
    }

    Optional<ComponentBuilder<PDFSerialiser, EvaluatedHtmlPresentationNode>> lTagComponentBuilder = Optional.ofNullable(TAG_COMPONENT_BUILDERS.get(lTagName));

    if (lTagComponentBuilder.isPresent()) {
      // Resolve any styles for the tag name and attributes defined on the tag (i.e. style and/or class) and build the
      // component
      ElementAttributes lElementAttributes = pSerialiser.getInheritedElementAttributes();
      // Resolve tag attributes from the string attribute result to an actual string, adding them only if the resolved
      // value is not null
      Map<String, String> lTagAttributes = new HashMap<>();
      pEvalNode.getAttributeMap(false).forEach((pAttribute, pValue) -> {
        Optional.ofNullable(pValue.getString())
                .ifPresent(pStringValue -> lTagAttributes.put(pAttribute, pStringValue));
      });
      pSerialiser.getCSSResolver().resolveStyles(lElementAttributes, lTagName, lTagAttributes);

      pSerialiser.pushElementAttributes(lElementAttributes);
      lTagComponentBuilder.get().buildComponent(pSerialisationContext, pSerialiser, pEvalNode);
      pSerialiser.popElementAttributes();
    }
    else {
      throw new ExInternal("Could not find a HTML tag component builder for tag '" + lTagName + "'");
    }
  }
}
