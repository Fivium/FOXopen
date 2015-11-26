package net.foxopen.fox;

import net.foxopen.fox.dom.xpath.ContextualityLevel;

import java.util.HashMap;
import java.util.Map;


/**
 * This enum defines built-in context labels which are set and used internally by the FOX engine.
 * It also encapsulates some properties of the built in context labels, namely their {@link ContextualityLevel}.<br/><br/>
 *
 * The ContextLabel enum should be expanded whenever a new built-in context label is introduced into FOX.
 */
public enum ContextLabel {

  /**
   * The root element of the module's Root DOM. The Root DOM is used to store persistent data on a per-module basis.
   * It is serialised to the database after every transaction processing cycle.
   */
  ROOT("root", ContextualityLevel.DOCUMENT),

  /**
   * The root element of the module's Theme DOM. The Theme DOM is used to store transient data for the duration of a
   * module call.
   */
  THEME("theme", ContextualityLevel.DOCUMENT),

  /**
   * The root element of the module's Environment DOM. The Environment DOM is used to store session level configuration
   * and is copied forward at the start of a module call.
   */
  ENV("env", ContextualityLevel.DOCUMENT),

  /**
   * The root element of the module's Parameter DOM. The Parameter DOM is populated with the parameters with which the
   * module was invoked. These could be from an fm:call-module command, from the GET parameter list, etc.
   */
  PARAMS("params", ContextualityLevel.DOCUMENT),

  /**
   * The root element of the module's Return DOM. The Return DOM is populated by a module to return data to its caller.
   */
  RETURN("return", ContextualityLevel.DOCUMENT),

  /**
   * The root element of the module's Result DOM. The Result DOM is populated with the Return DOM of a returned module
   * call.
   */
  RESULT("result", ContextualityLevel.DOCUMENT),

  /**
   * The root element of the module's Error DOM. The Error DOM is populated when errors are encountered by the fm:validate
   * command.
   */
  ERROR("error", ContextualityLevel.DOCUMENT),

  /**
   * The root element of the current Session DOM. The Session DOM is maintained for the duration of the browser session
   * and is accessible to all module calls within the scope of that browser session.
   */
  SESSION("session", ContextualityLevel.DOCUMENT),

  /**
   * The root element of the Temp DOM. The Temp DOM is used as a temporary area for DOM manipulation and is reset at the
   * end of every transaction processing cycle.
   */
  TEMP("temp", ContextualityLevel.DOCUMENT),

  /**
   * The root element of the User DOM. The User DOM is automatically popuplated with user credential information at the
   * start of every transaction processing cycle.
   */
  USER("user", ContextualityLevel.DOCUMENT),

  /**
   * The root element of the System DOM. The System DOM is populated with useful information about the FOX environment
   * and is refreshed at the start of every transaction processing cycle.
   */
  SYS("sys", ContextualityLevel.DOCUMENT),

  /**
   * The root element of the Preferences DOM. The Preferences DOM is used to permanently store a user's application-specific
   * preferences. It is only loaded from the database when required by an XPath. FOX modules may write data to it, but
   * the writes must conform to a specific schema.
   */
  PREFS("prefs", ContextualityLevel.DOCUMENT),

  /**
   * A pointer to the header of a SOAP envelope. Only available in entry theme do blocks which are processing web service
   * requests. The body of the SOAP envelope, which is what most applications need access to, is available in the Params
   * DOM.
   */
  SERVICE_HEADER("service-header", ContextualityLevel.DOCUMENT),

  /**
   * A special DOM used with storage location fm:valdiation blocks. The DOM contains the converted results of the storage
   * location's SELECT statement.
   */
  STORAGE_LOCATION_SELECT("storage-location-select", ContextualityLevel.LOCALISED),

  /**
   * The attach point of the current state. This is also used as the default context item of an XPath expression if one
   * is not explicitly specified. Note that some commands override the state's attach context with their own - this
   * behaviour is incorrect, as it means the state's attach point is no longer discernible. New code should not follow
   * this pattern.
   */
  ATTACH("attach", ContextualityLevel.STATE),

  /**
   * The relative node of the most recently run action. If the action was run without a contextual node, this will
   * generally default to the state's attach point.
   */
  ACTION("action", ContextualityLevel.STATE),

  /**
   * The baseself context is set to the initial context node of an XPath expression whenever that expression is executed.
   * This is used to overcome a weakness of XPath whereby the context node changes as the expression is run, thus
   * losing the previous reference. For instance given the path <code>./X1/Y[./X2 = 1]</code>, the '.' in the predicate
   * refers to node 'Y'. :{baseself} would be used to reference the initial context node within the predicate.
   */
  BASESELF("baseself", ContextualityLevel.ITEM),

  /**
   * An alternate attach point for a mapset, used when evaluating the mapset's <tt>fm:do</tt> block. This will be different
   * for every invocation.
   */
  MAP_SET_ATTACH("map-set-attach", ContextualityLevel.ITEM),

  /**
   * The assignee context is used by the fm:assign command to allow assignment expressions to reference the node which
   * they are assigning to.
   */
  ASSIGNEE("assignee", ContextualityLevel.ITEM),

  /**
   * The item context is used by mapsets and change actions to determine the context node for mapset initialisation or
   * change action processing.
   */
  ITEM("item", ContextualityLevel.ITEM),

  /**
   * The itemrec context is used by mapsets and change actions to determine the context node's parent for mapset
   * initialisation or change action processing.
   */
  ITEMREC("itemrec", ContextualityLevel.ITEM);

  //private static final

  private static final Map<String, ContextLabel> gReverseLookupMap = new HashMap<>();
  static {
    for(ContextLabel lLabel : values()) {
      gReverseLookupMap.put(lLabel.mLabel, lLabel);
    }
  }

  private final String mLabel;
  private final ContextualityLevel mContextualityLevel;

  /**
   * Gets the ContextLabel which corresponds to the given label name String.
   * @param pLabelName The name of the desired ContextLabel.
   * @return The matching ContextLabel, or null if not defined.
   */
  public static ContextLabel getContextLabel(String pLabelName) {
    //Implementation note: this COULD just use Enum.valueOf() but this method allows more flexibility with enum names
    return gReverseLookupMap.get(pLabelName);
  }

  public static boolean isLabelNameValid(String pLabel) {
    return pLabel != null && pLabel.matches("[A-Za-z0-9-_]+");
  }


  private ContextLabel(String pLabel, ContextualityLevel pContextualityLevel) {
    mLabel = pLabel;
    mContextualityLevel = pContextualityLevel;
  }

  /**
   * Get the string representation of this label. E.g. "root", "theme", etc.
   * @return The label as a String.
   */
  public String asString() {
    return mLabel;
  }

  /**
   * Get the string representation of this label as it would appear in an XPath, wrapped with ":{" and "}" tokens.
   * E.g. ":{root}", ":{theme}".
   * @return The label nested in "colon-squiggly" syntax.
   */
  public String asColonSquiggly() {
    return ":{" + mLabel + "}";
  }

  /**
   * Returns true if this context label is allowed to be set externally by a user (e.g. using the fm:context-set command).
   */
  public boolean isSetable() {
    return mContextualityLevel != ContextualityLevel.DOCUMENT && this != BASESELF && this != ASSIGNEE;
  }

  /**
   * Returns true if this context label is allowed to be cleared from a ContextUElem.
   */
  public boolean isClearable() {
    //No built in context labels can currently be cleared.
    return false;
  }

  /**
   * Returns the ContextualityLevel of this context label.
   */
  public ContextualityLevel contextualityLevel(){
    return mContextualityLevel;
  }
}
