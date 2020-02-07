package org.exoplatform.services.cms.documents;

import org.exoplatform.services.cms.documents.model.EditorButton;

/**
 * The Interface DocumentEditorPlugin provides API for handling such events from New Document feature: 
 * the document is going to be created, the document has been created. 
 * Also allows to get the provider and editor link.
 */
public interface DocumentEditorPlugin {

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

  /**
   * Gets editor button for the document
   * 
   * @param uuid the uuid
   * @param workspace workspace
   * @param context the context
   * @throws Exception the exception
   * @return the editor button
   */
  EditorButton getEditorButton(String uuid, String workspace, String context) throws Exception;

  /**
   * This handles is invoked when the DocumentUIActivity is rendered in the Activity Stream.
   * It allows to run custom code (JS initialization, setting sockets, etc) while 
   * the activity is being rendered.
   * 
   * @param fileId
   * @throws Exception the exception
   */
  void initActivity(String fileId) throws Exception;

  /**
   * This handles is invoked when the DocumentUIActivity is rendered in the Activity Stream.
   * It allows to run custom code (JS initialization, setting sockets, etc) while 
   * the preview is being rendered.
   * 
   * @param fileId
   * @throws Exception the exception
   */
  void initPreview(String fileId) throws Exception;

}
