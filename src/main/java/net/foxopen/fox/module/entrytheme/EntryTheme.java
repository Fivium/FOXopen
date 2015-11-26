package net.foxopen.fox.module.entrytheme;

import net.foxopen.fox.XFUtil;
import net.foxopen.fox.command.XDoCommandList;
import net.foxopen.fox.dom.DOM;
import net.foxopen.fox.dom.DOMList;
import net.foxopen.fox.dom.SchemaUtils;
import net.foxopen.fox.entrypoint.auth.AuthType;
import net.foxopen.fox.entrypoint.auth.AuthenticationType;
import net.foxopen.fox.ex.ExCardinality;
import net.foxopen.fox.ex.ExDoSyntax;
import net.foxopen.fox.ex.ExInternal;
import net.foxopen.fox.ex.ExModule;
import net.foxopen.fox.ex.ExUserRequest;
import net.foxopen.fox.ex.ExValidation;
import net.foxopen.fox.module.Mod;
import net.foxopen.fox.module.SyncMode;
import net.foxopen.fox.module.Validatable;
import net.foxopen.fox.thread.storage.DataDOMStorageLocation;
import net.foxopen.fox.track.Track;
import net.foxopen.fox.track.Trackable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;


public final class EntryTheme
implements Trackable, Validatable {

  //TODO NP/PN - convert to enums
  public static final String gTypeServiceRPC = "service-rpc";
  public static final String gTypeServiceDocument = "service-document";
  public static final String gTypeInternal = "internal";
  public static final String gTypeExternal = "external";

  private static final Set<String> VALID_THEME_TYPES = new HashSet<>();
  static {
    VALID_THEME_TYPES.add(gTypeServiceRPC);
    VALID_THEME_TYPES.add(gTypeServiceDocument);
    VALID_THEME_TYPES.add(gTypeInternal);
    VALID_THEME_TYPES.add(gTypeExternal);
  }

  private static final String SYNC_MODE_ATTR_NAME = "synchronisation-mode";
  private static final String VALIDATION_ENABLED_ATTR_NAME = "validation-enabled";

  private final Mod mModule;
  private final String mThemeName;
  private final String mAttachXPath;
  private final String mStateName;
  private final XDoCommandList mXDo;
  private final XDoCommandList mBeforeEntryXDo;
  private final List<DataDOMStorageLocation> mStorageLocationList;
  private final DataDOMStorageLocation mDefaultStorageLocation;
  private final Map<String, SyncMode> mStorageLocationNamesToSyncModes;
  private final Map<String, Boolean> mStorageLocationValidationFlags;
  private final String mType;
  private final boolean mPublished;
  private final EntryThemeSecurity mEntryThemeSecurity;
  private final Map<String, ThemeParam> mHeaderParamsMap = new HashMap<>();
  private final Map<String, ThemeParam> mParamsMap = new HashMap<>();
  private final List<ThemeParam> mParamsArray = new ArrayList<>();
  private final Map<String, ThemeParam> mReturnMap = new HashMap<>();
  private final List<ThemeParam> mReturnArray = new ArrayList<>();
  private final Map<String, ThemeParam> mMandParamsMap = new HashMap<>();
  private final Map<String, ThemeParam> mMandReturnMap = new HashMap<>();
  private final boolean mIsWebServiceOperation;
  private final AuthenticationType mAuthenticationType;
  private final boolean mIsThreadPersistent;
  private final boolean mDefaultEntryTheme;
  private final boolean mAllowedPasswordExpiredAccess;

  public EntryTheme(Mod pModule, DOM pEntryThemeDOM)
  throws ExModule, ExDoSyntax, ExInternal {
    // Set basic theme attributes
    mModule = pModule;

    // Set theme name
    mThemeName = pEntryThemeDOM.getAttr("name");
    if(mThemeName.length()==0){
      throw new ExModule("Bad entry-theme name (must not be empty string)", pEntryThemeDOM);
    }

    try {
      // Default the module type to internal
      mType = XFUtil.nvl(pEntryThemeDOM.getAttr("type"), gTypeInternal);
      if(!VALID_THEME_TYPES.contains(mType)) {
        throw new ExModule("Bad entry-theme type " + mType, pEntryThemeDOM);
      }

      // Set boolean so consumers can tell that we're acting as a web service operation
      mIsWebServiceOperation = (mType.equals(gTypeServiceRPC) || mType.equals(gTypeServiceDocument));

      // Default to unpublished
      mPublished = Boolean.valueOf(XFUtil.nvl(pEntryThemeDOM.getAttr("published"), "false"));

      // Default to not persistent
      mIsThreadPersistent = Boolean.valueOf(XFUtil.nvl(pEntryThemeDOM.getAttr("persistent-thread"), "false"));

      // Default to not allowed
      mAllowedPasswordExpiredAccess = Boolean.valueOf(XFUtil.nvl(pEntryThemeDOM.getAttr("allow-expired-access"), "false"));

      // Default to HTTP authentication for web services or portal otherwise
      String lAuthenticationType = pEntryThemeDOM.getAttr("authentication-type");
      try {
        if (!XFUtil.isNull(lAuthenticationType)) {
          mAuthenticationType = AuthenticationType.fromString(lAuthenticationType);
        }
        else {
          mAuthenticationType = (mIsWebServiceOperation ? AuthenticationType.HTTP_AUTH_BASIC : AuthenticationType.FOX_COMMAND);
        }
      }
      catch (ExInternal ex) {
        throw new ExModule("Bad entry-theme authentication type " + lAuthenticationType, pEntryThemeDOM, ex);
      }

      DOM lSecurityDOM = pEntryThemeDOM.get1EOrNull("fm:security");
      if(lSecurityDOM == null) {
        mEntryThemeSecurity = DefaultEntryThemeSecurity.instance();
      }
      else {
        mEntryThemeSecurity = DefinedEntryThemeSecurity.create(lSecurityDOM, pModule);
      }

      // Determine if this is the default entry theme
      mDefaultEntryTheme = Boolean.valueOf(XFUtil.nvl(pEntryThemeDOM.getAttr("default"), "false"));

      // Get attach point
      try {
        mAttachXPath = pEntryThemeDOM.get1S("fm:attach");
      }
      catch(ExCardinality x) {
        throw new ExModule("Bad entry-theme attach point", pEntryThemeDOM,x);
      }

      // Get state
      try {
        mStateName = pEntryThemeDOM.get1S("fm:state");
      }
      catch(ExCardinality x) {
        throw new ExModule("Bad entry-theme state", pEntryThemeDOM,x);
      }

      //Parse the storage location definition (may be in a list)
      StorageLocationParseResult lParseResult = parseStorageLocationDefinition(pEntryThemeDOM);
      mStorageLocationList = lParseResult.getStorageLocationList();
      mDefaultStorageLocation = lParseResult.getDefaultStorageLocation();
      mStorageLocationNamesToSyncModes = lParseResult.getStorageLocationSyncModes();
      mStorageLocationValidationFlags = lParseResult.getStorageLocationValidationFlags();

      // Look up "do" actions
      DOM lDoBlock;
      try {
        lDoBlock = pEntryThemeDOM.get1E("fm:do");
      }
      catch (ExCardinality x) {
        throw new ExModule("Bad entry-theme do", pEntryThemeDOM,x);
      }
      mXDo = new XDoCommandList(pModule, lDoBlock);

      //Parse before entry do block if specified
      DOM lBeforeEntryDoBlock = pEntryThemeDOM.get1EOrNull("fm:before-entry");
      if(lBeforeEntryDoBlock != null) {
        mBeforeEntryXDo = new XDoCommandList(pModule, lBeforeEntryDoBlock);
      }
      else {
        mBeforeEntryXDo = XDoCommandList.emptyCommandList();
      }

      //TODO subclass for web service entry themes

      // Retrieve the storage location root element name
      String lStoreLocationElemName = mDefaultStorageLocation.getNewDocRootElementName();

      // Parse module parameters
      DOMList lParamsList = pEntryThemeDOM.getUL("fm:param-list/fm:param");
      for (int i = 0; i < lParamsList.getLength(); i++) {
        ThemeParam lThemeParam = new ThemeParam(lParamsList.item(i), pModule, lStoreLocationElemName);
        if (!mParamsMap.containsKey(lThemeParam.getName())) {
          mParamsMap.put(lThemeParam.getName(), lThemeParam);
          if (lThemeParam.isMand()) {
            mMandParamsMap.put(lThemeParam.getName(), lThemeParam);
          }
          mParamsArray.add(lThemeParam);
        }
        else {
          throw new ExModule("Entry-theme param elements must be uniquely named or have XPaths that map to unique element names, duplicate found: '" + lThemeParam.getName() + "'.");
        }
      }

      DOMList lHeaderParamsList = pEntryThemeDOM.getUL("fm:header-param-list/fm:header-param");
      for (int i = 0; i < lHeaderParamsList.getLength(); i++) {
        ThemeParam lThemeParam = new ThemeParam(lHeaderParamsList.item(i), pModule, lStoreLocationElemName);
        if (!mHeaderParamsMap.containsKey(lThemeParam.getName())) {
          mHeaderParamsMap.put(lThemeParam.getName(), lThemeParam);
  //        if (lThemeParam.isMand()) {
  //          mHeaderParamsMap.put(lThemeParam.getName(), lThemeParam);
  //        }
        }
        else {
          throw new ExModule("Entry-theme header param elements must be uniquely named or have XPaths that map to unique element names, duplicate found: '" + lThemeParam.getName() + "'.");
        }
      }

      // Parse return values
      DOMList lReturnList = pEntryThemeDOM.getUL("fm:return-list/fm:return");
      for (int i = 0; i < lReturnList.getLength(); i++) {
        ThemeParam lThemeParam = new ThemeParam(lReturnList.item(i), pModule, lStoreLocationElemName);
        if (!mReturnMap.containsKey(lThemeParam.getName())) {
          mReturnMap.put(lThemeParam.getName(), lThemeParam);
          if (lThemeParam.isMand()) {
            mMandReturnMap.put(lThemeParam.getName(), lThemeParam);
          }
          mReturnArray.add(lThemeParam);
        }
        else {
          throw new ExModule("Entry-theme return elements must be uniquely named or have XPaths that map to unique element names, duplicate found: '" + lThemeParam.getName() + "'.");
        }
      }
    }
    catch (Throwable th) {
      throw new ExModule("Error in entry-theme definition " + mThemeName, th);
    }
  }

  private interface StorageLocationParseResult {
    List<DataDOMStorageLocation> getStorageLocationList();
    DataDOMStorageLocation getDefaultStorageLocation();
    Map<String, SyncMode> getStorageLocationSyncModes();
    Map<String, Boolean> getStorageLocationValidationFlags();
  }

  private StorageLocationParseResult parseStorageLocationDefinition(DOM pEntryThemeDOM)
  throws ExModule {

    //Search for a single SL definition or a list
    DOM lStorageLocationDefinition;
    String lStorageLocationName;
    try {
      lStorageLocationDefinition = pEntryThemeDOM.get1E("fm:storage-location");
      lStorageLocationName = lStorageLocationDefinition.value();
    }
    catch(ExCardinality x) {
      lStorageLocationDefinition = null;
      lStorageLocationName = null;
    }

    DOM lStorageLocationListDefinition = pEntryThemeDOM.get1EOrNull("fm:storage-location-list");

    //Check 1 of the 2 mutually exclusive elements is defined
    if(!XFUtil.isNull(lStorageLocationName) && lStorageLocationListDefinition != null) {
      throw new ExModule("fm:storage-location-list and fm:storage-location are mutually exclusive", pEntryThemeDOM);
    }
    else if(XFUtil.isNull(lStorageLocationName) && lStorageLocationListDefinition == null) {
      throw new ExModule("You must specify either an fm:storage-location-list or an fm:storage-location element containing a non-empty string", pEntryThemeDOM);
    }


    if(lStorageLocationListDefinition == null) {
      //Read the sync mode attribute off the single storage location definition
      final SyncMode lSyncMode;
      if(lStorageLocationDefinition.hasAttr(SYNC_MODE_ATTR_NAME)) {
        lSyncMode = SyncMode.fromExternalString(lStorageLocationDefinition.getAttr(SYNC_MODE_ATTR_NAME));
      }
      else {
        lSyncMode = SyncMode.SYNCHRONISED;
      }

      boolean lValidationEnabled = Boolean.valueOf(XFUtil.nvl(lStorageLocationDefinition.getAttr(VALIDATION_ENABLED_ATTR_NAME), "true"));

      //Single SL defined - this will also be the default
      final DataDOMStorageLocation lStorageLocation = mModule.getDataStorageLocation(lStorageLocationName);
      return new StorageLocationParseResult() {
        public List<DataDOMStorageLocation> getStorageLocationList() { return Collections.unmodifiableList(Collections.singletonList(lStorageLocation)); }
        public DataDOMStorageLocation getDefaultStorageLocation() { return lStorageLocation; }
        public Map<String, SyncMode> getStorageLocationSyncModes() { return Collections.singletonMap(lStorageLocation.getName(), lSyncMode); }
        public Map<String, Boolean> getStorageLocationValidationFlags() { return Collections.singletonMap(lStorageLocation.getName(), lValidationEnabled); }
      };
    }
    else {
      //Read a default sync mode attribute off the list container element
      SyncMode lDefaultSyncMode;
      if(lStorageLocationListDefinition.hasAttr(SYNC_MODE_ATTR_NAME)) {
        lDefaultSyncMode = SyncMode.fromExternalString(lStorageLocationListDefinition.getAttr(SYNC_MODE_ATTR_NAME));
      }
      else {
        lDefaultSyncMode = SyncMode.SYNCHRONISED;
      }

      boolean lValidationEnabledDefault = Boolean.valueOf(XFUtil.nvl(lStorageLocationListDefinition.getAttr(VALIDATION_ENABLED_ATTR_NAME), "true"));

      //List of SLs defined - parse it and ensure that a default is specified
      DOMList lSLDefinitionList = lStorageLocationListDefinition.getUL("fm:storage-location");

      //Check at least one SL has been specified in the list
      if(lSLDefinitionList.size() < 1) {
        throw new ExModule("Element fm:storage-location-list must contain at least one fm:storage-location child element", pEntryThemeDOM);
      }

      final List<DataDOMStorageLocation> lResultList = new ArrayList<>(lSLDefinitionList.size());
      final Map<String, SyncMode> lSyncModeMap = new HashMap<>(lSLDefinitionList.size());
      final Map<String, Boolean> lValidationFlagMap = new HashMap<>(lSLDefinitionList.size());
      Set<String> lContextLabels = new HashSet<>();

      //Retrieve the SL for each item in the list
      DataDOMStorageLocation lDefaultSL = null;
      for(DOM lSLDefinition : lSLDefinitionList) {
        String lSLName = lSLDefinition.value();
        if(XFUtil.isNull(lSLName)) {
          throw new ExModule("Element fm:storage-location must have text content", pEntryThemeDOM);
        }

        DataDOMStorageLocation lStorageLocation = mModule.getDataStorageLocation(lSLName);
        lResultList.add(lStorageLocation);

        //Work out sync mode for the SL
        SyncMode lSyncMode;
        if(lSLDefinition.hasAttr(SYNC_MODE_ATTR_NAME)) {
          lSyncMode = SyncMode.fromExternalString(lSLDefinition.getAttr(SYNC_MODE_ATTR_NAME));
        }
        else {
          lSyncMode = lDefaultSyncMode;
        }
        lSyncModeMap.put(lStorageLocation.getName(), lSyncMode);

        //Work out validation flag for the SL
        boolean lValidationEnabled;
        if(lSLDefinition.hasAttr(VALIDATION_ENABLED_ATTR_NAME)) {
          lValidationEnabled = Boolean.valueOf(XFUtil.nvl(lSLDefinition.getAttr(VALIDATION_ENABLED_ATTR_NAME), "true"));
        }
        else {
          lValidationEnabled = lValidationEnabledDefault;
        }
        lValidationFlagMap.put(lStorageLocation.getName(), lValidationEnabled);

        //Check that storage location context labels are unique in this entry theme
        String lContextLabel = lStorageLocation.getDocumentContextLabel();
        if(lContextLabels.contains(lContextLabel)) {
          throw new ExModule("Two or more storage locations in this entry theme share the same context label '" + lContextLabel + "'", pEntryThemeDOM);
        }
        lContextLabels.add(lContextLabel);

        //If this has been marked as the default SL, record that and check there are no other defaults defined
        if("true".equalsIgnoreCase(lSLDefinition.getAttr("default"))) {
          if(lDefaultSL == null) {
            lDefaultSL = lStorageLocation;
          }
          else {
            throw new ExModule("Only one storage location can be marked as default", pEntryThemeDOM);
          }
        }

        //Assert that the SL has a context label defined if required
        if(lStorageLocation.getDocumentContextLabel() == null && lSLDefinitionList.size() > 1) {
          throw new ExModule("All storage locations must have a context label defined when more than one is specified on an entry theme", pEntryThemeDOM);
        }
      }

      //If only one storage location was specified in the list we can assume that is the default, otherwise we have a problem
      if(lDefaultSL == null && lResultList.size() == 1) {
        lDefaultSL = lResultList.get(0);
      }
      else if(lDefaultSL == null) {
        throw new ExModule("Exactly one storage location in the fm:storage-location-list must be marked as default", pEntryThemeDOM);
      }

      final DataDOMStorageLocation lFinalDefaultSL = lDefaultSL;
      return new StorageLocationParseResult() {
        public List<DataDOMStorageLocation> getStorageLocationList() { return Collections.unmodifiableList(lResultList); }
        public DataDOMStorageLocation getDefaultStorageLocation() { return lFinalDefaultSL; }
        public Map<String, SyncMode> getStorageLocationSyncModes() { return Collections.unmodifiableMap(lSyncModeMap); }
        public Map<String, Boolean> getStorageLocationValidationFlags() { return lValidationFlagMap; }
      };
    }
  }

  /**
   * Validates that the theme, and its commands, are valid.
   *
   * @param module the module where the component resides
   * @throws ExInternal if the component syntax is invalid.
   */
  @Override
  public void validate(Mod module) throws ExInternal {
    mXDo.validate(module);
    mBeforeEntryXDo.validate(module);
  }

  /**
   * Returns the name of the theme.
   * @return the name of the entry theme.
   */
  public String getName() {
    return mThemeName;
  }

  /**
   * Returns the owning module.
   * @return reference to owner module
   */
  public final Mod getModule() {
    return mModule;
  }

  /**
   * Gets the storage location associated with this theme.
   * @return storage location
   */
  public final List<DataDOMStorageLocation> getStorageLocationList () {
    return mStorageLocationList;
  }


  public DataDOMStorageLocation getDefaultStorageLocation() {
    return mDefaultStorageLocation;
  }

  /**
   * Returns the type of theme (compare to Theme.TYPE_SERVICE | Theme.TYPE_INTERNAL | Theme.TYPE_EXTERNAL).
   * @return type as a string
   */
  public String getType() {
    return mType;
  }

  /**
   * Tests if this entry theme is externally accessible, i.e. it is allowed to be called via an HTTP GET request.
   * @return
   */
  public boolean isExternallyAccessible() {
    return !EntryTheme.gTypeInternal.equals(mType);
  }

  /**
   * Returns whether a theme should be considered published or unpublished
   * (i.e. for WSDL or other indexing purposes).
   * @return boolean
   */
  public boolean isPublished() {
    return mPublished;
  }

  /**
   * Returns whether a theme is acting as a web service operation.
   * @return boolean
   */
  public boolean isWebServiceOperation() {
    return mIsWebServiceOperation;
  }

  /**
   * Returns whether a thread should be persisted for this entry theme
   * (only applicable when the entry theme is a web service type theme).
   * @return boolean
   */
  public boolean isThreadPersistent() {
    return mIsThreadPersistent;
  }

  /**
   * Return a list of parsed parameter definitions.
   * @return ArrayList of parameters
   */
  public List<ThemeParam> getParamList() {
    return mParamsArray;
  }

  public Map getHeaderParamMap() {
    return new HashMap<>(mHeaderParamsMap);
  }

  /**
   * Return a list of parsed return definitions.
   * @return ArrayList of return types
   */
  public List<ThemeParam> getReturnList() {
    return mReturnArray;
  }

  /**
   * Return a list of mandatory parameter names.
   * @return ArrayList of required parameters
   */
  public List<String> getMandParamNames() {
    return new ArrayList<>(mMandParamsMap.keySet());
  }

  /**
   * Return a list of mandatory return names.
   * @return ArrayList of required return values
   */
  public List<String> getMandReturnNames() {
    return new ArrayList<>(mMandReturnMap.keySet());
  }

  /**
   * Gets and returns an AuthType instance based on the authorisation type of this EntyTheme
   * @return an AuthType subclass
   */
  public AuthType getAuthType () {
    return mAuthenticationType.getAuthType();
  }

  /**
   * Validates parameters against the theme params list.
   * @param pDOMList xml forest of parameters
   */
  public void validateParams (DOMList pDOMList)
  throws ExUserRequest {
    try {
      validateDOMList(pDOMList, mParamsMap, getMandParamNames());
    }
    catch (ExValidation ex) {
      throw new ExUserRequest("Invalid parameters", ex);
    }
    catch (Throwable ex) {
      throw new ExInternal("Could not validate parameters", ex);
    }
  }

  /**
   * Validates return values against the theme return list.
   * @param pDOMList xml forest of return value
   */
  public void validateReturn (DOMList pDOMList)
  throws ExModule {
    try {
      validateDOMList(pDOMList, mReturnMap, getMandReturnNames());
    }
    catch (ExValidation ex) {
      throw new ExModule("Invalid return values provided", ex);
    }
    catch (Throwable ex) {
      throw new ExInternal("Could not validate return values", ex);
    }
  }

  /**
   * Validates that a forest of parameters or return values match theme-level
   * expectations.
   * @param pDOMList xml forest to validate
   * @param pThemeParamMap parameter or return clauses to validate against
   * @param pMandThemeParamNames mandatory elements
   * @throws ExValidation validation failure
   */
  private void validateDOMList (DOMList pDOMList, Map pThemeParamMap, List<String> pMandThemeParamNames)
  throws ExValidation {
    // To hold references to mandatory params that have been validated
    List<String> lValidMandParams = new ArrayList<>();

    // Loop through what we actually have and check each item
    for (int i = 0; i < pDOMList.getLength(); i++) {
      // Get each forest element and its name
      DOM lDOMToValidate = pDOMList.item(i);
      String lElemName = lDOMToValidate.getName();

      // See if we have a corresponding param or return
      ThemeParam lThemeParam = (ThemeParam) pThemeParamMap.get(lElemName);
      if (lThemeParam == null) {
        // We have a param that doesn't match anything we're expecting to see
        // (this can be safely ignored)
        continue;
      }
      else {
        // We have a param that we can validate
        DOM lTypeDOM = lThemeParam.getTypeDOM();
        String lType = lThemeParam.getType();
        DOM lSchemaDOM;
        // If we don't have a declared spec, we have to rely on its simple type
        if (lTypeDOM == null) {
          lSchemaDOM = SchemaUtils.buildSchemaForType(lType, lElemName);
        }
        // Otherwise, we can validate against our declared schema fragment
        else {
          lSchemaDOM = SchemaUtils.buildSchemaFromFragment(lTypeDOM, lType, lElemName);
        }

        // Run validation
        lDOMToValidate.validateAgainstSchema(lSchemaDOM);

        // Store that this mandatory parameter has successfully validated
        if (lThemeParam.isMand()) {
          lValidMandParams.add(lThemeParam.getName());
        }
      }
    }

    // If we don't have enough mandatory parameters, build up an error string
    if (lValidMandParams.size() < pMandThemeParamNames.size()) {
      String lMissingParams = "";
      for (int i = 0; i < pMandThemeParamNames.size(); i++) {
        String lMandParamName = pMandThemeParamNames.get(i);
        if (lValidMandParams.contains(lMandParamName)) {
          continue;
        }
        else {
          lMissingParams += lMissingParams.length() > 0 ? ", " + lMandParamName : lMandParamName;
        }
      }
      throw new ExValidation("Missing mandatory parameters: " + lMissingParams);
    }
  }

  @Override
  public void writeTrackData() {
    Track.info("Name", mThemeName);
  }

  public boolean isDefaultEntryTheme() {
    return mDefaultEntryTheme;
  }

  public String getAttachXPath() {
    return mAttachXPath;
  }

  /**
   * Gets the list of commands to run for this entry theme.
   * @return
   */
  public XDoCommandList getXDo() {
    return mXDo;
  }

  /**
   * Gets a list of commands to run before the entry theme is entered, i.e. before the storage locations have been evaluated.
   * This can be used by module developers to initialise parameters for SL binds, etc.
   * @return Commands to run before entry theme processing. May be an empty list.
   */
  public XDoCommandList getBeforeEntryXDo() {
    return mBeforeEntryXDo;
  }

  public String getStateName() {
    return mStateName;
  }

  public EntryThemeSecurity getEntryThemeSecurity() {
    return mEntryThemeSecurity;
  }

  /**
   * Looks up the SyncMode defined for the storage location on this entry theme. This will only return null if the storage
   * location is not defined on the entry theme.
   * @param pStorageLocationName SL to look up.
   * @return SL sync mode.
   */
  public SyncMode getSyncModeForStorageLocation(String pStorageLocationName) {
    return mStorageLocationNamesToSyncModes.get(pStorageLocationName);
  }

  /**
   * Determines if the given storage location requires its fm:validation block to be executed within this entry theme.
   * Only synchronised storage locations with SELECT statements and fm:validation blocks defined are candidates for validation.
   * @param pStorageLocation SL to check.
   * @return True if validation is required.
   */
  public boolean isValidationRequiredForStorageLocation(DataDOMStorageLocation pStorageLocation) {
    String lStorageLocationName = pStorageLocation.getName();
    return pStorageLocation.hasQueryStatement() &&
      !pStorageLocation.getValidationCommands().isEmpty() &&
      mStorageLocationNamesToSyncModes.get(lStorageLocationName) == SyncMode.SYNCHRONISED &&
      mStorageLocationValidationFlags.get(lStorageLocationName);
  }

  public boolean isAllowedPasswordExpiredAccess() {
    return mAllowedPasswordExpiredAccess;
  }
}
