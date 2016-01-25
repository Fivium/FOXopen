package net.foxopen.fox.configuration.resourcemaster.model;

import net.foxopen.fox.XFUtil;
import net.foxopen.fox.boot.EngineInitialisationController;
import net.foxopen.fox.configuration.resourcemaster.definition.AppProperty;
import net.foxopen.fox.configuration.resourcemaster.definition.FoxApplicationDefinition;
import net.foxopen.fox.configuration.resourcemaster.definition.FoxEnvironmentDefinition;
import net.foxopen.fox.configuration.resourcemaster.definition.FoxEnvironmentProperty;
import net.foxopen.fox.dom.DOM;
import net.foxopen.fox.enginestatus.MessageLevel;
import net.foxopen.fox.enginestatus.StatusDestination;
import net.foxopen.fox.enginestatus.StatusDetail;
import net.foxopen.fox.enginestatus.StatusItem;
import net.foxopen.fox.enginestatus.StatusProvider;
import net.foxopen.fox.enginestatus.StatusTable;
import net.foxopen.fox.enginestatus.StatusText;
import net.foxopen.fox.entrypoint.FoxGlobals;
import net.foxopen.fox.entrypoint.servlets.FoxBootServlet;
import net.foxopen.fox.ex.ExApp;

import java.util.Map;

class EnvironmentConfigStatusProvider
implements StatusProvider {

  EnvironmentConfigStatusProvider() {
  }

  @Override
  public void refreshStatus(StatusDestination pDestination) {

    if(FoxGlobals.getInstance().isEngineInitialised()) {
      final FoxEnvironmentDefinition lEnvironmentDefinition = FoxGlobals.getInstance().getFoxEnvironment().getEnvironmentDefinition();
      //Environment properties
      StatusTable lEnvConfigTable = pDestination.addTable("Environment Config", "Property Path", "Property Value", "Property Location");
      lEnvConfigTable.setRowProvider(new StatusTable.RowProvider() {
        @Override
        public void generateRows(StatusTable.RowDestination pRowDestination) {
          for (FoxEnvironmentProperty lProp : FoxEnvironmentProperty.values()) {
            StatusItem lPropertyValue;
            try {
              if (lProp.isXML()) {
                DOM lPropertyAsDOM = lEnvironmentDefinition.getPropertyAsDOM(lProp);
                lPropertyValue = lPropertyAsDOM == null ? new StatusText("") : new StatusDetail("View value", lPropertyAsDOM.outputNodeContentsToString(true));
              }
              else {
                lPropertyValue = new StatusText(lEnvironmentDefinition.getPropertyAsString(lProp));
              }
            }
            catch (ExApp e) {
              lPropertyValue = new StatusText("Property not accessible: " + e.getMessage(), MessageLevel.ERROR);
            }

            pRowDestination.addRow(lProp.toString())
            .setColumn(lProp.getPath())
            .setColumn(lPropertyValue)
            .setColumn(lEnvironmentDefinition.getPropertyLocation(lProp.getPath()));
          }
        }
      });

      //App properties
      for (final Map.Entry<String, FoxApplicationDefinition> lAppDefinition : lEnvironmentDefinition.getAppMnemToAppDefinition().entrySet()) {
        StatusTable lAppTable = pDestination.addTable("App " + lAppDefinition.getKey(), "Property Path", "Property Value", "Property Location");
        lAppTable.setRowProvider(new StatusTable.RowProvider() {
          @Override
          public void generateRows(StatusTable.RowDestination pRowDestination) {
            for (AppProperty lProp : AppProperty.values()) {

              StatusItem lPropertyValue;
              try {
                if (lProp.isXML()) {
                  DOM lPropertyAsDOM = lAppDefinition.getValue().getPropertyAsDOM(lProp);
                  lPropertyValue = lPropertyAsDOM == null ? new StatusText("") : new StatusDetail("View value", lPropertyAsDOM.outputNodeContentsToString(true));
                }
                else {
                  lPropertyValue = new StatusText(lAppDefinition.getValue().getPropertyAsString(lProp));
                }
              }
              catch (ExApp e) {
                lPropertyValue = new StatusText("Property not accessible: " + e.getMessage(), MessageLevel.ERROR);
              }

              pRowDestination.addRow(lProp.toString())
              .setColumn(lProp.getPath())
              .setColumn(lPropertyValue)
              .setColumn(lAppDefinition.getValue().getPropertyLocation(lProp.getPath()));
            }
          }
        });
      }
    }
    else {
      pDestination.addDetailMessage("Environment configuration error", () -> new StatusText(XFUtil.getJavaStackTraceInfo(EngineInitialisationController.getLastInitError()), true, MessageLevel.ERROR));
      pDestination.addAction("Reinitialise engine", FoxBootServlet.BOOT_SERVLET_PATH + "/!INIT");
    }
  }

  @Override
  public String getCategoryTitle() {
    return "Environment Config";
  }

  @Override
  public String getCategoryMnemonic() {
    return "foxEnvironmentConfig";
  }

  @Override
  public boolean isCategoryExpandedByDefault() {
    return false;
  }
}
