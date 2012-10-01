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

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Session;
import javax.jcr.query.Query;

import org.exoplatform.commons.upgrade.UpgradeProductPlugin;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.container.component.RequestLifeCycle;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.services.cms.BasePath;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.jcr.ext.hierarchy.NodeHierarchyCreator;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.organization.Group;
import org.exoplatform.services.organization.OrganizationService;
import org.exoplatform.services.wcm.core.NodetypeConstant;

/**
 * Created by The eXo Platform SAS
 * Author : eXoPlatform
 *          exo@exoplatform.com
 * Sep 28, 2012  
 */
public class DriveDataUpgradePlugin extends UpgradeProductPlugin {

  private static final Log LOG = ExoLogger.getLogger(DriveDataUpgradePlugin.class.getName());

  private NodeHierarchyCreator nodeHierarchyCreator;
  private RepositoryService repoService;
  private OrganizationService organService;
  
  public DriveDataUpgradePlugin(RepositoryService repoService,
                                NodeHierarchyCreator nodeHierarchyCreator,
                                OrganizationService organizationService,
                                InitParams initParams) {
    super(initParams);
    
    // Get services
    this.nodeHierarchyCreator =  nodeHierarchyCreator;
    this.repoService = repoService;
    this.organService = organizationService;
  }


  @Override
  public void processUpgrade(String oldVersion, String newVersion) {
    SessionProvider sessionProvider = null;
    long time1 = System.currentTimeMillis();
    try {
      if (LOG.isInfoEnabled()) {
        LOG.info("Start " + this.getClass().getName() + ".............");
      }
      RequestLifeCycle.begin(PortalContainer.getInstance());

      sessionProvider = SessionProvider.createSystemProvider();
      ManageableRepository repo = repoService.getCurrentRepository();
      Session session = sessionProvider.getSession(repo.getConfiguration().getDefaultWorkspaceName(),
                                                   repo);
      String groupsPath = nodeHierarchyCreator.getJcrPath(BasePath.CMS_GROUPS_PATH);
      //Collection<Group> groups = organService.getGroupHandler().getAllGroups();
      //queries all sub nodes of groupNode
      NodeIterator subNodes = session.getWorkspace().getQueryManager().
        createQuery("SELECT * FROM nt:unstructured WHERE jcr:path like '" + groupsPath + "/%' " +
                    "ORDER BY exo:name", Query.SQL).execute().getNodes();
      //check if any of them is group drive folder, then
      int count = 0;
      while (subNodes.hasNext()) {
        Node subNode = subNodes.nextNode();
        //get node path as groupId
        String groupId = subNode.getPath().substring(groupsPath.length());
        if (groupId.endsWith("/")) {
          groupId = groupId.substring(0, groupId.length() - 1);
        }
        //chec if the path is same as a groupId
        Group group = organService.getGroupHandler().findGroupById(groupId);
        if (group != null) {
          //add mixin and property
          if (!subNode.isNodeType(NodetypeConstant.EXO_DRIVE)) {
            subNode.addMixin(NodetypeConstant.EXO_DRIVE);
            subNode.setProperty(NodetypeConstant.EXO_LABEL, group.getLabel());
            subNode.save();
          }
          //Log
          if (((++count) % 100 == 0) && LOG.isInfoEnabled()) {
            LOG.info(count + " groups processed...");
          }
        }
      }
    }
    catch (Exception e) {
      if (LOG.isErrorEnabled()) {
        LOG.error(this.getClass().getName() + " failed:", e);
      }
    }
    finally {
      RequestLifeCycle.end();
      if (sessionProvider != null) {
        sessionProvider.close();
      }
      if (LOG.isInfoEnabled()) {
        LOG.info("End " + this.getClass().getName() + ".............");
        LOG.info("Total execution time = " + (System.currentTimeMillis() - time1)/1000.0 + "(s).........................");
      }
    }
  }

  @Override
  public boolean shouldProceedToUpgrade(String previousVersion, String newVersion) {
    return true;
  }

}
