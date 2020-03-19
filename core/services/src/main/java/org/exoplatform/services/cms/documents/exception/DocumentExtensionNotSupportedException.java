package org.exoplatform.services.cms.documents.exception;

/**
 * The DocumentExtensionNotSupportedException is thrown when the service/plugin doesn't support the provided document extension.
 * For example {@link org.exoplatform.services.cms.documents.DocumentMetadataPlugin#updateMetadata updateMetadata} 
 * throws the exception if the plugin cannot process the document with specified extension.
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
   * @param message the message
   * @param cause the cause
   */
  public DocumentExtensionNotSupportedException(String message, Throwable cause) {
    super(message, cause);
  }

}
