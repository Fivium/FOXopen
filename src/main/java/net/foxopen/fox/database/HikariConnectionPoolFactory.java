package net.foxopen.fox.database;


public class HikariConnectionPoolFactory
implements ConnectionPoolFactory {

  private static final HikariConnectionPoolFactory INSTANCE = new HikariConnectionPoolFactory();

  public static HikariConnectionPoolFactory instance() {
    return INSTANCE;
  }

  private HikariConnectionPoolFactory() {
  }

  @Override
  public ConnectionPool createPool(ConnectionPoolConfig pPoolConfig) {
    return new HikariConnectionPool(pPoolConfig);
  }
}
