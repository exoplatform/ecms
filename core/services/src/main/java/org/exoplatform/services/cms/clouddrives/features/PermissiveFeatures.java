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
package org.exoplatform.services.cms.clouddrives.features;

import org.exoplatform.services.cms.clouddrives.CloudDrive;
import org.exoplatform.services.cms.clouddrives.CloudProvider;
import org.exoplatform.services.security.ConversationState;

/**
 * Specification with all features permitted. Created by The eXo Platform SAS
 * 
 * @author <a href="mailto:pnedonosko@exoplatform.com">Peter Nedonosko</a>
 * @version $Id: PermissiveFeatures.java 00000 Jan 30, 2014 pnedonosko $
 */
public class PermissiveFeatures implements CloudDriveFeatures {

  /**
   * Instantiates a new permissive features.
   */
  public PermissiveFeatures() {
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean canCreateDrive(String workspace, String nodePath, String userId, CloudProvider provider) {
    return true;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean isAutosyncEnabled(CloudDrive drive) {
    // Allow only drive owner run automatic synchronization
    try {
      ConversationState cs = ConversationState.getCurrent();
      return cs != null && drive.getLocalUser().equals(cs.getIdentity().getUserId());
    } catch (Throwable e) {
      // ignore here
    }
    return false;
  }

}
