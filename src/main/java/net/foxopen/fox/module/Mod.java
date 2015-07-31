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
package net.foxopen.fox.module;


import com.google.common.collect.Multimap;
import net.foxopen.fox.App;
import net.foxopen.fox.FoxComponent;
import net.foxopen.fox.FoxRequest;
import net.foxopen.fox.FoxResponse;
import net.foxopen.fox.StringUtil;
import net.foxopen.fox.XFUtil;
import net.foxopen.fox.database.ConnectionAgent;
import net.foxopen.fox.database.UCon;
import net.foxopen.fox.database.sql.out.SQLTypeConverter;
import net.foxopen.fox.dbinterface.DatabaseInterface;
import net.foxopen.fox.dom.DOM;
import net.foxopen.fox.dom.DOMList;
import net.foxopen.fox.dom.NamespaceAttributeTable;
import net.foxopen.fox.dom.paging.PagerDefinition;
import net.foxopen.fox.dom.xpath.saxon.StoredXPathResolver;
import net.foxopen.fox.entrypoint.FoxGlobals;
import net.foxopen.fox.ex.ExActionFailed;
import net.foxopen.fox.ex.ExApp;
import net.foxopen.fox.ex.ExBadPath;
import net.foxopen.fox.ex.ExCardinality;
import net.foxopen.fox.ex.ExDB;
import net.foxopen.fox.ex.ExDBTooFew;
import net.foxopen.fox.ex.ExDoSyntax;
import net.foxopen.fox.ex.ExInternal;
import net.foxopen.fox.ex.ExModule;
import net.foxopen.fox.ex.ExSecurity;
import net.foxopen.fox.ex.ExServiceUnavailable;
import net.foxopen.fox.ex.ExTooFew;
import net.foxopen.fox.ex.ExUserRequest;
import net.foxopen.fox.module.clientvisibility.ClientVisibilityRule;
import net.foxopen.fox.module.datadefinition.DataDefinition;
import net.foxopen.fox.module.datanode.NodeAttribute;
import net.foxopen.fox.module.datanode.NodeInfo;
import net.foxopen.fox.module.entrytheme.EntryTheme;
import net.foxopen.fox.module.mapset.MapSetDefinition;
import net.foxopen.fox.module.mapset.MapSetDefinitionFactory;
import net.foxopen.fox.module.parsetree.ParseTree;
import net.foxopen.fox.module.parsetree.presentationnode.BufferPresentationNode;
import net.foxopen.fox.module.serialiser.HtmlDoctype;
import net.foxopen.fox.security.SecurityTable;
import net.foxopen.fox.thread.storage.DataDOMStorageLocation;
import net.foxopen.fox.thread.storage.FileStorageLocation;
import nu.xom.Element;

import java.io.InputStream;
import java.io.Reader;
import java.sql.Clob;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;


public class Mod
extends FoxComponent implements Validatable, NodeInfoProvider {
  private static final String IMPORTED = "fox:_imported";
  public static final String FOX_BASE_URI    = "http://www.og.dti.gov/fox";
  public static final String FOX_LOCAL_NO_NS_URI    = "http://www.og.dti.gov/fox";
  public static final String FOX_GLOBAL_NO_NS_URI   = "http://www.og.dti.gov/fox_global";
  public static final String FOX_MODULE_URI = "http://www.og.dti.gov/fox_module";
  public static final String FOX_MODULE_NS_PREFIX = "fm";
  public static final String FOX_NS_PREFIX = "fox";
  public static final String INIT_MODULE_LOCAL_SUFFIX    = "/INIT";

  public static final String XSI_PREFIX = "xsi";
  public static final String XSI_URI = "http://www.w3.org/2001/XMLSchema-instance";

  public static final String MERGE_SOURCE_MODULE_ATTR_NAME = "merge.source.module";

  private static final Pattern gFoxXMLNSPattern = Pattern.compile("xmlns:([^=]+)=\"" + FOX_BASE_URI + "(_global|_module)?(?:/?([^\"]+))?\"");

  private static final int MAX_EXPAND_LEVEL = 100;

  private static final String SECURITY_RULE_CONDITION_ATTRS[] = new String[]
  {
     "privilege",
     "theme",
     "state",
     "xpath",
     "datum-rec-path",
     "datum-type",
     "datum-id",
     "datum-scope",
     "build-only"
  };
  private static final Set<String> SECURITY_RULE_CONDITION_ATTRS_SET = new HashSet<>(Arrays.asList(SECURITY_RULE_CONDITION_ATTRS));

  /** Owning Application */
  public final App mApp; // TODO - Anyone with time should make this a mnem to lookup jit in Mod.getApp()

  /** The module's Name */
  public final String mModuleName;

  private final String mAuthRequired;

  /** The module's title. */
  private String mTitle;

  /** A description of the module. */
  private String mDescription;

  /** Module Header Information */
  private Map<String, String> mHashHeader;

  /** The modes security table module entry. */
  private SecurityTable mModesSecurityTable = new SecurityTable();

  /** The views security table module entry. */
  private SecurityTable mViewsSecurityTable = new SecurityTable();

  public static final String NO_APPLY_ATTRIBUTE = "no-apply";

  /** Map of action names to XDo objects */
  private HashMap<String, ActionDefinition> mActionNamesToDefinitions = new HashMap<>();

  /** Map of entry theme names to EntryTheme objects */
  private final Map<String, EntryTheme> mEntryThemes = new HashMap<>();
  private static final String DEFAULT_ENTRY_THEME_NAME = "new";
  private final String mDefaultEntryThemeName;

  /** Map of state name to state object */
  private final Map<String, State> mModuleStates = new HashMap<>();

  /** Map of file storage locations name to FileStorageLocation object */
  private final Map<String, FileStorageLocation> mFileStorageLocations = new HashMap<>();

  /** Map of map set name to MapSetDefinition object */
  private final Map<String, MapSetDefinition> mMapSetDefinitions = new HashMap<>();

  /** Map of data defintion name to DataDefinition object */
  private final Map<String, DataDefinition> mDataDefinitions = new HashMap<>();

  /** Map of pager name to MapSetDefinition object */
  private final Map<String, PagerDefinition> mPageDefNameToPageDefMap = new HashMap<>();

  /** Map of data storage location name to DataStorageLocation object */
  private final Map<String, DataDOMStorageLocation> mDataStorageLocations = new HashMap<>();

  /** holds service type themes only */
  private Map<String, EntryTheme> mThemeNameToServiceThemeMap = new HashMap<>();

  /** a hashtable with a xpath for the key and hold the NodeInfo object for each one */
  private Map<String, NodeInfo> mAbsolutePathToNodeInfoMap = new HashMap<>();

  /** holds the whole model data for all the meta data schema */
  private DOMList mModelDOMList = new DOMList(8);

  private Map<String, DatabaseInterface> mNameToDbInterfaceMap = new HashMap<>();

  /** The attributes for the html presentation for a specific module **/
  private Map<String, String> mModAttrs2Values;

  /** A list of the templates for this module, mapped from template name to <code>Template</code> */
  private Map<String, Template> templatesMap = new TreeMap<>();

  /** Temporary list of valid action names */
  private Set<String> mValidActionNames;

  /** Bulk compile error messages */
  private String mBulkModuleErrorMessages = "";
  private String mBulkModuleWarningMessages = "";

  private DOM mModuleRawDOM;
  private DOM mModuleMergeTargetDOM;
  private DOM mModuleTypeExpandedDOM;

  private Map<String, LibComponent> mLibNameToUsedLibComponentMap;

  // Sets used to record if XMLSchema or ModuleMeta already loaded
  private Set<String> mLoadedLibFormalNamesSet;

  private final List<CSSListItem> mStyleSheets = new ArrayList<>();

  private final Multimap<AutoActionType, ActionDefinition> mAutoActionMultimap = AutoActionType.createAutoActionMultimap();

  //TODO this should not be a member (lazy), should be passed down where needed
  private UCon mModuleParseUCon;


  private Map<String, ClientVisibilityRule> mVisibilityRuleNameToVisibilityRuleMap = new HashMap<String, ClientVisibilityRule>();

  private HtmlDoctype mDocumentType = null;

  private final StoredXPathResolver mStoredXPathResolver;

  /** Map of buffer names to pre-parsed Presentaion Nodes */
  private HashMap<String, BufferPresentationNode> mParsedBuffers = new HashMap<String, BufferPresentationNode>();
  /** Default set-page buffer */
  private BufferPresentationNode mSetPageBuffer;

  /**
   * Convenience method for getting a module with a given app mnem and module name string. If the target module cannot
   * be found, an error is raised.
   * @param pAppMnem
   * @param pModuleName
   * @return
   */
  public static Mod getModuleForAppMnemAndModuleName(String pAppMnem, String pModuleName) {
    try {
      return FoxGlobals.getInstance().getFoxEnvironment().getAppByMnem(pAppMnem).getMod(pModuleName);
    }
    catch (ExServiceUnavailable | ExApp | ExModule | ExUserRequest e) {
      throw new ExInternal("Cannot retrieve mod " + pModuleName + " for app " + pAppMnem, e);
    }
  }

  private ExModule exceptionForNamedElement(DOM pNamedElement, String pElementType, Throwable pThrowable) {
    return new ExModule("Failed to parse " + pElementType + " '" + XFUtil.nvl(pNamedElement.getAttr("name"), "(unnamed " + pElementType + ")")
      + "' from module " + XFUtil.nvl(pNamedElement.getAttr(MERGE_SOURCE_MODULE_ATTR_NAME), mModuleName), pThrowable);
  }

  /**
   * @param p_name The name of the application meta data
   * @param pInitialDOM Pointer to the root of the meta data DOM from a database clob
   * @param p_app Reference to the application class
   */
  public Mod(String p_name, DOM pInitialDOM, App p_app)
  throws ExModule, ExDoSyntax, ExServiceUnavailable {
    DOMList lParseDOMList;

    // Locate module element
    DOM lModuleDOM;
    try {
      lModuleDOM = pInitialDOM.get1E("/xs:schema/xs:annotation/xs:appinfo/fm:module");
    }
    catch (ExCardinality x) {
      throw new ExModule("Module not located in XML", x);
    }

    // Extract module header/control information
    try {
      lParseDOMList = lModuleDOM.get1E("fm:header").getChildElements();
      lParseDOMList.addAll(lModuleDOM.get1E("fm:control").getChildElements());
    }
    catch (ExCardinality x) {
      throw new ExModule("Module fm:header or fm:control malformed", x);
    }
    mHashHeader = new HashMap<>();
    int headersLen = lParseDOMList.getLength();
    for(int i=0; i<headersLen; i++) {
      DOM header = lParseDOMList.item(i);
      mHashHeader.put(header.getName(), header.value());
    }

    // Validate module header and control sections
    Object headername = mHashHeader.get("fm:name");
    if(headername==null || !p_name.equals(headername)) {
      throw new ExModule("Module header name conflicts with resource name ("+p_name+"!="+headername+")");
    }

    // Set basic module attributes
    mApp = p_app;
    mModuleName = p_name;
    mTitle = mHashHeader.get("fm:title");
    mDescription = mHashHeader.get("fm:description");
    mTitle = (mTitle != null ? mTitle : mModuleName);
    mDescription = (mDescription != null ? mDescription : mTitle);
    mAuthRequired = mHashHeader.get("fm:authentication");

    // Process schema and libraries
    //mModuleParseUCon = FoxRequest.getCurrentFoxRequest().getReusableUCon(mApp.getConnectKey(), "Loading Module Libs");
    //mModuleParseUCon = FoxRequest.getCurrentFoxRequest().getReusableContextUCon().getUCon("Mod Load " + mModuleName);
    // TODO - NP - Temp made a unique connection, should use contextucon from a request context at some point
    mModuleParseUCon = ConnectionAgent.getConnection(p_app.getConnectionPoolName(), "Loading Module Libs");
    try {
      processLibraries(pInitialDOM, lModuleDOM, mModuleName);

      // Should not reference raw module dom again used mModuleMergeTargetDOM instead
      mModuleRawDOM = pInitialDOM;
      pInitialDOM = null;

      // Relocate module element (this time from library merged xml)
      try {
        lModuleDOM = mModuleMergeTargetDOM.get1E("/xs:schema/xs:annotation/xs:appinfo/fm:module");
      }
      catch (ExCardinality x) {
        throw new ExModule("Module not located in XML", x);
      }

      // Build the modes/views security policy tables
      DOMList securityRuleEntries = lModuleDOM.getUL("fm:security-list/fm:security-rule");
      Map<String, DOM> ruleNameTopRuleDOMMap = new HashMap<>();
      for (int n=0; n < securityRuleEntries.getLength(); n++) {
        String name = securityRuleEntries.item(n).getAttrOrNull("name");
        if (name == null) {
           throw new ExModule("One or more security-rule definitions do not have a name attribute in module, "+mModuleName);
        }
        ruleNameTopRuleDOMMap.put(name, securityRuleEntries.item(n));
      }

      DOMList securityTableEntries = lModuleDOM.getUL("fm:security-list/fm:mode-rule");
      expandSecurityRuleReferences(ruleNameTopRuleDOMMap, securityTableEntries);
      mModesSecurityTable.setEntries(securityTableEntries);

      // Build the modes/views security policy tables
      securityTableEntries = lModuleDOM.getUL("fm:security-list/fm:view-rule");
      expandSecurityRuleReferences(ruleNameTopRuleDOMMap, securityTableEntries);
      mViewsSecurityTable.setEntries(securityTableEntries);

      // Build list of valid module level action names (for cross reference)
      mValidActionNames = new HashSet<>();
      lParseDOMList =  lModuleDOM.getUL("fm:action-list/fm:action");
      for (DOM lParseDOM : lParseDOMList) {
        String lActionName = lParseDOM.getAttr("name");
        if(mValidActionNames.contains(lActionName)) {
          throw new ExModule("Action name duplicated in module level actions: "+lActionName);
        }
        mValidActionNames.add(lActionName);
      }

      // Parse out the module display attributes
      Map<String, String> lFoxEnvironmentAttr = new HashMap<>(FoxGlobals.getInstance().getFoxEnvironment().getEnvDisplayAttributeList());
      // Overload with App attributes
      for (Map.Entry<String, String> lAttrEntry : mApp.getAppDisplayAttributeList().entrySet()) {
        String lKey = lAttrEntry.getKey();
        String lValue = lAttrEntry.getValue();
        lFoxEnvironmentAttr.put(lKey, lValue);
      }

      mModAttrs2Values = new HashMap<>(lFoxEnvironmentAttr);
      DOMList displayAttrs = lModuleDOM.getUL("fm:presentation/fm:display-attr-list/fm:attr");
      int daLen = displayAttrs.getLength();
      DOM displayAttr;
      String name;
      for (int loop=0; loop<daLen; loop++) {
        displayAttr = displayAttrs.item(loop);
        name = displayAttr.getAttr("name");
        mModAttrs2Values.put(name, XFUtil.nvl(displayAttr.value()));
      }

      // Locate DBInterface sections - done here before actions so run-query/run-api has something to resolve
      DOMList lDbintList = lModuleDOM.getUL("fm:db-interface-list/fm:db-interface");
      for(DOM lDbintDOM : lDbintList) {
        try {
          // create and parse the interface
          DatabaseInterface lDbInterface = new DatabaseInterface(lDbintDOM, this);
          mNameToDbInterfaceMap.put(lDbInterface.getInterfaceName(), lDbInterface) ;
        }
        catch (Throwable th) {
          throw exceptionForNamedElement(lDbintDOM, "db-interface", th);
        }
      }

      // Load states and their actions
      DOMList lStateDOMList = lModuleDOM.getUL("fm:state-list/fm:state");
      // First load all the valid state level actions (constructing the state might have an action that relies on this)
      for (DOM lStateDOM : lStateDOMList) {
        String lStateName = lStateDOM.getAttr("name");
        lParseDOMList = lStateDOM.getUL("fm:action-list/fm:action");
        for (DOM lActionDOM : lParseDOMList) {
          String lActionName = lActionDOM.getAttr("name");
          String lStateActionName = lStateName + "/" + lActionName;
          if(mValidActionNames.contains(lStateActionName)) {
            throw new ExModule("Action name duplicated in state : " + lStateActionName);
          }
          mValidActionNames.add(lActionName);
          mValidActionNames.add(lStateActionName);
        }
      }

      // Process Action list into hashtable of XDo Objects - done before states because this is where auto actions are populated, which are needed by state constructor
      DOMList lActionList = lModuleDOM.getUL("fm:action-list/fm:action");
      for(DOM lAction : lActionList) {
        try {
          ActionDefinition lActionDefinition = ActionDefinition.createActionDefinition(lAction, this);
          String lActionName = lActionDefinition.getActionName();

          if(lActionDefinition.isAutoAction()) {
            mAutoActionMultimap.put(lActionDefinition.getAutoActionType(), lActionDefinition);
          }
          else {
            mActionNamesToDefinitions.put(lActionName, lActionDefinition);
          }
        }
        catch (Throwable th) {
          throw exceptionForNamedElement(lAction, "action", th);
        }
      }

      //Parse the module level XPath list
      mStoredXPathResolver = ModuleStoredXPathResolver.createFromDOMList(lModuleDOM.getUL("fm:xpath-list/fm:xpath"));

      // Once the actions names have been collected, actually construct the objects for each State
      for (DOM lStateDOM : lStateDOMList) {
        State lState;
        try {
          lState = State.createState(this, lStateDOM);

          if(mModuleStates.containsKey(lState.getName())) {
            throw new ExModule("State name duplicated in module: " + lState.getName());
          }
        }
        catch (Throwable th) {
          throw exceptionForNamedElement(lStateDOM, "state", th);
        }

        mModuleStates.put(lState.getName(), lState);
      }

      // Process schema root elements to construct Model DOM and Node Information maps
      mModuleTypeExpandedDOM = mModuleMergeTargetDOM.createDocument();
      DOMList lRootElementList = mModuleTypeExpandedDOM.getUL("/xs:schema/xs:element");
      if(lRootElementList.getLength()==0) {
        mBulkModuleWarningMessages += "XMLSchema has no root elements /xs:schema/xs:element";
      }
      for (DOM lRootElementDOM : lRootElementList) {
        map_schema(lRootElementDOM, "", null, null, 0, null, new AtomicInteger(0));
      }

      // Assign schema foxids and set to read only
      mModuleTypeExpandedDOM.assignAllRefs();
      mModuleTypeExpandedDOM.getDocControl().setDocumentReadOnly();

      // Error check point
      if(mBulkModuleErrorMessages.length()>0) {
        throw new ExModule(mBulkModuleErrorMessages);
      }

      // Locate and parse storage-location sections
      DOMList lStorageLocationDOMs = lModuleDOM.getUL("fm:storage-location-list/fm:storage-location");
      for (DOM lStorageLocationDOM : lStorageLocationDOMs) {
        try {
          DataDOMStorageLocation lDataStorageLocation = new DataDOMStorageLocation(this, lStorageLocationDOM);
          mDataStorageLocations.put(lDataStorageLocation.getName(), lDataStorageLocation);
        }
        catch (Throwable th) {
          throw exceptionForNamedElement(lStorageLocationDOM, "storage-location", th);
        }
      }

      // Locate and parse file-storage-location sections
      DOMList lFileStorageLocationDOMs = lModuleDOM.getUL("fm:storage-location-list/fm:file-storage-location");
      for (DOM lFileStorageLocationDOM : lFileStorageLocationDOMs) {
        try {
          FileStorageLocation lFileStorageLocation = new FileStorageLocation(this, lFileStorageLocationDOM);
          mFileStorageLocations.put(lFileStorageLocation.getName(), lFileStorageLocation);
        }
        catch (Throwable th) {
          throw exceptionForNamedElement(lFileStorageLocationDOM, "file-storage-location", th);
        }
      }

      // Locate and parse entry-theme sections
      DOMList lEntryThemes = lModuleDOM.getUL("fm:entry-theme-list/fm:entry-theme");
      String lDefaultEntryThemeName = null;
      for (DOM lEntryThemeDOM : lEntryThemes) {
        EntryTheme lEntryTheme = new EntryTheme(this, lEntryThemeDOM);

        if (mEntryThemes.containsKey(lEntryTheme.getName())) {
          throw new ExInternal("More than one EntryTheme defined in module '" + this.getName() + "' with name '" + lEntryTheme.getName() + "'");
        }

        mEntryThemes.put(lEntryTheme.getName(), lEntryTheme);
        if (lEntryTheme.getType().equals(EntryTheme.gTypeServiceDocument) || lEntryTheme.getType().equals(EntryTheme.gTypeServiceRPC)) {
          mThemeNameToServiceThemeMap.put(lEntryTheme.getName(), lEntryTheme);
        }
        if (lEntryTheme.isDefaultEntryTheme()) {
          if (lDefaultEntryThemeName != null) {
            throw new ExModule("More than one Entry Theme marked up as default: '" + lDefaultEntryThemeName + "' and '" + lEntryTheme.getName() + "'");
          }
          lDefaultEntryThemeName = lEntryTheme.getName();
        }
      }
      // Set default entry theme to "new" where no entry themes were defined as default
      if (lDefaultEntryThemeName == null) {
        mDefaultEntryThemeName = DEFAULT_ENTRY_THEME_NAME;
      }
      else {
        mDefaultEntryThemeName = lDefaultEntryThemeName;
      }

      // Locate cascading style sheets
      DOMList lCSSList = lModuleDOM.getUL("fm:css-list/fm:css");
      for (DOM lCSSItem : lCSSList) {
        String lType = lCSSItem.getAttr("type").toLowerCase();
        if (XFUtil.isNull(lType)) {
          lType = "standard-and-accessible";
        }
        String lOrder = lCSSItem.getAttr("order");
        mStyleSheets.add(new CSSListItem(lCSSItem.value(), lType, lCSSItem.getAttr("browser-condition"), lOrder));
      }

      // Sort the CSS items to solve any precedence issues
      Collections.sort(mStyleSheets, new Comparator<CSSListItem>() {
        public int compare(CSSListItem lCSSItemA, CSSListItem lCSSItemB) {
          return lCSSItemA.getOrder() - lCSSItemB.getOrder();
        }
      });

      // Locate and parse the global html buffers
      DOMList buffers = lModuleDOM.getUL("fm:presentation/fm:set-buffer");
      int buffersLen = buffers.getLength();
      for(int loop=0; loop < buffersLen; loop++) {
        DOM lBufferDOM = buffers.item(loop);
        try {
          // Pre-parse module level buffers
          ParseTree lBufferParseTree = new ParseTree(lBufferDOM);
          BufferPresentationNode lBuffer = (BufferPresentationNode)lBufferParseTree.getRootNode();
          mParsedBuffers.put(lBuffer.getName(), lBuffer);
        }
        catch (Throwable th) {
          throw exceptionForNamedElement(lBufferDOM, "set-buffer", th);
        }
      }

      try {
        DOM setPageBuffer = lModuleDOM.get1E("fm:presentation/fm:set-page");

        try {
          ParseTree lBufferParseTree = new ParseTree(setPageBuffer);
          BufferPresentationNode lBuffer = (BufferPresentationNode)lBufferParseTree.getRootNode();
          lBuffer.setName("set-page");
          mSetPageBuffer = lBuffer;

          // Get doctype for HTML output, if specified
          mDocumentType = HtmlDoctype.getByNameOrNull(setPageBuffer.getAttrOrNull("document-type"));
        }
        catch (Throwable th) {
          throw exceptionForNamedElement(setPageBuffer, "set-page", th);
        }
      }
      catch (ExCardinality e) {
        mSetPageBuffer = null;
      }

      // Parse template-list and associated template sections.
      DOMList templates = lModuleDOM.getUL("fm:template-list/fm:template");
      for (int n=0; n < templates.getLength(); n++) {
        DOM lTemplateElement = templates.item(n);
        try {
          Template template = new Template(lTemplateElement);
          templatesMap.put(template.getName(), template);
        }
        catch (Throwable th) {
          throw exceptionForNamedElement(lTemplateElement, "template", th);
        }
      }

      // Parse Map Set definitions
      MapSetDefinition lMapSetDfn;
      String lMapSetName;
      DOMList lMapSetDOMList = lModuleDOM.getUL("fm:map-set-list/fm:map-set");
      for(DOM lMapSetDOM : lMapSetDOMList) {
        try {
          lMapSetDfn = MapSetDefinitionFactory.createDefinitionFromDOM(lMapSetDOM, this);
          lMapSetName = lMapSetDfn.mLocalName;
          // Ensure we have not already defined a map set of the same name
          if (mMapSetDefinitions.containsKey(lMapSetName)) {
            throw new ExModule("Duplicate definition for map set " + lMapSetName);
          }
          mMapSetDefinitions.put(lMapSetName, lMapSetDfn);
        }
        catch (Throwable th) {
          throw exceptionForNamedElement(lMapSetDOM, "map-set", th);
        }
      }

      // Parse Data definitions
      DataDefinition lDataDfn;
      String lDataDefinitionName;
      DOMList lDataDefinitionDOMList = lModuleDOM.getUL("fm:data-definition-list/fm:data-definition");
      for(DOM lDataDefinitionDOM : lDataDefinitionDOMList) {
        try {
          lDataDfn = DataDefinition.createDefinitionFromDOM(lDataDefinitionDOM, this);
          lDataDefinitionName = lDataDfn.getName();
          // Ensure we have not already defined a data definition of the same name
          if (mDataDefinitions.containsKey(lDataDefinitionName)) {
            throw new ExModule("Duplicate definition for data-definition " + lDataDefinitionName);
          }
          mDataDefinitions.put(lDataDefinitionName, lDataDfn);
        }
        catch (Throwable th) {
          throw exceptionForNamedElement(lDataDefinitionDOM, "data-definition", th);
        }
      }


      // Parse pagination control definitions
      PagerDefinition lPagerDfn;
      String lDefinitionName;
      DOMList lPagerDOMList = lModuleDOM.getUL("fm:pagination-definition-list/fm:pagination-definition");
      for (DOM lPagerDOM : lPagerDOMList) {
        try {
          lPagerDfn = new PagerDefinition(lPagerDOM, this);
          lDefinitionName = lPagerDfn.getName();
          if (mPageDefNameToPageDefMap.containsKey(lDefinitionName)) {
            throw new ExModule("Duplicate definition for pagination control " + lDefinitionName);
          }
          mPageDefNameToPageDefMap.put(lDefinitionName, lPagerDfn);
        }
        catch (Throwable th) {
          throw exceptionForNamedElement(lPagerDOM, "pagination-definition", th);
        }
      }


      // Parse visibility rule definitions
      ClientVisibilityRule lRule;
      String lRuleName;
      DOMList lRuleList = lModuleDOM.getUL("fm:client-visibility-rule-list/fm:client-visibility-rule");
      for (DOM lRuleDOM : lRuleList) {
        try {
          lRule = new ClientVisibilityRule(lRuleDOM, this);
          lRuleName = lRule.getRuleName();
          if (mVisibilityRuleNameToVisibilityRuleMap.containsKey(lRuleName)) {
            throw new ExModule("Duplicate definition for client visibility rule " + lRuleName);
          }
          mVisibilityRuleNameToVisibilityRuleMap.put(lRuleName, lRule);}
        catch (Throwable th) {
          throw exceptionForNamedElement(lRuleDOM, "client-visibility-rule", th);
        }
      }

      // Error check point
      if(mBulkModuleErrorMessages.length()>0) {
        throw new ExModule(mBulkModuleErrorMessages);
      }

      // Set model DOM to read only
      // Assign foxid refs to all parts of model DOM to prevent later
      // getRef "get or create" behaviour causing concurrency issues
      for(int i=0; i<mModelDOMList.getLength(); i++) {
        mModelDOMList.item(i).getRootElement().assignAllRefs();
        mModelDOMList.item(i).getDocControl().setDocumentReadOnly();
      }

      // TODO - NP - This should be reimplemented properly when we have one prod/dev switch to rule them all
      // Clear module source - no longer required for production
      if(FoxGlobals.getInstance().getFoxBootConfig().isProduction()) {
        mModuleRawDOM = null;
        // mModuleMergeTargetDOM = null;
        mModuleTypeExpandedDOM = null;
      }

      // Otherwise remove internal imported flag and set read only in case extracted by developers
      else {
        DOMList lAllImportedAttrsList;
        try {
          lAllImportedAttrsList = mModuleMergeTargetDOM.xpathUL("//xs:element", null);
        } catch(ExBadPath x) {throw x.toUnexpected();};
        int lAllImportedAttrsListCount = lAllImportedAttrsList.getLength();
        for(int i=0;i<lAllImportedAttrsListCount; i++) {
          lAllImportedAttrsList.item(i).removeAttr(IMPORTED);
        }
        mModuleMergeTargetDOM.getDocControl().setDocumentReadOnly();
      }
    }
    finally {
      // Return the borrowed UCon
      //FoxRequest.getCurrentFoxRequest().getReusableContextUCon().returnUCon(mModuleParseUCon, "Mod Load " + mModuleName);
      if (mModuleParseUCon != null) {
        mModuleParseUCon.closeForRecycle();
        mModuleParseUCon = null;
      }
    }

  } // end Mod constructor

  /**
  * Expands referenced security-rule instances into the mode-rules/view-rules that
  * reference them.
  *
  */
  private void expandSecurityRuleReferences(Map ruleNameTopRuleDOMMap, DOMList securityTableEntries) throws ExModule {
    for (int n=0; n < securityTableEntries.getLength(); n++) {
      DOM securityTableEntry = securityTableEntries.item(n);
      try {
        String ruleRefName = securityTableEntry.getAttrOrNull("rule-ref");

        // Expand security-rule references.
        if (ruleRefName != null && (ruleRefName = ruleRefName.trim()).length() > 0) {
          DOM referencedRule = (DOM)ruleNameTopRuleDOMMap.get(ruleRefName);
          if (referencedRule == null) {
            throw new ExModule("A mode-rule in module, "+mModuleName+", refers to security-rule, \""+ruleRefName+"\", that does not exist.");
          }
          Set<String> ruleConditionAtts = new HashSet<>(securityTableEntry.getAttributeMap().keySet());
          ruleConditionAtts.retainAll(SECURITY_RULE_CONDITION_ATTRS_SET); // Retain condition attributes only

          if (ruleConditionAtts.size() > 0) {
            throw new ExModule("A mode-rule in module, "+mModuleName+", refers to security-rule, \""+ruleRefName+
                               "\", but the mode-rule has condition attributes of its own - this is not allowed. Only "+
                               "condition attributes on the security-rule referred to are allowed.");
          }

          for (String lConditionAttribute : SECURITY_RULE_CONDITION_ATTRS) {
            String lRuleAttVal = referencedRule.getAttrOrNull(lConditionAttribute);
            if (lRuleAttVal != null) {
              securityTableEntry.setAttr(lConditionAttribute, lRuleAttVal);
            }
          }
        }
      }
      catch (Throwable th) {
        throw exceptionForNamedElement(securityTableEntry, securityTableEntry.getName(), th);
      }
    }
  }

   /**
   * Validates that the module, and its sub-components, are valid.
   *
   * @param module the module where the component resides
   * @throws ExInternal if the component syntax is invalid.
   */
   @Override
   public void validate(Mod module) throws ExInternal {
      //---------------------------------------------------------------------------
      // Alow the embedded module components to validate themselves in the full
      // context of this module.
      // * Actions
      // * Entry Themes
      // * Module States
      // * File Storage Locations
      // * Map Set Definitions
      // * Page Control Definitions
      //---------------------------------------------------------------------------
      // Lazily add all required components to validate to a list.
      List<Validatable> moduleComponentsList = new ArrayList<>(20);
      moduleComponentsList.addAll(mActionNamesToDefinitions.values());
      moduleComponentsList.addAll(mEntryThemes.values());
      moduleComponentsList.addAll(mModuleStates.values());
      moduleComponentsList.addAll(mFileStorageLocations.values());
      moduleComponentsList.addAll(mMapSetDefinitions.values());
      moduleComponentsList.addAll(mPageDefNameToPageDefMap.values());
      moduleComponentsList.addAll(mDataStorageLocations.values());

      for (Iterator iter=moduleComponentsList.iterator(); iter.hasNext(); ) {
      Validatable lFoxModuleComponent = (Validatable)iter.next();
         lFoxModuleComponent.validate(this);
      }


      // Validate all attributes in the data dom (loop through all elements)
//      ELEMENT_PATH_LOOP: for (Iterator iter=mAbsolutePathToNodeInfoMap.entrySet().iterator(); iter.hasNext(); ) {
//          // We need the keys and values of the map set so use the Map.Entry interface
//          Map.Entry nodeDetails = (Map.Entry)iter.next();
//          String lAbsolutePath = (String)nodeDetails.getKey();
//          NodeInfo nodeInfo = (NodeInfo)nodeDetails.getValue();
//          try
//          {
//            // loop through all namespaces in the element
//            for (Iterator namesps=nodeInfo.getAttributes().entrySet().iterator(); namesps.hasNext(); )
//            {
//                Map.Entry namespace = (Map.Entry)namesps.next();
//                String lNamespace = (String)namespace.getKey();
//
//                // loop through all attributes for that namespace
//                for (Iterator attrs=((Map)namespace.getValue()).entrySet().iterator(); attrs.hasNext();)
//                {
//                  Map.Entry attribute = (Map.Entry)attrs.next();
//                  String lAttributeName = (String)attribute.getKey();
//                  String lAttributeValue = (String)attribute.getValue();
//                  // TODO: Add in validation code here
//                  if (lAttributeName.equals("col"))
//                  {
//                    validateDbColumnName(lNamespace, lAttributeValue);
//                  }
//                }
//            }
//          }
//          catch (ExModule ex)
//          {
//            throw new ExInternal ("Cannot validate element " + nodeInfo.getName() + " in path " +lAbsolutePath , ex);
//          }
//
//      } // ELEMENT_PATH_LOOP
  }

  /**
   * A recursive method that creates 3 data structures from parsing the schema information:
   * a hashtable mapping xpath to NodeInfo,
   * a hashtable mapping NodeInfo to a model dom element
   * and a model dom structure (simple version of the data dom).
   */
  private void map_schema (
    DOM pParseDOM
  , String pParentPath
  , NodeInfo pParentNodeInfo
  , DOM pParentModelDOM
  , int pElementLevel
  , String pCurrentCellmateKey
  , AtomicInteger pCellmateCount
  )
  throws
    ExInternal
  , ExModule
  {

    // String path = absolute;
    DOM lModelDOM = pParentModelDOM;
    NodeInfo lNodeInfo = pParentNodeInfo;
    String lPath = pParentPath;

    // Identify schema definition command
    String lCommand_intern_equals = pParseDOM.getName().intern();

    String lCurrentCellmateKey = null;

    // Process XMLSchema element definition
    if(lCommand_intern_equals=="xs:element") {
      // Get element name and compute and validate absolute path
      String lElementName = pParseDOM.getAttr("name");
      lPath = pParentPath+"/"+lElementName;
      if(mAbsolutePathToNodeInfoMap.containsKey(lPath)) {
        throw new ExModule(
          "Schema definition contains xs:choice or duplicates leading to identical absolute\n"
        + "XPATHs, not supported at this time: "+lPath
        );
      }

      // Increment element level count
      pElementLevel++;

      // Intercept simple or complex type definition
      String lType = pParseDOM.getAttrOrNull("type");
      if( lType!=null
      &&  !NodeInfo.containsSupportedXsDatatype(lType)
      &&  pElementLevel < MAX_EXPAND_LEVEL
      // workaround for now
      ) {

        // Locate simple / complex type definition node
        DOMList lComplexTypes = mModuleTypeExpandedDOM.getElementsByAttrValue("xs:complexType", "name", lType);
        DOM SimpleComplexDefinitionDOM;
        if(lComplexTypes.getLength()==1) {
          SimpleComplexDefinitionDOM = lComplexTypes.item(0);
        }
        else {
          DOMList lSimpleTypes = mModuleTypeExpandedDOM.getElementsByAttrValue("xs:simpleType", "name", lType);
          if(lSimpleTypes.getLength()==1) {
            SimpleComplexDefinitionDOM = lSimpleTypes.item(0);
          }
          else {
            throw new ExModule("Unable to expand type='"+lType+"'");
          }
        }

        // Allow annotation and documentation to pass through unharmed
        try {
          // Shortcut the validation by using the hasContent DOM method first
          if (pParseDOM.hasContent()) {
            // If we have content, we now go to the extra effort of running this xpath
            if (pParseDOM.xpathUL(".//*[name(.)!='xs:annotation' and name(.)!='xs:documentation']", null).getLength() > 0) {
              throw new ExModule("xs:element "+lPath+" has type='"+lType+"' should not have content");
            }
          }
        }
        catch (ExBadPath ex) {
          throw new ExInternal("Error validating child nodes of element at " + lPath);
        }

        // Copy non-name attributes
        Map<String, String> lAttrMap = SimpleComplexDefinitionDOM.getAttributeMap();
        lAttrMap.remove("name");

        // Expand type definition content inline
        SimpleComplexDefinitionDOM
          .clone(true, pParseDOM)
          .removeAttr("name")
          .moveToParent(pParseDOM);
        //pParseDOM.removeAttr("type"); removing type phantom breaks htmlgenerator logic

        // Add the attributes from the type object to the parent
        for (Map.Entry<String, String> lEntry : lAttrMap.entrySet()) {
          if (!pParseDOM.hasAttr(lEntry.getKey())) {
            pParseDOM.setAttr(lEntry.getKey(), lEntry.getValue());
          }
        }
      }

      // Apply the key to the element
      if (pCurrentCellmateKey != null) {
        pParseDOM.setAttr("fox:cellmateKey", pCurrentCellmateKey);
      }

      // Initialise Model DOM node for this element definition
      if (pParentModelDOM != null) {
        lModelDOM = pParentModelDOM.addElem(lElementName);
      }
      else {
        DOM lDOM = DOM.createDocument(lElementName);
        mModelDOMList.add(lDOM);
        lModelDOM = lDOM;
      }

      // Initialise NodeInfo for this element definition
      lNodeInfo = new NodeInfo(pParseDOM, lElementName, lModelDOM, pParentNodeInfo, lPath, this);
      // Register NodeInfo with Absolute Path Map
      mAbsolutePathToNodeInfoMap.put(lPath, lNodeInfo);
    }

    // Process XMLSchema enumeration
    else if (lCommand_intern_equals=="xs:enumeration") {
      pParentNodeInfo.addSchemaEnumeration(pParseDOM.getAttr("value"));
    }

    // Process XMLSchema pattern
    else if (lCommand_intern_equals=="xs:pattern") {
      String value = pParseDOM.getAttr("value");
      Pattern pattern;

      // perform some basic validation before proceeding
      if (value == null || value == "") {
        throw new ExModule ("Pattern value is null in xs:element " + pParentNodeInfo.getName());
      }

      try {
        pattern = Pattern.compile(value);
      }
      catch (PatternSyntaxException e) {
        throw new ExModule ("Pattern value " + value + " does not conform to Perl5 regular expression standard in xs:element " + pParentNodeInfo.getName(), e);
      }

      pParentNodeInfo.addSchemaPattern(pattern);
    }

    // Process XMLSchema xs: facets xs: da (e.g. xs:min inclusive = 1)
    else if (NodeInfo.containsSupportedXsFacet(lCommand_intern_equals)) {
      pParentNodeInfo.setXsFacet(lCommand_intern_equals, pParseDOM.getAttr("value"));
    }

    // Process XMLSchema restriction base xs: datatypes and constructs (e.g. xs:string)
    else if ( lCommand_intern_equals=="xs:restriction") {
      if (NodeInfo.containsSupportedXsDatatype(pParseDOM.getAttr("base"))) {
        pParentNodeInfo.setDataType(pParseDOM.getAttr("base"));
      }
    }
    else if (lCommand_intern_equals=="xs:sequence" && pParseDOM.hasAttr("fox:cellmateKey")) {
      lCurrentCellmateKey = pParseDOM.getAttr("fox:cellmateKey");
      NamespaceAttributeTable lCellmateAttributes = pParseDOM.getNamespaceAttributeTable();
      if (".".equals(lCurrentCellmateKey)) {
        // If they set the key to . Generate them a unique key and modify the value in the attr table to match
        lCurrentCellmateKey = lPath + "/CellMates#" + pCellmateCount.incrementAndGet();
        lCellmateAttributes.addAttribute("fox", "cellmateKey", lCurrentCellmateKey);
      }

      if (pCurrentCellmateKey != null) {
        throw new UnsupportedOperationException("Cellmate sequences cannot be nested, found \"" + lCurrentCellmateKey + "\" inside \"" + pCurrentCellmateKey + "\"");
      }

      pParentNodeInfo.addCellmateAttributes(lCurrentCellmateKey, lCellmateAttributes);
    }

    // Get the children of the current node and calls this algorithm with these children
    DOMList children = pParseDOM.getChildElements();
    AtomicInteger lCellmateCount = new AtomicInteger(0);
    for (DOM child : children) {
      map_schema(child, lPath, lNodeInfo, lModelDOM, pElementLevel, lCurrentCellmateKey, lCellmateCount);
    }

  } // end of map_schema

  /**
   * retrieve the NodeInfo of an element by passing the method an absolute path of the node, null is returned when no value is found.
   */
  @Override
  public NodeInfo getNodeInfo(String absolute) {
    return mAbsolutePathToNodeInfoMap.get(absolute);
  }

  /**
   * retrieve the NodeInfo of an element by passing the method the UElem, null is returned when no value is found.
   */
  @Override
  public NodeInfo getNodeInfo(DOM findModelElement) {
    String abs = findModelElement.absolute();
    return mAbsolutePathToNodeInfoMap.get(abs);
  }

  /**
   * Gets the default attribute (i.e. "fox:" attribute) from the NodeInfo of the given node.
   * @param pNode Target node.
   * @param pAttribute Attribute to look up.
   * @return Attribute value, or null if no node info or attribute definition was found.
   */
  public String getNodeInfoDefaultAttribute(DOM pNode, NodeAttribute pAttribute) {
    NodeInfo lNodeInfo = getNodeInfo(pNode);
    if(lNodeInfo != null) {
      return lNodeInfo.getFoxNamespaceAttribute(pAttribute);
    }
    else {
      return null;
    }
  }

  /**
   * Gets an attribute from the NodeInfo of the given node.
   * @param pNode Target node.
   * @param pNamespacePrefix Namespace of attribute to resolve.
   * @param pAttribute Attribute to look up.
   * @return Attribute value, or null if no node info or attribute definition was found.
   */
  public String getNodeInfoAttribute(DOM pNode, String pNamespacePrefix, NodeAttribute pAttribute) {
    NodeInfo lNodeInfo = getNodeInfo(pNode);
    if(lNodeInfo != null) {
      return lNodeInfo.getAttribute(pNamespacePrefix, pAttribute.getExternalString());
    }
    else {
      return null;
    }
  }

  /**
   * retrieve the database column name mapped to a given dom element for a given interface
   */
  public String getDbColumnName(DOM findModelElement, String dbInterfaceName)
  throws ExInternal {
    NodeInfo path = getNodeInfo(findModelElement.absolute());
    if (path != null) {
      return path.getAttribute(dbInterfaceName, "col") ;
    }
    else {
      return null;
    }
  }

  /**
   * Get a Module State object by name
   *
   * @param pName State name
   * @return
   */
  public State getState(String pName) {
    State lState = mModuleStates.get(pName);
    if(lState == null) {
      throw new ExInternal("State name not defined: " + pName);
    }

    return lState;
  }

  /** Returns application that owns module */
  public net.foxopen.fox.App getApp() {
    return mApp;

  }

  /** Returns module name */
  public String getName() {
    return mModuleName;
  }

   /**
   *  Returns the template with the name specified.
   *
   * @return  the template, or null if no template with
   *          the specified name exists.
   */
  public Template getTemplate(String name) {
      return templatesMap.get(name);
  }

  /**
   * Returns the specified file storage location. Throws an error if it does not exist.
   *
   * @param name the name of the file storage location to obtain.
   */
  public FileStorageLocation getFileStorageLocation(String name) {
    FileStorageLocation lFileStorageLocation = mFileStorageLocations.get(name);
    if(lFileStorageLocation == null) {
      throw new ExInternal("File storage location '" + name + "' cannot be located in module " + getName());
    }
    return lFileStorageLocation;
  }

  /**
   * Returs a store location from the module cache if it exists
   */
  public DataDOMStorageLocation getDataStorageLocation(String pStoreName) throws ExModule {
    if (!mDataStorageLocations.containsKey(pStoreName)) {
      throw new ExModule("Could not find store location "+pStoreName + " in module " + mModuleName);
    }
    return mDataStorageLocations.get(pStoreName);
  }

  @Deprecated
  @Override
  public FoxResponse processResponse(FoxRequest pRequest , StringBuffer pURLTail)
  throws ExInternal, ExSecurity, ExActionFailed, ExModule, ExApp, ExServiceUnavailable {
    throw new ExInternal("Modules are noot allowed to processResponse");
  }

  public final String getType() {
    return "module";
  }

  public final boolean badActionName(String pName) {
    return !mValidActionNames.contains(pName);
  }

  public String getTitle() {
    return mTitle;
  }

  public String getDescription() {
    return mDescription;
  }

  public SecurityTable getModeRulesTable() {
     return mModesSecurityTable;
  }

  public SecurityTable getViewRulesTable() {
     return mViewsSecurityTable;
  }

  public Map<String, String> getModuleAttributes() {
    return mModAttrs2Values;
  }

  public Map<String, ActionDefinition> getActionDefinitionMap() {
    return mActionNamesToDefinitions;
  }

  public String getDefaultEntryThemeName() {
    return mDefaultEntryThemeName;
  }

  public EntryTheme getDefaultEntryTheme()
  throws ExUserRequest {
    return getEntryTheme(mDefaultEntryThemeName);
  }

  public boolean isAuthenticationRequired() {
    return !"not-required".equals(mAuthRequired);
  }

  @Override
  public Reader getReader() {
    return null;
  }

  @Override
  public InputStream getInputStream() {
    return null;
  }

  private static class LibComponent {
    final String mFormalName;
    final String mOriginalSeekName;
    final DOM    mRawComponentDOM;
    LibComponent (
      String pFormalName
    , String pOriginalSeekName
    , DOM    pRawComponentDOM
    ) {
      mFormalName = pFormalName;
      mOriginalSeekName = pOriginalSeekName;
      mRawComponentDOM = pRawComponentDOM;
    }
  }

  private final void localiseAttrNamespaces(
    DOM pNodeDOM
  , String pLocaliseAttrNSpaceSuffix
  )
  throws
    ExInternal
  , ExModule
  {
    Map<String, String> lAttributeMap = pNodeDOM.getAttributeMap();

    // When attrs exist
    if(lAttributeMap.size() > 0) {

      // Remove all existing attrs - this ensures replaced attrs go in correct order
      REMOVE_LOOP: for(String lAttributeName : lAttributeMap.keySet()) {
        pNodeDOM.removeAttr(lAttributeName);
      }

      // Replace attrs with modified namespaces
      REPLACE_LOOP: for(Map.Entry<String, String> lAttribute : lAttributeMap.entrySet()) {
        String lAttributeName = lAttribute.getKey();

        // Deal with namespace preference attribute which is fox:'d in the module schema
        if ("fox:namespacePreference".equals(lAttributeName))  {
          lAttributeMap.put(lAttributeName, updateNamespaceReference(lAttribute.getValue(), pNodeDOM, pLocaliseAttrNSpaceSuffix));
          pNodeDOM.setAttr(lAttributeName, lAttributeMap.get(lAttributeName));
        }

        // Deal with non-namespace attributes
        int lColonPosition = lAttributeName.indexOf(':');
        if(lColonPosition == -1) {
          //Rewrite namespaces referenced by security rules
          if ("namespace".equals(lAttributeName) && (pNodeDOM.getName().endsWith("mode-rule") || pNodeDOM.getName().endsWith("view-rule")) )  {
            pNodeDOM.setAttr(lAttributeName, updateNamespaceReference(lAttribute.getValue(), pNodeDOM, pLocaliseAttrNSpaceSuffix));
          }
          else {
            pNodeDOM.setAttr(lAttributeName, lAttribute.getValue());
          }
          continue REPLACE_LOOP;
        }

        /*
         * For attributes in a fox namespace, we may need to rewrite the prefix and/or define the namespace in the
         * mModuleMergeTargetDOM DOM.
         *
         * Note: this code assumes that if mModuleMergeTargetDOM is null then a library is not being processed and
         * no rewriting needs to occur. mModuleMergeTargetDOM must be not-null to localise namespaces as it is required
         * to hold the new namespace prefix definitions for the eventual merge DOM.
         */

        // Extract attr namespace
        String lPrefix = lAttributeName.substring(0, lColonPosition);

        //The URI for this namespace as defined by the rules in parseModuleDOM
        String lNamespaceURI = XFUtil.nvl(pNodeDOM.getURIForNamespacePrefix(lPrefix), "");

        if(lNamespaceURI.indexOf(FOX_LOCAL_NO_NS_URI + "/") != -1 && mModuleMergeTargetDOM != null) {
          //This is a local namespace and we are librarying the module in
          //Generate a new prefix. I.e. ns1 => ns1--ml1
          String lNewPrefix = lPrefix + pLocaliseAttrNSpaceSuffix;
          //Declare the new namespsace on the library's root element. This allows us to rename the attribute locally
          //before it is copied to the merge target DOM.
          pNodeDOM.getRootElement().addNamespaceDeclaration(lNewPrefix, lNamespaceURI);
          //Put new namespace declaration on merge target root, so when the attribute is copied from the library it
          //has a valid definition in its new document.
          mModuleMergeTargetDOM.getRootElement().addNamespaceDeclaration(lNewPrefix, lNamespaceURI);
          //Set the attribute with its new name.
          pNodeDOM.setAttr(lNewPrefix + lAttributeName.substring(lColonPosition), lAttribute.getValue());
        }
        else if (lNamespaceURI.indexOf(FOX_LOCAL_NO_NS_URI + "/") != -1 && lNamespaceURI.endsWith(INIT_MODULE_LOCAL_SUFFIX)){
          //If this namespace was on the initial module, we need to rename its URI to remove the INIT MODULE marker
          String lNewNamespaceURI = lNamespaceURI.substring(0, lNamespaceURI.lastIndexOf(INIT_MODULE_LOCAL_SUFFIX));
          //Also rename the prefix to make sure it's unique
          String lNewPrefix = lPrefix + pLocaliseAttrNSpaceSuffix;

          pNodeDOM.getRootElement().addNamespaceDeclaration(lNewPrefix, lNewNamespaceURI);
          pNodeDOM.setAttr(lNewPrefix + lAttributeName.substring(lColonPosition), lAttribute.getValue());

        }
        else if (lNamespaceURI.indexOf(FOX_GLOBAL_NO_NS_URI + "/") != -1 && mModuleMergeTargetDOM != null) {
          //This is a global namespace - its prefix is not renamed, but the merge target DOM may need the additional
          //namespace defition on it.
          String lTargetDOMNamespaceURI = mModuleMergeTargetDOM.getURIForNamespacePrefix(lPrefix);
          if(lTargetDOMNamespaceURI == null){
            //Not yet defined, so do it now
            mModuleMergeTargetDOM.getRootElement().addNamespaceDeclaration(lPrefix, lNamespaceURI);
          }
          pNodeDOM.setAttr(lAttributeName, lAttribute.getValue());
        }
        else {
          //Other namespaces replaced as is
          pNodeDOM.setAttr(lAttributeName, lAttribute.getValue());
        }

      } // end REPLACE_LOOP

    } // end when attrs exist

    // Recurse into child elements
    DOMList lKidList = pNodeDOM.getChildElements();
    for(DOM lChild : lKidList) {
      localiseAttrNamespaces(lChild, pLocaliseAttrNSpaceSuffix);
    }
  }

  /**
   * Update namespaces referenced in lAttributeValue to their expanded versions (e.g. ns1,ns2 -> ns1--m,ns2--ml1)
   *
   * @param lAttributeValue Attribute value, possibly a CSV string
   * @param pNodeDOM DOM to get namespace uri from
   * @param pLocaliseAttrNSpaceSuffix pre-determined suffix
   * @return
   */
  private String updateNamespaceReference(String lAttributeValue, DOM pNodeDOM, String pLocaliseAttrNSpaceSuffix) {
    Iterator<String> lNamespaces = StringUtil.commaDelimitedListToIterableString(lAttributeValue).iterator();
    StringBuilder lReturnBuffer = new StringBuilder();
    while(lNamespaces.hasNext()) {
      String lNamespace = lNamespaces.next();
      String lRenameTo;
      String lNamespaceURI = XFUtil.nvl(pNodeDOM.getURIForNamespacePrefix(lNamespace), "");
      //If this is a LOCAL namespace and we're processing a library, its prefix needs renaming here.
      //We also rename local namespaces present on the initial module in the library structure because they will also have been renamed when merging.
      //Otherwise keep it as is.
      if((lNamespaceURI.indexOf(FOX_LOCAL_NO_NS_URI + "/") != -1 && mModuleMergeTargetDOM != null) || lNamespaceURI.endsWith(INIT_MODULE_LOCAL_SUFFIX)) {
        lRenameTo = lNamespace + pLocaliseAttrNSpaceSuffix;
      } else {
        lRenameTo = lNamespace;
      }
      //Rebuild the string
      lReturnBuffer.append(lRenameTo).append(lNamespaces.hasNext() ? "," : "");
    }
    return lReturnBuffer.toString();
  }


  /**
   * Loads a library from the database currently  only caters for simple library name
   * todo: ask jason about catering for app/module/library or app/lib formats
   *
   */
  private final LibComponent getLibComponentByName(
    final String pLibName
  , final String pLocaliseAttrNSpaceSuffix
  , final String pContext
  , boolean pIsInitialModule
  )
  throws
    ExModule
  , ExInternal
  {

    // Restore previous LibComponent
    LibComponent lLibComponent = mLibNameToUsedLibComponentMap.get(pLibName);
    if(lLibComponent!=null) {
      return lLibComponent;
    }

    String lFormalName = pLibName;

    // Load raw library DOM
    DOM lSchemaDOM;
    try {
      Clob lModuleClob = mModuleParseUCon.querySingleRow(mApp.getResourceTableParsedStatement(), pLibName).getClob("DATA");
      lSchemaDOM = Mod.parseModuleDOM(SQLTypeConverter.clobToStringBuffer(lModuleClob), pLibName, pIsInitialModule);
    }
    catch(ExDBTooFew x) {
      throw new ExModule ("Referenced Library " + pLibName + " could not be found, in "+pContext, x);
    }
    catch(ExDB x) {
      throw new ExModule ("Referenced Library " + pLibName + " could not be accessed, in "+pContext, x);
    }
    catch(Throwable x) {
      throw new ExModule ("Referenced Library " + pLibName + " error, in "+pContext, x);
    }

    // Validate schema root element
    if(!lSchemaDOM.getName().equals("xs:schema")) {
      throw new ExModule("loadLibrary "+pLibName+" root element is not xs:schema, in "+pContext);
    }

    // Rename namespaces in schema
    localiseAttrNamespaces(
      lSchemaDOM
    , pLocaliseAttrNSpaceSuffix
    );

    //Clean up temporary namespace definitions before they are propogated
    if(pIsInitialModule){
      int i = 0;
      Element lElem = (Element) lSchemaDOM.getNode();
      while(i < lElem.getNamespaceDeclarationCount()){
        String lPrefix = lElem.getNamespacePrefix(i);
        String lURI = lElem.getNamespaceURI(lPrefix);
        if(lURI.endsWith(INIT_MODULE_LOCAL_SUFFIX)){

          int lDeclarationCountBefore = lElem.getNamespaceDeclarationCount();
          lElem.removeNamespaceDeclaration(lPrefix);
          if(lElem.getNamespaceDeclarationCount() == lDeclarationCountBefore) {
            throw new ExModule("Unable to remove namespace declaration for NS prefix " + lPrefix + " URI " + lURI + " - this indicates the namespace has an invalid URI");
          }
        }
        else {
          i++;
        }
      }
    }

    // Create and cache LibComponent
    lLibComponent = new LibComponent(lFormalName, pLibName, lSchemaDOM);
    mLibNameToUsedLibComponentMap.put(lFormalName, lLibComponent);

    // Return LibComponent
    return lLibComponent;

  }

  private void processLibraries(
    final DOM pModuleRootDOM
  , final DOM pModuleMetaDOM
  , final String pModuleName
  )
  throws
    ExInternal
  , ExModule
  {

    final String lModuleContext = "(Module)"+pModuleName;

    // Locate library list
    DOM lLibraryList = null;
    try{
      lLibraryList = pModuleMetaDOM.get1E("fm:library-list");
    }
    catch ( ExTooFew x) {
      // OK this is fine as library includes are optional
    }
    catch ( ExCardinality x) {
      // module validation - if too many or too few schemas error
      throw new ExModule("Bad data schema root /module/fm:library-list", x);
    }

    // Initialise library component info map
    mLibNameToUsedLibComponentMap = new HashMap<>();

    // Initialise Sets used to record if XMLSchema or ModuleMeta already loaded
    mLoadedLibFormalNamesSet = new HashSet<>();

    // Get schema name
    String lSchemaName = "";
    if(lLibraryList!=null) {
      try {
        lSchemaName = lLibraryList.get1S("fm:schema");
      }
      catch(ExCardinality x) {}
    }

    // Process schema and module load sequence
    if(lSchemaName.length()!=0) {

      // Get library component
      LibComponent lSchemaLibComponent = getLibComponentByName(
        lSchemaName
      , "--s"
      , lModuleContext
      , true
      );

      final String lSchemaContext = lModuleContext+"(schema)"+lSchemaLibComponent.mFormalName;

      // Flag schema as having been loaded
      mLoadedLibFormalNamesSet.add(lSchemaLibComponent.mFormalName);

      // Initialise the merge target
      mModuleMergeTargetDOM = lSchemaLibComponent.mRawComponentDOM.createDocument();

      // Load schemas nested libraries
      mergeNestedLibraries(
        lSchemaLibComponent.mRawComponentDOM // pSourceDOM
      , false // pImportChecking
      , "--s" // pNamespaceContext
      , lSchemaContext
      );

      // Flag all current elements as belonging to schema
      DOMList lAllElementsList;
      try {
        lAllElementsList = mModuleMergeTargetDOM.xpathUL("//xs:element", null);
      } catch(ExBadPath x) {throw x.toUnexpected();};
      int lAllElementsCount = lAllElementsList.getLength();
      for(int i=0;i<lAllElementsCount; i++) {
        lAllElementsList.item(i).setAttr(IMPORTED, "");
      }

      // Get module component
      LibComponent lModuleComponent = getLibComponentByName(
        pModuleName
      , "--m"
      , lModuleContext
      , false
      );

      // Merge module nodes into target schema
      mergeSchemaNodeRecursive(
        mModuleMergeTargetDOM
      , lModuleComponent.mRawComponentDOM
      , lModuleContext+"/xs:schema"
      , null // pForFatherType
      , false // pForFatherWasInSchema
      , true // pEnableModuleMetaMerging
      , true // pSourceIsModuleMete
      , true // pImportChecking
      , pModuleName
      );

      // Flag module as having been loaded
      mLoadedLibFormalNamesSet.add(lModuleComponent.mFormalName);

      // Load modules nested libraries
      mergeNestedLibraries(
        lModuleComponent.mRawComponentDOM // pSourceDOM
      , true // pImportChecking
      , "--m" // pNamespaceContext
      , lModuleContext
      );

    }

    // Process module only load sequence
    else {

      // Get module component
      LibComponent lModuleComponent = getLibComponentByName(
        pModuleName
      , "--m"
      , lModuleContext
      , true
      );

      // Initialise the merge target
      mModuleMergeTargetDOM = lModuleComponent.mRawComponentDOM.createDocument();

      // Flag module as having been loaded
      mLoadedLibFormalNamesSet.add(lModuleComponent.mFormalName);

      // Load modules nested libraries
      mergeNestedLibraries(
        lModuleComponent.mRawComponentDOM // pSourceDOM
      , true // pImportChecking
      , "--m" // pNamespaceContext
      , lModuleContext
      );

    }

    // Tidy up
    mLibNameToUsedLibComponentMap = null;
    mLoadedLibFormalNamesSet = null;

  }


  private final void mergeNestedLibraries(
    final DOM pSourceDOM
  , final boolean pImportChecking
  , final String pNamespaceContext
  , final String pSourceContext
  )
  throws
    ExModule
  , ExInternal
  {

    int lLibCount = 0;

    // Get library list
    DOMList lLibraryDOMList = pSourceDOM.getUL(
      "/xs:schema/xs:annotation/xs:appinfo/fm:module/fm:library-list/fm:library");

    // Process library merges
    DOM lLibraryDOM;
    LIB_MERGE_LOOP: while((lLibraryDOM = lLibraryDOMList.popHead())!=null) {

      lLibCount++;

      // Extract library name
      String lLibraryName = lLibraryDOM.value();

      // Get library component
      LibComponent lLibComponent = getLibComponentByName(
        lLibraryName
      , pNamespaceContext+"l"+lLibCount
      , pSourceContext
      , false
      );

      // Skip library when already merged
      if(mLoadedLibFormalNamesSet.contains(lLibComponent.mFormalName)) {
        continue LIB_MERGE_LOOP;
      }

      // Flag library as loading
      mLoadedLibFormalNamesSet.add(lLibComponent.mFormalName);

      // Define library context
      String lLibraryContext = pSourceContext+"(lib)"+lLibComponent.mFormalName;

      // Merge library nodes to target
      mergeSchemaNodeRecursive(
        mModuleMergeTargetDOM
      , lLibComponent.mRawComponentDOM
      , lLibraryContext+"/xs:schema"
      , null // pForFatherType
      , false // pForFatherWasInSchema
      , true // pEnableModuleMetaMerging
      , false // pSourceIsModuleMete
      , pImportChecking
      , lLibraryName
      );

      // Merge this libraries nested libraries
      mergeNestedLibraries(
        lLibComponent.mRawComponentDOM // pSourceDOM
      , pImportChecking // pImportChecking
      , pNamespaceContext+"l"+lLibCount // pNamespaceContext
      , lLibraryContext
      );

    } // end LIB_MERGE_LOOP

  } // end mergeNestedLibraries

  private final void mergeSchemaNodeRecursive(
    final DOM pParentTargetNode
  , final DOM pParentSourceNode
  , final String pParentSourceContext
  , final String pForFatherType
  , final boolean pForFatherWasInSchema
  , final boolean pEnableModuleMetaMerging
  , final boolean pSourceIsModuleMeta
  , final boolean pInSchemaChecking
  , final String pSourceModuleName
  )
  throws
    ExModule
  , ExInternal
  {

    // Fine merge (transfer) attributes
    mergeTransferAttrs(pParentTargetNode, pParentSourceNode);

    // Process merge child nodes
    DOMList lMergeChildDOMList = pParentSourceNode.getChildElements();
    DOM lMergeChildDOM;
    MERGE_CHILD_LOOP: while((lMergeChildDOM=lMergeChildDOMList.popHead())!=null) {

      // Get child element name
      String lMergeChildNameIntern = lMergeChildDOM.getName().intern();
      String lMergeChildContext = pParentSourceContext+'/'+lMergeChildNameIntern;

      // Merge element definition
      if(lMergeChildNameIntern=="xs:element") {
        mergeElementDefinition(
        pParentTargetNode
        , pParentSourceNode
        , lMergeChildDOM
        , lMergeChildContext
        , pInSchemaChecking
        , pSourceModuleName
        );
      }

      // Merge complexType definition
      else if(
         lMergeChildNameIntern=="xs:complexType"
      || lMergeChildNameIntern=="xs:simpleType"
      ) {
        mergeSimpleOrComplexTypeDefinition(
        pParentTargetNode
        , pParentSourceNode
        , lMergeChildDOM
        , lMergeChildContext
        , lMergeChildNameIntern
        , pForFatherType
        , pForFatherWasInSchema
        , pInSchemaChecking
        , pSourceModuleName
        );
      }

      // Merge complexType definition
      else if(
         lMergeChildNameIntern=="xs:extension"
      || lMergeChildNameIntern=="xs:restriction"
      ) {
        mergeRestrictionExtensionDefinition(
        pParentTargetNode
        , pParentSourceNode
        , lMergeChildDOM
        , lMergeChildContext
        , lMergeChildNameIntern
        //, pForFatherType
        //, pForFatherWasInSchema
        , pInSchemaChecking
        , pSourceModuleName
        );
      }

      // Merge or clone sub-tree singleton nodes
     else if(
         lMergeChildNameIntern=="xs:sequence"
      || lMergeChildNameIntern=="xs:dd"
      ) {
        mergeAndRecurseOrCloneAndStop(
        pParentTargetNode
        , pParentSourceNode
        , lMergeChildDOM
        , lMergeChildContext
        , lMergeChildNameIntern
        , pForFatherType
        , pForFatherWasInSchema
        , pEnableModuleMetaMerging
        , pSourceIsModuleMeta
        , pInSchemaChecking
        , pSourceModuleName
        );
      }

      // Module meta-data - Merge named module sections (no recurse)
     else if(
         lMergeChildNameIntern=="fm:action"
      || lMergeChildNameIntern=="fm:set-buffer"
      || lMergeChildNameIntern=="fm:template"
      || lMergeChildNameIntern=="fm:query"
      || lMergeChildNameIntern=="fm:api"
      || lMergeChildNameIntern=="fm:entry-theme"
      || lMergeChildNameIntern=="fm:storage-location"
      || lMergeChildNameIntern=="fm:file-storage-location"
      || lMergeChildNameIntern=="fm:map-set"
      || lMergeChildNameIntern=="fm:security-rule"
      || lMergeChildNameIntern=="fm:pagination-definition"
      || lMergeChildNameIntern=="fm:client-visibility-rule"
      ) {

        if(!pEnableModuleMetaMerging) {
          continue MERGE_CHILD_LOOP;
        }

        mergeNamedSection(
        pParentTargetNode
        , pParentSourceNode
        , lMergeChildDOM
        , lMergeChildContext
        , lMergeChildNameIntern
        , pForFatherType
        , pForFatherWasInSchema
        , pEnableModuleMetaMerging
        , pSourceIsModuleMeta
        , pInSchemaChecking
        , false // pRecurse
        , pSourceModuleName
        );
      }

      //Elements which are not merged together, and brought in entirely from a library - overloading by main module is only allowed if a flag is set on the library element
      else if(
        lMergeChildNameIntern=="fm:xpath"
      ) {
        mergeNamedElementOrValidateOverload("name", pParentTargetNode, lMergeChildDOM, lMergeChildContext, lMergeChildNameIntern, pSourceModuleName);
      }

     else if(
         lMergeChildNameIntern=="fm:css"
      ) {

        if(!pEnableModuleMetaMerging) {
          continue MERGE_CHILD_LOOP;
        }

        mergeCSSItem(
        pParentTargetNode
        , pParentSourceNode
        , lMergeChildDOM
        , lMergeChildContext
        , lMergeChildNameIntern
        , pForFatherType
        , pForFatherWasInSchema
        , pEnableModuleMetaMerging
        , pSourceIsModuleMeta
        , pInSchemaChecking
        , false // pRecurse
        , pSourceModuleName
        );
      }

      // Module security rules
     else if(
         lMergeChildNameIntern=="fm:mode-rule"
      || lMergeChildNameIntern=="fm:view-rule"
      ) {

        if(!pEnableModuleMetaMerging) {
          continue MERGE_CHILD_LOOP;
        }

        mergeSecurityRule(
          pParentTargetNode
        , pParentSourceNode
        , lMergeChildDOM
        , lMergeChildContext
        , lMergeChildNameIntern
        , pSourceModuleName
        );
      }

      // Module meta-data - Merge named module sections (with recurse)
     else if(
         lMergeChildNameIntern=="fm:db-interface"
      || lMergeChildNameIntern=="fm:state"
      ) {

        if(!pEnableModuleMetaMerging) {
          continue MERGE_CHILD_LOOP;
        }

        mergeNamedSection(
          pParentTargetNode
        , pParentSourceNode
        , lMergeChildDOM
        , lMergeChildContext
        , lMergeChildNameIntern
        , pForFatherType
        , pForFatherWasInSchema
        , pEnableModuleMetaMerging
        , pSourceIsModuleMeta
        , pInSchemaChecking
        , true // pRecurse
        , pSourceModuleName
        );
      }

      // Module meta-data - Merge or clone sub-tree unnamed module sections (no recurse)
     else if(
         lMergeChildNameIntern=="fm:set-page"
      || lMergeChildNameIntern=="fm:display-attr-list"
      || lMergeChildNameIntern=="fm:table"
      ) {

        if(!pEnableModuleMetaMerging) {
          continue MERGE_CHILD_LOOP;
        }

        mergeOrCloneThenStop(
          pParentTargetNode
        , pParentSourceNode
        , lMergeChildDOM
        , lMergeChildContext
        , lMergeChildNameIntern
        , pForFatherType
        , pForFatherWasInSchema
        , pEnableModuleMetaMerging
        , pSourceIsModuleMeta
        , pInSchemaChecking
        );

      }

      // Module meta-data - Merge (or create single node) module sections
     else if(
         lMergeChildNameIntern=="xs:annotation"
      || lMergeChildNameIntern=="xs:appinfo"
      || lMergeChildNameIntern=="fm:module"
      || lMergeChildNameIntern=="fm:storage-location-list"
      || lMergeChildNameIntern=="fm:action-list"
      || lMergeChildNameIntern=="fm:state-list"
      || lMergeChildNameIntern=="fm:db-interface-list"
      || lMergeChildNameIntern=="fm:presentation"
      || lMergeChildNameIntern=="fm:template-list"
      || lMergeChildNameIntern=="fm:map-set-list"
      || lMergeChildNameIntern=="fm:security-list"
      || lMergeChildNameIntern=="fm:pagination-definition-list"
      || lMergeChildNameIntern=="fm:client-visibility-rule-list"
      || lMergeChildNameIntern=="fm:css-list"
      || lMergeChildNameIntern=="fm:xpath-list"
      ) {

        if(!pEnableModuleMetaMerging) {
          continue MERGE_CHILD_LOOP;
        }

        mergeOrCreateSingleNodeThenRecurse(
          pParentTargetNode
        , pParentSourceNode
        , lMergeChildDOM
        , lMergeChildContext
        , lMergeChildNameIntern
        , pForFatherType
        , pForFatherWasInSchema
        , pEnableModuleMetaMerging
        , pSourceIsModuleMeta
        , pInSchemaChecking
        , pSourceModuleName
        );
      }

      // Module meta-data - Replace sections that should only exist in main module
     else if(
         lMergeChildNameIntern=="fm:header"
      || lMergeChildNameIntern=="fm:control"
      || lMergeChildNameIntern=="fm:library-list"
      || lMergeChildNameIntern=="fm:entry-theme-list"
      ) {

        if(!pEnableModuleMetaMerging || !pSourceIsModuleMeta) {
          continue MERGE_CHILD_LOOP;
        }

        mergeReplaceSingleNode(
          pParentTargetNode
        , pParentSourceNode
        , lMergeChildDOM
        , lMergeChildContext
        , lMergeChildNameIntern
        , pForFatherType
        , pForFatherWasInSchema
        , pEnableModuleMetaMerging
        , pSourceIsModuleMeta
        , pInSchemaChecking
        );
      }

      // Ignored nodes
      else if(
         lMergeChildNameIntern=="xs:import"
        || lMergeChildNameIntern=="xs:include"
        || lMergeChildNameIntern=="xs:xx"
      ) {
        continue MERGE_CHILD_LOOP;
      }

      // Unexpected node
      else {
        throw new ExInternal("Unexpected XMLSchema node "+lMergeChildNameIntern+" in "+lMergeChildContext);
      }

    } // end MERGE_CHILD_LOOP

  } // end mergeSchemaNodeRecursive

  private final void mergeElementDefinition(
    final DOM pParentTargetNode
  , final DOM pParentSourceNode
  , final DOM pSourceElementDOM
  , final String pSourceElementContext
  , final boolean pInSchemaChecking
  , final String pSourceModuleName
  )
  throws
    ExModule
  , ExInternal
  {

    // Get element name or reference
    String lName = pSourceElementDOM.getAttrOrNull("name");
    String lRef  = pSourceElementDOM.getAttrOrNull("ref");
    String lNameContext = pSourceElementContext+'('+(lName!=null ? lName : lRef)+')';
    if(lName!=null && lRef!=null) {
      throw new ExModule("Attrs name/ref cannot be specified together in "+lNameContext);
    }

    // Identify valid target complement element definition for name
    DOMList lComplementList;
    String lForeFatherType;
    if(lName!=null) {

      // Ensure a ref element is not in target
      if(pParentTargetNode.getElementsByAttrValue("xs:element", "ref", lName).getLength()!=0) {
        throw new ExModule("Previous element definition defined as Attr ref (not name) in "+lNameContext);
      }

      // Search for complementing element definition in target
      lComplementList = pParentTargetNode.getElementsByAttrValue("xs:element", "name", lName);

      // Set element type used for nested validation
      lForeFatherType = pSourceElementDOM.getAttrOrNull("type");

    }

    // Identify valid target complement element definition for name
    else if(lRef!=null) {

      // Ensure a ref element is not in target
      if(pParentTargetNode.getElementsByAttrValue("xs:element", "name", lRef).getLength()!=0) {
        throw new ExModule("Previous element definition defined as Attr name (not ref) in "+lNameContext);
      }

      // Search for complementing element definition in target
      lComplementList = pParentTargetNode.getElementsByAttrValue("xs:element", "ref", lName);

      // Set element type used for nested validation
      lForeFatherType = "*ref*";

    }

    // Invalid element name or ref attribute
    else {
      throw new ExModule("Attrs name/ref not found in "+pSourceElementContext);
    }

    // Validate complement node count
    int lComplementCount = lComplementList.getLength();
    if(lComplementCount>1) {
      throw new ExModule("Duplicate element definitions located in schema/module/library when merging "
        +lNameContext);
    }

    // When no complement target node found
    if(lComplementCount==0) {

      // Ensure missing schema definitions trapped
      if(pInSchemaChecking && pSourceElementDOM.hasAttr("fox:inSchema")) {
        throw new ExModule("Element definition has fox:inSchema, but not in schema, in "+lNameContext);
      }

      // Merge (clone copy) new element definition
      DOM lCopiedNode = pSourceElementDOM.copyToParent(pParentTargetNode);
      setSourceModuleAttr(lCopiedNode, pSourceModuleName);
    }

    // When complement target node found
    else {

      DOM lComplementTargetDOM = lComplementList.item(0);

      // Schema imported validate
      boolean lIsInSchema = false;
      if(pInSchemaChecking) {

        boolean requiresImport = pSourceElementDOM.hasAttr("fox:inSchema");
        lIsInSchema = lComplementTargetDOM.hasAttr(IMPORTED);

        // Ensure non-marked schema definition elements trapped
        if(lIsInSchema && !requiresImport) {
          throw new ExModule("Element in schema but not defined with fox:inSchema, in "+lNameContext);
        }

      }

      // Validate element type
      if(!pSourceElementDOM.getAttr("type").equals(lComplementTargetDOM.getAttr("type"))) {
        throw new ExModule("Element definition type not the same as previous definition in "+ lNameContext);
      }

      setSourceModuleAttr(lComplementTargetDOM, pSourceModuleName);

      // Recurse into merged element definition source/target
      mergeSchemaNodeRecursive(
        lComplementTargetDOM
      , pSourceElementDOM
      , lNameContext
      , lForeFatherType
      , lIsInSchema // pForFatherWasInSchema
      , false // pEnableModuleMetaMerging - not applicable once in element
      , false // pSourceIsModuleMeta - not applicable once in element
      , pInSchemaChecking
      , pSourceModuleName
      );

    } // end when complement target node found

  } // end mergeElementDefinition

  private final void mergeSimpleOrComplexTypeDefinition(
    final DOM pParentTargetNode
  , final DOM pParentSourceNode
  , final DOM pSourceElementDOM
  , final String pSourceElementContext
  , final String pSourceElementNameIntern
  , final String pForFatherType
  , final boolean pForFatherWasInSchema
  , final boolean pInSchemaChecking
  , final String pSourceModuleName
  )
  throws
    ExModule
  , ExInternal
  {

    // Get complex name or reference
    String lName = pSourceElementDOM.getAttrOrNull("name");
    String lNameContext;

    // Identify valid target complement definition (with name)
    DOMList lComplementList;
    if(lName!=null) {

      lNameContext = pSourceElementContext+'('+lName+')';

      // Search for complementing definition in target
      lComplementList = pParentTargetNode.getElementsByAttrValue(pSourceElementNameIntern, "name", lName);

    }

    // Identify valid target complement local definition (no name)
    else {

      lNameContext = pSourceElementContext;

      // Check against xs:simpleType/xs:complexType mismatch
      String lNegativeNameIntern =
        (pSourceElementNameIntern=="xs:complexType" ? "xs:simpleType": "xs:complexType");
      if(pParentTargetNode.getUL(lNegativeNameIntern).getLength()!=0) {
        throw new ExModule("xs:simpleType/xs:complexType mismatch with previous definition, in "+lNameContext);
      }

      // Search for complementing element definition in target
      lComplementList = pParentTargetNode.getUL(pSourceElementNameIntern);

    }

    // Check for incompatible type override
    if(pForFatherType!=null) {
      throw new ExModule(pSourceElementNameIntern+" incompatible with previous definition type='"
      +pForFatherType+"', in "+ lNameContext);
    }

    // Validate complement node count
    int lComplementCount = lComplementList.getLength();
    if(lComplementCount>1) {
      throw new ExModule("Duplicate "+pSourceElementNameIntern+" definitions located in schema/module/library when merging "
        +lNameContext);
    }

    // When no complement target node found
    if(lComplementCount==0) {

      // InSchema elements cannot have simpleType merged onto it
      if(pForFatherWasInSchema && pSourceElementNameIntern=="xs:simpleType") {
        throw new ExModule("SimpleType declaration incompatible with schema definition type='"
        +pForFatherType+"', in "+ lNameContext);
      }

      // Merge (clone copy) new definition
      pSourceElementDOM.copyToParent(pParentTargetNode);

    }

    // When complement target node found
    else {

      // Ignore simple type content when element already loaded
      if(pSourceElementNameIntern=="xs:simpleType") {
        return;
      }

      DOM lComplementTargetDOM = lComplementList.item(0);

      // Recurse into merged definition source/target
      mergeSchemaNodeRecursive(
        lComplementTargetDOM
      , pSourceElementDOM
      , lNameContext
      , (pSourceElementNameIntern=="xs:complexType" ? "**Complex**" : "**Simple**") // pForFatherType
      , false // pForFatherWasInSchema
      , false // pEnableModuleMetaMerging - not applicable once in element
      , false // pSourceIsModuleMeta - not applicable once in element
      , pInSchemaChecking
      , pSourceModuleName
      );

    } // end when complement target node found

  } // end mergeSimpleOrComplexTypeDefinition

  private final void mergeRestrictionExtensionDefinition(
    final DOM pParentTargetNode
  , final DOM pParentSourceNode
  , final DOM pSourceElementDOM
  , final String pSourceElementContext
  , final String pSourceElementNameIntern
  //, final String pForFatherType
  //, final boolean pForFatherWasInSchema
  , final boolean pInSchemaChecking
  , final String pSourceModuleName
  )
  throws
    ExModule
  , ExInternal
  {

    // Check against xs:extension/xs:restriction mismatch
    String lNegativeNameIntern = (pSourceElementNameIntern == "xs:extension" ? "xs:restriction": "xs:extension");
    if(pParentTargetNode.getUL(lNegativeNameIntern).getLength()!=0) {
      throw new ExModule("xs:extension/xs:restriction mismatch with previous definition, in "
      +pSourceElementContext);
    }

    // Search for complementing element definition in target
    DOMList lComplementList = pParentTargetNode.getUL(pSourceElementNameIntern);

    // Validate complement node count
    int lComplementCount = lComplementList.getLength();
    if(lComplementCount>1) {
      throw new ExModule("Duplicate "+pSourceElementNameIntern+" definitions located in schema/module/library when merging "
        +pSourceElementContext);
    }

    // When no complement target node found
    if(lComplementCount==0) {
      throw new ExModule("Merge mismatch on "+pSourceElementNameIntern+" definition , in "+ pSourceElementContext);
    }

    // When complement target node found
    else {

      DOM lComplementTargetDOM = lComplementList.item(0);

      // Check bases the same
      if(!pSourceElementDOM.getAttr("base").equals(lComplementTargetDOM.getAttr("base"))) {
        throw new ExModule("previous base mismatch on "+pSourceElementNameIntern+" definition , in "+ pSourceElementContext);
      }

      // Recurse into merged definition source/target
      mergeSchemaNodeRecursive(
        lComplementTargetDOM
      , pSourceElementDOM
      , pSourceElementContext
      , null // pForFatherType
      , false // pForFatherWasInSchema
      , false // pEnableModuleMetaMerging - not applicable once in element
      , false // pSourceIsModuleMeta - not applicable once in element
      , pInSchemaChecking
      , pSourceModuleName
      );

    } // end when complement target node found

  } // end mergeRestrictionExtensionDefinition

  private final void mergeAndRecurseOrCloneAndStop(
    final DOM pParentTargetNode
  , final DOM pParentSourceNode
  , final DOM pSourceElementDOM
  , final String pSourceElementContext
  , final String pSourceElementName
  , final String pForFatherType
  , final boolean pForFatherWasInSchema
  , final boolean pEnableModuleMetaMerging
  , final boolean pSourceIsModuleMeta
  , final boolean pInSchemaChecking
  , final String pSourceModuleName
  )
  throws
    ExModule
  , ExInternal
  {

    // Identify valid target complement definition
    DOMList lComplementList = pParentTargetNode.getUL(pSourceElementName);

    // Validate complement node count
    int lComplementCount = lComplementList.getLength();
    if(lComplementCount>1) {
      throw new ExInternal("Duplicate "+pSourceElementName+" definitions located in schema/module/library when merging "
        +pSourceElementContext);
    }

    // When no complement target node found
    if(lComplementCount==0) {

      // Merge (clone copy) new definition
      pSourceElementDOM.copyToParent(pParentTargetNode);

    }

    // When complement target node found
    else {

      DOM lComplementTargetDOM = lComplementList.item(0);

      // Recurse into merged definition source/target
      mergeSchemaNodeRecursive(
        lComplementTargetDOM
      , pSourceElementDOM
      , pSourceElementContext
      , pForFatherType
      , pForFatherWasInSchema
      , pEnableModuleMetaMerging
      , pSourceIsModuleMeta
      , pInSchemaChecking
      , pSourceModuleName
      );

    } // end when complement target node found

  } // end mergeAndRecurseOrCloneAndStop

  private final void mergeOrCloneThenStop(
    final DOM pParentTargetNode
  , final DOM pParentSourceNode
  , final DOM pSourceElementDOM
  , final String pSourceElementContext
  , final String pSourceElementName
  , final String pForFatherType
  , final boolean pForFatherWasInSchema
  , final boolean pEnableModuleMetaMerging
  , final boolean pSourceIsModuleMeta
  , final boolean pInSchemaChecking
  )
  throws
    ExModule
  , ExInternal
  {

    // Identify valid target complement definition
    DOMList lComplementList = pParentTargetNode.getUL(pSourceElementName);

    // Validate complement node count
    int lComplementCount = lComplementList.getLength();
    if(lComplementCount>1) {
      throw new ExInternal("Duplicate "+pSourceElementName+" definitions located in schema/module/library when merging "
        +pSourceElementContext);
    }

    // When no complement target node found
    if(lComplementCount==0) {

      // Merge (clone copy) new definition
      pSourceElementDOM.copyToParent(pParentTargetNode);

    }

    // When complement target node found
    else {

      DOM lComplementTargetDOM = lComplementList.item(0);

      // Recurse into merged definition source/target
      mergeTransferAttrs(
        lComplementTargetDOM
      , pSourceElementDOM
      );

    } // end when complement target node found

  } // end mergeOrCloneThenStop

  private final void mergeNamedSection(
    final DOM pParentTargetNode
  , final DOM pParentSourceNode
  , final DOM pSourceElementDOM
  , final String pSourceElementContext
  , final String pSourceElementName
  , final String pForFatherType
  , final boolean pForFatherWasInSchema
  , final boolean pEnableModuleMetaMerging
  , final boolean pSourceIsModuleMeta
  , final boolean pInSchemaChecking
  , final boolean pRecurse
  , final String pSourceModuleName
  )
  throws
    ExModule
  , ExInternal
  {

    // Get section name
    String lName = pSourceElementDOM.getAttrOrNull("name");
    String lNameContext = pSourceElementContext+'('+lName+')';
    if(lName==null) {
      throw new ExModule("Name not specified in "+lNameContext);
    }

    // Identify valid target complement section for name
    DOMList lComplementList = pParentTargetNode.getElementsByAttrValue(pSourceElementName, "name", lName);

    // Validate complement node count
    int lComplementCount = lComplementList.getLength();
    if(lComplementCount>1) {
      throw new ExInternal("Duplicate "+pSourceElementName+" definitions located in schema/module/library when merging "
        +pSourceElementContext);
    }

    // When no complement target node found
    if(lComplementCount==0) {

      // Merge (clone copy) new definition
      DOM lCopiedNode = pSourceElementDOM.copyToParent(pParentTargetNode);
      setSourceModuleAttr(lCopiedNode, pSourceModuleName);
    }
    else {
      // When complement target node found
      DOM lComplementTargetDOM = lComplementList.item(0);

      // When no recurse wanted - just transfer attributes
      if(!pRecurse) {
        // Make source version is dominant when target allows stub-overload and source non-stub
        if(lComplementTargetDOM.hasAttr("stub-overload") && ! pSourceElementDOM.hasAttr("stub-overload")) {

          DOM lOldComplementTargetDOM = lComplementTargetDOM;
          DOM lNewComplementTargetDOM = lComplementTargetDOM.replaceThisWith(
            pSourceElementDOM.clone(true, lComplementTargetDOM)
          );

          mergeTransferAttrs(lNewComplementTargetDOM, lOldComplementTargetDOM);

        }
        else {
          mergeTransferAttrs(lComplementTargetDOM, pSourceElementDOM);
        }
      }
      else {
        // Recurse into merged definition source/target

        mergeSchemaNodeRecursive(
          lComplementTargetDOM
        , pSourceElementDOM
        , pSourceElementContext
        , pForFatherType
        , pForFatherWasInSchema
        , pEnableModuleMetaMerging
        , pSourceIsModuleMeta
        , pInSchemaChecking
        , pSourceModuleName
        );
      }

      setSourceModuleAttr(lComplementTargetDOM, pSourceModuleName);
    } // end when complement target node found

  } // end mergeNamedSection

  /**
   * Merges in a named element from a library module. If the element is defined in the target (main) module, the target
   * definition is retained only if the library definition is marked up with <tt>overload="allowed"</tt>. If overloading is
   * not allowed, an exception is thrown.
   *
   * @param pIdentifyingAttribute Attribute to use to determine the element's name.
   * @param pParentTargetNode Target container in the merge DOM.
   * @param pSourceElementDOM Element from nested library.
   * @param pSourceElementContext For debug.
   * @param pSourceElementName Name of element being merged.
   * @param pSourceModuleName Name of module currently being processed.
   * @throws ExModule
   */
  private final void mergeNamedElementOrValidateOverload(String pIdentifyingAttribute, DOM pParentTargetNode, DOM pSourceElementDOM,
                                                         String pSourceElementContext, String pSourceElementName, String pSourceModuleName)
  throws ExModule {

    //Establish identifier for this element (i.e. fm:xpath name attribute)
    String lIdentifier = pSourceElementDOM.getAttrOrNull(pIdentifyingAttribute);
    String lNameContext = pSourceElementContext+'('+lIdentifier+')';
    if(lIdentifier == null) {
      throw new ExModule(pIdentifyingAttribute + " not specified in " + lNameContext);
    }

    //Look up elements with the same name in the main module's list container (i.e. fm:xpath-list)
    DOMList lComplementList = pParentTargetNode.getElementsByAttrValue(pSourceElementName, pIdentifyingAttribute, lIdentifier);

    //Sanity check there are no duplicate definitions
    int lComplementCount = lComplementList.getLength();
    if(lComplementCount > 1) {
      throw new ExInternal("Duplicate "+pSourceElementName+" definitions located in schema/module/library when merging " +pSourceElementContext);
    }

    if(lComplementCount == 0) {
      //Definition is in library and not in main so it can be merged in without problems
      DOM lCopiedNode = pSourceElementDOM.copyToParent(pParentTargetNode);
      setSourceModuleAttr(lCopiedNode, pSourceModuleName);
    }
    else {
      //Definition is in both library and in main - check that the library allows an overload
      //(if it does, take no action and keep the definition from the main module)
      if(!"allowed".equals(pSourceElementDOM.getAttr("overload"))) {
        throw new ExModule("Overloading not allowed for " + pSourceElementName + " with " + pIdentifyingAttribute + " of '" + lIdentifier + "' (defined in " + pSourceModuleName + ")");
      }
    }
  }

  private static final void setSourceModuleAttr(DOM pTargetNode, String pSourceModuleName) {

    String lCurrentAttrVal = pTargetNode.getAttr(MERGE_SOURCE_MODULE_ATTR_NAME);
    if(!XFUtil.isNull(lCurrentAttrVal)) {
      lCurrentAttrVal += ", ";
    }

    pTargetNode.setAttr(MERGE_SOURCE_MODULE_ATTR_NAME, lCurrentAttrVal + pSourceModuleName);
  }


  private final void mergeCSSItem(
    final DOM pParentTargetNode
  , final DOM pParentSourceNode
  , final DOM pSourceElementDOM
  , final String pSourceElementContext
  , final String pSourceElementName
  , final String pForFatherType
  , final boolean pForFatherWasInSchema
  , final boolean pEnableModuleMetaMerging
  , final boolean pSourceIsModuleMeta
  , final boolean pInSchemaChecking
  , final boolean pRecurse
  , final String pSourceModuleName
  )
  throws
    ExModule
  , ExInternal
  {
    // Get section name
    String lName = pSourceElementDOM.value();
    String lNameContext = pSourceElementContext+'('+lName+')';
    if(lName==null) {
      throw new ExModule("Name not specified in "+lNameContext);
    }

    // Identify valid target complement section for name
    DOMList lComplementList = pParentTargetNode.getUL(pSourceElementName);
    int l = lComplementList.size();
    DOM lNode;
    for(int i=l-1; i>=0; i--) {
      lNode = lComplementList.get(i);
      if(!lName.equals(lNode.value())) {
        lComplementList.remove(i);
      }
    }

    // Validate complement node count
    int lComplementCount = lComplementList.getLength();
    if(lComplementCount > 1) {
      throw new ExInternal("Duplicate "+pSourceElementName+" definitions located in schema/module/library when merging "
        +pSourceElementContext);
    }

    // When no complement target node found
    if(lComplementCount == 0) {
      // Merge (clone copy) new definition
      pSourceElementDOM.copyToParent(pParentTargetNode);
    }
  }

  private final void mergeSecurityRule(
    final DOM pParentTargetNode
  , final DOM pParentSourceNode
  , final DOM pSourceElementDOM
  , final String pSourceElementContext
  , final String pSourceElementName
  , final String pSourceModuleName
  )
  throws
    ExModule
  , ExInternal
  {

    // Get section name
    String lName = pSourceElementDOM.getAttrOrNull("namespace");
    String lNameContext = pSourceElementContext+'('+lName+')';
    if(lName==null) {
      throw new ExModule("Namespace not specified in "+lNameContext);
    }

    // Identify valid target complement section for name
    DOMList lComplementList = pParentTargetNode.getElementsByAttrValue(pSourceElementName, "namespace", lName);

    // Merge (clone copy) new definition
    DOM lCopiedNode = pSourceElementDOM.copyToParent(pParentTargetNode);
    setSourceModuleAttr(lCopiedNode, pSourceModuleName);

  } // end mergeSecurityRule

  private final void mergeOrCreateSingleNodeThenRecurse(
    final DOM pParentTargetNode
  , final DOM pParentSourceNode
  , final DOM pSourceElementDOM
  , final String pSourceElementContext
  , final String pSourceElementName
  , final String pForFatherType
  , final boolean pForFatherWasInSchema
  , final boolean pEnableModuleMetaMerging
  , final boolean pSourceIsModuleMeta
  , final boolean pInSchemaChecking
  , final String pSourceModuleName
  )
  throws
    ExModule
  , ExInternal
  {

    // Identify valid target complement definition
    DOMList lComplementList = pParentTargetNode.getUL(pSourceElementName);

    // Validate complement node count
    int lComplementCount = lComplementList.getLength();
    if(lComplementCount>1) {
      throw new ExInternal("Duplicate "+pSourceElementName+" definitions located in schema/module/library when merging "
        +pSourceElementContext);
    }

    // When no complement target node found - create one
    DOM lComplementTargetDOM;
    if(lComplementCount==0) {
      lComplementTargetDOM =pParentTargetNode.addElem(pSourceElementName);
    }

    // When complement target node found
    else {
      lComplementTargetDOM = lComplementList.item(0);
    }

    // Recurse into merged definition source/target
    mergeSchemaNodeRecursive(
      lComplementTargetDOM
    , pSourceElementDOM
    , pSourceElementContext
    , pForFatherType
    , pForFatherWasInSchema
    , pEnableModuleMetaMerging
    , pSourceIsModuleMeta
    , pInSchemaChecking
    , pSourceModuleName
    );

  } // end mergeOrCreateSingleNodeThenRecurse

  private final void mergeReplaceSingleNode(
    final DOM pParentTargetNode
  , final DOM pParentSourceNode
  , final DOM pSourceElementDOM
  , final String pSourceElementContext
  , final String pSourceElementName
  , final String pForFatherType
  , final boolean pForFatherWasInSchema
  , final boolean pEnableModuleMetaMerging
  , final boolean pSourceIsModuleMeta
  , final boolean pInSchemaChecking
  )
  throws
    ExModule
  , ExInternal
  {

    // Identify valid target complement definition
    DOMList lComplementList = pParentTargetNode.getUL(pSourceElementName);

    // Validate complement node count
    int lComplementCount = lComplementList.getLength();
    if(lComplementCount>1) {
      throw new ExInternal("Duplicate "+pSourceElementName+" definitions located in schema/module/library when merging "
        +pSourceElementContext);
    }

    // When complement target node found - remove it
    if(lComplementCount==1) {
      lComplementList.item(0).remove();;
    }

    // Merge (clone copy) new definition
    pSourceElementDOM.copyToParent(pParentTargetNode);

  } // end mergeReplaceSingleNode

  private final void mergeTransferAttrs(
    final DOM pTargetDOM
  , final DOM pSourceDOM
  )
  throws ExInternal
  {

    List lAttrList = pSourceDOM.getAttrNames();
    int l = lAttrList.size();

    MERGE_LOOP: for(int i=0; i<l; i++) {

      String attr = (String) lAttrList.get(i);

      // Non-namespace attributes are not merged
      int p = attr.indexOf(':');
      if(p==-1) {
        continue MERGE_LOOP;
      }

      // Extract attr namespace
      String ns = attr.substring(0, p);

      // Special fox namespace is not merged when already in target
      if(ns.equals("fox") && pTargetDOM.hasAttr(attr)) {
        continue MERGE_LOOP;
      }

      // Merge Attribute
      pTargetDOM.setAttr(attr, pSourceDOM.getAttr(attr));
    }

  } // end mergeTransferAttrs

  public final DOM getModuleRawDOMOrNull() {
    return mModuleRawDOM;
  }

  public final DOM getModuleMergerDOMOrNull() {
    return mModuleMergeTargetDOM;
  }

  /** Diagnostic information about Model DOM and NodeInfo types (developers system menu) */
  public final String getDiagnsticModelDOMInfo() {
    StringBuffer lSB = new StringBuffer();
    SortedMap<String, NodeInfo> mOrder = new TreeMap<>(mAbsolutePathToNodeInfoMap);
    Iterator i = mOrder.entrySet().iterator();
    while(i.hasNext()) {
      Map.Entry e = (Map.Entry) i.next();
      NodeInfo ni = (NodeInfo) e.getValue();
      lSB.append(ni.getContainsCollection() ? "C.Col\t" : "\t");
      lSB.append(ni.isListContainer() ? "C.List\t" : "\t");
      //lSB.append(ni.getIsList() ? "IsList\t" : "\t");
      lSB.append(ni.getIsItem() ? "IsItem\t" : "\t");
      lSB.append("NT="+ni.getNodeType()+"\t");
      lSB.append(e.getKey());
      lSB.append("   ("+ni.getDataType()+")");
      lSB.append(" "+ni.getMinCardinality()+".."+ni.getMaxCardinality());
      lSB.append("\n");
    }
    return lSB.toString();
  }

  public final DOM getModuleMergerTypeExpandedDOMOrNull() {
    return mModuleTypeExpandedDOM;
  }

  public List<CSSListItem> getStyleSheets() {
    return mStyleSheets;
  }

  /**
   * Get module header or control attribute
   * EG: x = getHeaderControlAttribute("fm:title");
   **/
  public String getHeaderControlAttribute(String pAttrName) {
    return mHashHeader.get(pAttrName);
  }

  public final MapSetDefinition getMapSetDefinitionByName(String pName) {
    if(pName == null) {
      throw new ExInternal("Null passed to Mod.getMapSetDefinitionByName");
    }
    MapSetDefinition lMapSetDfn = mMapSetDefinitions.get(pName);
    if(lMapSetDfn == null) {
      throw new ExInternal("Map Set '"+pName+"' not defined in Module "+getName());
    }
    return lMapSetDfn;
  }

  public DataDefinition getDataDefinitionByName(String pName) {
    if(pName == null) {
      throw new ExInternal("Null passed to Mod.getDataDefinitionByName");
    }
    DataDefinition lDataDefinition = mDataDefinitions.get(pName);
    if(lDataDefinition == null) {
      throw new ExInternal("Data Definition '"+pName+"' not defined in Module "+getName());
    }
    return lDataDefinition;
  }

  public final PagerDefinition getPagerDefinitionByName(String pName) {
    if(pName == null) {
      throw new ExInternal("Null passed to Mod.getPagerDefinitionByName");
    }
    PagerDefinition lPagerDfn = mPageDefNameToPageDefMap.get(pName);
    if(lPagerDfn == null) {
      throw new ExInternal("Page control '"+pName+"' not defined in Module "+getName());
    }
    return lPagerDfn;
  }

  /**
   * Gets a client visibility rule definition from this moudle.
   * @param pName Name of the client visibility rule. Cannot be null.
   * @return The client visibility rule matching the given name.
   * @throws ExModule If the client visibility rule is not defined on this module.
   */
  public final ClientVisibilityRule getClientVisibilityRuleByName(String pName)
  throws ExModule {
    if(pName == null) {
      throw new ExInternal("Null passed to Mod.getClientVisibilityRuleByName");
    }
    ClientVisibilityRule lRule = mVisibilityRuleNameToVisibilityRuleMap.get(pName);
    if(lRule == null) {
      throw new ExModule("Visibility rule '"+pName+"' not defined in Module "+getName());
    }
    return lRule;
  }

  /** Converts a non-wellformed fox module string into a wellformed DOM
   *  by converting fox namespaces to be unique
   **/
  public static final DOM parseModuleDOM(StringBuffer pStringBuffer, String pModName,
                                         boolean pIsInitialModule) throws ExModule {

    StringBuffer lResult = new StringBuffer();
    Set<String> lNamespaceSet = new HashSet<String>();

    /*
     * Pattern to get the xmlns: definitions for both fox and fox_global namespaces, with optional URI suffix added by developer.
     * If the URI suffix is specified then it must match the namespace prefix being defined.
     * E.g. xmlns:ns1="http://www.og.dti.gov/fox/ns1" OR  xmlns:ns1="http://www.og.dti.gov/fox" OR
     *      xmlns:ns1="http://www.og.dti.gov/fox_global/ns1 OR xmlns:ns1="http://www.og.dti.gov/fox_global
     *
     * Local namespaces are rewritten with the module name as an additional suffix to enforce uniqueness when DOMs are merged.
     */
    //Note "?:" part of regex means this set of brackets does not count as a capturing group

    Matcher m = gFoxXMLNSPattern.matcher(pStringBuffer);

    while (m.find()) {
      String lPrefix = m.group(1);
      boolean lIsGlobal = "_global".equals(m.group(2));
      boolean lIsFoxModuleNS = "_module".equals(m.group(2));
      String lURISuffix = m.group(3);
      //Don't touch the actual fox namespace definition
      if (!FOX_NS_PREFIX.equals(lPrefix)) {
        if (!XFUtil.isNull(lURISuffix) && !lPrefix.equals(lURISuffix) && !lIsFoxModuleNS) {
          throw new ExModule("Namespace definition prefix (" + lPrefix + ") not matching URI suffix (" + lURISuffix + ")");
        }
        else if (lNamespaceSet.contains(lPrefix)) {
          throw new ExModule("Duplicate namespace prefix declaration across fox/fox_global namespace URIs for prefix " + lPrefix);
        }
        lNamespaceSet.add(lPrefix);
        //Append a suffix if this is the initial module in a library structure and the namespace is non-global, as this
        //will need to be renamed later when other libraries are brought in.
        //(Also - don't rewrite the module namespace URI as this is needed by Saxon for running XPaths against module DOMs)
        if(!lIsFoxModuleNS) {
          m.appendReplacement(lResult,
                              "xmlns:" + lPrefix + "=\"" + (lIsGlobal ? FOX_GLOBAL_NO_NS_URI : FOX_LOCAL_NO_NS_URI) +
                              "/" + lPrefix + (lIsGlobal ? "" : "/" + pModName) +
                              (pIsInitialModule && !lIsGlobal ? INIT_MODULE_LOCAL_SUFFIX : "") + "\"");
        }
      }
    }
    m.appendTail(lResult);

    // Create module dom
    DOM lMod = DOM.createDocumentFromXMLString(lResult.toString());

    //Special case any non-FOX related attributes (xs, xsi...) by forcibly declaring their namespace declarations.
    //This is so when the DOM is copied the declarations are also copied, allowing attributes to be set on the copied
    //DOM where their prefixes would otherwise not be recognised. Ultimately this is a XOM quirk we're working around
    //to do with how XOM maintains its namespace declarations.
    Element lModRootElem = (Element)lMod.getRootElement().getNode();
    int n = lModRootElem.getNamespaceDeclarationCount();
    for (int i = 0; i < n; i++) {
      String lPrefix = lModRootElem.getNamespacePrefix(i);
      if (!lNamespaceSet.contains(lPrefix) && !FOX_MODULE_NS_PREFIX.equals(lPrefix) &&
          !FOX_NS_PREFIX.equals(lPrefix)) {
        lModRootElem.addNamespaceDeclaration(lPrefix, lModRootElem.getNamespaceURI(lPrefix));
      }
    }

    //Force a declaration of the XSI prefix - some modules use it and some don't; problems occur when a non-using module
    //libraries in a using module.
    if (lMod.getURIForNamespacePrefix(XSI_PREFIX) == null) {
      lMod.addNamespaceDeclaration(XSI_PREFIX, XSI_URI);
    }

    return lMod;

  } // parseModuleDOM

  /**
   * Gets the database interface of the given name, throwing an exception if the interface does not exist.
   * @param pDbInterfaceName
   * @return
   */
  public DatabaseInterface getDatabaseInterface(String pDbInterfaceName) {

    DatabaseInterface lDbInterface = mNameToDbInterfaceMap.get(pDbInterfaceName);

    if(lDbInterface == null) {
      throw new ExInternal("Cannot find a db-interface with name " + pDbInterfaceName + " in module " + mModuleName);
    }

    return lDbInterface;
  }

  public List<EntryTheme> getEntryThemes () {
    return new ArrayList<>(mEntryThemes.values());
  }

  public boolean isValidEntryTheme(String pEntryThemeName) {
    return mEntryThemes.containsKey(pEntryThemeName);
  }

  public EntryTheme getEntryTheme(String pEntryThemeName)
  throws ExUserRequest {
    String lEntryThemeName = XFUtil.nvl(pEntryThemeName, getDefaultEntryThemeName());

    EntryTheme lEntryTheme = mEntryThemes.get(lEntryThemeName);

    if(lEntryTheme == null) {
      ExUserRequest lEx = new ExUserRequest("Entry Theme '"+lEntryThemeName+"' not found in module '"+mModuleName+"'");
      lEx.setHttpStatusCode(404);
      throw lEx;
    }

    return lEntryTheme;
  }

  public ArrayList<EntryTheme> getServiceEntryThemes () {
    return new ArrayList<>(mThemeNameToServiceThemeMap.values());
  }

  /**
   * Return a pre parse Presentation Node for a named buffer in this module
   * @param pBufferName Name of the buffer to return
   * @return Pre-parsed Presentation Node
   */
  public BufferPresentationNode getParsedBuffer(String pBufferName) {
    return mParsedBuffers.get(pBufferName);
  }

  /**
   * Get the default buffer for this module
   * @return Pre-parsed Presentation Node
   */
  public BufferPresentationNode getSetPageBuffer() {
    return mSetPageBuffer;
  }

  Multimap<AutoActionType, ActionDefinition> getAutoActionMultimap() {
    return mAutoActionMultimap;
  }

  public HtmlDoctype getDocumentType() {
    return mDocumentType;
  }

  /**
   * Gets the StoredXPathResolver for this module definition. This is a root level StoredXPathResolver with no parent to
   * delegate to.
   * @return StoredXPathResolver for this module.
   */
  public StoredXPathResolver getStoredXPathResolver() {
    return mStoredXPathResolver;
  }

  public void addBulkModuleErrorMessage(String pModuleErrorMessage) {
    mBulkModuleErrorMessages += pModuleErrorMessage;
  }

  public void addBulkModuleWarningMessage(String pModuleWarningMessage) {
    mBulkModuleWarningMessages += pModuleWarningMessage;
  }

  public String getBulkModuleErrorMessages() {
    return mBulkModuleErrorMessages;
  }

  public String getBulkModuleWarningMessages() {
    return mBulkModuleWarningMessages;
  }
}
