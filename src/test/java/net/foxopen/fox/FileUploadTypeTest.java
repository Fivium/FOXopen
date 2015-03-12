package net.foxopen.fox;

import net.foxopen.fox.dom.DOM;
import net.foxopen.fox.ex.ExInternal;
import net.foxopen.fox.ex.ExUploadValidation;
import net.foxopen.fox.filetransfer.UploadInfo;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


public class FileUploadTypeTest {
  public FileUploadTypeTest() {
  }

  private FileUploadType mFileUploadType;

  private static final String BLACKLIST_FILE_UPLOAD_TYPE = "<file-upload-type>\n" +
    "        <name default=\"true\">file</name>\n" +
    "        <disallowed-extension-csv-list>bat, bin, exe</disallowed-extension-csv-list>\n" +
    "        <disallowed-mime-type-csv-list description=\"Executable Files, Encrypted Archives\">application/x-dosexec, application/mac-binhex40, application/octet-stream</disallowed-mime-type-csv-list>\n" +
    "        <max-size-bytes>52428800</max-size-bytes>\n" +
    "      </file-upload-type>";

  private static final String EXTENSION_WHITELIST_FILE_UPLOAD_TYPE = "<file-upload-type>\n" +
    "        <name default=\"true\">pdf</name>\n" +
    "        <allowed-extension-csv-list>pdf</allowed-extension-csv-list>\n" +
    "      </file-upload-type>";

  private static final String MIME_WHITELIST_FILE_UPLOAD_TYPE = "<file-upload-type>\n" +
    "        <name default=\"true\">pdf</name>\n" +
    "        <allowed-mime-type-csv-list>application/pdf</allowed-mime-type-csv-list>\n" +
    "      </file-upload-type>";

  private static final String MIME_TYPE_OCTET_STREAM = "application/octet-stream";
  private static final String MIME_TYPE_PDF = "application/pdf";
  private static final String MIME_TYPE_DOC = "application/msword";
  private static final String MIME_TYPE_PPT = "application/mspowerpoint";

  private UploadInfo getUploadInfo(String pFileName, String pContentType) {
    UploadInfo lUploadInfo;
    lUploadInfo = mock(UploadInfo.class);
    when(lUploadInfo.getHttpContentLength()).thenReturn(500L);
    when(lUploadInfo.getFilename()).thenReturn(pFileName);
    when(lUploadInfo.getBrowserContentType()).thenReturn(pContentType);

    // Use the real methods instead of mock methods for these two calls only
    doCallRealMethod().when(lUploadInfo).setTrueContentType(anyString());
    doCallRealMethod().when(lUploadInfo).getTrueContentType();

    return lUploadInfo;
  }

  /**
   * Gets the first 4KB of a test file.
   * @param pExtension Extension of the desired test file.
   * @return First 4KB of content.
   */
  private byte[] getFileBytes(String pExtension){
    File lFile = new File(this.getClass().getResource("testfiles/upload-test." + pExtension).getPath());
    try {
      return XFUtil.toByteArray(new FileInputStream(lFile), 4096, 4096);
    }
    catch (FileNotFoundException e) {
      throw new ExInternal("Error loading unit test file", e);
    }
  }

  private String testMimeTypeDetectionInternal(String pFileName, String pContentType, String pTestFileType, ExUploadValidation.ValidationErrorType pExepectedError, String pExpectedMime){
    UploadInfo lUploadInfo = getUploadInfo(pFileName, pContentType);
    byte[] lFirstBytes = getFileBytes(pTestFileType);
    String lMimeType = "";
    try {
      mFileUploadType.validateContent(lUploadInfo, lFirstBytes);
      lMimeType = lUploadInfo.getTrueContentType();
    }
    catch (ExUploadValidation e) {
      if(pExepectedError != null){
        lMimeType = lUploadInfo.getTrueContentType();
        assertEquals("Validation error is of expected type", pExepectedError, e.getValidationErrorType());
        assertEquals("Detected mime type is correct", pExpectedMime, lMimeType);
        return lUploadInfo.getMagicContentTypes();
      }
      else {
        throw new ExInternal("Unexpected validation failure: " + e.getValidationErrorType().toString(), e);
      }
    }
    //Fail if an error was expected but did not occur
    assertTrue("No validation error occurred", pExepectedError == null);
    assertEquals("Detected mime type is correct", pExpectedMime, lMimeType);
    return lUploadInfo.getMagicContentTypes();
  }

  @Before
  public void init(){
    FileUploadType.init(new File(this.getClass().getResource("testfiles/mime-types.properties").getPath()), null);
  }

  /**
   * Test that the MIME detector can detect file types which are commonly uploaded to FOX.
   */
  @Test
  public void testMimeTypeDetection_CommonFileTypes() {
    mFileUploadType = FileUploadType.constructFromXML(DOM.createDocumentFromXMLString(BLACKLIST_FILE_UPLOAD_TYPE));

    testMimeTypeDetectionInternal("file.doc", MIME_TYPE_DOC, "doc", null, MIME_TYPE_DOC);
    testMimeTypeDetectionInternal("file.docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document", "docx", null, "application/vnd.openxmlformats-officedocument.wordprocessingml.document");
    testMimeTypeDetectionInternal("file.exe", MIME_TYPE_OCTET_STREAM, "exe", ExUploadValidation.ValidationErrorType.INVALID_CONTENT, MIME_TYPE_OCTET_STREAM);
    testMimeTypeDetectionInternal("file.gif", "image/gif", "gif", null, "image/gif");
    testMimeTypeDetectionInternal("file.htm", "text/html", "htm", null, "text/html");
    testMimeTypeDetectionInternal("file.jpg", "image/jpeg", "doc", null, "image/jpeg");
    testMimeTypeDetectionInternal("file.pdf", MIME_TYPE_PDF, "pdf", null, MIME_TYPE_PDF);
    testMimeTypeDetectionInternal("file.png", "image/png", "png", null, "image/png");
    testMimeTypeDetectionInternal("file.ppt", MIME_TYPE_PPT, "ppt", null, MIME_TYPE_PPT);
    testMimeTypeDetectionInternal("file.txt", "text/plain", "txt", null, "text/plain");
    testMimeTypeDetectionInternal("file.xls", "application/vnd.ms-excel", "xls", null, "application/vnd.ms-excel");
    testMimeTypeDetectionInternal("file.xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "xlsx", null, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
  }

  /**
   * Test that the blacklist rejection works if the majority of reported data about a file conforms to the blacklist
   * specification.
   */
  @Test
  public void testMimeTypeDetection_Blacklist() {

    mFileUploadType = FileUploadType.constructFromXML(DOM.createDocumentFromXMLString(BLACKLIST_FILE_UPLOAD_TYPE));

    //Format: uploaded file name, browser MIME type, actual file type, expected error (or null), expected derived MIME type

    //Legitimate PDF file uploaded - should pass
    testMimeTypeDetectionInternal("file.pdf", MIME_TYPE_PDF, "pdf", null, MIME_TYPE_PDF);

    //Legitimate PDF file uploaded but user's browser doesn't know about PDFs - should pass
    testMimeTypeDetectionInternal("file.pdf",  MIME_TYPE_OCTET_STREAM, "pdf", null, MIME_TYPE_PDF);

    //Legitimate PDF file uploaded but has no file extension - should pass
    testMimeTypeDetectionInternal("file", MIME_TYPE_PDF, "pdf", null, MIME_TYPE_PDF);

    //PDF file uploaded but looks like an EXE based on extension - should fail
    testMimeTypeDetectionInternal("file.exe", MIME_TYPE_OCTET_STREAM, "pdf", ExUploadValidation.ValidationErrorType.INVALID_CONTENT, MIME_TYPE_OCTET_STREAM);

    //File with strange, unrecognised extension uploaded. Should be detected as an octet-steam and fail
    testMimeTypeDetectionInternal("file.unknown", MIME_TYPE_OCTET_STREAM, "unknown", ExUploadValidation.ValidationErrorType.INVALID_CONTENT, MIME_TYPE_OCTET_STREAM);

    //Edge cases

    //"EXE" file uploaded but browser is reporting it as a PDF. This could be a malicious file or it could be because the
    //magic detector has failed to determine the type correctly. Let it through - it will be served back out as a PDF so should
    //never be executed.
    testMimeTypeDetectionInternal("file.pdf", MIME_TYPE_PDF, "exe", null, MIME_TYPE_PDF);

    //File with PDF extension uploaded but browser and magic see an octet stream. Again potentially we're letting an EXE
    //through but it could equally be legitimate.
    testMimeTypeDetectionInternal("file.pdf", MIME_TYPE_OCTET_STREAM, "exe", null, MIME_TYPE_PDF);
  }

  /**
   * Test strict extension whitelist behaviour.
   */
  @Test
  public void testMimeTypeDetection_ExtensionWhitelist() {

    mFileUploadType = FileUploadType.constructFromXML(DOM.createDocumentFromXMLString(EXTENSION_WHITELIST_FILE_UPLOAD_TYPE));

    //Format: uploaded file name, browser MIME type, actual file type, expected error (or null), expected derived MIME type

    //Legitimate PDF file uploaded
    testMimeTypeDetectionInternal("file.pdf", MIME_TYPE_PDF, "pdf", null, MIME_TYPE_PDF);

    //Legitimate PDF file uploaded but user's browser doesn't know about PDFs - should pass
    testMimeTypeDetectionInternal("file.pdf",  MIME_TYPE_OCTET_STREAM, "pdf", null, MIME_TYPE_PDF);

    //Legitimate PDF file uploaded but has no file extension - should fail
    testMimeTypeDetectionInternal("file", MIME_TYPE_PDF, "pdf", ExUploadValidation.ValidationErrorType.UNRECOGNISED_CONTENT, MIME_TYPE_PDF);

    //Legitimate PDF file uploaded but has unrecognised (by browser and FOX) file extension - should fail
    testMimeTypeDetectionInternal("file.unknown", MIME_TYPE_OCTET_STREAM, "pdf", ExUploadValidation.ValidationErrorType.INVALID_CONTENT, MIME_TYPE_OCTET_STREAM);

    //Legitimate PDF file uploaded but has unrecognised (by FOX only) file extension - should fail
    testMimeTypeDetectionInternal("file.unknown", MIME_TYPE_PDF, "pdf", ExUploadValidation.ValidationErrorType.INVALID_CONTENT, MIME_TYPE_PDF);

    //Legitimate Word doc uploaded - should fail
    testMimeTypeDetectionInternal("file.doc", MIME_TYPE_DOC, "doc", ExUploadValidation.ValidationErrorType.INVALID_CONTENT, MIME_TYPE_DOC);

    //Edge case. "EXE" file uploaded but browser is reporting it as a PDF. This could be a malicious file or it could be because the
    //magic detector has failed to determine the type correctly. Let it through - it will be served back out as a PDF so should
    //never be executed.
    testMimeTypeDetectionInternal("file.pdf", MIME_TYPE_PDF, "exe", null, MIME_TYPE_PDF);

    //File with PDF extension uploaded but browser and magic see an octet stream. Again potentially we're letting an EXE
    //through but it could equally be legitimate.
    testMimeTypeDetectionInternal("file.pdf", MIME_TYPE_OCTET_STREAM, "exe", null, MIME_TYPE_PDF);
  }

  /**
   * Test slightly more lenient MIME type whitelist behaviour.
   */
  @Test
  public void testMimeTypeDetection_MimeTypeWhitelist() {

    mFileUploadType = FileUploadType.constructFromXML(DOM.createDocumentFromXMLString(MIME_WHITELIST_FILE_UPLOAD_TYPE));

    //Format: uploaded file name, browser MIME type, actual file type, expected error (or null), expected derived MIME type

    //Legitimate PDF file uploaded
    testMimeTypeDetectionInternal("file.pdf", MIME_TYPE_PDF, "pdf", null, MIME_TYPE_PDF);

    //Legitimate PDF file uploaded but user's browser doesn't know about PDFs - should pass
    testMimeTypeDetectionInternal("file.pdf",  MIME_TYPE_OCTET_STREAM, "pdf", null, MIME_TYPE_PDF);

    //Legitimate PDF file uploaded but has no file extension - OK as no extension whitelist specified and MIME type conforms
    testMimeTypeDetectionInternal("file", MIME_TYPE_PDF, "pdf", null, MIME_TYPE_PDF);

    //Legitimate PDF file uploaded but has unrecognised (by browser and FOX) file extension - should fail
    testMimeTypeDetectionInternal("file.unknown", MIME_TYPE_OCTET_STREAM, "pdf", ExUploadValidation.ValidationErrorType.UNRECOGNISED_CONTENT, MIME_TYPE_OCTET_STREAM);

    //Legitimate PDF file uploaded but has unrecognised (by FOX only) file extension - should fail
    testMimeTypeDetectionInternal("file.unknown", MIME_TYPE_PDF, "pdf", ExUploadValidation.ValidationErrorType.UNRECOGNISED_CONTENT, MIME_TYPE_PDF);

    //Legitimate Word doc uploaded - should fail
    testMimeTypeDetectionInternal("file.doc", MIME_TYPE_DOC, "doc", ExUploadValidation.ValidationErrorType.INVALID_CONTENT, MIME_TYPE_DOC);

    //Edge case. "EXE" file uploaded but browser is reporting it as a PDF. This could be a malicious file or it could be because the
    //magic detector has failed to determine the type correctly. Let it through - it will be served back out as a PDF so should
    //never be executed.
    testMimeTypeDetectionInternal("file.pdf", MIME_TYPE_PDF, "exe", null, MIME_TYPE_PDF);

    //File with PDF extension uploaded but browser and magic see an octet stream. Again potentially we're letting an EXE
    //through but it could equally be legitimate.
    testMimeTypeDetectionInternal("file.pdf", MIME_TYPE_OCTET_STREAM, "exe", null, MIME_TYPE_PDF);
  }

  /**
   * Test behaviour is correct when multiple MIME types can be resolved from a single file extension.
   */
  @Test
  public void testMimeTypeDetection_MultiplePossibleMimeTypes() {

    //Allow all non-exe files
    mFileUploadType = FileUploadType.constructFromXML(DOM.createDocumentFromXMLString(BLACKLIST_FILE_UPLOAD_TYPE));

    //3 potential mappings in the extension map - should use the user agent to narrow down choice
    testMimeTypeDetectionInternal("file.ppt", "application/mspowerpoint", "ppt", null, "application/mspowerpoint");
    testMimeTypeDetectionInternal("file.ppt", "application/powerpoint", "ppt", null, "application/powerpoint");
    testMimeTypeDetectionInternal("file.ppt", "application/vnd.ms-powerpoint", "ppt", null, "application/vnd.ms-powerpoint");

    //No decent extension or browser MIME but the magic detects a PDF - this should be OK
    testMimeTypeDetectionInternal("file", MIME_TYPE_OCTET_STREAM, "pdf", null, MIME_TYPE_PDF);

    //No extension or browser MIME and the magic detects an exe - this should fail
    testMimeTypeDetectionInternal("file", MIME_TYPE_OCTET_STREAM, "exe", ExUploadValidation.ValidationErrorType.UNRECOGNISED_CONTENT, MIME_TYPE_OCTET_STREAM);
  }

}
