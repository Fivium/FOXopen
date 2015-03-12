package net.foxopen.fox.database.sql;

/**
 * Deliverer for delivering the results of an ExecutableAPI.
 */
public interface APIResultDeliverer
extends ResultDeliverer<ExecutableAPI> {
  
  public void deliver(ExecutableAPI pAPI);
  
}
