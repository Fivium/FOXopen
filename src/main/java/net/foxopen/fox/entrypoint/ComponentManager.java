package net.foxopen.fox.entrypoint;

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheFactory;
import net.foxopen.fox.App;
import net.foxopen.fox.ComponentCSS;
import net.foxopen.fox.ComponentText;
import net.foxopen.fox.FoxComponent;
import net.foxopen.fox.XFUtil;
import net.foxopen.fox.dom.DOM;
import net.foxopen.fox.dom.DOMList;
import net.foxopen.fox.entrypoint.servlets.StaticServlet;
import net.foxopen.fox.ex.ExApp;
import net.foxopen.fox.ex.ExInternal;
import net.foxopen.fox.ex.ExModule;
import net.foxopen.fox.ex.ExServiceUnavailable;
import net.foxopen.fox.ex.ExUserRequest;
import net.foxopen.fox.track.Track;

import javax.servlet.ServletContext;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;

public class ComponentManager {
  public static final MustacheFactory MUSTACHE_FACTORY = new DefaultMustacheFactory();

  private static final long COMPONENT_BROWSER_CACHE_MS = 7*60*60*1000; // 7 Hrs default for application components

  private static final Map<String, FoxComponent> INTERNAL_COMPONENTS_IMMUTABLE = new HashMap<>();
  private static final Map<String, FoxComponent> INTERNAL_COMPONENTS_OVERLOADABLE  = new HashMap<>();

  private static Mustache gInternalErrorComponent;

  private ComponentManager() {
  }

  /**
   * Parse WEB-INF/FoxComponents.xml where internal components of FOX are defined and load them in to memory. Components
   * can be overloadable or immutable and can consist of one file or several files concatenated together.
   * This method can be called repeatedly to refresh the hard in-memory cache of the components from the files on disk.
   */
  public static void loadInternalComponents() {
    ServletContext lServletContext = FoxGlobals.getInstance().getServletContext();

    // Read internal components XML
    DOM lInternalComponentsDOM = DOM.createDocument(new File(lServletContext.getRealPath("/WEB-INF/FoxComponents.xml")), false);

    // Clean folder for multi part files to be cached
    File lMultiPartCacheFolder = new File(lServletContext.getRealPath("/WEB-INF/.multi_part_component_cache"));
    XFUtil.deleteDir(lMultiPartCacheFolder);
    lMultiPartCacheFolder.mkdir();
    if (!lMultiPartCacheFolder.exists()) {
      throw new ExInternal("Failed to create the multi part cache folder: " + lMultiPartCacheFolder.getAbsolutePath());
    }

    // Clear old data
    INTERNAL_COMPONENTS_IMMUTABLE.clear();
    INTERNAL_COMPONENTS_OVERLOADABLE.clear();

    // Make Cache internal components
    DOMList lComponents = lInternalComponentsDOM.getUL("/*/COMPONENT");
    lComponents.addAll(lInternalComponentsDOM.getUL("/*/MULTI_PART_COMPONENT"));
    for (DOM lComponentDOM : lComponents) {
      String lName = lComponentDOM.get1SNoEx("NAME");
      String lType = lComponentDOM.get1SNoEx("TYPE");
      String lFileFormat = lComponentDOM.get1SNoEx("FILE_FORMAT");
      boolean lOverloadable = "TRUE".equals(lComponentDOM.get1SNoEx("OVERLOADABLE"));

      String lFilePath;
      File lFile;
      if ("COMPONENT".equals(lComponentDOM.getName())) {
        // Regular component definitions have one file path
        lFilePath = lServletContext.getRealPath(lComponentDOM.get1SNoEx("FILE_PATH").replace('\\', '/'));
        lFile = new File(lFilePath);
      }
      else if ("MULTI_PART_COMPONENT".equals(lComponentDOM.getName())) {
        if (!"CHAR".equals(lFileFormat)) {
          throw new ExInternal("Multi part components can only be CHAR format: " + lComponentDOM.outputNodeToString(true));
        }

        lFilePath = "Multipart: " + lName;
        lFile = combineMultiParthFile(lName, lComponentDOM, lServletContext, lMultiPartCacheFolder);
      }
      else {
        throw new ExInternal("Unknown internal component file definition: " + lComponentDOM.getName());
      }

      if(!lFile.canRead()) {
        throw new ExInternal("Internal Component File Missing/Unreadable: " + lFilePath);
      }

      if(!"CHAR".equals(lFileFormat) && !"BIN".equals(lFileFormat)) {
        throw new ExInternal("Internal Component File format not CHAR/BIN: " + lFilePath);
      }

      //Cache expiry times for internal components should be set to a long duration
      //If the files are modified the hash difference will cause clients to re-request the component
      //(this is why we generate a hash for these components)

      // Create internal component
      FoxComponent lFoxComponent = null;
      try {
        if("CHAR".equals(lFileFormat)) {
          Reader lReader = null;
          try {
            lReader = new FileReader(lFile);
            lFoxComponent = FoxComponent.createComponent(
              lName
            , lType
            , lReader
            , null
            , null
            , StaticServlet.staticResourceExpiryTimeMS()
            , true
            );
          }
          catch(FileNotFoundException x) {
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
        else if("BIN".equals(lFileFormat)) {
          InputStream lIS = null;
          try {
            lIS = new FileInputStream(lFile);
            lFoxComponent = FoxComponent.createComponent(
              lName
            , lType
            , null
            , lIS
            , null
            , StaticServlet.staticResourceExpiryTimeMS()
            , true
            );
          }
          catch(FileNotFoundException x) {
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
      }
      catch (ExApp | ExServiceUnavailable | ExModule e) {
        throw e.toUnexpected();
      }

      // Cache component
      if (lOverloadable) {
        INTERNAL_COMPONENTS_OVERLOADABLE.put(lName, lFoxComponent);
      }
      else {
        INTERNAL_COMPONENTS_IMMUTABLE.put(lName, lFoxComponent);
      }
    }

    // Load, parse and cache internal error component
    try {
      Map<String, Object> lCacheVars = new HashMap<>();
      ComponentText lErrorPage = (ComponentText)getComponent("internal-error.mustache");

      ComponentCSS lFOXStyleSheet = (ComponentCSS)getComponent("css/fox.css");
      lCacheVars.put("FoxCSS", lFOXStyleSheet.getText());

      ComponentCSS lCodeMirrorCSS = (ComponentCSS)getComponent("css/codemirror.css");
      lCacheVars.put("CodeMirrorCSS", lCodeMirrorCSS.getText());

      ComponentText lCodeMirrorJS = (ComponentText)getComponent("js/codemirror.js");
      lCacheVars.put("CodeMirrorJS", lCodeMirrorJS.getText());

      Mustache lCacheTemplate = MUSTACHE_FACTORY.compile(new StringReader(lErrorPage.getText().toString()), "internal-error.mustache");
      StringWriter lStringWriter = new StringWriter();
      lCacheTemplate.execute(lStringWriter, lCacheVars);
      gInternalErrorComponent = MUSTACHE_FACTORY.compile(new StringReader(lStringWriter.toString()), "CachedInternalError.mustache");
    }
    catch (ExApp | ExUserRequest | ExModule | ExServiceUnavailable pExServiceUnavailable) {
      gInternalErrorComponent = null;
    }
  }

  /**
   * Multi part components have a list of file paths to combine in to one file and use
   *
   * @param pName Name for the combined component
   * @param pComponentDOM DOM from FoxComponents that contains the list of files to combine
   * @param pServletContext Servlet context to use when getting the components base file
   * @param pMultiPartCacheFolder Folder to cache the combined component in
   * @return File reference to the combined component
   */
  private static File combineMultiParthFile(String pName, DOM pComponentDOM, ServletContext pServletContext, File pMultiPartCacheFolder) {
    File lCombinedCachedFile = new File(pMultiPartCacheFolder, pName.replaceAll("\\\\|/", "-"));
    if(!lCombinedCachedFile.exists()) {
      try {
        lCombinedCachedFile.createNewFile();
      }
      catch (IOException e) {
        throw new ExInternal("Failed to create a new file for the combined internal cache: " + pName, e);
      }
    }
    else if (!lCombinedCachedFile.canWrite()) {
      throw new ExInternal("Internal Component File Missing/Unreadable: " + lCombinedCachedFile.getAbsolutePath());
    }

    OutputStream lCombinedOutputStream = null;
    InputStream lInputStream = null;
    try {
      lCombinedOutputStream = new FileOutputStream(lCombinedCachedFile);
      byte[] lReadBuffer = new byte[1024];

      // Read each file and put into combined cached file
      for (DOM lFilePathElem : pComponentDOM.getUL("FILE_PATH_LIST/FILE_PATH")) {
        String lPartPath = pServletContext.getRealPath(lFilePathElem.get1SNoEx(".").replace('\\', '/'));
        lInputStream = new FileInputStream(new File(lPartPath));
        int lBytesRead;
        while ((lBytesRead = lInputStream.read(lReadBuffer)) >= 0) {
          lCombinedOutputStream.write(lReadBuffer, 0, lBytesRead);
          lCombinedOutputStream.flush();
        }
      }

    }
    catch (IOException e) {
      throw new ExInternal("Failed to read part of a multi-part component: " + pName, e);
    }
    finally {
      if (lInputStream != null) {
        try {
          lInputStream.close();
        }
        catch(IOException x) {}
      }

      if (lCombinedOutputStream != null) {
        try {
          lCombinedOutputStream.close();
        }
        catch(IOException x) {}
      }
    }

    return lCombinedCachedFile;
  }

  /**
   * Get a component from FOX looking up in various caches based on a request path that may or may not include an App Mnem
   *
   * @param pRequestURI
   * @return
   * @throws ExUserRequest
   * @throws ExModule
   * @throws ExServiceUnavailable
   * @throws ExApp
   */
  public static FoxComponent getComponent(StringBuilder pRequestURI)
  throws ExUserRequest, ExModule, ExServiceUnavailable, ExApp {
    return getComponent(pRequestURI, null);
  }

  /**
   * Get a component from FOX looking up in various caches based on a request path that may or may not include an App Mnem
   *
   * @param pRequestURI
   * @return
   * @throws ExUserRequest
   * @throws ExModule
   * @throws ExServiceUnavailable
   * @throws ExApp
   */
  public static FoxComponent getComponent(String pRequestURI)
  throws ExUserRequest, ExModule, ExServiceUnavailable, ExApp {
    return getComponent(new StringBuilder(pRequestURI));
  }

  /**
   * Get a component from FOX looking up in various caches based on a request path that may or may not include an App Mnem
   *
   * @param pRequestURI
   * @return
   * @throws ExUserRequest
   * @throws ExModule
   * @throws ExServiceUnavailable
   * @throws ExApp
   */
  public static FoxComponent getComponent(String pRequestURI, String pAppMnem)
  throws ExUserRequest, ExModule, ExServiceUnavailable, ExApp {
    return getComponent(new StringBuilder(pRequestURI), pAppMnem);
  }

  public static FoxComponent getComponent(StringBuilder pRequestURI, String pAppMnem)
  throws ExUserRequest, ExModule, ExServiceUnavailable, ExApp {
    FoxComponent lFoxComponent = null;

    String lFullPath = XFUtil.pathStripLeadSlashes(pRequestURI.toString());
    Track.info("getComponentThroughApplication", lFullPath);

    // Pop the head off the request URI, possibly getting the app mnemonic (if it was an app mnem)
    String lAppMnem = pAppMnem;
    if (XFUtil.isNull(lAppMnem)) {
      lAppMnem = XFUtil.pathPopHead(pRequestURI, true);
    }

    // Check for global fox based immutable cached components
    lFoxComponent = XFUtil.nvl(INTERNAL_COMPONENTS_IMMUTABLE.get(pRequestURI.toString()), INTERNAL_COMPONENTS_IMMUTABLE.get(lFullPath));
    if(lFoxComponent != null) {
      return lFoxComponent;
    }

    // Attempt to get component from an application, specified by the head of the path
    App lApp;
    try {
      lApp = FoxGlobals.getInstance().getFoxEnvironment().getAppByMnem(lAppMnem);

      return lApp.getComponent(pRequestURI.toString(), true);
    }
    catch (Throwable e) {
      //If the app wasn't found or the component in it, perhaps it was an internal mutable component
      lFoxComponent = XFUtil.nvl(INTERNAL_COMPONENTS_OVERLOADABLE.get(pRequestURI.toString()), INTERNAL_COMPONENTS_OVERLOADABLE.get(lFullPath));
      if (lFoxComponent != null) {
        return lFoxComponent;
      }
      else {
        // If it wasn't an internal mutable component, then re-throw the original error that App.getComponent() returned
        throw e;
      }
    }
  }

  /**
   * Gets the hash value for an internal component at the given path. If the component does not exist or is does not have
   * a hash generated for it, null is returned.
   * @param pPath Path for internal component.
   * @return Hash value or null.
   */
  public static String getInternalComponentHashOrNull(String pPath) {
    //Note: if an internal compontent has been overloaded, it will have a short cache timeout, so the hash value from here will have no effect
    FoxComponent lFoxComponent = XFUtil.nvl(INTERNAL_COMPONENTS_IMMUTABLE.get(pPath), INTERNAL_COMPONENTS_OVERLOADABLE.get(pPath));
    if(lFoxComponent != null) {
      return lFoxComponent.getHashOrNull();
    }
    else {
      return null;
    }
  }

  public static long getComponentBrowserCacheMS() {
    return COMPONENT_BROWSER_CACHE_MS;
  }

  public static Mustache getInternalErrorComponent() {
    return gInternalErrorComponent;
  }
}
