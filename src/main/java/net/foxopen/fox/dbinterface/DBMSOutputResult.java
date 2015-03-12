package net.foxopen.fox.dbinterface;

import net.foxopen.fox.command.XDoResult;

/**
 * Representation of DBMS_OUTPUT retrieved after running an InterfaceStatement. Only created when DBMS_OUTPUT is enabled
 * in the developer toolbar.
 */
public class DBMSOutputResult
implements XDoResult {

  private final String mStatementName;
  private final String mMatchRef;
  private final String mOutputString;

  public DBMSOutputResult(String pStatementName, String pMatchRef, String pOutputString) {
    mStatementName = pStatementName;
    mMatchRef = pMatchRef;
    mOutputString = pOutputString;
  }

  public String getStatementName() {
    return mStatementName;
  }

  public String getMatchRef() {
    return mMatchRef;
  }

  public String getOutputString() {
    return mOutputString;
  }
}
