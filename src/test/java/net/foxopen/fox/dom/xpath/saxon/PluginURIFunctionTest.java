package net.foxopen.fox.dom.xpath.saxon;

import net.foxopen.fox.dom.DOM;
import net.foxopen.fox.entrypoint.uri.RequestURIBuilderImpl;
import net.foxopen.fox.ex.ExBadPath;
import net.foxopen.fox.ex.ExPathInternal;
import net.foxopen.fox.ex.ExTooFew;
import net.foxopen.fox.ex.ExTooMany;
import net.foxopen.fox.thread.ActionRequestContext;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import javax.servlet.http.HttpServletRequest;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class PluginURIFunctionTest {

  private DOM mDOM = DOM.createDocumentFromXMLString("<ROOT><TEXT>abc</TEXT></ROOT>");

  @Before
  public void setup() {

    //Mock HTTP request which can get the servlet's context name
    HttpServletRequest lMockRequest = mock(HttpServletRequest.class);
    when(lMockRequest.getContextPath()).thenReturn("/testContext");

    //Mock action request context which returns a URI builder based on the mock request
    ActionRequestContext lRequestContext = mock(ActionRequestContext.class);
    when(lRequestContext.createURIBuilder()).thenAnswer(
      invocation -> RequestURIBuilderImpl.createFromHttpRequest(lMockRequest)
    );

    //Register on the thread local
    SaxonEnvironment.setThreadLocalRequestContext(lRequestContext);
  }

  @Test
  public void testBasicInvocation()
  throws ExTooMany, ExTooFew, ExBadPath {

    assertEquals("Can build a basic URI without parameters", "/testContext/ws/rest/plugin/pluginName/endPoint", mDOM.xpath1S("fox:plugin-uri('pluginName','endPoint')"));

    String lResult = mDOM.xpath1S("fox:plugin-uri('pluginName','endPoint', 'param1', 'value1')");
    assertEquals("Can build a basic URI with 1 string parameter", "/testContext/ws/rest/plugin/pluginName/endPoint?param1=value1", lResult);

    lResult = mDOM.xpath1S("fox:plugin-uri('pluginName','endPoint', 'param1', 'value1', 'param2', 'value2')");
    assertEquals("Can build a basic URI with 2 string parameters", "/testContext/ws/rest/plugin/pluginName/endPoint?param1=value1&param2=value2", lResult);

    lResult = mDOM.xpath1S("fox:plugin-uri('pluginName','endPoint', 'param1', 1, 'param2', 3.14, 'param3', xs:date('2015-01-01'))");
    assertEquals("Can build a basic URI with integer, float and date parameters", "/testContext/ws/rest/plugin/pluginName/endPoint?param1=1&param2=3.14&param3=2015-01-01", lResult);

    lResult = mDOM.xpath1S("fox:plugin-uri('pluginName','endPoint', 'param1', ./TEXT)");
    assertEquals("Can build a basic URI with element node parameter", "/testContext/ws/rest/plugin/pluginName/endPoint?param1=abc", lResult);

    lResult = mDOM.xpath1S("fox:plugin-uri('pluginName','endPoint', 'param1', ./TEXT/text())");
    assertEquals("Can build a basic URI with text node parameter", "/testContext/ws/rest/plugin/pluginName/endPoint?param1=abc", lResult);
  }

  @Test
  public void testNullParameterHandling()
  throws ExTooMany, ExTooFew, ExBadPath {

    String lResult = mDOM.xpath1S("fox:plugin-uri('pluginName','endPoint', 'param1', ./NO_TEXT, 'param2', 'text')");
    assertEquals("Node value parameter is excluded if node not found", "/testContext/ws/rest/plugin/pluginName/endPoint?param2=text", lResult);

    lResult = mDOM.xpath1S("fox:plugin-uri('pluginName','endPoint', 'param1', '', 'param2', 'text')");
    assertEquals("Empty string parameter is excluded", "/testContext/ws/rest/plugin/pluginName/endPoint?param2=text", lResult);
  }

    @Test
  public void testEncoding()
  throws ExTooMany, ExTooFew, ExBadPath {
    String lResult = mDOM.xpath1S("fox:plugin-uri('pluginName','endPoint', 'param name', 'param value')");
    assertEquals("Spaces in names and values are encoded", "/testContext/ws/rest/plugin/pluginName/endPoint?param+name=param+value", lResult);

    lResult = mDOM.xpath1S("fox:plugin-uri('pluginName','endPoint', 'param1', '+=value')");
    assertEquals("Special characters in values are encoded", "/testContext/ws/rest/plugin/pluginName/endPoint?param1=%2B%3Dvalue", lResult);
  }


  @Test(expected = ExPathInternal.class)
  public void testParameterMissingValue_Fails()
  throws ExTooMany, ExTooFew, ExBadPath {
    mDOM.xpath1S("fox:plugin-uri('pluginName','endPoint', 'param1')");
  }

  @Test(expected = ExPathInternal.class)
  public void testEmptyParameterName_Fails()
  throws ExTooMany, ExTooFew, ExBadPath {
    mDOM.xpath1S("fox:plugin-uri('pluginName','endPoint', '', 'value')");
  }

  @Test(expected = ExPathInternal.class)
  public void testSecondParameterMissingValue_Fails()
  throws ExTooMany, ExTooFew, ExBadPath {
    mDOM.xpath1S("fox:plugin-uri('pluginName','endPoint', 'param1', 'value1', 'param2')");
  }

  @Test(expected = ExPathInternal.class)
  public void testParameterNameMustBeString()
  throws ExTooMany, ExTooFew, ExBadPath {
    mDOM.xpath1S("fox:plugin-uri('pluginName','endPoint', 1, 2)");
  }

  @After
  public void cleanUp() {
    SaxonEnvironment.clearThreadLocalRequestContext();
  }

}

