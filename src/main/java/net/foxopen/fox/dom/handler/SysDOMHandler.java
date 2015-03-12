package net.foxopen.fox.dom.handler;

import net.foxopen.fox.ContextLabel;
import net.foxopen.fox.XFUtil;
import net.foxopen.fox.database.UCon;
import net.foxopen.fox.database.UConStatementResult;
import net.foxopen.fox.database.parser.ParsedStatement;
import net.foxopen.fox.database.parser.StatementParser;
import net.foxopen.fox.dom.DOM;
import net.foxopen.fox.entrypoint.FoxGlobals;
import net.foxopen.fox.ex.ExDB;
import net.foxopen.fox.ex.ExInternal;
import net.foxopen.fox.ex.ExTooMany;
import net.foxopen.fox.module.Mod;
import net.foxopen.fox.thread.ActionRequestContext;
import net.foxopen.fox.thread.RequestContext;
import net.foxopen.fox.thread.StatefulXThread;
import net.foxopen.fox.thread.stack.ModuleCallStack;
import net.foxopen.fox.thread.stack.ModuleStateChangeListener;

import java.net.InetAddress;
import java.net.UnknownHostException;


public class SysDOMHandler
implements DOMHandler, ModuleStateChangeListener {

  private static final ParsedStatement SYSDATE_PARSED_STATEMENT = StatementParser.parseSafely(
    "SELECT " +
    "  TO_CHAR(sysdate,'YYYY-MM-DD') \"sysdate\"" +
    ", TO_CHAR(sysdate,'YYYY-MM-DD\"T\"HH24:MI:SS') \"sysdatetime\"" +
    "FROM dual"
  , "SYS DOM sysdate select"
  );

  private final StatefulXThread mThread;

  private final DOM mSysDOM;

  public static SysDOMHandler createSysDOMHandler(StatefulXThread pThread) {
    return new SysDOMHandler(pThread);
  }

  private SysDOMHandler(StatefulXThread pThread) {
    mThread = pThread;
    mThread.getModuleCallStack().registerStateChangeListener(this);

    mSysDOM = DOM.createDocument(ContextLabel.SYS.asString());
    mSysDOM.getDocControl().setDocumentReadWriteAutoIds();

    mSysDOM.getCreate1ENoCardinalityEx("thread/ref").setText(pThread.getThreadRef());
    mSysDOM.getCreate1ENoCardinalityEx("thread/app_mnem").setText(pThread.getThreadAppMnem());
    mSysDOM.getCreate1ENoCardinalityEx("thread/thread_id").setText(pThread.getThreadId());
    mSysDOM.getCreate1ENoCardinalityEx("thread/session_id").setText(pThread.getUserThreadSessionId());

    mSysDOM.getCreate1ENoCardinalityEx("engine/release").setText(FoxGlobals.getInstance().getEngineVersionInfo().getVersionNumber());
    mSysDOM.getCreate1ENoCardinalityEx("engine/build-tag").setText(FoxGlobals.getInstance().getEngineVersionInfo().getBuildTag());
    mSysDOM.getCreate1ENoCardinalityEx("engine/build-time").setText(FoxGlobals.getInstance().getEngineVersionInfo().getBuildTime());
    mSysDOM.getCreate1ENoCardinalityEx("engine/fox_services").setText(FoxGlobals.getInstance().getFoxBootConfig().getFoxServiceList());

    DOM lFoxServiceList = mSysDOM.getCreate1ENoCardinalityEx("engine/fox_service_list");
    String[] lFoxServices = FoxGlobals.getInstance().getFoxBootConfig().getFoxServiceList().split(",");
    for(int i=0; i < lFoxServices.length; i++) {
      if(XFUtil.exists(lFoxServices[i].trim())){
        lFoxServiceList.addElem("fox_service", lFoxServices[i].trim());
      }
    }

    String lStatus = FoxGlobals.getInstance().getFoxBootConfig().isProduction() ? "PRODUCTION" : "DEVELOPMENT";
    mSysDOM.getCreate1ENoCardinalityEx("engine/status").setText(lStatus);

    //TODO PN XTHREAD - logout URLs (code copied from previous XThread)
//    mSysDOM.getCreate1ENoCardinalityEx("portal_urls/logout_url").setText(mThreadDOM.get1SNoEx("logout_url"));
//    mSysDOM.getCreate1ENoCardinalityEx("portal_urls/return_url").setText(mThreadDOM.get1SNoEx("return_url"));
//
//    // Only append url when http request is available (avoid breaking remote action call)
//    try {
//      mSysDOM.getCreate1ENoCardinalityEx("portal_urls/engine_url").setText(
//        (mFoxRequest instanceof FoxRequestHttp) ? mFoxRequest.getRequestURLPathAbsolute(getTopApp()) : "NOT-AVAILABLE"
//      );
//    }
//    catch (ExServiceUnavailable ex) {
//      throw new ExInternal("Failed to get App", ex);
//    }

    try {
      mSysDOM.getCreate1ENoCardinalityEx("host/hostname").setText(InetAddress.getLocalHost().getHostName());
      mSysDOM.getCreate1ENoCardinalityEx("host/address").setText(InetAddress.getLocalHost().getHostAddress());
    }
    catch (UnknownHostException ex) {
      mSysDOM.getCreate1ENoCardinalityEx("host/hostname").setText("unknown");
      mSysDOM.getCreate1ENoCardinalityEx("host/address").setText("unknown");
    }

    refreshStateInfo(pThread.getModuleCallStack());
    refreshModuleInfo(pThread.getModuleCallStack());
  }

  @Override
  public void open(ActionRequestContext pRequestContext) {

    //Refresh sysdate and sysdatetime components
    try{
      UCon lUCon = pRequestContext.getContextUCon().getUCon("Sys DOM");
      try {
        UConStatementResult lSysdateQueryResult = lUCon.querySingleRow(SYSDATE_PARSED_STATEMENT);
        for(String lColName : lSysdateQueryResult.getColumnNames()) {
          mSysDOM.getCreate1ENoCardinalityEx("database/" + lColName).setText(lSysdateQueryResult.getString(lColName));
        }

        mSysDOM.getCreate1ENoCardinalityEx("database/name").setText(lUCon.getDatabaseName());
      }
      finally {
        pRequestContext.getContextUCon().returnUCon(lUCon, "Sys DOM");
      }
    }
    catch (ExDB e) {
      throw new ExInternal("Failed to retrieve sysdate for sys DOM", e);
    }

  }

  @Override
  public DOM getDOM() {
    return mSysDOM;
  }


  private void refreshStateInfo(ModuleCallStack pModuleCallStack) {

    if(pModuleCallStack.getStackSize() > 0 && pModuleCallStack.getTopModuleCall().getTopState() != null){
      try {
        mSysDOM
         .getCreate1E("state")
           .getCreate1E("name").setText(pModuleCallStack.getTopModuleCall().getTopState().getName()).getParentOrNull()
           .getCreate1E("title").setText(pModuleCallStack.getTopModuleCall().getTopState().getTitle());
      }
      catch (ExTooMany e) {}
    }
  }

  private void refreshModuleInfo(ModuleCallStack pModuleCallStack) {

    if(pModuleCallStack.getStackSize() > 0){

      Mod lPreviousModule = pModuleCallStack.getPreviousModuleOrNull();
      String lPreviousModuleName = "";
      String lPreviousModuleTitle = "Start";
      if(lPreviousModule != null){
        lPreviousModuleName = lPreviousModule.getName();
        lPreviousModuleTitle = lPreviousModule.getTitle();
      }

      try {
        mSysDOM
         .getCreate1E("module")
           .getCreate1E("name").setText(pModuleCallStack.getTopModuleCall().getModule().getName()).getParentOrNull()
           .getCreate1E("title").setText(pModuleCallStack.getTopModuleCall().getModule().getTitle()).getParentOrNull()
           .getCreate1E("application-title").setText(pModuleCallStack.getTopModuleCall().getModule().getHeaderControlAttribute("fm:application-title"));
        mSysDOM
          .getCreate1E("thread")
            .getCreate1E("call_id").setText(pModuleCallStack.getTopModuleCall().getCallId());
        mSysDOM
         .getCreate1E("previous_module")
           .getCreate1E("name").setText(lPreviousModuleName).getParentOrNull()
           .getCreate1E("title").setText(lPreviousModuleTitle);
        mSysDOM
         .getCreate1E("theme")
           .getCreate1E("name").setText(pModuleCallStack.getTopModuleCall().getEntryTheme().getName());
      }
      catch (ExTooMany e) {}
    }

  }

  @Override
  public void close(ActionRequestContext pRequestContext) {
  }

  @Override
  public boolean isTransient(){
    return false;
  }

  public String getContextLabel() {
    return ContextLabel.SYS.asString();
  }

  @Override
  public void handleStateChange(RequestContext pRequestContext, EventType pEventType, ModuleCallStack pCallStack) {

    if(pEventType == net.foxopen.fox.thread.stack.ModuleStateChangeListener.EventType.MODULE) {
      refreshModuleInfo(pCallStack);
      refreshStateInfo(pCallStack);
    }
    else if(pEventType == net.foxopen.fox.thread.stack.ModuleStateChangeListener.EventType.STATE) {
      refreshStateInfo(pCallStack);
    }

  }

  public void addInfo(String pPath, String pContent) {
    try {
      mSysDOM.getCreate1E(pPath).setText(pContent);
    }
    catch (ExTooMany e) {
      throw new ExInternal("Too many elements encountered when attempting to add Sys DOM info, check path (" + pPath + ")", e);
    }
  }

  @Override
  public int getLoadPrecedence() {
    return LOAD_PRECEDENCE_MEDIUM;
  }
}
