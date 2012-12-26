/*
 * Copyright (C) 2003-2012 eXo Platform SAS.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.exoplatform.ecms.upgrade.plugins;

import java.util.List;

import javax.jcr.RepositoryException;

import org.exoplatform.commons.upgrade.UpgradeProductPlugin;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.services.cms.documents.TrashService;
import org.exoplatform.services.jcr.access.AccessControlList;
import org.exoplatform.services.jcr.core.ExtendedNode;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

/**
 * Created by The eXo Platform SAS
 * Author : Nguyen The Vinh From ECM Of eXoPlatform
 *          vinh_nguyen@exoplatform.com
 * 28 Nov 2012  
 */
public class TrashPermissionUpgradePlugin extends UpgradeProductPlugin {
  private TrashService     trashService;
  private static final Log    LOG                = ExoLogger.getLogger(TrashPermissionUpgradePlugin.class.getName());
  private static final String TRASH_ANY_IDENTITY = "any";
  public TrashPermissionUpgradePlugin(TrashService trashService, InitParams initParams) {
    super(initParams);
    this.trashService = trashService;
  }

  @Override
  public void processUpgrade(String oldVersion, String newVersion)  {
    if (LOG.isInfoEnabled()) {
      LOG.info("Start TrashPermissionUpgradePlugin");
    }
    ExtendedNode trashNode = (ExtendedNode)trashService.getTrashHomeNode();
    AccessControlList acl;
    try {
      acl = trashNode.getACL();
      List<String> anyPermission = acl.getPermissions(TRASH_ANY_IDENTITY);
      if (anyPermission != null & !anyPermission.isEmpty()) {
        trashNode.removePermission(TRASH_ANY_IDENTITY);
      }
    } catch (RepositoryException re) {
      if (LOG.isErrorEnabled()) {
        LOG.error("Error while getting the Trash's ACL, ", re);
      }
    }
    if (LOG.isInfoEnabled()) {
      LOG.info("Finish TrashPermissionUpgradePlugin");
    }
  }

  @Override
  public boolean shouldProceedToUpgrade(String previousVersion, String newVersion) {
    return !previousVersion.equals(newVersion);
    
  }
}