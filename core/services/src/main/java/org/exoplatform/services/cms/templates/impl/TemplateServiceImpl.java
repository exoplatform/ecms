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
package org.exoplatform.services.cms.templates.impl;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.security.AccessControlException;
import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.jcr.AccessDeniedException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.nodetype.NodeType;

import org.exoplatform.container.component.ComponentPlugin;
import org.exoplatform.services.cache.CacheService;
import org.exoplatform.services.cache.ExoCache;
import org.exoplatform.services.cms.BasePath;
import org.exoplatform.services.cms.impl.DMSConfiguration;
import org.exoplatform.services.cms.impl.DMSRepositoryConfiguration;
import org.exoplatform.services.cms.impl.Utils;
import org.exoplatform.services.cms.templates.ContentTypeFilterPlugin;
import org.exoplatform.services.cms.templates.TemplateService;
import org.exoplatform.services.cms.templates.ContentTypeFilterPlugin.FolderFilterConfig;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.access.SystemIdentity;
import org.exoplatform.services.jcr.config.RepositoryEntry;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.jcr.ext.hierarchy.NodeHierarchyCreator;
import org.exoplatform.services.jcr.impl.core.nodetype.NodeTypeImpl;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.resources.LocaleConfigService;
import org.exoplatform.services.resources.Orientation;
import org.exoplatform.services.security.Identity;
import org.exoplatform.services.security.IdentityRegistry;
import org.exoplatform.services.security.MembershipEntry;
import org.exoplatform.services.wcm.core.NodetypeConstant;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;
import org.picocontainer.Startable;

/**
 * @author benjaminmestrallet
 */
public class TemplateServiceImpl implements TemplateService, Startable {
  private RepositoryService    repositoryService_;
  private IdentityRegistry identityRegistry_;
  private String               cmsTemplatesBasePath_;
  private List<TemplatePlugin> plugins_ = new ArrayList<TemplatePlugin>();

  private Map<String,HashMap<String,List<String>>> foldersFilterMap = new HashMap<String,HashMap<String,List<String>>> ();  
  private Map<String,List<String>> managedDocumentTypesMap = new HashMap<String,List<String>>();
  private org.exoplatform.groovyscript.text.TemplateService templateService;
  private LocaleConfigService localeConfigService_;
  
  private static final Log LOG  = ExoLogger.getLogger(TemplateService.class.getName());
  /**
   * DMS configuration which used to store informations
   */   
  private DMSConfiguration dmsConfiguration_;
  
  private static String NODETYPE_LIST = "nodeTypeList";
  
  @SuppressWarnings("unchecked")
  private ExoCache nodeTypeListCached ;
  
  /**
   * Constructor method
   * Init jcrService, nodeHierarchyCreator, identityRegistry, localeConfigService, caService, 
   * dmsConfiguration
   * @param jcrService              RepositoryService
   * @param nodeHierarchyCreator    NodeHierarchyCreator
   * @param identityRegistry        IdentityRegistry
   * @param localeConfigService     LocaleConfigService
   * @param caService               CacheService
   * @param dmsConfiguration        DMSConfiguration
   * @throws Exception
   */
  public TemplateServiceImpl(RepositoryService jcrService,
      NodeHierarchyCreator nodeHierarchyCreator, IdentityRegistry identityRegistry,
      org.exoplatform.groovyscript.text.TemplateService templateService, 
      DMSConfiguration dmsConfiguration, LocaleConfigService localeConfigService,
      CacheService caService) throws Exception {
    identityRegistry_ = identityRegistry;
    repositoryService_ = jcrService;
    cmsTemplatesBasePath_ = nodeHierarchyCreator.getJcrPath(BasePath.CMS_TEMPLATES_PATH);
    this.templateService = templateService;
    dmsConfiguration_ = dmsConfiguration;
    localeConfigService_ = localeConfigService;
    nodeTypeListCached = caService.getCacheInstance(TemplateService.class.getName());
  }
  
  /**
   * {@inheritDoc}
   */
  public void start() {
    try {
      for (TemplatePlugin plugin : plugins_) {
        plugin.init();
      }

      //Cached all nodetypes that is document type in the map
      for(RepositoryEntry repositoryEntry:repositoryService_.getConfig().getRepositoryConfigurations()) {
        String repositoryName = repositoryEntry.getName();
        List<String> managedContentTypes = getAllDocumentNodeTypes(repositoryEntry.getName());
        if(managedContentTypes.size() != 0) {
          managedDocumentTypesMap.put(repositoryName,managedContentTypes);
        }
      }
    } catch (Exception e) {
      LOG.error("An unexpected exception occurs when init plugins", e);
    }
  }
  
  /**
   * {@inheritDoc}
   */
  public void stop() {
  }
  
  /**
   * {@inheritDoc}
   */
  public void addContentTypeFilterPlugin(ContentTypeFilterPlugin filterPlugin) {
    String repository = filterPlugin.getRepository();
    HashMap<String,List<String>> folderFilterMap = foldersFilterMap.get(repository); 
    if(folderFilterMap == null) {
      folderFilterMap = new HashMap<String,List<String>>();
    }    
    for(FolderFilterConfig filterConfig: filterPlugin.getFolderFilterConfigList()) {      
      String folderType = filterConfig.getFolderType();
      List<String> contentTypes = filterConfig.getContentTypes();
      List<String> value = folderFilterMap.get(folderType);
      if(value == null) {
        folderFilterMap.put(folderType,contentTypes);
      }else {
        value.addAll(contentTypes);
        folderFilterMap.put(folderType,value);
      }
    }
    foldersFilterMap.put(repository,folderFilterMap);
  }
  
  /**
   * {@inheritDoc}
   */
  public Set<String> getAllowanceFolderType(String repository) {
    Map<String,List<String>> folderFilterMap = foldersFilterMap.get(repository);
    if (folderFilterMap == null) folderFilterMap = foldersFilterMap.get("repository");
    return folderFilterMap.keySet();
  }

  /**
   * {@inheritDoc}
   */
  public void addTemplates(ComponentPlugin plugin) {
    if (plugin instanceof TemplatePlugin)
      plugins_.add((TemplatePlugin) plugin);
  }
  
  /**
   * {@inheritDoc}
   */
  public void init(String repository) throws Exception {
    for (TemplatePlugin plugin : plugins_) {
      plugin.init(repository);
    }
  }

  /**
   * {@inheritDoc}
   */
  public Node getTemplatesHome(String repository, SessionProvider provider) throws Exception {
    try {
      Session session = getSession(repository, provider);
      return (Node) session.getItem(cmsTemplatesBasePath_);
    } catch (AccessDeniedException ace) {
      LOG.error("Access denied. You can not access to this template");
      return null;
    }
  }

  /**
   * {@inheritDoc}
   */
  public List<String> getCreationableContentTypes(Node node) throws Exception {
    String folderType = node.getPrimaryNodeType().getName();    
    String repository = ((ManageableRepository)node.getSession().getRepository()).getConfiguration().getName();
    List<String> testContentTypes = null;    
    HashMap<String,List<String>> folderFilterMap = foldersFilterMap.get(repository);
    if(folderFilterMap != null) {
      List<String> list = folderFilterMap.get(folderType);
      if(list != null && list.size() != 0) {
        testContentTypes = list;
      }
    }
    if(testContentTypes == null) {
      testContentTypes = getDocumentTemplates(repository);
    }    
    List<String> result = new ArrayList<String>();
    for(String contentType: testContentTypes) {
      if(isChildNodePrimaryTypeAllowed(node,contentType)) {
        if (!folderType.equals(contentType))                  //When content type is not parent node's content type
          result.add(contentType);
      }
    }            
    return result;
  }

  /**
   * {@inheritDoc}
   */
  public boolean isChildNodePrimaryTypeAllowed(Node parent, String childNodeTypeName) throws Exception{
    NodeType childNodeType = parent.getSession().getWorkspace().getNodeTypeManager().getNodeType(childNodeTypeName);
    //In some cases, the child node is mixins type of a nt:file example
    if(childNodeType.isMixin()) return true;    
    List<NodeType> allNodeTypes = new ArrayList<NodeType>();
    allNodeTypes.add((NodeType)parent.getPrimaryNodeType());
    for(NodeType mixin: parent.getMixinNodeTypes()) {
      allNodeTypes.add((NodeType)mixin);
    }
    for (NodeType nodetype:allNodeTypes) {      
    	if (((NodeTypeImpl)nodetype).isChildNodePrimaryTypeAllowed(childNodeTypeName)) {
        return true;
      } 
    }
    return false;
  }

  /**
   * {@inheritDoc}
   */
  public boolean isManagedNodeType(String nodeTypeName, String repository) throws Exception {
    //check if the node type is document type first
    List<String> managedDocumentTypes = managedDocumentTypesMap.get(repository);
    if(managedDocumentTypes != null && managedDocumentTypes.contains(nodeTypeName)) 
      return true;
    SessionProvider provider = SessionProvider.createSystemProvider();
    Session session = getSession(repository, provider);
    Node systemTemplatesHome = (Node) session.getItem(cmsTemplatesBasePath_);
    boolean b = false;
    if (systemTemplatesHome.hasNode(nodeTypeName)) {
      b = true;
    }
    provider.close();
    return b;
  }

  /**
   * {@inheritDoc}
   */
  public String getTemplatePath(Node node, boolean isDialog) throws Exception {
    String userId = node.getSession().getUserID();
    String repository = 
      ((ManageableRepository) node.getSession().getRepository()).getConfiguration().getName();
    String templateType = null;
    if (node.isNodeType("exo:presentationable") && node.hasProperty("exo:presentationType")) {
      templateType = node.getProperty("exo:presentationType").getString();
    } else if (node.isNodeType("nt:frozenNode")) {
      templateType = node.getProperty("jcr:frozenPrimaryType").getString();
    } else {
      templateType = node.getPrimaryNodeType().getName();
    }
    if (isManagedNodeType(templateType, repository))
      return getTemplatePathByUser(isDialog, templateType, userId, repository);
    throw new Exception("The content type: " + templateType + " isn't supported by any template");
  }
  
  /**
   * {@inheritDoc}
   */
  public NodeIterator getAllTemplatesOfNodeType(boolean isDialog, String nodeTypeName,
      String repository, SessionProvider provider) throws Exception {
    Node nodeTypeHome = getTemplatesHome(repository, provider).getNode(nodeTypeName);
    if (isDialog) {
      if(!nodeTypeHome.hasNode(DIALOGS)) return null;
      return nodeTypeHome.getNode(DIALOGS).getNodes();
    }
    if(!nodeTypeHome.hasNode(VIEWS)) return null;
    return nodeTypeHome.getNode(VIEWS).getNodes();
  }

  /**
   * {@inheritDoc}
   */
  public String getDefaultTemplatePath(boolean isDialog, String nodeTypeName) {
    if (isDialog)
      return cmsTemplatesBasePath_ + "/" + nodeTypeName + DEFAULT_DIALOGS_PATH;
    return cmsTemplatesBasePath_ + "/" + nodeTypeName + DEFAULT_VIEWS_PATH;
  }

  /**
   * {@inheritDoc}
   */
  public Node getTemplateNode(String type, String nodeTypeName, String templateName,
      String repository, SessionProvider provider) throws Exception {
    Node nodeTypeNode = getTemplatesHome(repository, provider).getNode(nodeTypeName);
    return nodeTypeNode.getNode(type).getNode(templateName);
  }

  /**
   * {@inheritDoc}
   */
  public String getTemplatePathByUser(boolean isDialog, String nodeTypeName, String userName,
      String repository) throws Exception {
    if(SystemIdentity.ANONIM.equals(userName) || userName == null) {
      return getTemplatePathByAnonymous(isDialog, nodeTypeName, repository);
    }
    Session session = getSession(repository);
    Node templateHomeNode = (Node) session.getItem(cmsTemplatesBasePath_);
    String type = DIALOGS;
    if (!isDialog)
      type = VIEWS;
    Node nodeTypeNode = templateHomeNode.getNode(nodeTypeName);
    NodeIterator templateIter = nodeTypeNode.getNode(type).getNodes();
    while (templateIter.hasNext()) {
      Node node = templateIter.nextNode();
      String roles = getTemplateRoles(node);
      if(hasPermission(userName, roles, identityRegistry_)) {
        String templatePath = node.getPath() ;
        session.logout();
        return templatePath ;
      }
    }
    session.logout();
    throw new AccessControlException("You don't have permission to access any template");
  }

  /**
   * {@inheritDoc}
   */
  public String getTemplatePath(boolean isDialog, String nodeTypeName, String templateName,
      String repository) throws Exception {
    Session session = getSession(repository);
    String type = DIALOGS;
    if (!isDialog)
      type = VIEWS;
    Node templateNode = getTemplateNode(session, type, nodeTypeName, templateName);
    String path = templateNode.getPath();
    session.logout();
    return path;
  }

  /**
   * {@inheritDoc}
   */
  public String getTemplateLabel(String nodeTypeName, String repository) throws Exception {
    SessionProvider provider = SessionProvider.createSystemProvider();
    Node templateHome = getTemplatesHome(repository, provider);
    Node nodeType = templateHome.getNode(nodeTypeName);
    String label = "";
    if (nodeType.hasProperty("label")) {
      label = nodeType.getProperty("label").getString();
    }
    provider.close();
    return label;
  }

  /**
   * {@inheritDoc}
   */
  public String getTemplate(String type, String nodeTypeName, String templateName,
      String repository) throws Exception {
    Session session = getSession(repository);
    Node templateNode = getTemplateNode(session, type, nodeTypeName, templateName);
    session.logout();
    return getTemplate(templateNode);
  }

  /**
   * {@inheritDoc}
   */
  @Deprecated
  public String getTemplateRoles(String type, String nodeTypeName, String templateName,
      String repository) throws Exception {
    Session session = getSession(repository);
    Node templateNode = getTemplateNode(session, type, nodeTypeName, templateName);
    session.logout();
    return getTemplateRoles(templateNode);
  }

  /**
   * {@inheritDoc}
   */
  public void removeTemplate(String type, String nodeTypeName, String templateName,
      String repository) throws Exception {
    Session session = getSession(repository);
    Node templatesHome = (Node) session.getItem(cmsTemplatesBasePath_);
    Node nodeTypeHome = templatesHome.getNode(nodeTypeName);
    Node specifiedTemplatesHome = nodeTypeHome.getNode(type);
    Node contentNode = specifiedTemplatesHome.getNode(templateName);
    contentNode.remove();
    nodeTypeHome.save();
    session.save();
    session.logout();
  }

  /**
   * {@inheritDoc}
   */
  public void removeManagedNodeType(String nodeTypeName, String repository) throws Exception {
    Session session = getSession(repository);
    Node templatesHome = (Node) session.getItem(cmsTemplatesBasePath_);
    Node managedNodeType = templatesHome.getNode(nodeTypeName);
    managedNodeType.remove();    
    session.save();
    session.logout();
    //Update managedDocumentTypeMap
    List<String> managedDocumentTypes = managedDocumentTypesMap.get(repository);
    managedDocumentTypes.remove(nodeTypeName);
    removeTemplateNodeTypeList();
  }

  /**
   * {@inheritDoc}
   */
  @Deprecated
  public String addTemplate(boolean isDialog, String nodeTypeName, String label,
      boolean isDocumentTemplate, String templateName, String[] roles, String templateFile,
      String repository) throws Exception {
    Session session = getSession(repository);
    Node templatesHome = (Node) session.getItem(cmsTemplatesBasePath_);
    String templateType = DIALOGS;
    if(!isDialog) templateType = VIEWS;
    String templatePath = getContentNode(templateType, templatesHome, nodeTypeName, label, 
        isDocumentTemplate, templateName, roles, new ByteArrayInputStream(templateFile.getBytes()));
    templatesHome.save();
    session.save();
    session.logout();
    //Update managedDocumentTypesMap
    removeCacheTemplate(templatePath);
    removeTemplateNodeTypeList();
    updateDocumentsTemplate(isDocumentTemplate, repository, nodeTypeName);
    return templatePath;
  }

  /**
   * {@inheritDoc}
   */
  @Deprecated
  public String addTemplate(String templateType, String nodeTypeName, String label,
      boolean isDocumentTemplate, String templateName, String[] roles, String templateFile,
      String repository) throws Exception {
    return addTemplate(templateType, nodeTypeName, label, isDocumentTemplate, templateName, roles, new ByteArrayInputStream(templateFile.getBytes()), repository);
  }
  
  /**
   * {@inheritDoc}
   */
  public List<String> getDocumentTemplates(String repository) throws Exception {    
    List<String> templates = managedDocumentTypesMap.get(repository);
    if(templates != null) 
      return templates;
    templates = getAllDocumentNodeTypes(repository);
    managedDocumentTypesMap.put(repository,templates);
    return templates;    
  }

  /**
   * {@inheritDoc}
   */
  public String getTemplatePathByAnonymous(boolean isDialog, String nodeTypeName, String repository) throws Exception {
    Session session = getSession(repository);
    String type = DIALOGS;
    if (!isDialog)
      type = VIEWS;
    Node homeNode = (Node) session.getItem(cmsTemplatesBasePath_);
    Node nodeTypeNode = homeNode.getNode(nodeTypeName);
    NodeIterator templateIter = nodeTypeNode.getNode(type).getNodes();
    while (templateIter.hasNext()) {
      Node node = templateIter.nextNode();
      String role = getTemplateRoles(node);
      if(hasPublicTemplate(role)) {
        String templatePath = node.getPath() ;
        session.logout();
        return templatePath ;
      }
    }
    session.logout();
    return null;
  }
  
  /**
   * {@inheritDoc}
   */
  public void removeCacheTemplate(String name) throws Exception {
    templateService.reloadTemplate(name);
  }
  
  /**
   * {@inheritDoc}
   */
  public void removeAllTemplateCached() {
    templateService.reloadTemplates();
  }
  
  /**
   * {@inheritDoc}
   */
  @SuppressWarnings("unchecked")
  public List<String> getAllDocumentNodeTypes(String repository) throws Exception {
    List<String> nodeTypeList = (List<String>)nodeTypeListCached.get(NODETYPE_LIST);
    if(nodeTypeList != null && nodeTypeList.size() > 0) return nodeTypeList;
    
    List<String> contentTypes = new ArrayList<String>();
    Session session = getSession(repository);
    Node templatesHome = (Node) session.getItem(cmsTemplatesBasePath_);
    for (NodeIterator templateIter = templatesHome.getNodes(); templateIter.hasNext();) {
      Node template = templateIter.nextNode();
      if (template.getProperty(DOCUMENT_TEMPLATE_PROP).getBoolean())
        contentTypes.add(template.getName());
    }
    session.logout();
    nodeTypeListCached.put(NODETYPE_LIST, contentTypes);
    return contentTypes;
  }  
  
  /**
   * {@inheritDoc}
   */
  public String getSkinPath(String nodeTypeName, String skinName, String locale, String repository) throws Exception {
    Session session = getSession(repository);
    Node homeNode = (Node) session.getItem(cmsTemplatesBasePath_);
    Node nodeTypeNode = homeNode.getNode(nodeTypeName);
    Orientation orientation = getOrientation(locale);
    String skinPath = null;
    if(orientation.isLT()) {
      StringBuilder templateData = new StringBuilder("/**");
      templateData.append("LTR stylesheet for "+nodeTypeNode.getName()+" template").append("*/");
      skinPath = addNewSkinNode(homeNode, nodeTypeNode, skinName, "-lt", templateData.toString());
    } else if(orientation.isRT()) {
      StringBuilder templateData = new StringBuilder("/**");
      templateData.append("RTL stylesheet for "+nodeTypeNode.getName()+" template").append("*/");
      skinPath = addNewSkinNode(homeNode, nodeTypeNode, skinName, "-rt", templateData.toString());
    }
    session.logout();
    return skinPath;
  }  
  
  /**
   * {@inheritDoc}
   */
  public String buildDialogForm(String nodeTypeName, String repository) throws Exception {
    if (plugins_ == null && plugins_.size() == 0) throw new Exception("Cannot find plugin for template");
    TemplatePlugin templatePlugin = plugins_.get(0);
    ManageableRepository manageRepo = repositoryService_.getRepository(repository);
    NodeType nodeType = manageRepo.getNodeTypeManager().getNodeType(nodeTypeName);
    return templatePlugin.buildDialogForm(nodeType);
  }

  /**
   * {@inheritDoc}
   */
  public String buildViewForm(String nodeTypeName, String repository) throws Exception {
    if (plugins_ == null && plugins_.size() == 0) throw new Exception("Cannot find plugin for template");
    TemplatePlugin templatePlugin = plugins_.get(0);
    ManageableRepository manageRepo = repositoryService_.getRepository(repository);
    NodeType nodeType = manageRepo.getNodeTypeManager().getNodeType(nodeTypeName);
    return templatePlugin.buildViewForm(nodeType);
  }

  /**
   * {@inheritDoc}
   */
  public String buildStyleSheet(String nodeTypeName, String repository) throws Exception {
    if (plugins_ == null && plugins_.size() == 0) throw new Exception("Cannot find plugin for template");
    TemplatePlugin templatePlugin = plugins_.get(0);
    return templatePlugin.buildStyleSheet(null);
  }
  
  /**
   * Get template with the following specified params 
   * @param session         Session         
   * @param templateType    String
   *                        The value of template type
   * @param nodeTypeName    String
   *                        The name of NodeType
   * @param templateName    String
   *                        The name of template
   * @param repository      String
   *                        The name of repository    
   * @return
   * @throws Exception
   */
  private Node getTemplateNode(Session session, String type, String nodeTypeName,
      String templateName) throws Exception {
    Node homeNode = (Node) session.getItem(cmsTemplatesBasePath_);
    Node nodeTypeNode = homeNode.getNode(nodeTypeName);
    return nodeTypeNode.getNode(type).getNode(templateName);
  }
  
  /**
   * Get content of the specified node 
   * @param isDialog              boolean
   * @param templatesHome         Node
   * @param nodeTypeName          String
   *                              The name of NodeType
   * @param label                 String
   *                              The label of template
   * @param isDocumentTemplate    boolean
   * @param templateName          String
   *                              The name of template
   * @see                         Node                              
   * @return
   * @throws Exception
   */
  private String getContentNode(String templateType, Node templatesHome, String nodeTypeName, 
      String label, boolean isDocumentTemplate, String templateName, String[] roles, InputStream templateFile) throws Exception {
    Node nodeTypeHome = null;
    if (!templatesHome.hasNode(nodeTypeName)) {
      nodeTypeHome = Utils.makePath(templatesHome, nodeTypeName, NT_UNSTRUCTURED);
      if (isDocumentTemplate) {
        nodeTypeHome.setProperty(DOCUMENT_TEMPLATE_PROP, true);
      } else
        nodeTypeHome.setProperty(DOCUMENT_TEMPLATE_PROP, false);
      nodeTypeHome.setProperty(TEMPLATE_LABEL, label);
    } else {
      nodeTypeHome = templatesHome.getNode(nodeTypeName);
    }
    Node specifiedTemplatesHome = null;
    try {
      specifiedTemplatesHome = nodeTypeHome.getNode(templateType);
    } catch(PathNotFoundException e) {
      specifiedTemplatesHome = Utils.makePath(nodeTypeHome, templateType, NT_UNSTRUCTURED);
    }
    String templatePath = null;
    if (specifiedTemplatesHome.hasNode(templateName)) {
      templatePath = specifiedTemplatesHome.getNode(templateName).getPath();
    } else {
      templatePath = createTemplate(specifiedTemplatesHome, templateName, templateFile, roles);
    }
    templatesHome.save();
    templatesHome.getSession().save();
    return templatePath;
  }
  
  /**
   * Update document template
   * @param isDocumentTemplate    boolean
   * @param repository            String
   *                              The name of repository
   * @param nodeTypeName          String
   *                              The name of NodeType
   * @see                         Node
   * @see                         NodeType                              
   */
  private void updateDocumentsTemplate(boolean isDocumentTemplate, String repository, 
      String nodeTypeName) {
    if(isDocumentTemplate) {
      List<String> documentList = managedDocumentTypesMap.get(repository);
      if(documentList == null) {
        documentList = new ArrayList<String>();
        documentList.add(nodeTypeName);
        managedDocumentTypesMap.put(repository,documentList);
      } else {
        if(!documentList.contains(nodeTypeName)) {
          documentList.add(nodeTypeName);
          managedDocumentTypesMap.put(repository,documentList);
        } 
      }
    }    
  }
  
  /**
   * Return session of the specified repository
   * @param repository      String
   *                        The name of repository
   * @return
   * @see                   ManageableRepository
   * @see                   DMSRepositoryConfiguration
   * @throws Exception
   */
  private Session getSession(String repository) throws Exception {
    ManageableRepository manageableRepository = repositoryService_.getRepository(repository);
    DMSRepositoryConfiguration dmsRepoConfig = dmsConfiguration_.getConfig(repository);
    return manageableRepository.getSystemSession(dmsRepoConfig.getSystemWorkspace());
  }

  /**
   * Return session of the specified repository
   * @param repository      String
   *                        The name of repository
   * @param provider        SessionProvider                    
   * @return
   * @see                   SessionProvider
   * @see                   ManageableRepository
   * @see                   DMSRepositoryConfiguration
   * @throws Exception
   */
  private Session getSession(String repository, SessionProvider provider) throws Exception {
    ManageableRepository manageableRepository = repositoryService_.getRepository(repository);
    DMSRepositoryConfiguration dmsRepoConfig = dmsConfiguration_.getConfig(repository);
    return provider.getSession(dmsRepoConfig.getSystemWorkspace(), manageableRepository);
  }

  /**
   * Check permission of the user with roles
   * @param userId              String
   *                            The specified user
   * @param roles               Value[]
   * @param identityRegistry    IdentityRegistry
   * @see                       MembershipEntry
   * @see                       IdentityRegistry
   * @return
   * @throws Exception
   */
  private boolean hasPermission(String userId,String roles, IdentityRegistry identityRegistry) throws Exception {        
    if(SystemIdentity.SYSTEM.equalsIgnoreCase(userId)) {
      return true ;
    } 
    Identity identity = identityRegistry.getIdentity(userId) ;
    if(identity == null) {
      return false ; 
    }
    String[] listRoles = roles.split("; ");
    for (int i = 0; i < listRoles.length; i++) {
      String role = listRoles[i];
      if("*".equalsIgnoreCase(role)) return true ;
      MembershipEntry membershipEntry = MembershipEntry.parse(role) ;
      if(identity.isMemberOf(membershipEntry)) {
        return true ;
      }
    }
    return false ;
  }
  
  /**
   * Check public template with the roles
   * @param roles         Value[]
   * @return
   * @throws Exception
   */
  private boolean hasPublicTemplate(String role) throws Exception {
    String[] roles = role.split("; ");
    for (int i = 0; i < roles.length; i++) {
      if("*".equalsIgnoreCase(roles[i])) return true ;
    }
    return false ;
  }

  /**
   * Get orientation of current locate
   * @param locale      String
   *                    The locale name which specified by user
   * @return
   * @throws Exception
   */
  private Orientation getOrientation(String locale) throws Exception {
    return localeConfigService_.getLocaleConfig(locale).getOrientation();
  }
  
  /**
   * Add new skin node if the locale specified is not existing
   * @param templatesHome
   * @param nodeTypeNode
   * @param skinName
   * @param orientation
   * @param templateData
   * @return
   * @throws Exception
   */
  private String addNewSkinNode(Node templatesHome, Node nodeTypeNode, String skinName, String orientation, 
      String templateData) throws Exception {
    String label = nodeTypeNode.getProperty(TEMPLATE_LABEL).getString();
    return getContentNode(SKINS, templatesHome, nodeTypeNode.getName(), label, true, skinName + orientation, new String[] {"*"}, new ByteArrayInputStream(templateData.getBytes()));
  }
  
  private void removeTemplateNodeTypeList() throws Exception {
    nodeTypeListCached.clearCache();
  }
  
  /**
   * {@inheritDoc}
   */
  public String addTemplate(String templateType, String nodeTypeName, String label, boolean isDocumentTemplate, String templateName, String[] roles, InputStream templateFile, String repository) throws Exception {
    Session session = getSession(repository);
    Node templatesHome = (Node) session.getItem(cmsTemplatesBasePath_);
    String templatePath = null;
    try {
      templatePath = templatesHome.getPath() + "/" + nodeTypeName + "/" + templateType + "/" + templateName;
      Node templateNode = (Node)session.getItem(templatePath);
      updateTemplate(templateNode,templateFile, roles);
      session.save();
    } catch(PathNotFoundException e) {
      templatePath = getContentNode(templateType, templatesHome, nodeTypeName, label, 
          isDocumentTemplate, templateName, roles, templateFile);
      session.save();
    } finally {
      session.logout();
    }
    //Update managedDocumentTypesMap
    removeCacheTemplate(templatePath);
    removeTemplateNodeTypeList();
    updateDocumentsTemplate(isDocumentTemplate, repository, nodeTypeName);
    return templatePath;
  }
  
  /**
   * {@inheritDoc}
   */
  public String addTemplate(String templateType, String nodeTypeName, String label, boolean isDocumentTemplate, String templateName, 
      String[] roles, InputStream templateFile, String repository, Node templatesHome) throws Exception {
    Session session = getSession(repository);
    String templatePath = null;
    try {
      templatePath = templatesHome.getPath() + "/" + nodeTypeName + "/" + templateType + "/" + templateName;
      Node templateNode = (Node)session.getItem(templatePath);
      updateTemplate(templateNode,templateFile, roles);
      session.save();
    } catch(PathNotFoundException e) {
      templatePath = getContentNode(templateType, templatesHome, nodeTypeName, label, 
          isDocumentTemplate, templateName, roles, templateFile);
      session.save();
    } finally {
      session.logout();
    }
    //Update managedDocumentTypesMap
    removeCacheTemplate(templatePath);
    removeTemplateNodeTypeList();
    updateDocumentsTemplate(isDocumentTemplate, repository, nodeTypeName);
    return templatePath;
  }
  
  /**
   * {@inheritDoc}
   */
  public String createTemplate(Node templateFolder, String name, InputStream data, String[] roles) {
    Session session = null;
    try {
      Node contentNode = templateFolder.addNode(name, NodetypeConstant.NT_FILE);
      Node resourceNode = contentNode.addNode(NodetypeConstant.JCR_CONTENT, NodetypeConstant.EXO_RESOURCES);
      resourceNode.setProperty(NodetypeConstant.JCR_ENCODING, "UTF-8");
      resourceNode.setProperty(NodetypeConstant.JCR_MIME_TYPE, "application/x-groovy+html");
      resourceNode.setProperty(NodetypeConstant.JCR_LAST_MODIFIED, new GregorianCalendar());
      resourceNode.setProperty(NodetypeConstant.JCR_DATA, data);
      resourceNode.setProperty(NodetypeConstant.EXO_ROLES, roles);
      String templatePath = contentNode.getPath();
      session = getSession(WCMCoreUtils.getRepository(null).getConfiguration().getName());
      session.save();
      return templatePath;
    } catch (Exception e) {
      LOG.error("An error has been occurred when adding template", e);
    } finally {
      if (session != null) session.logout();
    }
    return null;
  }
  
  /**
   * {@inheritDoc}
   */
  public String updateTemplate(Node template, InputStream data, String[] roles) {
    Session session = null;
    try {
      Node resourceNode = template.getNode(NodetypeConstant.JCR_CONTENT);
      resourceNode.setProperty(NodetypeConstant.EXO_ROLES, roles);
      resourceNode.setProperty(NodetypeConstant.JCR_LAST_MODIFIED, new GregorianCalendar());
      resourceNode.setProperty(NodetypeConstant.JCR_DATA, data);
      String templatePath = template.getPath();
      session = getSession(WCMCoreUtils.getRepository(null).getConfiguration().getName());
      session.save();
      return templatePath;
    } catch (Exception e) {
      LOG.error("An error has been occurred when updating template", e);
    } finally {
      if (session != null) session.logout();
    }
    return null;
  }
  
  /**
   * {@inheritDoc}
   */
  public String getTemplate(Node template) {
    try {
      Node resourceNode = template.getNode(NodetypeConstant.JCR_CONTENT);
      return resourceNode.getProperty(NodetypeConstant.JCR_DATA).getString();
    } catch (Exception e) {
      LOG.error("An error has been occurred when getting template", e);
    }
    return null;
  }
  
  /**
   * {@inheritDoc}
   */
  public String getTemplateRoles(Node template) {
    try {
      Value[] values = template.getNode("jcr:content").getProperty(NodetypeConstant.EXO_ROLES).getValues();
      StringBuffer roles = new StringBuffer();
      for (int i = 0; i < values.length; i++) {
        if (roles.length() > 0)
          roles.append("; ");
        roles.append(values[i].getString());
      }
      return roles.toString();  
    } catch (Exception e) {
      LOG.error("An error has been occurred when getting template's roles", e);
    }
    return null;
  }
}
