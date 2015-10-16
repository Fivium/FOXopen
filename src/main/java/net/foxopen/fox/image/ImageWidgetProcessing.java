/*

Copyright (c) 2010, UK DEPARTMENT OF ENERGY AND CLIMATE CHANGE -
                    ENERGY DEVELOPMENT UNIT (IT UNIT)
All rights reserved.

Redistribution and use in source and binary forms, with or without modification,
are permitted provided that the following conditions are met:

    * Redistributions of source code must retain the above copyright notice,
      this list of conditions and the following disclaimer.
    * Redistributions in binary form must reproduce the above copyright notice,
      this list of conditions and the following disclaimer in the documentation
      and/or other materials provided with the distribution.
    * Neither the name of the DEPARTMENT OF ENERGY AND CLIMATE CHANGE nor the
      names of its contributors may be used to endorse or promote products
      derived from this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR
ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
(INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
(INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

$Id$

*/
package net.foxopen.fox.image;

import net.foxopen.fox.App;
import net.foxopen.fox.configuration.resourcemaster.definition.AppProperty;
import net.foxopen.fox.configuration.resourcemaster.definition.FoxApplicationDefinition;
import net.foxopen.fox.database.UCon;
import net.foxopen.fox.dom.DOM;
import net.foxopen.fox.dom.DOMList;
import net.foxopen.fox.ex.ExApp;
import net.foxopen.fox.ex.ExDB;
import net.foxopen.fox.ex.ExDBDuplicateValue;
import net.foxopen.fox.ex.ExDBSyntax;
import net.foxopen.fox.ex.ExDBTimeout;
import net.foxopen.fox.ex.ExDBTooFew;
import net.foxopen.fox.ex.ExDBTooMany;
import net.foxopen.fox.ex.ExImageCurrentlyProcessing;
import net.foxopen.fox.ex.ExInternal;
import net.foxopen.fox.ex.ExModule;
import net.foxopen.fox.ex.ExServiceUnavailable;
import net.foxopen.fox.ex.ExUserRequest;
import net.foxopen.fox.filetransfer.ImageUploadInfo;
import net.foxopen.fox.logging.FoxLogger;
import oracle.sql.BLOB;
import oracle.sql.Datum;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;


public class ImageWidgetProcessing {

  //Type of the images created by the Image widget
  private static final String STANDARD_IMAGE_TYPE = "PNG";

  private final String SELECT_IMAGE_SQL_STATEMENT;

  private final String SELECT_TRUESIZE_IMAGE_SQL_STATEMENT;

  private final String SELECT_ORIGINAL_IMAGE_SQL_STATEMENT;

  private final String SELECT_ORIGINAL_IMAGE_FOR_UPDATE_SQL_STATEMENT;

  private final String UPDATE_ORIGINAL_FOX_IMAGE_ROTATION;

  private final String INSERT_FOX_IMAGES;

  private final String UPDATE_FOX_IMAGES;

  private final String INSERT_ORIGINAL_FOX_IMAGE;

  private final String IMAGE_STATUS_CHECK;

  private final String PROCESS_IMAGE_DISPATCH_URL;

  private Map mImageSeries;

  public static final String IMAGE_STATUS_DONE = "DONE";
  public static final String IMAGE_STATUS_PENDING = "PENDING";

  public static ImageWidgetProcessing createImageWidgetProcessing(FoxApplicationDefinition pAppDefinition) throws ExApp {
    // Image properties
    String lProcessedImageTable;
    try {
      lProcessedImageTable = pAppDefinition.getPropertyAsString(AppProperty.IMAGE_PROCESSED_IMAGE_TABLE);
      String lProcessedImageUrl = pAppDefinition.getPropertyAsString(AppProperty.IMAGE_PROCESSED_IMAGE_URL);
      DOM lProcessedImageSeriesListDOM = pAppDefinition.getPropertyAsDOM(AppProperty.IMAGE_PROCESSED_IMAGE_SERIES_LIST);
      Map<String, Map<String, Object>> lImageSeries = processImageSeriesList(lProcessedImageSeriesListDOM);
      return new ImageWidgetProcessing(lProcessedImageTable, lProcessedImageUrl, lImageSeries);
    }
    catch (ExApp e) {
      throw new ExApp("Could not create the ImageWidgetProcessing due to an error. ", e);
    }
  }

  private static Map<String, Map<String, Object>> processImageSeriesList(DOM pProcessedImageSeriesListDOM) throws ExApp {
    Map<String, Map<String, Object>> lImageSeries = new HashMap<>();
    boolean lResizeBaseFound = false;
    DOMList lProcessedImageSeriesListDOM = pProcessedImageSeriesListDOM.getUL("*");
    for (int i = 0; i < lProcessedImageSeriesListDOM.getLength(); i++) {
      DOM lProcessedImageSeriesDOM = lProcessedImageSeriesListDOM.item(i);

      String lImageCacheSize = lProcessedImageSeriesDOM.get1SNoEx("image-size");
      String lImageCacheType = lProcessedImageSeriesDOM.get1SNoEx("image-cache");
      int lImageMaxWidth = Integer.parseInt(lProcessedImageSeriesDOM.get1SNoEx("image-max-width-px"));
      int lImageMaxHeight = Integer.parseInt(lProcessedImageSeriesDOM.get1SNoEx("image-max-height-px"));
      int lImageMaxColorDepth = Integer.parseInt(lProcessedImageSeriesDOM.get1SNoEx("image-max-colour-depth"));


      if(lImageCacheType.equals("RESIZE-BASE")) {
        if(lResizeBaseFound) {
          throw new ExInternal("Cannot have two resize bases for image cache type.");
        }
        lResizeBaseFound = true;
      }

      HashMap<String, Object> lImageSeriesSingle = new HashMap<>();
      lImageSeriesSingle.put("CacheSize",lImageCacheSize);
      lImageSeriesSingle.put("CacheType",lImageCacheType);
      lImageSeriesSingle.put("MaxWidth",new Integer(lImageMaxWidth));
      lImageSeriesSingle.put("MaxHeight",new Integer(lImageMaxHeight));
      lImageSeriesSingle.put("MaxColorDepth",new Integer(lImageMaxColorDepth));
      lImageSeries.put(lImageCacheSize, lImageSeriesSingle);
    }

    return lImageSeries;
  }

  public ImageWidgetProcessing(String pProcessedImageTable, String pProcessedImageUrl, Map pImageSeries) {

    PROCESS_IMAGE_DISPATCH_URL = pProcessedImageUrl;

    mImageSeries = pImageSeries;

    final String lGenSelect = "SELECT fi.image_blob" +
                                 ", fi.width" +
                                 ", fi.height " +
                                 ", fi.rotation " +
                                 ", fi.process_type " +
                                 "FROM foxmgr."+pProcessedImageTable+" fi ";

    IMAGE_STATUS_CHECK =  "WITH q AS( \n" +
                          "  SELECT * \n" +
                          "  FROM(\n" +
                          "    SELECT \n" +
                          "      CASE WHEN process_type = 'TRUE_SIZE' AND width <= :1 AND height <= :2 OR (width = :3 AND height <= :4) OR (width <= :5 AND height = :6) THEN 'CACHED' WHEN process_type IN('CACHED','TEMP') AND (width = :7 OR height = :8) THEN 'RESIZE_BASE' ELSE process_type END process_type_true\n" +
                          "    , creation_date  \n" +
                          "    FROM foxmgr."+pProcessedImageTable+"\n" +
                          "    WHERE file_id = :9\n" +
                          "    AND (\n" +
                          "      (process_type IN('CACHED','TEMP') AND (\n" +
                          "           (width = :6 OR height = :10) --target size\n" +
                          "        OR (width = :8 OR height = :11) --resize base\n" +
                          "       )\n" +
                          "      OR process_type NOT IN('CACHED','TEMP')\n" +
                          "      )\n" +
                          "    )\n" +
                          "    AND rotation = :12\n" +
                          "    UNION\n" +
                          "    SELECT\n" +
                          "      'PENDING' process_type_true\n" +
                          "    , sysdate creation_date\n" +
                          "    FROM dual\n" +
                          "  )\n" +
                          "  ORDER BY \n" +
                          "    CASE process_type_true\n" +
                          "    WHEN 'CACHED' THEN 1\n" +
                          "    WHEN 'TEMP' THEN 1\n" +
                          "    WHEN 'RESIZE_BASE' THEN 2\n" +
                          "    WHEN 'TRUE_SIZE' THEN 3\n" +
                          "    WHEN 'ORIGINAL' THEN 4\n" +
                          "    WHEN 'PENDING' THEN 5\n" +
                          "    ELSE 6\n" +
                          "  END\n" +
                          "  )\n" +
                          "SELECT\n" +
                          "  process_type_true\n" +
                          ", CASE \n" +
                          "    WHEN process_type_true IN('CACHED','TEMP') THEN 100\n" +
                          "    WHEN process_type_true = 'RESIZE_BASE' THEN LEAST(99, (90 + ((SYSDATE - creation_date) * 24 * 60 * 30)))\n" +
                          "    WHEN process_type_true = 'TRUE_SIZE' THEN LEAST(89, (70 + ((SYSDATE - creation_date) * 24 * 60 * 30)))\n" +
                          "    WHEN process_type_true = 'ORIGINAL' THEN LEAST(49, (10 + ((SYSDATE - creation_date) * 24 * 60 * 30)))\n" +
                          "    ELSE LEAST(9, ((SYSDATE - creation_date) * 24 * 60 * 30) + 1)\n" +
                          "  END pct_complete\n" +
                          "FROM q\n" +
                          "WHERE rownum = 1  ";

    SELECT_IMAGE_SQL_STATEMENT = lGenSelect +
                                 "WHERE fi.file_id = :1 " +
                                 "AND (:2 IS NULL OR fi.rotation = :3) " +
                                 "AND " +
                                 "(" +
                                 "  (" +
                                 "    (" +
                                 "      fi.width = :4 " +
                                 "      AND fi.height <= :5 " +
                                 "    )" +
                                 "    OR" +
                                 "    (" +
                                 "      fi.width <= :6 " +
                                 "      AND fi.height = :7 " +
                                 "    )" +
                                 "  )" +
                                 "  OR" +
                                 "  (" +
                                 "    process_type = 'TRUE_SIZE' " +
                                 "    AND fi.width <= :8 " +
                                 "    AND fi.height <= :9 " +
                                 "  )" +
                                 ")";
    SELECT_TRUESIZE_IMAGE_SQL_STATEMENT = lGenSelect +
                                          "WHERE fi.file_id = :1 " +
                                          "AND (:2 IS NULL OR fi.rotation = :3) " +
                                          "AND process_type = 'TRUE_SIZE'";
    SELECT_ORIGINAL_IMAGE_SQL_STATEMENT = lGenSelect +
                                          "WHERE fi.file_id = :1 " +
                                          "AND process_type = 'ORIGINAL'";
    SELECT_ORIGINAL_IMAGE_FOR_UPDATE_SQL_STATEMENT = SELECT_ORIGINAL_IMAGE_SQL_STATEMENT + " FOR UPDATE OF fi.image_blob";
    UPDATE_ORIGINAL_FOX_IMAGE_ROTATION = "UPDATE foxmgr."+pProcessedImageTable+
                                          " SET rotation = :1 " +
                                          "WHERE file_id = :2 " +
                                          " AND rotation = :3 " +
                                          " AND process_type = 'ORIGINAL'";
    INSERT_FOX_IMAGES = "INSERT INTO foxmgr."+pProcessedImageTable+"(file_id, width, height, rotation, image_type, process_type)" +
                        " VALUES (:1, :2, :3, :4, '', 'LOCK')";
    UPDATE_FOX_IMAGES = "UPDATE foxmgr."+pProcessedImageTable+
                        " SET image_blob = :1" +
                        ", image_type = :2" +
                        ", process_type = :3 " +
                        ", creation_date = sysdate " +
                        "WHERE file_id = :4" +
                        "  AND width = :5" +
                        "  AND height = :6" +
                        "  AND rotation = :7";
    INSERT_ORIGINAL_FOX_IMAGE = "DECLARE" +
                                "  PRAGMA AUTONOMOUS_TRANSACTION; " +
                                "BEGIN" +
                                "  INSERT INTO foxmgr."+pProcessedImageTable+"(file_id, image_blob, width, height, rotation, image_type, process_type, creation_date)" +
                                "  VALUES (:1, :2, 0, 0, 0, 'UNKNOWN', 'ORIGINAL', sysdate);" +
                                "  COMMIT;" +
                                "END;";
  }

  /** Look for an image in the cache, if not there get off database **/
  public BLOB getOrCreateImage(String pFileId, int pWidth, int pHeight, Integer pRotation, boolean pNoWait, UCon pUCon)  throws
    ExServiceUnavailable
  , ExModule // when module fails to validate
  , ExApp // when app resource file type not known
  , ExUserRequest // when URL pathname invalid
  , ExInternal
  , SQLException //thrown by convert BLOB to image
  , IOException //thrown by convert blob to image
  {
    try {
      Object[] lImage;

      int lRotation;
      if(pRotation == null) {
        lRotation = getImageRotation(pFileId,pUCon);
      }
      else {
        lRotation = pRotation.intValue();
      }

      //Width or height are not positive return truesize
      if(pWidth <= 0 || pHeight <= 0) {
        lImage = getTruesizeImage(pFileId,lRotation,true,pNoWait,pUCon);
        return (BLOB)lImage[0];
      }

      lImage = getOrCreateImageUsingImageSeries(pFileId,pWidth,pHeight,lRotation,true,pNoWait,pUCon);

      return (BLOB)lImage[0];
    }
    catch(ExImageCurrentlyProcessing e) {
      BLOB lBlob = pUCon.getTemporaryBlob();
      ImageIO.write(ImageUtils.generateTextImage(pWidth==0?200:pWidth,pHeight==0?200:pHeight,"Image processing, please wait..."), ImageWidgetProcessing.STANDARD_IMAGE_TYPE, lBlob.setBinaryStream(1));
      return lBlob;
    }
    catch(ExInternal e) {
      FoxLogger.getLogger().error("Error processing image", e);
      BLOB lBlob = pUCon.getTemporaryBlob();
      ImageIO.write(ImageUtils.generateTextImage(pWidth==0?200:pWidth,pHeight==0?200:pHeight,"Error processing image..."), ImageWidgetProcessing.STANDARD_IMAGE_TYPE, lBlob.setBinaryStream(1));
      return lBlob;
    }
  }

  private final Object[] getOrDispatchCachedImage(String pFileId, int pNewWidth, int pNewHeight, int pNewRotation, boolean pNoWait, UCon pUCon) throws ExServiceUnavailable {
    if(pUCon == null) {
      throw new ExInternal("pUCon is not optional.");
    }
    pNewRotation = ImageUtils.getNormalisedRotation(pNewRotation);

    Object[] lCachedImage = getImageFromCache(pFileId, pNewWidth, pNewHeight, pNewRotation, pUCon);
    if(lCachedImage != null) {
      return lCachedImage;
    }
    dispatchRequestToCache(pFileId,pNewWidth,pNewHeight,pNewRotation, pNoWait);
    lCachedImage = getImageFromCache(pFileId, pNewWidth, pNewHeight, pNewRotation, pUCon);
    if(lCachedImage == null) {
      throw new ExInternal("Dispatched request has not created required image.");
    }
    return lCachedImage;
  }

  private final Object[] getOrCreateCachedImage(String pFileId, int pNewWidth, int pNewHeight, int pNewRotation, boolean pDispatchable, boolean pResizeBase, boolean pNoWait, UCon pUCon) throws ExServiceUnavailable {
    if(pUCon == null) {
      throw new ExInternal("pUCon is not optional.");
    }
    pNewRotation = ImageUtils.getNormalisedRotation(pNewRotation);

    Object[] lImage = getImageFromCache(pFileId, pNewWidth, pNewHeight, pNewRotation, pUCon);
    if(lImage != null) {
      return lImage;
    }
    lImage = getRotateCandidate(pFileId, pNewWidth, pNewHeight, pNewRotation, pUCon);
    if(lImage != null) {
      return rotateAndCache(pFileId, lImage, pNewRotation, "CACHED", pUCon);
    }

    if(pDispatchable && pResizeBase) {
      dispatchRequestToCache(pFileId,pNewWidth,pNewHeight,pNewRotation,pNoWait);
      lImage = getImageFromCache(pFileId, pNewWidth, pNewHeight, pNewRotation, pUCon);
      if(lImage == null) {
        throw new ExInternal("Dispatched request has not created required image.");
      }
      return lImage;
    }
    lImage = getOrCreateImageUsingImageSeries(pFileId, pNewWidth, pNewHeight, pNewRotation, true, pDispatchable, pNoWait, pUCon);
    return resizeAndCache(pFileId, lImage, pNewWidth, pNewHeight, "CACHED", pUCon);
  }

  /** Look for truesize images, will not create new truesize images **/
  private List getTruesizeImages(String pFileId, UCon pUCon) {
    if(pUCon == null) {
      throw new ExInternal("pUCon is not optional.");
    }
    try {
      return pUCon.executeSelectAllRows(SELECT_TRUESIZE_IMAGE_SQL_STATEMENT, new String[] {pFileId, null, null}, false, false);
    }
    catch(ExDB e) {
      throw e.toUnexpected();
    }
  }

  private final Object[] getTruesizeImage(String pFileId, int pNewRotation, boolean pDispatchable, boolean pNoWait, UCon pUCon) throws ExServiceUnavailable {
    if(pUCon == null) {
      throw new ExInternal("pUCon is not optional.");
    }

    pNewRotation = ImageUtils.getNormalisedRotation(pNewRotation);
    try {
      return pUCon.selectOneRow(SELECT_TRUESIZE_IMAGE_SQL_STATEMENT, new String[] {pFileId, Integer.toString(pNewRotation), Integer.toString(pNewRotation)});
    } catch(ExDBTooFew x) {
      if(pDispatchable) {
        if(!dispatchRequestToCache(pFileId,0,0,pNewRotation, pNoWait)) {
          throw new ExInternal("Dispatched request to cache image failed");
        }
      }
      else {
        createTruesizeImage(pFileId,pNewRotation,pNoWait,pUCon);
      }
      try {
        return pUCon.selectOneRow(SELECT_TRUESIZE_IMAGE_SQL_STATEMENT, new String[] {pFileId, Integer.toString(pNewRotation), Integer.toString(pNewRotation)});
      } catch (ExDBTooFew e) {
         throw new ExInternal("Could not find cached image.");
      } catch (ExDBTooMany e) {
         throw new ExInternal("Somebody has been playing with the unique keys on the processed image table, what a naughty person!");
      } catch (ExDB e) {
        throw x.toServiceUnavailable();
      }
    } catch (ExDBTooMany e) {
      throw new ExInternal("Somebody has been playing with the unique keys on the processed image table, what a naughty person!");
    } catch(ExDB x) {
      throw x.toUnexpected();
    }
  }

  /** Look for image in cache **/
  private Object[] getImageFromCache(String pFileId, int pNewWidth, int pNewHeight, int pNewRotation, UCon pUCon)
    throws ExServiceUnavailable {
    if(pUCon == null) {
      throw new ExInternal("pUCon is not optional.");
    }
    pNewRotation = ImageUtils.getNormalisedRotation(pNewRotation);
    try {
      //Attempt to select to already rotated image from the database
      return pUCon.selectOneRow(SELECT_IMAGE_SQL_STATEMENT, new String[] {pFileId, Integer.toString(pNewRotation), Integer.toString(pNewRotation), Integer.toString(pNewWidth), Integer.toString(pNewHeight), Integer.toString(pNewWidth), Integer.toString(pNewHeight), Integer.toString(pNewWidth), Integer.toString(pNewHeight)});
    } catch (ExDBTooFew ignoreEx) {
    } catch (ExDBTimeout e) {
      throw e.toServiceUnavailable();
    } catch (ExDBSyntax e) {
      throw e.toUnexpected();
    } catch (ExDBTooMany e) {
      throw new ExInternal("Found more than one cached image, check unique keys on processed image table.",e);
    }
    return null;
  }

  /** Look for images in cache **/
  private List getImagesFromCache(String pFileId, int pNewWidth, int pNewHeight, UCon pUCon)
    throws ExServiceUnavailable {
    if(pUCon == null) {
      throw new ExInternal("pUCon is not optional.");
    }
    try {
      //Attempt to select to already rotated image from the database
      return pUCon.executeSelectAllRows(SELECT_IMAGE_SQL_STATEMENT, new String[] {pFileId, null, null, Integer.toString(pNewWidth), Integer.toString(pNewHeight), Integer.toString(pNewWidth), Integer.toString(pNewHeight), Integer.toString(pNewWidth), Integer.toString(pNewHeight)}, false, false);
    } catch (ExDBTimeout e) {
      throw e.toServiceUnavailable();
    } catch (ExDBSyntax e) {
      throw e.toUnexpected();
    }
  }

  public void createCachedImage(String pFileId, int pWidth, int pHeight, Integer pRotation, boolean pDirectFromTruesize, boolean pNoWait, UCon pUCon)  throws ExServiceUnavailable {
    if(pUCon == null) {
      throw new ExInternal("pUCon is not optional.");
    }

    int lRotation = pRotation == null ? getImageRotation(pFileId,pUCon) : pRotation.intValue();

    //Width or height are not positive return truesize
    if(pWidth <= 0 || pHeight <= 0) {
      createTruesizeImage(pFileId,lRotation,pNoWait,pUCon);
    }
    else if(pDirectFromTruesize) {
      createCachedFromTruesizeImage(pFileId,pWidth,pHeight,lRotation,pNoWait,pUCon);
    }
    else {
      getOrCreateImageUsingImageSeries(pFileId,pWidth,pHeight,lRotation,false,pNoWait,pUCon);
    }
  }

  public final boolean cacheOriginalImage(String pFileId, Datum pImageToCache, UCon pUCon) throws ExServiceUnavailable {
    if(pUCon == null) {
      throw new ExInternal("UCon to cache with cannot be null");
    }
    try {
      try {
        pUCon.selectOneRow(SELECT_ORIGINAL_IMAGE_SQL_STATEMENT, new String[] {pFileId});
        return true;
      } catch (ExDBTimeout e) {
        throw e.toServiceUnavailable();
      } catch (ExDBSyntax e) {
        throw e.toUnexpected();
      } catch (ExDB ignoreEx) {
      }

      //Create the bind variables object for the insert statement
      Object[] lBindVariables = new Object[2];
      lBindVariables[0] = pFileId;
      lBindVariables[1] = pImageToCache;

      //Run the insert statement
      pUCon.executeDML(INSERT_ORIGINAL_FOX_IMAGE, lBindVariables);
      // We don't need a commit here as we committed using an autonomous transaction.
      return true;
    } catch (ExDBTimeout e) {
      throw e.toServiceUnavailable();
    } catch (ExDBSyntax e) {
      throw new ExInternal("Error caching image",e);
    }

//    //Now create resize bases on other server
//    int lResizeWidth = Integer.MAX_VALUE;
//    int lResizeHeight=Integer.MAX_VALUE;
//    Iterator lIter = mImageSeries.values().iterator();
//    while(lIter.hasNext()) {
//      Map lCurrentSeries = (Map)lIter.next();
//      boolean lResizeBase = lCurrentSeries.get("CacheType").equals("RESIZE-BASE");
//      int lMaxWidth = ((Integer)lCurrentSeries.get("MaxWidth")).intValue();
//      int lMaxHeight = ((Integer)lCurrentSeries.get("MaxHeight")).intValue();
//      if( (lResizeBase && lMaxWidth <= lResizeWidth && lMaxHeight <= lResizeHeight)) {
//        lResizeHeight = lMaxHeight;
//        lResizeWidth = lMaxWidth;
//      }
//    }
//    return dispatchRequestToCache(pFileId, lResizeWidth, lResizeHeight, 0, true, true);
  }

  public Object[] getOrCreateImageUsingImageSeries(String pFileId, int pNewWidth, int pNewHeight, int pNewRotation, boolean pDispatchable, boolean pNoWait, UCon pUCon) throws ExServiceUnavailable {
    return getOrCreateImageUsingImageSeries(pFileId,pNewWidth,pNewHeight, pNewRotation, false, pDispatchable, pNoWait, pUCon);
  }

  private final Object[] getOrCreateImageUsingImageSeries(String pFileId, int pNewWidth, int pNewHeight, int pNewRotation, boolean pForceUseNextResizeBase, boolean pDispatchable, boolean pNoWait, UCon pUCon) throws ExServiceUnavailable {
    if(pUCon == null) {
      throw new ExInternal("pUCon is not optional.");
    }
    if(!pForceUseNextResizeBase) { //If they do want the resize base it probably won't match width/height
      Object[] lCachedImage = getImageFromCache(pFileId, pNewWidth, pNewHeight, pNewRotation, pUCon);
      if(lCachedImage!=null) {
        return lCachedImage;
      }
    }
    Map lSeriesToUse = null;
    Map lResizeBaseSeries = null;
    Map lPrevResizeBaseSeries = null;
    Iterator lIter = mImageSeries.values().iterator();
    while(lIter.hasNext()) {
      Map lCurrentSeries = (Map)lIter.next();
      boolean lResizeBase = lCurrentSeries.get("CacheType").equals("RESIZE-BASE");
      int lMaxWidth = ((Integer)lCurrentSeries.get("MaxWidth")).intValue();
      int lMaxHeight = ((Integer)lCurrentSeries.get("MaxHeight")).intValue();
      if( (lResizeBase && lMaxWidth >= pNewWidth && lMaxHeight >= pNewHeight) || (lMaxWidth == pNewWidth || lMaxHeight == pNewHeight) ) { //Remove second condition set of == in order to use the nearest series to resize non-standard from, as it is it will always use the resize base.
        if(lResizeBase && (lResizeBaseSeries == null || (lMaxWidth <= ((Integer)lResizeBaseSeries.get("MaxWidth")).intValue() && lMaxWidth <= ((Integer)lResizeBaseSeries.get("MaxHeight")).intValue()) )) { //Update lResizeBaseSeries when the current lResizeBaseSeries is bigger than the new proposal
          lPrevResizeBaseSeries = lResizeBaseSeries;
          lResizeBaseSeries = lCurrentSeries;
        }
        else if(lResizeBase && (lPrevResizeBaseSeries == null || (lMaxWidth <= ((Integer)lPrevResizeBaseSeries.get("MaxWidth")).intValue() && lMaxWidth <= ((Integer)lPrevResizeBaseSeries.get("MaxHeight")).intValue()) )) { //Need to set lPrevResizeBaseSeries properly if the series are not in descending order
          lPrevResizeBaseSeries = lCurrentSeries;
        }
        if(lSeriesToUse == null || (lMaxWidth <= ((Integer)lSeriesToUse.get("MaxWidth")).intValue() && lMaxWidth <= ((Integer)lSeriesToUse.get("MaxHeight")).intValue()) ) {
          lSeriesToUse = lCurrentSeries;
        }
      }
    }
    if(lSeriesToUse == null) {
      if(lResizeBaseSeries == null) { //We haven't got a series to use or a resize base to use, so we'll have to use the truesize image.
        Object[] lImage = getTruesizeImage(pFileId,pNewRotation,pDispatchable,pNoWait,pUCon);
        if(pForceUseNextResizeBase) {
          return lImage;
        }
        else {
          return resizeAndCache(pFileId, lImage, pNewWidth, pNewHeight, "TEMP", pUCon);
        }
      } //Otherwise we get the resize base
      return getOrCreateCachedImage(pFileId,((Integer)lResizeBaseSeries.get("MaxWidth")).intValue(),((Integer)lResizeBaseSeries.get("MaxHeight")).intValue(),pNewRotation,pDispatchable,pDispatchable,pNoWait,pUCon);
    }
    else if(pForceUseNextResizeBase) { //We have a series to use but we want to force using the resize base anyways
      if(lResizeBaseSeries == lSeriesToUse || lResizeBaseSeries == null) {
        if(lPrevResizeBaseSeries == null) {//There is no resize base to use so we'll have to resort to the truesize image.
          return getTruesizeImage(pFileId,pNewRotation,pDispatchable,pNoWait,pUCon);
        }
        return getOrCreateCachedImage(pFileId,((Integer)lPrevResizeBaseSeries.get("MaxWidth")).intValue(),((Integer)lPrevResizeBaseSeries.get("MaxHeight")).intValue(),pNewRotation,pDispatchable,pDispatchable,pNoWait,pUCon);
      }
      //Otherwise we can just get the resize base image
      return getOrCreateCachedImage(pFileId,((Integer)lResizeBaseSeries.get("MaxWidth")).intValue(),((Integer)lResizeBaseSeries.get("MaxHeight")).intValue(),pNewRotation,pDispatchable,pDispatchable,pNoWait,pUCon);
    }
    else { //Otherwise we want to use the series we've found
      if(((Integer)lSeriesToUse.get("MaxWidth")).intValue() != pNewWidth && ((Integer)lSeriesToUse.get("MaxHeight")).intValue() != pNewHeight) { //Check for rotate of TEMP first
        Object[] lImage = getRotateCandidate(pFileId, pNewWidth, pNewHeight, pNewRotation, pUCon);
        if(lImage != null) {
          return rotateAndCache(pFileId,lImage,pNewRotation,"TEMP",pUCon);
        }
     }
      Object[] lImage = getOrCreateCachedImage(pFileId,((Integer)lSeriesToUse.get("MaxWidth")).intValue(),((Integer)lSeriesToUse.get("MaxHeight")).intValue(),pNewRotation,pDispatchable,lSeriesToUse.get("CacheType").equals("RESIZE-BASE"),pNoWait,pUCon);
      if(((Integer)lSeriesToUse.get("MaxWidth")).intValue() != pNewWidth && ((Integer)lSeriesToUse.get("MaxHeight")).intValue() != pNewHeight) {//The actual image wanted is not a series
        return resizeAndCache(pFileId, lImage, pNewWidth, pNewHeight, "TEMP", pUCon);
      }
      return lImage;
    }
  }

  public final void createTruesizeImage(String pFileId, int pNewRotation, boolean pNoWait, UCon pUCon) throws ExServiceUnavailable {
    try {
      if(pUCon == null) {
        throw new ExInternal("pUCon is not optional");
      }
      pNewRotation = ImageUtils.getNormalisedRotation(pNewRotation);
      try {
        pUCon.selectOneRow(SELECT_TRUESIZE_IMAGE_SQL_STATEMENT, new String[] {pFileId, Integer.toString(pNewRotation), Integer.toString(pNewRotation)});
       } catch(ExDBTooFew x) {
        try {
          List lRotCandidates = getTruesizeImages(pFileId, pUCon);
          if(lRotCandidates != null && lRotCandidates.size() > 0) { //If there is any truesize image to rotate from we'll rotate from that
            Object[] lRotCandidate = (Object[])lRotCandidates.get(0);
            synchronized(App.TRUESIZE_LOCK) {
              rotateAndCache(pFileId, lRotCandidate, pNewRotation, "TRUE_SIZE", pUCon);
            }
          }
          else { //Otherwise fallback to original image to create true size
            try {
              Object[] lQueryResults = pUCon.selectOneRow(SELECT_ORIGINAL_IMAGE_FOR_UPDATE_SQL_STATEMENT+(pNoWait?" NOWAIT":""), new String[] {pFileId});
              synchronized(App.TRUESIZE_LOCK) {
                try { //Retry getting cached from database rather than redo work if another beat us here.
                  pUCon.selectOneRow(SELECT_TRUESIZE_IMAGE_SQL_STATEMENT, new String[] {pFileId, Integer.toString(pNewRotation), Integer.toString(pNewRotation)});
                  return;
                } catch(ExDBTooFew ignoreEx) {
                }
                lRotCandidates = getTruesizeImages(pFileId, pUCon); //Retry getting a suitable rotation candidate.
                if(lRotCandidates != null && lRotCandidates.size() > 0) { //If there is any truesize image to rotate from we'll rotate from that
                  Object[] lRotCandidate = (Object[])lRotCandidates.get(0);
                  synchronized(App.TRUESIZE_LOCK) {
                    rotateAndCache(pFileId, lRotCandidate, pNewRotation, "TRUE_SIZE", pUCon);
                    return;
                  }
                }

                BufferedImage lDatabaseImage = ImageUtils.convertBlobToImage((BLOB)lQueryResults[0]);
                lQueryResults = null;
                if(startCachingImage(pFileId,lDatabaseImage.getWidth(),lDatabaseImage.getHeight(),0,pUCon)) { //Add a 0 degrees TRUE_SIZE since we have it in memory already.
                  endCachingImage(pFileId,lDatabaseImage,"TRUE_SIZE",0,pUCon);
                }
                if(!lDatabaseImage.getColorModel().getColorSpace().isCS_sRGB()) {//Try reloading if it is not sRGB - this works out faster than keeping non-sRGB image in memory.
                  lDatabaseImage = ImageUtils.convertBlobToImage((BLOB)pUCon.selectOneRow(SELECT_TRUESIZE_IMAGE_SQL_STATEMENT, new String[] {pFileId, Integer.toString(0), Integer.toString(0)})[0]);
                }
                if(pNewRotation != 0) {
                  rotateAndCache(pFileId, lDatabaseImage, pNewRotation, "TRUE_SIZE", pUCon);
                }
                lDatabaseImage = null;
              }
            } catch(ExDBTimeout e) {
              throw new ExImageCurrentlyProcessing("Image currently processing.",e);
            } catch(ExDBTooFew e) {
              //If image wanted not in table then we have a problem
              throw new ExInternal("Could not find original image", e);
            }
          }
        } catch(ExDB e) {
          throw e.toServiceUnavailable();
        } catch (SQLException e) {
          throw new ExInternal("Could not get truesize image", e);
        } catch (IOException e) {
          throw new ExInternal("Could not get truesize image", e);
        }
      } catch(ExDB x) {
       throw x.toServiceUnavailable();
      }
    }
    catch(OutOfMemoryError oom) {
      FoxLogger.getLogger().error("Ran out of memory processing image, using: {}", Runtime.getRuntime().totalMemory(), oom);
      throw new ExInternal("Not enough memory to work with image", oom);
    }
  }

  /**
   * This method is used to created a cached image directly from the truesize, if it needs to create the truesize from the original it will do so and keep the truesize in memory to create the cached from (other paths will reload the truesize from the database after having created it)
   * @param pFileId
   * @param pNewWidth
   * @param pNewHeight
   * @param pNewRotation
   * @param pNoWait
   * @param pUCon
   * @throws ExServiceUnavailable
   */
  public final void createCachedFromTruesizeImage(String pFileId, int pNewWidth, int pNewHeight, int pNewRotation, boolean pNoWait, UCon pUCon) throws ExServiceUnavailable {
    try {
      if(pUCon == null) {
        throw new ExInternal("pUCon is not optional");
      }
      pNewRotation = ImageUtils.getNormalisedRotation(pNewRotation);
      BufferedImage lTruesize = null;
      synchronized(App.TRUESIZE_LOCK) {
        try {
          try {
            lTruesize = ImageUtils.convertBlobToImage((BLOB)pUCon.selectOneRow(SELECT_TRUESIZE_IMAGE_SQL_STATEMENT, new String[] {pFileId, Integer.toString(pNewRotation), Integer.toString(pNewRotation)})[0]);
          }
          catch (SQLException e) {
            throw new ExInternal("Could not load truesize image", e);
          } catch (IOException e) {
            throw new ExInternal("Could not load truesize image", e);
          }
         } catch(ExDBTooFew x) {
          try {
            List lRotCandidates = getTruesizeImages(pFileId, pUCon);
            if(lRotCandidates != null && lRotCandidates.size() > 0) { //If there is any truesize image to rotate from we'll rotate from that
              Object[] lRotCandidate = (Object[])lRotCandidates.get(0);
              lTruesize = rotateAndCacheReturningImage(pFileId, ImageUtils.convertBlobToImage((BLOB)lRotCandidate[0]), ((Double)lRotCandidate[3]).intValue(), pNewRotation, "TRUE_SIZE", pUCon);
            }
            else { //Otherwise fallback to original image to create true size
              try {
                Object[] lQueryResults = pUCon.selectOneRow(SELECT_ORIGINAL_IMAGE_FOR_UPDATE_SQL_STATEMENT+(pNoWait?" NOWAIT":""), new String[] {pFileId});
                try { //Retry getting cached from database rather than redo work if another beat us here.
                  lTruesize = ImageUtils.convertBlobToImage((BLOB)pUCon.selectOneRow(SELECT_TRUESIZE_IMAGE_SQL_STATEMENT, new String[] {pFileId, Integer.toString(pNewRotation), Integer.toString(pNewRotation)})[0]);
                } catch(ExDBTooFew ignoreEx) {
                  lRotCandidates = getTruesizeImages(pFileId, pUCon); //Retry getting a suitable rotation candidate.
                  if(lRotCandidates != null && lRotCandidates.size() > 0) { //If there is any truesize image to rotate from we'll rotate from that
                    Object[] lRotCandidate = (Object[])lRotCandidates.get(0);
                    lTruesize = rotateAndCacheReturningImage(pFileId, ImageUtils.convertBlobToImage((BLOB)lRotCandidate[0]), ((Double)lRotCandidate[3]).intValue(), pNewRotation, "TRUE_SIZE", pUCon);
                  }
                  else {
                    lTruesize = ImageUtils.convertBlobToImage((BLOB)lQueryResults[0]);
                    lQueryResults = null;
                    if(startCachingImage(pFileId,lTruesize.getWidth(),lTruesize.getHeight(),0,pUCon)) { //Add a 0 degrees TRUE_SIZE since we have it in memory already.
                      endCachingImage(pFileId,lTruesize,"TRUE_SIZE",0,pUCon);
                    }
                    if(!lTruesize.getColorModel().getColorSpace().isCS_sRGB()) {//Try reloading if it is not sRGB - this works out faster than keeping non-sRGB image in memory.
                      lTruesize = ImageUtils.convertBlobToImage((BLOB)pUCon.selectOneRow(SELECT_TRUESIZE_IMAGE_SQL_STATEMENT, new String[] {pFileId, Integer.toString(0), Integer.toString(0)})[0]);
                    }
                    if(pNewRotation != 0) {
                      lTruesize = rotateAndCacheReturningImage(pFileId, lTruesize, 0, pNewRotation, "TRUE_SIZE", pUCon);
                    }
                  }
                }
              } catch(ExDBTimeout e) {
                throw new ExImageCurrentlyProcessing("Image currently processing.",e);
              } catch(ExDBTooFew e) {
                //If image wanted not in table then we have a problem
                throw new ExInternal("Could not find original image", e);
              }
            }
          } catch(ExDB e) {
            throw e.toServiceUnavailable();
          } catch (SQLException e) {
            throw new ExInternal("Could not get truesize image", e);
          } catch (IOException e) {
            throw new ExInternal("Could not get truesize image", e);
          }
        } catch(ExDB x) {
         throw x.toServiceUnavailable();
        }
        if(lTruesize == null) {
          throw new ExInternal("Error getting truesize");
        }
        resizeAndCacheFromImage(pFileId, lTruesize, pNewWidth, pNewHeight, pNewRotation, "CACHED", pUCon);
        lTruesize = null;
      }
    }
    catch(OutOfMemoryError oom) {
      FoxLogger.getLogger().error("Ran out of memory processing image, using: {}", Runtime.getRuntime().totalMemory(), oom);
      throw new ExInternal("Not enough memory to work with image", oom);
    }
  }

  private final boolean dispatchRequestToCache(String pFileId, int pNewWidth, int pNewHeight, int pNewRotation, boolean pNoWait) {
    return dispatchRequestToCache(pFileId, pNewWidth, pNewHeight, pNewRotation, false, pNoWait);
  }
  private final boolean dispatchRequestToCache(String pFileId, int pNewWidth, int pNewHeight, int pNewRotation, boolean pDirectFromTruesize, boolean pNoWait) {
    try {
      HttpURLConnection con = (HttpURLConnection)new URL(PROCESS_IMAGE_DISPATCH_URL+"/!CACHE-IMAGE?fileid="+pFileId+"&width="+pNewWidth+"&height="+pNewHeight+"&rotation="+pNewRotation+"&fromtruesize="+Boolean.toString(pDirectFromTruesize)+"&nowait="+Boolean.toString(pNoWait)).openConnection();
      con.connect();
      int responseCode = con.getResponseCode();
      String responseMessage = con.getResponseMessage();
      InputStream lResponseStream = ((InputStream)con.getContent());
      char[] lResponseTextChars = new char[lResponseStream.available()];
      new InputStreamReader(lResponseStream).read(lResponseTextChars);
      String lResponseText = new String(lResponseTextChars);
      con.disconnect();
      if (lResponseText.indexOf("Image currently processing.") > 0) {
        throw new ExImageCurrentlyProcessing("Image currently processing.");
      }
      return responseCode == 200 && responseMessage.toUpperCase().equals("OK") && lResponseText.toUpperCase().equals("<HTML><BODY><PRE>OK</PRE></BODY></HTML>");
    }
    catch(IOException e2) {
      throw new ExInternal("Could not dispatch image processing",e2);
    }
  }

  private final Object[] getRotateCandidate(String pFileId, int pNewWidth, int pNewHeight, int pNewRotation, UCon pUCon) throws ExServiceUnavailable {
    if(pUCon == null) {
      throw new ExInternal("pUCon is not optional.");
    }
    List lRotCandidates = getImagesFromCache(pFileId, pNewWidth, pNewHeight, pUCon);
    if(lRotCandidates != null) {
      Iterator lIter = lRotCandidates.iterator();
      while(lIter.hasNext()) {
        Object[] lRotCandidate = (Object[])lIter.next();
        int lRot = ((Double)lRotCandidate[3]).intValue();
        if((pNewRotation-lRot)%180 == 0 || pNewWidth == pNewHeight) { //Rotation between the images is a multiple of 180 degrees then it doesn't matter that width/height are different in the image specification
          return lRotCandidate;
        }
      }
    }
    return null;
  }

  public int getImageRotation(String pImageId, UCon pUCon) {
    Object[] lResult;
    try {
      lResult = pUCon.selectOneRow(SELECT_ORIGINAL_IMAGE_SQL_STATEMENT, new String[]{pImageId});
    } catch (ExDB e) {
      return 0; //If there is no original image then surely the default is 0?
//      throw new ExInternal("Database error getting image rotation", e);
    }
    return ((Double)lResult[3]).intValue();
  }

  /**
   * Use to retrieve image rotation or a default value (0) if the original
   * image is not guaranteed to exist at invocation time.
   * @param pImageId
   * @param pUCon
   * @return
   */
  public int getImageRotationOrZero(String pImageId, UCon pUCon) {
    Object[] lResult;
    try {
      lResult = pUCon.selectOneRow(SELECT_ORIGINAL_IMAGE_SQL_STATEMENT, new String[]{pImageId});
    } catch (ExDBTooFew e) {
      return 0;
    } catch (ExDB e) {
      throw new ExInternal("Database error getting image rotation", e);
    }
    return ((Double)lResult[3]).intValue();
  }

  private final void rotateAndCache(String pFileId, BufferedImage pDatabaseImage, int pNewRotation, String pProcessType, UCon pUCon) throws ExServiceUnavailable {
    if(pUCon == null) {
      throw new ExInternal("pUCon is not optional.");
    }
    pNewRotation = ImageUtils.getNormalisedRotation(pNewRotation);

    Dimension lDim = ImageUtils.getRotatedDimensions(pDatabaseImage.getWidth(),pDatabaseImage.getHeight(),pNewRotation);
    if(startCachingImage(pFileId,(int)lDim.getWidth(),(int)lDim.getHeight(),pNewRotation,pUCon)) {
      try {
        BufferedImage lBufferedImage = ImageUtils.rotate(pDatabaseImage,pNewRotation);
        pDatabaseImage = null;
        endCachingImage(pFileId,lBufferedImage,pProcessType,pNewRotation,pUCon);
        lBufferedImage = null;
      } finally {
        try {
          pUCon.rollback();
        } catch (ExDB ignoreEx) {
        }
      }
    }
  }

  private final Object[] rotateAndCache(String pFileId, Object[] pImage, int pNewRotation, String pProcessType, UCon pUCon) throws ExServiceUnavailable {
    if(pUCon == null) {
      throw new ExInternal("pUCon is not optional.");
    }
    pNewRotation = ImageUtils.getNormalisedRotation(pNewRotation);
    int lRotation = ImageUtils.getNormalisedRotation(((Double)pImage[3]).intValue());
    if(lRotation == pNewRotation) {
      return pImage;
    }

    int lDeltaRotation = ImageUtils.getNormalisedRotation(pNewRotation-lRotation);
    Dimension lDim = ImageUtils.getRotatedDimensions(((Double)pImage[1]).intValue(),((Double)pImage[2]).intValue(),lDeltaRotation);
    if(startCachingImage(pFileId,(int)lDim.getWidth(),(int)lDim.getHeight(),pNewRotation,pUCon)) {
      try {
        BufferedImage lDatabaseImage = ImageUtils.convertBlobToImage((BLOB)pImage[0]);
        BufferedImage lBufferedImage = ImageUtils.rotate(lDatabaseImage,lDeltaRotation);
        lDatabaseImage = null;
        endCachingImage(pFileId,lBufferedImage,pProcessType,pNewRotation,pUCon);
        lBufferedImage = null;
      } catch (SQLException e) {
        throw new ExInternal("Error encountered when rotating image.");
      } catch (IOException e) {
        throw new ExInternal("Error encountered when rotating image.");
      } finally {
        try {
          pUCon.rollback();
        } catch (ExDB ignoreEx) {
        }
      }
    }
    return getImageFromCache(pFileId, (int)lDim.getWidth(), (int)lDim.getHeight(), pNewRotation, pUCon);
  }

  /**
   * Can be used to save from loading the BufferedImage twice if you need it outside of this function.
   * Warning: can wait on a database lock whilst holding image in memory.
   * @param pFileId
   * @param lImage
   * @param pNewRotation
   * @param pProcessType
   * @param pUCon
   * @return
   * @throws ExServiceUnavailable
   */
  private final BufferedImage rotateAndCacheReturningImage(String pFileId, BufferedImage lImage, int pCurrentRotation, int pNewRotation, String pProcessType, UCon pUCon) throws ExServiceUnavailable {
    if(pUCon == null) {
      throw new ExInternal("pUCon is not optional.");
    }
    pNewRotation = ImageUtils.getNormalisedRotation(pNewRotation);
    int lRotation = ImageUtils.getNormalisedRotation(pCurrentRotation);
    try {
      if(lRotation == pNewRotation) {
        return lImage;
      }

      int lDeltaRotation = ImageUtils.getNormalisedRotation(pNewRotation-lRotation);
      Dimension lDim = ImageUtils.getRotatedDimensions(lImage.getWidth(),lImage.getHeight(),lDeltaRotation);
      if(startCachingImage(pFileId,(int)lDim.getWidth(),(int)lDim.getHeight(),pNewRotation,pUCon)) {
        try {
          BufferedImage lBufferedImage = ImageUtils.rotate(lImage,lDeltaRotation);
          lImage = null;
          endCachingImage(pFileId,lBufferedImage,pProcessType,pNewRotation,pUCon);
          return lBufferedImage;
        } finally {
          try {
            pUCon.rollback();
          } catch (ExDB ignoreEx) {
          }
        }
      }
      return ImageUtils.convertBlobToImage((BLOB)getImageFromCache(pFileId, (int)lDim.getWidth(), (int)lDim.getHeight(), pNewRotation, pUCon)[0]);
    } catch (SQLException e) {
      throw new ExInternal("Error encountered when rotating image.");
    } catch (IOException e) {
      throw new ExInternal("Error encountered when rotating image.");
    }
  }

  private final Object[] resizeAndCache(String pFileId, Object[] lImage, int pNewWidth, int pNewHeight, String pProcessType, UCon pUCon) throws ExServiceUnavailable {
    if(pUCon == null) {
      throw new ExInternal("pUCon is not optional.");
    }
    if(pNewWidth >= ((Double)lImage[1]).intValue() && pNewHeight >= ((Double)lImage[2]).intValue()) {
      return lImage; //Don't scale up
    }
    try {
      int lRot = ((Double)lImage[3]).intValue();
      double lScalingFactor = Math.min( pNewWidth / ((Double)lImage[1]).doubleValue(), pNewHeight / ((Double)lImage[2]).doubleValue() );
      Dimension lDim = ImageUtils.getResizedDimensions(((Double)lImage[1]).intValue(),((Double)lImage[2]).intValue(),lScalingFactor,0);
      if(startCachingImage(pFileId,(int)lDim.getWidth(),(int)lDim.getHeight(),lRot,pUCon)) {
        try {
          BufferedImage lThumbnailBufferedImage;
          if("TRUE_SIZE".equals(lImage[4])) { //Synchronise for truesize images
            synchronized(App.TRUESIZE_LOCK) {
              BufferedImage lDatabaseImage = ImageUtils.convertBlobToImage((BLOB)lImage[0]);
              lThumbnailBufferedImage = ImageUtils.resizeImage(lDatabaseImage,lScalingFactor,0);
              lDatabaseImage = null;
            }
          }
          else {
            BufferedImage lDatabaseImage = ImageUtils.convertBlobToImage((BLOB)lImage[0]);
            lThumbnailBufferedImage = ImageUtils.resizeImage(lDatabaseImage,lScalingFactor,0);
            lDatabaseImage = null;
          }
          endCachingImage(pFileId,lThumbnailBufferedImage,pProcessType,lRot,pUCon);
          lThumbnailBufferedImage = null;
        }
        finally {
          try {
            pUCon.rollback();
          } catch (ExDB ignoreEx) {
          }
        }
      }
      return getImageFromCache(pFileId, pNewWidth, pNewHeight, lRot, pUCon);
    } catch (SQLException e) {
      throw new ExInternal("Error encountered when scaling image.");
    } catch (IOException e) {
      throw new ExInternal("Error encountered when scaling image.");
    }
  }

  /**
   * Resizes from a BufferedImage and caches.
   * Warning: does not lock on truesize, you should already have done so.
   * @param pFileId
   * @param pImage
   * @param pNewWidth
   * @param pNewHeight
   * @param pRotation
   * @param pProcessType
   * @param pUCon
   * @throws ExServiceUnavailable
   */
  private final void resizeAndCacheFromImage(String pFileId, BufferedImage pImage, int pNewWidth, int pNewHeight, int pRotation, String pProcessType, UCon pUCon) throws ExServiceUnavailable {
    if(pUCon == null) {
      throw new ExInternal("pUCon is not optional.");
    }
    if(pNewWidth >= pImage.getWidth() && pNewHeight >= pImage.getHeight()) {
      return; //Don't scale up
    }
    double lScalingFactor = Math.min(pNewWidth/(double)pImage.getWidth(), pNewHeight/(double)pImage.getHeight());
    Dimension lDim = ImageUtils.getResizedDimensions(pImage.getWidth(),pImage.getHeight(),lScalingFactor,0);
    if(startCachingImage(pFileId,(int)lDim.getWidth(),(int)lDim.getHeight(),pRotation,pUCon)) {
      try {
        BufferedImage lThumbnailBufferedImage = ImageUtils.resizeImage(pImage,lScalingFactor,0);
        pImage = null;
        endCachingImage(pFileId,lThumbnailBufferedImage,pProcessType,pRotation,pUCon);
        lThumbnailBufferedImage = null;
      }
      finally {
        try {
          pUCon.rollback();
        } catch (ExDB ignoreEx) {
        }
      }
    }
  }

  private final boolean startCachingImage(String pFileId, int pWidth, int pHeight, int pRotation, UCon pUCon) throws ExServiceUnavailable {
    if(pUCon == null) {
      throw new ExInternal("pUCon is not optional");
    }
    try {
      //Create the bind variables object for the insert statement
      Object[] lBindVariables = new Object[4];
      lBindVariables[0] = pFileId;
      lBindVariables[1] = Integer.toString(pWidth);
      lBindVariables[2] = Integer.toString(pHeight);
      lBindVariables[3] = Integer.toString(pRotation);

      //Run the insert statement
      pUCon.executeDML(INSERT_FOX_IMAGES, lBindVariables);
      return true;
    } catch (ExDBTimeout e) {
      throw new ExInternal("Error caching image",e);
    } catch (ExDBDuplicateValue e) {
      return false;
    } catch (ExDBSyntax e) {
      throw new ExInternal("Error caching image",e);
    }
  }

  private final void endCachingImage(String pFileId, BufferedImage pImageToCache, String pProcessType, int pRotation, UCon pUCon) throws ExServiceUnavailable {
    if(pUCon == null) {
      throw new ExInternal("pUCon is not optional");
    }
    try {
      if(!pProcessType.equals("TRUE_SIZE")) {
        List lResultsList = getTruesizeImages(pFileId, pUCon); //Check for cached truesize image
        if(lResultsList.size() > 0) {
        Object[] lResults = (Object[])lResultsList.get(0);
          if( (((Double)lResults[1]).intValue() == pImageToCache.getHeight() && ((Double)lResults[2]).intValue() == pImageToCache.getWidth())
              || (((Double)lResults[1]).intValue() == pImageToCache.getWidth() && ((Double)lResults[2]).intValue() == pImageToCache.getHeight()) ) {
            pProcessType = "TRUE_SIZE";
          }
          else if( (((Double)lResults[1]).intValue() < pImageToCache.getHeight() && ((Double)lResults[2]).intValue() < pImageToCache.getWidth())
              || (((Double)lResults[1]).intValue() < pImageToCache.getWidth() && ((Double)lResults[2]).intValue() < pImageToCache.getHeight()) ) { //Image to cache is bigger than true size
            //TODO PG: We may not necessarily want to block this, although I'd suggest they have to specify a SCALED_UP processType or similar in order for this to be allowed.
            throw new ExInternal("Attempted to cache a scaled up image");
          }
        }
        else { //It doesn't exist, this means we're caching the first TRUE_SIZE image
          throw new ExInternal("No TRUE_SIZE image is cached yet but we're trying to cache a CACHED image type, check this."); //TODO PG: Deletion policy may make this a valid situation?
//            processType = "TRUE_SIZE"; //PG: May not always be true if we decide not to cache TRUE_SIZE - see exception above, comments may be swapped round if desired behaviour changes.
        }
      }

      BLOB lBlob = pUCon.getTemporaryBlob();

      //Create the bind variables object for the insert statement
      Object[] lBindVariables = new Object[7];
      lBindVariables[0] = lBlob;
      lBindVariables[1] = ImageWidgetProcessing.STANDARD_IMAGE_TYPE;
      lBindVariables[2] = pProcessType;
      lBindVariables[3] = pFileId;
      lBindVariables[4] = Integer.toString(pImageToCache.getWidth());
      lBindVariables[5] = Integer.toString(pImageToCache.getHeight());
      lBindVariables[6] = Integer.toString(pRotation);

      //Convert the image to a BLOB and rewrite to lBlob
      ImageIO.write(pImageToCache, ImageWidgetProcessing.STANDARD_IMAGE_TYPE, lBlob.setBinaryStream(1));
      pImageToCache = null;

      //Run the insert statement
      pUCon.executeDML(UPDATE_FOX_IMAGES, lBindVariables);
      pUCon.commit();
    } catch (IOException e) {
      throw new ExInternal("Error caching image",e);
    } catch (SQLException e) {
      throw new ExInternal("Error caching image",e);
    } catch (ExDBTimeout e) {
      throw new ExInternal("Error caching image",e);
    } catch (ExDBSyntax e) {
      throw new ExInternal("Error caching image",e);
    }
    finally {
      try {
        pUCon.rollback();
      } catch (ExDB ignoreEx) {
      }
    }
  }

  public String[] getImageStatusAndPercentage(String pFileId, int pWidth, int pHeight, Integer pRotation, UCon pUCon) {
    if(pUCon == null) {
      throw new ExInternal("pUCon is not optional");
    }
    String lWidth = Integer.toString(pWidth);
    String lHeight = Integer.toString(pHeight);
    String lRotation = Integer.toString(pRotation==null?getImageRotation(pFileId,pUCon):ImageUtils.getNormalisedRotation(pRotation.intValue()));

    //establish resize base dimensions
    String lRBWidth = "1500";
    String lRBHeight = "1500";
    Iterator lIter = mImageSeries.values().iterator();
    while(lIter.hasNext()) {
      Map lCurrentSeries = (Map)lIter.next();
      if(lCurrentSeries.get("CacheType").equals("RESIZE-BASE")) {
        lRBWidth = ((Integer)lCurrentSeries.get("MaxWidth")).toString();
        lRBHeight = ((Integer)lCurrentSeries.get("MaxHeight")).toString();
      }
    }

    String[] lQryParams = new String[14];
    lQryParams[0] = lQryParams[2] = lQryParams[4] = lQryParams[9] = lWidth;
    lQryParams[1] = lQryParams[3] = lQryParams[5] = lQryParams[10] = lHeight;
    lQryParams[6] = lQryParams[11] = lRBWidth;
    lQryParams[7] = lQryParams[12] = lRBHeight;
    lQryParams[8] = pFileId;
    lQryParams[13] = lRotation;

    try {
      Object[] lResult = pUCon.selectOneRow(IMAGE_STATUS_CHECK, lQryParams);

      String[] lResultObj = new String[2];

      String lResultString = (String) lResult[0];
      if("CACHED".equals(lResultString) || "TEMP".equals(lResultString)) {
        lResultString = IMAGE_STATUS_DONE;
      }
      lResultObj[0] = lResultString;
      lResultObj[1] = String.valueOf(((Double)lResult[1]).intValue());

      return lResultObj;
    } catch (ExDB e) {
      throw new ExInternal("Error getting image status",e);
    }
  }

  public String getImageStatus(String pFileId, int pWidth, int pHeight, Integer pRotation, UCon pUCon) {
    return getImageStatusAndPercentage(pFileId, pWidth, pHeight, pRotation, pUCon)[0];
  }

  private String getImagePercentage(String pFileId, int pWidth, int pHeight, Integer pRotation, UCon pUCon) {
    return getImageStatusAndPercentage(pFileId, pWidth, pHeight, pRotation, pUCon)[1];
  }

  /**
   * Returns the current processing status of the image identified by pImageCacheKey
   * in DOM format for AJAX request
   * @param pImageCacheKey
   * @return
   */
  public DOM getImageStatusDOM(String pFileId, int pImageWidth, int pImageHeight, Integer pImageRotation, String pFoxServletMnem, UCon pUCon) {
    pImageRotation = new Integer( pImageRotation==null ? getImageRotation(pFileId,pUCon) : ImageUtils.getNormalisedRotation(pImageRotation.intValue()) );

    String[] lStatusPct = getImageStatusAndPercentage(pFileId,pImageWidth,pImageHeight,pImageRotation,pUCon);

    DOM lResultDOM = DOM.createDocument("root")
      .addElem("status", lStatusPct[0]).getParentOrNull()
      .addElem("pct-done",lStatusPct[1]).getParentOrNull();

    if(IMAGE_STATUS_DONE.equals(lStatusPct[0])) {
      lResultDOM.addElem("img-src", pFoxServletMnem+"!GET-IMAGE/" + ImageUploadInfo.imageCacheKey(pFileId,pImageWidth,pImageHeight,pImageRotation.intValue())+"&"); //default rotation to 0 if unspecified
    }

    return lResultDOM;
  }

  public final DOM updateImageRotation(String pFileId, int pWidth, int pHeight, int pOldRotation, int pNewRotation, String pFoxUrl, UCon pUCon) throws ExServiceUnavailable {
    if(pUCon == null) {
      throw new ExInternal("pUCon is not optional");
    }
    try {
      pNewRotation = ImageUtils.getNormalisedRotation(pNewRotation);
      pOldRotation = ImageUtils.getNormalisedRotation(pOldRotation);

      //Create the bind variables object for the insert statement
      Object[] lBindVariables = new Object[3];
      lBindVariables[0] = Integer.toString(pNewRotation);
      lBindVariables[1] = pFileId;
      lBindVariables[2] = Integer.toString(pOldRotation);

      //Run the insert statement
      int rowsUpdated = pUCon.executeDML(UPDATE_ORIGINAL_FOX_IMAGE_ROTATION, lBindVariables);
      DOM lResponse = DOM.createDocument("root");
      lResponse.addElem("img-src",pFoxUrl+"!GET-IMAGE/"+ImageUploadInfo.imageCacheKey(pFileId,pWidth,pHeight,getImageRotation(pFileId,pUCon)));
      if( rowsUpdated == 1 ) {//If the rotation was successfuly updated on the original image
        pUCon.commit();
        lResponse.addElem("message","OK");
        return lResponse;
      }
      else if( rowsUpdated == 0 ) {
        lResponse.addElem("message", "Rotation failed. The image may have been rotated by another user. It has been updated to reflect its current rotation.\n\n" +
          "If the rotation is still not correct, you can try rotating it again.");
        return lResponse;
      }
      else {
        try {
          pUCon.rollback();
        } catch (ExDB e) {
          throw new ExInternal("More than one original image was updated, rolling back failed.", e);
        }
        throw new ExInternal("More than one original image was updated, rolled back.");
      }
    } catch (ExDBTimeout e) {
      throw new ExInternal("Error setting image rotation",e);
    } catch (ExDBSyntax e) {
      throw new ExInternal("Error setting image rotation",e);
    } finally {
      try {
        pUCon.rollback();
      } catch (ExDB ignoreEx) {
      }
    }
  }

  public Map<String, Object> getImageSeriesInfo(String pImageSeriesName) {
    HashMap lMap = (HashMap)mImageSeries.get(pImageSeriesName);
    if(lMap == null) {
      return null;
    }
    return (Map)lMap.clone();
  }
}
