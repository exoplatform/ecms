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
   */
  void beforeDocumentCreate();

  /**
   * This handler is invoked after creation a new document via NewDocumentTemplatePlugin
   *
   * @param node the node
   * @throws Exception the exception
   */
  void onDocumentCreated(Node node) throws Exception;

}
