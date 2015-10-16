package net.foxopen.fox.dom.xpath.saxon;

import net.foxopen.fox.App;
import net.foxopen.fox.ContextUCon;
import net.foxopen.fox.ContextUElem;
import net.foxopen.fox.FoxRequest;
import net.foxopen.fox.auth.AuthenticationContext;
import net.foxopen.fox.auth.SecurityScope;
import net.foxopen.fox.command.XDoCommandList;
import net.foxopen.fox.command.XDoIsolatedRunner;
import net.foxopen.fox.command.XDoResult;
import net.foxopen.fox.command.XDoRunner;
import net.foxopen.fox.command.flow.XDoControlFlow;
import net.foxopen.fox.dbinterface.InterfaceQuery;
import net.foxopen.fox.dom.DOM;
import net.foxopen.fox.dom.DOMList;
import net.foxopen.fox.download.DownloadManager;
import net.foxopen.fox.entrypoint.FoxSession;
import net.foxopen.fox.entrypoint.uri.RequestURIBuilder;
import net.foxopen.fox.ex.ExBadPath;
import net.foxopen.fox.ex.ExTooFew;
import net.foxopen.fox.ex.ExTooMany;
import net.foxopen.fox.module.Mod;
import net.foxopen.fox.module.State;
import net.foxopen.fox.module.entrytheme.EntryTheme;
import net.foxopen.fox.module.facet.ModuleFacetProvider;
import net.foxopen.fox.module.mapset.MapSet;
import net.foxopen.fox.module.serialiser.ThreadInfoProvider;
import net.foxopen.fox.thread.ActionRequestContext;
import net.foxopen.fox.thread.DOMHandlerProvider;
import net.foxopen.fox.thread.ExitResponse;
import net.foxopen.fox.thread.XThreadInterface;
import net.foxopen.fox.thread.devtoolbar.DevToolbarContext;
import net.foxopen.fox.thread.persistence.PersistenceContext;
import net.foxopen.fox.thread.stack.ModuleCall;
import net.foxopen.fox.thread.stack.transform.StateStackTransformation;
import net.foxopen.fox.thread.storage.TempResourceProvider;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class StoredXPathTranslationTest {

  private static final String XPATH_1 = "/*/ELEMENT_1";
  private static final String XPATH_2 = "/*/ELEMENT_2";

  @Test
  public void testBasicXPathTranslation() {

    String lResult;

    lResult = StoredXPathTranslator.instance().translateXPathReferences(new TestXPathResolver(), "${xpath1}");
    assertEquals("Basic XPath reference is replaced", XPATH_1, lResult);

    lResult = StoredXPathTranslator.instance().translateXPathReferences(new TestXPathResolver(), "/*/${xpath1}");
    assertEquals("Basic XPath reference is replaced (leading content)", "/*/" + XPATH_1, lResult);

    lResult = StoredXPathTranslator.instance().translateXPathReferences(new TestXPathResolver(), "${xpath1}/*");
    assertEquals("Basic XPath reference is replaced (trailing content)", XPATH_1 + "/*", lResult);

    lResult = StoredXPathTranslator.instance().translateXPathReferences(new TestXPathResolver(), "string('${xpath1}')");
    assertEquals("XPath reference is replaced even if in quotes", "string('" + XPATH_1 + "')", lResult);
  }

  @Test
  public void testInvalidSyntaxTranslation() {

    String lResult;

    lResult = StoredXPathTranslator.instance().translateXPathReferences(new TestXPathResolver(), "$ {xpath1}");
    assertEquals("XPath not replaced (space between $ and {)", "$ {xpath1}", lResult);

    lResult = StoredXPathTranslator.instance().translateXPathReferences(new TestXPathResolver(), "$\\{xpath1}");
    assertEquals("XPath not replaced (slash between $ and {)", "$\\{xpath1}", lResult);

    lResult = StoredXPathTranslator.instance().translateXPathReferences(new TestXPathResolver(), "${xpath1");
    assertEquals("XPath not replaced (missing trailing })", "${xpath1", lResult);

    lResult = StoredXPathTranslator.instance().translateXPathReferences(new TestXPathResolver(), "${xpath 1}");
    assertEquals("XPath not replaced (space in reference)", "${xpath 1}", lResult);

    lResult = StoredXPathTranslator.instance().translateXPathReferences(new TestXPathResolver(), "${}");
    assertEquals("XPath not replaced (no reference string)", "${}", lResult);

    lResult = StoredXPathTranslator.instance().translateXPathReferences(new TestXPathResolver(), "${xpath1 ${xpath2}");
    assertEquals("First XPath not replaced (missing trailing }) but second allowed", "${xpath1 " + XPATH_2, lResult);
  }

  @Test
  public void testXPathNameValidation() {

    assertTrue("Name containing alphabetic characters is valid",  StoredXPathTranslator.instance().validateXPathName("myXpath"));
    assertTrue("Name containing numbers is valid",  StoredXPathTranslator.instance().validateXPathName("123"));
    assertTrue("Name containing colon and hyphen is valid",  StoredXPathTranslator.instance().validateXPathName("namespace:my-xpath"));
    assertFalse("Name containing whitespace is not valid",  StoredXPathTranslator.instance().validateXPathName("my xpath"));
    assertFalse("Name containing dollar character is not valid",  StoredXPathTranslator.instance().validateXPathName("$myxpath"));
    assertFalse("Name containing brace characters is not valid",  StoredXPathTranslator.instance().validateXPathName("{myxpath}"));
  }

  @Test
  public void testMultipleXPathTranslation() {

    String lResult;
    lResult = StoredXPathTranslator.instance().translateXPathReferences(new TestXPathResolver(), "${xpath1} | ${xpath2}");
    assertEquals("Multiple XPath references in same string are replaced (with separator)", XPATH_1 + " | " + XPATH_2, lResult);

    lResult = StoredXPathTranslator.instance().translateXPathReferences(new TestXPathResolver(), "/*/${xpath1} | ${xpath2}/text()");
    assertEquals("Multiple XPath references in same string are replaced (with separator and leading/trailing content)", "/*/" + XPATH_1 + " | " + XPATH_2 + "/text()", lResult);

    lResult = StoredXPathTranslator.instance().translateXPathReferences(new TestXPathResolver(), "${xpath1}${xpath2}");
    assertEquals("Multiple XPath references in same string are replaced (no separator)", XPATH_1 + XPATH_2, lResult);
  }

  @Test
  public void testXPathIntegration()
  throws ExTooMany, ExTooFew, ExBadPath {
    //Tests the XPath engine can use the local RequestContext to translate XPath references
    TestActionRequestContext lRequestContext = new TestActionRequestContext();
    SaxonEnvironment.setThreadLocalRequestContext(lRequestContext);

    DOM lDOM = DOM.createDocumentFromXMLString("<ROOT><ELEMENT_1/><ELEMENT_2/><ELEMENT_3/></ROOT>");

    DOM lResult = lDOM.xpath1E("${xpath1}");
    assertNotNull("XPath selected element (simple XPath)", lResult);
    assertEquals("XPath selected correct element", "ELEMENT_1", lResult.getName());

    lResult = lDOM.xpath1E("${xpath1}[1][substring(name(), 1, 1) = 'E']");
    assertNotNull("XPath selected element (complex XPath)", lResult);
    assertEquals("XPath selected correct element", "ELEMENT_1", lResult.getName());

    DOMList lListResult = lDOM.xpathUL("${xpath1} | ${xpath2}");
    assertEquals("XPath selected 2 elements (complex XPath UL)", 2, lListResult.getLength());
  }

  private class TestXPathResolver
  implements StoredXPathResolver{
    @Override
    public String resolveXPath(String pXPathName) {
      switch (pXPathName) {
        case "xpath1":
          return XPATH_1;
        case "xpath2":
          return XPATH_2;
        default:
          return null;
      }
    }
  }

  /**
   * ActionRequestContext which only provides a TestXPath resolver.
   */
  public class TestActionRequestContext
  implements ActionRequestContext {

    @Override
    public StoredXPathResolver getStoredXPathResolver() {
      return new TestXPathResolver();
    }

    @Override
    public XPathVariableManager getXPathVariableManager() {
      return null;
    }

    @Override
    public ThreadInfoProvider getThreadInfoProvider() {
      return null;
    }

    @Override
    public ContextUElem getContextUElem() {
      return null;
    }

    @Override
    public App getModuleApp() {
      return null;
    }

    @Override
    public Mod getCurrentModule() {
      return null;
    }

    @Override
    public State getCurrentState() {
      return null;
    }

    @Override
    public EntryTheme getCurrentTheme() {
      return null;
    }

    @Override
    public App getRequestApp() {
      return null;
    }

    @Override
    public AuthenticationContext getAuthenticationContext() {
      return null;
    }

    @Override
    public XDoCommandList resolveActionName(String pActionName) {
      return null;
    }

    @Override
    public XDoRunner createCommandRunner(boolean pIsTopLevel) {
      return null;
    }

    @Override
    public XDoIsolatedRunner createIsolatedCommandRunner(boolean pErrorOnTransformation) {
      return null;
    }

    @Override
    public void addXDoResult(XDoResult pXDoResult) {

    }

    @Override
    public <T extends XDoResult> List<T> getXDoResults(Class<T> pForClass) {
      return null;
    }

    @Override
    public DOMHandlerProvider getDOMHandlerProvider() {
      return null;
    }

    @Override
    public ExitResponse getDefaultExitResponse() {
      return null;
    }

    @Override
    public XDoControlFlow handleStateStackTransformation(StateStackTransformation pTransformation) {
      return null;
    }

    @Override
    public XThreadInterface createNewXThread(ModuleCall.Builder pModuleCallBuilder, boolean pSameSession) {
      return null;
    }

    @Override
    public void addSysDOMInfo(String pPath, String pContent) {

    }

    @Override
    public void changeAttachPoint(String pAttachToXPath) {

    }

    @Override
    public void changeSecurityScope(SecurityScope pSecurityScope) {

    }

    @Override
    public MapSet resolveMapSet(String pMapSetName, DOM pItemDOM, String pMapSetAttachXPath) {
      return null;
    }

    @Override
    public MapSet resolveMapSet(String pMapSetName, DOM pItemDOM, DOM pMapSetAttachDOM) {
      return null;
    }

    @Override
    public InterfaceQuery resolveInterfaceQuery(String pDBInterfaceName, String pQueryName) {
      return null;
    }

    @Override
    public void refreshMapSets(String pMapSetName) {

    }

    @Override
    public void postDOM(String pDOMLabel) {

    }

    @Override
    public PersistenceContext getPersistenceContext() {
      return null;
    }

    @Override
    public DevToolbarContext getDevToolbarContext() {
      return null;
    }

    @Override
    public DownloadManager getDownloadManager() {
      return null;
    }

    @Override
    public TempResourceProvider getTempResourceProvider() {
      return null;
    }

    @Override
    public <T extends ModuleFacetProvider> T getModuleFacetProvider(Class<T> pProviderClass) {
      return null;
    }

    @Override
    public String getCurrentCallId() {
      return null;
    }

    @Override
    public void applyClientActions(String pClientActionJSON) {

    }

    @Override
    public FoxRequest getFoxRequest() {
      return null;
    }

    @Override
    public ContextUCon getContextUCon() {
      return null;
    }

    @Override
    public String getRequestAppMnem() {
      return null;
    }

    @Override
    public FoxSession getFoxSession() {
      return null;
    }

    @Override
    public void forceNewFoxSession(FoxSession pNewSession) {

    }

    @Override
    public SecurityScope getCurrentSecurityScope() {
      return null;
    }

    @Override
    public RequestURIBuilder createURIBuilder() {
      return null;
    }
  }

}
