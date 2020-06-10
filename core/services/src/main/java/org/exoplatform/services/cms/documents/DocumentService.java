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

import java.util.LinkedHashMap;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.exoplatform.container.component.ComponentPlugin;
import org.exoplatform.services.cms.documents.exception.DocumentEditorProviderNotFoundException;
import org.exoplatform.services.cms.documents.model.Document;
import org.exoplatform.services.cms.drives.DriveData;

/**
 * DMS DocumentService interface. Created by The eXo Platform SAS Author : eXoPlatform exo@exoplatform.com Mar 22, 2011
 */
public interface DocumentService {

  /**
   * Find doc by ID.
   *
   * @param id the id
   * @return the document
   * @throws RepositoryException the repository exception
   */
  Document findDocById(String id) throws RepositoryException;

  /**
   * Get the short link to display a document in the Documents app by its id.
   *
   * @param workspaceName The workspace of the node
   * @param nodeId The id of the node
   * @return The link to open the document
   * @throws Exception the exception
   */
  String getShortLinkInDocumentsApp(String workspaceName, String nodeId) throws Exception;

  /**
   * return the URL of the shared document in the Shared Personal Documents folder of the user destination.
   *
   * @param currentNode the current node
   * @param username the username
   * @return the document url in personal documents
   * @throws Exception the exception
   */
  default String getDocumentUrlInPersonalDocuments(Node currentNode, String username) throws Exception {
    return null;
  }

  /**
   * Return the URL of the shared document in the Shared Documents folder of the space destination.
   *
   * @param currentNode the current node
   * @param spaceId the space id
   * @return the document url in space documents
   * @throws Exception the exception
   */
  default String getDocumentUrlInSpaceDocuments(Node currentNode, String spaceId) throws Exception {
    return null;
  }

  /**
   * Get the link to display a document in the Documents app. It will try to get the best matching context (personal doc, space
   * doc, ...).
   *
   * @param nodePath The path of the node
   * @return The link to open the document
   * @throws Exception the exception
   */
  String getLinkInDocumentsApp(String nodePath) throws Exception;

  /**
   * Get the link to display a document in the Documents app in the given drive. It will try to get the best matching context
   * (personal doc, space doc, ...).
   *
   * @param nodePath The path of the node
   * @param drive The drive to use
   * @return The link to open the document
   * @throws Exception the exception
   */
  String getLinkInDocumentsApp(String nodePath, DriveData drive) throws Exception;

  /**
   * Get the drive containing the node with the given node path, for the current user. If several drives contain the node, try to
   * find the best matching.
   *
   * @param nodePath The path of the node
   * @return The drive containing the node
   * @throws Exception the exception
   */
  DriveData getDriveOfNode(String nodePath) throws Exception;

  /**
   * Get the drive containing the node with the given node path. If several drives contain the node, try to find the best
   * matching.
   *
   * @param nodePath The path of the node
   * @param userId The user id
   * @param memberships The user memberships
   * @return The drive containing the node
   * @throws Exception the exception
   */
  DriveData getDriveOfNode(String nodePath, String userId, List<String> memberships) throws Exception;

  /**
   * Get the file preview breadCrumb.
   *
   * @param fileNode The file node
   * @return The file preview breadCrumb
   * @throws Exception the exception
   */
  default LinkedHashMap<String, String> getFilePreviewBreadCrumb(Node fileNode) throws Exception {
    throw new UnsupportedOperationException();
  }

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
   * Creates the document from template.
   *
   * @param currentNode the current node
   * @param title the title
   * @param template the template
   * @return the node
   * @throws Exception the exception
   */
  Node createDocumentFromTemplate(Node currentNode, String title, NewDocumentTemplate template) throws Exception;

  /**
   * Gets the registered template providers.
   *
   * @return the registered template providers
   */
  List<NewDocumentTemplateProvider> getNewDocumentTemplateProviders();

  /**
   * Registers document metadata plugin.
   * 
   * @param plugin the ComponentPlugin
   */
  void addDocumentMetadataPlugin(ComponentPlugin plugin);

  /**
   * Gets preferred editor provider for specified file and user.
   *
   * @param userId the userId
   * @param uuid the uuid
   * @param workspace the workspace
   * @return the preferred editor (provider)
   * @throws RepositoryException the exception
   */
  String getPreferredEditor(String userId, String uuid, String workspace) throws RepositoryException;

  /**
   * Sets preferred editor provider for specified file and user.
   *
   * @param userId the userId
   * @param provider the editor provider
   * @param uuid the uuid
   * @param workspace the workspace
   * @throws RepositoryException the exception
   */
  void savePreferredEditor(String userId, String provider, String uuid, String workspace) throws RepositoryException;

  /**
   * Gets the editor providers.
   *
   * @return the editor providers
   */
  List<DocumentEditorProvider> getDocumentEditorProviders();

  /**
   * Updates information about current open editor for the document. It is assumed that the document is being edited at the
   * moment.
   *
   * @param uuid the uuid
   * @param workspace the workspace
   * @param provider the current provider or null if the document has been closed.
   * @throws RepositoryException the exception
   */
 public void saveCurrentDocumentProvider(String uuid, String workspace, String provider) throws RepositoryException;


  /**
   * Gets current opened document editor.
   *
   * @param uuid the uuid
   * @param workspace the workspace
   * @return the current document provider
   * @throws RepositoryException the exception
   */
  String getCurrentDocumentProvider(String uuid, String workspace) throws RepositoryException;

  /**
   * Gets the editor provider.
   *
   * @param provider the provider
   * @return the editor provider
   * @throws DocumentEditorProviderNotFoundException the document editor provider not found exception
   */
  DocumentEditorProvider getEditorProvider(String provider) throws DocumentEditorProviderNotFoundException;

  /**
   * Gets the documents by folder.
   *
   * @param folder the folder
   * @param condition the condition
   * @param limit the limit
   * @return the documents by folder
   * @throws Exception the exception
   */
  default List<Document> getDocumentsByFolder(String folder, String condition, long limit) throws Exception {
    throw new UnsupportedOperationException();
  }

  /**
   * Gets the documents by query.
   *
   * @param query the query
   * @param limit the limit
   * @return the documents by query
   * @throws Exception the exception
   */
  default List<Document> getDocumentsByQuery(String query, long limit) throws Exception {
    throw new UnsupportedOperationException();
  }

  /**
   * Gets the favorite documents.
   *
   * @param userId the userId
   * @param limit the limit
   * @return the favorite documents
   * @throws Exception the exception
   */
  default List<Document> getFavoriteDocuments(String userId, int limit) throws Exception {
    throw new UnsupportedOperationException();
  }

  /**
   * Gets the shared documents.
   *
   * @param userId the userId
   * @param limit the limit
   * @return the shared documents
   * @throws Exception the exception
   */
  default List<Document> getSharedDocuments(String userId, int limit) throws Exception {
    throw new UnsupportedOperationException();
  }

  /**
   * Gets the recent spaces documents.
   *
   * @param limit the limit
   * @return the recent spaces documents
   * @throws Exception the exception
   */
  default List<Document> getRecentSpacesDocuments(int limit) throws Exception {
    throw new UnsupportedOperationException();
  }

  // TODO consider do we need this in this API level service
//  /**
//   * Gets the user public node (shared to all platform users). The node will be acquired in system session.
//   *
//   * @param userId the user ID in organization service
//   * @return the user public node in system session
//   * @throws Exception the exception
//   */
//  Node getUserPublicNode(String userId) throws Exception;
//
//  /**
//   * Gets the user profile node (a node where /Private and /Public nodes live). The node will be acquired in system session.
//   *
//   * @param userId the user ID in organization service
//   * @return the user profile node in system session
//   * @throws Exception the exception
//   */
//  Node getUserProfileNode(String userId) throws Exception;

  /**
   * NewDocumentTypesConfig contains all registered template configs for specified provider.
   */
  static class DocumentTemplatesConfig {

    /** The document templates. */
    protected List<NewDocumentTemplateConfig> templates;

    /** The providerName. */
    protected String                          providerName;

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
     * Sets the provider name.
     *
     * @param providerName the new provider name
     */
    public void setProviderName(String providerName) {
      this.providerName = providerName;
    }
  }

}
