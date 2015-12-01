package net.foxopen.fox.thread.assertion;

import net.foxopen.fox.App;
import net.foxopen.fox.FoxComponent;
import net.foxopen.fox.XFUtil;
import net.foxopen.fox.auth.StandardAuthenticationContext;
import net.foxopen.fox.entrypoint.FoxGlobals;
import net.foxopen.fox.ex.ExApp;
import net.foxopen.fox.ex.ExFoxConfiguration;
import net.foxopen.fox.ex.ExInternal;
import net.foxopen.fox.ex.ExServiceUnavailable;
import net.foxopen.fox.ex.ExUserRequest;
import net.foxopen.fox.module.ActionDefinition;
import net.foxopen.fox.module.Mod;
import net.foxopen.fox.module.entrytheme.EntryTheme;
import net.foxopen.fox.thread.RequestContext;
import net.foxopen.fox.thread.StatefulXThread;
import net.foxopen.fox.thread.ThreadProperty;
import net.foxopen.fox.thread.XThreadBuilder;
import net.foxopen.fox.thread.stack.ModuleCall;
import net.foxopen.fox.track.Track;
import org.apache.commons.io.FilenameUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Runs all assertion actions in modules which match a set of filter criteria, and produces an AssertionReport containing
 * the results.
 */
public class AssertionRunner {

  /** App to retrieve assertion modules from. */
  private final App mApp;

  /** Glob like filter pattern to match module names against. */
  private final String mModuleNameFilter;

  /** Name of test category to run assertions from. */
  private final String mCategoryFilter;

  /** Report for tracking assertion results. */
  private final AssertionReport mReport = new AssertionReport();

  /**
   * Creates a new AssertionRunner with the given module filter criteria.
   * @param pApp App to retrieve assertion modules from.
   * @param pModuleNameFilter Glob-like module name filter (supports * and ? characters).
   * @param pCategoryFilter Name of the test category to filter on - null/empty means all categories.
   */
  AssertionRunner(App pApp, String pModuleNameFilter, String pCategoryFilter) {
    mApp = pApp;
    mModuleNameFilter = pModuleNameFilter;
    mCategoryFilter = pCategoryFilter;
  }

  /**
   * Gets all the assertion modules matching this runner's filter criteria. The modules will be populated into the
   * application's module cache. Only modules marked up as being assertion modules are returned.
   * @return Set of assertion modules.
   */
  private Collection<Mod> getAssertionModules() {

    Track.pushInfo("GetAssertionModules");
    try {
      //Flush all modules - the load procedure below will re-populate the cache. It is important that the Module instances
      //returned by this method are also retrieved by the assertion runner's XThreads - flushing the cache is the easiest way
      //to ensure this happens.
      try {
        FoxGlobals.getInstance().getFoxEnvironment().flushApplicationCache();
      }
      catch (ExApp | ExFoxConfiguration | ExServiceUnavailable e) {
        throw new ExInternal("Failed to flush application cache", e);
      }

      List<Mod> lModules = new ArrayList<>();
      for (String lModuleName : mApp.getComponentNameSet()) {
        Track.pushInfo("LoadComponent", lModuleName);
        try {
          //Filter by name glob match
          if (FilenameUtils.wildcardMatch(lModuleName, mModuleNameFilter)) {
            FoxComponent lComponent = mApp.getComponent(lModuleName, true);

            //Only examine modules, leave other component types alone
            if(lComponent instanceof Mod) {
              Mod lMod = (Mod) lComponent;

              Track.info("ModuleWildcardMatch", lModuleName);

              if(lMod.isAssertionModule()) {
                Track.info("IsAssertionModule", lModuleName);

                //Filter by category if specified
                if ((XFUtil.isNull(mCategoryFilter) || lMod.getAssertionConfig().getTestCategories().contains(mCategoryFilter))) {
                  Track.info("ModuleCategoryMatch", lModuleName);
                  lModules.add(lMod);
                  //Record successful load
                  mReport.recordModuleLoad(lModuleName);
                }
              }
            }
          }
        }
        catch (Throwable th) {
          //Record failed load
          mReport.recordModuleLoadError(lModuleName, th);
        }
        finally {
          Track.pop("LoadComponent");
        }
      }

      return lModules;
    }
    finally {
      Track.pop("GetAssertionModules");
    }
  }

  /**
   * Runs all the assertions statements found using this AssertionRunner's filter criteria, and returns the results.
   * @param pRequestContext Current RequestContext.
   * @return Report containing all relevant details for this assertion run.
   */
  public AssertionReport runAssertions(RequestContext pRequestContext) {

    mReport.startOverallTimer();
    Track.pushInfo("RunAssertions");
    try {
      //Run all assertion actions in every matching assertion module
      for (Mod lMod : getAssertionModules()) {
        Track.pushInfo("AssertionModule", lMod.getName());
        try {
          runModuleAssertionActions(pRequestContext, lMod);
        }
        catch (Throwable th) {
          //Catch module level errors
          mReport.recordError(lMod.getName(), th);
        }
        finally {
          Track.pop("AssertionModule");
        }
      }
      mReport.stopOverallTimer();

      return mReport;
    }
    finally {
      Track.pop("RunAssertions");
    }
  }

  /**
   * Runs all the assertion actions in the given module, recording results on the current AssertionReport. This method
   * may throw exceptions if the XThread cannot be successfully started. Otherwise, exceptions caught running the assertion
   * actions will be recorded and suppressed.
   * @param pRequestContext Current RequestContext.
   * @param pMod Module to run actions for.
   */
  private void runModuleAssertionActions(RequestContext pRequestContext, Mod pMod) {

    StatefulXThread lXThread = createXThread(pRequestContext, pMod);

    ModuleAssertionConfig lAssertionConfig = pMod.getAssertionConfig();
    //Establish default attach point for action running
    String lAttach = lXThread.getModuleCallStack().getTopModuleCall().getContextUElem().attachDOM().getFoxId();

    for (ActionDefinition lAction : pMod.getActionDefinitionMap().values()) {
      //Only run assertion actions
      if (lAction.isAssertion()) {
        Track.pushInfo("AssertionAction", lAction.getActionName());
        try {
          //Run before action if configured
          if (!XFUtil.isNull(lAssertionConfig.getBeforeActionName())) {
            lXThread.processAction(pRequestContext, lAssertionConfig.getBeforeActionName(), lAttach, false, rc -> null);
          }

          List<AssertionResult> lAssertionResults;
          mReport.startActionTimer(pMod.getName(), lAction.getActionName());
          try {
            //Run the actual assertion action
            lAssertionResults = lXThread.processAction(pRequestContext, lAction.getActionName(), lAttach, false, rc -> rc.getXDoResults(AssertionResult.class));
          }
          finally {
            mReport.stopActionTimer(pMod.getName(), lAction.getActionName());

            //Run after action if configured
            if (!XFUtil.isNull(lAssertionConfig.getAfterActionName())) {
              lXThread.processAction(pRequestContext, lAssertionConfig.getAfterActionName(), lAttach, false, rc -> null);
            }
          }

          //Only record results after entire action sequence has run
          mReport.recordActionResults(pMod.getName(), lAction.getActionName(), lAssertionResults);
        }
        catch (Throwable th) {
          mReport.recordError(pMod.getName(), lAction.getActionName(), th);
        }
        finally {
          Track.pop("AssertionAction");
        }
      }
    }
  }

  /**
   * Creates a new XThread, ready for running assertion actions.
   * @param pRequestContext Current RequestContext.
   * @param pMod Module for the XThread to enter in order to run assertion actions.
   * @return New XThread for running assertions in the given module.
   */
  private StatefulXThread createXThread(RequestContext pRequestContext, Mod pMod) {

    Track.pushInfo("CreateAssertionThread");
    try {
      XThreadBuilder lXThreadBuilder = new XThreadBuilder(mApp.getAppMnem(), new StandardAuthenticationContext(pRequestContext));
      //TODO - when implemented, disable database persistence for this thread
      //lXThreadBuilder.setDatabasePersistence(false);
      //Tell the XThread it's running assertions so assertion failures don't hard error
      lXThreadBuilder.setBooleanThreadProperty(ThreadProperty.Type.IS_ASSERTION_MODE, true);

      StatefulXThread lXThread = lXThreadBuilder.createXThread(pRequestContext);

      ModuleCall.Builder lModCallBuilder;
      try {
        //Establish entry theme from assertion config, using default if not specified
        EntryTheme lTheme;
        if (!XFUtil.isNull(pMod.getAssertionConfig().getEntryThemeName())) {
          lTheme = pMod.getEntryTheme(pMod.getAssertionConfig().getEntryThemeName());
        }
        else {
          lTheme = pMod.getDefaultEntryTheme();
        }

        lModCallBuilder = new ModuleCall.Builder(lTheme);
      }
      catch (ExUserRequest e) {
        throw new ExInternal("Failed to get entry theme", e);
      }

      lXThread.startThread(pRequestContext, lModCallBuilder, false);
      return lXThread;
    }
    finally {
      Track.pop("CreateAssertionThread");
    }
  }
}
