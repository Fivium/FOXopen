package net.foxopen.fox.thread.stack;

import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import net.foxopen.fox.App;
import net.foxopen.fox.ContextLabel;
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
import net.foxopen.fox.dom.xpath.ContextualityLevel;
import net.foxopen.fox.dom.xpath.XPathResult;
import net.foxopen.fox.dom.xpath.saxon.SaxonEnvironment;
import net.foxopen.fox.dom.xpath.saxon.StoredXPathResolver;
import net.foxopen.fox.dom.xpath.saxon.XPathVariableManager;
import net.foxopen.fox.download.DownloadManager;
import net.foxopen.fox.entrypoint.FoxSession;
import net.foxopen.fox.entrypoint.uri.RequestURIBuilder;
import net.foxopen.fox.ex.ExActionFailed;
import net.foxopen.fox.ex.ExInternal;
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
import net.foxopen.fox.thread.persistence.kryo.KryoManager;
import net.foxopen.fox.thread.stack.transform.StateStackTransformation;
import net.foxopen.fox.thread.storage.TempResourceProvider;
import net.sf.saxon.value.DateValue;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static junit.framework.Assert.assertNull;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class ModuleXPathVariableManagerTest {

  private DOM mRootDOM;
  private ContextUElem mContextUElem;
  private ActionRequestContext mActionRequestContext;
  private ModuleXPathVariableManager mVariableManager;

  @Before
  public void setUpContextUElem() {

    mContextUElem = new ContextUElem();
    mRootDOM = DOM.createDocumentFromXMLString("<ROOT>\n" +
                                               "  <ITEM_LIST>\n" +
                                               "    <ITEM>Item 1</ITEM>\n" +
                                               "    <ITEM>Item 2</ITEM>\n" +
                                               "    <ITEM>Item 3</ITEM>\n" +
                                               "  </ITEM_LIST>\n" +
                                               "  <FORM>\n" +
                                               "    <FIELD_1>Field 1</FIELD_1>\n" +
                                               "    <FIELD_2>Field 2</FIELD_2>\n" +
                                               "  </FORM>\n" +
                                               "</ROOT>");
    mContextUElem.setUElem("ctxt", ContextualityLevel.LOCALISED, mRootDOM);
    mContextUElem.setUElem(ContextLabel.ATTACH, mRootDOM);

    mVariableManager = new ModuleXPathVariableManager();

    mActionRequestContext = new TestRequestContext();

    SaxonEnvironment.clearThreadLocalRequestContext();
    SaxonEnvironment.setThreadLocalRequestContext(mActionRequestContext);
  }

  @After
  public void cleanup(){
    SaxonEnvironment.clearThreadLocalRequestContext();
  }

  private XPathResult getXPathResult(String pFoxExtendedXpath)
  throws ExActionFailed {
    return mContextUElem.extendedXPathResult(mContextUElem.attachDOM(), pFoxExtendedXpath);
  }

  @Test
  public void testDOMListConversion()
  throws ExActionFailed {
    //DOM results from XPath evaluation should be cloned into unconnected elements and stored as a list
    mVariableManager.setVariableFromXPathResult("item-list", getXPathResult(":{ctxt}//ITEM"));

    Object lVariable = mVariableManager.resolveVariable("item-list");

    assertTrue("Variable is a list", lVariable instanceof List);

    List lVarAsList = (List) lVariable;

    assertTrue("List contains DOM objects", lVarAsList.get(0) instanceof DOM);
    assertEquals("List contains three items", 3, lVarAsList.size());
    assertEquals("First item is Item 1", "Item 1", ((DOM) lVarAsList.get(0)).value());
    assertFalse("DOM item is unattached", ((DOM) lVarAsList.get(0)).isAttached());
  }

  @Test
  public void testEmptyListConversion()
  throws ExActionFailed {
    //Variables should be able to be set to empty list
    mVariableManager.setVariable("empty-list", new ArrayList<>());

    Object lVariable = mVariableManager.resolveVariable("empty-list");

    assertTrue("Variable is a list", lVariable instanceof List);

    List lVarAsList = (List) lVariable;

    assertEquals("List contains 0 items", 0, lVarAsList.size());
  }

  @Test
  public void testStringListConversion()
  throws ExActionFailed {
    //Text nodes from XPath results should be converted to strings
    mVariableManager.setVariableFromXPathResult("string-list", getXPathResult(":{ctxt}//ITEM/text()"));

    Object lVariable = mVariableManager.resolveVariable("string-list");

    assertTrue("Variable is a list", lVariable instanceof List);

    List lVarAsList = (List) lVariable;

    assertEquals("List contains three items", 3, lVarAsList.size());
    assertEquals("1st item is Item 1", "Item 1", lVarAsList.get(0).toString());
    assertEquals("3rd item is Item 3", "Item 3", lVarAsList.get(2).toString());
  }

  @Test
  public void testMixedListConversion()
  throws ExActionFailed {
    //Tests a variable can be set to an XPath sequence containing various item types

    mVariableManager.setVariableFromXPathResult("item-list", getXPathResult("(:{ctxt}/FORM/FIELD_1, :{ctxt}/FORM/FIELD_1/text(), 'fixed string', 2, 3.14, xs:date('2014-01-01'))"));

    Object lVariable = mVariableManager.resolveVariable("item-list");

    assertTrue("Variable is a list", lVariable instanceof List);

    List lVarAsList = (List) lVariable;

    assertEquals("List contains 6 items", 6, lVarAsList.size());

    assertEquals("1st item is DOM for element FIELD_1", "FIELD_1", ((DOM) lVarAsList.get(0)).getName());
    assertEquals("2nd item is string text value of FIELD_1", "Field 1", lVarAsList.get(1));
    assertEquals("3rd item is constant text value", "fixed string", lVarAsList.get(2));
    assertEquals("4th item is integer 2", 2, ((BigInteger) lVarAsList.get(3)).intValue());
    assertEquals("5th item is decimal 3.14", 3.14, ((BigDecimal) lVarAsList.get(4)).doubleValue(), 0.1);
    assertTrue("6th item is date", lVarAsList.get(5) instanceof DateValue);
    assertEquals("6th item is date with correct value", "2014-01-01", ((DateValue) lVarAsList.get(5)).getStringValue());
  }

  @Test
  public void testValueResolvingWithinXPaths_SetDirectly()
  throws ExActionFailed {
    //Tests variables are correctly retrieved/typed after they are set "directly" on the manager (i.e. not from an XPath result)

    mVariableManager.setVariable("string", "string value");
    mVariableManager.setVariable("integer", 1);
    mVariableManager.setVariable("double", 3.14d);
    mVariableManager.setVariable("boolean", true);
    mVariableManager.setVariable("dom", DOM.createDocumentFromXMLString("<ELEMENT>element value</ELEMENT>"));
    mVariableManager.setVariable("dom-list", mRootDOM.getUL("/*/ITEM_LIST/ITEM"));
    mVariableManager.setVariable("empty-list", mRootDOM.getUL("/*/ITEM_LIST/NOT_AN_ITEM"));
    mVariableManager.setVariable("collection", Arrays.asList("string value", 1, DOM.createDocumentFromXMLString("<ELEMENT>element value</ELEMENT>")));

    assertEquals("String value resolved correctly", "string value", getXPathResult("$string").asString());
    assertEquals("Integer value resolved correctly", 1, getXPathResult("$integer").asNumber().intValue());
    assertEquals("Double value resolved correctly", 3.14, getXPathResult("$double").asNumber().doubleValue(), 0.1);
    assertTrue("Boolean value resolved correctly", getXPathResult("$boolean").asBoolean());
    assertEquals("DOM element resolved correctly", "ELEMENT", getXPathResult("$dom").asResultDOMOrNull().getName());
    assertEquals("DOM element text value resolved correctly", "element value", getXPathResult("$dom/text()").asString());

    assertEquals("DOM list resolved correctly", 3, getXPathResult("$dom-list").asDOMList().size());
    assertEquals("DOM list resolved correctly", "Item 1", getXPathResult("$dom-list[1]/text()").asString());

    assertEquals("Empty list resolved correctly", 0, getXPathResult("count($empty-list)").asNumber().intValue());

    assertEquals("String in collection resolved correctly", "string value", getXPathResult("$collection[1]").asString());
    assertEquals("Integer in collection resolved correctly", 1, getXPathResult("$collection[2]").asNumber().intValue());
    assertEquals("DOM element in collection resolved correctly", 1, getXPathResult("$collection[2]").asNumber().intValue());

  }

  private void setFromXPathResults()
  throws ExActionFailed {

    mVariableManager.setVariableFromXPathResult("string", getXPathResult(":{ctxt}/FORM/FIELD_1/text() || ' concat'"));
    mVariableManager.setVariableFromXPathResult("integer", getXPathResult("count(:{ctxt}/ITEM_LIST/ITEM)"));
    mVariableManager.setVariableFromXPathResult("double", getXPathResult("count(:{ctxt}/ITEM_LIST/ITEM) div 9"));
    mVariableManager.setVariableFromXPathResult("boolean", getXPathResult("true()"));
    mVariableManager.setVariableFromXPathResult("date", getXPathResult("xs:date('2014-01-01')"));
    mVariableManager.setVariableFromXPathResult("date-time", getXPathResult("xs:dateTime('2014-01-01T01:02:03')"));
    mVariableManager.setVariableFromXPathResult("year-month-duration", getXPathResult("xs:date('2015-01-01') - xs:date('2014-01-01')"));
    mVariableManager.setVariableFromXPathResult("day-time-duration", getXPathResult("xs:dateTime('2014-01-01T12:00:00') - xs:dateTime('2014-01-01T00:00:00')"));

    mVariableManager.setVariableFromXPathResult("dom", getXPathResult(":{ctxt}/FORM"));
    mVariableManager.setVariableFromXPathResult("dom-list", getXPathResult(":{ctxt}/ITEM_LIST/ITEM"));
    mVariableManager.setVariableFromXPathResult("empty-list", getXPathResult(":{ctxt}/ITEM_LIST/ITEM[1=0]"));
    mVariableManager.setVariableFromXPathResult("collection", getXPathResult("(:{ctxt}/FORM/FIELD_1, :{ctxt}/FORM/FIELD_1/text(), 'fixed string', 2, 3.14, xs:date('2014-01-01'))"));
  }

  private void assertXPathResults()
  throws ExActionFailed {
    //Asserts the variable values set in setFromXPathResults

    assertEquals("String value resolved correctly", "Field 1 concat", getXPathResult("$string").asString());
    assertEquals("Integer value resolved correctly", 3, getXPathResult("$integer").asNumber().intValue());
    assertEquals("Double value resolved correctly", 0.33, getXPathResult("$double").asNumber().doubleValue(), 0.1);
    assertTrue("Boolean value resolved correctly", getXPathResult("$boolean").asBoolean());
    assertEquals("Date value resolved correctly", 2014, getXPathResult("year-from-date($date)").asNumber().intValue());
    assertEquals("DateTime value resolved correctly", 2, getXPathResult("minutes-from-dateTime($date-time)").asNumber().intValue());
    assertEquals("YearMonthDuration value resolved correctly", "P365D", getXPathResult("string($year-month-duration)").asString());
    assertEquals("DayTimeDuration value resolved correctly", "PT12H", getXPathResult("string($day-time-duration)").asString());


    assertEquals("DOM element resolved correctly", "FORM", getXPathResult("$dom").asResultDOMOrNull().getName());
    assertEquals("DOM element can be used in XPath function", "FORM_NAME", getXPathResult("name($dom) || '_NAME'").asString());
    assertEquals("DOM element can be added to sequence", "FORM_NAME", getXPathResult("name($dom) || '_NAME'").asString());
    assertEquals("DOM child element resolved correctly", "Field 1", getXPathResult("$dom/FIELD_1/text()").asString());

    assertEquals("DOM list resolved correctly", 3, getXPathResult("$dom-list").asDOMList().size());
    assertEquals("DOM list can be referred to as sequence", "Item 1", getXPathResult("$dom-list[1]/text()").asString());

    assertEquals("Empty list resolved correctly", 0, getXPathResult("count($empty-list)").asNumber().intValue());

    assertEquals("DOM element in collection resolved correctly", "FIELD_1", getXPathResult("name($collection[1])").asString());
    assertEquals("Text node string in collection resolved correctly", "Field 1", getXPathResult("$collection[2]").asString());
    assertEquals("Fixed string in collection resolved correctly", "fixed string", getXPathResult("$collection[3]").asString());
    assertEquals("Integer in collection resolved correctly", 2, getXPathResult("$collection[4]").asNumber().intValue());
    assertEquals("Float in collection resolved correctly", 3.14, getXPathResult("$collection[5]").asNumber().doubleValue(), 0.1);
    assertEquals("Date in collection resolved correctly", "2014-01-01", getXPathResult("string($collection[6])").asString());
  }

  @Test
  public void testValueResolvingWithinXPaths_SetFromXPathResult()
  throws ExActionFailed {
    //Tests XPath results can be set on the manager and then immediately resolved (without serialise/deserialise cycle)
    setFromXPathResults();
    assertXPathResults();
  }

  @Test
  public void testSerialise_Deserialise()
  throws ExActionFailed, IOException {
    //Tests  XPath results can be set on the manager and still referenced correctly after a deserialise

    setFromXPathResults();

    //Cycle object through serialise/deserialise and test results are identical
    ByteArrayOutputStream lBAOS = new ByteArrayOutputStream();
    Output lOutput = new Output(lBAOS, 100);
    KryoManager.getKryoInstance().writeClassAndObject(lOutput, mVariableManager);
    lOutput.close();

    mVariableManager = (ModuleXPathVariableManager) KryoManager.getKryoInstance().readClassAndObject(new Input(new ByteArrayInputStream(lBAOS.toByteArray())));

    assertXPathResults();
  }

  @Test(expected = ExInternal.class)
  public void testNameValidation_NoColonsAllowed() {
    mVariableManager.setVariable("ns:var", "1");
  }

  @Test(expected = ExInternal.class)
  public void testNameValidation_NameStartsWithAlphaChar() {
    mVariableManager.setVariable("12var", "1");
  }

  @Test(expected = ExInternal.class)
  public void testNameValidation_NameNotEmpty() {
    mVariableManager.setVariable("", "1");
  }

  @Test(expected = ExInternal.class)
  public void testNameValidation_NameNotNull() {
    mVariableManager.setVariable(null, "1");
  }

  @Test
   public void testClearVariable() {
    mVariableManager.setVariable("testVar", "test");
    assertNotNull("Variable available immediately after setting", mVariableManager.resolveVariable("testVar"));
    mVariableManager.clearVariable("testVar");
    assertNull("Variable not available after clearing", mVariableManager.resolveVariable("testVar"));
  }

  @Test
  public void testLocalVariables()
  throws ExActionFailed {
    mVariableManager.setVariable("globalVar", "test");

    Map<String, Object> lLocalVars = new HashMap<>();
    lLocalVars.put("localStringVar", "test string");
    lLocalVars.put("localIntegerVar", 2);
    lLocalVars.put("localXPathVar", getXPathResult(":{ctxt}/FORM/FIELD_1"));

    assertNull("Local variables not available before localise", mVariableManager.resolveVariable("localStringVar"));

    mVariableManager.localise("testing", lLocalVars);

    assertEquals("Local string variable can now be resolved", "test string", mVariableManager.resolveVariable("localStringVar"));
    assertEquals("Local integer variable can now be resolved", 2, mVariableManager.resolveVariable("localIntegerVar"));
    assertEquals("Local XPath variable can now be resolved", "FIELD_1", ((List<DOM>) mVariableManager.resolveVariable("localXPathVar")).get(0).getName());

    mVariableManager.delocalise("testing");

    assertNull("Local string variable not available after delocalise", mVariableManager.resolveVariable("localStringVar"));
    assertNull("Local integer variable not available after delocalise", mVariableManager.resolveVariable("localIntegerVar"));
    assertNull("Local XPath variable not available after delocalise", mVariableManager.resolveVariable("localXPathVar"));
  }

  @Test
  public void testLocalVariables_MultipleLocalisations() {

    mVariableManager.setVariable("globalVar", "global level");

    Map<String, Object> lLocalVars = new HashMap<>();
    lLocalVars.put("localVar", "localised level 1");
    lLocalVars.put("localVar2", "localised level 1");

    mVariableManager.localise("level 1", lLocalVars);

    assertEquals("Global variable resolved to correct value", "global level", mVariableManager.resolveVariable("globalVar"));
    assertEquals("Overloaded local variable resolved to level 1 value", "localised level 1", mVariableManager.resolveVariable("localVar"));
    assertEquals("Non-overloaded local variable resolved to level 1 value", "localised level 1", mVariableManager.resolveVariable("localVar2"));

    //Localise again with overloaded localVar
    lLocalVars = new HashMap<>();
    lLocalVars.put("localVar", "localised level 2");
    lLocalVars.put("level2Var", "localised level 2");

    mVariableManager.localise("level 2", lLocalVars);
    assertEquals("Global variable resolved to correct value", "global level", mVariableManager.resolveVariable("globalVar"));
    assertEquals("Overloaded local variable resolved to level 2 value", "localised level 2", mVariableManager.resolveVariable("localVar"));
    assertEquals("Non-overloaded local variable resolved to level 1 value", "localised level 1", mVariableManager.resolveVariable("localVar2"));
    assertNotNull("New local variable now available", mVariableManager.resolveVariable("level2Var"));

    mVariableManager.delocalise("level 2");

    assertEquals("Global variable resolved to correct value", "global level", mVariableManager.resolveVariable("globalVar"));
    assertEquals("Overloaded local variable resolved to level 1 value", "localised level 1", mVariableManager.resolveVariable("localVar"));
    assertEquals("Non-overloaded local variable resolved to level 1 value", "localised level 1", mVariableManager.resolveVariable("localVar2"));
    assertNull("Level 2 local variable no longer available", mVariableManager.resolveVariable("level2Var"));

    mVariableManager.delocalise("level 1");

    assertEquals("Global variable resolved to correct value", "global level", mVariableManager.resolveVariable("globalVar"));
    assertNull("Overloaded local variable no longer available", mVariableManager.resolveVariable("localVar"));
    assertNull("Non-overloaded local variable no longer available", mVariableManager.resolveVariable("localVar2"));
  }

  @Test
  public void testCannotClearLocalVariable() {

    mVariableManager.setVariable("globalVar", "test");

    Map<String, Object> lLocalVars = new HashMap<>();
    lLocalVars.put("localVar", "localised");

    mVariableManager.localise("test", lLocalVars);

    mVariableManager.clearVariable("localVar");
    assertNotNull("Attempt to clear local variable does not succeed", mVariableManager.resolveVariable("localVar"));

    mVariableManager.clearVariable("globalVar");
    assertNull("Global variable is cleared even when localised", mVariableManager.resolveVariable("globalVar"));

  }

  @Test
  public void testSetVariableWhenLocalised() {
    mVariableManager.localise("test", Collections.emptyMap());
    mVariableManager.setVariable("globalVar", "test");
    mVariableManager.delocalise("test");

    assertNotNull("Global variable is available even though it was set when localised", mVariableManager.resolveVariable("globalVar"));
  }

  @Test
  public void testIsVariableSet() {

    mVariableManager.setVariable("globalVar", "test");
    assertTrue("globalVar is set (global search)", mVariableManager.isVariableSet("globalVar", false));
    assertFalse("globalVar is not set (local search)", mVariableManager.isVariableSet("globalVar", true));

    mVariableManager.clearVariable("globalVar");
    assertFalse("globalVar is not set after being cleared", mVariableManager.isVariableSet("globalVar", true));

    mVariableManager.setVariable("emptyVar", Collections.emptyList());
    assertTrue("nullVar is set even though value is empty sequence", mVariableManager.isVariableSet("emptyVar", false));

    Map<String, Object> lLocalVars = new HashMap<>();
    lLocalVars.put("localVar", "localised");
    mVariableManager.localise("test", lLocalVars);
    mVariableManager.setVariable("localVar", "test");

    assertTrue("localVar is set (global search)", mVariableManager.isVariableSet("localVar", false));
    assertTrue("localVar is set (local search)", mVariableManager.isVariableSet("localVar", true));

  }

  @Test
  public void testIsSetXPathFunction()
  throws ExActionFailed {

    mVariableManager.setVariable("testVar", "test");
    assertTrue("XPath function detects variable is set", getXPathResult("fox:is-set('testVar')").asBoolean());
    assertFalse("XPath function detects variable is not set", getXPathResult("fox:is-set('unsetVar')").asBoolean());

  }

  @Test(expected = ExInternal.class)
  public void testDelocaliseFailsOnMismatch() {

    mVariableManager.localise("L1", Collections.emptyMap());
    mVariableManager.localise("L2", Collections.emptyMap());
    //Should fail because L2 is top of stack
    mVariableManager.delocalise("L1");
  }

  @Test
  public void testGetAllVariableNames() {

    mVariableManager.setVariable("globalVar", "test");
    Map<String, Object> lLocalVars = new HashMap<>();
    lLocalVars.put("localVar", "localised");
    mVariableManager.localise("test", lLocalVars);

    assertTrue("Name collection contains global variable", mVariableManager.getAllVariableNames().contains("globalVar"));
    assertTrue("Name collection contains local variable", mVariableManager.getAllVariableNames().contains("localVar"));
  }

  private class TestRequestContext implements ActionRequestContext {

    @Override
    public XPathVariableManager getXPathVariableManager() {
      return mVariableManager;
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
    public StoredXPathResolver getStoredXPathResolver() {
      return null;
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
