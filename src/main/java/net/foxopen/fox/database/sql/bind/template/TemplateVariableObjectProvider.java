package net.foxopen.fox.database.sql.bind.template;

import net.foxopen.fox.database.sql.bind.BindObjectProvider;
import net.foxopen.fox.dom.xpath.XPathResult;

import java.util.Map;

/**
 * A specialised BindObjectProivder which can also be used to provide template variables. As opposed to returning BindObjects,
 * a template provider must provide XPathResults so the consuming template engine can convert the raw result to an appropriate
 * object for the template.
 */
public interface TemplateVariableObjectProvider
extends BindObjectProvider {

  /**
   * Tests if a template variable of the given name is available from this provider. This should return true if a definition
   * exists - the variable itself may be null or empty.
   * @param pVariableName Name of template variable to check.
   * @return True if a definition exists for the variable.
   */
  boolean isTemplateVariableDefined(String pVariableName);

  /**
   * Resolves an XPathResult for the given variable name. This method should return null if the variable is not defined.
   * @param pVariableName Variable to resolve.
   * @return XPathResult for the value of the given variable, or null.
   */
  XPathResult getXPathResultForTemplateVariable(String pVariableName);

  /**
   * Creates a wrapper for this provider, which exposes available variables via the Map interface. The Map should be treated
   * as read only.
   * @return Map wrapper for this provider.
   */
  default Map<String, Object> asTemplateVariableMap() {
    return new TemplateVariableObjectProviderMapAdaptor(this);
  }

}
