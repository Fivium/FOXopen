package net.foxopen.fox.database;


import net.foxopen.fox.ex.ExInternal;

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
    try {
      return new HikariConnectionPool(pPoolConfig);
    }
    catch (Throwable th) {
      throw new ExInternal("Error during creation of Hikari pool " + pPoolConfig.getPoolName(), th);
    }
  }
}
