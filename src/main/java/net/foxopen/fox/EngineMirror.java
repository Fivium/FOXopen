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


import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import net.foxopen.fox.dom.DOM;
import net.foxopen.fox.dom.DOMList;
import net.foxopen.fox.entrypoint.ComponentManager;
import net.foxopen.fox.entrypoint.FoxGlobals;
import net.foxopen.fox.entrypoint.LocalComponent;
import net.foxopen.fox.ex.ExApp;
import net.foxopen.fox.ex.ExBadPath;
import net.foxopen.fox.ex.ExInternal;
import net.foxopen.fox.ex.ExTooFew;
import net.foxopen.fox.ex.ExTooMany;
import net.foxopen.fox.logging.FoxLogger;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// TODO - NP - This needs a huge rewrite for new resource master stuff and general improvements
public class EngineMirror {
  private static Boolean REFRESHING_ENGINE_MIRROR = false;
  private static final String ENGINE_MIRROR_FOLDER_SUFFIX = "_enginemirror";
  private static final Table<String, String, LocalComponent> APP_COMPONENT_MAP = HashBasedTable.create();

  private static Map<String, Map<String, LocalComponent>> INTERNAL_APP_COMPONENT_MAP = new HashMap<>();
  private static SimpleDateFormat DATE_FORMATTER = new SimpleDateFormat("HH:mm:ss dd-MM-yyyy");
  private static final String MIRROR_FOLDER_RELATIVE = "/WEB-INF/.engine_mirror";
  private static String MIRROR_FOLDER;

  public static final String getMirrorFolder() {
    return FoxGlobals.getInstance().getServletContext().getRealPath(MIRROR_FOLDER_RELATIVE);
  }

  private static boolean gLoadSuccess = false;

  /**
   * Initialise the Engine Mirror, reading any previously cached files in to the EngineMirrorAppComponentMap
   *
   * @param lEngineConfigFilePrefix
   * @return
   */
  public static boolean init(String lEngineConfigFilePrefix) {
    File lMirrorFolder = new File(lEngineConfigFilePrefix + ENGINE_MIRROR_FOLDER_SUFFIX);
    File[] lAppFolders = lMirrorFolder.listFiles();

    if (lAppFolders == null) {
      return false;
    }
    else {
      try {
        //Create a temporary cache
        Table<String, String, LocalComponent> lTemporaryAppComponentMap = HashBasedTable.create();

        // Cache references to each component in the app folders
        for (File lAppfolder : lAppFolders) {
          if (lAppfolder.isDirectory()) {
            DOMList lComponentsDOMList = DOM.createDocument(new File(lAppfolder.getAbsolutePath() + File.separator + "components.xml"), false).getUL("/*/COMPONENT");

            for (DOM lComponentDOM : lComponentsDOMList) {
              LocalComponent lComponent = new LocalComponent(new File(lComponentDOM.get1SNoEx("FILE_PATH")), lComponentDOM.get1SNoEx("TYPE"), lComponentDOM.get1SNoEx("FILE_FORMAT"), true);

              lTemporaryAppComponentMap.put(lAppfolder.getName(), lComponentDOM.get1SNoEx("NAME"), lComponent);
            }
          }
        }

        // Replace the app component cache once all apps are processed
        synchronized(APP_COMPONENT_MAP) {
          APP_COMPONENT_MAP.clear();
          APP_COMPONENT_MAP.putAll(lTemporaryAppComponentMap);
        }

        return true;
      }
      catch (Throwable th) {
        FoxLogger.getLogger().error("Something went wrong loading files from the engine mirror. Removing corrupt folder and skipping load", th);
        XFUtil.deleteDir(lMirrorFolder);
        return false;
      }
    }
  }

  public static boolean populateCacheFromDatabase(String lEngineConfigFilePrefix) {

    synchronized (REFRESHING_ENGINE_MIRROR) {
      if (REFRESHING_ENGINE_MIRROR) {
        //EngineStatus.createOrUpdateMessage("General", "Engine Mirror", "Didn't repopulate", MessageLevel.SUCCESS);
        return false;
      }
      else {
        REFRESHING_ENGINE_MIRROR = true;
      }
    }

    File lTempMirrorFolder = new File(lEngineConfigFilePrefix + ENGINE_MIRROR_FOLDER_SUFFIX + new SimpleDateFormat("yyMMdd HH;mm;ss").format(new Date()));

//    String lDefaultErrorAppMnem = FoxGlobals.getInstance().getFoxEnvironment().getDefaultApp().getErrorComponentName()

    // List<App> lApps = ResourceMater.getAppList();
    /*
     * for (App lApp : lApps) {
     *   String lErrorModuleName = lApp.getErrorComponentName();
     *
     *   List<String> lEngineMirrorComponentNames = lApp.getEngineMirrorComponentNames();
     *   for (String lComponentName : lEngineMirrorComponentNames) {
     *     Row lRow = App.getComponentFromThreadTable(lComponentName);
     *      Clob lClob = pRow.getClob("DATA");
            Blob lBlob = pRow.getBlob("BINDATA");
            String lName = pRow.getString("NAME");
            String lType = pRow.getString("TYPE");
     *
     *     if (lComp.getName().equals(lErrorModuleName)) {
     *       // Store it in the error map and xml file
     *     }
     *     // Store lComp on disk and in the app component xml and map
     *   }
     * }
     */

//    //Safely attempt to copy new mirror files over old ones
//    if (lMirrorFolder.exists()) {
//      // If there's an existing folder, move it to lMirrorFolderCopy
//      XFUtil.copyDir(lMirrorFolder, lMirrorFolderCopy);
//      XFUtil.deleteDir(lMirrorFolder);
//    }
//
//    // Move the temp folder to the actual folder
//    XFUtil.copyDir(lTempMirrorFolder, lMirrorFolder);
//    XFUtil.deleteDir(lTempMirrorFolder);
//
//    if (lMirrorFolderCopy.exists()) {
//      // If we made a copy of the original earlier then clean it up
//      XFUtil.deleteDir(lMirrorFolderCopy);
//    }
//
//    // If fails (this in a catch block)
//      if (lMirrorFolderCopy.exists()) {
//        if (lMirrorFolder.exists()) {
//          XFUtil.deleteDir(lMirrorFolder);
//        }
//        XFUtil.copyDir(lMirrorFolderCopy, lMirrorFolder);
//        XFUtil.deleteDir(lMirrorFolderCopy);
//      }
//      if (lTempMirrorFolder.exists()) {
//        // If we have any half finished temp folder, remove it
//        XFUtil.deleteDir(lTempMirrorFolder);
//      }

    // Make sure FOX has latest info, e.g. error page mappings

    synchronized (REFRESHING_ENGINE_MIRROR) {
      REFRESHING_ENGINE_MIRROR = false;
    }
    //EngineStatus.createOrUpdateMessage("General", "Engine Mirror", "Populated", MessageLevel.SUCCESS);
    return true;
  }

  public static StringBuffer loadCacheFromDatabase (boolean pBangOutput) {
    //TODO NP - reimplement
    return null;
//    MIRROR_FOLDER = Fox.getFoxServletConfig().getServletContext().getRealPath(MIRROR_FOLDER_RELATIVE);
//    //Start timing
//    long lStart = System.currentTimeMillis();
//    gLoadSuccess = true;
//
//    //Temporary failed status to be overridden at the end
//    Fox.gInternalStatuses.put("EngineMirror", new String[] {"FAILED", ""});
//
//    //Stringbuffer to build result html in
//    StringBuffer lResult = new StringBuffer();
//
//    // Get resource master
//    DOM lResourceMasterDOM = null;
//    synchronized (Fox.class){
//      // Load and parse xml clob
////      try {
//        // TODO AT
//        //lResourceMasterDOM = Fox.mFoxEnvironment.getResourceMaster(Fox.gConnectionKey);
////      }
////      catch (ExServiceUnavailable e) {
////        gLoadSuccess = false;
////        if (pBangOutput) {
////          return new StringBuffer("<h1>Failed to refresh engine mirror!</h1>Cannot get Resource Master from database:<br />" + e.getMessage());
////        }
////        else {
////          throw new ExInternal("Failed to refresh engine mirro. Cannot get Resource Master from database", e);
////        }
////      }
//    }
//
//    File lTempDir = null;
//    HashMap lApplicationConnections = new HashMap();
//    File lMirrorFolder = new File(MIRROR_FOLDER);
//    File lMirrorFolderTemp = new File(MIRROR_FOLDER + new SimpleDateFormat("yyMMdd HH;mm;ss").format(new Date()));
//
//    try {
//      //Create the temp folder
//      try {
//        lTempDir = XFUtil.createTempDir("EngineMirror", "temp");
//      }
//      catch (IOException e) {
//        gLoadSuccess = false;
//        if (pBangOutput) {
//          return new StringBuffer("<h1>Failed to refresh engine mirror!</h1>Cannot create temporary folder:<br />" + e.getMessage());
//        }
//        else {
//          throw new ExInternal("Failed to refresh engine mirror. Cannot create temporary folder", e);
//        }
//      }
//
//      //Generate error page list dom
//      File lErrorPageFile = new File(lTempDir, "error_pages.xml");
//      DOM lErrorPageDOM = DOM.createDocument("ERROR_PAGES");
//      lErrorPageDOM.addElem("DEFAULT-ERROR-APP", lResourceMasterDOM.get1SNoEx("default-error-page-app"));
//
//      //Create a new temporary cache
//      HashMap lTempMap = new HashMap();
//
//      //Get list of apps from the resource master
//      DOMList lAppList = lResourceMasterDOM.getUL("/resource-list/application");
//
//      //Loop over each app, make a connection, run querys to check for app specific engine mirror files and save them
//      Map<String, LocalComponent> lComponentsMeta;
//      //App Loop
//      for (int lAppItem = 0; lAppItem < lAppList.getLength(); ++lAppItem) {
//        lComponentsMeta = new HashMap<>();
//        String lAppMnem = lAppList.item(lAppItem).get1SNoEx("mnem");
//        if (XFUtil.isNull(lAppMnem)) {
//          gLoadSuccess = false;
//          throw new ExInternal("Failed to refresh engine mirror. App mnemonic missing for an app element in the resource master?");
//        }
//
//        String lAppErrorModule = lAppList.item(lAppItem).get1SNoEx("error-module");
//
//        lResult.append("<h1>" + lAppMnem + "</h1>\r\n");
//
//        //Make app folder for mirror files
//        File lAppFolder;
//        try {
//           lAppFolder = XFUtil.createDir(lAppMnem, lTempDir);
//        }
//        catch (IOException e) {
//          gLoadSuccess = false;
//          return new StringBuffer("<h1>Failed to refresh engine mirror!</h1>Cannot create temporary folder:<br />" + e.getMessage());
//        }
//        //Create empty file for component listing
//        File lXMLFile = new File(lAppFolder, "components.xml");
//
//        DOM lComponentList = DOM.createDocument("COMPONENTS_LIST");
//
//        //Get a connection
//        String lAppConnectKey = lAppList.item(lAppItem).get1SNoEx("connect-key");
//        UCon lApplicationUCon = null;
//        try {
//          lApplicationUCon = (UCon)lApplicationConnections.get(lAppConnectKey);
//          if (lApplicationUCon == null) {
//            lApplicationUCon = ConnectionAgent.getConnection(lAppConnectKey, "Fetching Engine Mirror componenets");
//            lApplicationConnections.put(lAppConnectKey, lApplicationUCon);
//          }
//        }
//        catch (ExServiceUnavailable e) {
//          gLoadSuccess = false;
//          if (pBangOutput) {
//            return new StringBuffer("<h1>Failed to refresh engine mirror!</h1>Failed to get a connection to database for app (" + lAppMnem + "):<br />" + e.getMessage());
//          }
//          else {
//            throw new ExInternal("Failed to refresh engine mirror. Failed to get a connection to database for app (" + lAppMnem + ")", e);
//          }
//        }
//
//        //Generate app specific querys to get engine mirror candidates and their data
//        String lQuery;
//        try {
//          lQuery = generateEngineMirrorQuery(lAppList.item(lAppItem));
//        }
//        catch (ExApp e) {
//          gLoadSuccess = false;
//          if (pBangOutput) {
//            return new StringBuffer("<h1>Failed to refresh engine mirror!</h1>Failed to generate querys for app (" + lAppMnem + "):<br />" + e.getMessage());
//          }
//          else {
//            throw new ExInternal("Failed to refresh engine mirror. Failed to generate querys for app (" + lAppMnem + ")", e);
//          }
//        }
//
//        //Find engine-mirrorable files
//        lResult.append("<pre>" + lQuery + "</pre><hr />");
//        UCon.ResultIterator lMirrorIterator;
//        List lMirrorList;
//        try {
//          lMirrorIterator = lApplicationUCon.executeIterator(lQuery, null, false);
//          lMirrorList = lMirrorIterator.selectAllRows(true);
//         }
//        catch (ExDB e) {
//          gLoadSuccess = false;
//          throw e.toUnexpected("Error running query to get engine mirror candidate file names");
//        }
//
//        //Get data for mirror files
//        Object[] lRow = {};
//        //Mirror File Loop
//        for (int i = 1; i < lMirrorList.size(); ++i) {
//          lRow = (Object[])lMirrorList.get(i);
//          String lComponentName = lRow[0].toString();
//
//          //Infer details and save from row data
//          CLOB lClob = (CLOB) lRow[1];
//          BLOB lBlob = (BLOB) lRow[2];
//          String lType = (String) lRow[3];
//          Reader lReader = null;
//          InputStream lInputStream = null;
//          try {
//              String lFileFormat = "";
//              File lFile = null;
//              try {
//                lFile = File.createTempFile("TempFile", "tmp", lAppFolder);
//                lFile.createNewFile();
//                if(lClob != null) {
//                  lFileFormat = "CHAR";
//                  lClob.open(CLOB.MODE_READONLY);
//                  lReader = lClob.characterStreamValue();
//                  try{
//                    XFUtil.writeFile(lFile, lReader);
//                  }catch (Exception e){
//                    System.out.println("Error: " + e.getMessage());
//                  }
//                }
//                if(lBlob != null) {
//                  lFileFormat = "BIN";
//                  lBlob.open(BLOB.MODE_READONLY);
//                  lInputStream = lBlob.getBinaryStream();
//                  try{
//                    XFUtil.writeFile(lFile, lInputStream);
//                  }catch (Exception e){
//                    System.out.println("Error: " + e.getMessage());
//                  }
//                }
//              }
//              catch (IOException e) {
//                gLoadSuccess = false;
//                if (pBangOutput) {
//                  lResult.append("Failed to make temp file for (" + lComponentName + "):<br />" + e.getMessage());
//                }
//                else {
//                  throw new ExInternal("Failed to refresh engine mirror. Could not make temporary file for (" + lComponentName + ")", e);
//                }
//              }
//
//              //Add file info to apps component xml
//              String lFilePath = MIRROR_FOLDER_RELATIVE + File.separator + lAppFolder.getName() + File.separator + lFile.getName();
//              lComponentList.addElem("COMPONENT").addElem("NAME", lComponentName)
//                .getParentOrNull()
//                .addElem("TYPE", lType)
//                .getParentOrNull()
//                .addElem("FILE_PATH", lFilePath)
//                .getParentOrNull()
//                .addElem("FILE_FORMAT", lFileFormat);
//
//              //Add to the internal map too
//              LocalComponent lComponent = new LocalComponent(new File(lFilePath), lType, lFileFormat, true);
//              lComponentsMeta.put(lComponentName, lComponent);
//
//              //If it's the apps error page, store that as well
//              if (lAppErrorModule.equals(lComponentName)) {
//                lErrorPageDOM.addElem("COMPONENT").addElem("APP", lAppMnem)
//                  .getParentOrNull()
//                  .addElem("FILE_PATH", MIRROR_FOLDER_RELATIVE + File.separator + lAppFolder.getName() + File.separator + lFile.getName())
//                  .getParentOrNull()
//                  .addElem("NAME", lComponentName);
//              }
//
//              lResult.append("<span style=\"color: #00FF00;\"><strong>Matched: " + lComponentName + " - " + lType + "</strong></span><br />");
//          }
//          catch(SQLException x) {
//            gLoadSuccess = false;
//            throw new ExInternal("Error reading blob/clob", x);
//          }
//          finally {
//            try { if(lReader != null) lReader.close(); } catch(IOException x) {}
//            try { if(lInputStream != null) lInputStream.close(); } catch(IOException x) {}
//            try { if(lClob != null) lClob.close(); } catch(SQLException x) {}
//            try { if(lBlob != null) lBlob.close(); } catch(SQLException x) {}
//          }
//        } //Mirror File Loop
//
//        //Add the components to the temporary map
//        lTempMap.put(lAppMnem, lComponentsMeta);
//
//        lResult.append("<br /><br />");
//
//        try {
//          XFUtil.writeFile(lXMLFile, lComponentList.outputDocumentToString());
//          XFUtil.writeFile(new File(lTempDir, "README.TXT"), "FOX generates all the files in this directory, altering anything in here would not be advised.\r\n"
//            + "Any modifications made will be overwritten the next time a person flushes.\r\n"
//            + "\r\n"
//            + "Last updated: " + DATE_FORMATTER.format(new Date()));
//        }
//        catch (IOException e) {
//        }
//      } //App Loop
//
//      //Clean up all connections made for the apps
//      Iterator lAppConnIterator = lApplicationConnections.values().iterator();
//      while (lAppConnIterator.hasNext()) {
//        ((UCon)lAppConnIterator.next()).closeForRecycle();
//      }
//
//      //Save error page DOM
//      XFUtil.writeFile(lErrorPageFile, lErrorPageDOM.outputDocumentToString());
//
//      //Safely attempt to copy new mirror files over old ones
//      if (lMirrorFolder.exists()) {
//        XFUtil.copyDir(lMirrorFolder, lMirrorFolderTemp);
//        XFUtil.deleteDir(lMirrorFolder);
//      }
//      XFUtil.copyDir(lTempDir, lMirrorFolder);
//      XFUtil.deleteDir(lTempDir);
//      if (lMirrorFolderTemp.exists()) {
//        XFUtil.deleteDir(lMirrorFolderTemp);
//      }
//
//      //Copy temp cache over old one
//      synchronized(INTERNAL_APP_COMPONENT_MAP) {
//        INTERNAL_APP_COMPONENT_MAP = lTempMap;
//      }
//    }
//    catch (Throwable x) {
//      gLoadSuccess = false;
//      Fox.gInternalStatuses.put("EngineMirror", new String[] {"FAILED", x.getMessage()});
//      if (pBangOutput) {
//        lResult.append("Failed! Reason: " + x.getMessage());
//        return lResult;
//      }
//    }
//    finally {
//      //Tidy up
//      //Make sure not to leave things in the temp folder
//      if (lTempDir != null && lTempDir.exists()) {
//        XFUtil.deleteDir(lTempDir);
//      }
//      //Clean up after possible errors
//      if (lMirrorFolderTemp.exists() && lMirrorFolder.exists()) {
//        XFUtil.deleteDir(lMirrorFolderTemp);
//      }
//      else if (lMirrorFolderTemp.exists() && !lMirrorFolder.exists()) {
//        XFUtil.copyDir(lMirrorFolderTemp, lMirrorFolder);
//        XFUtil.deleteDir(lMirrorFolderTemp);
//        loadCacheFromDisk();
//      }
//
//      //Make sure no connections are left unclosed
//      Iterator lAppConnIterator = lApplicationConnections.values().iterator();
//      while (lAppConnIterator.hasNext()) {
//        UCon lCon = (UCon)lAppConnIterator.next();
//        if (lCon.getJDBCConnection() != null) {
//          lCon.closeForRecycle();
//        }
//      }
//
//      //Re-populate the Fox engine error component map
//      Fox.populateErrorComponentMap();
//    }
//
//    lResult.append("<br />Took: " + ((System.currentTimeMillis() - lStart)/1000) + "s to update.<br />");
//    if (gLoadSuccess) {
//      Fox.gInternalStatuses.put("EngineMirror", new String[] {"LOADED", "Last refreshed: " + DATE_FORMATTER.format(new Date()) + ", Took: " + ((System.currentTimeMillis() - lStart)/1000) + "s"});
//    }
//    return lResult;
  }

  public static void storeFile (String pApp, String pName, Object pData, String pType)
  throws IOException {
    MIRROR_FOLDER = FoxGlobals.getInstance().getServletContext().getRealPath(MIRROR_FOLDER_RELATIVE);
    File lAppFolder = new File(MIRROR_FOLDER + File.separator + pApp);
    File lFile = File.createTempFile("TempFile", "tmp", lAppFolder);
    File lXMLFile = new File(lAppFolder, "components.xml");
    DOM lComponentsXML = DOM.createDocument(lXMLFile, false);
    String lFormat = "";

    if (pData instanceof InputStream) {
      XFUtil.writeFile(lFile, (InputStream)pData);
      lFormat = "BIN";
    }
    else if (pData instanceof Reader) {
      XFUtil.writeFile(lFile, (Reader)pData);
      lFormat = "CHAR";
    }
    else if (pData instanceof String) {
      XFUtil.writeFile(lFile, (String)pData);
      lFormat = "CHAR";
    }
    else {
      throw new ExInternal("Cannot save data in this format into the engine mirror: " + pData.getClass().getName());
    }

    DOM lCurrentNode;
    try {
      lCurrentNode = lComponentsXML.xpath1E("/COMPONENTS_LIST/COMPONENT[NAME = '" + pName + "']/FILE_PATH");
      lCurrentNode.setText(File.separator + "WEB-INF" + File.separator + "engine_mirror" + File.separator + pApp + File.separator + lFile.getName());
      XFUtil.writeFile(lXMLFile, lComponentsXML.outputDocumentToString());
    }
    catch (ExTooFew e) {
      lComponentsXML.addElem("COMPONENT").addElem("NAME", pName)
        .getParentOrNull()
        .addElem("TYPE", pType)
        .getParentOrNull()
        .addElem("FILE_PATH", File.separator + "WEB-INF" + File.separator + "engine_mirror" + File.separator + pApp + File.separator + lFile.getName())
        .getParentOrNull()
        .addElem("FILE_FORMAT", lFormat);
        XFUtil.writeFile(lXMLFile, lComponentsXML.outputDocumentToString());
    }
    catch (ExBadPath e) {}
    catch (ExTooMany e) {}
  }

  public static String generateEngineMirrorQuery(App pApp)
  throws ExApp {
    List<String> lResourceTables = pApp.getResourceTableList();
    if(lResourceTables.size() == 0) {
      throw new ExInternal("Cannot generate Engine Mirror cache for app '" + pApp.getAppMnem() + "' as no resource tables found");
    }

    StringBuilder lEngineMirrorQuery = new StringBuilder();
    lEngineMirrorQuery.append("WITH component_names AS (\n");

    int i = 0;
    for (String lResourceTableName : lResourceTables) {
      if(XFUtil.isNull(lResourceTableName)) {
        throw new ExApp("Empty resource table name found in app '" + pApp.getAppMnem() + "' when attempting to generate engine mirror query");
      }

      if(i!=0) {
        lEngineMirrorQuery.append("  UNION ALL\n");
      }
      lEngineMirrorQuery.append("  SELECT name, " + i + " pos, engine_mirror, data, bindata, type\n  FROM " + lResourceTableName + " WHERE engine_mirror = 'Y'\n");
      i++;
    }

    lEngineMirrorQuery.append(")\n" +
    "SELECT cn1.name, cn1.data, cn1.bindata, cn1.type\n" +
    "FROM component_names cn1 \n" +
    "WHERE cn1.engine_mirror = 'Y'\n" +
    "AND NOT EXISTS (\n" +
    "  SELECT 1\n" +
    "  FROM component_names cn2\n" +
    "  WHERE cn2.name = cn1.name\n" +
    "  AND cn2.pos < cn1.pos\n" +
    "  AND cn2.engine_mirror IS NULL\n" +
    ")\n" +
    "AND cn1.pos = (\n" +
    "  SELECT MIN(cn3.pos)\n" +
    "  FROM component_names cn3\n" +
    "  WHERE cn3.engine_mirror = 'Y'\n" +
    "  AND cn3.name = cn1.name\n" +
    ")");

    return lEngineMirrorQuery.toString();
  }

  public static FoxComponent getComponentOrNull(String pApp, String pName) {
    Map<String, LocalComponent> lAppMapp = null;
    synchronized(INTERNAL_APP_COMPONENT_MAP) {
      lAppMapp = INTERNAL_APP_COMPONENT_MAP.get(pApp);
    }
    if (lAppMapp == null) {
      return null;
    }
    LocalComponent lComponent = lAppMapp.get(pName);
    if (lComponent == null) {
      return null;
    }

    FoxComponent lFoxComponent = null;
    File lFile = lComponent.getFile();
    if(!lFile.canRead()) {
      throw new ExInternal("Internal Component File Missing/Unreadable: " + lFile.getAbsolutePath());
    }
    String lType = lComponent.getType();
    String lFormat = lComponent.getFormat();
    if(lFormat != "CHAR" && lFormat != "BIN") {
      throw new ExInternal("Internal Component File format not CHAR/BIN: " + lFile.getAbsolutePath() + " says it's " + lFormat);
    }

    if(lFormat == "CHAR") {
      Reader lReader= null;
      try {
        lReader = new FileReader(lFile);
        lFoxComponent = FoxComponent.createComponent(
          pName
        , lType
        , lReader
        , null
        , null
        , ComponentManager.getComponentBrowserCacheMS()
        );
      }
      catch(Throwable x) {
        throw new ExInternal("Internal Fox File Error: ", x);
      }
      finally {
        if(lReader != null) {
          try {
            lReader.close();
          }
          catch(IOException x) {}
        }
      }
    }
    else if(lFormat == "BIN") {
      InputStream lIS = null;
      try {
        lIS = new FileInputStream(lFile);
        lFoxComponent = FoxComponent.createComponent(
          pName
        , lType
        , null
        , lIS
        , null
        , ComponentManager.getComponentBrowserCacheMS()
        );
      }
      catch(Throwable x) {
        throw new ExInternal("Internal Fox File Error: ", x);
      }
      finally {
        if(lIS != null) {
          try {
            lIS.close();
          }
          catch(IOException x) {}
        }
      }
    }

    return lFoxComponent;
  }

  public static boolean loadCacheFromDisk() {
    //Create a new temporary cache
    HashMap lTempMap = new HashMap();

    MIRROR_FOLDER = FoxGlobals.getInstance().getServletContext().getRealPath(MIRROR_FOLDER_RELATIVE);
    File lMirrorFolder = new File(MIRROR_FOLDER);
    File[] lAppFolders = lMirrorFolder.listFiles();

    if (lAppFolders == null) {
      return false;
    }

    try {
      for (int i = 0; i < lAppFolders.length; ++i) {
        if (lAppFolders[i].isDirectory()) {
          Map<String, LocalComponent> lComponents = new HashMap<>();
          DOM lComponentsRootDOM = DOM.createDocument(new File(lAppFolders[i].getAbsolutePath() + File.separator + "components.xml"), false);
          DOMList lComponentsDOMList = lComponentsRootDOM.getUL("/*/COMPONENT");
          DOM lComponentDOM;

          for (int j = 0; j < lComponentsDOMList.getLength(); j++) {
            lComponentDOM = lComponentsDOMList.item(j);
            LocalComponent lComponent = new LocalComponent(new File(lComponentDOM.get1SNoEx("FILE_PATH")), lComponentDOM.get1SNoEx("TYPE"),  lComponentDOM.get1SNoEx("FILE_FORMAT"), true);

            lComponents.put(lComponentDOM.get1SNoEx("NAME"), lComponent);
          }

          lTempMap.put(lAppFolders[i].getName(), lComponents);
        }
      }

      synchronized(INTERNAL_APP_COMPONENT_MAP) {
        INTERNAL_APP_COMPONENT_MAP = lTempMap;
      }

      return true;
    }
    catch (Throwable th) {
      System.out.println("Something went wrong loading files from the engine mirror. Removing corrupt folder and skipping load");
      XFUtil.deleteDir(lMirrorFolder);
      return false;
    }
  }
}
