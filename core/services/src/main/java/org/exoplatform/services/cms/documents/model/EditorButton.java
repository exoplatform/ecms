package org.exoplatform.services.cms.documents.model;

/**
 * The Class EditorButton.
 */
public class EditorButton {

  /** The editor link. */
  private final String editorLink;

  /** The label. */
  private final String label;
  
  /**  The fileId. */
  private final String fileId;

  /** The provider. */
  private final String provider;

  /**
   * Instantiates a new editor button.
   *
   * @param editorLink the editor link
   * @param label the label
   * @param fileId the fileId
   * @param provider the provider
   */
  public EditorButton(String editorLink, String label, String fileId, String provider) {
    this.editorLink = editorLink;
    this.label = label;
    this.fileId = fileId;
    this.provider = provider;
  }

  /**
   * Gets the editor link.
   *
   * @return the editor link
   */
  public String getEditorLink() {
    return editorLink;
  }

  /**
   * Gets the label.
   *
   * @return the label
   */
  public String getLabel() {
    return label;
  }

  /**
   * Gets the provider.
   *
   * @return the provider
   */
  public String getProvider() {
    return provider;
  }

  
   /**
    * Gets the file id.
    *
    * @return the file id
    */
   public String getFileId() {
     return fileId;
   }

}
