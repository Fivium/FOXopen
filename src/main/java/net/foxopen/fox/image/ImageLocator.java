package net.foxopen.fox.image;

import net.foxopen.fox.FoxResponseByteStream;
import net.foxopen.fox.database.UCon;
import net.foxopen.fox.database.UConStatementResult;
import net.foxopen.fox.database.parser.ParsedStatement;
import net.foxopen.fox.entrypoint.ComponentManager;
import net.foxopen.fox.ex.ExDB;
import net.foxopen.fox.ex.ExInternal;
import net.foxopen.fox.sql.SQLManager;
import net.foxopen.fox.thread.RequestContext;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Blob;
import java.sql.SQLException;
import java.util.List;

/**
 * Locates images in the legacy FOX4 cached image table. The smallest possible image which conforms to the requested
 * width/height is chosen. This means we don't serve out an image larger than we need to, and tries to ensure the served
 * image will never have to be scaled up by the browser.
 */
public class ImageLocator {

  private final String mImageId;
  private final int mLocateWidth;
  private final int mLocateHeight;
  private final int mRotation;

  ImageLocator(String pImageId, Integer pWidth, Integer pHeight, int pRotation) {
    mImageId = pImageId;
    mLocateWidth = pWidth == null ? Integer.MAX_VALUE : pWidth;
    mLocateHeight = pHeight == null ? Integer.MAX_VALUE : pHeight;
    mRotation = pRotation;
  }

  void locateAndRespond(RequestContext pRequestContext) {

    ParsedStatement lStatement =  SQLManager.instance().getStatement("LocateImage.sql", getClass());
    UCon lUCon = pRequestContext.getContextUCon().getUCon("LocateImage");
    try {
      List<UConStatementResult> lImageList = lUCon.queryMultipleRows(lStatement, mImageId, mRotation);

      if(lImageList.size() == 0) {
        throw new ExInternal("No suitable images found for image id " + mImageId + ", rotation " + mRotation);
      }

      //Loop through every image, from smallest to largest, until we find one with a dimension that is at least as large
      //as the smaller of our target width and height. We don't want an image smaller than the target size because it will
      // be stretched when it is displayed - however the largest available image will be selected as the fallback
      //(the loop will run through to completion).

      UConStatementResult lResultImage = lImageList.get(0); //Could be initialised to null but this is flagged as a potential bug
      for(UConStatementResult lImage : lImageList) {
        int lWidth = lImage.getInteger("WIDTH");
        int lHeight = lImage.getInteger("HEIGHT");

        lResultImage = lImage;

        //If we are searching for width (i.e. width is the smaller dimension), break the loop when we find an image with matching or greater width
        //Same logic for height if that's the search criterion
        if((mLocateWidth <= mLocateHeight && lWidth >= mLocateWidth) || (mLocateHeight <= mLocateWidth && lHeight >= mLocateHeight)) {
          break;
        }
      }

      //Content type should currently only be PNG
      Blob lImageBlob = lResultImage.getBlob("IMAGE_BLOB");
      String lImageType = "image/" + lResultImage.getString("IMAGE_TYPE").toLowerCase();

      FoxResponseByteStream lFoxResponse = new FoxResponseByteStream(lImageType, pRequestContext.getFoxRequest(), ComponentManager.getComponentBrowserCacheMS());

      //Stream image out as response
      InputStream lBinaryStream = lImageBlob.getBinaryStream();
      IOUtils.copy(lBinaryStream, lFoxResponse.getHttpServletOutputStream());
      lBinaryStream.close();

    }
    catch (ExDB | SQLException | IOException e) {
      throw new ExInternal("Failed to retrieve image " + mImageId, e);
    }
    finally {
      pRequestContext.getContextUCon().returnUCon(lUCon, "LocateImage");
    }
  }
}

