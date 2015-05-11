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
package org.exoplatform.ecms.upgrade.templates;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;

import org.apache.commons.lang.StringUtils;
import org.exoplatform.commons.upgrade.UpgradeProductPlugin;
import org.exoplatform.commons.utils.PrivilegedSystemHelper;
import org.exoplatform.commons.version.util.VersionComparator;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.services.cms.views.ApplicationTemplateManagerService;
import org.exoplatform.services.cms.views.impl.ApplicationTemplateManagerServiceImpl;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

/**
 * Created by The eXo Platform SAS
 * Author : Nguyen Anh Vu
 *          vuna@exoplatform.com
 * Feb 24, 2012
 *
 * This class will be used to upgrade pre-defined templates of Content-list-viewer and Search portlets. 
 * Views with desire of manual upgration can be specified in file configuration.properties.<br/>
 * Syntax :<br/> 
 * unchanged-clv-templates=<template name list>
 * unchanged-wcm-search-templates=<template name list>
 * For examples :<br/>
 * unchanged-clv-templates=list/BigHotNewsTemplateCLV.gtmpl,navigation/CategoryList.gtmpl
 * unchanged-wcm-search-templates=search-form/UIDefaultSearchForm.gtmpl
 *       
 */
public class WCMTemplateUpgradePlugin extends UpgradeProductPlugin {

  private static final Log log = ExoLogger.getLogger(WCMTemplateUpgradePlugin.class.getName());
  private ApplicationTemplateManagerService appTemplateService_;
  
  public WCMTemplateUpgradePlugin(ApplicationTemplateManagerService appTemplateService, InitParams initParams) {
    super(initParams);
    this.appTemplateService_ = appTemplateService;
  }

  public void processUpgrade(String oldVersion, String newVersion) {
    if (log.isInfoEnabled()) {
      log.info("Start " + this.getClass().getName() + ".............");
    }
    String unchangedClvTemplates = PrivilegedSystemHelper.getProperty("unchanged-clv-templates");
    String unchangedSearchTemplates = PrivilegedSystemHelper.getProperty("unchanged-wcm-search-templates");
    upgrade(unchangedClvTemplates, "content-list-viewer");
    upgrade(unchangedSearchTemplates, "search");
    
    try {
      // re-initialize new scripts
      ((ApplicationTemplateManagerServiceImpl)appTemplateService_).start();
    } catch (Exception e) {
      if (log.isErrorEnabled()) {
        log.error("An unexpected error occurs when migrating templates for portlet CLV and WCMSearch: ", e);        
      }
    }
  }
  
  private void upgrade(String unchangedTemplates, String portletName) {
    SessionProvider sessionProvider = null;
    if (StringUtils.isEmpty(unchangedTemplates)) {
      unchangedTemplates = "";
    }
    try {
      Set<String> unchangedTemplateSet = new HashSet<String>();
      Set<String> configuredTemplates = appTemplateService_.getConfiguredAppTemplateMap(portletName);
      List<Node> removedNodes = new ArrayList<Node>();
      for (String unchangedTemplate : unchangedTemplates.split(",")) {
        if(StringUtils.isNotEmpty(unchangedTemplate.trim()))
          unchangedTemplateSet.add(unchangedTemplate.trim());
      }
      //get all old query nodes that need to be removed.
      sessionProvider = SessionProvider.createSystemProvider();
      Node templateHomeNode = appTemplateService_.getApplicationTemplateHome(portletName, sessionProvider);
      QueryManager queryManager = templateHomeNode.getSession().getWorkspace().getQueryManager();
      NodeIterator iter = queryManager.
          createQuery("SELECT * FROM nt:file WHERE jcr:path LIKE '" + templateHomeNode.getPath() + "/%'", Query.SQL).
          execute().getNodes();
      if(unchangedTemplateSet.size()>0) {
        while (iter.hasNext()) {
          Node templateNode = iter.nextNode();
          if (!unchangedTemplateSet.contains(templateNode.getPath().substring(templateHomeNode.getPath().length() + 1)) &&
                  configuredTemplates.contains(templateNode.getPath().substring(templateHomeNode.getPath().length() + 1))) {
            removedNodes.add(templateNode);
          }
        }
      }
      //remove all old script nodes
      for (Node removedNode : removedNodes) {
        try {
          removedNode.remove();
          templateHomeNode.save();
        } catch (Exception e) {
          if (log.isInfoEnabled()) {
            log.error("Error in " + this.getName() + ": Can not remove old template: " + removedNode.getPath());
          }
        }
      }
    } catch (Exception e) {
      if (log.isErrorEnabled()) {
        log.error("An unexpected error occurs when migrating templates for portlet: " + portletName + ": ", e);        
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
