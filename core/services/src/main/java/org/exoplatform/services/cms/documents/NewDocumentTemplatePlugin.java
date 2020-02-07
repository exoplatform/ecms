package org.exoplatform.services.cms.documents;

import java.util.List;

import javax.jcr.Node;

/**
 * The Interface NewDocumentTemplatePlugin provides API for getting templates of specific provider.
 * Also allows to create documents based on DocumentTemplate.
 */
public interface NewDocumentTemplatePlugin {

  /**
   * Gets the templates.
   *
   * @return the templates
   */
  List<DocumentTemplate> getTemplates();
  
  /**
   * Gets the template 
   * 
   * @param name the name
   * @return the template
   */
  DocumentTemplate getTemplate(String name);
  
  /**
   * Gets editor plugin
   * 
   * @return the editorPlugin
   */
  DocumentEditorPlugin getEditor();

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
