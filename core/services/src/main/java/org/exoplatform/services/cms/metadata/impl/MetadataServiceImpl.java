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

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.Session;
import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.NodeTypeIterator;
import javax.jcr.nodetype.PropertyDefinition;

import org.exoplatform.container.component.ComponentPlugin;
import org.exoplatform.services.cms.BasePath;
import org.exoplatform.services.cms.impl.DMSConfiguration;
import org.exoplatform.services.cms.impl.DMSRepositoryConfiguration;
import org.exoplatform.services.cms.metadata.MetadataService;
import org.exoplatform.services.cms.templates.TemplateService;
import org.exoplatform.services.cms.templates.impl.TemplatePlugin;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.jcr.core.nodetype.ExtendedNodeTypeManager;
import org.exoplatform.services.jcr.ext.hierarchy.NodeHierarchyCreator;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;
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
   * Property name INTERNAL_USE
   */
  final static public String INTERNAL_USE = "exo:internalUse";

  /**
   * NodeType METADATA_TYPE
   */
  final static public String METADATA_TYPE = "exo:metadata";

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

  private TemplateService templateService;

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
    templateService = WCMCoreUtils.getService(TemplateService.class);
  }

  /**
   * {@inheritDoc}
   */
  public void start() {
    try {
      init();
    } catch (Exception e) {
      if (LOG.isErrorEnabled()) {
        LOG.error("Unexpected error", e);
      }
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
  public void init() throws Exception{
    for(TemplatePlugin plugin : plugins_) {
      try {
        plugin.setBasePath(baseMetadataPath_);
        plugin.init();
      } catch(Exception e) {
        if (LOG.isErrorEnabled()) {
          LOG.error("Unexpected error", e);
        }
      }
    }
  }

  /**
   * {@inheritDoc}
   * @deprecated Since WCM 2.1-CLOUD-DEV you should use {@link #init()} instead.
   */
  @Deprecated
  public void init(String repository) throws Exception {
    init();
  }

  /**
   * {@inheritDoc}
   */
  @Deprecated
  public String addMetadata(String nodetype,
                            boolean isDialog,
                            String role,
                            String content,
                            boolean isAddNew,
                            String repository) throws Exception {
    return addMetadata(nodetype, isDialog, role, content, isAddNew);
  }
  
  /**
   * {@inheritDoc}
   */
  public String addMetadata(String nodetype,
                            boolean isDialog,
                            String role,
                            String content,
                            boolean isAddNew) throws Exception {
    Session session = getSession();
    Node metadataHome = (Node)session.getItem(baseMetadataPath_);
    String path = null;
    if(!isAddNew) {
      if(isDialog) {
        Node dialog1 = metadataHome.getNode(nodetype).getNode(DIALOGS).getNode(DIALOG1);
        path = templateService.updateTemplate(dialog1, new ByteArrayInputStream(content.getBytes()), role.split(";"));
      } else {
        Node view1 = metadataHome.getNode(nodetype).getNode(VIEWS).getNode(VIEW1);
        path = templateService.updateTemplate(view1, new ByteArrayInputStream(content.getBytes()), role.split(";"));
      }
    } else {
      Node metadata = null;
      if(metadataHome.hasNode(nodetype)) metadata = metadataHome.getNode(nodetype);
      else metadata = metadataHome.addNode(nodetype, NT_UNSTRUCTURED);
      addTemplate(metadata, role, new ByteArrayInputStream(content.getBytes()), isDialog);
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
  private void addTemplate(Node nodetype, String role, InputStream content, boolean isDialog) throws Exception {
    Node templateHome = createTemplateHome(nodetype, isDialog);
    String[] arrRoles = {};
    if(role != null) arrRoles = role.split(";");
    if(isDialog) {
      templateService.createTemplate(templateHome, DIALOG1, content, arrRoles);
    } else {
      templateService.createTemplate(templateHome, VIEW1, content, arrRoles);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Deprecated
  public void removeMetadata(String nodetype, String repository) throws Exception {
    removeMetadata(nodetype);
  }

  /**
   * {@inheritDoc}
   */
  public void removeMetadata(String nodetype) throws Exception {
    Session session = getSession();
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
  @Deprecated
  public List<String> getMetadataList(String repository) throws Exception {
    return getMetadataList();
  }

  /**
   * {@inheritDoc}
   */
  public List<String> getMetadataList() throws Exception {
    List<String> metadataTypes = new ArrayList<String>();
    for(NodeType metadata:getAllMetadatasNodeType()) {
      metadataTypes.add(metadata.getName());
    }
    return metadataTypes;
  }
  
  /**
   * {@inheritDoc}
   */
  @Deprecated
  public List<NodeType> getAllMetadatasNodeType(String repository) throws Exception {
    return getAllMetadatasNodeType();
  }
  
  /**
   * {@inheritDoc}
   */
  public List<NodeType> getAllMetadatasNodeType() throws Exception {
    List<NodeType> metadataTypes = new ArrayList<NodeType>();
    ExtendedNodeTypeManager ntManager = repositoryService_.getCurrentRepository().getNodeTypeManager();
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
  @Deprecated
  public String getMetadataTemplate(String name, boolean isDialog, String repository) throws Exception {
    return getMetadataTemplate(name, isDialog);
  }
  
  /**
   * {@inheritDoc}
   */
  public String getMetadataTemplate(String name, boolean isDialog) throws Exception {
    Session session = getSession();
    Node metadataHome = (Node)session.getItem(baseMetadataPath_);
    Node template = null;
    if(!hasMetadata(name)) return null;
    if(isDialog) template = metadataHome.getNode(name).getNode(DIALOGS).getNode(DIALOG1);
    else template = metadataHome.getNode(name).getNode(VIEWS).getNode(VIEW1);
    String ret = templateService.getTemplate(template);
    session.logout();
    return ret;
  }  

  /**
   * {@inheritDoc}
   */
  @Deprecated
  public String getMetadataPath(String name, boolean isDialog, String repository) throws Exception {
    return getMetadataPath(name, isDialog);
  }
  
  /**
   * {@inheritDoc}
   */
  public String getMetadataPath(String name, boolean isDialog) throws Exception {
    Session session = getSession();
    Node metadataHome = (Node)session.getItem(baseMetadataPath_);
    if(!hasMetadata(name)) return null;
    Node template = null;
    if(isDialog){
      template = metadataHome.getNode(name).getNode(DIALOGS).getNode(DIALOG1);
    } else {
      template = metadataHome.getNode(name).getNode(VIEWS).getNode(VIEW1);
    }
    String ret = template.getPath();
    session.logout();
    return ret;
  }  

  /**
   * {@inheritDoc}
   */
  @Deprecated
  public String getMetadataRoles(String name, boolean isDialog, String repository) throws Exception {
    return getMetadataRoles(name, isDialog);
  }
  
  /**
   * {@inheritDoc}
   */
  public String getMetadataRoles(String name, boolean isDialog) throws Exception {
    Session session = getSession();
    Node metadataHome = (Node)session.getItem(baseMetadataPath_);
    Node template = null;
    if(!hasMetadata(name)) return null;
    if(isDialog){
      template = metadataHome.getNode(name).getNode(DIALOGS).getNode(DIALOG1);
    } else {
      template = metadataHome.getNode(name).getNode(VIEWS).getNode(VIEW1);
    }
    String ret = templateService.getTemplateRoles(template);
    session.logout();
    return ret;
  }  

  /**
   * {@inheritDoc}
   */
  @Deprecated
  public boolean hasMetadata(String name, String repository) throws Exception {
    return hasMetadata(name);
  }
  
  /**
   * {@inheritDoc}
   */
  public boolean hasMetadata(String name) throws Exception {
    Session session = getSession();
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
  @Deprecated
  public List<String> getExternalMetadataType(String repository) throws Exception {
    return getExternalMetadataType();
  }
  
  /**
   * {@inheritDoc}
   */
  public List<String> getExternalMetadataType() throws Exception {
    List<String> extenalMetaTypes = new ArrayList<String>();
    for (NodeType metadata : getAllMetadatasNodeType()) {
      for (PropertyDefinition pro : metadata.getPropertyDefinitions()) {
        if (pro.getName().equals(INTERNAL_USE)) {
          if (!pro.getDefaultValues()[0].getBoolean() && !metadata.getName().equals(METADATA_TYPE))
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
  private Session getSession() throws Exception{
    ManageableRepository manageableRepository = repositoryService_.getCurrentRepository();
    DMSRepositoryConfiguration dmsRepoConfig = dmsConfiguration_.getConfig();
    return manageableRepository.getSystemSession(dmsRepoConfig.getSystemWorkspace());
  }
}
