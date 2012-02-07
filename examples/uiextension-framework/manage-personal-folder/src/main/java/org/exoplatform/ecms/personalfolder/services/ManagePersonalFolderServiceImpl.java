/***************************************************************************
 * Copyright (C) 2003-2009 eXo Platform SAS.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, see<http://www.gnu.org/licenses/>.
 *
 **************************************************************************/
package org.exoplatform.ecms.personalfolder.services;

import java.util.List;

import javax.jcr.Node;

import org.exoplatform.container.xml.InitParams;
import org.exoplatform.services.jcr.ext.distribution.DataDistributionManager;
import org.exoplatform.services.jcr.ext.distribution.DataDistributionMode;
import org.exoplatform.services.jcr.ext.distribution.DataDistributionType;
import org.exoplatform.services.jcr.ext.hierarchy.NodeHierarchyCreator;
import org.exoplatform.services.jcr.ext.hierarchy.impl.AddPathPlugin;
import org.exoplatform.services.jcr.ext.hierarchy.impl.HierarchyConfig;
import org.exoplatform.services.jcr.ext.hierarchy.impl.HierarchyConfig.JcrPath;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;

/**
 * Created by The eXo Platform SARL
 * Author : Dang Van Minh
 *          minh.dang@exoplatform.com
 * Jan 10, 2012
 * 9:47:02 AM  
 */
public class ManagePersonalFolderServiceImpl {

  private static final Log LOG = ExoLogger.getLogger(ManagePersonalFolderServiceImpl.class);

  private final HierarchyConfig config_;

  private final NodeHierarchyCreator nodeHierarchyCreatorService_;
  
  private final DataDistributionType dataDistributionType_;
  
  public ManagePersonalFolderServiceImpl(NodeHierarchyCreator nodeHierarchyCreatorService, 
      DataDistributionManager dataDistributionManager, InitParams params) {
    dataDistributionType_ = dataDistributionManager.getDataDistributionType(DataDistributionMode.NONE);
    config_ = params.getObjectParamValues(HierarchyConfig.class).get(0);
    nodeHierarchyCreatorService.addPlugin(new AddPathPlugin(params));
    nodeHierarchyCreatorService_ = nodeHierarchyCreatorService;
  }
  
  public void initUserFolder(String userName) {
    try {
       Node userNode = nodeHierarchyCreatorService_.getUserNode(WCMCoreUtils.getSystemSessionProvider(), userName);
       List<JcrPath> jcrPaths = config_.getJcrPaths();
       for (JcrPath jcrPath : jcrPaths) {
         dataDistributionType_.getOrCreateDataNode(userNode, jcrPath.getPath(), jcrPath.getNodeType(), jcrPath.getMixinTypes(),
              jcrPath.getPermissions(userName));
       }         
    } catch (Exception e) {
       LOG.error("An error occurs while initializing the user directory of '" + userName + "'", e);
    }
  }
}
