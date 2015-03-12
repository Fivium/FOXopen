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
package net.foxopen.fox;


import com.google.common.base.Joiner;
import eu.medsea.mimeutil.MimeType;
import eu.medsea.mimeutil.MimeUtil;
import net.foxopen.fox.dom.DOM;
import net.foxopen.fox.ex.ExCardinality;
import net.foxopen.fox.ex.ExInternal;
import net.foxopen.fox.ex.ExUploadValidation;
import net.foxopen.fox.filetransfer.UploadInfo;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public class FileUploadType{

  public static final String MIME_TYPE_AUX_CONFIG_MNEM = "mime-types.properties";

  private final String mName;
  private final String mDescription;
  private final Set<String> mAllowedExtensions;
  private final Set<String> mDisallowedExtensions;
  private final Set<String> mAllowedMimeTypes = new HashSet<String>();
  private final Set<String> mDisallowedMimeTypes = new HashSet<String>();
  private final String mAllowedMimeTypesDesc;
  private final String mDisallowedMimeTypesDesc;
  private final long mMinSize;
  private final long mMaxSize;

  private final boolean mUseWhitelistLogic;

  private static final String INVALID_EXTENSION_MESSAGE = "the file's extension is not allowed";
  private static final String INVALID_CONTENT_MESSAGE = "the file you are uploading is not valid";
  private static final String UNRECOGNISED_CONTENT_MESSAGE = "the file's type could not be determined";

  private static final String SIZE_TOO_SMALL = "the file is too small";
  private static final String SIZE_TOO_LARGE = "the file is too large";

  //100kb of leeway when judging file size
  private static final int SIZE_CHECK_BUFFER_BYTES = 100 * 1000;

  private static final String SELECT_RESOURCE_MASTER_FILENAME = "GetEnvironmentAuxiliaryConfig.sql";

  /**
   * Mapping of file extensions (lower case) to one or more MIME types. Access to this map should be synchronized.
   */
  private static Map<String, Set<String>> gExtensionToMimeTypesMap = new HashMap<String, Set<String>>();

  private static int gAdditionalMimeTypeCount = 0;

  public static int getAdditionalMimeTypeCount() {
    return gAdditionalMimeTypeCount;
  }

  /**
   * Populates the extension Map from a BufferedReader. An exception will be thrown if the stream contents is invalid.
   * @param pReader Reader for the defintion file.
   * @return Number of extension mappings read.
   */
  private static int readExtensionDefinitions(BufferedReader pReader){
    String lLine;
    int lCount = 0;
    try {
      while((lLine = pReader.readLine()) != null){
        //skip nulls/comments
        if(lLine.trim().length() == 0 || lLine.trim().charAt(0) == '#'){
          continue;
        }
        if(lLine.indexOf('=') == -1){
          throw new ExInternal("Invalid line syntax: format should be 'extension=mime-type, mime-type2\\n'. Line contents: " + lLine);
        }
        String lExtension = lLine.substring(0,lLine.indexOf('=')).trim();
        String lMimes = lLine.substring(lLine.indexOf('=')+1).trim();

        //Validate the line
        if(XFUtil.isNull(lExtension)){
          throw new ExInternal("Invalid line syntax: missing extension before '='. Line contents: " + lLine);
        }
        else if(XFUtil.isNull(lMimes)){
          throw new ExInternal("Invalid line syntax: missing MIME CSV list after '='. Line contents: " + lLine);
        }

        gExtensionToMimeTypesMap.put(lExtension.toLowerCase(), StringUtil.commaDelimitedListToSet(lMimes));
        lCount++;
      }
    }
    catch (Throwable e) {
      throw new ExInternal("Invalid mime-type.properties file", e);
    }

    return lCount;
  }

  /**
   * Initialises the static extension to MIME type Map for this class..
   * @param pMimeTypePropertiesFile The default property File included in the FOX build.
   * @param pOptionalAdditionalReader Optional - if specified, used to retrieve augmented file extension defintions from the
   *                                  aux config table
   */
  public static void init(File pMimeTypePropertiesFile, Reader pOptionalAdditionalReader){

    //Sync on the map to stop new FileUploadTypes being constructed as it is populated
    synchronized(gExtensionToMimeTypesMap){

      gExtensionToMimeTypesMap.clear();

      //register magic MIME detector
      MimeUtil.registerMimeDetector("eu.medsea.mimeutil.detector.MagicMimeMimeDetectorFixedFile");

      //Open the extension->MIME type properties file
      BufferedReader lReader;
      try {
        lReader = new BufferedReader(new InputStreamReader(new FileInputStream(pMimeTypePropertiesFile)));
      }
      catch (FileNotFoundException e) {
        throw new ExInternal("mime-types.properties file not found", e);
      }

      //Read definitions in from the built-in file.
      readExtensionDefinitions(lReader);

      //If there's a row defined read each line from the DB CLOB
      if(pOptionalAdditionalReader != null) {
        lReader = new BufferedReader(pOptionalAdditionalReader);
        gAdditionalMimeTypeCount = readExtensionDefinitions(lReader);
        try {
          lReader.close();
        }
        catch (IOException e) {
          throw new ExInternal("Failed to read additional mime types from database", e);
        }
      }
    }
  }

  /**
   * Deserialise a FileUploadType from an XML definition.
   * @param pXML Root element of the definition.
   * @return A new FileUploadType.
   */
  public static FileUploadType constructFromXML(DOM pXML){

    String lName = pXML.get1SNoEx("name");
    String lDescription = pXML.get1EOrNull("name").getAttrOrNull("description");
    String lCsvAllowedExtensions = pXML.get1SNoEx("allowed-extension-csv-list");
    String lCsvDisallowedExtensions = pXML.get1SNoEx("disallowed-extension-csv-list");
    String lCsvAllowedMimeTypes = pXML.get1SNoEx("allowed-mime-type-csv-list");
    String lCsvDisallowedMimeTypes = pXML.get1SNoEx("disallowed-mime-type-csv-list");

    String lAllowedMimeDesc = null;
    String lDisallowedMimeDesc = null;
    try{
      lAllowedMimeDesc = pXML.get1E("allowed-mime-type-csv-list").getAttr("description");
    }
    catch (ExCardinality ignore) {}

    try{
      lDisallowedMimeDesc = pXML.get1E("disallowed-mime-type-csv-list").getAttr("description");
    }
    catch (ExCardinality ignore) {}

    String lMinSizeBytes = pXML.get1SNoEx("min-size-bytes");
    String lMaxSizeBytes = pXML.get1SNoEx("max-size-bytes");

    Set<String> lAllowedExtSet = null;
    Set<String> lDisallowedExtSet = null;
    Set <String> lAllowedMimeSet = null;
    Set <String> lDisallowedMimeSet = null;
    long lMinSize;
    long lMaxSize;

    if(XFUtil.isNull(lName)) {
      throw new ExInternal("Error constructing app: name required for each file upload type.");
    }

   //Validate and parse
    if(!XFUtil.isNull(lCsvAllowedExtensions) && !XFUtil.isNull(lCsvDisallowedExtensions)){
      throw new ExInternal("Error constructing app: cannot have both allowed and disallowed extension lists for file upload type " + lName);
    }
    else if(!XFUtil.isNull(lCsvAllowedExtensions)){
      lAllowedExtSet = StringUtil.commaDelimitedListToSet(lCsvAllowedExtensions.toLowerCase());
    }
    else if(!XFUtil.isNull(lCsvDisallowedExtensions)){
      lDisallowedExtSet = StringUtil.commaDelimitedListToSet(lCsvDisallowedExtensions.toLowerCase());
    }

    if(!XFUtil.isNull(lCsvAllowedMimeTypes) && !XFUtil.isNull(lCsvDisallowedMimeTypes)){
      throw new ExInternal("Error constructing app: cannot have both allowed and disallowed mime type lists for file upload type " + lName);
    }
    else if(!XFUtil.isNull(lCsvAllowedMimeTypes)){
      lAllowedMimeSet = StringUtil.commaDelimitedListToSet(lCsvAllowedMimeTypes.toLowerCase());
      validateMimeTypes(lAllowedMimeSet);
    }
    else if(!XFUtil.isNull(lCsvDisallowedMimeTypes)){
      lDisallowedMimeSet = StringUtil.commaDelimitedListToSet(lCsvDisallowedMimeTypes.toLowerCase());
      validateMimeTypes(lDisallowedMimeSet);
    }

    //Parse min/max byte sizes
    try{
      if(!XFUtil.isNull(lMinSizeBytes)){
        lMinSize = Long.parseLong(lMinSizeBytes);
      }
      else {
        lMinSize = -1;
      }
      if(!XFUtil.isNull(lMaxSizeBytes)){
        lMaxSize = Long.parseLong(lMaxSizeBytes);
      }
      else {
        lMaxSize = -1;
      }
    }
    catch (NumberFormatException e){
      throw new ExInternal("Error constructing app: invalid number specified for min/max bytes in file upload type " + lName);
    }

    if(lMinSize >= lMaxSize && (lMinSize != -1 || lMaxSize != -1)){
      throw new ExInternal("Error constructing app: min/max bytes inequality problem in file upload type " + lName);
    }

    return new FileUploadType(lName, lDescription, lAllowedExtSet, lDisallowedExtSet, lAllowedMimeSet, lDisallowedMimeSet, lAllowedMimeDesc, lDisallowedMimeDesc,  lMinSize, lMaxSize);

  }

  private FileUploadType(String pName, String pDescription, Set<String> pAllowedExtensions, Set<String> pDisallowedExtensions,
                        Set<String> pAllowedMimeTypes, Set<String> pDisallowedMimeTypes, String pAllowedMimeDesc,
                        String pDisallowedMimeDesc, long pMinSize, long pMaxSize){

    mName = pName;
    mDescription = pDescription;
    mAllowedExtensions = pAllowedExtensions;
    mDisallowedExtensions = pDisallowedExtensions;
    if(pAllowedMimeTypes != null) mAllowedMimeTypes.addAll(pAllowedMimeTypes);
    if(pDisallowedMimeTypes != null) mDisallowedMimeTypes.addAll(pDisallowedMimeTypes);
    mAllowedMimeTypesDesc = pAllowedMimeDesc;
    mDisallowedMimeTypesDesc = pDisallowedMimeDesc;
    mMinSize = pMinSize;
    mMaxSize = pMaxSize;

    synchronized(gExtensionToMimeTypesMap){
      //convert file extensions to mime types and add to appropriate sets
      if(mAllowedExtensions != null){
        for(String lExt : mAllowedExtensions){
          Set<String> lSet;
          if((lSet = gExtensionToMimeTypesMap.get(lExt)) != null){
            mAllowedMimeTypes.addAll(lSet);
          }
          else {
            throw new ExInternal("Could not construct File Upload Type " + pName + ". No extension to mime type mapping found for extension: " + lExt);
          }
        }
      }

      if(mDisallowedExtensions != null){
          for(String lExt : mDisallowedExtensions){
          Set<String> lSet;
          if((lSet = gExtensionToMimeTypesMap.get(lExt)) != null){
            mDisallowedMimeTypes.addAll(lSet);
          }else{
            throw new ExInternal("Could not construct File Upload Type " + pName + ".  No extension to mime type mapping found for extension: " + lExt);
          }
        }
      }
    }

    //TODO FOX5 - error on conflicting whitelist/blacklist definitions - logic has changed so they are now incompatible.
    //However no change made yet as all engines will require reconfiguring.

    //validate sets are distinct
    if(containsAny(mAllowedMimeTypes, mDisallowedMimeTypes)){
      String lDupes = "";
      for(Iterator it = mAllowedMimeTypes.iterator(); it.hasNext();){
        String lMime = (String) it.next();
        if(mDisallowedMimeTypes.contains(lMime)){
          lDupes += lMime + ",";
        }
      }
      throw new ExInternal("Could not construct File Upload Type " + pName + ". Allowed and disallowed MIME type sets are not distinct. The following exist in the intersection: " + lDupes
        + " Check the mime-types.properties file in FOX config - an extension provided may be yielding an unexpected MIME type.");
    }

    //Work out which validation logic to use (if any allowed mime types are specified, this indicates a whitelist)
    mUseWhitelistLogic = !mAllowedMimeTypes.isEmpty();
  }

  /**
   * Validates the content of a File Upload against this definition. Content length and type is determined and validated.
   * The UploadInfo object is updated with the determined Content Type.
   * @param pUploadInfo File Upload to be validated
   * @param pFirstPacket Variable length byte array containing the first n bytes of the file. This is used for magic MIME detection/
   * @throws ExUploadValidation If validation fails. The thrown exception will contain a more detailed reason for the
   * validation failure.
   */
  public void validateContent(UploadInfo pUploadInfo, byte[] pFirstPacket)
  throws ExUploadValidation {

    //Size Validation
    validateSize(pUploadInfo.getHttpContentLength());

    //Content type validation
    String lExtension = pUploadInfo.getFilename().indexOf('.') != -1 ? pUploadInfo.getFilename().substring(pUploadInfo.getFilename().lastIndexOf('.')+1) : "";
    /*
     * Magic MIME type check
     *
     * The first UploadWorkItem.BYTE_READ_QUANTITY bytes of an upload are passed to
     * the mimeutil MIME type detector (See http://sourceforge.net/projects/mime-util)
     * The magic.mime file syntax specifies fixed addresses of bytes in the file to
     * inspect, so we know that we have passed  enough of the file for the detection
     * to work. Note the custom implementation of the MagicMimeDetector - this overloads
     * the behaviour of the supplied class, which looks at various filesystem/environment
     * variable locations to find magic.mime files.
     *
     */
    Collection lMimeTypes = new MimeUtil().getMimeTypes(pFirstPacket);

    Set<String> lMimeTypeStrings = new HashSet<String>();

    //log MIME type data
    for(Iterator it = lMimeTypes.iterator(); it.hasNext();){
      MimeType lMimeType = (MimeType) it.next();
      if(lMimeType != null) {
        pUploadInfo.addMagicContentType(lMimeType.toString());
        lMimeTypeStrings.add(lMimeType.toString());
      }
    }

    try {
      String lMime = getAndValidateMimeType(lExtension, pUploadInfo.getBrowserContentType(), lMimeTypeStrings);
      pUploadInfo.setTrueContentType(lMime);
    }
    catch (ExUploadValidation ex) {
      pUploadInfo.setTrueContentType(ex.getMimeType());
      throw ex;
    }
  }

  /**
   * Validates the size of a file against this definition. If the file is too large or small, an exception is thrown.
   * @param pSize Size in bytes.
   * @throws ExUploadValidation If the size is invalid.
   */
  public void validateSize(long pSize)
  throws ExUploadValidation {
    if(mMinSize != -1 && pSize < mMinSize - SIZE_CHECK_BUFFER_BYTES){
      throw new ExUploadValidation(SIZE_TOO_SMALL, ExUploadValidation.ValidationErrorType.FILE_SIZE);
    }

    if(mMaxSize != -1 && pSize > mMaxSize + SIZE_CHECK_BUFFER_BYTES){
      throw new ExUploadValidation(SIZE_TOO_LARGE, ExUploadValidation.ValidationErrorType.FILE_SIZE);
    }

  }

  /**
   * Determines the content type of a file and whether it is valid according to this definition.<br><br>
   *
   * Whitelist logic checks the file extension (if an extension whitelist is defined) or content type (if only a content
   * type whitelist  is defined). Content type is derived from either the extension to content type mapping or agreement
   * between the browser-reported content type and the magically detected content type.<br><br>
   *
   * Blacklist logic is more lenient and checks the file extension and content type (derived in the same way) against
   * a blacklist. If the definition contains both whitelist and blacklist markup, the whitelist logic is used.<br><br>
   *
   * After validating that the contents is valid its 'true' content type is then derived. Precedence is given to
   * the file extension mapping, then the browser MIME if it is acceptable, then finally the magic MIME if that
   * is acceptable.<br><br>
   *
   * Note that in both cases FOX may let unacceptable files through if the user changes the extension or browser MIME
   * type. This is considered preferrable to rejecting files which cannot be recognised (i.e. by magic MIME).
   *
   * @param pFileExtension Extension of the file (can be empty or null).
   * @param pBrowserMime The MIME type of the file as reported by the user's browser.
   * @param pMagicMimeSet A set of fully-qualified MIME type strings.
   * @return The determined content type of the file.
   * @throws ExUploadValidation If the content type is not valid.
   */
  public String getAndValidateMimeType(String pFileExtension, String pBrowserMime, Collection<String> pMagicMimeSet)
  throws ExUploadValidation {

    pFileExtension = pFileExtension.toLowerCase();
    //Clone this set so consumers aren't surprised by its contents changing
    pMagicMimeSet = new HashSet<String>(pMagicMimeSet);
    Set<String> lFileExtensionMimes = null;

    boolean lBrowserMimeMatchesMagicMime = pMagicMimeSet.contains(pBrowserMime);

    if(!XFUtil.isNull(pFileExtension)){
      synchronized(gExtensionToMimeTypesMap){
        lFileExtensionMimes = gExtensionToMimeTypesMap.get(pFileExtension);
      }
    }

    //Validate the file contents

    if(mUseWhitelistLogic){
      //WHITELIST LOGIC

      //Step 1 - are we validating against a whitelist of file extensions? If so, check the extension is in the list.
      if(mAllowedExtensions != null){
        if(XFUtil.isNull(pFileExtension)){
          //No file extension but there's a file extension whitelist - don't allow the upload
          throw new ExUploadValidation(UNRECOGNISED_CONTENT_MESSAGE + "Uploaded files must have a valid file extension. ", ExUploadValidation.ValidationErrorType.UNRECOGNISED_CONTENT, pBrowserMime);
        }
        else if(!mAllowedExtensions.contains(pFileExtension)){
          //The file extensin is not in the whitelist
          throw new ExUploadValidation(INVALID_EXTENSION_MESSAGE, ExUploadValidation.ValidationErrorType.INVALID_CONTENT, pBrowserMime);
        }
      }

      //Step 2 - validate against the MIME type whitelist.
      if(XFUtil.isNull(pFileExtension)){
        //No file extension available.
        if(lBrowserMimeMatchesMagicMime){
          //If the magic MIME matches the browser MIME it is likely to be reliable - check on the whitelist.
          if(!mAllowedMimeTypes.contains(pBrowserMime)){
            throw new ExUploadValidation(INVALID_CONTENT_MESSAGE, ExUploadValidation.ValidationErrorType.INVALID_CONTENT, pBrowserMime);
          }
        }
        else {
          //We can't determine enough about the file to accept it.
          throw new ExUploadValidation(UNRECOGNISED_CONTENT_MESSAGE, ExUploadValidation.ValidationErrorType.UNRECOGNISED_CONTENT, pBrowserMime);
        }
      }
      else if(lFileExtensionMimes == null){
        //No MIME type definitions for this file extension - FOX doesn't know about it.
        throw new ExUploadValidation(UNRECOGNISED_CONTENT_MESSAGE, ExUploadValidation.ValidationErrorType.UNRECOGNISED_CONTENT, pBrowserMime);
      }
      else {
        //We know about the file extension - is at least one of its MIME types in this whitelist?
        if(!containsAny(mAllowedMimeTypes, lFileExtensionMimes)){
          throw new ExUploadValidation(INVALID_CONTENT_MESSAGE, ExUploadValidation.ValidationErrorType.INVALID_CONTENT, pBrowserMime);
        }
      }

    }
    else {
      //BLACKLIST LOGIC

      //Step 1 - are we validating against a blacklist of file extensions? If so, check the extension is not in the list.
      if(mDisallowedExtensions != null && mDisallowedExtensions.contains(pFileExtension)){
        throw new ExUploadValidation(INVALID_EXTENSION_MESSAGE, ExUploadValidation.ValidationErrorType.INVALID_CONTENT, pBrowserMime);
      }

      //Step 2 - validate against the MIME type blacklist.
      if(lFileExtensionMimes == null){
        //No MIME type definitions for this file extension - FOX doesn't know about it or the file had no extension.
        //If the browser MIME agrees with the magic MIME then we can use this MIME to check the blacklist.
        if(lBrowserMimeMatchesMagicMime && mDisallowedMimeTypes.contains(pBrowserMime)){
          throw new ExUploadValidation(INVALID_CONTENT_MESSAGE, ExUploadValidation.ValidationErrorType.INVALID_CONTENT, pBrowserMime);
        }
        //Otherwise, the file is accepted
      }
      else {
        //We know about the file extension - check none of its MIME types are in the blacklist.
        if(containsAny(mDisallowedMimeTypes, lFileExtensionMimes)){
          throw new ExUploadValidation(INVALID_CONTENT_MESSAGE, ExUploadValidation.ValidationErrorType.INVALID_CONTENT, pBrowserMime);
        }
      }
    }

    //Determine the 'true' MIME type

    if(lFileExtensionMimes != null && lFileExtensionMimes.contains(pBrowserMime)){
      //If the browser and extension MIMEs match then this is a good bet.
      return pBrowserMime;
    }
    else if(lFileExtensionMimes != null && containsAny(pMagicMimeSet, lFileExtensionMimes)){
      //Intersect the extension MIME set with the magic MIME set and pick one.
      lFileExtensionMimes.retainAll(pMagicMimeSet);
      return lFileExtensionMimes.iterator().next();
    }
    else if(lFileExtensionMimes != null){
      //Otherwise use one of the file extension mappings at random.
      return lFileExtensionMimes.iterator().next();
    }
    else if((mUseWhitelistLogic && mAllowedMimeTypes.contains(pBrowserMime)) || (!mUseWhitelistLogic && !mDisallowedMimeTypes.contains(pBrowserMime))){
      //No file extension mappings avaialable. The browser MIME is acceptable according to the white/black list then use that
      return pBrowserMime;
    }
    else if(mUseWhitelistLogic){
      pMagicMimeSet.retainAll(mAllowedMimeTypes);
      if(!pMagicMimeSet.isEmpty()){
        return pMagicMimeSet.iterator().next().toString();
      }
    }
    else if (!mUseWhitelistLogic){
      pMagicMimeSet.removeAll(mDisallowedMimeTypes);
      if(!pMagicMimeSet.isEmpty()){
        return pMagicMimeSet.iterator().next().toString();
      }
    }

    //MIME detector algorithm has failed to determine the type
    throw new ExUploadValidation(UNRECOGNISED_CONTENT_MESSAGE + " (MIME type inconsistency) ", ExUploadValidation.ValidationErrorType.UNRECOGNISED_CONTENT, pBrowserMime);
  }

  /**
   *
   * @param pTarget
   * @param pSource
   * @return true if pTarget contains any element from pSource, false else
   */
  public static boolean containsAny(Collection pTarget, Collection pSource){
    for(Iterator it = pSource.iterator(); it.hasNext();){
      if(pTarget.contains(it.next()))
        return true;
    }
    return false;
  }

  public String getReadableExtensionRestrictionList(){
    if(mAllowedExtensions != null && !mAllowedExtensions.isEmpty()){
      return "The following file extensions are allowed: " + Joiner.on(", ").join(mAllowedExtensions);
    }
    else if(mDisallowedExtensions != null && !mDisallowedExtensions.isEmpty()) {
      return "The following file extensions are not allowed: " + Joiner.on(", ").join(mDisallowedExtensions);
    }
    else  {
      return "";
    }
  }

  public String getReadableMimeTypeRestrictionList(){
    if(!XFUtil.isNull(mAllowedMimeTypesDesc)){
      return "The following file types are allowed: " + mAllowedMimeTypesDesc;
    }
    else if (!XFUtil.isNull(mDisallowedMimeTypesDesc)){
      return "The following file types are not allowed: " + mDisallowedMimeTypesDesc;
    }
    else {
      return null;
    }
  }

  public String getReadableMinSize(){
    return (mMinSize != -1) ? readableSize(mMinSize) : null;
  }

  public String getReadableMaxSize(){
    return (mMaxSize != -1) ? readableSize(mMaxSize) : null;
  }

  public String getReadableSummaryDescription() {
    return mDescription;
  }

  private static String readableSize(long pSize){
    String[] lUnits = new String[]{"b","kb","mb","gb","tb"};
    long lSize = pSize;

    for(int i=0; i < lUnits.length; i++){
      if(lSize <= 1024){
        return lSize + lUnits[i];
      } else {
        lSize /= 1024;
      }
    }
    return "?";
  }

  private static void validateMimeTypes(Set pSet){
    for(Iterator it = pSet.iterator(); it.hasNext();){
      String lMime = (String) it.next();
      if(!MimeUtil.isMimeTypeKnown(lMime)){
        throw new ExInternal("Error constructing app: The mime type " + lMime + " cannot currently be recognised by FOX.");
      }
    }
  }

  public String getName() {
    return mName;
  }

  public Set<String> getAllowedExtensions() {
    return mAllowedExtensions == null ? Collections.<String>emptySet() : Collections.unmodifiableSet(mAllowedExtensions);
  }

  public Set<String> getDisallowedExtensions() {
    return mDisallowedExtensions == null ? Collections.<String>emptySet() : Collections.unmodifiableSet(mDisallowedExtensions);
  }
}
