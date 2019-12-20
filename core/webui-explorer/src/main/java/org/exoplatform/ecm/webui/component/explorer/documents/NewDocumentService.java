package org.exoplatform.ecm.webui.component.explorer.documents;

import java.util.List;
import java.util.Map;

import javax.jcr.Node;

import org.exoplatform.container.component.ComponentPlugin;

/**
 * Provides APIs to manage document template and editor plugins.
 * With these API types, you can:

 * <ul>
 * <li>Register DocumentTemplatePlugin or DocumentEditorPlugin.</li>
 * <li>Get registered DocumentTemplatePlugin or DocumentEditorPlugin.</li>
 * <li>Check if any document template plugin is registered</li>
 * <li>Get document templates</li>
 * <li>Create a new document</li>
 * </ul>
 * 
 */
public interface NewDocumentService {

  /**
   * Adds the document template plugin.
   *
   * @param plugin the plugin
   */
  void addDocumentTemplatePlugin(ComponentPlugin plugin);

  /**
   * Adds the document editor plugin.
   *
   * @param plugin the plugin
   */
  void addDocumentEditorPlugin(ComponentPlugin plugin);

  /**
   * Creates the document.
   *
   * @param currentNode the current node
   * @param title the title
   * @param template the template
   * @return the node
   * @throws Exception the exception
   */
  Node createDocument(Node currentNode, String title, DocumentTemplate template) throws Exception;

  /**
   * Gets the document template.
   *
   * @param provider the provider
   * @param label the label
   * @return the document template
   */
  DocumentTemplate getDocumentTemplate(String provider, String label);

  /**
   * Gets the document template plugin.
   *
   * @param provider the provider
   * @return the document template plugin
   */
  NewDocumentTemplatePlugin getDocumentTemplatePlugin(String provider);

  /**
   * Gets the document editor plugin.
   *
   * @param provider the provider
   * @return the document editor plugin
   */
  NewDocumentEditorPlugin getDocumentEditorPlugin(String provider);

  /**
   * Gets the registered template plugins.
   *
   * @return the registered template plugins
   */
  Map<String, NewDocumentTemplatePlugin> getRegisteredTemplatePlugins();

  /**
   * Checks for document template plugins.
   *
   * @return true, if successful
   */
  boolean hasDocumentTemplatePlugins();

  /**
   * NewDocumentTypesConfig contains all registered templates for specified provider.
   */
  public static class DocumentTemplatesConfig {

    /** The document templates. */
    protected List<DocumentTemplate> templates;

    /** The provider. */
    protected String                 provider;

    /**
     * Gets the document templates.
     *
     * @return the document types
     */
    public List<DocumentTemplate> getTemplates() {
      return templates;
    }

    /**
     * Sets the document templates.
     *
     * @param templates the new templates
     */
    public void setTemplates(List<DocumentTemplate> templates) {
      this.templates = templates;
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
     * {
     * @param provider
     */
    public void setProvider(String provider) {
      this.provider = provider;
    }

  }

}
