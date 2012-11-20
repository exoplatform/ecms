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

import org.exoplatform.commons.upgrade.UpgradeProductPlugin;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.services.cms.views.ApplicationTemplateManagerService;
import org.exoplatform.services.cms.views.impl.ApplicationTemplateManagerServiceImpl;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

/**
 * Created by The eXo Platform SAS
 * Author : Nguyen The Vinh
 *          vinh_nguyen@exoplatform.com
 * Nov 16, 2012
 *
 * This upgrade plugin will remove the old navigation/CategoryTree.gtmpl of CLV's portlet templates and
 * replace by the new one of upgrade version because of the problem in 
 * https://jira.exoplatform.org/browse/ECMS-3776
 */
public class CLVCategoryTemplateUpgradePlugin extends UpgradeProductPlugin {

  private static final Log LOG = ExoLogger.getLogger(CLVCategoryTemplateUpgradePlugin.class.getName());
  private ApplicationTemplateManagerService appTemplateService_;

  public CLVCategoryTemplateUpgradePlugin(ApplicationTemplateManagerService appTemplateService, InitParams initParams) {
    super(initParams);
    this.appTemplateService_ = appTemplateService;
  }

  public void processUpgrade(String oldVersion, String newVersion) {
    if (LOG.isInfoEnabled()) {
      LOG.info("Start " + this.getClass().getName() + ".............");
    }
    upgrade("navigation/CategoryTree.gtmpl", "content-list-viewer");

    try {
      // re-initialize new scripts
      ((ApplicationTemplateManagerServiceImpl)appTemplateService_).start();
    } catch (Exception e) {
      if (LOG.isErrorEnabled()) {
        LOG.error("An unexpected error occurs when migrating templates for portlet CLV and WCMSearch: ", e);
      }
    }
  }

  private void upgrade(String upgradeTemplate, String portletName) {
    SessionProvider sessionProvider = null;
    try {
      //get all old query nodes that need to be removed.
      sessionProvider = SessionProvider.createSystemProvider();
      Node templateHomeNode = appTemplateService_.getApplicationTemplateHome(portletName, sessionProvider);
      Node removedTemplate = templateHomeNode.getNode(upgradeTemplate);
      removedTemplate.remove();
      templateHomeNode.save();
    } catch (Exception e) {
      if (LOG.isErrorEnabled()) {
        LOG.error("An unexpected error occurs when migrating templates for portlet: " + portletName + ": ", e);
      }
    } finally {
      if (sessionProvider != null) {
        sessionProvider.close();
      }
    }
  }

  @Override
  public boolean shouldProceedToUpgrade(String previousVersion, String newVersion) {
    return true;
  }

}
