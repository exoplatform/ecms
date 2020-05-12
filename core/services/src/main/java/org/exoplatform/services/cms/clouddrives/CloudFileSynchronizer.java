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
package org.exoplatform.services.cms.clouddrives;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

/**
 * Synchronize local to cloud file.<br>
 * Created by The eXo Platform SAS
 * 
 * @author <a href="mailto:pnedonosko@exoplatform.com">Peter Nedonosko</a>
 * @version $Id: CloudFileSynchronizer.java 00000 Mar 21, 2014 pnedonosko $
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
   * @return boolean, <code>true</code> if given file can be handled by this
   *         synchronizer
   * @throws RepositoryException if local storage error happened
   * @throws SkipSyncException if given node should be ignored by
   *           synchronization
   */
  boolean accept(Node file) throws RepositoryException, SkipSyncException;

  /**
   * Remove cloud file by given path and/or id using given API. This method
   * doesn't check if the file can be handled by the synchronizer - use
   * {@link CloudFileSynchronizer#accept(Node)} to perform such check before
   * calling the method.
   * 
   * @param path {@link String}
   * @param id {@link String}
   * @param isFolder {@link Boolean}
   * @param api {@link CloudFileAPI}
   * @return boolean, <code>true</code> if file was successfully removed,
   *         <code>false</code> otherwise.
   * @throws CloudDriveException in case of cloud side error (or restriction)
   * @throws RepositoryException if local storage error happened
   * @see #accept(Node)
   */
  boolean remove(String path, String id, boolean isFolder, CloudFileAPI api) throws CloudDriveException, RepositoryException;

  /**
   * Move cloud file to remote trash (if supported) by given path and/or id
   * using given API. This method doesn't check if the file can be handled by
   * the synchronizer - use {@link CloudFileSynchronizer#accept(Node)} to
   * perform such check before calling the method.
   * 
   * @param path {@link String}
   * @param id {@link String}
   * @param isFolder {@link Boolean}
   * @param api {@link CloudFileAPI}
   * @return boolean, <code>true</code> if file was successfully trashed,
   *         <code>false</code> otherwise.
   * @throws CloudDriveException in case of cloud side error (or restriction)
   * @throws RepositoryException if local storage error happened
   * @see #accept(Node)
   */
  boolean trash(String path, String id, boolean isFolder, CloudFileAPI api) throws CloudDriveException, RepositoryException;

  /**
   * Restore cloud file from remote trash (if supported) by given JCR node using
   * given API. This method doesn't check if the file can be handled by the
   * synchronizer - use {@link CloudFileSynchronizer#accept(Node)} to perform
   * such check before calling the method.
   * 
   * @param file {@link Node}
   * @param api {@link CloudFileAPI}
   * @return {@link CloudFile} restored file.
   * @throws CloudDriveException in case of cloud side error (or restriction)
   * @throws RepositoryException if local storage error happened
   * @see #accept(Node)
   */
  CloudFile untrash(Node file, CloudFileAPI api) throws CloudDriveException, RepositoryException;

  /**
   * Create cloud file remotely from given JCR node using given API. This method
   * doesn't check if the file can be handled by the synchronizer - use
   * {@link CloudFileSynchronizer#accept(Node)} to perform such check before
   * calling the method.
   * 
   * @param file {@link Node}
   * @param api {@link CloudFileAPI}
   * @return {@link CloudFile} if file was successfully created,
   *         <code>null</code> otherwise.
   * @throws CloudDriveException in case of cloud side error (or restriction)
   * @throws RepositoryException if local storage error happened
   * @see #accept(Node)
   */
  CloudFile create(Node file, CloudFileAPI api) throws CloudDriveException, RepositoryException;

  /**
   * Copy cloud file remotely from given source and destination JCR nodes using
   * given API. This method doesn't check if the file can be handled by the
   * synchronizer - use {@link CloudFileSynchronizer#accept(Node)} to perform
   * such check before calling the method.
   * 
   * @param srcFile {@link Node}
   * @param destFile {@link Node}
   * @param api {@link CloudFileAPI}
   * @return {@link CloudFile} created file.
   * @throws CloudDriveException in case of cloud side error (or restriction)
   * @throws RepositoryException if local storage error happened
   * @see #accept(Node)
   */
  CloudFile copy(Node srcFile, Node destFile, CloudFileAPI api) throws CloudDriveException, RepositoryException;

  /**
   * Update remote cloud file metadata from given JCR nodes using given API.
   * Under metadata it assumes: file name and/or parents. This method doesn't
   * check if the file can be handled by the synchronizer - use
   * {@link CloudFileSynchronizer#accept(Node)} to perform such check before
   * calling the method.
   * 
   * @param file {@link Node}
   * @param api {@link CloudFileAPI}
   * @return {@link CloudFile} updated file.
   * @throws CloudDriveException in case of cloud side error (or restriction)
   * @throws RepositoryException if local storage error happened
   * @see #accept(Node)
   */
  CloudFile update(Node file, CloudFileAPI api) throws CloudDriveException, RepositoryException;

  /**
   * Update remote cloud file content and optionally its metadata from given JCR
   * nodes using given API. Under metadata it assumes: file name and/or parents.
   * This method doesn't check if the file can be handled by the synchronizer -
   * use {@link CloudFileSynchronizer#accept(Node)} to perform such check before
   * calling the method.
   * 
   * @param file {@link Node}
   * @param api {@link CloudFileAPI}
   * @return {@link CloudFile} updated file.
   * @throws CloudDriveException in case of cloud side error (or restriction)
   * @throws RepositoryException if local storage error happened
   * @see #accept(Node)
   */
  CloudFile updateContent(Node file, CloudFileAPI api) throws CloudDriveException, RepositoryException;
}
