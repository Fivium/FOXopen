package net.foxopen.fox.database;

import net.foxopen.fox.XFUtil;
import net.foxopen.fox.sql.SQLManager;

import java.util.concurrent.TimeUnit;

/**
 * Configuration for a connection pool
 *
 * TODO - NP - Perhaps add statement cache options? prepStmtCacheSize/prepStmtCacheSqlLimit
 */
public class ConnectionPoolConfig {
  public static final String DEFAULT_CHECKIN_SQL = "BEGIN DBMS_SESSION.RESET_PACKAGE; ${schema.fox}.fox_engine.set_engine_type('FOX5'); END;";

  private static final String DEFAULT_INIT_SQL = "BEGIN ${schema.fox}.fox_engine.set_engine_type('FOX5'); "
                                                + UCon.SET_MODULE_INFO_STATEMENT.replace(":1", "'READY'").replace(":2", "''")
                                                + " END;";

  private final String mPoolName;
  private final String mURL;
  private final String mUser;
  private final String mPassword;
  private String mDataSourceClassName = "oracle.jdbc.pool.OracleDataSource";
  private String mConnectionInitSQL = SQLManager.replaceSQLSubstitutionVariables(DEFAULT_INIT_SQL);
  private String mConnectionCheckoutSQL = null;
  private String mConnectionCheckinSQL = SQLManager.replaceSQLSubstitutionVariables(DEFAULT_CHECKIN_SQL);
  private String mConnectionAliveTestSQL = null;
  private int mMinPoolSize = 1;
  private int mMaxPoolSize = 10;
  private long mAcquireTimeoutMS = TimeUnit.SECONDS.toMillis(30);
  private long mIdleTimeoutMS = TimeUnit.MINUTES.toMillis(10);
  private int mMaximumRecycles = 20;
  private boolean mAutoCommit = false;

  // These are the database fields used to construct a connection pool config.
  private final static String POOL_NAME_FIELD = "POOL_NAME"; // Required
  private final static String USERNAME_FIELD = "USERNAME"; // Required
  private final static String MIN_POOL_SIZE_FIELD = "MIN_POOL_SIZE"; // Required
  private final static String MAX_POOL_SIZE_FIELD = "MAX_POOL_SIZE"; // Required
  private final static String CONNECTION_TIMEOUT_MS = "CONNECTION_TIMEOUT_MS";
  private final static String CONNECTION_INIT_SQL_FIELD = "CONNECTION_INIT_SQL";
  private final static String CONNECTION_CHECKIN_SQL_FIELD = "CONNECTION_CHECKIN_SQL";
  private final static String CONNECTION_CHECKOUT_SQL_FIELD = "CONNECTION_CHECKOUT_SQL";

  /**
   * Construct a connection pool configuration object from the database. Use the result and grab the required fields.
   *
   * @param pFoxUConStatement Database UCon with the connection information
   * @param pDatabaseURL Database URL co
   * @param pPassword
   * @return
   */
  public static ConnectionPoolConfig createConnectionPoolConfig(UConStatementResult pFoxUConStatement, String pDatabaseURL, String pPassword) {
    // Grab fields
    String lPoolName = pFoxUConStatement.getString(POOL_NAME_FIELD);
    String lUsername =  pFoxUConStatement.getString(USERNAME_FIELD);
    Integer lMinPoolSize = pFoxUConStatement.getInteger(MIN_POOL_SIZE_FIELD);
    Integer lMaxPoolSize = pFoxUConStatement.getInteger(MAX_POOL_SIZE_FIELD);
    Long lAcquireTimeoutMS = pFoxUConStatement.getLong(CONNECTION_TIMEOUT_MS);
    String lInitSql = pFoxUConStatement.getString(CONNECTION_INIT_SQL_FIELD);
    String lCheckInSql = pFoxUConStatement.getString(CONNECTION_CHECKIN_SQL_FIELD);
    String lCheckOutSql = pFoxUConStatement.getString(CONNECTION_CHECKOUT_SQL_FIELD);

    // Construct connection pool config and set fields
    ConnectionPoolConfig lConnectionPoolConfig = new ConnectionPoolConfig(lPoolName, pDatabaseURL, lUsername, pPassword);

    if (!XFUtil.isNull(lInitSql)) {
      // Surround user input with setting of session variable.
      lInitSql = "BEGIN ${schema.fox}.fox_engine.set_engine_type('FOX5'); \n" + lInitSql + "\n END;";
      lConnectionPoolConfig.setConnectionInitSQL(lInitSql);
    }

    if (!XFUtil.isNull(lCheckOutSql)) {
      lConnectionPoolConfig.setConnectionCheckoutSQL(lCheckOutSql);
    }

    if (!XFUtil.isNull(lCheckInSql)) {
      lConnectionPoolConfig.setConnectionCheckinSQL(lCheckInSql);
    }

    if (!XFUtil.isNull(lAcquireTimeoutMS)) {
      lConnectionPoolConfig.setAcquireTimeoutMS(lAcquireTimeoutMS);
    }

    if (!XFUtil.isNull(lMaxPoolSize)) {
      lConnectionPoolConfig.setMaxPoolSize(lMaxPoolSize);
    }

    if (!XFUtil.isNull(lMinPoolSize)) {
      lConnectionPoolConfig.setMinPoolSize(lMinPoolSize);
    }

    return lConnectionPoolConfig;
  }

  /**
   * Create a default connection pool configuration. You probably want to set other values as the defaults are minimal.
   * Be careful when constructing this, as the ConnectionInitSQL is not defaulted to set the fox engine package variable
   * to FOX5 so that in the futuredifferent databases can be used. This must be set explicitly using DEFAULT_CHECKIN_SQL
   * if necessary.
   * @param pPoolName Name of the connection pool
   * @param pDatabaseURL JDBC connection URL
   * @param pUser Username to connect to Oracle with
   * @param pPassword Password to connect as pUser
   */
  public ConnectionPoolConfig (String pPoolName, String pDatabaseURL, String pUser, String pPassword) {
    if (XFUtil.isNull(pPoolName) || XFUtil.isNull(pDatabaseURL) || XFUtil.isNull(pUser) || XFUtil.isNull(pPassword)) {
      throw new IllegalArgumentException("Cannot construct a connection pool config without all the parameters having a value");
    }

    mPoolName = pPoolName;
    mURL = pDatabaseURL;
    mUser = pUser;
    mPassword = pPassword;
  }

  /**
   * Get the connection pool name
   *
   * @return Connection pool name
   */
  public final String getPoolName() {
    return mPoolName;
  }

  /**
   * Get the JDBC connection URL
   *
   * @return JDBC connection URL
   */
  public final String getURL() {
    return mURL;
  }

  /**
   * Get the Username to connect to Oracle with
   *
   * @return Username to connect to Oracle with
   */
  public final String getUser() {
    return mUser;
  }

  /**
   * Get the Password to connect as pUser.
   *
   * @return Password to connect as pUser
   */
  public final String getPassword() {
    return mPassword;
  }

  /**
   * Change the DataSource class name from oracle.jdbc.pool.OracleDataSource
   *
   * @param pDataSourceClassName Class name of a class that implements DataSource
   */
  public final void setDataSourceClassName(String pDataSourceClassName) {
    mDataSourceClassName = pDataSourceClassName;
  }

  /**
   * Get the DataSource class name that the connection pool should use
   *
   * @return DataSource class name
   */
  public final String getDataSourceClassName() {
    return mDataSourceClassName;
  }

  /**
   * Set the minimum amount of connections that the pool should have open, either idle or in use.
   *
   * Default is 1 connection.
   *
   * @param pMinPoolSize Minimum amount of connections to keep in the pool
   */
  public final void setMinPoolSize(int pMinPoolSize) {
    mMinPoolSize = pMinPoolSize;
  }

  /**
   * Get the minimum amount of connections that the pool should have open, either idle or in use
   *
   * @return Minimum amount of connections to keep in the pool
   */
  public final int getMinPoolSize() {
    return mMinPoolSize;
  }

  /**
   * Set the maximum amount of connections that the pool can have active at any time. If more connections are requested
   * they will have to wait for another connection to be released back to the pool.
   *
   * Default is 10 connections.
   *
   * @return Maximum amount of connections that can be in the pool
   */
  public final void setMaxPoolSize(int pMaxPoolSize) {
    mMaxPoolSize = pMaxPoolSize;
  }

  /**
   * Get the maximum amount of connections that the pool can have active at any time. If more connections are requested
   * they will have to wait for another connection to be released back to the pool
   *
   * @return Maximum amount of connections that can be in the pool
   */
  public final int getMaxPoolSize() {
    return mMaxPoolSize;
  }


  /**
   * How long to wait (in milliseconds) when acquiring a connection before giving up on the attempt.
   *
   * Default is 5 seconds.
   *
   * @param pAcquireTimeoutMS Milliseconds to wait giving up on a connection attempt
   */
  public final void setAcquireTimeoutMS(long pAcquireTimeoutMS) {
    mAcquireTimeoutMS = pAcquireTimeoutMS;
  }

  /**
   * How long to wait (in milliseconds) when acquiring a connection before giving up on the attempt
   *
   * @return Milliseconds to wait giving up on a connection attempt
   */
  public final long getAcquireTimeoutMS() {
    return mAcquireTimeoutMS;
  }

  /**
   * Set a SQL to run when a connection is created before it is added to the pool
   *
   * @param pConnectionInitSql SQL Statement
   */
  public final void setConnectionInitSQL(String pConnectionInitSql) {
    if(pConnectionInitSql != null) {
      pConnectionInitSql = SQLManager.replaceSQLSubstitutionVariables(pConnectionInitSql);
    }
    this.mConnectionInitSQL = pConnectionInitSql;
  }

  /**
   * Get a SQL to run when a connection is created before it is added to the pool
   *
   * @return SQL Statement
   */
  public final String getConnectionInitSQL() {
    return mConnectionInitSQL;
  }

  /**
   * Set a SQL to run when a connection is checked out from the pool
   *
   * @param pConnectionCheckoutSQL SQL Statement
   */
  public final void setConnectionCheckoutSQL(String pConnectionCheckoutSQL) {
    if(pConnectionCheckoutSQL != null) {
      pConnectionCheckoutSQL = SQLManager.replaceSQLSubstitutionVariables(pConnectionCheckoutSQL);
    }
    this.mConnectionCheckoutSQL = pConnectionCheckoutSQL;
  }

  /**
   * Get a SQL to run when a connection is checked out from the pool
   *
   * @return SQL Statement
   */
  public final String getConnectionCheckoutSQL() {
    return mConnectionCheckoutSQL;
  }

  /**
   * Set a SQL to run when a connection is checked back in to the pool
   *
   * @param pConnectionCheckinSQL SQL Statement
   */
  public final void setConnectionCheckinSQL(String pConnectionCheckinSQL) {
    if (XFUtil.isNull(pConnectionCheckinSQL)) {
      throw new IllegalArgumentException("Connection check in SQL can not be null or an empty string. This will cause the connection agent to fail.");
    }

    this.mConnectionCheckinSQL = SQLManager.replaceSQLSubstitutionVariables(pConnectionCheckinSQL);
  }

  /**
   * Get a SQL to run when a connection is checked back in to the pool
   *
   * @return SQL Statement
   */
  public final String getConnectionCheckinSQL() {
    return mConnectionCheckinSQL;
  }

  /**
   * Set the maximum amount of time (in milliseconds) that a connection is allowed to sit idle in the pool before it is
   * retired.
   *
   * Default is 10 minutes.
   *
   * @param pIdleTimeoutMS Milliseconds that a connection is allowed to sit idle in the pool
   */
  public final void setIdleTimeoutMS(long pIdleTimeoutMS) {
    this.mIdleTimeoutMS = pIdleTimeoutMS;
  }

  /**
   * Get the maximum amount of time (in milliseconds) that a connection is allowed to sit idle in the pool before it is
   * retired.
   *
   * @return Milliseconds that a connection is allowed to sit idle in the pool
   */
  public final long getIdleTimeoutMS() {
    return mIdleTimeoutMS;
  }

  /**
   * Sets if the connection should be in auto commit mode. If so all statements are run in their own transaction and
   * committed.
   *
   * Default is false.
   *
   * @param pAutoCommit
   */
  public final void setAutoCommit(boolean pAutoCommit) {
    this.mAutoCommit = pAutoCommit;
  }

  /**
   * Checks if the connection should be in auto commit mode. If so all statements are run in their own transaction and
   * committed.
   *
   * @return AutoCommit mode
   */
  public final boolean isAutoCommit() {
    return mAutoCommit;
  }

  /**
   * Sets the amount of times a connection can be put back in the recycle pool before it is forcefully closed.
   *
   * Default is 20.
   *
   * @param pMaximumRecycles
   */
  public final void setMaximumRecycles(int pMaximumRecycles) {
    this.mMaximumRecycles = pMaximumRecycles;
  }

  /**
   * Sets the amount of times a connection can be put back in the recycle pool before it is forcefully closed.
   *
   * @return Maximum times a connection can be recycled
   */
  public final int getMaximumRecycles() {
    return mMaximumRecycles;
  }

  /**
   * Sets the query to use to test a connection is alive (typically something like "SELECT * FROM dual).
   * Setting this to anything will make it use this for alive-testing, if not set (default) the connection pool will use
   * the isValid() function from the JDBC4 spec.
   *
   * @param pConnectionAliveTestSQL SQL Statement
   */
  public void setConnectionAliveTestSQL(String pConnectionAliveTestSQL) {
    this.mConnectionAliveTestSQL = pConnectionAliveTestSQL;
  }

  /**
   * Gets the query to use to test a connection is alive (typically something like "SELECT * FROM dual)
   *
   * @return SQL Statement
   */
  public String getConnectionAliveTestSQL() {
    return mConnectionAliveTestSQL;
  }
}
