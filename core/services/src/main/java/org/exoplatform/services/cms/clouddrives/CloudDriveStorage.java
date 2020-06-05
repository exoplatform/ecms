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

import java.util.concurrent.ExecutionException;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.exoplatform.services.cms.clouddrives.CloudDrive.Command;
import org.exoplatform.services.cms.clouddrives.viewer.ContentReader;

/**
 * Cloud Drive local storage level operations: access internal data and
 * modification.<br>
 * Created by The eXo Platform SAS
 * 
 * @author <a href="mailto:pnedonosko@exoplatform.com">Peter Nedonosko</a>
 * @version $Id: CloudDriveStorage.java 00000 Dec 16, 2014 pnedonosko $
 */
public interface CloudDriveStorage {

  /**
   * Change to Cloud Drive storage.
   *
   * @param <R> the generic type
   */
  interface Change<R> {

    /**
     * Apply this change in the Cloud Drive storage.
     *
     * @return resulting object or <code>null</code>
     * @throws RepositoryException the repository exception
     * @throws NotCloudDriveException the not cloud drive exception
     * @throws DriveRemovedException the drive removed exception
     * @throws CloudDriveException the cloud drive exception
     */
    R apply() throws RepositoryException, NotCloudDriveException, DriveRemovedException, CloudDriveException;
  }

  /**
   * Tell if given node is a local node in this cloud drive, thus it is not a
   * cloud drive, not a cloud file, it is not already ignored local node and it
   * is not currently updating node by the drive (not uploading to remote site).
   *
   * @param node {@link Node}
   * @return boolean <code>true</code> if node is local only in this drive,
   *         <code>false</code> otherwise
   * @throws RepositoryException the repository exception
   * @throws DriveRemovedException the drive removed exception
   */
  public boolean isLocal(Node node) throws RepositoryException, DriveRemovedException;

  /**
   * Tell if given node is ignored in this cloud drive.
   *
   * @param node {@link Node}
   * @return boolean <code>true</code> if node is ignored, <code>false</code>
   *         otherwise
   * @throws RepositoryException the repository exception
   * @throws NotCloudDriveException if given node doesn't belong to cloud drive
   * @throws NotCloudFileException if given node is a root folder of the drive
   * @throws DriveRemovedException if drive removed
   */
  boolean isIgnored(Node node) throws RepositoryException, NotCloudDriveException, NotCloudFileException, DriveRemovedException;

  /**
   * Mark given file node as ignored. This operation doesn't remove local or
   * remote file. This operation saves the node to persist its ignored state.
   *
   * @param node {@link Node}
   * @return boolean <code>true</code> if file was ignored, <code>false</code>
   *         if it is already ignored
   * @throws RepositoryException the repository exception
   * @throws NotCloudDriveException if node doesn't belong to cloud drive
   * @throws NotCloudFileException if node belongs to cloud drive but not
   *           represent a cloud file (not yet added or ignored node) or node is
   *           a root folder of the drive
   * @throws DriveRemovedException if drive removed
   */
  boolean ignore(Node node) throws RepositoryException, NotCloudDriveException, NotCloudFileException, DriveRemovedException;

  /**
   * Remove ignorance mark if given node marked as ignored. This operation
   * doesn't remove local or remote file. This operation saves the node to
   * persist its unignored state.
   *
   * @param node {@link Node}
   * @return boolean <code>true</code> if file was unignored successfully,
   *         <code>false</code> if it is already ignored
   * @throws RepositoryException the repository exception
   * @throws NotCloudDriveException if node doesn't belong to cloud drive
   * @throws DriveRemovedException if drive removed
   * @throws NotCloudFileException if node belongs to cloud drive but not
   *           represent a cloud file (not yet added or ignored node) or node is
   *           a root folder of the drive
   */
  boolean unignore(Node node) throws RepositoryException, NotCloudDriveException, DriveRemovedException, NotCloudFileException;

  /**
   * Initiate cloud file creation from this node. If node already represents a
   * cloud file nothing will happen. This operation will have no effect also if
   * ignorance marker set on given node. Use {@link #unignore(Node)} to reset
   * this marker before calling this method. If it is was not a cloud file node,
   * file creation will run asynchronously in another thread. You may use
   * {@link CloudDriveListener} for informing about file creation results.
   * 
   * @param node {@link Node} a node under cloud drive folder
   * @return boolean, <code>true</code> if file creation initiated successfully,
   *         <code>false</code> if node already represents a cloud file
   * @throws RepositoryException if storage error occurred
   * @throws NotCloudDriveException if node doesn't belong to cloud drive folder
   * @throws DriveRemovedException if drive removed
   * @throws NotCloudFileException if given node is a root folder of the drive
   * @throws CloudDriveException if creation preparation failed
   * @see #unignore(Node)
   * @see #isIgnored(Node)
   */
  boolean create(Node node) throws RepositoryException,
                            NotCloudDriveException,
                            DriveRemovedException,
                            NotCloudFileException,
                            CloudDriveException;

  /**
   * Currently processing {@link Command} in the drive. It can be connect or
   * synchronization operation or an empty command if nothing active in the
   * moment. This method result changes in time. Its main purpose for precise
   * concurrency organization. E.g. when some file change is currently updating
   * (synchronizing) and need wait for the processing in the drive and then
   * continue the work. Use {@link Command#isDone()} method to consult if
   * operation completed or {@link Command#await()} to wait its completion.
   * 
   * @return {@link Command}
   */
  Command getCurentCommand();

  /**
   * Wait for currently processing drive operations. This method doesn't provide
   * accurate state when finished as a new command can be started asynchronously
   * at that moment. Use it for information purpose and in tests (when all
   * commands controllable).
   * 
   * @throws InterruptedException if waiting thread was interrupted
   * @throws ExecutionException if some command failed
   */
  void await() throws InterruptedException, ExecutionException;

  /**
   * Apply local change in this drive storage. Local change it is content
   * changes that <strong>will not be</strong> synchronized to the remote drive.
   * Use this method with care as creation of only local JCR nodes for example,
   * may lead to conflicts during the synchronization.
   *
   * @param <R> the generic type
   * @param change {@link Change} instance
   * @return resulting object or <code>null</code>
   * @throws NotCloudDriveException the not cloud drive exception
   * @throws DriveRemovedException the drive removed exception
   * @throws RepositoryException the repository exception
   * @throws CloudDriveException the cloud drive exception
   */
  <R> R localChange(Change<R> change) throws NotCloudDriveException,
                                      DriveRemovedException,
                                      RepositoryException,
                                      CloudDriveException;

  /**
   * Actual file content from provider side.
   *
   * @param fileId {@link String}
   * @return {@link ContentReader} or <code>null</code> if content cannot be
   *         obtained
   * @throws RepositoryException the repository exception
   * @throws CloudDriveException the cloud drive exception
   */
  ContentReader getFileContent(String fileId) throws RepositoryException, CloudDriveException;

  /**
   * File preview content from provider side or generated by the drive
   * implementation.
   *
   * @param fileId {@link String}
   * @return {@link ContentReader} or <code>null</code> if preview not available
   * @throws RepositoryException the repository exception
   * @throws CloudDriveException the cloud drive exception
   */
  ContentReader getFilePreview(String fileId) throws RepositoryException, CloudDriveException;

  //
  // /**
  // * File thumbnail content from provider side or generated by the drive
  // implementation.
  // *
  // * @return {@link ContentReader} or <code>null</code> if thumbnail not
  // available
  // * @throws RepositoryException
  // * @throws CloudDriveException
  // */
  // ContentReader getFileThumbnail() throws RepositoryException,
  // CloudDriveException;
}
