package net.foxopen.fox.database;


import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import net.foxopen.fox.entrypoint.FoxGlobals;
import oracle.jdbc.OracleConnection;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;


public class HikariConnectionPool
implements ConnectionPool {

  private final HikariDataSource mDataSource;
  private final ConnectionPoolConfig mConfig;

  HikariConnectionPool(ConnectionPoolConfig pPoolConfig) {
    mConfig = pPoolConfig;

    HikariConfig lConfig = new HikariConfig();
    lConfig.setPoolName(mConfig.getPoolName());

    lConfig.setDataSourceClassName(mConfig.getDataSourceClassName());
    lConfig.addDataSourceProperty("URL", "jdbc:oracle:thin:@" + mConfig.getURL());
    lConfig.addDataSourceProperty("user", mConfig.getUser());
    lConfig.addDataSourceProperty("password", mConfig.getPassword());

    //Pass a properties object through to the OracleConnection so we can set the "program" session value to the current FOX version
    Properties lProperties = new Properties();
    //Note: database 11.2.0.3 fails to return binary XML correctly when "JDBC Thin Client" is not a substring of v$session.program
    lProperties.put(OracleConnection.CONNECTION_PROPERTY_THIN_VSESSION_PROGRAM, FoxGlobals.getInstance().getEngineVersionNumber() + " - JDBC Thin Client");
    lConfig.addDataSourceProperty("connectionProperties", lProperties);

    lConfig.setUsername(mConfig.getUser());
    lConfig.setPassword(mConfig.getPassword());

    lConfig.setMaximumPoolSize(mConfig.getMaxPoolSize());
    lConfig.setMinimumIdle(mConfig.getMinPoolSize());

    lConfig.setIdleTimeout(mConfig.getIdleTimeoutMS());

    lConfig.setConnectionTimeout(mConfig.getAcquireTimeoutMS());

    lConfig.setAutoCommit(mConfig.isAutoCommit());

    // Forced to true so !SHOWSTATS can see the state of pools. Perhaps turn off one day as this has a performance hit
    lConfig.setRegisterMbeans(true);

    // Make the config fail when first constructed rather than waiting for a connection get
    lConfig.setInitializationFailFast(true);

    if (mConfig.getConnectionAliveTestSQL() != null) {
      lConfig.setConnectionTestQuery(mConfig.getConnectionAliveTestSQL());
      lConfig.setJdbc4ConnectionTest(false);
    }

    lConfig.setConnectionInitSql(mConfig.getConnectionInitSQL());

    mDataSource = new HikariDataSource(lConfig);
  }

  @Override
  public void shutdownPool() {
    mDataSource.shutdown();
  }

  @Override
  public Connection getConnection() throws SQLException {
    return mDataSource.getConnection();
  }

  @Override
  public ConnectionPoolConfig getConfig() {
    return mConfig;
  }

  @Override
  public void releaseConnection(Connection pCon) {
    try {
      pCon.close();
    }
    catch (SQLException e) {
    }
  }

  @Override
  public void forceCloseConnection(Connection pCon) {
    mDataSource.evictConnection(pCon);
  }
}
