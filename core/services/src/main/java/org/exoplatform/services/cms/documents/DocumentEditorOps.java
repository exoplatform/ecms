package org.exoplatform.services.cms.documents;

import java.util.Arrays;
import java.util.List;

/**
 * The Interface DocumentEditorOps.
 */
public interface DocumentEditorOps {

  /**
   * This handler is invoked before creation a new document via NewDocumentTemplatePlugin.
   *
   * @param template the document template
   * @param parentPath the parent path
   * @param title the title
   * @throws Exception the exception
   */
  void beforeDocumentCreate(DocumentTemplate template, String parentPath, String title) throws Exception;

  /**
   * This handler is invoked after creation a new document via NewDocumentTemplatePlugin.
   *
   * @param workspace the workspace
   * @param path the path
   * @throws Exception the exception
   */
  void onDocumentCreated(String workspace, String path) throws Exception;

  /**
   * This handles is invoked when the DocumentUIActivity is rendered in the Activity Stream.
   * It allows to run custom code (JS initialization, setting sockets, etc) while 
   * the activity is being rendered.
   *
   * @param uuid the uuid
   * @param workspace workspace
   * @param activityId the activity id
   * @param context the context
   * @throws Exception the exception
   */
  void initActivity(String uuid, String workspace, String activityId, String context) throws Exception;

  /**
   * This handles is invoked when the DocumentUIActivity is rendered in the Activity Stream.
   * It allows to run custom code (JS initialization, setting sockets, etc) while 
   * the preview is being rendered.
   * 
   * @param uuid the uuid
   * @param workspace workspace
   * @param activityId the activity id
   * @param context the context
   * @param index the index
   * @throws Exception the exception
   */
  void initPreview(String uuid, String workspace, String activityId, String context, int index) throws Exception;

  /**
   * Gets the provider name.
   *
   * @return the provider name
   */
  String getProviderName();
  
  /**
   * Gets the permissions.
   *
   * @return the permissions
   */
  default List<String> getPermissions() {
    return Arrays.asList("*");
  }

  /**
   * Checks if is active.
   *
   * @return true, if is active
   */
  default boolean isActive() {
    return true;
  }
}
