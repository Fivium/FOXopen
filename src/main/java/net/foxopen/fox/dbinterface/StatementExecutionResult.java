package net.foxopen.fox.dbinterface;

import net.foxopen.fox.database.sql.bind.CachedBindObjectProvider;

/**
 * Container for any objects resulting from an {@link InterfaceStatement} execution. Currently, a statement may result in a
 * CachedBindObjectProvider if it was executed with bind caching enabled. Note that most "results" of a statement execution
 * are processed by a ResultDeliverer.
 */
public class StatementExecutionResult {

  private static final StatementExecutionResult DEFAULT_EMPTY_INSTANCE = new StatementExecutionResult(null);

  /**
   * Creates an instance for a statement execution which did not cache any binds.
   * @return Default empty instance.
   */
  static StatementExecutionResult defaultEmptyResult() {
    return DEFAULT_EMPTY_INSTANCE;
  }

  /**
   * Creates StatementExecutionResult for a statement which resulted in the given BindObjectProvider.
   * @param pCachedBindObjectProvider BindObjectProvider from the statement execution.
   * @return New StatementExecutionResult.
   */
  static StatementExecutionResult createCachedBindResult(CachedBindObjectProvider pCachedBindObjectProvider) {
    return new StatementExecutionResult(pCachedBindObjectProvider);
  }

  private final CachedBindObjectProvider mCachedBindObjectProvider;

  private StatementExecutionResult(CachedBindObjectProvider pCachedBindObjectProvider) {
    mCachedBindObjectProvider = pCachedBindObjectProvider;
  }

  /**
   * Gets a CachedBindObjectProvider which was created when an InterfaceStatement was executed.
   * This may be null if bind caching was not enabled when the statement was executed.
   * @return CachedBindObjectProvider from statement execution, or null.
   */
  public CachedBindObjectProvider getCachedBindObjectProvider() {
    return mCachedBindObjectProvider;
  }
}
