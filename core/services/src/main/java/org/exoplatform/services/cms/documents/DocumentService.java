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

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.exoplatform.container.component.ComponentPlugin;
import org.exoplatform.services.cms.documents.exception.DocumentEditorProviderNotFoundException;
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
  public Node createDocumentFromTemplate(Node currentNode, String title, NewDocumentTemplate template) throws Exception;

  /**
   * Gets the registered template providers.
   *
   * @return the registered template providers
   */
  public List<NewDocumentTemplateProvider> getNewDocumentTemplateProviders();
  
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
   * @throws RepositoryException the exception
   */
  public String getPreferedEditor(String userId, String uuid, String workspace) throws RepositoryException;
  
  /**
   * Sets preffered editor provider for specified file and user.
   *
   * @param userId the userId
   * @param provider the editor provider
   * @param uuid the uuid
   * @param workspace the workspace
   * @throws RepositoryException the exception
   */
  public void savePreferedEditor(String userId, String provider, String uuid, String workspace) throws RepositoryException;

  /**
   * Gets the editor providers.
   *
   * @return the editor providers
   */
  public List<DocumentEditorProvider> getDocumentEditorProviders();
  
  /**
   * Gets the editor provider.
   *
   * @param provider the provider
   * @return the editor provider
   */
  public DocumentEditorProvider getEditorProvider(String provider) throws DocumentEditorProviderNotFoundException;
  
  /**
   * Gets the documents by folder.
   *
   * @param folder the folder
   * @param limit the limit
   * @return the documents by folder
   */
  public List<Document> getDocumentsByFolder(String folder, long limit) throws Exception;
  
  /**
   * Gets the documents by query.
   *
   * @param query the query
   * @param limit the limit
   * @return the documents by query
   */
  public List<Document> getDocumentsByQuery(String query, long limit) throws Exception;
  
  /**
   * Gets the favorite documents.
   *
   * @param userId the userId
   * @param limit the limit
   * @return the favorite documents
   */
  public List<Document> getFavoriteDocuments(String userId, int limit) throws Exception;
  
  /**
   * Gets the shared documents.
   *
   * @param userId the userId
   * @param limit the limit
   * @return the shared documents
   */
  public List<Document> getSharedDocuments(String userId, int limit) throws Exception;
  
  /**
   * Gets the recent documents.
   *
   * @param userId the userId
   * @param limit the limit
   * @return the recent documents
   */
  public List<Document> getRecentDocuments(String userId, int limit) throws Exception;
  
  /**
   * NewDocumentTypesConfig contains all registered template configs for specified provider.
   */
  public static class DocumentTemplatesConfig {

    /** The document templates. */
    protected List<NewDocumentTemplateConfig> templates;

    /** The providerName. */
    protected String                 providerName;

    /**
     * Gets the document templates.
     *
     * @return the document types
     */
    public List<NewDocumentTemplateConfig> getTemplates() {
      return templates;
    }

    /**
     * Sets the document templates.
     *
     * @param templates the new templates
     */
    public void setTemplates(List<NewDocumentTemplateConfig> templates) {
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
