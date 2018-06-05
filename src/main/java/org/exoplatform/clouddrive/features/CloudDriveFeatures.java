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
package org.exoplatform.clouddrive.features;

import org.exoplatform.clouddrive.CannotCreateDriveException;
import org.exoplatform.clouddrive.CloudDrive;
import org.exoplatform.clouddrive.CloudProvider;

/**
 * Cloud Drive features specified as managed. It is a composite on top of
 * per-feature specifications registered as plugins. Each feature specification
 * will be calculated in runtime using permissive policy: if something not
 * restricted then it is permitted. If some feature has exactly configured
 * specifications then all of them should allow it. At least one forbidding will
 * forbid the feature. Created by The eXo Platform SAS.
 * 
 * @author <a href="mailto:pnedonosko@exoplatform.com">Peter Nedonosko</a>
 * @version $Id: CloudDriveFeatures.java 00000 Jan 30, 2014 pnedonosko $
 */
public interface CloudDriveFeatures {

  /**
   * Answer to: can create a drive for given user in the given node optionally
   * taking in account his user id and requested provider.
   * 
   * @param workspace {@link String}
   * @param nodePath {@link String}
   * @param userId {@link String} can be <code>null</code>, then the
   *          specification should take a decision using own source of the user
   *          id
   * @param provider {@link CloudProvider} can be <code>null</code>, then the
   *          specification should take a decision assuming any provider.
   * @return boolean, <code>true</code> if drive can be created,
   *         <code>false</code> otherwise.
   * @throws CannotCreateDriveException if drive can not be created (optional
   *           alternative to returning <code>false</code> value.
   */
  boolean canCreateDrive(String workspace,
                         String nodePath,
                         String userId,
                         CloudProvider provider) throws CannotCreateDriveException;

  /**
   * Answer is auto-synchronization should be enabled for given drive.
   * 
   * @param drive {@link CloudDrive}
   * @return boolean, <code>true</code> if enable auto synchronization for the
   *         drive, <code>false</code> otherwise.
   */
  boolean isAutosyncEnabled(CloudDrive drive);

}
