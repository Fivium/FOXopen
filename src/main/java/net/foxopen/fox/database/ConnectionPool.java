package net.foxopen.fox.database;

import java.sql.Connection;
import java.sql.SQLException;


public interface ConnectionPool {

  public void shutdownPool();

  public ConnectionPoolConfig getConfig();

  public Connection getConnection() throws SQLException;

  public void releaseConnection(Connection pCon);

  public void forceCloseConnection(Connection pCon);

}
