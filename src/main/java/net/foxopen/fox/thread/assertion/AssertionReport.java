package net.foxopen.fox.thread.assertion;

import com.google.common.collect.Table;
import com.google.common.collect.TreeBasedTable;
import net.foxopen.fox.XFUtil;
import net.foxopen.fox.dom.DOM;
import net.foxopen.fox.ex.ExInternal;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Result tracking and output generation for an assertion run performed by an {@link AssertionRunner}. Only the associated
 * AssertionRunner should manipulate the state of this object.
 */
class AssertionReport {

  /** Map of module names to exceptions encountered when trying to load them. Null key indicates successful load. */
  private final Map<String, Throwable> mModuleLoadResults = new HashMap<>();

  /** Table of module name and action name to all assertion results for that action. */
  private final Table<String, String, List<AssertionResult>> mAllAssertionResults = TreeBasedTable.create();

  /** Table of module name and action name to the time in MS for that action. While the action is running, the value represents the start time. */
  private final Table<String, String, Long> mActionTimings = TreeBasedTable.create();

  /** Table of module name and action name (may be empty string) to any exception encountered when running that module/action. */
  private final Table<String, String, Throwable> mAllErrors = TreeBasedTable.create();

  /** Overall assertion timer. When running this is the start time in MS, at the end it is the total elapsed time. */
  private long mOverallTimeMS = -1;

  /**
   * Records a successfully loaded module.
   * @param pModuleName Name of the loaded module.
   */
  void recordModuleLoad(String pModuleName) {
    mModuleLoadResults.put(pModuleName, null);
  }

  /**
   * Records an error encountered when attempting to load a module.
   * @param pModuleName Name of the module being loaded.
   * @param pLoadError Error encountered during load.
   */
  void recordModuleLoadError(String pModuleName, Throwable pLoadError) {
    mModuleLoadResults.put(pModuleName, pLoadError);
  }

  /**
   * Records all the AssertionResults generated from running an action.
   * @param pModuleName Name of module action is being run in.
   * @param pActionName Name of action containing the assertion command(s).
   * @param pAssertionResults Results of running all assertion commands in the given action.
   */
  void recordActionResults(String pModuleName, String pActionName, List<AssertionResult> pAssertionResults) {
    mAllAssertionResults.put(pModuleName, pActionName, pAssertionResults);
  }

  /**
   * Starts the overall timer for this report run. Only invoke this once.
   */
  void startOverallTimer() {
    mOverallTimeMS = System.currentTimeMillis();
  }

  /**
   * Stops the overall timer for this report run. Only invoke this once.
   */
  void stopOverallTimer() {
    mOverallTimeMS = System.currentTimeMillis() - mOverallTimeMS;
  }

  /**
   * Starts the timer for the given action.
   * @param pModuleName Module containing the action.
   * @param pActionName Action being timed.
   */
  void startActionTimer(String pModuleName, String pActionName) {
    mActionTimings.put(pModuleName, pActionName, System.currentTimeMillis());
  }

  /**
   * Stops the timer for the given action. This depends on startTimer() having already been invoked for the given action.
   * @param pModuleName Module containing the action.
   * @param pActionName Action being timed.
   */
  void stopActionTimer(String pModuleName, String pActionName) {
    mActionTimings.put(pModuleName, pActionName, System.currentTimeMillis() - mActionTimings.get(pModuleName, pActionName));
  }

  /**
   * Records an error encountered when attempting to run assertions for the given module, before any action was attempted
   * to be run, e.g. entry theme errors.
   * @param pModuleName Name of module where the error occurred.
   * @param pError Caught error.
   */
  void recordError(String pModuleName, Throwable pError) {
    mAllErrors.put(pModuleName, "", pError);
  }

  /**
   * Records an error encountered when attempting to run an assertion action.
   * @param pModuleName Name of module action is in.
   * @param pActionName Name of action where error occurred.
   * @param pError Caught error.
   */
  void recordError(String pModuleName, String pActionName, Throwable pError) {
    if(XFUtil.isNull(pActionName)) {
      throw new ExInternal("Action name must be specified");
    }

    mAllErrors.put(pModuleName, pActionName, pError);
  }

  /**
   * Outputs the results gathered by this report as an XML document.
   * @return XML containing report information.
   */
  public DOM asXML() {

    int lPassedCount = 0;
    int lFailedCount = 0;
    int lErrorCount = 0;

    long lActionTimeMS = 0;

    DOM lResultList = DOM.createUnconnectedElement("ASSERTION_RESULT_LIST");

    //Report errors at the top
    for (Table.Cell<String, String, Throwable> lCell : mAllErrors.cellSet()) {

      DOM lOutputRow = lResultList.addElem("ASSERTION_RESULT");

      lOutputRow.addElem("MODULE").setText(lCell.getRowKey());
      if(!XFUtil.isNull(lCell.getColumnKey())) {
        lOutputRow.addElem("ACTION").setText(lCell.getColumnKey());
        lOutputRow.addElem("ERROR_TYPE").setText("ACTION");
      }
      else {
        lOutputRow.addElem("ERROR_TYPE").setText("MODULE");
      }

      lOutputRow.addElem("STACK_TRACE").setText(XFUtil.getJavaStackTraceInfo(lCell.getValue()));
      lOutputRow.addElem("OUTCOME").setText("ERROR");

      lErrorCount++;
    }

    //Print every assertion result as a separate "row"
    for (Table.Cell<String, String, List<AssertionResult>> lCell : mAllAssertionResults.cellSet()) {
      for (AssertionResult lAssertionResult : lCell.getValue()) {

        DOM lOutputRow = lResultList.addElem("ASSERTION_RESULT");

        lOutputRow.addElem("MODULE").setText(lCell.getRowKey());
        lOutputRow.addElem("ACTION").setText(lCell.getColumnKey());

        lOutputRow.addElem("ASSERTION_TYPE").setText(lAssertionResult.getAssertionType());
        lOutputRow.addElem("MESSAGE").setText(lAssertionResult.getFullMessage());
        lOutputRow.addElem("XPATH").setText(lAssertionResult.getTestXPath());
        lOutputRow.addElem("OUTCOME").setText(lAssertionResult.assertionPassed() ? "PASSED" : "FAILED");

        if(lAssertionResult.assertionPassed()) {
          lPassedCount++;
        }
        else {
          lFailedCount++;
        }
      }
    }

    //Print action timings
    DOM lTimingList = DOM.createUnconnectedElement("ACTION_TIMING_LIST");
    for (Table.Cell<String, String, Long> lCell : mActionTimings.cellSet()) {
      DOM lOutputRow = lTimingList.addElem("ACTION_TIMING");

      lOutputRow.addElem("MODULE").setText(lCell.getRowKey());
      lOutputRow.addElem("ACTION").setText(lCell.getColumnKey());

      lOutputRow.addElem("TIME_MS").setText(String.valueOf(lCell.getValue()));

      lActionTimeMS += lCell.getValue();
    }

    int lModuleLoadCount = 0;
    int lModuleFailCount = 0;

    //Print module load summary about any modules which failed to load
    DOM lModLoadSummary = DOM.createUnconnectedElement("MODULE_LOAD_SUMMARY");
    for (Map.Entry<String, Throwable> lModLoadEntry : mModuleLoadResults.entrySet()) {
      DOM lModElem = lModLoadSummary.addElem("MODULE");
      lModElem.addElem("NAME", lModLoadEntry.getKey());

      if(lModLoadEntry.getValue() != null) {
        lModElem.addElem("STATUS", "NOT_LOADED");
        lModElem.addElem("LOAD_EXCEPTION", XFUtil.getJavaStackTraceInfo(lModLoadEntry.getValue()));
        lModuleFailCount++;
      }
      else {
        lModElem.addElem("STATUS", "LOADED");
        lModuleLoadCount++;
      }
    }

    //Add summary information
    DOM lResultDocument = DOM.createDocument("ASSERTION_REPORT");
    DOM lSummaryElem = lResultDocument.addElem("SUMMARY");

    lSummaryElem.addElem("OVERALL_OUTCOME", lErrorCount > 0 ? "ERROR" : (lFailedCount == 0 ? "PASSED" : "FAILED"));
    lSummaryElem.addElem("TOTAL_ASSERTIONS_RUN", String.valueOf(lPassedCount + lFailedCount));
    lSummaryElem.addElem("TOTAL_ASSERTIONS_PASSED", String.valueOf(lPassedCount));
    lSummaryElem.addElem("TOTAL_ASSERTIONS_FAILED", String.valueOf(lFailedCount));
    lSummaryElem.addElem("TOTAL_ASSERTION_ERRORS", String.valueOf(lErrorCount));

    lSummaryElem.addElem("MODULES_LOADED", String.valueOf(lModuleLoadCount));
    lSummaryElem.addElem("MODULE_LOAD_ERRORS", String.valueOf(lModuleFailCount));

    lSummaryElem.addElem("ACTION_TIME_MS", String.valueOf(lActionTimeMS));
    lSummaryElem.addElem("OVERALL_TIME_MS", String.valueOf(mOverallTimeMS));

    lResultList.copyToParent(lResultDocument);
    lTimingList.copyToParent(lResultDocument);
    lModLoadSummary.copyToParent(lResultDocument);

    return lResultDocument;
  }

}
