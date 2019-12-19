package org.exoplatform.ecm.webui.component.explorer.documents;

import java.util.List;

import javax.jcr.Node;

/**
 * The Interface NewDocumentTemplatePlugin provides API for getting templates of specific provider.
 * Also allows to create documents based on DocumentTemplate
 */
public interface NewDocumentTemplatePlugin {

  /**
   * Gets the provider.
   *
   * @return the provider
   */
  String getProvider();

  /**
   * Gets the templates.
   *
   * @return the templates
   */
  List<DocumentTemplate> getTemplates();

  /**
   * Creates the document from specified template.
   *
   * @param parent the parent
   * @param title the title
   * @param template the template
   * @return the node
   * @throws Exception the exception
   */
  Node createDocument(Node parent, String title, DocumentTemplate template) throws Exception;

}
