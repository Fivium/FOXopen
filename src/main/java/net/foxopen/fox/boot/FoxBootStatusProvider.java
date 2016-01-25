package net.foxopen.fox.boot;

import net.foxopen.fox.ContextUCon;
import net.foxopen.fox.configuration.FoxBootConfig;
import net.foxopen.fox.database.UCon;
import net.foxopen.fox.database.UConStatementResult;
import net.foxopen.fox.database.parser.StatementParser;
import net.foxopen.fox.enginestatus.EngineStatus;
import net.foxopen.fox.enginestatus.MessageLevel;
import net.foxopen.fox.enginestatus.StatusDestination;
import net.foxopen.fox.enginestatus.StatusProvider;
import net.foxopen.fox.entrypoint.FoxGlobals;
import net.foxopen.fox.ex.ExDB;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

public class FoxBootStatusProvider
implements StatusProvider{

  @Override
  public void refreshStatus(StatusDestination pDestination) {

    FoxGlobals lGlobals = FoxGlobals.getInstance();

    try {
      pDestination.addMessage("Engine Locator", lGlobals.getEngineLocator());
      pDestination.addMessage("Engine Port", lGlobals.getEnginePort());

      pDestination.addMessage("Server IP", lGlobals.getServerIP());
      pDestination.addMessage("Server Hostname", lGlobals.getServerHostName());

      pDestination.addMessage("Build Version Number", lGlobals.getEngineVersionInfo().getVersionNumber());
      pDestination.addMessage("Build Tag", lGlobals.getEngineVersionInfo().getBuildTag());
      pDestination.addMessage("Build Time", lGlobals.getEngineVersionInfo().getBuildTime());

      pDestination.addMessage("Context Name", lGlobals.getContextName());

      pDestination.addMessage("Last Initialised", EngineStatus.formatDate(new Date(EngineInitialisationController.getLastInitAttemptTime())));
    }
    catch (Throwable th) {
      pDestination.addMessage("GLOBAL STATUS ERROR", th.getMessage(), MessageLevel.ERROR);
    }

    FoxBootConfig lBootConfig = lGlobals.getFoxBootConfig();
    try {
      pDestination.addMessage("Service List", lBootConfig.getFoxServiceList());

      pDestination.addMessage("Production Status", lBootConfig.getProductionStatus());

      pDestination.addMessage("Environment Name", lBootConfig.getFoxEnvironmentKey());

      pDestination.addMessage("Database URL", lBootConfig.getDatabaseURL());
      pDestination.addMessage("Database User", lBootConfig.getMainDatabaseUsername());
    }
    catch (Throwable th) {
      pDestination.addMessage("BOOT STATUS ERROR", th.getMessage(), MessageLevel.ERROR);
    }

    try {
      getDatabaseInfo(pDestination);
    }
    catch (Throwable th) {
      pDestination.addMessage("DB CHECK ERROR", th.getMessage(), MessageLevel.ERROR);
    }
  }

  @Override
  public String getCategoryTitle() {
    return "Engine Information";
  }

  @Override
  public String getCategoryMnemonic() {
    return "engine";
  }

  @Override
  public boolean isCategoryExpandedByDefault() {
    return true;
  }

  private void getDatabaseInfo(StatusDestination pCategory) {

    ContextUCon lContextUCon = ContextUCon.createContextUCon(FoxGlobals.getInstance().getEngineConnectionPoolName(), "DB status");
    lContextUCon.pushConnection("DBStatus");
    try {
      getDatabaseVersionInfo(lContextUCon, pCategory);
      checkDatabaseTime(lContextUCon, pCategory);
    }
    finally {
      lContextUCon.rollbackAndCloseAll(true);
    }
  }

  private void getDatabaseVersionInfo(ContextUCon pContextUCon, StatusDestination pCategory) {

    UCon lUCon = pContextUCon.getUCon("Get database version");
    try {
      List<UConStatementResult> lRows = lUCon.queryMultipleRows(StatementParser.parseSafely("SELECT banner FROM V$VERSION", "Select DB version"));
      pCategory.addMessage("Database Version Info", lRows.stream().map(e -> e.getString("BANNER")).collect(Collectors.joining("\n")));
    }
    catch (ExDB e) {
      throw e.toUnexpected();
    }
    finally {
      pContextUCon.returnUCon(lUCon, "Get database version");
    }
  }

  private void checkDatabaseTime(ContextUCon pContextUCon, StatusDestination pCategory) {

    UCon lUCon = pContextUCon.getUCon("Check database time");
    try {
      Date lSysdate = (Date) lUCon.queryScalarObject(StatementParser.parseSafely("SELECT sysdate FROM dual", "Select sysdate"));
      //Establish difference in seconds
      long lDiffInSecs = (lSysdate.getTime() - new Date().getTime()) / 1000L;
      pCategory.addMessage("Time (app server vs database)", "App server time and database time differ by " + lDiffInSecs + " seconds", Math.abs(lDiffInSecs) > 15 ? MessageLevel.WARNING : MessageLevel.INFO);

      pCategory.addMessage("Time (database)", EngineStatus.formatDate(lSysdate));
      pCategory.addMessage("Time (app server)", EngineStatus.formatDate(new Date()));
    }
    catch (ExDB e) {
      throw e.toUnexpected();
    }
    finally {
      pContextUCon.returnUCon(lUCon, "Check database time");
    }
  }
}
