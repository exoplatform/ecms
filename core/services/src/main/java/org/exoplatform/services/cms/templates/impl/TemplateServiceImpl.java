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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.jcr.AccessDeniedException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.ValueFormatException;
import javax.jcr.nodetype.NodeType;

import org.apache.commons.lang.StringUtils;
import org.exoplatform.container.component.ComponentPlugin;
import org.exoplatform.services.cache.CacheService;
import org.exoplatform.services.cache.ExoCache;
import org.exoplatform.services.cms.BasePath;
import org.exoplatform.services.cms.impl.DMSConfiguration;
import org.exoplatform.services.cms.impl.DMSRepositoryConfiguration;
import org.exoplatform.services.cms.impl.Utils;
import org.exoplatform.services.cms.templates.ContentTypeFilterPlugin;
import org.exoplatform.services.cms.templates.ContentTypeFilterPlugin.FolderFilterConfig;
import org.exoplatform.services.cms.templates.TemplateService;
import org.exoplatform.services.context.DocumentContext;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.access.DynamicIdentity;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.jcr.ext.hierarchy.NodeHierarchyCreator;
import org.exoplatform.services.jcr.impl.Constants;
import org.exoplatform.services.jcr.impl.core.nodetype.NodeTypeImpl;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.resources.LocaleConfigService;
import org.exoplatform.services.resources.Orientation;
import org.exoplatform.services.security.Identity;
import org.exoplatform.services.security.IdentityConstants;
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
  private IdentityRegistry     identityRegistry_;
  private String               cmsTemplatesBasePath_;
  private List<TemplatePlugin> plugins_ = new ArrayList<TemplatePlugin>();
  private Set<String> configuredNodeTypes;

  /**
   * The key is a folder type, the value is the List of content types.
   */
  private HashMap<String, List<String>> foldersFilterMap = null;

  /**
   * The key is a repository name, the value is the List of template names.
   */
  private HashMap<String, List<String>> managedDocumentTypesMap = new HashMap<String, List<String>>();

  private org.exoplatform.groovyscript.text.TemplateService templateService;
  private LocaleConfigService localeConfigService_;

  private static final Log LOG  = ExoLogger.getLogger(TemplateService.class.getName());

  /**
   * DMS configuration which used to store informations
   */
  private DMSConfiguration dmsConfiguration_;

  private static final String NODETYPE_LIST = "nodeTypeList";
  private static final String EDITED_CONFIGURED_NODE_TYPES = "EditedConfiguredNodeTypes";

  private ExoCache nodeTypeListCached ;

  /**
   * Constructor method.
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
    configuredNodeTypes = new HashSet<String>();
    try {
      for (TemplatePlugin plugin : plugins_) {
        plugin.init();
        configuredNodeTypes.addAll(plugin.getAllConfiguredNodeTypes());
      }

      // Cached all nodetypes that is document type in the map
      getDocumentTemplates();
    } catch (Exception e) {
      if (LOG.isErrorEnabled()) {
        LOG.error("An unexpected exception occurs when init plugins", e);
      }
    }
  }

  /**
   * {@inheritDoc}
   */
  public void stop() {
  }

  /**
   * {@inheritDoc}
   * @throws RepositoryException
   */
  public void addContentTypeFilterPlugin(ContentTypeFilterPlugin filterPlugin) {
    HashMap<String,List<String>> folderFilterMap = foldersFilterMap;
    if (folderFilterMap == null) {
      folderFilterMap = new HashMap<String,List<String>>();
    }
    for (FolderFilterConfig filterConfig : filterPlugin.getFolderFilterConfigList()) {
      String folderType = filterConfig.getFolderType();
      List<String> contentTypes = filterConfig.getContentTypes();
      List<String> value = folderFilterMap.get(folderType);
      if (value == null) {
        folderFilterMap.put(folderType, contentTypes);
      } else {
        value.addAll(contentTypes);
        folderFilterMap.put(folderType, value);
      }
    }
    foldersFilterMap = folderFilterMap;
  }

  /**
   * {@inheritDoc}
   */
  public Set<String> getAllowanceFolderType() {
    HashMap<String, List<String>> map = foldersFilterMap;
    if (map != null)
      return map.keySet();
    return null;
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
  public void init() throws Exception {
    for (TemplatePlugin plugin : plugins_) {
      plugin.init();
    }
  }

  /**
   * {@inheritDoc}
   */
  public Node getTemplatesHome(SessionProvider provider) throws Exception {
    try {
      Session session = getSession(provider);
      return (Node) session.getItem(cmsTemplatesBasePath_);
    } catch (AccessDeniedException ace) {
      if (LOG.isErrorEnabled()) {
        LOG.error("Access denied. You can not access to this template");
      }
      return null;
    }
  }

  /**
   * {@inheritDoc}
   */
  public List<String> getCreationableContentTypes(Node node) throws Exception {
    String folderType = node.getPrimaryNodeType().getName();
    List<String> testContentTypes = null;
    HashMap<String,List<String>> folderFilterMap = foldersFilterMap;
    if (folderFilterMap != null) {
      List<String> list = folderFilterMap.get(folderType);
      if (list != null && list.size() != 0) {
        testContentTypes = list;
      }
    }
    if (testContentTypes == null) {
      testContentTypes = getDocumentTemplates();
    }
    List<String> result = new ArrayList<String>();
    for (String contentType : testContentTypes) {
      if (isChildNodePrimaryTypeAllowed(node, contentType)) {
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
    allNodeTypes.add(parent.getPrimaryNodeType());
    for(NodeType mixin: parent.getMixinNodeTypes()) {
      allNodeTypes.add(mixin);
    }
    for (NodeType nodetype:allNodeTypes) {
      if (((NodeTypeImpl)nodetype).isChildNodePrimaryTypeAllowed(
                                                                 Constants.JCR_ANY_NAME,
                                                                 ((NodeTypeImpl)childNodeType).getQName())) {
        return true;
      }
    }
    return false;
  }

  /**
   * {@inheritDoc}
   */
  public boolean isManagedNodeType(String nodeTypeName) throws RepositoryException {
    //check if the node type is document type first
    List<String> managedDocumentTypes = getManagedDocumentTypesMap();
    if(managedDocumentTypes != null && managedDocumentTypes.contains(nodeTypeName))
      return true;
    SessionProvider provider = SessionProvider.createSystemProvider();
    Session session = getSession(provider);
    try {
      Node systemTemplatesHome = (Node) session.getItem(cmsTemplatesBasePath_);
      return systemTemplatesHome.hasNode(nodeTypeName);
    } finally {
      provider.close();
    }
  }

  /**
   * {@inheritDoc}
   */
  public String getTemplatePath(Node node, boolean isDialog) throws Exception {
    String userId = node.getSession().getUserID();
    String templateType = null;
    if (node.isNodeType("exo:presentationable") && node.hasProperty("exo:presentationType")) {
      templateType = node.getProperty("exo:presentationType").getString();
    } else if (node.isNodeType("nt:frozenNode")) {
      templateType = node.getProperty("jcr:frozenPrimaryType").getString();
    } else {
      templateType = node.getPrimaryNodeType().getName();
    }
    if (isManagedNodeType(templateType)) {
      return getTemplatePathByUser(isDialog, templateType, userId);
    }

    // Check if node's nodetype or its supper type has managed template type
    String managedTemplateType = getManagedTemplateType(node);
    if (StringUtils.isNotEmpty(managedTemplateType)) {
      return getTemplatePathByUser(isDialog, managedTemplateType, userId);
    }

    throw new Exception("The content type: " + templateType + " isn't supported by any template");
  }

  /**
   * {@inheritDoc}
   */
  public NodeIterator getAllTemplatesOfNodeType(boolean isDialog, String nodeTypeName,
                                                SessionProvider provider) throws Exception {
    Node nodeTypeHome = getTemplatesHome(provider).getNode(nodeTypeName);
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
                              SessionProvider provider) throws Exception {
    Node nodeTypeNode = getTemplatesHome(provider).getNode(nodeTypeName);
    return nodeTypeNode.getNode(type).getNode(templateName);
  }

  /**
   * {@inheritDoc}
   */
  public String getTemplatePathByUser(boolean isDialog, String nodeTypeName, String userName) throws RepositoryException {
    if(IdentityConstants.ANONIM.equals(userName) || DynamicIdentity.DYNAMIC.equals(userName) || userName == null) {
      return getTemplatePathByAnonymous(isDialog, nodeTypeName);
    }
    Node templateHomeNode =
        (Node) getSession(WCMCoreUtils.getSystemSessionProvider()).getItem(cmsTemplatesBasePath_);
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
        return templatePath ;
      }
    }
    throw new AccessControlException("You don't have permission to access any template");
  }

  /**
   * {@inheritDoc}
   */
  public String getTemplatePath(boolean isDialog, String nodeTypeName, String templateName) throws Exception {
    String type = DIALOGS;
    if (!isDialog)
      type = VIEWS;
    Node templateNode = getTemplateNode(type, nodeTypeName, templateName);
    String path = templateNode.getPath();
    return path;
  }

  /**
   * {@inheritDoc}
   */
  public String getTemplateLabel(String nodeTypeName) throws Exception {
    SessionProvider provider = SessionProvider.createSystemProvider();
    try {
      Node templateHome = getTemplatesHome(provider);
      Node nodeType = templateHome.getNode(nodeTypeName);
      if (nodeType.hasProperty("label")) {
        return nodeType.getProperty("label").getString();
      }
    } finally {
      provider.close();
    }
    return "";
  }

  /**
   * {@inheritDoc}
   */
  public String getTemplate(String type, String nodeTypeName, String templateName) throws Exception {
    Node templateNode = getTemplateNode(type, nodeTypeName, templateName);
    return getTemplate(templateNode);
  }

  /**
   * {@inheritDoc}
   */
  public void removeTemplate(String type, String nodeTypeName, String templateName) throws Exception {
    Node templatesHome =
        (Node) getSession(WCMCoreUtils.getSystemSessionProvider()).getItem(cmsTemplatesBasePath_);
    Node nodeTypeHome = templatesHome.getNode(nodeTypeName);
    Node specifiedTemplatesHome = nodeTypeHome.getNode(type);
    Node contentNode = specifiedTemplatesHome.getNode(templateName);
    contentNode.remove();
    nodeTypeHome.save();

    // Update list of changed template node type list
    addEditedConfiguredNodeType(nodeTypeName);
  }

  /**
   * {@inheritDoc}
   */
  public void removeManagedNodeType(String nodeTypeName) throws Exception {
    Node templatesHome =
        (Node) getSession(WCMCoreUtils.getSystemSessionProvider()).getItem(cmsTemplatesBasePath_);
    Node managedNodeType = templatesHome.getNode(nodeTypeName);
    managedNodeType.remove();
    templatesHome.save();
    //Update managedDocumentTypeMap
    List<String> managedDocumentTypes = getManagedDocumentTypesMap();
    managedDocumentTypes.remove(nodeTypeName);
    removeTemplateNodeTypeList();

    // Add to edited predefined template node type list
    addEditedConfiguredNodeType(nodeTypeName);
  }

  /**
   * {@inheritDoc}
   */
  public List<String> getDocumentTemplates() throws RepositoryException {
    List<String> templates = getManagedDocumentTypesMap();
    if (templates != null)
      return new ArrayList<String>(templates);
    templates = getAllDocumentNodeTypes();
    setManagedDocumentTypesMap(templates);
    return templates == null ? templates : new ArrayList<String>(templates);
  }

  /**
   * {@inheritDoc}
   */
  public String getTemplatePathByAnonymous(boolean isDialog, String nodeTypeName) throws RepositoryException {
    String type = DIALOGS;
    if (!isDialog)
      type = VIEWS;
    Node homeNode =
        (Node) getSession(WCMCoreUtils.getSystemSessionProvider()).getItem(cmsTemplatesBasePath_);
    Node nodeTypeNode = homeNode.getNode(nodeTypeName);
    NodeIterator templateIter = nodeTypeNode.getNode(type).getNodes();
    while (templateIter.hasNext()) {
      Node node = templateIter.nextNode();
      String role = getTemplateRoles(node);
      if(hasPublicTemplate(role)) {
        String templatePath = node.getPath() ;
        return templatePath ;
      }
    }
    return null;
  }

  /**
   * {@inheritDoc}
   */
  public void removeCacheTemplate(String name) throws Exception {
    try {
      templateService.reloadTemplate(name);
    } catch (IllegalArgumentException e) {
      return;
    }
  }

  /**
   * {@inheritDoc}
   */
  public void removeAllTemplateCached() {
    templateService.reloadTemplates();
  }

  @SuppressWarnings("unchecked")
  public List<String> getAllDocumentNodeTypes() throws PathNotFoundException, RepositoryException {
    List<String> nodeTypeList = (List<String>) nodeTypeListCached.get(NODETYPE_LIST);
    if(nodeTypeList != null && nodeTypeList.size() > 0)
      return nodeTypeList;

    List<String> contentTypes = new ArrayList<String>();
    SessionProvider sessionProvider = SessionProvider.createSystemProvider();
    try {
      Node templatesHome =
          (Node) getSession(sessionProvider).getItem(cmsTemplatesBasePath_);
      for (NodeIterator templateIter = templatesHome.getNodes(); templateIter.hasNext();) {
        Node template = templateIter.nextNode();
        if (template.getProperty(DOCUMENT_TEMPLATE_PROP).getBoolean())
          contentTypes.add(template.getName());
      }
      nodeTypeListCached.put(NODETYPE_LIST, contentTypes);
    } finally {
      sessionProvider.close();
    }
    return contentTypes;
  }

  /**
   * {@inheritDoc}
   */
  public String getSkinPath(String nodeTypeName, String skinName, String locale) throws Exception {
    Node homeNode =
        (Node) getSession(WCMCoreUtils.getSystemSessionProvider()).getItem(cmsTemplatesBasePath_);
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
    return skinPath;
  }

  /**
   * {@inheritDoc}
   */
  public String buildDialogForm(String nodeTypeName) throws Exception {
    if (plugins_ == null && plugins_.size() == 0) throw new Exception("Cannot find plugin for template");
    TemplatePlugin templatePlugin = plugins_.get(0);
    ManageableRepository manageRepo = repositoryService_.getCurrentRepository();
    NodeType nodeType = manageRepo.getNodeTypeManager().getNodeType(nodeTypeName);
    return templatePlugin.buildDialogForm(nodeType);
  }

  /**
   * {@inheritDoc}
   */
  public String buildViewForm(String nodeTypeName) throws Exception {
    if (plugins_ == null && plugins_.size() == 0) throw new Exception("Cannot find plugin for template");
    TemplatePlugin templatePlugin = plugins_.get(0);
    ManageableRepository manageRepo = repositoryService_.getCurrentRepository();
    NodeType nodeType = manageRepo.getNodeTypeManager().getNodeType(nodeTypeName);
    return templatePlugin.buildViewForm(nodeType);
  }

  /**
   * {@inheritDoc}
   */
  public String buildStyleSheet(String nodeTypeName) throws Exception {
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
  private Node getTemplateNode(String type, String nodeTypeName,
                               String templateName) throws Exception {
    Node homeNode = (Node) getSession(WCMCoreUtils.getSystemSessionProvider()).getItem(cmsTemplatesBasePath_);
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
  private String getContentNode(String templateType,
                                Node templatesHome,
                                String nodeTypeName,
                                String label,
                                boolean isDocumentTemplate,
                                String templateName,
                                String[] roles,
                                InputStream templateFile) throws Exception {
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
   * @param nodeTypeName          String
   *                              The name of NodeType
   * @throws RepositoryException
   * @see                         Node
   * @see                         NodeType
   */
  private void updateDocumentsTemplate(boolean isDocumentTemplate, String nodeTypeName) throws RepositoryException{
    if(isDocumentTemplate) {
      List<String> documentList = getManagedDocumentTypesMap();
      if(documentList == null) {
        documentList = new ArrayList<String>();
        documentList.add(nodeTypeName);
        setManagedDocumentTypesMap(documentList);
      } else {
        if(!documentList.contains(nodeTypeName)) {
          documentList.add(nodeTypeName);
          setManagedDocumentTypesMap(documentList);
        }
      }
    }
  }

  /**
   * Return session of the specified repository
   * @param provider        SessionProvider
   * @return
   * @throws RepositoryException
   * @see                   SessionProvider
   * @see                   ManageableRepository
   * @see                   DMSRepositoryConfiguration
   * @throws Exception
   */
  private Session getSession(SessionProvider provider) throws RepositoryException {
    ManageableRepository manageableRepository = repositoryService_.getCurrentRepository();
    DMSRepositoryConfiguration dmsRepoConfig = dmsConfiguration_.getConfig();
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
   */
  private boolean hasPermission(String userId, String roles, IdentityRegistry identityRegistry) {
    if(IdentityConstants.SYSTEM.equalsIgnoreCase(userId)) {
      return true ;
    }
    Identity identity = identityRegistry.getIdentity(userId) ;
    if(identity == null) {
      return false ;
    }
    String[] listRoles = roles.split(",");
    for (int i = 0; i < listRoles.length; i++) {
      String role = listRoles[i].trim();
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
   */
  private boolean hasPublicTemplate(String role) {
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
  private String addNewSkinNode(Node templatesHome,
                                Node nodeTypeNode,
                                String skinName,
                                String orientation,
                                String templateData) throws Exception {
    String label = nodeTypeNode.getProperty(TEMPLATE_LABEL).getString();
    return getContentNode(SKINS, templatesHome, nodeTypeNode.getName(), label, true, skinName
                          + orientation, new String[] { "*" }, new ByteArrayInputStream(templateData.getBytes()));
  }

  private void removeTemplateNodeTypeList() throws Exception {
    nodeTypeListCached.clearCache();
  }

  /**
   * {@inheritDoc}
   */
  public String addTemplate(String templateType,
                            String nodeTypeName,
                            String label,
                            boolean isDocumentTemplate,
                            String templateName,
                            String[] roles,
                            InputStream templateFile) throws Exception {
    Session session = getSession(WCMCoreUtils.getSystemSessionProvider());
    Node templatesHome = (Node) session.getItem(cmsTemplatesBasePath_);
    String templatePath = null;
    try {
      templatePath = templatesHome.getPath() + "/" + nodeTypeName + "/" + templateType + "/" + templateName;
      Node templateNode = (Node)session.getItem(templatePath);
      DocumentContext.getCurrent().getAttributes().put(DocumentContext.IS_SKIP_RAISE_ACT, true);
      updateTemplate(templateNode,templateFile, roles);
      session.save();
    } catch(PathNotFoundException e) {
      DocumentContext.getCurrent().getAttributes().put(DocumentContext.IS_SKIP_RAISE_ACT, true);
      templatePath = getContentNode(templateType, templatesHome, nodeTypeName, label,
                                    isDocumentTemplate, templateName, roles, templateFile);
    }
    //Update managedDocumentTypesMap
    removeCacheTemplate(templatePath);
    removeTemplateNodeTypeList();
    updateDocumentsTemplate(isDocumentTemplate, nodeTypeName);
    return templatePath;
  }

  /**
   * {@inheritDoc}
   */
  public String addTemplate(String templateType,
                            String nodeTypeName,
                            String label,
                            boolean isDocumentTemplate,
                            String templateName,
                            String[] roles,
                            InputStream templateFile,
                            Node templatesHome) throws Exception {
    String templatePath = null;
    try {
      templatePath = templatesHome.getPath() + "/" + nodeTypeName + "/" + templateType + "/" + templateName;
      Node templateNode = (Node)templatesHome.getSession().getItem(templatePath);
      updateTemplate(templateNode,templateFile, roles);
      templateNode.save();
    } catch(PathNotFoundException e) {
      templatePath = getContentNode(templateType, templatesHome, nodeTypeName, label,
                                    isDocumentTemplate, templateName, roles, templateFile);
    }
    // Update managedDocumentTypesMap
    removeCacheTemplate(templatePath);
    removeTemplateNodeTypeList();
    return templatePath;
  }

  /**
   * {@inheritDoc}
   */
  @Deprecated
  public String createTemplate(Node templateFolder, String name, InputStream data, String[] roles) {
    return createTemplate(templateFolder, name, name, data, roles);
  }

  /**
   * {@inheritDoc}
   */
  public String createTemplate(Node templateFolder, String title, String templateName, InputStream data, String[] roles) {
    try {
      Node contentNode = templateFolder.addNode(templateName, NodetypeConstant.NT_FILE);
      Node resourceNode = contentNode.addNode(NodetypeConstant.JCR_CONTENT, NodetypeConstant.EXO_RESOURCES);
      resourceNode.setProperty(NodetypeConstant.JCR_ENCODING, "UTF-8");
      resourceNode.setProperty(NodetypeConstant.JCR_MIME_TYPE, "application/x-groovy+html");
      resourceNode.setProperty(NodetypeConstant.JCR_LAST_MODIFIED, new GregorianCalendar());
      resourceNode.setProperty(NodetypeConstant.JCR_DATA, data);
      resourceNode.setProperty(NodetypeConstant.EXO_ROLES, roles);
      if(!resourceNode.isNodeType(NodetypeConstant.DC_ELEMENT_SET)) {
        resourceNode.addMixin(NodetypeConstant.DC_ELEMENT_SET);
      }
      resourceNode.setProperty(NodetypeConstant.DC_TITLE, new String[] {title});
      resourceNode.getSession().save();
      return contentNode.getPath();
    } catch (Exception e) {
      if (LOG.isErrorEnabled()) {
        LOG.error("An error has been occurred when adding template", e);
      }
    }
    return null;
  }

  /**
   * {@inheritDoc}
   */
  public String updateTemplate(Node template, InputStream data, String[] roles) {
    try {
      Node resourceNode = template.getNode(NodetypeConstant.JCR_CONTENT);
      resourceNode.setProperty(NodetypeConstant.EXO_ROLES, roles);
      resourceNode.setProperty(NodetypeConstant.JCR_LAST_MODIFIED, new GregorianCalendar());
      resourceNode.setProperty(NodetypeConstant.JCR_DATA, data);
      resourceNode.getSession().save();

      // Add to edited predefined node type list
      addEditedConfiguredNodeType(template.getParent().getParent().getName());
      return template.getPath();
    } catch (Exception e) {
      if (LOG.isErrorEnabled()) {
        LOG.error("An error has been occurred when updating template", e);
      }
    }
    return null;
  }

  /**
   * {@inheritDoc}
   */
  public String getTemplate(Node template) {
    Node resourceNode;
    try {
      resourceNode = template.getNode(NodetypeConstant.JCR_CONTENT);
      return resourceNode.getProperty(NodetypeConstant.JCR_DATA).getString();
    } catch (ValueFormatException e) {
      if (LOG.isErrorEnabled()) {
        LOG.error("Wrong Value format ", e);
      }
    } catch (PathNotFoundException e) {
      if (LOG.isErrorEnabled()) {
        LOG.error("Can not found the template because of: ", e);
      }
    } catch (RepositoryException e) {
      if (LOG.isErrorEnabled()) {
        LOG.error("Repository failed ", e);
      }
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
      if (LOG.isErrorEnabled()) {
        LOG.error("An error has been occurred when getting template's roles", e);
      }
    }
    return null;
  }

  private String getRepoName() throws RepositoryException{
    String repositoryName = repositoryService_.getCurrentRepository().getConfiguration().getName();
    return repositoryName;
  }

  private List<String> getManagedDocumentTypesMap() throws RepositoryException{
    return managedDocumentTypesMap.get(getRepoName());
  }

  private void setManagedDocumentTypesMap(List<String> types) throws RepositoryException{
    managedDocumentTypesMap.put(getRepoName(), types);
  }

  public Set<String> getAllConfiguredNodeTypes() {
    return configuredNodeTypes;
  }

  public Set<String> getAllEditedConfiguredNodeTypes() throws Exception {
    return Utils.getAllEditedConfiguredData(this.getClass().getSimpleName(), EDITED_CONFIGURED_NODE_TYPES, true);
  }

  private void addEditedConfiguredNodeType(String nodeType) throws Exception {
    Utils.addEditedConfiguredData(nodeType, this.getClass().getSimpleName(), EDITED_CONFIGURED_NODE_TYPES, true);
  }

  /**
   * Check if node's nodetype or its supper type has managed template type and return the template type.
   *
   * @param node
   * @return managed template type. Null value mean not exist template
   * @throws Exception
   */
  private String getManagedTemplateType(Node node) throws Exception {
    // Check if the node type is document type first
    List<String> managedDocumentTypes = getManagedDocumentTypesMap();
    for (String documentType : managedDocumentTypes) {
      if (node.getPrimaryNodeType().isNodeType(documentType)) {
        return documentType;
      }
    }

    // Check if node's nodetype or its supper type has managed template type
    SessionProvider provider = WCMCoreUtils.getSystemSessionProvider();
    Session session = getSession(provider);
    Node systemTemplatesHome = (Node) session.getItem(cmsTemplatesBasePath_);
    NodeIterator templatesIter = systemTemplatesHome.getNodes();
    while(templatesIter.hasNext()) {
      String templateName = templatesIter.nextNode().getName();
      if (node.getPrimaryNodeType().isNodeType(templateName)) {
        return templateName;
      }
    }

    return null;
  }
}
