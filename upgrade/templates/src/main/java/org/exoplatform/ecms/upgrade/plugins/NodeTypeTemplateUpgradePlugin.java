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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.jcr.Node;
import javax.jcr.NodeIterator;

import org.apache.commons.lang.StringUtils;
import org.exoplatform.commons.upgrade.UpgradeProductPlugin;
import org.exoplatform.commons.utils.PrivilegedSystemHelper;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.services.cms.metadata.MetadataService;
import org.exoplatform.services.cms.metadata.impl.MetadataServiceImpl;
import org.exoplatform.services.cms.templates.TemplateService;
import org.exoplatform.services.cms.templates.impl.TemplateServiceImpl;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

/**
 * Created by The eXo Platform SAS
 * Author : Nguyen Anh Vu
 *          vuna@exoplatform.com
 * Feb 23, 2012
 *
 * This class will be used to upgrade node type templates like view1.gtmpl, dialog1.gtmpl,
 * stylesheet-rt.css, stylesheet-lt.css. Node type templates with desire of manual upgration
 * can be specified in file configuration.properties.<br/>
 * Syntax :<br/>
 * unchanged-nodetype-templates=<node name list>
 * For examples :<br/>
 * unchanged-nodetype-templates=nt:file, exo:webContent
 *
 */
public class NodeTypeTemplateUpgradePlugin extends UpgradeProductPlugin {

  private static final Log LOG = ExoLogger.getLogger(NodeTypeTemplateUpgradePlugin.class.getName());

  private TemplateService templateService_;
  private MetadataService metadataService_;

  public NodeTypeTemplateUpgradePlugin(TemplateService templateService, MetadataService metadataService, InitParams initParams) {
    super(initParams);
    this.templateService_ = templateService;
    this.metadataService_ = metadataService;
  }

  @Override
  public void processUpgrade(String oldVersion, String newVersion) {
    if (LOG.isInfoEnabled()) {
      LOG.info("Start " + this.getClass().getName() + ".............");
    }
    String unchangedNodeTypes = PrivilegedSystemHelper.getProperty("unchanged-nodetype-templates");
    SessionProvider sessionProvider = null;
    if (StringUtils.isEmpty(unchangedNodeTypes)) {
      unchangedNodeTypes = "";
    }
    String unchangedMetadataTypes = PrivilegedSystemHelper.getProperty("unchanged-metadata-templates");
    if (StringUtils.isEmpty(unchangedMetadataTypes)) {
      unchangedMetadataTypes = "";
    }
    try {
      Set<String> unchangedNodeTypeSet = new HashSet<String>();
      Set<String> unchangedMetadataTypeSet = new HashSet<String>();
      Set<String> configuredNodeTypeSet = templateService_.getAllConfiguredNodeTypes();
      Set<String> editedPredefinedNodeTypes = templateService_.getAllEditedConfiguredNodeTypes();
      List<String> configuredMetadataTypeSet = metadataService_.getMetadataList();
      List<Node> removedNodes = new ArrayList<Node>();
      List<String> removedMetadatas = new ArrayList<String>();
      
      for (String unchangedNodeType : unchangedNodeTypes.split(",")) {
        unchangedNodeTypeSet.add(unchangedNodeType.trim());
      }
      for (String unchangedMetadataType : unchangedMetadataTypes.split(",")) {
        unchangedMetadataTypeSet.add(unchangedMetadataType.trim());
       }
      //get all node type nodes that need to be removed
      sessionProvider = SessionProvider.createSystemProvider();
      Node templateHomeNode = templateService_.getTemplatesHome(sessionProvider);
      NodeIterator iter = templateHomeNode.getNodes();
      while (iter.hasNext()) {
        Node templateNode = iter.nextNode();
        if (configuredNodeTypeSet.contains(templateNode.getName()) 
           && !unchangedNodeTypeSet.contains(templateNode.getName())
           && !editedPredefinedNodeTypes.contains(templateNode.getName())) {
          removedNodes.add(templateNode);
        }
      }
      // get all metadata type nodes that need to be removed
      for (String metadataType : configuredMetadataTypeSet) {
        if(!unchangedMetadataTypeSet.contains(metadataType)) 
          removedMetadatas.add(metadataType);
      }
      // remove all old node type nodes
      for (Node removedNode : removedNodes) {
        try {
          removedNode.remove();
          templateHomeNode.save();
        } catch (Exception e) {
          if (LOG.isInfoEnabled()) {
            LOG.error("Error in " + this.getName() + ": Can not remove old template: " + removedNode.getPath());
          }
        }
      }
      // remove all old metadata node type
      for (String removedMetadaNode : removedMetadatas) {
        metadataService_.removeMetadata(removedMetadaNode);
      }
      // reinitialize new templates
      ((TemplateServiceImpl)templateService_).start();
      ((MetadataServiceImpl)metadataService_).start();
    } catch (Exception e) {
      if (LOG.isErrorEnabled()) {
        LOG.error("An unexpected error occurs when migrating node type template", e);
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
