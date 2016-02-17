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
package org.exoplatform.ecms.upgrade.views;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Session;

import org.apache.commons.lang.StringUtils;
import org.exoplatform.commons.upgrade.UpgradeProductPlugin;
import org.exoplatform.commons.utils.PrivilegedSystemHelper;
import org.exoplatform.commons.version.util.VersionComparator;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.services.cms.BasePath;
import org.exoplatform.services.cms.impl.DMSConfiguration;
import org.exoplatform.services.cms.impl.DMSRepositoryConfiguration;
import org.exoplatform.services.cms.views.ManageViewService;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.jcr.ext.hierarchy.NodeHierarchyCreator;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

/**
 * Created by The eXo Platform SAS
 * Author : Nguyen Anh Vu
 *          vuna@exoplatform.com
 * Feb 24, 2012  
 *
 * This class will be used to upgrade pre-defined templates of Site Explorer. Templates with desire of manual upgration 
 * can be specified in file configuration.properties.<br>
 * Syntax :<br>
 * unchanged-site-explorer-templates={templates name list}
 * For examples :<br>
 * unchanged-site-explorer-templates=ThumbnailsView, ContentView
 * 
 */
public class SiteExplorerTemplateUpgradePlugin extends UpgradeProductPlugin {

  private static final Log log = ExoLogger.getLogger(SiteExplorerTemplateUpgradePlugin.class.getName());
  private NodeHierarchyCreator nodeHierarchyCreator_;
  private DMSConfiguration dmsConfiguration_;
  private RepositoryService repositoryService_;
  private ManageViewService manageViewService_;

  public SiteExplorerTemplateUpgradePlugin(NodeHierarchyCreator nodeHierarchyCreator, RepositoryService repoService, 
                                       DMSConfiguration dmsConfiguration, ManageViewService manageViewService,
                                       InitParams initParams) {
    super(initParams);
    this.nodeHierarchyCreator_ = nodeHierarchyCreator;
    this.repositoryService_ = repoService;
    this.dmsConfiguration_ = dmsConfiguration;
    this.manageViewService_ = manageViewService;
  }
  
  public void processUpgrade(String oldVersion, String newVersion) {
    if (log.isInfoEnabled()) {
      log.info("Start " + this.getClass().getName() + ".............");
    }
    String unchangedViews = PrivilegedSystemHelper.getProperty("unchanged-site-explorer-templates");
    SessionProvider sessionProvider = null;
    if (StringUtils.isEmpty(unchangedViews)) {
      unchangedViews = "";
    }
    try {
      Set<String> unchangedViewSet = new HashSet<String>();
      Set<String> configuredTemplates = manageViewService_.getConfiguredTemplates();
      List<Node> removedNodes = new ArrayList<Node>();
      for (String unchangedView : unchangedViews.split(",")) {
        unchangedViewSet.add(unchangedView.trim());
      }
      //get all old query nodes that need to be removed.
      sessionProvider = SessionProvider.createSystemProvider();
      DMSRepositoryConfiguration dmsRepoConfig = dmsConfiguration_.getConfig();
      Session session = sessionProvider.getSession(dmsRepoConfig.getSystemWorkspace(), 
                                                   repositoryService_.getCurrentRepository());
      
      String ecmExplorerViewNodePath = nodeHierarchyCreator_.getJcrPath(BasePath.ECM_EXPLORER_TEMPLATES);
      Node ecmExplorerViewNode = (Node)session.getItem(ecmExplorerViewNodePath);
      NodeIterator iter = ecmExplorerViewNode.getNodes();
      while (iter.hasNext()) {
        Node viewNode = iter.nextNode();
        if (!unchangedViewSet.contains(viewNode.getName()) &&
            configuredTemplates.contains(viewNode.getName())) {
          removedNodes.add(viewNode);
        }
      }
      //remove the old query nodes
      for (Node removedNode : removedNodes) {
        try {
          removedNode.remove();
          ecmExplorerViewNode.save();
        } catch (Exception e) {
          if (log.isInfoEnabled()) {
            log.error("Error in " + this.getName() + ": Can not remove old query node: " + removedNode.getPath());
          }
        }
      }
      //re-initialize new views
      manageViewService_.init();
    } catch (Exception e) {
      if (log.isErrorEnabled()) {
        log.error("An unexpected error occurs when migrating Site Explorer views:", e);        
      }
    } finally {
      if (sessionProvider != null) {
        sessionProvider.close();
      }
    }
  }
  
  @Override
  public boolean shouldProceedToUpgrade(String newVersion, String previousVersion) {
    // --- return true only for the first version of platform
    return VersionComparator.isAfter(newVersion,previousVersion);
  }

}
