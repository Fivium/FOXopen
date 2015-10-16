package net.foxopen.fox.module.serialiser.pdf.css;

import com.itextpdf.tool.xml.css.CSSFileWrapper;
import com.itextpdf.tool.xml.css.CssFile;
import com.itextpdf.tool.xml.css.CssFileProcessor;
import com.itextpdf.tool.xml.css.CssFiles;
import com.itextpdf.tool.xml.css.CssFilesImpl;
import com.itextpdf.tool.xml.net.FileRetrieve;
import com.itextpdf.tool.xml.net.FileRetrieveImpl;
import net.foxopen.fox.FoxComponent;
import net.foxopen.fox.entrypoint.FoxGlobals;
import net.foxopen.fox.ex.ExInternal;
import net.foxopen.fox.module.CSSListItem;
import net.foxopen.fox.module.serialiser.FoxComponentUtils;
import net.foxopen.fox.module.serialiser.SerialisationContext;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

/**
 * Loads CSS files from module CSS items (css-list)
 */
public class CSSFileManager {
  /**
   * The relative path to the default CSS file to be applied to PDF elements, this must exist
   */
  private static final String DEFAULT_CSS_RELATIVE_PATH = "/WEB-INF/components-pdf/pdfDefaults.css";
  /**
   * The module css-list item type to be processed by the file manager, all other css file types are ignored
   */
  private static final String MODULE_CSS_ITEM_TYPE = "pdf";

  private final CssFiles mCSSFiles = new CssFilesImpl();

  /**
   * Creates a CSS files manager with the default PDF CSS file loaded, along with any CSS files resolved from the CSS
   * list items of type "print". CSS list items must target a CSS file in the component table, if a fixed URL is
   * specified an exception is thrown.
   * @param pSerialisationContext The serialisation context, used to access the application components table
   * @param pModuleCSSItems The module css-list items to resolve and load
   */
  public CSSFileManager(SerialisationContext pSerialisationContext, List<CSSListItem> pModuleCSSItems) {
    addCSSFile(getDefaultCSS());
    pModuleCSSItems.stream()
                   .filter(pModuleCSSItem -> MODULE_CSS_ITEM_TYPE.equals(pModuleCSSItem.getType()))
                   .map(pModuleCSSItem -> getCSSFileFromModuleCSSItem(pSerialisationContext, pModuleCSSItem))
                   .forEach(this::addCSSFile);
  }

  /**
   * Returns the loaded CSS files
   * @return The loaded CSS Files
   */
  public CssFiles getCSSFiles() {
    return mCSSFiles;
  }

  /**
   * Add a CSS file to the list of loaded files
   * @param pCSSFile The CSS file to add
   */
  private void addCSSFile(CssFile pCSSFile) {
    mCSSFiles.add(pCSSFile);
  }

  /**
   * Returns the default PDF CSS file
   * @return The default CSS file for PDF documents
   * @throws ExInternal If the default CSS could not be found
   */
  private CssFile getDefaultCSS() throws ExInternal {
    String lDefaultCssPath = FoxGlobals.getInstance().getServletContext().getRealPath(DEFAULT_CSS_RELATIVE_PATH);

    try {
      return getCSSFile(new FileInputStream(lDefaultCssPath));
    }
    catch (FileNotFoundException e) {
      throw new ExInternal("Could not find default CSS '" + lDefaultCssPath + "'", e);
    }
  }

  /**
   * Returns a CSS file resolved from a module css-list item
   * @param pSerialisationContext The serialisation context, used to get the file from the application components table
   * @param pModuleCSSItem The module css-list item
   * @return The resolved CSS file
   * @throws ExInternal If a fixed URI was specified in the module css item, see
   *         {@link net.foxopen.fox.entrypoint.uri.RequestURIBuilder#isFixedURI}
   */
  private CssFile getCSSFileFromModuleCSSItem(SerialisationContext pSerialisationContext, CSSListItem pModuleCSSItem) throws ExInternal {
    CssFile lCSSFile;
    String lPath = pModuleCSSItem.getStyleSheetPath();

    if (!pSerialisationContext.createURIBuilder().isFixedURI(lPath)) {
      lCSSFile = getCSSFileFromComponent(pSerialisationContext, lPath);
    }
    else {
      throw new ExInternal("Fixed CSS file URIs cannot be used during PDF serialisation (URI: '" + lPath + "')",
                           new UnsupportedOperationException());
    }

    return lCSSFile;
  }

  /**
   * Returns a CSS file from the given component path
   * @param pSerialisationContext The serialisation context, used to get the file from the application components table
   * @param pComponentPath The path to the component
   * @return The resolved CSS file
   */
  private CssFile getCSSFileFromComponent(SerialisationContext pSerialisationContext, String pComponentPath) {
    FoxComponent lCSSComponent = FoxComponentUtils.getComponent(pSerialisationContext, pComponentPath);
    return getCSSFile(lCSSComponent.getInputStream());
  }

  /**
   * Returns an immutable CSS file from the CSS file input stream
   * @param pCSSFileInputStream The input stream containing the CSS file
   * @return A CSS file taken from the input stream
   * @throws ExInternal If the CSS file input stream could not be read
   */
  private CssFile getCSSFile(InputStream pCSSFileInputStream) throws ExInternal {
    FileRetrieve lFileRetriever = new FileRetrieveImpl();
    CssFileProcessor lCSSFileProcessor = new CssFileProcessor();

    try {
      lFileRetriever.processFromStream(pCSSFileInputStream, lCSSFileProcessor);
    }
    catch (IOException e) {
      throw new ExInternal("Failed to read CSS file input stream");
    }

    return new CSSFileWrapper(lCSSFileProcessor.getCss(), true);
  }
}
