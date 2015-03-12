package net.foxopen.fox.database.sql;

import net.foxopen.fox.ex.ExDB;

/**
 * An object which is capable of delivering the results from an ExecutableStatement to an appropriate destination.
 * Typically a ResultDeliverer will need to be constructed just before the statement is executed and contain some contextual
 * information about where the results should be delivered to (e.g. a DOM node or Java object). The deliver method is
 * called after the statement is executed but before it is closed, to allow access to any temporary resources the statement
 * may have created.
 * @param <T> Type of ExecutableStatement being delivered from - either an ExecutableAPI or ExecutableQuery.
 */
public interface ResultDeliverer<T extends ExecutableStatement> {
  
  /**
   * Delivers the results of the executed (but not yet closed) ExecutableStatement to the destination determined by the implementor.
   * @param pStatement Executed ExecutableStatement.
   * @throws ExDB If delivery fails.
   */
  public void deliver(T pStatement)
  throws ExDB;
  
  /**
   * Tests if this deliverer requires its associated statement to be closed immediately after delivery. If false,
   * the consumer will be expected to manually close the statement and its CloseableBinds after it has finished processing
   * the statement results. <br/><br/>
   * This flag allows ResultDeliverers to perform their delivery processing in steps (i.e. external loop iterations) rather 
   * than all in one go.
   * @return True if the statement should be immediately closed following delivery.
   */
  public boolean closeStatementAfterDelivery();
  
}
