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
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
* GNU Affero General Public License for more details.
*
* You should have received a copy of the GNU Affero General Public License
* along with this program. If not, see <http://www.gnu.org/licenses/>.
*/

package org.exoplatform.upgrade;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import org.exoplatform.services.cms.templates.TemplateService;
import org.exoplatform.services.cms.templates.impl.TemplateServiceImpl;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.picocontainer.Startable;
import org.apache.commons.lang.StringUtils;

/**
* Created by The eXo Platform SAS
* Author : Ha Quang Tan
* tanhq@exoplatform.com
* May 24, 2012
*
* This class will be used to upgrade node type templates like view1.gtmpl, dialog1.gtmpl,
* stylesheet-rt.css, stylesheet-lt.css. Node type templates with desire of manual upgration
* can be specified in file ecms.properties.<br/>
* Syntax :<br/>
* migrated-nodetype-templates=<node name list>
* For examples :<br/>
* migrated-nodetype-templates=nt:file, exo:article
*
*/

public class NodeTypeTemplateMigrationService implements Startable{
	
  final static private Log LOG = ExoLogger.getLogger(NodeTypeTemplateMigrationService.class.getName());
  
  private RepositoryService repoService_;
  
  private TemplateService templateService_;
  
  public NodeTypeTemplateMigrationService(RepositoryService repositoryService, TemplateService templateService) {
    repoService_ = repositoryService;
    templateService_ = templateService;
  }
  
  public void start() {
    if (LOG.isInfoEnabled()) LOG.info("Start " + this.getClass().getName() + ".............");    
    String migratedNodeTypes = null;
    try {
    	Properties prop = new Properties();
    	InputStream in = getClass().getResourceAsStream("/conf/emcs-upgrade.properties");
    	prop.load(in);
    	migratedNodeTypes = prop.getProperty("migrate-nodetype-templates");
    	in.close();	    
    } catch (IOException e) {
    	if (LOG.isErrorEnabled()) LOG.error("An unexpected problem occurs when loading nodetype templates from properties file");
    }        
    if (StringUtils.isEmpty(migratedNodeTypes)) {          	
      return;
    }
    SessionProvider sessionProvider = SessionProvider.createSystemProvider();
    try {  		
      ManageableRepository repository = repoService_.getCurrentRepository(); 
      Set<String> migratedNodeTypeSet = new HashSet<String>();
      List<Node> removedNodes = new ArrayList<Node>();
  		
      List<String> configuredNodeTypeSet = templateService_.getAllDocumentNodeTypes(repository.getConfiguration().getName());
      for (String migratedNodeType : migratedNodeTypes.split(",")) {
        migratedNodeTypeSet.add(migratedNodeType.trim());
      }      
      //get all node type nodes that need to be removed
      Node templateHomeNode = templateService_.getTemplatesHome(repository.getConfiguration().getName(), sessionProvider);
      NodeIterator iter = templateHomeNode.getNodes();
      while (iter.hasNext()) {
        Node templateNode = iter.nextNode();        
        if (configuredNodeTypeSet.contains(templateNode.getName()) && migratedNodeTypeSet.contains(templateNode.getName())) {
          removedNodes.add(templateNode);
        }
      }      
      // remove all old node type nodes
      for (Node removedNode : removedNodes) {
	      try {
	        removedNode.remove();
	        templateHomeNode.save();
	      } catch (Exception e) {
	        if (LOG.isErrorEnabled()) {
	          if (LOG.isErrorEnabled()) LOG.error("An unexpected problem occurs when migrating templates to new structure", e);
	        }
	      }
      }
      // reinitialize new templates
      ((TemplateServiceImpl)templateService_).start();
    } catch (Exception e) {
      if (LOG.isErrorEnabled()) LOG.error("An unexpected problem occurs when migrating templates to new structure", e);
    } finally {
      sessionProvider.close();
    }
  }
  
  public void stop() {
  }

}
