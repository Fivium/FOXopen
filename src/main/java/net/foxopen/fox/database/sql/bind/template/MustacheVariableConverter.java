package net.foxopen.fox.database.sql.bind.template;

import net.foxopen.fox.dom.DOM;
import net.foxopen.fox.dom.xpath.XPathResult;
import net.foxopen.fox.ex.ExInternal;
import net.foxopen.fox.track.Track;
import net.sf.saxon.value.AtomicValue;
import nu.xom.Node;

import java.util.List;

/**
 * Converts XPathResults into an appropriate format for a Mustache template.
 */
public class MustacheVariableConverter
implements TemplateVariableConverter {

  public static final MustacheVariableConverter INSTANCE = new MustacheVariableConverter();

  private MustacheVariableConverter() {}

  /**
   * Converts the given XPathResult into an object which can be used in a Mustache template. The following rules apply:
   * <ul>
   *   <li>If the result is a sequence, only the first item is used</li>
   *   <li>If the item is a boolean it is returned as is</li>
   *   <li>If the item is a node, the shallow string value of the node is returned if the trimmed value is not null</li>
   *   <li>For other item types the item's string value is returned</li>
   * </ul>
   * @param pVariableName Name of variable being converted, for debug purposes.
   * @param pXPathResult Result to convert.
   * @return An object which can be applied to a Mustache template, or null.
   */
  public Object convertVariableObject(String pVariableName, XPathResult pXPathResult) {

    Object pObject = pXPathResult.asObject();

    //For lists only take the first item
    if(pObject instanceof List) {
      List lResultObjectAsList = (List) pObject;
      if (lResultObjectAsList.size() > 1) {
        Track.alert("MustacheTemplateVariable", "Bind '" + pVariableName + "' resulted in a many-item sequence, taking first item only");
      }
      else if (lResultObjectAsList.size() == 0) {
        return null;
      }

      return convert(lResultObjectAsList.get(0));
    }
    else {
      return convert(pObject);
    }
  }

  private Object convert(Object pObject) {

    //Mustache treats nulls and booleans specially so they shouldn't be wrapped
    if(pObject == null || pObject instanceof Boolean) {
      return pObject;
    }
    else if(pObject instanceof List) {
      throw new ExInternal("Cannot wrap a list for use in a mustache template");
    }
    else {

      String lObjectStringValue;
      if(pObject instanceof DOM) {
        lObjectStringValue = ((DOM) pObject).value(false);
      }
      else if(pObject instanceof Node){
        lObjectStringValue = new DOM((Node) pObject).value(false);
      }
      else if(pObject instanceof AtomicValue) {
        lObjectStringValue = ((AtomicValue) pObject).getStringValue();
      }
      else {
        lObjectStringValue = pObject.toString();
      }

      if(lObjectStringValue.trim().length() > 0) {
        return lObjectStringValue;
      }
      else {
        return null;
      }
    }
  }
}
