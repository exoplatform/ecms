package org.exoplatform.ecm.webui.component.explorer.documents;

import javax.jcr.Node;

/**
 * The Interface NewDocumentEditorPlugin.
 */
public interface NewDocumentEditorPlugin {

  /**
   * Gets the provider.
   *
   * @return the provider
   */
  String getProvider();

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
