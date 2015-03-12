package net.foxopen.fox.database.sql.bind;

import java.sql.SQLException;

/**
 * BindObjects which hold temporary resources and require closing after statement execution.
 */
public interface CloseableBindObject
extends BindObject {
  
  /**
   * Closes any temporary resources associated with this bind object. This will be invoked after the associated statement 
   * has been executed.
   */
  public void close()
  throws SQLException;
}
