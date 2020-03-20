package org.exoplatform.services.cms.documents.exception;

/**
 * The Class PermissionValidationException.
 */
public class PermissionValidationException extends Exception {

  /** The Constant serialVersionUID. */
  private static final long serialVersionUID = 1284225353267641132L;

  /**
   * Instantiates a new permission validation exception.
   */
  public PermissionValidationException() {
    super();
  }

  /**
   * Instantiates a new permission validation exception.
   *
   * @param message the message
   */
  public PermissionValidationException(String message) {
    super(message);
  }

  /**
   * Instantiates a new permission validation exception.
   *
   * @param cause the cause
   */
  public PermissionValidationException(Throwable cause) {
    super(cause);
  }

  /**
   * Instantiates a new permission validation exception.
   *
   * @param message the message
   * @param cause the cause
   */
  public PermissionValidationException(String message, Throwable cause) {
    super(message, cause);
  }
}
