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
package org.exoplatform.services.cms.clouddrives.jcr;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.exoplatform.services.cms.clouddrives.CloudDriveException;
import org.exoplatform.services.cms.clouddrives.CloudFile;
import org.exoplatform.services.cms.clouddrives.CloudFileAPI;
import org.exoplatform.services.cms.clouddrives.CloudFileSynchronizer;
import org.exoplatform.services.cms.clouddrives.SkipSyncException;
import org.exoplatform.services.cms.clouddrives.SyncNotSupportedException;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

/**
 * Synchronizer created to handle cases when stored class of
 * {@link CloudFileSynchronizer} cannot be loaded and file node doesn't exists.
 * This synchronizer does nothing but call {@link CloudFileAPI} methods for
 * {@link #remove(String, String, boolean, CloudFileAPI)} and
 * {@link #trash(String, String, boolean, CloudFileAPI)} invocations.<br>
 * Created by The eXo Platform SAS.
 * 
 * @author <a href="mailto:pnedonosko@exoplatform.com">Peter Nedonosko</a>
 * @version $Id: LostRemovalSynchronizer.java 00000 Apr 22, 2014 pnedonosko $
 */
public final class LostRemovalSynchronizer implements CloudFileSynchronizer {

  /** The Constant LOG. */
  protected static final Log LOG = ExoLogger.getLogger(LostRemovalSynchronizer.class);

  /**
   * Instantiates a new lost removal synchronizer.
   */
  public LostRemovalSynchronizer() {
  }

  /**
   * {@inheritDoc}
   */
  public String[] getSupportedNodetypes() {
    return new String[] {};
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean accept(Node file) throws RepositoryException, SkipSyncException {
    return false;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean remove(String filePath, String fileId, boolean isFolder, CloudFileAPI api) throws CloudDriveException,
                                                                                            RepositoryException {
    if (isFolder) {
      api.removeFolder(fileId);
    } else {
      api.removeFile(fileId);
    }
    return true;
  }

  /**
   * {@inheritDoc}
   */
  public boolean trash(String filePath, String fileId, boolean isFolder, CloudFileAPI api) throws RepositoryException,
                                                                                           CloudDriveException {
    if (isFolder) {
      return api.trashFolder(fileId);
    } else {
      return api.trashFile(fileId);
    }
  }

  /**
   * {@inheritDoc}
   */
  public CloudFile create(Node file, CloudFileAPI api) throws RepositoryException, CloudDriveException {
    throw new SyncNotSupportedException("Not supported");
  }

  /**
   * {@inheritDoc}
   */
  public CloudFile untrash(Node file, CloudFileAPI api) throws RepositoryException, CloudDriveException {
    throw new SyncNotSupportedException("Not supported");
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public CloudFile update(Node file, CloudFileAPI api) throws CloudDriveException, RepositoryException {
    throw new SyncNotSupportedException("Not supported");
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public CloudFile updateContent(Node file, CloudFileAPI api) throws CloudDriveException, RepositoryException {
    throw new SyncNotSupportedException("Not supported");
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public CloudFile copy(Node srcFile, Node destFile, CloudFileAPI api) throws CloudDriveException, RepositoryException {
    throw new SyncNotSupportedException("Not supported");
  }

}
