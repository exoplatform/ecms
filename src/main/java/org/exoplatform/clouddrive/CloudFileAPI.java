/*
 * Copyright (C) 2003-2016 eXo Platform SAS.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.exoplatform.clouddrive;

import java.io.InputStream;
import java.util.Calendar;
import java.util.Collection;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

/**
 * An API to synchronize cloud files with its state on provider side. This API is a part of Connector API.<br>
 * 
 * Created by The eXo Platform SAS.
 * 
 * @author <a href="mailto:pnedonosko@exoplatform.com">Peter Nedonosko</a>
 * @version $Id: CloudFileAPI.java 00000 Mar 21, 2014 pnedonosko $
 * 
 */
public interface CloudFileAPI {

  /**
   * Gets the id.
   *
   * @param fileNode the file node
   * @return the id
   * @throws RepositoryException the repository exception
   */
  String getId(Node fileNode) throws RepositoryException;

  /**
   * Gets the title.
   *
   * @param fileNode the file node
   * @return the title
   * @throws RepositoryException the repository exception
   */
  String getTitle(Node fileNode) throws RepositoryException;

  /**
   * Gets the parent id.
   *
   * @param fileNode the file node
   * @return the parent id
   * @throws RepositoryException the repository exception
   */
  String getParentId(Node fileNode) throws RepositoryException;

  /**
   * Gets the created.
   *
   * @param fileNode the file node
   * @return the created
   * @throws RepositoryException the repository exception
   */
  Calendar getCreated(Node fileNode) throws RepositoryException;

  /**
   * Gets the modified.
   *
   * @param fileNode the file node
   * @return the modified
   * @throws RepositoryException the repository exception
   */
  Calendar getModified(Node fileNode) throws RepositoryException;
  
  /**
   * Gets the type.
   *
   * @param fileNode the file node
   * @return the type
   * @throws RepositoryException the repository exception
   */
  String getType(Node fileNode) throws RepositoryException;

  /**
   * Find parents.
   *
   * @param id the id
   * @return the collection
   * @throws DriveRemovedException the drive removed exception
   * @throws RepositoryException the repository exception
   */
  Collection<String> findParents(String id) throws DriveRemovedException, RepositoryException;

  /**
   * Checks if is drive.
   *
   * @param node the node
   * @return true, if is drive
   * @throws RepositoryException the repository exception
   */
  boolean isDrive(Node node) throws RepositoryException;

  /**
   * Checks if is folder.
   *
   * @param node the node
   * @return true, if is folder
   * @throws RepositoryException the repository exception
   */
  boolean isFolder(Node node) throws RepositoryException;

  /**
   * Checks if is file.
   *
   * @param node the node
   * @return true, if is file
   * @throws RepositoryException the repository exception
   */
  boolean isFile(Node node) throws RepositoryException;

  /**
   * Checks if is file resource.
   *
   * @param node the node
   * @return true, if is file resource
   * @throws RepositoryException the repository exception
   */
  boolean isFileResource(Node node) throws RepositoryException;

  /**
   * Checks if is ignored.
   *
   * @param node the node
   * @return true, if is ignored
   * @throws RepositoryException the repository exception
   */
  boolean isIgnored(Node node) throws RepositoryException;

  /**
   * Mark given file as ignored.
   *
   * @param node {@link Node}
   * @return boolean <code>true</code> if file was ignored, <code>false</code> if it is already ignored
   * @throws RepositoryException the repository exception
   */
  boolean ignore(Node node) throws RepositoryException;

  /**
   * Remove ignorance mark on given file.
   *
   * @param node {@link Node}
   * @return boolean <code>true</code> if file unignored, <code>false</code> if file not ignored
   * @throws RepositoryException the repository exception
   */
  boolean unignore(Node node) throws RepositoryException;

  /**
   * Removes the file.
   *
   * @param id the id
   * @return true, if successful
   * @throws CloudDriveException the cloud drive exception
   * @throws RepositoryException the repository exception
   */
  boolean removeFile(String id) throws CloudDriveException, RepositoryException;

  /**
   * Removes the folder.
   *
   * @param id the id
   * @return true, if successful
   * @throws CloudDriveException the cloud drive exception
   * @throws RepositoryException the repository exception
   */
  boolean removeFolder(String id) throws CloudDriveException, RepositoryException;

  /**
   * Checks if is trash supported.
   *
   * @return true, if is trash supported
   */
  boolean isTrashSupported();

  /**
   * Trash file.
   *
   * @param id the id
   * @return true, if successful
   * @throws CloudDriveException the cloud drive exception
   * @throws RepositoryException the repository exception
   */
  boolean trashFile(String id) throws CloudDriveException, RepositoryException;

  /**
   * Trash folder.
   *
   * @param id the id
   * @return true, if successful
   * @throws CloudDriveException the cloud drive exception
   * @throws RepositoryException the repository exception
   */
  boolean trashFolder(String id) throws CloudDriveException, RepositoryException;

  /**
   * Untrash file.
   *
   * @param fileNode the file node
   * @return the cloud file
   * @throws CloudDriveException the cloud drive exception
   * @throws RepositoryException the repository exception
   */
  CloudFile untrashFile(Node fileNode) throws CloudDriveException, RepositoryException;

  /**
   * Untrash folder.
   *
   * @param fileNode the file node
   * @return the cloud file
   * @throws CloudDriveException the cloud drive exception
   * @throws RepositoryException the repository exception
   */
  CloudFile untrashFolder(Node fileNode) throws CloudDriveException, RepositoryException;

  /**
   * Creates the file.
   *
   * @param fileNode the file node
   * @param created the created
   * @param modified the modified
   * @param mimeType the mime type
   * @param content the content
   * @return the cloud file
   * @throws CloudDriveException the cloud drive exception
   * @throws RepositoryException the repository exception
   */
  CloudFile createFile(Node fileNode,
                       Calendar created,
                       Calendar modified,
                       String mimeType,
                       InputStream content) throws CloudDriveException, RepositoryException;

  /**
   * Creates the folder.
   *
   * @param folderNode the folder node
   * @param created the created
   * @return the cloud file
   * @throws CloudDriveException the cloud drive exception
   * @throws RepositoryException the repository exception
   */
  CloudFile createFolder(Node folderNode, Calendar created) throws CloudDriveException, RepositoryException;

  /**
   * Update folder.
   *
   * @param folderNode the folder node
   * @param modified the modified
   * @return the cloud file
   * @throws CloudDriveException the cloud drive exception
   * @throws RepositoryException the repository exception
   */
  CloudFile updateFolder(Node folderNode, Calendar modified) throws CloudDriveException, RepositoryException;

  /**
   * Update file.
   *
   * @param fileNode the file node
   * @param modified the modified
   * @return the cloud file
   * @throws CloudDriveException the cloud drive exception
   * @throws RepositoryException the repository exception
   */
  CloudFile updateFile(Node fileNode, Calendar modified) throws CloudDriveException, RepositoryException;

  /**
   * Update file content.
   *
   * @param fileNode the file node
   * @param modified the modified
   * @param mimeType the mime type
   * @param content the content
   * @return the cloud file
   * @throws CloudDriveException the cloud drive exception
   * @throws RepositoryException the repository exception
   */
  CloudFile updateFileContent(Node fileNode, Calendar modified, String mimeType, InputStream content)
                                                                                                      throws CloudDriveException,
                                                                                                      RepositoryException;

  /**
   * Copy file.
   *
   * @param srcFileNode the src file node
   * @param destFileNode the dest file node
   * @return the cloud file
   * @throws CloudDriveException the cloud drive exception
   * @throws RepositoryException the repository exception
   */
  CloudFile copyFile(Node srcFileNode, Node destFileNode) throws CloudDriveException, RepositoryException;

  /**
   * Copy folder.
   *
   * @param srcFolderNode the src folder node
   * @param destFolderNode the dest folder node
   * @return the cloud file
   * @throws CloudDriveException the cloud drive exception
   * @throws RepositoryException the repository exception
   */
  CloudFile copyFolder(Node srcFolderNode, Node destFolderNode) throws CloudDriveException, RepositoryException;

  /**
   * Restore.
   *
   * @param id the id
   * @param path the path
   * @return the cloud file
   * @throws NotFoundException the not found exception
   * @throws CloudDriveException the cloud drive exception
   * @throws RepositoryException the repository exception
   */
  CloudFile restore(String id, String path) throws NotFoundException, CloudDriveException, RepositoryException;

}
