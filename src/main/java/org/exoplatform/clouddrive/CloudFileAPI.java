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
 * An API to synchronize cloud files with its state on provider side.
 * 
 * Created by The eXo Platform SAS.
 * 
 * @author <a href="mailto:pnedonosko@exoplatform.com">Peter Nedonosko</a>
 * @version $Id: CloudFileAPI.java 00000 Mar 21, 2014 pnedonosko $
 * 
 */
public interface CloudFileAPI {

  String getId(Node fileNode) throws CloudDriveException, RepositoryException;

  String getTitle(Node fileNode) throws CloudDriveException, RepositoryException;

  String getParentId(Node fileNode) throws CloudDriveException, RepositoryException;

  Collection<String> findParents(Node fileNode) throws CloudDriveException, RepositoryException;

  String createFile(Node fileNode,
                    String description,
                    Calendar created,
                    Calendar modified,
                    String mimeType,
                    InputStream content) throws CloudDriveException, RepositoryException;

  String createFolder(Node folderNode, String description, Calendar created) throws CloudDriveException,
                                                                            RepositoryException;

  void updateFolder(Node folderNode, String description, Calendar modified) throws CloudDriveException,
                                                                           RepositoryException;

  void updateFile(Node fileNode, String description, Calendar modified) throws CloudDriveException,
                                                                       RepositoryException;

  void updateFileContent(Node fileNode,
                         String description,
                         Calendar modified,
                         String mimeType,
                         InputStream content) throws CloudDriveException, RepositoryException;

  void remove(String id) throws CloudDriveException, RepositoryException;

  boolean isTrashSupported();
  
  boolean trash(String id) throws CloudDriveException, RepositoryException;
  
  boolean untrash(Node fileNode) throws CloudDriveException, RepositoryException;

}
