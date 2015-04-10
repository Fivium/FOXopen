package net.foxopen.fox.module.serialiser.widgets.html;

import net.foxopen.fox.XFUtil;
import net.foxopen.fox.ex.ExInternal;
import net.foxopen.fox.image.ImageServlet;
import net.foxopen.fox.module.UploadedFileInfo;
import net.foxopen.fox.module.datanode.EvaluatedNodeInfoFileItem;
import net.foxopen.fox.module.datanode.NodeAttribute;
import net.foxopen.fox.module.serialiser.SerialisationContext;
import net.foxopen.fox.module.serialiser.fragmentbuilder.MustacheFragmentBuilder;
import net.foxopen.fox.module.serialiser.html.HTMLSerialiser;

import java.util.List;
import java.util.Map;

public class ImageWidgetBuilder
extends WidgetBuilderHTMLSerialiser<EvaluatedNodeInfoFileItem> {

  private static final ImageWidgetBuilder INSTANCE = new ImageWidgetBuilder();
  private static final String MUSTACHE_TEMPLATE = "html/ImageWidget.mustache";

  public static ImageWidgetBuilder getInstance() {
    return INSTANCE;
  }

  @Override
  protected void buildWidgetInternal(SerialisationContext pSerialisationContext, HTMLSerialiser pSerialiser, EvaluatedNodeInfoFileItem pEvalNode) {

    //Assert we have exactly one image - don't set out anything if not
    List<UploadedFileInfo> lFileInfoList = pEvalNode.getUploadedFileInfoList();
    if(lFileInfoList.size() == 0) {
      return;
    }
    else if(lFileInfoList.size() > 1) {
      throw new ExInternal("Image widget only supports displaying single uploads at this time (widget " + pEvalNode.getIdentityInformation() + ")");
    }

    //Work out target width/height, either from a named "image series" or an explicit width/height value defined on the node info
    ImageDimensions lImageDimensions = getDisplayDimensions(pSerialisationContext, pEvalNode);

    Map<String, Object> lTemplateVars = getGenericTemplateVars(pSerialiser, pEvalNode);

    UploadedFileInfo lFileInfo = lFileInfoList.get(0);
    String lImgSrc = ImageServlet.generateImageDisplayURI(pSerialisationContext.createURIBuilder(), lFileInfo.getFileId(), lImageDimensions.mWidth, lImageDimensions.mHeight, 0);

    //Use field prompt as alt text
    String lImgAlt = pEvalNode.getPrompt().getString();

    int lWidth = XFUtil.nvl(lImageDimensions.mWidth, Integer.MAX_VALUE);
    int lHeight = XFUtil.nvl(lImageDimensions.mHeight, Integer.MAX_VALUE);

    lTemplateVars.put("ImgSrc", lImgSrc);
    lTemplateVars.put("ImgAlt", lImgAlt);

    //TODO - if the widget can find out the image's actual dimensions, it should be possible for the developer to specify
    //a max width AND a max height - the widget could then pick the appropriate constraining dimension and request
    //an appropriate size which maintained aspect ratio (alternative, port the image resizing code from FOX4)

    //Use the smaller dimension to act as a size restriction (only set one so the browser maintains image aspect ratio) */
    //Set max-width/height rather than width/height to avoid stretching smaller images
    if(lWidth <= lHeight) {
      lTemplateVars.put("ImgStyle", "max-width: " + lWidth + "px;");
    }
    else {
      lTemplateVars.put("ImgStyle", "max-height: " + lHeight + "px;");
    }

    lTemplateVars.put("Class", ("uploadedImage " + XFUtil.nvl(lTemplateVars.get("Class"), "")).trim());

    //If widget has an action defined the mustache template should wrap the image in a link
    if(pEvalNode.getFieldMgr().isRunnable()) {
      lTemplateVars.put("Runnable", "true");
      lTemplateVars.put("LinkTitle", pEvalNode.getStringAttribute(NodeAttribute.LINK_TITLE));
    }

    MustacheFragmentBuilder.applyMapToTemplate(MUSTACHE_TEMPLATE, lTemplateVars, pSerialiser.getWriter());
  }

  /**
   * Gets the width and height to set an image to when it is displayed. The developer may either specify an "image-display-size",
   * which is a reference to an "image series" in the resource master, or an explicit width/height. The former takes precedence.
   * A default value of 0 is used if a dimension is not specified.
   *
   * @param pSerialisationContext
   * @param pEvalNode
   * @return Width/height to display image.
   */
  private ImageDimensions getDisplayDimensions(SerialisationContext pSerialisationContext, EvaluatedNodeInfoFileItem pEvalNode) {
    String lDisplaySize =  pEvalNode.getStringAttribute(NodeAttribute.IMAGE_DISPLAY_SIZE);

    Integer lDisplayWidth = null;
    Integer lDisplayHeight = null;

    if(!XFUtil.isNull(lDisplaySize)) {
      Map<String, Object> lImageSeriesInfo = pSerialisationContext.getApp().getImageWidgetProcessing().getImageSeriesInfo(lDisplaySize);

      if(lImageSeriesInfo == null) {
        throw new ExInternal("Failed to locate an image series called " + lDisplaySize + " for widget "  + pEvalNode.getIdentityInformation());
      }

      lDisplayWidth = (Integer) lImageSeriesInfo.get("MaxWidth");
      lDisplayHeight = (Integer) lImageSeriesInfo.get("MaxHeight");
    }
    else {
      if(pEvalNode.isAttributeDefined(NodeAttribute.IMAGE_DISPLAY_WIDTH)) {
        try {
          lDisplayWidth = Integer.parseInt(pEvalNode.getStringAttribute(NodeAttribute.IMAGE_DISPLAY_WIDTH));
        }
        catch (NumberFormatException e) {
          throw new ExInternal("Invalid width dimension specified for image widget " + pEvalNode.getIdentityInformation(), e);
        }
      }

      if(pEvalNode.isAttributeDefined(NodeAttribute.IMAGE_DISPLAY_HEIGHT)) {
        try {
          lDisplayHeight = Integer.parseInt(pEvalNode.getStringAttribute(NodeAttribute.IMAGE_DISPLAY_HEIGHT));
        }
        catch (NumberFormatException e) {
          throw new ExInternal("Invalid height dimension specified for image widget " + pEvalNode.getIdentityInformation(), e);
        }
      }
    }

    return new ImageDimensions(lDisplayWidth, lDisplayHeight);
  }

  /** Internal data class to store image width/height */
  private static class ImageDimensions {
    final Integer mWidth;
    final Integer mHeight;

    public ImageDimensions(Integer pWidth, Integer pHeight) {
      mWidth = pWidth;
      mHeight = pHeight;
    }
  }
}
