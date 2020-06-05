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
 * Integration helper for Cloud Drive file operations support.<br>
 * Created by The eXo Platform SAS
 * 
 * @author <a href="mailto:pnedonosko@exoplatform.com">Peter Nedonosko</a>
 * @version $Id: CloudDriveManager.java 00000 May 15, 2014 pnedonosko $
 */
public class CloudDriveManager {

  /** The drive. */
  private final CloudDrive drive;

  /**
   * Instantiates a new cloud drive manager.
   *
   * @param drive the drive
   */
  public CloudDriveManager(CloudDrive drive) {
    this.drive = drive;
  }

  /**
   * Inits the copy.
   *
   * @param srcNode the src node
   * @param destNode the dest node
   * @throws CloudDriveException the cloud drive exception
   * @throws RepositoryException the repository exception
   */
  public void initCopy(Node srcNode, Node destNode) throws CloudDriveException, RepositoryException {
    drive.initCopy(srcNode, destNode);
  }

  /**
   * Inits the remove.
   *
   * @param file the file
   * @throws CloudDriveException the cloud drive exception
   * @throws RepositoryException the repository exception
   */
  public void initRemove(Node file) throws CloudDriveException, RepositoryException {
    drive.initRemove(file);
  }

}
