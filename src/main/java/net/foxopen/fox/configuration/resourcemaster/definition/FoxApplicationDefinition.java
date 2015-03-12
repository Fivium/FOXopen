 package net.foxopen.fox.configuration.resourcemaster.definition;

import net.foxopen.fox.database.UCon;
import net.foxopen.fox.database.UConBindMap;
import net.foxopen.fox.database.UConStatementResult;
import net.foxopen.fox.dom.DOM;
import net.foxopen.fox.ex.ExApp;
import net.foxopen.fox.ex.ExDB;
import net.foxopen.fox.sql.SQLManager;

import java.util.List;

public class FoxApplicationDefinition
extends FoxConfigDefinition {
  private final static String GET_FOX_APP_PROPERTIES = "GetAppProperties.sql";

  private final String mAppMnem;

  public static FoxApplicationDefinition createAppDefinition(UCon pUCon, String pFoxEnvironmentKey, String pAppMnem) throws ExApp {
    // Get the list of all property values from each fox table.
    UConBindMap lBindMap = new UConBindMap();
    lBindMap.defineBind(":environment_key", pFoxEnvironmentKey);
    lBindMap.defineBind(":app_mnem", pAppMnem);

    List<PropertyDOM> lPropertyDOMList;
    try {
      List<UConStatementResult> lAppPropertiesSQLList = pUCon.queryMultipleRows(SQLManager.instance().getStatement(GET_FOX_APP_PROPERTIES, FoxApplicationDefinition.class), lBindMap);
      lPropertyDOMList = FoxConfigDefinition.createPropertyDOMListFromStatement(lAppPropertiesSQLList);
    }
    catch (ExDB e) {
      throw new ExApp("Error while trying to acquire the app definition from the database with application " + pAppMnem, e);
    }

    // Construct the app that does the look up of values
    return new FoxApplicationDefinition(pAppMnem, lPropertyDOMList);
  }

  private FoxApplicationDefinition(String pAppMnem, List<PropertyDOM> pPropertyDOMList)
  throws ExApp {
    super(pPropertyDOMList);
    mAppMnem = pAppMnem;
  }

  public String getAppMnem() {
    return mAppMnem;
  }

  public boolean getPropertyAsBoolean(AppProperty pAppProperty)
  throws ExApp {
    return getPropertyAsBoolean(pAppProperty.getPath(), pAppProperty.isMandatory(), (Boolean) pAppProperty.getDefaultValue());
  }

  public int getPropertyAsInteger(AppProperty pAppPropertyName)
  throws ExApp {
    return getPropertyAsInteger(pAppPropertyName.getPath(), pAppPropertyName.isMandatory(), (Integer) pAppPropertyName.getDefaultValue());
  }

  public String getPropertyAsString(AppProperty pAppProperty)
  throws ExApp {
    if (!pAppProperty.isXML()) {
      String lDefaultValue = pAppProperty.getDefaultValue() == null ? "" : pAppProperty.getDefaultValue().toString();
      return getPropertyAsString(pAppProperty.getPath(), pAppProperty.isMandatory(), lDefaultValue);
    }
    else {
      throw new ExApp(pAppProperty + " is a DOM property");
    }
  }

  public DOM getPropertyAsDOM(AppProperty pAppProperty)
  throws ExApp {
    if (pAppProperty.isXML()) {
      return getPropertyAsDOM(pAppProperty.getPath(), pAppProperty.isMandatory());
    }
    else {
      throw new ExApp(pAppProperty + " is a String property");
    }
  }
}
