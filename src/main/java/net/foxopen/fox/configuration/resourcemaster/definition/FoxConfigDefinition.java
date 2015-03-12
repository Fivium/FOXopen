package net.foxopen.fox.configuration.resourcemaster.definition;

import net.foxopen.fox.XFUtil;
import net.foxopen.fox.database.UConStatementResult;
import net.foxopen.fox.dom.DOM;
import net.foxopen.fox.ex.ExApp;
import net.foxopen.fox.ex.ExTooFew;
import net.foxopen.fox.ex.ExTooMany;

import java.util.ArrayList;
import java.util.List;

public abstract class FoxConfigDefinition {

  private final List<PropertyDOM> mPropertyDOMList;

  protected static List<PropertyDOM> createPropertyDOMListFromStatement(List<UConStatementResult> lAppPropertiesSQLList) {

    List<PropertyDOM> lPropertyDOMList = new ArrayList<>();

    for (UConStatementResult lAppProperty : lAppPropertiesSQLList) {
      String lPropertyLocation = lAppProperty.getString("LOCATION");
      DOM lPropertyDOM = lAppProperty.getDOMFromSQLXML("CONFIG_XML");
      if(lPropertyDOM != null) {
        lPropertyDOMList.add(new PropertyDOM(lPropertyDOM, lPropertyLocation));
      }
    }

    return lPropertyDOMList;
  }

  protected FoxConfigDefinition(List<PropertyDOM> pPropertyDOMList) {
    mPropertyDOMList = pPropertyDOMList;
  }

//  public String getPropertyAsString(AppProperty pAppPropertyName) throws ExApp {
//    return getPropertyAsString(pAppPropertyName, "");
//  }

  protected String getPropertyAsString(String pPropertyPath, boolean pIsMandatory, String pDefaultValue)
  throws ExApp {
    String lPropertyText = "";
    for (PropertyDOM lPropertyDOM : mPropertyDOMList) {
      DOM lCurrentDOM = lPropertyDOM.getDOM();
      try {
        lPropertyText = lCurrentDOM.get1S(pPropertyPath);
      }
      catch (ExTooFew ignore) { }
      catch (ExTooMany e) {
        throw new ExApp("Invalid XML definition: property path " + pPropertyPath + " returns more than 1 element in " + lPropertyDOM.getLocation(), e);
      }

      if (!XFUtil.isNull(lPropertyText)) {
        // Put into map here of properties and where they come from.
        return lPropertyText;
      }
    }

    if (pIsMandatory) {
      throw new ExApp("Mandatory property was not found in any config locations for path " + pPropertyPath);
    }
    else {
      return pDefaultValue;
    }
  }

  protected DOM getPropertyAsDOM(String pPropertyPath, boolean pIsMandatory)
  throws ExApp {
    DOM lProperty = null;
    for (PropertyDOM lPropertyDOM : mPropertyDOMList) {
      DOM lCurrentDOM = lPropertyDOM.getDOM();
      try {
        lProperty = lCurrentDOM.get1E(pPropertyPath);
      }
      catch (ExTooFew ignore) { }
      catch (ExTooMany e) {
        throw new ExApp("Invalid XML definition: property path " + pPropertyPath + " returns more than 1 element in " + lPropertyDOM.getLocation(), e);
      }

      if (!XFUtil.isNull(lProperty)) {
        return lProperty;
      }
    }

    if (pIsMandatory) {
      throw new ExApp("No DOM property was found in any config locations for path " + pPropertyPath);
    }
    else {
      return null;
    }
  }

  protected boolean getPropertyAsBoolean(String pPropertyPath, boolean pIsMandatory, boolean pDefaultValueIfNotMandatory)
  throws ExApp {
    String lPropertyValue = getPropertyAsString(pPropertyPath, pIsMandatory, "");

    if (XFUtil.isNull(lPropertyValue)) {
      return pDefaultValueIfNotMandatory;
    }
    else if (!("false".equals(lPropertyValue) || "true".equals(lPropertyValue))) {
      throw new ExApp("A boolean property was not equal to true or false for property path " + pPropertyPath);
    }

    return Boolean.parseBoolean(lPropertyValue);
  }

  protected int getPropertyAsInteger(String pPropertyPath, boolean pIsMandatory, int pDefaultValueIfNotMandatory)
  throws ExApp {
    String lPropertyValue = getPropertyAsString(pPropertyPath, pIsMandatory, "");

    if (XFUtil.isNull(lPropertyValue)) {
      return pDefaultValueIfNotMandatory;
    }
    else {
      try {
        return Integer.parseInt(lPropertyValue);
      }
      catch(NumberFormatException e) {
        throw new ExApp("Error converting the property " + pPropertyPath + " to a number", e);
      }
    }
  }

  public String getPropertyLocation(String pPropertyPath) {
    for (PropertyDOM lPropertyDOM : mPropertyDOMList) {
      String lCurrentLocation = lPropertyDOM.getLocation();
      DOM lProperty = lPropertyDOM.getDOM().get1EOrNull(pPropertyPath);
      if (!XFUtil.isNull(lProperty)) {
        return lCurrentLocation;
      }
    }

    return "ENGINE_DEFAULT";
  }
}
