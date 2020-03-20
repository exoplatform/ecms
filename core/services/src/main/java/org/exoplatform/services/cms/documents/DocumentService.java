/*
 * Copyright (C) 2003-2011 eXo Platform SAS.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, see<http://www.gnu.org/licenses/>.
 */
package org.exoplatform.services.cms.documents;

import java.util.List;
import java.util.Set;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.exoplatform.container.component.ComponentPlugin;
import org.exoplatform.services.cms.documents.model.Document;
import org.exoplatform.services.cms.drives.DriveData;

/**
 * Created by The eXo Platform SAS Author : eXoPlatform exo@exoplatform.com Mar
 * 22, 2011
 */
public interface DocumentService {
  public Document findDocById(String id) throws RepositoryException;

  /**
   * Get the short link to display a document in the Documents app by its id.
   * @param workspaceName The workspace of the node
   * @param nodeId The id of the node
   * @return The link to open the document
   * @throws Exception
   */
  public String getShortLinkInDocumentsApp(String workspaceName, String nodeId) throws Exception;

  /** return the URL of the shared document in the Shared Personal Documents folder of the user destination
   *
   * @param currentNode
   * @param username
   * @return
   * @throws Exception
   */
  default String getDocumentUrlInPersonalDocuments(Node currentNode, String username) throws Exception {
    return null;
  }

  /** return the URL of the shared document in the Shared Documents folder of the space destination
   *
   * @param currentNode
   * @param spaceId
   * @return
   * @throws Exception
   */
  default String getDocumentUrlInSpaceDocuments(Node currentNode, String spaceId) throws Exception {
    return null;
  }

  /**
   * Get the link to display a document in the Documents app.
   * It will try to get the best matching context (personal doc, space doc, ...).
   * @param nodePath The path of the node
   * @return The link to open the document
   * @throws Exception
   */
  public String getLinkInDocumentsApp(String nodePath) throws Exception;

  /**
   * Get the link to display a document in the Documents app in the given drive.
   * It will try to get the best matching context (personal doc, space doc, ...).
   * @param nodePath The path of the node
   * @param drive The drive to use
   * @return The link to open the document
   * @throws Exception
   */
  public String getLinkInDocumentsApp(String nodePath, DriveData drive) throws Exception;

  /**
   * Get the drive containing the node with the given node path, for the current user.
   * If several drives contain the node, try to find the best matching.
   * @param nodePath The path of the node
   * @return The drive containing the node
   * @throws Exception
   */
  DriveData getDriveOfNode(String nodePath) throws Exception;

  /**
   * Get the drive containing the node with the given node path.
   * If several drives contain the node, try to find the best matching.
   * @param nodePath The path of the node
   * @param userId The user id
   * @param memberships The user memberships
   * @return The drive containing the node
   * @throws Exception
   */
  public DriveData getDriveOfNode(String nodePath, String userId, List<String> memberships) throws Exception;
  
  /**
   * Adds the document template plugin.
   *
   * @param plugin the plugin
   */
  public void addDocumentTemplatePlugin(ComponentPlugin plugin);

  /**
   * Adds the document editor plugin.
   *
   * @param plugin the plugin
   */
  public void addDocumentEditorPlugin(ComponentPlugin plugin);

  /**
   * Creates the document from template.
   *
   * @param currentNode the current node
   * @param title the title
   * @param template the template
   * @return the node
   * @throws Exception the exception
   */
  public Node createDocumentFromTemplate(Node currentNode, String title, DocumentTemplate template) throws Exception;

  /**
   * Gets the registered template plugins.
   *
   * @return the registered template plugins
   */
  public Set<NewDocumentTemplatePlugin> getRegisteredTemplatePlugins();
  
  /**
   * Gets the registered editor plugins.
   *
   * @return the registered editors plugins
   */
  public Set<DocumentEditorPlugin> getRegisteredEditorPlugins();

  /**
   * Checks for document editor plugins.
   *
   * @return true, if successful
   */
  public boolean hasDocumentEditorPlugins();
  
  /**
   * Checks for document template plugins.
   *
   * @return true, if successful
   */
  public boolean hasDocumentTemplatePlugins();
  
  /**
   * Registers document metadata plugin.
   * 
   * @param plugin the ComponentPlugin
   */
  public void addDocumentMetadataPlugin(ComponentPlugin plugin);
  
  /**
   * Gets prefered editor provider for specified file and user.
   *
   * @param userId the userId
   * @param uuid the uuid
   * @param workspace the workspace
   * @return the preffered editor (provider)
   * @throws Exception the exception
   */
  public String getPreferedEditor(String userId, String uuid, String workspace) throws Exception;
  
  /**
   * Sets preffered editor provider for specified file and user.
   *
   * @param userId the userId
   * @param provider the editor provider
   * @param uuid the uuid
   * @param workspace the workspace
   * @throws Exception the exception
   */
  public void setPreferedEditor(String userId, String provider, String uuid, String workspace) throws Exception;

  /**
   * NewDocumentTypesConfig contains all registered templates for specified provider.
   */
  public static class DocumentTemplatesConfig {

    /** The document templates. */
    protected List<DocumentTemplate> templates;

    /** The providerName. */
    protected String                 providerName;

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
    public String getProviderName() {
      return providerName;
    }

    /**
     * {
     * @param providerName
     */
    public void setProviderName(String providerName) {
      this.providerName = providerName;
    }
    
  }
}
