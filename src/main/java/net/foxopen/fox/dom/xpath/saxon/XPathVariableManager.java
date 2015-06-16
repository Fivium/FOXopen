package net.foxopen.fox.dom.xpath.saxon;

import net.foxopen.fox.dom.xpath.XPathResult;

/**
 * An object which is capable of storing and resolving XPath variables for use by the XPath processing engine.
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
   * @param pVariableName Name of variable to resolve.
   * @return Variable value or null.
   */
  Object resolveVariable(String pVariableName);

  /**
   * Sets the value of a variable from an XPath result.
   * @param pVariableName Name of variable to set. This must be a valid NCName.
   * @param pXPathResult XPath result to use as the value.
   */
  void setVariableFromXPathResult(String pVariableName, XPathResult pXPathResult);

  /**
   * Sets the value of a variable to an arbitrary object (which may be a Collection). The manager may attempt to convert
   * value objects to a suitable format. If the object if not of a valid type, an exception is thrown.
   * @param pVariableName Name of variable to set. This must be a valid NCName.
   * @param pValue Value to set variable to.
   */
  void setVariable(String pVariableName, Object pValue);

}
