package net.foxopen.fox.configuration.resourcemaster.model;

import net.foxopen.fox.dom.DOM;
import net.foxopen.fox.ex.ExApp;
import net.foxopen.fox.ex.ExTooFew;
import net.foxopen.fox.ex.ExTooMany;


public class ProcessedImageSeries {
  private String mImageSize;
  private String mImageCache;
  private int mImageMaxWidthPx;
  private int mImageMaxHeightPx;
  private int mImageMaxColourDepth;

  private final static String IMAGE_SIZE_ELEMENT_NAME = "image-size";
  private final static String IMAGE_CACHE_ELEMENT_NAME = "image-cache";
  private final static String IMAGE_MAX_WIDTH_ELEMENT_NAME = "image-max-width-px";
  private final static String IMAGE_MAX_HEIGHT_ELEMENT_NAME = "image-max-height-px";
  private final static String IMAGE_COLOUR_DEPTH_ELEMENT_NAME = "image-max-colour-depth";


  public static ProcessedImageSeries createProcessImageSeries(DOM pProcessedImageSeriesDOM) throws ExApp {
    ProcessedImageSeries lProcessedImageSeries = new ProcessedImageSeries(pProcessedImageSeriesDOM);
    return lProcessedImageSeries;
  }

  public ProcessedImageSeries(DOM pProcessedImageSeriesDOM) throws ExApp {
    try {
      mImageSize = pProcessedImageSeriesDOM.get1S(IMAGE_SIZE_ELEMENT_NAME);
      mImageCache = pProcessedImageSeriesDOM.get1S(IMAGE_CACHE_ELEMENT_NAME);
      mImageMaxWidthPx = Integer.parseInt(pProcessedImageSeriesDOM.get1S(IMAGE_MAX_WIDTH_ELEMENT_NAME));
      mImageMaxHeightPx = Integer.parseInt(pProcessedImageSeriesDOM.get1S(IMAGE_MAX_HEIGHT_ELEMENT_NAME));
      mImageMaxColourDepth = Integer.parseInt(pProcessedImageSeriesDOM.get1S(IMAGE_COLOUR_DEPTH_ELEMENT_NAME));
    }
    catch (ExTooMany e) {
      throw new ExApp("An error occured processed a processed image series. There were ", e);
    }
    catch (ExTooFew e) {
      throw new ExApp("An error occured processed a processed image series. ", e);
    }
    catch (NumberFormatException e) {
      throw new ExApp("An error occured processed a processed image series. ", e);
    }
  }
}
