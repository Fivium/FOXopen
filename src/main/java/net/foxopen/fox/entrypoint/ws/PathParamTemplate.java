package net.foxopen.fox.entrypoint.ws;

import net.foxopen.fox.XFUtil;
import net.foxopen.fox.ex.ExInternal;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * A PathParamTemplate contains an end points's definition of its URI suffix, which may contain parameters to be passed to
 * the end point. Paramaters are delimited by curly braces and expanded into a map when {@link #parseURI} is invoked.
 * The suffix is all of the URI which appears after the end point's path name. For instance given the template string:<br><br>
 *
 * <code>/document/{doc_id}/format/{doc_format}</code><br><br>
 *
 * When invoked as the following URI:<br><br>
 *
 * <code>/fox/ws/rest/category/service/end_point/document/100/format/pdf</code><br><br>
 *
 * Will result in a parameter map like this:
 *
 * <pre>
 * doc_id => 100
 * doc_format => pdf
 * </pre>
 *
 * Requests to the endpoint must match this pattern. If they do not, an error is raised. For the purposes of this class
 * an "end point" refers to either a WebService EndPoint, or an arbitrary servlet path.
 */
public class PathParamTemplate {

  /** Original URI template string provided to the object - for debugging/reporting */
  private final String mOriginalTemplate;

  /** Parsed template with {params} converted to regex wildcard groups (i.e. "(.*?)") */
  private final String mParsedTemplate;

  /** List of param names ordered by their appearance in the template string */
  private final List<String> mParamNames;

  /**
   * Constructs a new PathParamTemplate which will match the given string.
   * @param pPathTemplate
   */
  public PathParamTemplate(String pPathTemplate) {

    mOriginalTemplate = XFUtil.pathStripLeadSlashes(pPathTemplate);

    Pattern p = Pattern.compile("\\{(.*?)\\}");
    Matcher m = p.matcher(mOriginalTemplate);

    mParamNames = new ArrayList<>();
    StringBuffer lParsedTemplate = new StringBuffer();
    while(m.find()) {
      mParamNames.add(m.group(1));
      m.appendReplacement(lParsedTemplate, "(.*?)");
    }
    m.appendTail(lParsedTemplate);

    mParsedTemplate = lParsedTemplate.toString();
  }

  /**
   * Parses the given URI path into a map of string parameters, based on the current template. The URI path should only
   * contain the relevant part of the URI, for instance for the pattern:<br><br>
   *
   * <code>/param/{param1}</code><br><br>
   *
   * Only the following URI should be used for the match:<br><br>
   *
   * <code>/param/my_param_values</code><br><br>
   *
   * An error is thrown if the match fails.
   * @param pURIPath Paramater path part of the URI.
   * @return The path parsed into a map of parameters based upon this object's parameter path defintion.
   */
  public Map<String, String> parseURI(String pURIPath) {

    Pattern p = Pattern.compile(mParsedTemplate);
    Matcher m = p.matcher(XFUtil.pathStripLeadSlashes(pURIPath));
    if(m.matches()) {
      //For each param, get the value from its corresponding regex match group (note +1 as group 0 is the whole matched pattern)
      Map<String, String> lParamMap = new HashMap<>();
      for(int i=0; i < mParamNames.size(); i++) {
        lParamMap.put(mParamNames.get(i), m.group(i+1));
      }
      return lParamMap;
    }
    else {
      throw new ExInternal("Provided URI path " + pURIPath + " does not match expected pattern " + mOriginalTemplate);
    }
  }

  /**
   * Generates an actual URI suffix for matching against this PathParamTemplate, based on the given parameters. If the
   * template requires a parameter which is not provided in the map, an error is raised. The parameter map may contain
   * additional parameters which are not used by this template - in this case, no error is raised.<br><br>
   *
   * Example: for the template <tt>"/thread/{thread_id}"</tt>, this method will return <tt>"/thread/123"</tt> if passed a
   * param map containing the mapping <tt>thread_id => 123</tt>.<br><br>
   *
   * Regardless of the original template string, this method always appends a preceding slash to the path so results are
   * consistent.
   * @param pParamMap Map of param names to values. Values will be URLEncoded by this method. Null values are converted
   *                  to empty strings.
   * @return This template, "filled in" with paramters from the map.
   */
  public String generateURIFromParamMap(Map<String, String> pParamMap) {

    String lResult = mOriginalTemplate;
    for(String lParamName : mParamNames) {
      if(pParamMap.containsKey(lParamName)) {
        try {
          lResult = lResult.replace("{" + lParamName +  "}", URLEncoder.encode(XFUtil.nvl(pParamMap.get(lParamName)), "UTF-8"));
        }
        catch (UnsupportedEncodingException e) {
          throw new ExInternal("Unable to encode path param", e);
        }
      }
      else {
        throw new ExInternal("PathParamTemplate requires parameter " + lParamName + " to be specified");
      }
    }

    if(!lResult.startsWith("/")) {
      lResult = "/" + lResult;
    }

    return lResult;
  }

  /**
   * Tests if this template contains a parameter of the given name.
   * @param pParamName Name to test.
   * @return
   */
  public boolean hasParam(String pParamName) {
    return mParamNames.contains(pParamName);
  }

  public String toString() {
    return mOriginalTemplate;
  }
}
