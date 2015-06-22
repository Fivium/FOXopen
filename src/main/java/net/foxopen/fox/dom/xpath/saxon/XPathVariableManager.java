package net.foxopen.fox.dom.xpath.saxon;

import net.foxopen.fox.dom.xpath.XPathResult;

import java.util.Map;

/**
 * An object which is capable of storing and resolving XPath variables for use by the XPath processing engine. Variables
 * may be "global" or "local". Global variables may be set and unset programmatically. Local variables are set when
 * {@link #localise(String, Map)} is called and are immutable thereafter. A manager may be localised multiple times, but
 * a matching call to {@link #delocalise(String)} should always be made when the local scope is no longer required.
 */
public interface XPathVariableManager {

  /**
   * Resolves a variable name to a previously determined value, or null if the variable is not defined in this manager.
   * The following object types may be returned:
   * <ul>
   *   <li>Unconnected DOM element (i.e. a DOM which is not in a parent document)</li>
   *   <li>String</li>
   *   <li>Number</li>
   *   <li>Boolean</li>
   *   <li>Any {@link net.sf.saxon.type.AtomicType}</li>
   *   <li>A Collection containing any of the above (Collections will not be nested)</li>
   * </ul>
   * Both local and global variables are searched, starting from the most recent local scope.
   *
   * @param pVariableName Name of variable to resolve.
   * @return Variable value or null.
   */
  Object resolveVariable(String pVariableName);

  /**
   * Sets the value of a global variable from an XPath result.
   * @param pVariableName Name of variable to set. This must be a valid NCName.
   * @param pXPathResult XPath result to use as the value.
   */
  void setVariableFromXPathResult(String pVariableName, XPathResult pXPathResult);

  /**
   * Sets the value of a global variable to an arbitrary object (which may be a Collection). The manager may attempt to convert
   * value objects to a suitable format. If the object if not of a valid type, an exception is thrown.
   * @param pVariableName Name of variable to set. This must be a valid NCName.
   * @param pValue Value to set variable to.
   */
  void setVariable(String pVariableName, Object pValue);

  /**
   * Clears (unsets) a global variable.
   * @param pVariableName Variable to clear.
   */
  void clearVariable(String pVariableName);

  /**
   * Tests if a variable of the given name has been set on this manager. This should be used instead of checking for null
   * results from {@link #resolveVariable}, because it is possible that a variable may be set to a null value.
   * @param pVariableName Variable to check.
   * @param pLocalOnly If true, only local variables are searched. If false, both local and global variables are searched.
   * @return True if the variable is set on this manager, even if it has a null value.
   */
  boolean isVariableSet(String pVariableName, boolean pLocalOnly);

  /**
   * Localises this manager and sets the given local variables, which will be available in the new scope and any nested
   * scopes. {@link XPathResult}s in the map are converted into value objects as they would be for {@link #setVariableFromXPathResult}.
   * @param pPurpose Localisation purpose, for debugging.
   * @param pLocalVariables 0 or more local variables to set (map key is variable name).
   */
  void localise(String pPurpose, Map<String, Object> pLocalVariables);

  /**
   * Delocalises this manager, unsetting any local variables which were set with the corresponding {@link #localise} call.
   * @param pPurpose Localisation purpose, for validating that the stack has been correctly maintained.
   */
  void delocalise(String pPurpose);

}
