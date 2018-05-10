/*
 * Copyright (C) 2003-2018 eXo Platform SAS.
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
   * Gets the ID of local file.
   *
   * @param fileNode the file node
   * @return the id
   * @throws RepositoryException the repository exception
   */
  String getId(Node fileNode) throws RepositoryException;

  /**
   * Gets the title of local file.
   *
   * @param fileNode the file node
   * @return the title
   * @throws RepositoryException the repository exception
   */
  String getTitle(Node fileNode) throws RepositoryException;

  /**
   * Gets the parent ID of local file.
   *
   * @param fileNode the file node
   * @return the parent id
   * @throws RepositoryException the repository exception
   */
  String getParentId(Node fileNode) throws RepositoryException;

  /**
   * Gets the created date of local file.
   *
   * @param fileNode the file node
   * @return the created
   * @throws RepositoryException the repository exception
   */
  Calendar getCreated(Node fileNode) throws RepositoryException;

  /**
   * Gets the modified date of local file.
   *
   * @param fileNode the file node
   * @return the modified
   * @throws RepositoryException the repository exception
   */
  Calendar getModified(Node fileNode) throws RepositoryException;

  /**
   * Gets the author of local file.
   *
   * @param fileNode the file node
   * @return the author
   * @throws RepositoryException the repository exception
   */
  String getAuthor(Node fileNode) throws RepositoryException;

  /**
   * Gets the last user of local file.
   *
   * @param fileNode the file node
   * @return the last user
   * @throws RepositoryException the repository exception
   */
  String getLastUser(Node fileNode) throws RepositoryException;

  /**
   * Gets the type of local file node.
   *
   * @param fileNode the file node
   * @return the type
   * @throws RepositoryException the repository exception
   */
  String getType(Node fileNode) throws RepositoryException;

  /**
   * Find IDs of local file parents.
   *
   * @param id the id
   * @return the collection
   * @throws DriveRemovedException the drive removed exception
   * @throws RepositoryException the repository exception
   */
  Collection<String> findParents(String id) throws DriveRemovedException, RepositoryException;

  /**
   * Checks if is a drive node.
   *
   * @param node the node
   * @return true, if is drive
   * @throws RepositoryException the repository exception
   */
  boolean isDrive(Node node) throws RepositoryException;

  /**
   * Checks if is local folder node.
   *
   * @param node the node
   * @return true, if is folder
   * @throws RepositoryException the repository exception
   */
  boolean isFolder(Node node) throws RepositoryException;

  /**
   * Checks if is local file node.
   *
   * @param node the node
   * @return true, if is file
   * @throws RepositoryException the repository exception
   */
  boolean isFile(Node node) throws RepositoryException;

  /**
   * Checks if is a local file resource.
   *
   * @param node the node
   * @return true, if is file resource
   * @throws RepositoryException the repository exception
   */
  boolean isFileResource(Node node) throws RepositoryException;

  /**
   * Checks if is local file node ignored.
   *
   * @param node the node
   * @return true, if is ignored
   * @throws RepositoryException the repository exception
   */
  boolean isIgnored(Node node) throws RepositoryException;

  /**
   * Mark given local file node as ignored.
   *
   * @param node {@link Node}
   * @return boolean <code>true</code> if file was ignored, <code>false</code> if it is already ignored
   * @throws RepositoryException the repository exception
   */
  boolean ignore(Node node) throws RepositoryException;

  /**
   * Remove ignorance mark on given local file node.
   *
   * @param node {@link Node}
   * @return boolean <code>true</code> if file unignored, <code>false</code> if file not ignored
   * @throws RepositoryException the repository exception
   */
  boolean unignore(Node node) throws RepositoryException;

  /**
   * Removes the file on cloud side.
   *
   * @param id the id
   * @return true, if successful
   * @throws CloudDriveException the cloud drive exception
   * @throws RepositoryException the repository exception
   */
  boolean removeFile(String id) throws CloudDriveException, RepositoryException;

  /**
   * Removes the folder on cloud side.
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
   * Trash file on cloud side.
   *
   * @param id the id
   * @return true, if successful
   * @throws CloudDriveException the cloud drive exception
   * @throws RepositoryException the repository exception
   */
  boolean trashFile(String id) throws CloudDriveException, RepositoryException;

  /**
   * Trash folder on cloud side.
   *
   * @param id the id
   * @return true, if successful
   * @throws CloudDriveException the cloud drive exception
   * @throws RepositoryException the repository exception
   */
  boolean trashFolder(String id) throws CloudDriveException, RepositoryException;

  /**
   * Untrash file on cloud side.
   *
   * @param fileNode the file node
   * @return the cloud file
   * @throws CloudDriveException the cloud drive exception
   * @throws RepositoryException the repository exception
   */
  CloudFile untrashFile(Node fileNode) throws CloudDriveException, RepositoryException;

  /**
   * Untrash folder on cloud side. It may untrash folder children recursive (depending on a provider
   * capabilities).
   *
   * @param fileNode the file node
   * @return the cloud file
   * @throws CloudDriveException the cloud drive exception
   * @throws RepositoryException the repository exception
   */
  CloudFile untrashFolder(Node fileNode) throws CloudDriveException, RepositoryException;

  /**
   * Creates the file on cloud side.
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
  CloudFile createFile(Node fileNode, Calendar created, Calendar modified, String mimeType, InputStream content) throws CloudDriveException,
                                                                                                                 RepositoryException;

  /**
   * Creates the folder on cloud side.
   *
   * @param folderNode the folder node
   * @param created the created
   * @return the cloud file
   * @throws CloudDriveException the cloud drive exception
   * @throws RepositoryException the repository exception
   */
  CloudFile createFolder(Node folderNode, Calendar created) throws CloudDriveException, RepositoryException;

  /**
   * Update folder on cloud side.
   *
   * @param folderNode the folder node
   * @param modified the modified
   * @return the cloud file
   * @throws CloudDriveException the cloud drive exception
   * @throws RepositoryException the repository exception
   */
  CloudFile updateFolder(Node folderNode, Calendar modified) throws CloudDriveException, RepositoryException;

  /**
   * Update file on cloud side.
   *
   * @param fileNode the file node
   * @param modified the modified
   * @return the cloud file
   * @throws CloudDriveException the cloud drive exception
   * @throws RepositoryException the repository exception
   */
  CloudFile updateFile(Node fileNode, Calendar modified) throws CloudDriveException, RepositoryException;

  /**
   * Update file content on cloud side.
   *
   * @param fileNode the file node
   * @param modified the modified
   * @param mimeType the mime type
   * @param content the content
   * @return the cloud file
   * @throws CloudDriveException the cloud drive exception
   * @throws RepositoryException the repository exception
   */
  CloudFile updateFileContent(Node fileNode, Calendar modified, String mimeType, InputStream content) throws CloudDriveException, RepositoryException;

  /**
   * Copy file on cloud side.
   *
   * @param srcFileNode the src file node
   * @param destFileNode the dest file node
   * @return the cloud file
   * @throws CloudDriveException the cloud drive exception
   * @throws RepositoryException the repository exception
   */
  CloudFile copyFile(Node srcFileNode, Node destFileNode) throws CloudDriveException, RepositoryException;

  /**
   * Copy folder on cloud side.
   *
   * @param srcFolderNode the src folder node
   * @param destFolderNode the dest folder node
   * @return the cloud file
   * @throws CloudDriveException the cloud drive exception
   * @throws RepositoryException the repository exception
   */
  CloudFile copyFolder(Node srcFolderNode, Node destFolderNode) throws CloudDriveException, RepositoryException;

  /**
   * Restore the file from cloud side. If file exists remotely it should be restored locally at right location
   * and its {@link CloudFile} returned, otherwise locally existing file(s) should be removed and
   * <code>null</code> returned.
   * <br>
   * This method will be used by synchronization of local-to-remote changes in case of a failure and need to
   * restore the state from cloud side. Method should care about removal of all file duplicates if this
   * may have a place during the sync.
   *
   * @param id the id
   * @param path the path
   * @return the cloud file of a restore item or <code>null</code> if it was removed
   * @throws CloudDriveException the cloud drive exception
   * @throws RepositoryException the repository exception
   */
  CloudFile restore(String id, String path) throws CloudDriveException, RepositoryException;

}
