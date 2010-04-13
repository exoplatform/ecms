/*
 * Copyright (C) 2003-2007 eXo Platform SAS.
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
 */
package org.exoplatform.services.cms.metadata.impl;

import java.util.ArrayList;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.NodeTypeIterator;
import javax.jcr.nodetype.PropertyDefinition;

import org.exoplatform.container.component.ComponentPlugin;
import org.exoplatform.services.cms.BasePath;
import org.exoplatform.services.cms.impl.DMSConfiguration;
import org.exoplatform.services.cms.impl.DMSRepositoryConfiguration;
import org.exoplatform.services.cms.metadata.MetadataService;
import org.exoplatform.services.cms.templates.impl.TemplatePlugin;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.jcr.core.nodetype.ExtendedNodeTypeManager;
import org.exoplatform.services.jcr.ext.hierarchy.NodeHierarchyCreator;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.picocontainer.Startable;

/**
 * @author Hung Nguyen Quang
 * @mail   nguyenkequanghung@yahoo.com
 * Process with meta data for system
 */

public class MetadataServiceImpl implements MetadataService, Startable{
  
  /**
   * NodeType NT_UNSTRUCTURED
   */
  final static public String NT_UNSTRUCTURED = "nt:unstructured";
  
  /**
   * NodeType EXO_TEMPLATE
   */
  final static public String EXO_TEMPLATE = "exo:template";
  
  /**
   * Property name EXO_ROLES_PROP contains permission
   */
  final static public String EXO_ROLES_PROP = "exo:roles";
  
  /**
   * Property name EXO_TEMPLATE_FILE_PROP contains content of template
   */
  final static public String EXO_TEMPLATE_FILE_PROP = "exo:templateFile";
  
  /**
   * Property name INTERNAL_USE
   */
  final static public String INTERNAL_USE = "exo:internalUse".intern();
  
  /**
   * NodeType METADATA_TYPE
   */
  final static public String METADATA_TYPE = "exo:metadata".intern();
  
  /**
   * Node name DIALOGS
   */
  final static public String DIALOGS = "dialogs";
  
  /**
   * Node name VIEWS
   */
  final static public String VIEWS = "views";
  
  /**
   * Node name DIALOG1
   */
  final static public String DIALOG1 = "dialog1";
  
  /**
   * Node name VIEW1
   */
  final static public String VIEW1 = "view1";

  /**
   * RepositoryService object process with repository
   */
  private RepositoryService repositoryService_;
  
  /**
   * NodeHierarchyCreator object
   */
  private NodeHierarchyCreator nodeHierarchyCreator_;
  
  /**
   * Path to Metadata node in System workspace
   */
  private String baseMetadataPath_;
  
  /**
   * List of TemplatePlugin plugins_
   */
  private List<TemplatePlugin> plugins_ = new ArrayList<TemplatePlugin>();
 
  /**
  * DMS configuration which used to store informations
  */   
  private DMSConfiguration dmsConfiguration_;
  private static final Log LOG  = ExoLogger.getLogger(MetadataServiceImpl.class);

  /**
   * Constructor method
   * Init nodeHierarchyCreator_, repositoryService_, baseMetadataPath_
   * @param nodeHierarchyCreator  NodeHierarchyCreator object
   * @param repositoryService     RepositoryService object
   * @throws Exception
   */
  public MetadataServiceImpl(NodeHierarchyCreator nodeHierarchyCreator, 
      RepositoryService repositoryService, DMSConfiguration dmsConfiguration) throws Exception {
    nodeHierarchyCreator_ = nodeHierarchyCreator;
    repositoryService_ = repositoryService;
    baseMetadataPath_ = nodeHierarchyCreator_.getJcrPath(BasePath.METADATA_PATH);
    dmsConfiguration_ = dmsConfiguration;
  }

  /**
   * {@inheritDoc}
   */
  public void start() {
    try {      
      init();
    } catch (Exception e) {
      LOG.error("Unexpected error", e);
    }    
  }

  /**
   * {@inheritDoc}
   */
  public void stop() {}

  /**
   * Add TemplatePlugin 
   * @param plugin
   */
  public void addPlugins(ComponentPlugin plugin) {
    if (plugin instanceof TemplatePlugin) plugins_.add((TemplatePlugin) plugin);    
  }

  /**
   * Call all available in list of TemplatePlugin to 
   * add some predefine template to all repository got 
   * from configuration
   * @throws Exception
   */
  private void init() throws Exception{    
    for(TemplatePlugin plugin : plugins_) {
      try {
        plugin.setBasePath(baseMetadataPath_);
        plugin.init();
      } catch(Exception e) {
        LOG.error("Unexpected error", e);
      }
    }
  }

  /**
   * {@inheritDoc}
   */
  public void init(String repository) throws Exception {    
    for(TemplatePlugin plugin : plugins_) {
      try {
        plugin.setBasePath(baseMetadataPath_);
        plugin.init(repository);
      } catch(Exception e) {
        LOG.error("Unexpected error", e);
      }
    }
  }

  /**
   * {@inheritDoc}
   */
  public String addMetadata(String nodetype, boolean isDialog, String role, String content, boolean isAddNew, String repository) throws Exception {
    Session session = getSession(repository);
    Node metadataHome = (Node)session.getItem(baseMetadataPath_);
    String path = null;
    if(!isAddNew) {
      if(isDialog) {
        Node dialog1 = metadataHome.getNode(nodetype).getNode(DIALOGS).getNode(DIALOG1);
        dialog1.setProperty(EXO_ROLES_PROP, role.split(";"));
        dialog1.setProperty(EXO_TEMPLATE_FILE_PROP, content);
        dialog1.save();
        path = dialog1.getPath();
      } else {
        Node view1 = metadataHome.getNode(nodetype).getNode(VIEWS).getNode(VIEW1);
        view1.setProperty(EXO_ROLES_PROP, role.split(";"));
        view1.setProperty(EXO_TEMPLATE_FILE_PROP, content);
        view1.save();
        path = view1.getPath();
      }      
    } else {
      Node metadata = null;
      if(metadataHome.hasNode(nodetype)) metadata = metadataHome.getNode(nodetype);
      else metadata = metadataHome.addNode(nodetype, NT_UNSTRUCTURED);
      addTemplate(metadata, role, content, isDialog);
      metadataHome.save();
    }    
    session.save(); 
    session.logout();
    return path;
  }

  /**
   * Add new node named nodetype
   * And child node for dialog template node or view template node
   * Set property EXO_ROLES_PROP, EXO_TEMPLATE_FILE_PROP for child node
   * @param nodetype    Node name for processing
   * @param isDialog    true for dialog template
   * @param role        permission
   * @param content     content of template
   * @throws Exception
   */
  private void addTemplate(Node nodetype, String role, String content, boolean isDialog) throws Exception {
    Node templateHome = createTemplateHome(nodetype, isDialog);
    Node template = null;
    if(isDialog) {
      if(templateHome.hasNode(DIALOG1)) template = templateHome.getNode(DIALOG1);
      else template = templateHome.addNode(DIALOG1, EXO_TEMPLATE);
    } else {
      if(templateHome.hasNode(VIEW1)) template = templateHome.getNode(VIEW1);
      else template = templateHome.addNode(VIEW1, EXO_TEMPLATE);
    }    
    String[] arrRoles = {};
    if(role != null) arrRoles = role.split(";");
    
    template.setProperty(EXO_ROLES_PROP, arrRoles);
    template.setProperty(EXO_TEMPLATE_FILE_PROP, content);
  }

  /**
   * {@inheritDoc}
   */
  public void removeMetadata(String nodetype, String repository) throws Exception {    
    Session session = getSession(repository);
    Node metadataHome = (Node)session.getItem(baseMetadataPath_);
    Node metadata = metadataHome.getNode(nodetype); 
    metadata.remove();
    metadataHome.save();
    session.save();
    session.logout();
  } 

  /**
   * {@inheritDoc}
   */
  public List<String> getMetadataList(String repository) throws Exception {
    List<String> metadataTypes = new ArrayList<String>();
    for(NodeType metadata:getAllMetadatasNodeType(repository)) {
      metadataTypes.add(metadata.getName());
    }
    return metadataTypes;
  }

  /**
   * {@inheritDoc}
   */
  public List<NodeType> getAllMetadatasNodeType(String repository) throws Exception {
    List<NodeType> metadataTypes = new ArrayList<NodeType>();    
    ExtendedNodeTypeManager ntManager = repositoryService_.getRepository(repository).getNodeTypeManager();     
    NodeTypeIterator ntIter = ntManager.getMixinNodeTypes();
    while(ntIter.hasNext()) {
      NodeType nt = ntIter.nextNodeType();
      if(nt.isNodeType(METADATA_TYPE) && !nt.getName().equals(METADATA_TYPE)) metadataTypes.add(nt);
    }
    return metadataTypes;
  }

  
  /**
   * Create node for Dialog template or view template
   * @param nodetype    Node name for processing
   * @param isDialog    true for dialog template, false for view template
   * @return            Node for dialog template if isDialog = true
   *                    Node for dialog template if isDialog = false    
   * @throws Exception
   */
  private Node createTemplateHome(Node nodetype, boolean isDialog) throws Exception{
    if(isDialog) {
      Node dialogs = null;
      if(nodetype.hasNode(DIALOGS)) dialogs = nodetype.getNode(DIALOGS);      
      else dialogs = nodetype.addNode(DIALOGS, NT_UNSTRUCTURED);
      return dialogs;
    }
    Node views = null;
    if(nodetype.hasNode(VIEWS)) views = nodetype.getNode(VIEWS);      
    else views = nodetype.addNode(VIEWS, NT_UNSTRUCTURED);
    return views;    
  }

  /**
   * {@inheritDoc}
   */
  public String getMetadataTemplate(String name, boolean isDialog, String repository) throws Exception {
    Session session = getSession(repository);
    Node metadataHome = (Node)session.getItem(baseMetadataPath_);
    Node template = null;
    if(!hasMetadata(name, repository)) return null;
    if(isDialog) template = metadataHome.getNode(name).getNode(DIALOGS).getNode(DIALOG1);
    else template = metadataHome.getNode(name).getNode(VIEWS).getNode(VIEW1);
    session.logout();
    return template.getProperty(EXO_TEMPLATE_FILE_PROP).getString();
  }

  /**
   * {@inheritDoc}
   */
  public String getMetadataPath(String name, boolean isDialog, String repository) throws Exception {
    Session session = getSession(repository);
    Node metadataHome = (Node)session.getItem(baseMetadataPath_);
    if(!hasMetadata(name, repository)) return null;
    Node template = null;
    if(isDialog){
      template = metadataHome.getNode(name).getNode(DIALOGS).getNode(DIALOG1);
    } else {
      template = metadataHome.getNode(name).getNode(VIEWS).getNode(VIEW1);
    }
    session.logout();
    return template.getPath();
  }

  /**
   * {@inheritDoc}
   */
  public String getMetadataRoles(String name, boolean isDialog, String repository) throws Exception {
    Session session = getSession(repository);
    Node metadataHome = (Node)session.getItem(baseMetadataPath_);
    Node template = null;
    if(!hasMetadata(name, repository)) return null;
    if(isDialog){
      template = metadataHome.getNode(name).getNode(DIALOGS).getNode(DIALOG1);
    } else {
      template = metadataHome.getNode(name).getNode(VIEWS).getNode(VIEW1);
    }
    Value[] values = template.getProperty(EXO_ROLES_PROP).getValues();
    StringBuffer roles = new StringBuffer();
    for(int i = 0; i < values.length; i ++ ){
      if(roles.length() > 0 ) roles.append(";");
      roles.append(values[i].getString());
    }
    session.logout();
    return roles.toString();
  }  

  /**
   * {@inheritDoc}
   */
  public boolean hasMetadata(String name, String repository) throws Exception {
    Session session = getSession(repository);
    Node metadataHome = (Node)session.getItem(baseMetadataPath_);
    if(metadataHome.hasNode(name)) {
      session.logout();
      return true; 
    }
    session.logout();
    return false; 
  }

  /**
   * {@inheritDoc}
   */
  public List<String> getExternalMetadataType(String repository) throws Exception {
    List<String> extenalMetaTypes = new ArrayList<String>();
    for(NodeType metadata: getAllMetadatasNodeType(repository)) {      
      for(PropertyDefinition pro : metadata.getPropertyDefinitions()) {
    	  if(pro.getName().equals(INTERNAL_USE)) {
    		  if(!pro.getDefaultValues()[0].getBoolean() && !metadata.getName().equals(METADATA_TYPE))
    			  extenalMetaTypes.add(metadata.getName());
    		  break;
    	  }
      }
    }
    
    return extenalMetaTypes;
  }

  /**
   * Get session of respository
   * @param repository    The name of repository
   * @see                 Session 
   * @return              Session
   * @throws Exception
   */
  private Session getSession(String repository) throws Exception{ 
    ManageableRepository manageableRepository = repositoryService_.getRepository(repository);
    DMSRepositoryConfiguration dmsRepoConfig = dmsConfiguration_.getConfig(repository);
    return manageableRepository.getSystemSession(dmsRepoConfig.getSystemWorkspace());
  }
}