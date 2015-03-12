package net.foxopen.fox.configuration.resourcemaster.definition;

import net.foxopen.fox.database.UCon;
import net.foxopen.fox.database.UConBindMap;
import net.foxopen.fox.database.UConStatementResult;
import net.foxopen.fox.dom.DOM;
import net.foxopen.fox.ex.ExApp;
import net.foxopen.fox.ex.ExDB;
import net.foxopen.fox.ex.ExFoxConfiguration;
import net.foxopen.fox.ex.ExServiceUnavailable;
import net.foxopen.fox.sql.SQLManager;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FoxEnvironmentDefinition
extends FoxConfigDefinition {
  private final static String GET_FOX_ENVIRONMENT_FILENAME = "GetEngineConfig.sql";
  private final static String GET_FOX_ENVIRONMENT_APPS_FILENAME = "GetAppDefinitions.sql";
  private final static String FOX_APP_MNEM_COL = "APP_MNEM";

  private final Map<String, FoxApplicationDefinition> mAppMnemToAppDefinition = new HashMap<>();

  public static FoxEnvironmentDefinition createFoxEnvironmentDefinition(UCon pUCon, String pFoxEnvironmentKey)
  throws ExServiceUnavailable, ExApp, ExFoxConfiguration {
    List<UConStatementResult> lConfigRows;
    try {
      UConBindMap lBindMap = new UConBindMap().defineBind(":environment_key", pFoxEnvironmentKey);
      lConfigRows = pUCon.queryMultipleRows(SQLManager.instance().getStatement(GET_FOX_ENVIRONMENT_FILENAME, FoxEnvironmentDefinition.class), lBindMap);
    }
    catch (ExDB e) {
      throw new ExApp("Error while trying to acquire the environment definition from the database", e);
    }

    return new FoxEnvironmentDefinition(FoxConfigDefinition.createPropertyDOMListFromStatement(lConfigRows), pUCon, pFoxEnvironmentKey);
  }

  private FoxEnvironmentDefinition(List<PropertyDOM> pPropertyDOMList, UCon pUCon, String pFoxEnvironmentKey) throws ExApp, ExFoxConfiguration {
    super(pPropertyDOMList);
    // select out applications for the environment
    try {
      UConBindMap lBindMap = new UConBindMap();
      lBindMap.defineBind(":environment_key", pFoxEnvironmentKey);
      List<UConStatementResult> lAppList = pUCon.queryMultipleRows(SQLManager.instance().getStatement(GET_FOX_ENVIRONMENT_APPS_FILENAME, FoxEnvironmentDefinition.class), lBindMap);
      for (UConStatementResult lApp : lAppList) {
        String lAppMnem = lApp.getString(FOX_APP_MNEM_COL);
        FoxApplicationDefinition lAppDefinition = FoxApplicationDefinition.createAppDefinition(pUCon, pFoxEnvironmentKey, lAppMnem);
        mAppMnemToAppDefinition.put(lAppMnem, lAppDefinition);
      }
    }
    catch (ExDB e) {
      throw new ExApp("Error while trying to acquire the environment definition from the database", e);
    }
  }

  public Map<String, FoxApplicationDefinition> getAppMnemToAppDefinition() {
    return mAppMnemToAppDefinition;
  }

  public String getPropertyAsString(FoxEnvironmentProperty pEnvironmentProperty)
  throws ExApp {
    if (!pEnvironmentProperty.isXML()) {
      String lDefault = pEnvironmentProperty.getDefaultValue() == null ? "" : pEnvironmentProperty.getDefaultValue().toString();
      return getPropertyAsString(pEnvironmentProperty.getPath(), pEnvironmentProperty.isMandatory(), lDefault);
    }
    else {
      throw new ExApp(pEnvironmentProperty + " is a DOM property");
    }
  }

  public DOM getPropertyAsDOM(FoxEnvironmentProperty pEnvironmentProperty)
  throws ExApp {
    if (pEnvironmentProperty.isXML()) {
      return getPropertyAsDOM(pEnvironmentProperty.getPath(), pEnvironmentProperty.isMandatory());
    }
    else {
      throw new ExApp(pEnvironmentProperty + " is not a DOM property");
    }
  }

  public int getPropertyAsInteger(FoxEnvironmentProperty pEnvironmentProperty)
  throws ExApp {
    if (!pEnvironmentProperty.isXML()) {
      return getPropertyAsInteger(pEnvironmentProperty.getPath(), pEnvironmentProperty.isMandatory(), (Integer) pEnvironmentProperty.getDefaultValue());
    }
    else {
      throw new ExApp(pEnvironmentProperty + " is a DOM property");
    }
  }
}
