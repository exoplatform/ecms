
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

import javax.jcr.Node;
import javax.jcr.RepositoryException;

/**
 * Security operations applicable to {@link CloudDrive} instance. <br>
 * 
 * Created by The eXo Platform SAS
 * 
 * @author <a href="mailto:pnedonosko@exoplatform.com">Peter Nedonosko</a>
 * @version $Id: CloudDriveSecurity.java 00000 Aug 11, 2015 pnedonosko $
 * 
 */
public interface CloudDriveSecurity {

  /**
   * Tells if sharing in cloud provider API supported.<br>
   * 
   * @return <code>true</code> if sharing supported, <code>false</code> otherwise
   */
  boolean isSharingSupported();

  /**
   * Share cloud file in cloud provider API. If sharing not supported {@link CloudDriveException} will be
   * thrown.<br>
   *
   * @param fileNode {@link Node} file node
   * @param identities array of {@link String} with organization identities in eXo Platform
   * @throws RepositoryException the repository exception
   * @throws CloudDriveException the cloud drive exception
   * @see #isSharingSupported()
   */
  void shareFile(Node fileNode, String... identities) throws RepositoryException, CloudDriveException;

}
