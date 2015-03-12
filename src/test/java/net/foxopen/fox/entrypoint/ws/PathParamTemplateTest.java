package net.foxopen.fox.entrypoint.ws;

import java.util.Map;

import static org.junit.Assert.assertEquals;
import org.junit.Test;

public class PathParamTemplateTest {

  /**
   * @see PathParamTemplate#parseURI(java.lang.String)
   */
  @Test
  public void testParse_SingleParam() {
    PathParamTemplate lPathParam = new PathParamTemplate("{param1}");
    Map<String, String> lParamMap = lPathParam.parseURI("/my_param_1");

    assertEquals("First parameter correctly read from URI", "my_param_1", lParamMap.get("param1"));
  }

  /**
   * @see PathParamTemplate#parseURI(java.lang.String)
   */
  @Test
  public void testParse_SingleParamPath() {
    PathParamTemplate lPathParam = new PathParamTemplate("path/{param1}");
    Map<String, String> lParamMap = lPathParam.parseURI("path/my_param_1");

    assertEquals("First parameter correctly read from URI", "my_param_1", lParamMap.get("param1"));
  }

  /**
   * @see PathParamTemplate#parseURI(java.lang.String)
   */
  @Test
  public void testParse_MultiParamPath() {
    PathParamTemplate lPathParam = new PathParamTemplate("path/{param1}/more_path/{param2}");
    Map<String, String> lParamMap = lPathParam.parseURI("path/my_param_1/more_path/2");

    assertEquals("First parameter correctly read from URI", "my_param_1", lParamMap.get("param1"));
    assertEquals("Second parameter correctly read from URI", "2", lParamMap.get("param2"));

  }

  /**
   * @see PathParamTemplate#parseURI(java.lang.String)
   */
  @Test
  public void testParse_MultiParamPath_LeadingSlash() {
    PathParamTemplate lPathParam = new PathParamTemplate("/path/{param1}/more_path/{param2}");
    Map<String, String> lParamMap = lPathParam.parseURI("/path/my_param_1/more_path/2");

    assertEquals("First parameter correctly read from URI", "my_param_1", lParamMap.get("param1"));
  }


  /**
   * @see PathParamTemplate#parseURI(java.lang.String)
   */
  @Test(expected = Throwable.class)
  public void testParse_FailsWhenNotMatched() {
    PathParamTemplate lPathParam = new PathParamTemplate("/path/{param1}");
    Map<String, String> lParamMap = lPathParam.parseURI("/not_the_path/my_param_1");
  }

}
