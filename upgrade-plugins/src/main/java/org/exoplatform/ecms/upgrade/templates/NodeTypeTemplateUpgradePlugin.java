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
import javax.jcr.Workspace;

import org.apache.commons.lang.StringUtils;

import org.exoplatform.commons.info.MissingProductInformationException;
import org.exoplatform.commons.info.ProductInformations;
import org.exoplatform.commons.upgrade.UpgradeProductPlugin;
import org.exoplatform.commons.utils.PrivilegedSystemHelper;
import org.exoplatform.commons.version.util.VersionComparator;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.services.cms.templates.TemplateService;
import org.exoplatform.services.cms.templates.impl.TemplateServiceImpl;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.cms.impl.Utils;

/**
 * Created by The eXo Platform SAS
 * Author : Nguyen Anh Vu
 *          vuna@exoplatform.com
 * Feb 23, 2012  
 * 
 * This class will be used to upgrade node type templates like view1.gtmpl, dialog1.gtmpl,
 * stylesheet-rt.css, stylesheet-lt.css. Node type templates with desire of manual upgration 
 * can be specified in file configuration.properties.<br>
 * Syntax :<br>
 * unchanged-nodetype-templates={node name list}
 * For examples :<br>
 * unchanged-nodetype-templates=nt:file, exo:article
 *
 */
public class NodeTypeTemplateUpgradePlugin extends UpgradeProductPlugin {

  private static final Log log = ExoLogger.getLogger(NodeTypeTemplateUpgradePlugin.class.getName());
  private static final String PRODUCT_VERSION_ZERO = "0";
  private static final String EDITED_CONFIGURED_NODE_TYPES = "EditedConfiguredNodeTypes";
  private static final String UNCHANG_NODE_TYPES_CONFIG = "exo.ecms.upgrades.unchanged-nodetype-templates";
  
  private TemplateService templateService_;
  private ProductInformations productInformations_;
  
  public NodeTypeTemplateUpgradePlugin(TemplateService templateService, ProductInformations productInformations, InitParams initParams) {
    super(initParams);
    this.templateService_ = templateService;
    this.productInformations_ = productInformations;
  }
 
  @Override
  public void processUpgrade(String oldVersion, String newVersion) {
    if (log.isInfoEnabled()) {
      log.info("Start " + this.getClass().getName() + ".............");
    }
    String unchangedNodeTypes = PrivilegedSystemHelper.getProperty(UNCHANG_NODE_TYPES_CONFIG);
    String previousPlfVersion = PRODUCT_VERSION_ZERO;
    Set<String> modifiedTemplateLog = new HashSet<String>();
    try {
      modifiedTemplateLog = Utils.getAllEditedConfiguredData(TemplateServiceImpl.class.getSimpleName(),
                                                                         EDITED_CONFIGURED_NODE_TYPES,true);
    } catch (Exception e1) {
      if (log.isErrorEnabled()) {
        log.error("Can not get All Edited Template log",e1);
      }
    }
    try {
      previousPlfVersion = productInformations_.getPreviousVersion();
    } catch (MissingProductInformationException e2) {
      if (log.isErrorEnabled()) {
        log.error("Can not get PLF previous version, set it to '0'", e2);
      }
    }
    
    SessionProvider sessionProvider = null;
    if (StringUtils.isEmpty(unchangedNodeTypes)) {
      unchangedNodeTypes = "";
    }
    try {
      Set<String> unchangedNodeTypeSet = new HashSet<String>();
      Set<String> configuredNodeTypeSet = templateService_.getAllConfiguredNodeTypes();
      List<Node> removedNodes = new ArrayList<Node>();
      for (String unchangedNodeType : unchangedNodeTypes.split(",")) {
        unchangedNodeTypeSet.add(unchangedNodeType.trim());
      }
      //get all node type nodes that need to be removed
      sessionProvider = SessionProvider.createSystemProvider();
      Node templateHomeNode = templateService_.getTemplatesHome(sessionProvider);
      Workspace workspace = templateHomeNode.getSession().getWorkspace();
      NodeIterator iter = templateHomeNode.getNodes();
      while (iter.hasNext()) {
        Node templateNode = iter.nextNode();
        if (configuredNodeTypeSet.contains(templateNode.getName()) && !unchangedNodeTypeSet.contains(templateNode.getName())) {
          removedNodes.add(templateNode);
        }
      }
      // remove all old node type nodes
      for (Node removedNode : removedNodes) {
        try {
          String removedTemplateName = removedNode.getName();
          //if Template had not been edited before, remove it
          if(!modifiedTemplateLog.contains(removedTemplateName)){
            if (log.isInfoEnabled()) {
              log.info("Update templates of node type {} with a new version", removedTemplateName);
            }
            removedNode.remove();
          }else{
            //else if Template was edited before, rename it
            if (log.isWarnEnabled()) {
              StringBuffer logContent = new StringBuffer();
              logContent.append("Templates of {} have been customized. ");
              logContent.append("They will be updated by the new version included in eXo Platform ").append(productInformations_.getVersion());
              logContent.append(" but your customized templates will be kept and renamed. ");
              logContent.append("If you want to re-apply your customizations to the new templates versions, ");
              logContent.append("you can retrieve them in the Content Administration.");
              log.warn(logContent.toString(),new Object[]{removedTemplateName});
            }
            
            renameTemplate(removedNode, previousPlfVersion, workspace);
          }

          templateHomeNode.save();
          
          //remove template out of edit log
          removeTemplateFromEditLog(removedTemplateName);
          
        } catch (Exception e) {
          if (log.isErrorEnabled()) {
            log.error("Error in " + this.getName() + ": Can not remove old template: " + removedNode.getPath(),e);
          }
        }
      }
      // reinitialize new templates
      ((TemplateServiceImpl)templateService_).start();
    } catch (Exception e) {
      if (log.isErrorEnabled()) {
        log.error("An unexpected error occurs when migrating node type template", e);
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
  
  private void removeTemplateFromEditLog(String templateName){
    try {
      Utils.removeEditedConfiguredData(templateName,
                                       TemplateServiceImpl.class.getSimpleName(),
                                       EDITED_CONFIGURED_NODE_TYPES,
                                       true);
    } catch (Exception e) {
      if (log.isErrorEnabled()) {
        log.error("Can not remove edited log of template {}", templateName);
      }
    }
  }
  
  private void renameTemplate(Node templateNode, String plfVersion, Workspace workspace){
    String[] childNodesName = new String[]{TemplateService.DIALOGS, TemplateService.VIEWS, TemplateService.SKINS};
    for (String nodeName : childNodesName) {
      try {
        if(templateNode.hasNode(nodeName)){
          Node childNode = templateNode.getNode(nodeName);
          if (log.isInfoEnabled()) {
            log.info("Process rename children of {}", nodeName);
          }
          NodeIterator iter = childNode.getNodes();
          while (iter.hasNext()) {
            Node node = iter.nextNode();
            StringBuffer path =  new StringBuffer(node.getPath());
            workspace.move(path.toString(), path.append("_").append(plfVersion).toString());
          }
        }
      } catch (Exception e) {
        if (log.isErrorEnabled()) {
          log.error("Exceptions happen while renaming children of {}", nodeName, e);
        }
      }
    }
  }
 
}
