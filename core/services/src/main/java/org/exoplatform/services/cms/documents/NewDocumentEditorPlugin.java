package org.exoplatform.services.cms.documents;

/**
 * The Interface NewDocumentEditorPlugin provides API for handling such events from New Document feature: 
 * the document is going to be created, the document has been created. Also allows to get the provider.
 */
public interface NewDocumentEditorPlugin {

  /**
   * Gets the provider name.
   *
   * @return the provider name
   */
  String getProviderName();

  /**
   * This handler is invoked before creation a new document via NewDocumentTemplatePlugin
   * 
   * @param template the document template
   * @param parentPath the parent path
   * @param title the title
   */
  void beforeDocumentCreate(DocumentTemplate template, String parentPath, String title);

  /**
   * This handler is invoked after creation a new document via NewDocumentTemplatePlugin
   *
   * @param workspace the workspace
   * @param path the path
   * @throws Exception the exception
   */
  void onDocumentCreated(String workspace, String path) throws Exception;

}
