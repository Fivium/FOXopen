package net.foxopen.fox.database.sql.bind.template;

import net.foxopen.fox.dom.xpath.XPathResult;

/**
 * Implementors can convert XPath results into objects which can be used as values for their corresponding template type.
 */
public interface TemplateVariableConverter {

  /**
   * Converts the given XPathResult into an object which can be used in a template. May return null if the XPathResult
   * represents a null value and the template engine supports null values.
   * @param pVariableName Name of variable being converted, for debug purposes.
   * @param pXPathResult Result to convert.
   * @param pIsXMLBind If true, a DOM node from XPath results should be converted into a boolean to represent their existence.
   *                   If false, the node's text content should be used.
   * @return An object which can be applied to a template, or null.
   */
  Object convertVariableObject(String pVariableName, XPathResult pXPathResult, boolean pIsXMLBind);

}