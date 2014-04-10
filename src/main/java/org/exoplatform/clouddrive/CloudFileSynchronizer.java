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

import com.sun.javadoc.ThrowsTag;

import java.util.List;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.nodetype.NodeType;

/**
 * Synchronize local to cloud file.
 * 
 * Created by The eXo Platform SAS
 * 
 * @author <a href="mailto:pnedonosko@exoplatform.com">Peter Nedonosko</a>
 * @version $Id: CloudFileSynchronizer.java 00000 Mar 21, 2014 pnedonosko $
 * 
 */
public interface CloudFileSynchronizer {

  /**
   * All nodetype names supported by the synchronizer.
   * 
   * @return array of {@link String}
   */
  String[] getSupportedNodetypes();
  
  /**
   * Check if given file can be accepted by this synchronizer.
   * 
   * @param file {@link Node}
   * @return boolean, <code>true</code> if given file can be handled by this synchronizer
   * @throws RepositoryException
   * @throws SkipSyncException if given node should be ignored by synchronization
   * @see #synchronize(Node, CloudFileAPI)
   */
  boolean accept(Node file) throws RepositoryException, SkipSyncException;

  /**
   * Synchronize local node with cloud using given API. This method doesn't check if the file can be handled
   * by
   * the synchronizer - use {@link #accept(Node)} to perform such check before calling the method.
   * 
   * @param file {@link Node}
   * @param api {@link CloudFileAPI}
   * @return boolean, <code>true</code> if file was successfully processed (e.g. created on the cloud),
   *         <code>false</code> otherwise.
   * @see #accept(Node)
   */
  @Deprecated
  boolean synchronize(Node file, CloudFileAPI api) throws CloudDriveException, RepositoryException;

  /**
   * Remove cloud file represented by given path and/or id using given API. This method doesn't check if
   * the file can be handled by the synchronizer - use {@link #accept(Node)} to perform such check before
   * calling the method.
   * 
   * @param filePath {@link String}
   * @param fileId {@link String}
   * @param api {@link CloudFileAPI}
   * @return boolean, <code>true</code> if file was successfully removed, <code>false</code> otherwise.
   * @see #accept(Node)
   */
  boolean remove(String filePath, String fileId, CloudFileAPI api) throws CloudDriveException,
                                                                  RepositoryException;

  boolean trash(String filePath, String fileId, CloudFileAPI api) throws CloudDriveException, RepositoryException;

  boolean untrash(Node file, CloudFileAPI api) throws CloudDriveException, RepositoryException;

  boolean create(Node file, CloudFileAPI api) throws CloudDriveException, RepositoryException;

  boolean update(Node file, CloudFileAPI api) throws CloudDriveException, RepositoryException;

  boolean updateContent(Node file, CloudFileAPI api) throws CloudDriveException, RepositoryException;
}
