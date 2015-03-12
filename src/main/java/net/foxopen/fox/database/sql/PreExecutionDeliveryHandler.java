package net.foxopen.fox.database.sql;

import net.foxopen.fox.database.UCon;

/**
 * Implementors of this interface will be notified just before query execution is about to begin.
 * @param <T> Statement type the deliverer takes results from.
 */
public interface PreExecutionDeliveryHandler<T extends ExecutableStatement>
extends ResultDeliverer<T> {

  /**
   * This method is invoked before an ExecutableStatement is executed, allowing the deliverer to perform any just-in-time
   * preparation it requires.
   * @param pUCon UCon which statement will be executed with.
   * @param pExecutableStatement Statement about to be exectued.
   */
  void processBeforeExecution(UCon pUCon, T pExecutableStatement);

}
