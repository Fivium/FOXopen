package net.foxopen.fox.command.util;

import net.foxopen.fox.XFUtil;
import net.foxopen.fox.database.UCon;
import net.foxopen.fox.database.storage.lob.LOBWorkDoc;
import net.foxopen.fox.dom.DOM;
import net.foxopen.fox.ex.ExInternal;
import net.foxopen.fox.thread.ActionRequestContext;
import net.foxopen.fox.thread.storage.WorkingFileStorageLocation;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

/**
 * Utility functions for creating and dealing with GeneratorDestinations.
 */
public class GeneratorDestinationUtils {
  public static final String OCTET_STREAM_MIME_TYPE = "application/octet-stream";
  private static final GenerateMethod DEFAULT_GENERATE_METHOD = GenerateMethod.DOWNLOAD;
  private static final Map<String, GenerateMethod> DEFAULT_GENERATE_METHOD_VALUES = new HashMap<>();
  static {
    DEFAULT_GENERATE_METHOD_VALUES.put("preview", GenerateMethod.DOWNLOAD);
    DEFAULT_GENERATE_METHOD_VALUES.put("storage-location", GenerateMethod.STORAGE_LOCATION);
  }

  private GeneratorDestinationUtils() {
  }

  public static String getDefaultFileName(String pExtension) {
    return "generated-" + XFUtil.unique() + "." + pExtension;
  }

  /**
   * Constructs an appropriate GeneratorDestination based on the legacy markup of the fm:generate command.
   * @param pMarkupDOM DOM containing command markup.
   * @param pDefaultFileExtension File extension to use if a filename is not specified.
   * @param pDefaultContentType Content type to use if not specified.
   * @return
   */
  public static GeneratorDestination getDestinationFromGenerateCommandMarkup(DOM pMarkupDOM, String pDefaultFileExtension, String pDefaultContentType) {
    return getDestinationFromGenerateCommandMarkup(pMarkupDOM, pDefaultFileExtension, pDefaultContentType, DEFAULT_GENERATE_METHOD_VALUES);
  }

  /**
   * Constructs an appropriate GeneratorDestination based on the legacy markup of the fm:generate command, specifying
   * which method attribute values represent which generate method
   * @param pMarkupDOM DOM containing command markup.
   * @param pDefaultFileExtension File extension to use if a filename is not specified.
   * @param pDefaultContentType Content type to use if not specified.
   * @param pGenerateMethodValues A map of method attribute values and the generate methods they specify
   * @return
   */
  public static GeneratorDestination getDestinationFromGenerateCommandMarkup(DOM pMarkupDOM, String pDefaultFileExtension, String pDefaultContentType, Map<String, GenerateMethod> pGenerateMethodValues) {
    GenerateMethod lMethod = getGenerateMethodFromGenerateCommandMarkup(pMarkupDOM, pGenerateMethodValues);

    if (lMethod == GenerateMethod.STORAGE_LOCATION) {
      String lStoreLocationName = pMarkupDOM.getAttrOrNull("storage-location");
      if(!XFUtil.exists(lStoreLocationName)) {
        throw new ExInternal("fm:generate: Method='storage-location' requires storage-location attr");
      }

      return new StorageLocationGeneratorDestination(lStoreLocationName);
    }
    else if(lMethod == GenerateMethod.DOWNLOAD) {
      //mCache = calcMins(XFUtil.nvl(commandElement.getAttrOrNull("client-cache"), "0")); // default is 0 min on clients browser
      //mScope =  XFUtil.nvl(commandElement.getAttrOrNull("scope"), "SESSION");

      String lFileNameXPath = XFUtil.nvl(pMarkupDOM.getAttrOrNull("file-name"), getDefaultFileName(pDefaultFileExtension));
      String lContentType = XFUtil.nvl(pMarkupDOM.getAttrOrNull("content-type"), XFUtil.nvl(pDefaultContentType, OCTET_STREAM_MIME_TYPE));
      String lDispositionAttachmentXPath = pMarkupDOM.getAttrOrNull("serve-as-attachment");
      String lExpires = calcMins(XFUtil.nvl(pMarkupDOM.getAttrOrNull("expires"), "00:09")); // HH:MM

      if(XFUtil.isNull(lFileNameXPath)) {
        throw new ExInternal("Filename for a generated download cannot be null");
      }

      return new DownloadGeneratorDestination(lFileNameXPath, lContentType, lDispositionAttachmentXPath, "false()", lExpires);
    }
    else {
      throw new ExInternal("fm:generate: Method unknown: "+lMethod);
    }
  }

  private static GenerateMethod getGenerateMethodFromGenerateCommandMarkup(DOM pMarkupDOM, Map<String, GenerateMethod> pGenerateMethodValues) {
    String lMethodAttrValue = pMarkupDOM.getAttrOrNull("method");
    return pGenerateMethodValues.getOrDefault(lMethodAttrValue, DEFAULT_GENERATE_METHOD);
  }

  /**
   * Constructs an appropriate GeneratorDestination based on the legacy markup of the fm:show-popup command. Traditionally
   * this command only allowed downloads.
   * @param pMarkupDOM DOM containing command markup.
   * @param pDefaultFileExtension File extension to use if a filename is not specified.
   * @param pDefaultContentType Content type to use if not specified.
   * @return
   */
  public static DownloadGeneratorDestination getDestinationFromShowPopupCommandMarkup(DOM pMarkupDOM, String pDefaultFileExtension, String pDefaultContentType) {

    String lFileName = XFUtil.nvl(pMarkupDOM.getAttrOrNull("file-name"), "generated." + pDefaultFileExtension);
    String lContentType = XFUtil.nvl(pDefaultContentType, OCTET_STREAM_MIME_TYPE);

    //Legacy defaults were false
    String lDispositionAttachmentXPath = XFUtil.nvl(pMarkupDOM.getAttrOrNull("serve-as-attachment"), "false()");
    String lServeAsResponseXPath = XFUtil.nvl(pMarkupDOM.getAttrOrNull("serve-as-response"), "false()");

    return new DownloadGeneratorDestination(lFileName, lContentType, lDispositionAttachmentXPath, lServeAsResponseXPath, "00:00");
  }

  private static String calcMins(String pTime) {
    int mins = 0;
    int pos;
    String str = pTime;
    if ((pos = str.indexOf(':'))!=-1) {
      str = str.substring(0,pos);
      if (XFUtil.exists(str)) {
        mins = 60*Integer.valueOf(str).intValue();
      }
    }
    else {
      pos=-1;
    }
    str = pTime.substring(pos+1);
    if (XFUtil.exists(str)) {
      mins += Integer.valueOf(str).intValue();
    }
    return String.valueOf(mins);
  }

  /**
   * Writes character data to a Clob.
   * @param pRequestContext
   * @param pGenerator Data source.
   * @param pWFSL Destination Clob location.
   */
  static void clobWriteToWFSL(ActionRequestContext pRequestContext, WriterGenerator pGenerator, WorkingFileStorageLocation<Clob> pWFSL) {

    LOBWorkDoc<Clob> lWorkDoc = pWFSL.createWorkDoc(false);

    UCon lUCon = pRequestContext.getContextUCon().getUCon("GenerateToWFSL");
    try {
      lWorkDoc.open(lUCon);
      try (Writer lClobWriter = lWorkDoc.getLOB().setCharacterStream(0)) {
        pGenerator.writeOutput(lClobWriter);
      }
      lWorkDoc.close(lUCon);
    }
    catch (IOException | SQLException e) {
      throw new ExInternal("Failed to open CLOB stream", e);
    }
    finally {
      pRequestContext.getContextUCon().returnUCon(lUCon, "GenerateToWFSL");
    }
  }

  /**
   * Writes binary data to a Blob.
   * @param pRequestContext
   * @param pGenerator Data source.
   * @param pWFSL Destination Blob location.
   */
  static void blobWriteToWFSL(ActionRequestContext pRequestContext, OutputStreamGenerator pGenerator, WorkingFileStorageLocation<Blob> pWFSL) {

    LOBWorkDoc<Blob> lWorkDoc = pWFSL.createWorkDoc(false);

    UCon lUCon = pRequestContext.getContextUCon().getUCon("GenerateToWFSL");
    try {
      lWorkDoc.open(lUCon);

      try(OutputStream lBlobOS = lWorkDoc.getLOB().setBinaryStream(0)) {
        pGenerator.writeOutput(lBlobOS);
      }
      lWorkDoc.close(lUCon);
    }
    catch (IOException | SQLException e) {
      throw new ExInternal("Failed to open BLOB stream", e);
    }
    finally {
      pRequestContext.getContextUCon().returnUCon(lUCon, "GenerateToWFSL");
    }
  }
}
