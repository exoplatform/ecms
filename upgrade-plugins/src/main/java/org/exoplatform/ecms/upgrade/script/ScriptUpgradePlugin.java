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
package org.exoplatform.ecms.upgrade.script;

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
import org.exoplatform.services.cms.scripts.ScriptService;
import org.exoplatform.services.cms.scripts.impl.ScriptServiceImpl;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

/**
 * Created by The eXo Platform SAS
 * Author : Nguyen Anh Vu
 *          vuna@exoplatform.com
 * Feb 23, 2012
 * 
 * This class will be used to upgrade pre-defined scripts. Scripts with desire of manual upgration 
 * can be specified in file configuration.properties.<br/>
 * Syntax :<br/> 
 * unchanged-scripts=<script name list>
 * For examples :<br/>
 * unchanged-scripts=action/AddMetadataScript.groovy, action/AddTaxonomyActionScript.groovy
 * 
 */
public class ScriptUpgradePlugin extends UpgradeProductPlugin {
  
  private static final Log log = ExoLogger.getLogger(ScriptUpgradePlugin.class.getName());
  private ScriptService scriptService_;

  
  public ScriptUpgradePlugin(ScriptService scriptService, InitParams initParams) {
    super(initParams);
    this.scriptService_ = scriptService;
  }
  
  @Override
  public void processUpgrade(String oldVersion, String newVersion) {
    if (log.isInfoEnabled()) {
      log.info("Start " + this.getClass().getName() + ".............");
    }
    String unchangedTemplates = PrivilegedSystemHelper.getProperty("unchanged-scripts");
    SessionProvider sessionProvider = null;
    if (StringUtils.isEmpty(unchangedTemplates)) {
      unchangedTemplates = "";
    }
    try {
      Set<String> unchangedTemplateSet = new HashSet<String>();
      Set<String> allConfiguredScripts = scriptService_.getAllConfiguredScripts();
      List<Node> removedNodes = new ArrayList<Node>();
      for (String unchangedTemplate : unchangedTemplates.split(",")) {
        unchangedTemplateSet.add(unchangedTemplate.trim());
      }
      //get all script nodes that need to be removed
      sessionProvider = SessionProvider.createSystemProvider();
      Node ecmExplorer = scriptService_.getECMScriptHome(sessionProvider);
      QueryManager queryManager = ecmExplorer.getSession().getWorkspace().getQueryManager();
      NodeIterator iter = queryManager.
          createQuery("SELECT * FROM nt:file WHERE jcr:path LIKE '" +ecmExplorer.getPath() + "/%'", Query.SQL).
          execute().getNodes();
      while (iter.hasNext()) {
        Node scriptNode = iter.nextNode();
        if (!unchangedTemplateSet.contains(scriptNode.getPath().substring(ecmExplorer.getPath().length() + 1)) &&
            allConfiguredScripts.contains(scriptNode.getName())) {
          removedNodes.add(scriptNode);
        }
      }
      //remove all old script nodes
      for (Node removedNode : removedNodes) {
        try {
          removedNode.remove();
          ecmExplorer.save();
        } catch (Exception e) {
          if (log.isInfoEnabled()) {
            log.error("Error in " + this.getName() + ": Can not remove old template: " + removedNode.getPath());
          }
        }
      }
      // re-initialize new scripts
      ((ScriptServiceImpl)scriptService_).start();
    } catch (Exception e) {
      if (log.isErrorEnabled()) {
        log.error("An unexpected error occurs when migrating scripts", e);        
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
