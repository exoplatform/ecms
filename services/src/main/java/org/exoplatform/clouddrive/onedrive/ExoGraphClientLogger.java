package org.exoplatform.clouddrive.onedrive;

import com.microsoft.graph.logger.ILogger;
import com.microsoft.graph.logger.LoggerLevel;

import org.exoplatform.services.log.Log;

/**
 * Exo logger adapter for use in graph client.
 */
class ExoGraphClientLogger implements ILogger {

  private final Log log;

  public ExoGraphClientLogger(Log log) {
    this.log = log;
  }

  @Override
  public void setLoggingLevel(LoggerLevel loggerLevel) {

  }

  @Override
  public LoggerLevel getLoggingLevel() {
    return LoggerLevel.DEBUG;
  }

  @Override
  public void logDebug(String s) {
    if (log.isDebugEnabled()) {
      log.debug(s);
    }
  }

  @Override
  public void logError(String s, Throwable throwable) {
    log.error(s, throwable);
  }
}
