/*
 * Copyright (C) 2003-2014 eXo Platform SAS.
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

  String getId(Node fileNode) throws RepositoryException;

  String getTitle(Node fileNode) throws RepositoryException;

  String getParentId(Node fileNode) throws RepositoryException;

  Calendar getCreated(Node fileNode) throws RepositoryException;

  Calendar getModified(Node fileNode) throws RepositoryException;
  
  String getType(Node fileNode) throws RepositoryException;

  Collection<String> findParents(String id) throws DriveRemovedException, RepositoryException;

  boolean isDrive(Node node) throws RepositoryException;

  boolean isFolder(Node node) throws RepositoryException;

  boolean isFile(Node node) throws RepositoryException;

  boolean isFileResource(Node node) throws RepositoryException;

  boolean isIgnored(Node node) throws RepositoryException;

  /**
   * Mark given file as ignored.
   * 
   * @param node {@link Node}
   * @return boolean <code>true</code> if file was ignored, <code>false</code> if it is already ignored
   * @throws RepositoryException
   */
  boolean ignore(Node node) throws RepositoryException;

  /**
   * Remove ignorance mark on given file.
   * 
   * @param node {@link Node}
   * @return boolean <code>true</code> if file unignored, <code>false</code> if file not ignored
   * @throws RepositoryException
   */
  boolean unignore(Node node) throws RepositoryException;

  boolean removeFile(String id) throws CloudDriveException, RepositoryException;

  boolean removeFolder(String id) throws CloudDriveException, RepositoryException;

  boolean isTrashSupported();

  boolean trashFile(String id) throws CloudDriveException, RepositoryException;

  boolean trashFolder(String id) throws CloudDriveException, RepositoryException;

  CloudFile untrashFile(Node fileNode) throws CloudDriveException, RepositoryException;

  CloudFile untrashFolder(Node fileNode) throws CloudDriveException, RepositoryException;

  CloudFile createFile(Node fileNode,
                       Calendar created,
                       Calendar modified,
                       String mimeType,
                       InputStream content) throws CloudDriveException, RepositoryException;

  CloudFile createFolder(Node folderNode, Calendar created) throws CloudDriveException, RepositoryException;

  CloudFile updateFolder(Node folderNode, Calendar modified) throws CloudDriveException, RepositoryException;

  CloudFile updateFile(Node fileNode, Calendar modified) throws CloudDriveException, RepositoryException;

  CloudFile updateFileContent(Node fileNode, Calendar modified, String mimeType, InputStream content)
                                                                                                      throws CloudDriveException,
                                                                                                      RepositoryException;

  CloudFile copyFile(Node srcFileNode, Node destFileNode) throws CloudDriveException, RepositoryException;

  CloudFile copyFolder(Node srcFolderNode, Node destFolderNode) throws CloudDriveException, RepositoryException;

  CloudFile restore(String id, String path) throws NotFoundException, CloudDriveException, RepositoryException;

}
