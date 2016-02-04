package net.foxopen.fox.dom.xpath.saxon;

import net.foxopen.fox.XFUtil;
import net.foxopen.fox.dom.xpath.XPathDefinition;
import net.foxopen.fox.ex.ExInternal;

import java.util.Collection;
import java.util.HashSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Singleton which can be used to convert stored XPath references within XPath strings into actual XPaths from a module
 * definition.
 */
public class StoredXPathTranslator {

  private static final StoredXPathTranslator INSTANCE = new StoredXPathTranslator();

  private static final Pattern FOX_STORED_XPATH_NAME_PATTERN = Pattern.compile("[^\\s\\${}]+?");
  private static final Pattern FOX_STORED_XPATH_MATCH_PATTERN = Pattern.compile("\\$\\{([^\\s]+?)\\}");

  /** Pattern to match $ and \ characters, which need to be escaped before the string can be used as a replacement value */
  private static final Pattern SPECIAL_CHAR_ESCAPE_PATTERN = Pattern.compile("[\\$\\\\]");

  public static StoredXPathTranslator instance() {
    return INSTANCE;
  }

  private StoredXPathTranslator() {}

  /**
   * Validates that the given name is an allowed XPath name, i.e. is allowed to appear in <tt>${xpath}</tt> syntax.
   * @param pXPathName XPath name to validate.
   * @return True if valid, false otherwise.
   */
  public boolean validateXPathName(String pXPathName) {
    return FOX_STORED_XPATH_NAME_PATTERN.matcher(pXPathName).matches();
  }

  /**
   * Tests if the given XPath <i>probably</i> contains an XPath reference. This is a quick check which should be used to
   * decide whether to invoke {@link #translateXPathReferences}.
   * @param pXPath XPath to test.
   * @return True if the given XPath probably contains an XPath reference and needs to be translated.
   */
  public boolean containsStoredXPathReference(String pXPath) {
    return pXPath.contains("${");
  }

  /**
   * Translates stored XPath references in the given string into their corresponding XPaths. This signature relies on the
   * SaxonEnvironment ThreadLocalRequestContext to provide a StoredXPathResolver.
   *
   * @param pXPath XPath potentially containing references to translate.
   * @return XPathDefinition containing the translated XPath and the original path string used to construct it.
   */
  public XPathDefinition translateXPathReferences(String pXPath) {
    return XPathDefinition.forRewrittenXPath(pXPath, translateXPathReferences(SaxonEnvironment.getThreadLocalRequestContext().getStoredXPathResolver(), pXPath));
  }

  /**
   * Translates stored XPath references in the given string into their corresponding XPaths.
   *
   * @param pStoredXPathResolver  StoredXPathResolver to use to resolve XPath references.
   * @param pXPath XPath potentially containing references to translate.
   * @return Given XPath with any stored XPath references translated.
   */
  public String translateXPathReferences(StoredXPathResolver pStoredXPathResolver, String pXPath) {
    Matcher lMatcher = FOX_STORED_XPATH_MATCH_PATTERN.matcher(pXPath);

    StringBuffer lResult = new StringBuffer();
    while(lMatcher.find()) {
      String lRef = lMatcher.group(1);
      String lResolvedXPath = pStoredXPathResolver.resolveXPath(lRef);

      if(XFUtil.isNull(lResolvedXPath)) {
        throw new ExInternal("No XPath definition found for name '" + lRef + "'");
      }

      //Escape $ and \ by replacing each char with itself preceded by a backslash - otherwise, appendReplacement() treats the characters specially
      lResolvedXPath = SPECIAL_CHAR_ESCAPE_PATTERN.matcher(lResolvedXPath).replaceAll("\\\\$0");

      lMatcher.appendReplacement(lResult, lResolvedXPath);
    }
    lMatcher.appendTail(lResult);

    return lResult.toString();
  }

  /**
   * Gets a set of the names of all the stored XPath references found within the given path. The set is in no defined order.
   * @param pXPath XPath to examine.
   * @return Set of 0 or more XPath references found in the XPath.
   */
  public Collection<String> allReferencesInPath(String pXPath) {
    Matcher lMatcher = FOX_STORED_XPATH_MATCH_PATTERN.matcher(pXPath);
    Collection<String> lResult = new HashSet<>();
    while(lMatcher.find()) {
      lResult.add(lMatcher.group(1));
    }

    return lResult;
  }
}
