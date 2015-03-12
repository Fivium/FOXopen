package net.foxopen.fox.plugin.api.database;

import net.foxopen.fox.database.UCon;

public interface FxpContextUCon<UC extends FxpUCon> {
  /**
   * Pushes the connection with the given name to the top of the connection stack. This may create a new connection if
   * one does not already exist. Programmatic transaction control on the connection is not permitted.
   * @param pConnectionName
   */
  void pushConnection(String pConnectionName);

  /**
   * Creates a new autonomous connection (if a connection of this name does not already exist) and pushes it to the top
   * of the stack. Programmatic transaction control on the connection is permitted.
   * @param pConnectionName
   */
  void pushAutonomousConnection(String pConnectionName);

  /**
   * Creates a new connection (if a connection of this name does not already exist) and pushes it to the top of the stack.
   * Transaction control on the connection is permitted. The connection will be retained after popping, meaning it can be
   * pushed back onto the stack in the same state at a later point.
   * @param pConnectionName
   */
  void pushRetainedConnection(String pConnectionName);

  /**
   * Removes the top connection from the connection stack. The connection name should be provided to validate that the stack
   * is in the expected state. The connection may be retained if it is still in use further up the stack, or if such behaviour
   * has been explicitly requested. Otherwise any underlying UCon is closed and the connection is removed from the connection
   * map. If the connection is being removed from the map and a transaction is still active on it, an error is raised.
   * @param pConnectionName
   */
  void popConnection(String pConnectionName);

  /**
   * Gets the name of the top connection on the connection stack.
   * @return
   */
  String getCurrentConnectionName();

  /**
   * Tests if transaction control (i.e. from FOX markup) is permitted on the current connection.
   * @return
   */
  boolean isTransactionControlAllowed();

  /**
   * Gets a UCon from the top connection on the connection stack. This should be returned to the ContextUCon when it is
   * finished with - see {@link ContextUCon}documentation.
   * @param pPurpose Description for debugging and reporting purposes.
   * @return UCon for the top connection of the stack.
   */
  UC getUCon(String pPurpose);

  /**
   * Returns a UCon previously acquired from {@link #getUCon(String)}. Note this may cause the UCon to be closed, so
   * ensure any operations which require the UCon to be open are performed before calling this method (i.e. reading from
   * result sets, temporary LOBs, etc). You must not continue to use the UCon after returning it.
   * @param pUCon UCon object being returned (used to validate object identity).
   * @param pPurpose Purpose string which exactly matches the purpose used when getting the UCon.
   */
  void returnUCon(UC pUCon, String pPurpose);

  /**
   * Returns a UCon with the option to force it to be retained. See {@link #returnUCon(UCon,String)}.
   * @param pUCon UCon object being returned.
   * @param pPurpose Purpose string which exactly matches the purpose used when getting the UCon.
   * @param pRetainConnection If true, the UCon and by extension its connection will be retained by this ContextUCon until
   * {@link #closeAllRetainedConnections}is called.
   */
  void returnUCon(UC pUCon, String pPurpose, boolean pRetainConnection);

  /**
   * Starts retaining the UCon attached to the connection (if there is one) at the top of the stack, meaning the connection will be held even
   * if there is no transaction active on it. The UCon will be retained until the connection is popped or {@link #stopRetainingUCon}is called.
   */
  void startRetainingUCon();

  /**
   * Stops retaining the UCon on the top connection of the stack. The UCon will be immediately closed if there is no transaction
   * active on it.
   */
  void stopRetainingUCon();

  /**
   * Closes all the connections this ContextUCon is currently retaining. If any of the connections have a transaction active
   * on them, or if any are still on the connection stack, this method will throw an error.
   */
  void closeAllRetainedConnections();

  /**
   * Rolls back and closes all the connections in this ContextUCon's connection map, then resets the ContextUCon to an
   * empty state. All errors are suppressed and logged to Track.
   * @param pAllowRecycle If true, UCons will be recycled on close.
   */
  void rollbackAndCloseAll(boolean pAllowRecycle);

  /**
   * Issues a commit on the top connection, if it allows transaction control. Otherwise an error is raised.
   */
  void commitCurrentConnection();

  /**
   * Issues a commit on the top connection, regardless of whether it allows transaction control. This allows the commit
   * to be executed by entry point code without having to call getUCon, which may create an unnecessary UCon. The connection
   * name must be provided to validate that the desired connection is being committed.
   */
  void commit(String pConnectionName);

  /**
   * Issues a rollback on the top connection, if it allows transaction control. Otherwise an error is raised.
   */
  void rollbackCurrentConnection();
}
