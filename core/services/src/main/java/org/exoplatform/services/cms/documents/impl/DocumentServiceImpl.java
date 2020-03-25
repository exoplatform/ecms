/*
 * Copyright (C) 2003-2011 eXo Platform SAS.
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
package org.exoplatform.services.cms.documents.impl;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.gatein.api.Portal;
import org.gatein.api.navigation.Navigation;
import org.gatein.api.navigation.Nodes;
import org.gatein.api.site.SiteId;

import org.exoplatform.commons.utils.CommonsUtils;
import org.exoplatform.container.component.ComponentPlugin;
import org.exoplatform.container.xml.PortalContainerInfo;
import org.exoplatform.portal.config.UserPortalConfig;
import org.exoplatform.portal.config.UserPortalConfigService;
import org.exoplatform.portal.mop.SiteKey;
import org.exoplatform.portal.mop.user.UserNavigation;
import org.exoplatform.portal.mop.user.UserPortalContext;
import org.exoplatform.resolver.ApplicationResourceResolver;
import org.exoplatform.resolver.ResourceResolver;
import org.exoplatform.services.cms.BasePath;
import org.exoplatform.services.cms.documents.DocumentEditor;
import org.exoplatform.services.cms.documents.DocumentEditorProvider;
import org.exoplatform.services.cms.documents.DocumentMetadataPlugin;
import org.exoplatform.services.cms.documents.DocumentService;
import org.exoplatform.services.cms.documents.NewDocumentTemplate;
import org.exoplatform.services.cms.documents.NewDocumentTemplatePlugin;
import org.exoplatform.services.cms.documents.NewDocumentTemplateProvider;
import org.exoplatform.services.cms.documents.exception.DocumentEditorProviderNotFoundException;
import org.exoplatform.services.cms.documents.exception.DocumentExtensionNotSupportedException;
import org.exoplatform.services.cms.documents.model.Document;
import org.exoplatform.services.cms.drives.DriveData;
import org.exoplatform.services.cms.drives.ManageDriveService;
import org.exoplatform.services.cms.drives.impl.ManageDriveServiceImpl;
import org.exoplatform.services.cms.impl.Utils;
import org.exoplatform.services.cms.jcrext.activity.ActivityCommonService;
import org.exoplatform.services.cms.link.LinkManager;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.jcr.ext.app.SessionProviderService;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.jcr.ext.hierarchy.NodeHierarchyCreator;
import org.exoplatform.services.jcr.impl.core.NodeImpl;
import org.exoplatform.services.listener.ListenerService;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.organization.OrganizationService;
import org.exoplatform.services.security.ConversationState;
import org.exoplatform.services.security.IdentityConstants;
import org.exoplatform.services.wcm.core.NodetypeConstant;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;
import org.exoplatform.webui.application.WebuiRequestContext;

/**
 * Created by The eXo Platform SAS Author : eXoPlatform exo@exoplatform.com Mar
 * 22, 2011
 */
public class DocumentServiceImpl implements DocumentService {

  public static final String MIX_REFERENCEABLE = "mix:referenceable";
  public static final String EXO_LAST_MODIFIER_PROP = "exo:lastModifier";
  public static final String EXO_DATE_CREATED_PROP = "exo:dateCreated";
  public static final String EXO_RSS_ENABLE_PROP = "exo:rss-enable";
  public static final String NT_FILE = "nt:file";
  public static final String NT_RESOURCE = "nt:resource";
  public static final String MIX_VERSIONABLE = "mix:versionable";
  public static final String JCR_LAST_MODIFIED_PROP = "jcr:lastModified";
  public static final String JCR_CONTENT = "jcr:content";
  public static final String JCR_DATA = "jcr:data";
  public static final String JCR_MIME_TYPE = "jcr:mimeType";
  public static final String EXO_OWNER_PROP = "exo:owner";
  public static final String EXO_TITLE_PROP = "exo:title";
  private static final String EXO_DOCUMENT = "exo:document";
  private static final String EXO_USER_PREFFERENCES = "exo:userPrefferences";
  private static final String EXO_PREFFERED_EDITOR = "exo:prefferedEditor";
  public static final String CURRENT_STATE_PROP = "publication:currentState";
  public static final String DOCUMENTS_APP_NAVIGATION_NODE_NAME = "documents";
  public static final String DOCUMENT_NOT_FOUND = "?path=doc-not-found";
  private static final String DOCUMENTS_NODE = "Documents";
  private static final String SHARED_NODE = "Shared";
  private static final Log LOG                 = ExoLogger.getLogger(DocumentServiceImpl.class);
  private final List<NewDocumentTemplateProvider> templateProviders = new ArrayList<>();
  private final List<DocumentEditorProvider> editorProviders = new ArrayList<>();
  private List<NewDocumentTemplateProvider> unmodifiebleTemplateProviders = Collections.unmodifiableList(templateProviders);
  private List<DocumentEditorProvider> unmodifiebleEditorProviders = Collections.unmodifiableList(editorProviders);
  private ManageDriveService manageDriveService;
  private Portal portal;
  private SessionProviderService sessionProviderService;
  private RepositoryService repoService;
  private NodeHierarchyCreator nodeHierarchyCreator;
  private LinkManager linkManager;
  private PortalContainerInfo portalContainerInfo;
  private Map<String, DocumentMetadataPlugin> metadataPlugins = new HashMap<>();
  private OrganizationService organizationService;

  public DocumentServiceImpl(ManageDriveService manageDriveService, Portal portal, SessionProviderService sessionProviderService, RepositoryService repoService, NodeHierarchyCreator nodeHierarchyCreator, LinkManager linkManager, PortalContainerInfo portalContainerInfo, OrganizationService organizationService) {
    this.manageDriveService = manageDriveService;
    this.sessionProviderService = sessionProviderService;
    this.repoService = repoService;
    this.nodeHierarchyCreator = nodeHierarchyCreator;
    this.portal = portal;
    this.linkManager = linkManager;
    this.portalContainerInfo = portalContainerInfo;
    this.organizationService = organizationService;
  }

  @Override
  public Document findDocById(String documentId) throws RepositoryException {
    RepositoryService repositoryService = WCMCoreUtils.getService(RepositoryService.class);
    ManageableRepository manageRepo = repositoryService.getCurrentRepository();
    SessionProvider sessionProvider = WCMCoreUtils.getUserSessionProvider();

    String ws = documentId.split(":/")[0];
    String uuid = documentId.split(":/")[1];

    Node node = sessionProvider.getSession(ws, manageRepo).getNodeByUUID(uuid);
    // Create Document
    String title = node.hasProperty(EXO_TITLE_PROP) ? node.getProperty(EXO_TITLE_PROP).getString() : "";
    String id = node.isNodeType(MIX_REFERENCEABLE) ? node.getUUID() : "";
    String state = node.hasProperty(CURRENT_STATE_PROP) ? node.getProperty(CURRENT_STATE_PROP).getValue().getString() : "";
    String author = node.hasProperty(EXO_OWNER_PROP) ? node.getProperty(EXO_OWNER_PROP).getString() : "";
    Calendar lastModified = (node.hasNode(JCR_CONTENT) ? node.getNode(JCR_CONTENT)
                                                             .getProperty(JCR_LAST_MODIFIED_PROP)
                                                             .getValue()
                                                             .getDate() : null);
    Calendar dateCreated = (node.hasProperty(EXO_DATE_CREATED_PROP) ? node.getProperty(EXO_DATE_CREATED_PROP)
                                                                          .getValue()
                                                                          .getDate()
                                                                   : null);
    String lastEditor = (node.hasProperty(EXO_LAST_MODIFIER_PROP) ? node.getProperty(EXO_LAST_MODIFIER_PROP)
                                                                        .getValue()
                                                                        .getString()
                                                                 : "");
    Document doc = new Document(id, node.getName(), title, node.getPath(), 
                                ws, state, author, lastEditor, lastModified, dateCreated);
    return doc;
  }

  /**
   *
   * {@inheritDoc}
   */
  public String getDocumentUrlInPersonalDocuments(Node currentNode, String username) throws Exception {
    Node rootNode = null;
    try {
      SessionProvider sessionProvider = sessionProviderService.getSystemSessionProvider(null);
      ManageableRepository repository = repoService.getCurrentRepository();
      Session session = sessionProvider.getSession(repository.getConfiguration().getDefaultWorkspaceName(), repository);
      //add symlink to user folder destination
      nodeHierarchyCreator.getJcrPath(BasePath.CMS_USERS_PATH);
      rootNode = (Node) session.getItem(nodeHierarchyCreator.getJcrPath(BasePath.CMS_USERS_PATH) + getPrivatePath(username));
      String sharedLink = getSharedLink(currentNode, rootNode);
      return sharedLink;
    } catch (Exception e) {
      LOG.error(e.getMessage(), e);
      return "";
    }
  }

  /**
   *
   * {@inheritDoc}
   */
  public String getDocumentUrlInSpaceDocuments(Node currentNode, String spaceId) throws Exception {
    Node rootNode = null;
    try {
      SessionProvider sessionProvider = sessionProviderService.getSystemSessionProvider(null);
      ManageableRepository repository = repoService.getCurrentRepository();
      Session session = sessionProvider.getSession(repository.getConfiguration().getDefaultWorkspaceName(), repository);
      //add symlink to space destination
      nodeHierarchyCreator.getJcrPath(BasePath.CMS_GROUPS_PATH);
      rootNode = (Node) session.getItem(nodeHierarchyCreator.getJcrPath(BasePath.CMS_GROUPS_PATH) + spaceId);
      String sharedLink = getSharedLink(currentNode, rootNode);
      return sharedLink;
    } catch (Exception e) {
      LOG.error(e.getMessage(), e);
      return "";
    }
  }

  /**
   * Get the short link to display a document in the Documents app by its id.
   * @param workspaceName The workspace of the node
   * @param nodeId The id of the node
   * @return The link to open the document
   * @throws Exception
   */
  @Override
  public String getShortLinkInDocumentsApp(String workspaceName, String nodeId) throws Exception {
    StringBuilder url = new StringBuilder();
    String containerName = portalContainerInfo.getContainerName();
    url.append("/")
            .append(containerName)
            .append("/private/")
            .append(CommonsUtils.getRestContextName())
            .append("/documents/view/")
            .append(workspaceName)
            .append("/")
            .append(nodeId);
    return url.toString();
  }

  /**
   * Get link to open a document in the Documents application.
   * This method will try to guess what is the best drive to use based on the node path.
   * @param nodePath path of the nt:file node to open
   * @return Link to open the document
   * @throws Exception
   */
  @Override
  public String getLinkInDocumentsApp(String nodePath) throws Exception {
    String userId = ConversationState.getCurrent().getIdentity().getUserId();
    boolean isAnonymous = userId == null || userId.isEmpty() || userId.equals(IdentityConstants.ANONIM);
    if(nodePath == null || isAnonymous) {
      return null;
    }
    // find the best matching drive to display the document
    DriveData drive = this.getDriveOfNode(nodePath);
    return getLinkInDocumentsApp(nodePath, drive);
  }

  /**
   * Get link to open a document in the Documents application with the given drive
   * @param nodePath path of the nt:file node to open
   * @param drive drive to use to open the nt:file node
   * @return Link to open the document
   * @throws Exception
   */
  @Override
  public String getLinkInDocumentsApp(String nodePath, DriveData drive) throws Exception {
    if(nodePath == null) {
      return null;
    }

    String containerName = portalContainerInfo.getContainerName();
    StringBuffer url = new StringBuffer();
    url.append("/").append(containerName);
    if (drive == null) {
      SiteKey siteKey = getDefaultSiteKey();
      url.append("/").append(siteKey.getName()).append("/").append(DOCUMENTS_APP_NAVIGATION_NODE_NAME)
          .append(DOCUMENT_NOT_FOUND);
      return url.toString();
    }

    String encodedDriveName = URLEncoder.encode(drive.getName(), "UTF-8");
    String encodedNodePath = URLEncoder.encode(nodePath, "UTF-8");
    if(drive.getName().startsWith(".spaces")) {
      // handle group drive case
      String groupId = drive.getParameters().get(ManageDriveServiceImpl.DRIVE_PARAMATER_GROUP_ID);
      if(groupId != null) {
        String groupPageName;
          // the doc is in a space -> we use the documents application of the space
          // we need to retrieve the root navigation URI of the space since it can differ from
          // the group id if the space has been renamed
          String rootNavigation = getSpaceRootNavigationNodeURI(groupId.replace(".","/"));

          groupPageName = rootNavigation + "/" + DOCUMENTS_APP_NAVIGATION_NODE_NAME;

        url.append("/g/").append(groupId.replaceAll("\\.", ":")).append("/").append(groupPageName)
                .append("?path=").append(encodedDriveName).append(encodedNodePath)
                .append("&").append(ManageDriveServiceImpl.DRIVE_PARAMATER_GROUP_ID).append("=").append(groupId);
      } else {
        throw new Exception("Cannot get group id from node path " + nodePath);
      }
    } else if(drive.getName().equals(ManageDriveServiceImpl.USER_DRIVE_NAME)
            || drive.getName().equals(ManageDriveServiceImpl.PERSONAL_DRIVE_NAME)) {
      // handle personal drive case
      SiteKey siteKey = getDefaultSiteKey();
      url.append("/").append(siteKey.getName()).append("/").append(DOCUMENTS_APP_NAVIGATION_NODE_NAME)
              .append("?path=" + encodedDriveName + encodedNodePath);
      String[] splitedNodePath = nodePath.split("/");
      if(splitedNodePath != null && splitedNodePath.length >= 6) {
        String userId = splitedNodePath[5];
        url.append("&").append(ManageDriveServiceImpl.DRIVE_PARAMATER_USER_ID).append("=").append(userId);
      }
    } else {
      // default case
      SiteKey siteKey = getDefaultSiteKey();
      url.append("/").append(siteKey.getName()).append("/").append(DOCUMENTS_APP_NAVIGATION_NODE_NAME)
              .append("?path=" + encodedDriveName + encodedNodePath);
    }

    return url.toString();
  }

  /**
   * Retrieve the root navigation node URi of a space
   * This method uses the Portal Navigation API and not the Space API to avoid making ECMS depends on Social
   * @param spaceGroupId The groupId of the space
   * @return The URI of the root navigation node of the space
   */
  protected String getSpaceRootNavigationNodeURI(String spaceGroupId) throws Exception {
    String navigationName = null;

    Navigation spaceNavigation = portal.getNavigation(new SiteId(org.gatein.api.site.SiteType.SPACE, spaceGroupId));
    if(spaceNavigation != null) {
      org.gatein.api.navigation.Node navigationRootNode = spaceNavigation.getRootNode(Nodes.visitChildren());
      if(navigationRootNode != null && navigationRootNode.iterator().hasNext()) {
        // we assume there is only one root navigation node, that's how spaces work
        org.gatein.api.navigation.Node node = navigationRootNode.iterator().next();
        navigationName = node.getName();
      }
    }

    return navigationName;
  }

  @Override
  public DriveData getDriveOfNode(String nodePath) throws Exception {
    return getDriveOfNode(nodePath, ConversationState.getCurrent().getIdentity().getUserId(), Utils.getMemberships());
  }

  @Override
  public DriveData getDriveOfNode(String nodePath, String userId, List<String> memberships) throws Exception {
    DriveData nodeDrive = null;
    List<DriveData> drives = manageDriveService.getDriveByUserRoles(userId, memberships);

    // Manage special cases
    String[] splitedPath = nodePath.split("/");
    if (splitedPath != null && splitedPath.length >= 2 && splitedPath.length >= 6
        && splitedPath[1].equals(ManageDriveServiceImpl.PERSONAL_DRIVE_ROOT_NODE)) {
      if (splitedPath[5].equals(userId)) {
        nodeDrive = manageDriveService.getDriveByName(ManageDriveServiceImpl.PERSONAL_DRIVE_NAME);
      } else {
        nodeDrive = manageDriveService.getDriveByName(ManageDriveServiceImpl.USER_DRIVE_NAME);
      }
      if (nodeDrive != null) {
        nodeDrive = nodeDrive.clone();
        nodeDrive.getParameters().put(ManageDriveServiceImpl.DRIVE_PARAMATER_USER_ID,
                                      splitedPath[2] + "/" + splitedPath[3] + "/" + splitedPath[4] + "/" + splitedPath[5]);
      }
    }
    if (splitedPath != null && splitedPath.length >= 2 && splitedPath[1].equals(ManageDriveServiceImpl.GROUPS_DRIVE_ROOT_NODE)) {
      int groupDocumentsRootNodeName = nodePath.indexOf("/Documents");
      if(groupDocumentsRootNodeName >= 0) {
        // extract group id for doc path
        String groupId = nodePath.substring(ManageDriveServiceImpl.GROUPS_DRIVE_ROOT_NODE.length() + 1, groupDocumentsRootNodeName);
        nodeDrive = manageDriveService.getDriveByName(groupId.replaceAll("/", "."));
      }
    }
    if (nodeDrive == null) {
      for (DriveData drive : drives) {
        if (nodePath.startsWith(drive.getResolvedHomePath())) {
          if (nodeDrive == null || nodeDrive.getResolvedHomePath().length() < drive.getResolvedHomePath().length()) {
            nodeDrive = drive;
          }
        }
      }
    }
    return nodeDrive;
  }


  protected UserPortalConfig getDefaultUserPortalConfig() throws Exception {
    UserPortalConfigService userPortalConfigSer = WCMCoreUtils.getService(UserPortalConfigService.class);
    UserPortalContext NULL_CONTEXT = new UserPortalContext() {
      public ResourceBundle getBundle(UserNavigation navigation) {
        return null;
      }
      public Locale getUserLocale() {
        return Locale.ENGLISH;
      }
    };
    String remoteId = ConversationState.getCurrent().getIdentity().getUserId() ;
    UserPortalConfig userPortalCfg = userPortalConfigSer.
            getUserPortalConfig(userPortalConfigSer.getDefaultPortal(), remoteId, NULL_CONTEXT);
    return userPortalCfg;
  }

  protected SiteKey getDefaultSiteKey() throws Exception {
    UserPortalConfig prc = getDefaultUserPortalConfig();
    if (prc == null) {
      return null;
    }
    SiteKey siteKey = SiteKey.portal(prc.getPortalConfig().getName());
    return siteKey;
  }

  private String getPrivatePath(String user) {
    return "/" + user.substring(0, 1) + "___/" + user.substring(0, 2) + "___/" + user.substring(0, 3) + "___/" + user + "/Private";
  }

  private String getSharedLink(Node currentNode, Node rootNode) {
    Node shared = null;
    Node link = null;
    try {
      rootNode = rootNode.getNode(DOCUMENTS_NODE);
      if (!rootNode.hasNode(SHARED_NODE)) {
        shared = rootNode.addNode(SHARED_NODE);
      } else {
        shared = rootNode.getNode(SHARED_NODE);
      }
      if (currentNode.isNodeType(NodetypeConstant.EXO_SYMLINK)) {
        currentNode = linkManager.getTarget(currentNode);
      }
      List<String> path = new ArrayList<>();
      Node targetNode = null;
      boolean existingSymlink = false;
      for (NodeIterator it = shared.getNodes(); it.hasNext(); ) {
        Node node = it.nextNode();
        path.add(((NodeImpl) node).getInternalPath().getAsString());
        if (path.contains(((NodeImpl) shared).getInternalPath().getAsString() + "[]" + currentNode.getName() + ":1")) {
          existingSymlink = true;
          targetNode = node;
          break;
        }
      }
      if (existingSymlink) {
        link = targetNode;
      } else {
        link = linkManager.createLink(shared, currentNode);
      }
      return CommonsUtils.getCurrentDomain() + getShortLinkInDocumentsApp(link.getSession().getWorkspace().getName(), ((NodeImpl) link).getInternalIdentifier());
    } catch (Exception e) {
      LOG.error(e.getMessage(), e);
      return "";
    }
  }
  
  /**
   * {@inheritDoc}
   */
  @Override
  public void addDocumentTemplatePlugin(ComponentPlugin plugin) {
    Class<NewDocumentTemplatePlugin> pclass = NewDocumentTemplatePlugin.class;
    if (pclass.isAssignableFrom(plugin.getClass())) {
      NewDocumentTemplatePlugin newPlugin = pclass.cast(plugin);
      if (LOG.isDebugEnabled()) {
        LOG.debug("Adding NewDocumentTemplatePlugin [{}]", newPlugin.toString());
      }
      templateProviders.add(new NewDocumentTemplateProviderImpl(newPlugin));
      if (LOG.isDebugEnabled()) {
        LOG.debug("Registered NewDocumentTemplatePlugin instance of {}", plugin.getClass().getName());
      }
    } else {
      LOG.error("The NewDocumentTemplatePlugin plugin is not an instance of " + pclass.getName());
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void addDocumentEditorPlugin(ComponentPlugin plugin) {
    Class<DocumentEditor> pclass = DocumentEditor.class;
    if (pclass.isAssignableFrom(plugin.getClass())) {
      DocumentEditor editor = pclass.cast(plugin);
      if (LOG.isDebugEnabled()) {
        LOG.debug("Adding DocumentEditor [{}]", editor.toString());
      }
      editorProviders.add(new DocumentEditorProviderImpl(editor));
      if (LOG.isDebugEnabled()) {
        LOG.debug("Registered DocumentEditor instance of {}", plugin.getClass().getName());
      }
    } else {
      LOG.error("The DocumentEditor plugin is not an instance of " + pclass.getName());
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Node createDocumentFromTemplate(Node currentNode, String title, NewDocumentTemplate template) throws Exception {
    InputStream data = new ByteArrayInputStream(new byte[0]);
    if (template.getPath() != null && !template.getPath().trim().isEmpty()) {
      WebuiRequestContext context = WebuiRequestContext.getCurrentInstance();
      ApplicationResourceResolver appResolver = context.getApplication().getResourceResolver();
      ResourceResolver resolver = appResolver.getResourceResolver(template.getPath());
      data = resolver.getInputStream(template.getPath());
      DocumentMetadataPlugin metadataPlugin = metadataPlugins.get(template.getExtension());
      if(metadataPlugin != null && metadataPlugin.isExtensionSupported(template.getExtension())) {
        try {
          data = metadataPlugin.updateMetadata(template.getExtension(), data, new Date(), getCurrentUserDisplayName());
        } catch (DocumentExtensionNotSupportedException e) {
          LOG.error("Document extension is not supported by metadata plugin.", e);
        } catch (IOException e) {
          LOG.error("Couldn't add metadata to the document from template.", e);
        } 
      } else {
        LOG.warn("Couldn't find appropriate metadata plugin for the {} extension.", template.getExtension());
      }
    }
    // Add node
    Node addedNode = currentNode.addNode(title, NT_FILE);

    // Set title
    if (!addedNode.hasProperty(EXO_TITLE_PROP)) {
      addedNode.addMixin(EXO_RSS_ENABLE_PROP);
    }
    // Enable versioning
    if (addedNode.canAddMixin(MIX_VERSIONABLE)) {
      addedNode.addMixin(MIX_VERSIONABLE);
    }

    addedNode.setProperty(EXO_TITLE_PROP, title);
    Node content = addedNode.addNode(JCR_CONTENT, NT_RESOURCE);

    content.setProperty(JCR_DATA, data);
    content.setProperty(JCR_MIME_TYPE, template.getMimeType());
    content.setProperty(JCR_LAST_MODIFIED_PROP, new GregorianCalendar());
    ListenerService listenerService = WCMCoreUtils.getService(ListenerService.class);
    listenerService.broadcast(ActivityCommonService.FILE_CREATED_ACTIVITY, null, addedNode);
    currentNode.save();
    data.close();
    return addedNode;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public List<NewDocumentTemplateProvider> getNewDocumentTemplateProviders() {
    return unmodifiebleTemplateProviders;
  }
  
  /**
   * {@inheritDoc}
   */
  @Override
  public void addDocumentMetadataPlugin(ComponentPlugin plugin) {
    Class<DocumentMetadataPlugin> pclass = DocumentMetadataPlugin.class;
    if (pclass.isAssignableFrom(plugin.getClass())) {
      DocumentMetadataPlugin newPlugin = pclass.cast(plugin);
      LOG.info("Adding DocumentMetadataPlugin [{}]", plugin.toString());
      newPlugin.getSupportedExtensions().forEach(ext -> metadataPlugins.put(ext, newPlugin));
      if (LOG.isDebugEnabled()) {
        LOG.debug("Registered DocumentMetadataPlugin instance of {}", plugin.getClass().getName());
      }
    } else {
      LOG.error("The DocumentMetadataPlugin plugin is not an instance of " + pclass.getName());
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void savePreferedEditor(String userId, String provider, String uuid, String workspace) throws RepositoryException {
    Node node = nodeByUUID(uuid, workspace);
    if (node.canAddMixin(EXO_DOCUMENT)) {
      node.addMixin(EXO_DOCUMENT);
    }
    Node userPrefferences;
    if (!node.hasNode(userId)) {
      userPrefferences = node.addNode(userId, EXO_USER_PREFFERENCES);
    } else {
      userPrefferences = node.getNode(userId);
    }
    userPrefferences.setProperty(EXO_PREFFERED_EDITOR, provider);
    node.save();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String getPreferedEditor(String userId, String uuid, String workspace) throws RepositoryException {
    Node node = nodeByUUID(uuid, workspace);
    if (node.hasNode(userId)) {
      Node userPrefferences = node.getNode(userId);
      if (userPrefferences.hasProperty(EXO_PREFFERED_EDITOR)) {
        return userPrefferences.getProperty(EXO_PREFFERED_EDITOR).getString();
      }
    }
    return null;
  }
  
  /**
   * {@inheritDoc}
   */
  @Override
  public List<DocumentEditorProvider> getDocumentEditorProviders() {
    return unmodifiebleEditorProviders;
  }
  
  /**
   * {@inheritDoc}
   */
  @Override
  public DocumentEditorProvider getEditorProvider(String provider) throws DocumentEditorProviderNotFoundException {
    return getDocumentEditorProviders().stream()
                                .filter(editorProvider -> editorProvider.getProviderName().equals(provider))
                                .findFirst()
                                .orElseThrow(DocumentEditorProviderNotFoundException::new);
  }

  /**
   * Gets display name of current user. In case of any errors return current userId
   * 
   * @return the display name
   */
  protected String getCurrentUserDisplayName() {
    String userId = ConversationState.getCurrent().getIdentity().getUserId();
    try {
      return organizationService.getUserHandler().findUserByName(userId).getDisplayName();
    } catch (Exception e) {
      LOG.error("Error searching user " + userId, e);
      return userId;
    }
  }
  
  /**
   * Gets the user session.
   *
   * @param workspace the workspace
   * @return the user session
   * @throws RepositoryException the repository exception
   */
  protected Node nodeByUUID(String uuid, String workspace) throws RepositoryException {
    if (workspace == null) {
      workspace = repoService.getCurrentRepository().getConfiguration().getDefaultWorkspaceName();
    }
    SessionProvider sp = sessionProviderService.getSessionProvider(null);
    Session session = sp.getSession(workspace, repoService.getCurrentRepository());
    return session.getNodeByUUID(uuid);
  }
}
