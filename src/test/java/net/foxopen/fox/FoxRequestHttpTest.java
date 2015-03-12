package net.foxopen.fox;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.junit.Test;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;


public class FoxRequestHttpTest {
  public FoxRequestHttpTest() {
  }

  private static final String JSON_STRING = "[{\"t\" : \"111\", \"f\" : \"aaa\"}, {\"t\" : \"222\", \"f\" : \"bbb\"}, {\"t\" : \"333\", \"f\" : \"ccc\"}]";


  @Test
  public void testJSONCookieSerialiser() throws UnsupportedEncodingException {
    JSONArray lJSONArray = (JSONArray) JSONValue.parse(JSON_STRING);
    String lCookieString = FoxRequestHttp.serialiseJSONArrayToCookieString(lJSONArray, 1000);

    assertTrue("Full JSON array is serialised to under 1000 characters", lCookieString.length() < 1000);

    // Test all values were serialised and none got cut by the 1000 char limit
    Set<String> lFoundThreads = new HashSet<>();
    JSONArray lParsedResult = (JSONArray)JSONValue.parse(URLDecoder.decode(lCookieString, "UTF-8"));
    for (Object lItem : lParsedResult) {
      lFoundThreads.add((String) ((JSONObject) lItem).get("t")); // I wish the JSON lib used generics!
    }
    Set<String> lExpectedThreads = new HashSet<>();
    lExpectedThreads.add("111");
    lExpectedThreads.add("222");
    lExpectedThreads.add("333");
    assertEquals("Full JSON array is serialised", lExpectedThreads, lFoundThreads);
  }

  @Test
  public void testJSONCookieSerialiser_TrimsFirstItemsWhenLengthExceeded() throws UnsupportedEncodingException {
    JSONArray lJSONArray = (JSONArray) JSONValue.parse(JSON_STRING);

    // Test first item (thread 111) gets trimmed when the size limit is exceeded when serialising
    String lCookieString = FoxRequestHttp.serialiseJSONArrayToCookieString(lJSONArray, 150);
    Set<String> lFoundThreads = new HashSet<>();
    JSONArray lParsedResult = (JSONArray)JSONValue.parse(URLDecoder.decode(lCookieString, "UTF-8"));
    for (Object lItem : lParsedResult) {
      lFoundThreads.add((String) ((JSONObject) lItem).get("t")); // I wish the JSON lib used generics!
    }
    Set<String> lExpectedThreads = new HashSet<>();
    lExpectedThreads.add("222");
    lExpectedThreads.add("333");
    assertEquals("Cookie trims first item in list when length exceeded", lExpectedThreads, lFoundThreads);

    //the length of the 3 items is 103 chars
    // Test first two items (thread 111 and 222) get trimmed when the size limit is exceeded when serialising
    lCookieString = FoxRequestHttp.serialiseJSONArrayToCookieString(lJSONArray, 102);
    lFoundThreads.clear();
    lParsedResult = (JSONArray)JSONValue.parse(URLDecoder.decode(lCookieString, "UTF-8"));
    for (Object lItem : lParsedResult) {
      lFoundThreads.add((String) ((JSONObject) lItem).get("t")); // I wish the JSON lib used generics!
    }
    lExpectedThreads.clear();
    lExpectedThreads.add("333");
    assertEquals("Cookie trims first 2 items in list when length exceeded", lExpectedThreads, lFoundThreads);
  }

  @Test
  public void testJSONCookieSerialiser_EmptyJSONArray() {
    JSONArray lJSONArray = new JSONArray();
    String lCookieString = FoxRequestHttp.serialiseJSONArrayToCookieString(lJSONArray, 100);
    assertEquals("Empty array is serialised", "%5B%5D", lCookieString);
  }
}
