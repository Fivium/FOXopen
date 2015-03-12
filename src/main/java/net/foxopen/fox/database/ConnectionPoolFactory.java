package net.foxopen.fox.database;


public interface ConnectionPoolFactory {

  public ConnectionPool createPool(ConnectionPoolConfig pPoolConfig);

}
