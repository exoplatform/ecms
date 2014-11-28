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

import org.apache.commons.lang.StringUtils;
import org.exoplatform.commons.upgrade.UpgradeProductPlugin;
import org.exoplatform.commons.utils.PrivilegedSystemHelper;
import org.exoplatform.commons.version.util.VersionComparator;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.services.cms.impl.Utils;
import org.exoplatform.services.cms.views.ManageViewService;
import org.exoplatform.services.cms.views.ViewConfig;
import org.exoplatform.services.cms.views.impl.ManageViewPlugin;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
/**
 * Created by The eXo Platform SAS
 * Author : eXoPlatform
 *          exo@exoplatform.com
 * Feb 24, 2012
 *
 * This class will be used to upgrade pre-defined views of Site Explorer. Views with desire of manual upgration 
 * can be specified in file configuration.properties.<br/>
 * Syntax :<br/> 
 * unchanged-site-explorer-views=<view name list>
 * For examples :<br/>
 * unchanged-site-explorer-views=anonymous-view
 *    
 */
public class UserViewUpgradePlugin extends UpgradeProductPlugin {
  
  private static final Log log = ExoLogger.getLogger(UserViewUpgradePlugin.class.getName());
  private ManageViewService manageViewService_;

  public UserViewUpgradePlugin(ManageViewService manageViewService, InitParams initParams) {
    super(initParams);
    this.manageViewService_  = manageViewService;
  }
  
  public void processUpgrade(String oldVersion, String newVersion) {
    if (log.isInfoEnabled()) {
      log.info("Start " + this.getClass().getName() + ".............");
    }
    String unchangedViews = PrivilegedSystemHelper.getProperty("unchanged-site-explorer-views");
    SessionProvider sessionProvider = null;
    if (StringUtils.isEmpty(unchangedViews)) {
      unchangedViews = "";
    }
    try {
      Set<String> unchangedViewSet = new HashSet<String>();
      Set<String> configuredViews = manageViewService_.getConfiguredViews();
      List<Node> removedNodes = new ArrayList<Node>();
      for (String unchangedView : unchangedViews.split(",")) {
        unchangedViewSet.add(unchangedView.trim());
      }
      //get all old query nodes that need to be removed.
      sessionProvider = SessionProvider.createSystemProvider();
      Node parentNode = null;
      for (ViewConfig viewConfig : manageViewService_.getAllViews()) {
        String viewName = viewConfig.getName();
        if (!unchangedViewSet.contains(viewName) && configuredViews.contains(viewName)) {
          removedNodes.add(manageViewService_.getViewByName(viewName, sessionProvider));
          parentNode = manageViewService_.getViewByName(viewName, sessionProvider).getParent();
        }
      }
      //remove the old query nodes
      for (Node removedNode : removedNodes) {
        try {
          String editedViews = removedNode.getName();
          Utils.removeEditedConfiguredData(editedViews, ManageViewPlugin.class.getSimpleName(),
                                           ManageViewPlugin.EDITED_CONFIGURED_VIEWS, true);
          removedNode.remove();
          parentNode.save();
        } catch (Exception e) {
          if (log.isInfoEnabled()) {
            log.error("Error in " + this.getName() + ": Can not remove old query node: " + removedNode.getPath());
          }
        }
      }
      //re-initialize new views
      manageViewService_.init();
      
      if (log.isInfoEnabled()) {
        log.info("Finish " + this.getClass().getName() + " successfully");
      }      
      
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
