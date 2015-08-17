package net.foxopen.fox.dbinterface;

import net.foxopen.fox.database.sql.bind.CachedBindObjectProvider;
import net.foxopen.fox.database.sql.bind.DecoratingBindObjectProvider;

/**
 * Parameter object for controlling how binds are supplied for the execution of an {@link InterfaceStatement}.
 */
public interface StatementExecutionBindOptions {

  /**
   * Gets an optional DecoratingBindObjectProvider which will be used to decorate the statement's default provider.
   * Default is null.
   * @return DecoratingBindObjectProvider or null.
   */
  default DecoratingBindObjectProvider getDecoratingBindObjectProvider() {
    return null;
  }

  /**
   * Gets an optional CachedBindObjectProvider to use to execute the statement. This will be used instead of the default
   * BindProvider if specified, but will still be decorated by the DecoratingBindObjectProvider if one is available.
   * @return CachedBindObjectProvider or null.
   */
  default CachedBindObjectProvider getCachedBindObjectProvider() {
    return null;
  }

  /**
   * If true, bind caching will be enabled for the statement execution. This may cause a performance overhead as bound
   * objects may need to be cloned to cacheable. This cannot be true if a CachedBindObjectProvider is provided. Cached
   * binds can be retrieved from a {@link StatementExecutionResult}.
   * @return True if binds should be cached.
   */
  default boolean cacheBinds() {
    return false;
  }
}
