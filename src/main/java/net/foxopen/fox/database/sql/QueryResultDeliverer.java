package net.foxopen.fox.database.sql;

import net.foxopen.fox.ex.ExDB;

/**
 * Deliverer for delivering the results of an ExecutableQuery.
 */
public interface QueryResultDeliverer
extends ResultDeliverer<ExecutableQuery> {

  public void deliver(ExecutableQuery pQuery)
  throws ExDB;
  
}
