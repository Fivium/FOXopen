package net.foxopen.fox.module.serialiser.components.pdf.htmltags;

import com.google.common.io.ByteStreams;
import com.itextpdf.text.BadElementException;
import com.itextpdf.text.Chunk;
import com.itextpdf.text.Image;
import net.foxopen.fox.FoxComponent;
import net.foxopen.fox.ex.ExInternal;
import net.foxopen.fox.module.evaluatedattributeresult.StringAttributeResult;
import net.foxopen.fox.module.parsetree.evaluatedpresentationnode.EvaluatedHtmlPresentationNode;
import net.foxopen.fox.module.serialiser.FoxComponentUtils;
import net.foxopen.fox.module.serialiser.SerialisationContext;
import net.foxopen.fox.module.serialiser.components.ComponentBuilder;
import net.foxopen.fox.module.serialiser.pdf.PDFSerialiser;
import net.foxopen.fox.module.serialiser.pdf.css.CSSResolverUtils;
import net.foxopen.fox.module.serialiser.pdf.elementattributes.ElementAttributes;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;

/**
 * Serialises an img tag
 */
public class ImageComponentBuilder extends ComponentBuilder<PDFSerialiser, EvaluatedHtmlPresentationNode> {
  private static final String IMAGE_SOURCE_ATTRIBUTE = "src";
  private static final String IMAGE_WIDTH_ATTRIBUTE = "width";
  private static final String IMAGE_HEIGHT_ATTRIBUTE = "height";
  public static final float IMAGE_X_OFFSET = 0f;
  public static final float IMAGE_Y_OFFSET = 0f;
  private static final ComponentBuilder<PDFSerialiser, EvaluatedHtmlPresentationNode> INSTANCE = new ImageComponentBuilder();

  public static final ComponentBuilder<PDFSerialiser, EvaluatedHtmlPresentationNode> getInstance() {
    return INSTANCE;
  }

  private ImageComponentBuilder() {
  }

  @Override
  public void buildComponent(SerialisationContext pSerialisationContext, PDFSerialiser pSerialiser, EvaluatedHtmlPresentationNode pEvalNode) {
    Map<String, StringAttributeResult> lNodeAttributes = pEvalNode.getAttributeMap(false);
    String lSourceURI = Optional.ofNullable(lNodeAttributes.get(IMAGE_SOURCE_ATTRIBUTE))
                                .map(StringAttributeResult::getString)
                                .flatMap(Optional::ofNullable)
                                .orElseThrow(() -> new ExInternal("Could not find '" + IMAGE_SOURCE_ATTRIBUTE + "' attribute on HTML image"));
    Image lImage = getImage(pSerialisationContext, lSourceURI);

    // Set the image dimensions if specified in the node attributes, using the current element attributes if dimensions
    // are specified in relative units e.g. ems
    setImageDimensions(lImage, lNodeAttributes, pSerialiser.getElementAttributes());

    // Add the image within a chunk so it appears inline and changes the line leading to fit the image
    pSerialiser.add(new Chunk(lImage, IMAGE_X_OFFSET, IMAGE_Y_OFFSET, true));
  }

  /**
   * Returns an image resolved from the URI
   * @param pSerialisationContext The serialisation context, used to get the image from the application components table
   * @param pSourceURI The URI of the the image component
   * @return The resolved image
   * @throws ExInternal If a fixed URI was specified in the module css item, see
   *         {@link net.foxopen.fox.entrypoint.uri.RequestURIBuilder#isFixedURI}
   */
  private Image getImage(SerialisationContext pSerialisationContext, String pSourceURI) throws ExInternal {
    Image lImage;

    if (!pSerialisationContext.createURIBuilder().isFixedURI(pSourceURI)) {
      lImage = getImageFromComponent(pSerialisationContext, pSourceURI);
    }
    else {
      throw new ExInternal("Fixed image source URIs cannot be used during PDF serialisation (source: '" + pSourceURI + "')",
                           new UnsupportedOperationException());
    }

    return lImage;
  }

  /**
   * Returns an image from the given component path
   * @param pSerialisationContext The serialisation context, used to get the image from the application components table
   * @param pComponentPath The path to the component
   * @return The resolved image
   * @throws ExInternal If there was an error creating an image from the component file
   */
  private Image getImageFromComponent(SerialisationContext pSerialisationContext, String pComponentPath) throws ExInternal {
    FoxComponent lImageComponent = FoxComponentUtils.getComponent(pSerialisationContext, pComponentPath);

    try {
      return Image.getInstance(ByteStreams.toByteArray(lImageComponent.getInputStream()));
    }
    catch (IOException | BadElementException e) {
      throw new ExInternal("Failed to create image from component '" + pComponentPath + "'", e);
    }
  }

  /**
   * Sets the image dimensions if they are specified in the node attributes
   * @param pImage The image to be modified
   * @param pNodeAttributes The attributes of the image node
   * @param pElementAttributes The element attributes that should be used when a relative dimension unit is specified on
   *                           the attribute, e.g. if the unit is ems.
   */
  private void setImageDimensions(Image pImage, Map<String, StringAttributeResult> pNodeAttributes, ElementAttributes pElementAttributes) {
    getDimension(pNodeAttributes, pElementAttributes, IMAGE_WIDTH_ATTRIBUTE).ifPresent(pImage::scaleAbsoluteWidth);
    getDimension(pNodeAttributes, pElementAttributes, IMAGE_HEIGHT_ATTRIBUTE).ifPresent(pImage::scaleAbsoluteHeight);
  }

  /**
   * Returns the resolved attribute value in points if it has been specified
   * @param pNodeAttributes The image node attributes
   * @param pElementAttributes The element attributes that should be used when a relative dimension unit is specified on
   *                           the attribute, e.g. if the unit is ems.
   * @param pDimensionAttributeName The attribute name of the dimension
   * @return The resolved attribute value in points, or empty if it isn't specified in the node attributes
   */
  private Optional<Float> getDimension(Map<String, StringAttributeResult> pNodeAttributes, ElementAttributes pElementAttributes, String pDimensionAttributeName) {
    return Optional.ofNullable(pNodeAttributes.get(pDimensionAttributeName))
                   .map(StringAttributeResult::getString)
                   .flatMap(Optional::ofNullable)
                   .map(pDimension -> CSSResolverUtils.parseValueToPt(pDimension, pElementAttributes));
  }
}
