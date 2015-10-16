package net.foxopen.fox.module.serialiser.pdf.css;

import com.google.common.base.Joiner;
import com.itextpdf.text.Rectangle;
import com.itextpdf.tool.xml.Tag;
import com.itextpdf.tool.xml.css.CSS;
import com.itextpdf.tool.xml.css.CssFiles;
import com.itextpdf.tool.xml.css.StyleAttrCSSResolver;
import com.itextpdf.tool.xml.html.HTML;
import net.foxopen.fox.XFUtil;
import net.foxopen.fox.module.serialiser.pdf.css.propertyresolvers.BackgroundColorPropertyResolver;
import net.foxopen.fox.module.serialiser.pdf.css.propertyresolvers.BorderStylePropertyResolver;
import net.foxopen.fox.module.serialiser.pdf.css.propertyresolvers.ColorPropertyResolver;
import net.foxopen.fox.module.serialiser.pdf.css.propertyresolvers.FloatPropertyResolver;
import net.foxopen.fox.module.serialiser.pdf.css.propertyresolvers.FontSizePropertyResolver;
import net.foxopen.fox.module.serialiser.pdf.css.propertyresolvers.FontStylePropertyResolver;
import net.foxopen.fox.module.serialiser.pdf.css.propertyresolvers.FontWeightPropertyResolver;
import net.foxopen.fox.module.serialiser.pdf.css.propertyresolvers.MarginBottomPropertyResolver;
import net.foxopen.fox.module.serialiser.pdf.css.propertyresolvers.MarginTopPropertyResolver;
import net.foxopen.fox.module.serialiser.pdf.css.propertyresolvers.PaddingPropertyResolver;
import net.foxopen.fox.module.serialiser.pdf.css.propertyresolvers.PageBreakInsidePropertyResolver;
import net.foxopen.fox.module.serialiser.pdf.css.propertyresolvers.PropertyResolver;
import net.foxopen.fox.module.serialiser.pdf.css.propertyresolvers.TextAlignPropertyResolver;
import net.foxopen.fox.module.serialiser.pdf.css.propertyresolvers.WidthPropertyResolver;
import net.foxopen.fox.module.serialiser.pdf.elementattributes.CellAttributes;
import net.foxopen.fox.module.serialiser.pdf.elementattributes.ElementAttributes;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Resolves CSS property styles to element attributes
 */
public class CSSResolver {
  /**
   * The class that is applied during CSS resolving of every element if the PDF is generating in debug mode
   */
  public static final String DEBUG_CLASS = "debug";
  /**
   * The property resolver for font size, which is applied first before any other other property resolvers as other
   * resolvers may define units in terms of the font size (e.g. ems)
   */
  private static final PropertyResolver FONT_SIZE_PROPERTY_RESOLVER = new FontSizePropertyResolver();
  /**
   * A map of CSS property names to the property resolvers which modify the corresponding element attributes
   */
  private static final Map<String, PropertyResolver> CSS_PROPERTY_RESOLVERS = new HashMap<>();
  static {
    // Font resolvers
    CSS_PROPERTY_RESOLVERS.put(CSS.Property.FONT_WEIGHT, new FontWeightPropertyResolver());
    CSS_PROPERTY_RESOLVERS.put(CSS.Property.FONT_STYLE,  new FontStylePropertyResolver());
    CSS_PROPERTY_RESOLVERS.put(CSS.Property.COLOR, new ColorPropertyResolver());

    // Paragraph resolvers
    CSS_PROPERTY_RESOLVERS.put(CSS.Property.TEXT_ALIGN, new TextAlignPropertyResolver());
    CSS_PROPERTY_RESOLVERS.put(CSS.Property.MARGIN_TOP, new MarginTopPropertyResolver());
    CSS_PROPERTY_RESOLVERS.put(CSS.Property.MARGIN_BOTTOM, new MarginBottomPropertyResolver());

    // Cell resolvers
    CSS_PROPERTY_RESOLVERS.put(CSS.Property.PADDING_TOP, new PaddingPropertyResolver(CellAttributes::setPaddingTop));
    CSS_PROPERTY_RESOLVERS.put(CSS.Property.PADDING_RIGHT, new PaddingPropertyResolver(CellAttributes::setPaddingRight));
    CSS_PROPERTY_RESOLVERS.put(CSS.Property.PADDING_BOTTOM, new PaddingPropertyResolver(CellAttributes::setPaddingBottom));
    CSS_PROPERTY_RESOLVERS.put(CSS.Property.PADDING_LEFT, new PaddingPropertyResolver(CellAttributes::setPaddingLeft));
    CSS_PROPERTY_RESOLVERS.put(CSS.Property.BORDER_TOP_STYLE, new BorderStylePropertyResolver(Rectangle.TOP));
    CSS_PROPERTY_RESOLVERS.put(CSS.Property.BORDER_RIGHT_STYLE, new BorderStylePropertyResolver(Rectangle.RIGHT));
    CSS_PROPERTY_RESOLVERS.put(CSS.Property.BORDER_BOTTOM_STYLE, new BorderStylePropertyResolver(Rectangle.BOTTOM));
    CSS_PROPERTY_RESOLVERS.put(CSS.Property.BORDER_LEFT_STYLE, new BorderStylePropertyResolver(Rectangle.LEFT));
    CSS_PROPERTY_RESOLVERS.put(CSS.Property.BACKGROUND_COLOR, new BackgroundColorPropertyResolver());

    // Table resolvers
    CSS_PROPERTY_RESOLVERS.put(CSS.Property.WIDTH, new WidthPropertyResolver());
    CSS_PROPERTY_RESOLVERS.put(CSS.Property.FLOAT, new FloatPropertyResolver());
    CSS_PROPERTY_RESOLVERS.put("page-break-inside", new PageBreakInsidePropertyResolver());
  }

  private final StyleAttrCSSResolver mCssPropertyResolver;
  private final boolean mIsDebug;

  /**
   * Creates a CSS resolver to modify element attributes based on the corresponding CSS styles
   * @param pCSSFiles The css files that should be used when resolving styles
   * @param pIsDebug If true, the debug class is applied when resolving styles
   */
  public CSSResolver(CssFiles pCSSFiles, boolean pIsDebug) {
    mCssPropertyResolver = new StyleAttrCSSResolver(pCSSFiles);
    mIsDebug = pIsDebug;
  }

  /**
   * Modifies the element attributes based on the CSS properties of the HTML tag attributes (style, class) and tag name
   * @param pElementAttributes The element attributes to be modified
   * @param pTagName The name of the HTML tag, used for CSS selectors that target via tag name
   * @param pTagAttributes The attributes of the tag, CSS properties are extracted from the class and style attributes
   */
  public void resolveStyles(ElementAttributes pElementAttributes, String pTagName, Map<String, String> pTagAttributes) {
    // When debug the tag attributes may be augmented so the debug class is included when resolving styles
    Map<String, String> lTagAttributes = mIsDebug ? addDebugClass(pTagAttributes) : pTagAttributes;

    // Resolve the tag style properties (i.e. parse the style and class attributes)
    Tag lTag = new Tag(pTagName, lTagAttributes);
    mCssPropertyResolver.resolveStyles(lTag);

    // Apply the applicable property styles to the element attributes
    applyPropertyStyles(pElementAttributes, lTag.getCSS());
  }

  /**
   * Modifies the element attributes based on the specified CSS classes, styles and tag name
   * @param pElementAttributes The element attributes to be modified
   * @param pTagName The name of the HTML tag, used for CSS selectors that target via tag name
   * @param pClasses The list of classes to be applied to the tag when resolving CSS styles
   * @param pStyles The list of styles to be applied to the tag when resolving CSS styles
   */
  public void resolveStyles(ElementAttributes pElementAttributes, String pTagName, List<String> pClasses, List<String> pStyles) {
    // Create tag attributes of class and style, and resolve the tag
    Map<String, String> lTagAttributes = new HashMap<>();
    lTagAttributes.put(HTML.Attribute.CLASS, Joiner.on(" ").skipNulls().join(pClasses));
    lTagAttributes.put(HTML.Attribute.STYLE, Joiner.on(" ").skipNulls().join(pStyles));
    resolveStyles(pElementAttributes, pTagName, lTagAttributes);
  }

  /**
   * Returns a map of tag attributes with the debug class added. The class attribute is added to the tag attributes if
   * it does not already exist, and the debug class name is augmented. As a new copy of the tag attributes is created,
   * the tag attributes may be immutable.
   * @param pTagAttributes The current tag attributes, may be immutable
   * @return A map of tag attributes with the debug class added
   */
  private Map<String, String> addDebugClass(Map<String, String> pTagAttributes) {
    Map<String, String> lTagAttributes = new HashMap<>(pTagAttributes);
    lTagAttributes.compute("class", (pKey, pValue) -> DEBUG_CLASS + " " + XFUtil.nvl(pValue));
    return lTagAttributes;
  }

  /**
   * Applies the CSS properties by modifying the corresponding element attributes
   * @param pElementAttributes The element attributes to be modified
   * @param pProperties The CSS properties and values
   */
  private void applyPropertyStyles(ElementAttributes pElementAttributes, Map<String, String> pProperties) {
    // If font size is specified, it must be applied first as other properties that specify dimensions in terms of the
    // current font size (e.g. ems) should use this new font size
    Optional.ofNullable(pProperties.get(CSS.Property.FONT_SIZE))
            .ifPresent(pValue -> FONT_SIZE_PROPERTY_RESOLVER.apply(pElementAttributes, pValue));

    // Apply the remaining properties
    pProperties.forEach((pProperty, pValue) -> {
      Optional.ofNullable(CSS_PROPERTY_RESOLVERS.get(pProperty))
              .ifPresent(pResolver -> pResolver.apply(pElementAttributes, pValue));
    });
  }
}
