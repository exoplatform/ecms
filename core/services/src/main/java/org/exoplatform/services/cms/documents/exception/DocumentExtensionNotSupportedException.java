package org.exoplatform.services.cms.documents.exception;

/**
 * The Class DocumentExtensionNotSupportedException.
 */
public class DocumentExtensionNotSupportedException extends Exception {

  /** The Constant serialVersionUID. */
  private static final long serialVersionUID = -4638450700971596435L;


  /**
   * Instantiates a new document extension not supported exception.
   *
   * @param message the message
   */
  public DocumentExtensionNotSupportedException(String message) {
    super(message);
  }

  /**
   * Instantiates a new document extension not supported exception.
   *
   * @param cause the cause
   */
  public DocumentExtensionNotSupportedException(Throwable cause) {
    super(cause);
  }


  /**
   * Instantiates a new document extension not supported exception.
   *
   * @param message the message
   * @param cause the cause
   */
  public DocumentExtensionNotSupportedException(String message, Throwable cause) {
    super(message, cause);
  }

}