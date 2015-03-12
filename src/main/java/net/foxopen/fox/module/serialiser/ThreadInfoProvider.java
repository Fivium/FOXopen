package net.foxopen.fox.module.serialiser;

public interface ThreadInfoProvider {

  public String getThreadId();

  public String getThreadAppMnem();

  /**
   * Gets a human-readable reference for this thread. This is NOT an identifier; use {@link #getThreadId} for the actual
   * thread ID.
   * @return Thread ID concatenated with app mnem.
   */
  public String getThreadRef();

  public String getUserThreadSessionId();

  public int getScrollPosition();

  public String getCurrentCallId();

}
