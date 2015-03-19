package net.foxopen.fox;

import net.foxopen.fox.enginestatus.EngineStatus;
import net.foxopen.fox.enginestatus.MessageLevel;
import net.foxopen.fox.enginestatus.StatusCollection;
import net.foxopen.fox.enginestatus.StatusDestination;
import net.foxopen.fox.enginestatus.StatusDetail;
import net.foxopen.fox.enginestatus.StatusItem;
import net.foxopen.fox.enginestatus.StatusMessage;
import net.foxopen.fox.enginestatus.StatusProvider;
import net.foxopen.fox.enginestatus.StatusTable;
import net.foxopen.fox.enginestatus.StatusText;
import net.foxopen.fox.entrypoint.FoxGlobals;
import net.foxopen.fox.filetransfer.VirusScannerDefinition;

import java.util.TreeSet;

class AppStatusProvider
implements StatusProvider {
  @Override
  public void refreshStatus(StatusDestination pDestination) {

    if(!FoxGlobals.getInstance().isEngineInitialised()) {
      //Shortcut out if engine is not initialised
      pDestination.addMessage("Engine not configured", "Unconfigured engine - no apps available", MessageLevel.ERROR);
      return;
    }

    final int lAdditionalMimeTypeCount = FileUploadType.getAdditionalMimeTypeCount();
    pDestination.addMessage("File upload mime config", lAdditionalMimeTypeCount == 0 ? "No additional mime mappings found" : "Loaded " + lAdditionalMimeTypeCount + " mime types from database");

    for(final App lApp : FoxGlobals.getInstance().getFoxEnvironment().getAllApps()) {
      StatusTable lTable = pDestination.addTable(lApp.getAppMnem(), "Property name", "Detail");
      lTable.setRowProvider(new StatusTable.RowProvider() {
        @Override
        public void generateRows(StatusTable.RowDestination pRowDestination) {

          pRowDestination.addRow().setColumn("Aliases").setColumn(StatusCollection.fromStringSet("aliases", lApp.getAppAliasList()));
          pRowDestination.addRow().setColumn("App construction time").setColumn(EngineStatus.formatDate(lApp.getObjectCreatedDate()));

          pRowDestination.addRow().setColumn("Components").setColumn(new StatusDetail("View all", new StatusDetail.Provider(){
            @Override
            public StatusItem getDetailMessage() {
              return StatusCollection.fromStringSet("components", new TreeSet<>(lApp.getComponentNameSet()));
            }
          }));

          pRowDestination.addRow().setColumn("Connection pool name").setColumn(lApp.getConnectionPoolName());
          pRowDestination.addRow().setColumn("Entry theme security").setColumn(lApp.isEntryThemeSecurityOn() ? new StatusText("ON") : new StatusText("OFF", MessageLevel.WARNING));
          pRowDestination.addRow().setColumn("Exit page").setColumn(lApp.getExitPage());
          pRowDestination.addRow().setColumn("Display attributes").setDetailColumn("View attributes", new StatusDetail.Provider() {
            @Override
            public StatusItem getDetailMessage() {
              return StatusCollection.fromStringMap("displayAttrs", lApp.getAppDisplayAttributeList());
            }
          });

          pRowDestination.addRow().setColumn("Default HTML doctype").setColumn(lApp.getDefaultDocumentType() != null? lApp.getDefaultDocumentType().toString() : "");
          pRowDestination.addRow().setColumn("Default file upload type").setColumn(lApp.getDefaultFileUploadType());
          pRowDestination.addRow().setColumn("Default module").setColumn(lApp.getDefaultModuleName());

          pRowDestination.addRow().setColumn("HTML response method").setColumn(lApp.getResponseMethod().toString());
          pRowDestination.addRow().setColumn("Logout page").setColumn(lApp.getLogoutPage());
          pRowDestination.addRow().setColumn("Resource table list").setColumn(StatusCollection.fromStringSet("resourceTables", lApp.getResourceTableList()));

          pRowDestination.addRow().setColumn("Security check module").setColumn(lApp.getSecurityCheckModuleName());
          pRowDestination.addRow().setColumn("Secure cookies").setColumn(Boolean.toString(lApp.isSecureCookies()));
          pRowDestination.addRow().setColumn("Timeout module").setColumn(lApp.getTimeoutModuleName());

          StatusItem lVSItem;
          if(lApp.getVirusScannerDefinitions().size() == 0 ) {
            lVSItem = new StatusText("IGNORED", MessageLevel.WARNING);
          }
          else {
            StatusCollection lVSCollection = new StatusCollection("virusScanners");
            for(VirusScannerDefinition lScannerDefinition : lApp.getVirusScannerDefinitions()) {
              StatusCollection lVSDefinition = new StatusCollection(lScannerDefinition.getType());
              lVSDefinition.addItem(new StatusMessage("Type", lScannerDefinition.getType()));
              lVSDefinition.addItem(new StatusMessage("Host", lScannerDefinition.getHost()));
              lVSDefinition.addItem(new StatusMessage("Port", Integer.toString(lScannerDefinition.getPort())));
              lVSDefinition.addItem(new StatusMessage("Timeout seconds", Integer.toString(lScannerDefinition.getTimeoutSeconds())));

              lVSCollection.addItem(lVSDefinition);
            }
            lVSItem = lVSCollection;
          }

          pRowDestination.addRow().setColumn("Virus scanner").setColumn(lVSItem);

        }
      });
    }
  }

  @Override
  public String getCategoryTitle() {
    return "App Status";
  }

  @Override
  public String getCategoryMnemonic() {
    return "appStatus";
  }

  @Override
  public boolean isCategoryExpandedByDefault() {
    return false;
  }
}
