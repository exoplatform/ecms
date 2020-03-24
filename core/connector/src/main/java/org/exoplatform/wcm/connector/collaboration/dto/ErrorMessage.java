package org.exoplatform.wcm.connector.collaboration.dto;

/**
 * The Class ErrorMessage.
 */
public class ErrorMessage {

  /** The message. */
  private final String message;

  /** The error. */
  private final String error;

  /**
   * Instantiates a new error message.
   *
   * @param message the message
   * @param error the error
   */
  public ErrorMessage(String message, String error) {
    super();
    this.message = message;
    this.error = error;
  }

  /**
   * Gets the message.
   *
   * @return the message
   */
  public String getMessage() {
    return message;
  }

  /**
   * Gets the error.
   *
   * @return the error
   */
  public String getError() {
    return error;
  }

}