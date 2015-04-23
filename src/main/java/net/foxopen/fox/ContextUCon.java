/*

Copyright (c) 2010, UK DEPARTMENT OF ENERGY AND CLIMATE CHANGE -
                    ENERGY DEVELOPMENT UNIT (IT UNIT)
All rights reserved.

Redistribution and use in source and binary forms, with or without modification,
are permitted provided that the following conditions are met:

    * Redistributions of source code must retain the above copyright notice,
      this list of conditions and the following disclaimer.
    * Redistributions in binary form must reproduce the above copyright notice,
      this list of conditions and the following disclaimer in the documentation
      and/or other materials provided with the distribution.
    * Neither the name of the DEPARTMENT OF ENERGY AND CLIMATE CHANGE nor the
      names of its contributors may be used to endorse or promote products
      derived from this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR
ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
(INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
(INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

$Id$

*/
package net.foxopen.fox;


import com.google.common.base.Joiner;
import net.foxopen.fox.command.builtin.TransactionCommand;
import net.foxopen.fox.database.ConnectionAgent;
import net.foxopen.fox.database.UCon;
import net.foxopen.fox.ex.ExDB;
import net.foxopen.fox.ex.ExInternal;
import net.foxopen.fox.ex.ExServiceUnavailable;
import net.foxopen.fox.plugin.api.database.FxpContextUCon;
import net.foxopen.fox.track.Track;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.Map;


/**
 * Manages connection switching and transaction logic for FOX action processing, enabling FOX developers to have control
 * over the transactional intergrity of their modules while minimising excessive connection usage.<br/><br/>
 *
 * A ContextUElem maintains a map of zero or more <i>connections</i>, and a stack to record which connection should be
 * in use at any given point. Consumers should use one of the <tt>push</tt> methods to set which connection to use,
 * execute any dependent actions, then call <tt>pop</tt> (ideally in a finally block) to return the ContextUElem to its
 * previous state. Most action processing should only require a single connection to minimise the risk of transaction boundary
 * problems.<br/><br/>
 *
 * Consuming code should use {@link #getUCon(String)}to retrieve a UCon from a ContextUCon. This may cause a UCon to be
 * retrieved from the connection pool just in time if one is not cached against the top <i>connection</i> in the stack.
 * {@link #returnUCon(UCon,String)}should be called when the UCon is no longer needed. This may close the UCon if there
 * is no transaction active on it (amongst other criteria). The <i>connection</i> maintains a stack of purposes to ensure
 * every call to getUCon is matched by a corresponding call to returnUCon.<br/><br/>
 *
 * Consuming code may wish to retain a UCon after it is returned even if there is no transaction active on it, to minimise
 * the overhead of going to the connection pool each time a UCon is requested. This behaviour can be activated and deactivated
 * by calling {@link #startRetainingUCon}and {@link #stopRetainingUCon}respectively. The UCon will be retained until the
 * connection is popped from the connection stack when this behaviour is active. Note this could cause a connection to
 * be retained for a long time if used improperly.<br/><br/>
 *
 * <i>Connections</i> are always retained if they were created by a call to {@link #pushRetainedConnection(String)}or
 * retention is requested when the UCon is returned. Retained connections should be cleaned up at the end of action processing
 * with a call to {@link #closeAllRetainedConnections}. Note that a retained connection is not necessarily present on the
 * connection stack but will always be in the connection map.<br/><br/>
 *
 * Consumers using a UCon retrieved from a ContextUCon should avoid calling commit and rollback methods directly on the UCon.
 * Instead, the commit and rollback methods on the ContextUCon should be used as these validate that the operation is allowed
 * for the current UCon.<br/><br/>
 *
 * <b>Example Usage</b><br/><br/>
 *
 * Most consumers will only need the basic functionality of this class. To retrive and return a UCon, do the following:<br/><br/>
 *
 * <pre>
 * {@code
 * UCon lUCon = lContextUCon.getUCon("Query Purpose");
 * try {
 * //Use UCon here
 * }
 * finally {
 * //Note purposes must match exactly
 * lContextUCon.returnUCon(lUCon, "Query Purpose");
 * }
 * }
 * </pre>
 * @see TransactionCommand
 */
public class ContextUCon implements FxpContextUCon<UCon> {

  /** The connection stack - names correspond to entries in the connection map. */
  private final Deque<String> mConnectionNameUsageStack = new ArrayDeque<>();

  /** Map of the connections currently known about by this ContextUCon (they may or may not be in the stack). */
  private final Map<String, Connection> mConnectionMap = new LinkedHashMap<>(); //Linked to preserve insertion order

  /** Name of the Connection Pool to use when retrieving a just-in-time UCon. */
  private final String mPoolName;

  /** Description of this ContextUCon. */
  private final String mContextUConPurpose;

  private static final String TRACK_TIMER_EVENT_SUFFIX = " connection hold";

  private class Connection {

    /** Current UCon for this Connection if one is being retained. Can be null. */
    private UCon mOptionalCachedUCon;
    /** True if this connection should be retained after being popped, even if it would otherwise be discarded. */
    private boolean mRetainConnection;
    /** True if the current UCon should be retained after being returned, even if it would otherwise be closed. */
    private boolean mRetainUCon;

    private final String mConnectionName;
    private final Deque<String> mPurposeStack = new ArrayDeque<>();
    private final boolean mAllowTransactionControl;

    private Connection(String pConnectionName, UCon pOptionalUCon, boolean pForceRetain, boolean pAllowTransactionControl) {
      mConnectionName = pConnectionName;
      mOptionalCachedUCon = pOptionalUCon;
      mRetainConnection = pForceRetain;
      //Default - may be switched independently
      mRetainUCon = pForceRetain;
      mAllowTransactionControl = pAllowTransactionControl;
    }

    /**
     * Gets a UCon, either from the cached UCon on this Connection or by requesting a new UCon from the connection pool.
     * @param pPurpose Purpose of the UCon usage - will be added to the purpose stack.
     * @return Cached or new UCon.
     */
    private UCon getUCon(String pPurpose) {
      UCon lUCon;

      Track.pushDebug("GetUCon", "Getting UCon for connection " + getConnectionID() + " for purpose " + pPurpose);
      try {
        //Get the cached UCon or get one JIT from the connection pool
        if(mOptionalCachedUCon != null) {
          lUCon = mOptionalCachedUCon;
          Track.debug("ConnectionExists", "Using existing connection");
        }
        else {
          try {
            Track.debug("NewConnection", "Retrieving new connection");
            lUCon = ConnectionAgent.getConnection(mPoolName, getConnectionID());
            //Record the new UCon against this connection entry
            mOptionalCachedUCon = lUCon;
            Track.timerStart(getConnectionID() + TRACK_TIMER_EVENT_SUFFIX);
          }
          catch (ExServiceUnavailable e) {
            throw new ExInternal("Connection for purpose " + pPurpose + " not available", e);
          }
        }

        //Record purpose against connection name
        mPurposeStack.addFirst(pPurpose);
      }
      finally {
        Track.pop("GetUCon");
      }

      return lUCon;
    }

    private String getConnectionID() {
      return mContextUConPurpose + "/" +  mConnectionName;
    }

    /**
     * Validates that a UCon is allowed to be returned to this connection, then handles the return ({@link #returnUConInternal}).
     * @param pUCon UCon being returned.
     * @param pPurpose Purpose which should match the purpose given when the connection was retrieved.
     * @param pRetainConnection If true, the Connection is set to be retained when popped.
     */
    private void validateAndReturnUCon(UCon pUCon, String pPurpose, boolean pRetainConnection) {

      //Note: exceptions from this method MAY clobber root exceptions as this method is often called from finally blocks.
      //The exceptions below are considered "development time" errors and should be encountered by engine/app developers
      //before the code is deployed to production.

      String lTopPurpose = mPurposeStack.getFirst();
      if(!lTopPurpose.equals(pPurpose)) {
        throw new ExInternal("Connection stack error: purpose mismatch. Expected returned purpose of '" + lTopPurpose + "' but was given '" + pPurpose + "'. " +
                             "Full stack: " + Joiner.on(",").join(mPurposeStack));
      }
      else if(pUCon == null) {
        throw new ExInternal("Connection stack error: returned UCon object was null");
      }
      else if(!pUCon.equals(mOptionalCachedUCon)) {
        throw new ExInternal("Connection stack error: returned UCon object does not match original UCon object.");
      }

      //If consumer requires it, ensure the connection is forcibly retained from here on out
      if(pRetainConnection) {
        mRetainConnection = true;
      }

      //This method suppresses all exceptions
      returnUConInternal(pPurpose, false);

      //Pop the latest status for this connection
      mPurposeStack.removeFirst();
    }

    /**
     * Decides if the current UCon should be closed. The default behaviour is to close it if it has no more purposes on
     * the purpose stack, the UCon or connection is not being retained and there is no transaction active on the UCon.
     * Note: this method suppresses exceptions.
     * @param pPurpose Used for logging messages.
     * @param pForceClose Override the default behaviour and closes the UCon regardless.
     */
    private void returnUConInternal(String pPurpose, boolean pForceClose) {

      try {
        boolean lClose = pForceClose;

        //Note order of if statement - check the transaction last as this is a slow operation.
        if(!lClose) {
          if(mRetainConnection || mRetainUCon) {
            Track.debug("ReturnUCon", "Retaining UCon for connection " + getConnectionID() + " as " +
            (mRetainUCon ? "UCon" : "Connection") + " retention requested after use for " + pPurpose);
          }
          else if ((mPurposeStack.size() > 1)) {
            Track.debug("ReturnUCon", "Retaining UCon for connection " + getConnectionID() + " as the purpose stack is not empty (still in use) - returned purpose was " + pPurpose);
          }
          else if(isTransactionActive()) {
            Track.debug("ReturnUCon", "Retaining UCon for connection " + getConnectionID() + " as transaction active after use for " + pPurpose);
          }
          else {
            lClose = true;
          }
        }

        if (lClose){
          //No need to retain, so close
          Track.debug("ReturnUCon", "Closing UCon for connection " + getConnectionID() + " as no transaction active and retention not required after use for " + pPurpose);
          if(mOptionalCachedUCon != null) {
            closeUCon(true);
          }
        }
      }
      catch (Throwable th) {
        //Don't allow errors to propagate out of this method
        //If a severe error has occurred upstream (e.g. ORA-600) the connection will be closed and this method will fail
        //Suppressed errors should be printed to stdout by the logger
        Track.recordSuppressedException("returnUConInternal", th);
      }
    }

    /**
     * Closes the attached UCon and dereferences it from this Connection.
     * @param pRecycle If true, the UCon will be recycled.
     */
    private void closeUCon(boolean pRecycle) {
      if(pRecycle) {
        mOptionalCachedUCon.closeForRecycle();
      }
      else {
        mOptionalCachedUCon.closeNoRecycle();
      }
      mOptionalCachedUCon = null;

      Track.timerPause(getConnectionID() + TRACK_TIMER_EVENT_SUFFIX);
    }

    private boolean isTransactionActive() {
      if(mOptionalCachedUCon == null) {
        //No UCon so there can't be a transaction
        return false;
      }
      else {
        return mOptionalCachedUCon.isTransactionActive();
      }
    }
  }

  /**
   * Creates an empty ContextUCon. This should be populated with a call to a <tt>push</tt> method before use.
   * @param pPoolName Connection pool to use when constructing a new UCon (see {@link ConnectionAgent}).
   * @param pContextUConPurpose Description of the ContextUCon.
   * @return New ContextUCon.
   */
  public static ContextUCon createContextUCon(String pPoolName, String pContextUConPurpose) {
    ContextUCon lNewContextUCon = new ContextUCon(pPoolName, pContextUConPurpose);
    return lNewContextUCon;
  }

  private ContextUCon(String pPoolName, String pContextUConPurpose) {
    mPoolName = pPoolName;
    mContextUConPurpose = pContextUConPurpose;
  }

  /**
   * Gets the top connection from the stack.
   * @return Top connection.
   */
  private Connection getTopConnection() {
    return mConnectionMap.get(mConnectionNameUsageStack.getFirst());
  }

  /**
   * Pushes the connection with the given name to the top of the connection stack. This may create a new connection if
   * one does not already exist. Programmatic transaction control on the connection is not permitted.
   * @param pConnectionName
   */
  public void pushConnection(String pConnectionName) {
    pushConnection(pConnectionName, null, false, false);
  }

  /**
   * Creates a new autonomous connection (if a connection of this name does not already exist) and pushes it to the top
   * of the stack. Programmatic transaction control on the connection is permitted.
   * @param pConnectionName
   */
  public void pushAutonomousConnection(String pConnectionName) {
    pushConnection(pConnectionName, null, false, true);
  }

  /**
   * Creates a new connection (if a connection of this name does not already exist) and pushes it to the top of the stack.
   * Transaction control on the connection is permitted. The connection will be retained after popping, meaning it can be
   * pushed back onto the stack in the same state at a later point.
   * @param pConnectionName
   */
  public void pushRetainedConnection(String pConnectionName) {
    pushConnection(pConnectionName, null, true, true);
  }

  /**
   * Gets the connection with the given name, or creates one with the specified options if it does not exist, then pushes
   * the connection to the top of the connection stack.
   * @param pConnectionName Name of new/existing connection.
   * @param pOptionalInitialUCon Initial UCon to use when constructing the connection. Can be null.
   * @param pForceRetention Forces the new connection to be retained when it is popped even if there is no transaction active.
   * @param pAllowTransactionControl Allows consumers to use commit and rollback methods on the new connection.
   */
  private void pushConnection(String pConnectionName, UCon pOptionalInitialUCon, boolean pForceRetention, boolean pAllowTransactionControl) {

    //Get or create a connection from the map
    Connection lConnection = mConnectionMap.get(pConnectionName);
    if(lConnection == null) {
      lConnection = new Connection(pConnectionName, pOptionalInitialUCon, pForceRetention, pAllowTransactionControl);
      mConnectionMap.put(pConnectionName, lConnection);
    }

    //Add to the top of the connection stack
    mConnectionNameUsageStack.addFirst(pConnectionName);
  }

  public int getConnectionStackSize() {
    return mConnectionNameUsageStack.size();
  }

  /**
   * Removes the top connection from the connection stack. The connection name should be provided to validate that the stack
   * is in the expected state. The connection may be retained if it is still in use further up the stack, or if such behaviour
   * has been explicitly requested. Otherwise any underlying UCon is closed and the connection is removed from the connection
   * map. If the connection is being removed from the map and a transaction is still active on it, an error is raised.
   * @param pConnectionName
   */
  public void popConnection(String pConnectionName) {

    //Validate that there's a connection left to pop
    if(mConnectionNameUsageStack.size() == 0) {
      throw new ExInternal("Connection stack error: attempted to pop connection " + pConnectionName + " from an empty stack");
    }

    Connection lTopConnection = getTopConnection();

    if(!mConnectionNameUsageStack.getFirst().equals(pConnectionName)) {
      throw new ExInternal("Connection stack error: expected to pop entry named " + pConnectionName + " but found entry named " + lTopConnection.mConnectionName);
    }

    //Pop this connection usage from the usage stack
    mConnectionNameUsageStack.removeFirst();

    if(mConnectionNameUsageStack.contains(pConnectionName)) {
      //If this connection is still in use further down the stack, don't attempt to close it
      Track.debug("PopConnection", "Not closing connection " + pConnectionName + " as it is still in use on the connection stack");
    }
    else if(lTopConnection.mRetainConnection && lTopConnection.mPurposeStack.size() == 0) {
      //Retain the connection if required, but don't allow a pop when the purpose stack is non-empty (this indicates improper usage)
      Track.debug("PopConnection", "Not closing connection " + pConnectionName + " as it is being retained");
    }
    else {
      //Validate the pop - no purposes should be defined and transaction should not be active
      if(lTopConnection.mPurposeStack.size() > 0) {
        throw new ExInternal("Connection stack error: cannot pop connection named " + pConnectionName + " with a non-empty purpose stack - has " +
                             lTopConnection.mPurposeStack.size() + " entries (top: " + lTopConnection.mPurposeStack.getFirst() + ")" );
      }
      else if(lTopConnection.isTransactionActive()) {
        throw new ExInternal("Connection stack error: cannot pop connection named " + pConnectionName + " because a transaction is still active.");
      }

      if(lTopConnection.mOptionalCachedUCon != null) {
        lTopConnection.closeUCon(true);
      }

      mConnectionMap.remove(pConnectionName);
    }
  }

  /**
   * Gets the name of the top connection on the connection stack.
   * @return
   */
  public String getCurrentConnectionName() {
    return getTopConnection().mConnectionName;
  }

  /**
   * Tests if transaction control (i.e. from FOX markup) is permitted on the current connection.
   * @return
   */
  public boolean isTransactionControlAllowed() {
    return getTopConnection().mAllowTransactionControl;
  }

  /**
   * Gets a UCon from the top connection on the connection stack. This should be returned to the ContextUCon when it is
   * finished with using {@link #returnUCon(UCon, String)} - see {@link ContextUCon} documentation.
   * @param pPurpose Description for debugging and reporting purposes.
   * @return UCon for the top connection of the stack.
   */
  public UCon getUCon(String pPurpose) {
    return getTopConnection().getUCon(pPurpose);
  }

  /**
   * Returns a UCon previously acquired from {@link #getUCon(String)}. Note this may cause the UCon to be closed, so
   * ensure any operations which require the UCon to be open are performed before calling this method (i.e. reading from
   * result sets, temporary LOBs, etc). You must not continue to use the UCon after returning it.
   * @param pUCon UCon object being returned (used to validate object identity).
   * @param pPurpose Purpose string which exactly matches the purpose used when getting the UCon.
   */
  public void returnUCon(UCon pUCon, String pPurpose) {
    returnUCon(pUCon, pPurpose, false);
  }

  /**
   * Returns a UCon with the option to force it to be retained. See {@link #returnUCon(UCon, String)}.
   * @param pUCon UCon object being returned.
   * @param pPurpose Purpose string which exactly matches the purpose used when getting the UCon.
   * @param pRetainConnection If true, the UCon and by extension its connection will be retained by this ContextUCon until
   * {@link #closeAllRetainedConnections} is called.
   */
  public void returnUCon(UCon pUCon, String pPurpose, boolean pRetainConnection) {
    //Validate the correct UCon is being returned
    getTopConnection().validateAndReturnUCon(pUCon, pPurpose, pRetainConnection);
  }

  /**
   * Starts retaining the UCon attached to the connection (if there is one) at the top of the stack, meaning the connection will be held even
   * if there is no transaction active on it. The UCon will be retained until the connection is popped or {@link #stopRetainingUCon} is called.
   */
  public void startRetainingUCon() {
    Connection lTopConnection = getTopConnection();
    //Warn if we're already retaining - there's a risk stopRetaining will be called prematurely
    if(lTopConnection.mRetainUCon) {
      Track.alert("StartRetainingUCon", "Top connection is already marked for retention");
    }
    lTopConnection.mRetainUCon = true;
  }

  /**
   * Stops retaining the UCon on the top connection of the stack. The UCon will be immediately closed if there is no transaction
   * active on it.
   */
  public void stopRetainingUCon() {
    Connection lTopConnection = getTopConnection();
    lTopConnection.mRetainUCon = false;
    //If a UCon was provided during the retention period, attempt to release it now
    if(lTopConnection.mOptionalCachedUCon != null) {
      lTopConnection.returnUConInternal("STOP_RETENTION", false);
    }
  }

  /**
   * Closes all the connections this ContextUCon is currently retaining. If any of the connections have a transaction active
   * on them, or if any are still on the connection stack, this method will throw an error.
   */
  public void closeAllRetainedConnections() {
    for(Connection lEntry : mConnectionMap.values()) {
      if(lEntry.mRetainConnection) {
        if(lEntry.isTransactionActive()) {
          throw new ExInternal("Failed to close all connections: transaction still active on connection " + lEntry.getConnectionID());
        }
        else if(mConnectionNameUsageStack.contains(lEntry.mConnectionName)) {
          throw new ExInternal("Failed to close all connections: connection " + lEntry.getConnectionID() + " is still in use on the connection stack");
        }
        else {
          lEntry.returnUConInternal("CLOSE_ALL_RETAINED", true);
        }
      }
    }
  }

  /**
   * Rolls back and closes all the connections in this ContextUCon's connection map, then resets the ContextUCon to an
   * empty state. All errors are suppressed and logged to Track.
   * @param pAllowRecycle If true, UCons will be recycled on close.
   */
  public void rollbackAndCloseAll(boolean pAllowRecycle) {
    for(Connection lEntry : mConnectionMap.values()) {
      if(lEntry.mOptionalCachedUCon != null) {
        try {
          //Don't allow rollback failures to prevent the UCon from being closed - otherwise the CP won't know to release the connection
          try {
            lEntry.mOptionalCachedUCon.rollback();
          }
          catch (Throwable th) {
            Track.recordSuppressedException("RollbackAndCloseAll", th);
          }

          //Important: always ensure the connection is closed
          lEntry.closeUCon(pAllowRecycle);
        }
        catch (Throwable th) {
          Track.alert("RollbackAndCloseAllFailure", th.getMessage());
        }
      }
    }
    mConnectionMap.clear();
    mConnectionNameUsageStack.clear();
  }

  /**
   * Issues a commit on the top connection, if it allows transaction control. Otherwise an error is raised.
   */
  public void commitCurrentConnection() {
    Connection lTopConnection = getTopConnection();
    if(isTransactionControlAllowed()) {
      commit(lTopConnection.mConnectionName);
    }
    else {
      throw new ExInternal("Programmatic commit not allowed for connection " + lTopConnection.mConnectionName);
    }
  }

  /**
   * Issues a commit on the top connection, regardless of whether it allows transaction control. This allows the commit
   * to be executed by entry point code without having to call getUCon, which may create an unnecessary UCon. The connection
   * name must be provided to validate that the desired connection is being committed.
   */
  public void commit(String pConnectionName) {
    Connection lTopConnection = getTopConnection();
    if(!lTopConnection.mConnectionName.equals(pConnectionName)) {
      throw new ExInternal("Incorrect connection name for top connection, expected " + pConnectionName + " got " + lTopConnection.mConnectionName);
    }

    if(lTopConnection.mOptionalCachedUCon != null) {
      try {
        lTopConnection.mOptionalCachedUCon.commit();
      }
      catch (ExServiceUnavailable e) {
        throw new ExInternal("Failed to commit connection for " + pConnectionName, e);
      }
    }
  }

  /**
   * Issues a rollback on the top connection, if it allows transaction control. Otherwise an error is raised.
   */
  public void rollbackCurrentConnection() {
    Connection lTopConnection = getTopConnection();
    if(isTransactionControlAllowed()) {
      if(lTopConnection.mOptionalCachedUCon != null) {
        try {
          lTopConnection.mOptionalCachedUCon.rollback();
        }
        catch (ExDB e) {
          throw new ExInternal("Failed to rollback connection for " + lTopConnection.mConnectionName, e);
        }
      }
    }
    else {
      throw new ExInternal("Programmtic rollback not allowed for connection " + lTopConnection.mConnectionName);
    }
  }
}
