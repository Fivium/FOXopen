package net.foxopen.fox.logging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FoxLogger {
  private static final Logger mLogger = LoggerFactory.getLogger("FoxLogger");

  public static Logger getLogger() {
    return mLogger;
  }

  private FoxLogger() {
  }
}
