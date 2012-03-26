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
package org.exoplatform.ecm.webui.utils;

import java.io.InputStream;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipInputStream;

import javax.imageio.ImageIO;
import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.nodetype.NodeDefinition;
import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.NodeTypeManager;

import org.apache.commons.lang.StringEscapeUtils;
import org.exoplatform.container.ExoContainer;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.container.definition.PortalContainerConfig;
import org.exoplatform.download.DownloadService;
import org.exoplatform.download.InputStreamDownloadResource;
import org.exoplatform.portal.resource.SkinConfig;
import org.exoplatform.portal.resource.SkinService;
import org.exoplatform.portal.webui.util.Util;
import org.exoplatform.portal.webui.workspace.UIPortalApplication;
import org.exoplatform.services.cms.documents.TrashService;
import org.exoplatform.services.cms.link.LinkManager;
import org.exoplatform.services.cms.templates.TemplateService;
import org.exoplatform.services.cms.thumbnail.ThumbnailService;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.jcr.ext.app.SessionProviderService;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.jcr.util.Text;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.resources.ResourceBundleService;
import org.exoplatform.services.wcm.publication.WCMComposer;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;
import org.exoplatform.web.application.RequestContext;
import org.exoplatform.services.security.ConversationState;
import org.exoplatform.services.security.Identity;
import org.exoplatform.services.security.IdentityRegistry;
import org.exoplatform.services.security.MembershipEntry;
import org.exoplatform.webui.application.WebuiRequestContext;
import org.exoplatform.webui.application.portlet.PortletRequestContext;
import org.exoplatform.webui.core.UIComponent;
import org.exoplatform.webui.core.UIContainer;
import org.exoplatform.webui.ext.UIExtension;
import org.exoplatform.webui.ext.UIExtensionManager;

/**
 * Created by The eXo Platform SARL Author : Dang Van Minh
 * minh.dang@exoplatform.com May 8, 2008 3:13:32 PM
 */
public class Utils {
  final public static String WORKSPACE_NAME = "workspace";
  final public static String JCR_PATH = "path";
  final public static String DRIVE_FOLDER = "allowCreateFolder";
  final public static String MIN_WIDTH = "minwidth";
  final public static String CB_DOCUMENT_NAME = "documentName";
  final public static String CB_SCRIPT_NAME = "scriptName";
  final public static String CB_REF_DOCUMENT = "reference";
  final public static String CB_CHILD_DOCUMENT = "child";
  final public static String CB_NB_PER_PAGE = "nbPerPage";
  final public static String CB_QUERY_STATEMENT = "queryStatement";
  final public static String CB_QUERY_ISNEW = "isAddNew";
  final public static String CB_QUERY_TYPE = "queryType";
  final public static String CB_QUERY_STORE = "queryStore";
  final public static String CB_QUERY_LANGUAGE = "queryLanguage";
  final public static String CB_VIEW_TOOLBAR = "viewToolbar";
  final public static String CB_VIEW_TAGMAP = "viewTagMap";
  final public static String CB_VIEW_COMMENT = "viewComment";
  final public static String CB_VIEW_VOTE = "viewVote";
  final public static String CB_SEARCH_LOCATION = "searchLocation";
  final public static String CB_ENABLE_SEARCH_LOCATION = "enableSearch";
  final public static String CB_FILTER_CATEGORY = "filterCategory";
  final static public String EXO_AUDITABLE = "exo:auditable";
  final public static String CB_BOX_TEMPLATE = "boxTemplate";
  final public static String CB_TEMPLATE = "template";
  final public static String CB_USECASE = "usecase";
  final public static String CB_ALLOW_PUBLISH = "isAllowPublish";

  final public static String FROM_PATH = "From Path";
  final public static String USE_DOCUMENT = "Document";
  final public static String USE_JCR_QUERY = "Using a JCR query";
  final public static String USE_SCRIPT = "Using a script";

  final public static String CB_USE_FROM_PATH = "path";
  final public static String CB_USE_DOCUMENT = "detail-document";
  final public static String CB_USE_JCR_QUERY = "query";
  final public static String CB_USE_SCRIPT = "script";

  final public static String SEMI_COLON = ";";
  final public static String COLON = ":";
  final public static String SLASH = "/";
  final public static String BACKSLASH = "\\";
  final public static String EXO_CREATED_DATE = "exo:dateCreated";
  final public static String EXO_DATETIME = "exo:datetime";
  final public static String EXO_MODIFIED_DATE = "exo:dateModified";
  final public static String EXO_OWNER = "exo:owner";

  final public static String SPECIALCHARACTER[] = { SEMI_COLON, COLON, SLASH,
      BACKSLASH, "'", "|", ">", "<", "\"", "?", "!", "@", "#", "$", "%", "^",
      "&", "*", "(", ")", "[", "]", "{", "}" };
  final public static String REPOSITORY = "repository";
  final public static String VIEWS = "views";
  final public static String DRIVE = "drive";
  final static public String TRASH_HOME_NODE_PATH = "trashHomeNodePath"
      ;
  final static public String TRASH_REPOSITORY = "trashRepository";
  final static public String TRASH_WORKSPACE = "trashWorkspace";
  final public static String JCR_INFO = "jcrInfo";
  final static public String NT_UNSTRUCTURED = "nt:unstructured";
  final static public String NT_FILE = "nt:file";
  final static public String NT_FOLDER = "nt:folder";
  final static public String NT_FROZEN = "nt:frozenNode";
  final static public String EXO_TITLE = "exo:title";
  final static public String EXO_SUMMARY = "exo:summary";
  final static public String EXO_RELATION = "exo:relation";
  @Deprecated
  final static public String EXO_TAXANOMY = "exo:taxonomy";
  final static public String EXO_TAXONOMY = "exo:taxonomy";
  final static public String EXO_IMAGE = "exo:image";
  final static public String EXO_ARTICLE = "exo:article";
  final static public String EXO_LANGUAGE = "exo:language";
  final static public String LANGUAGES = "languages";
  final static public String EXO_METADATA = "exo:metadata";
  final static public String MIX_REFERENCEABLE = "mix:referenceable";
  final static public String MIX_VERSIONABLE = "mix:versionable";
  final static public String NT_RESOURCE = "nt:resource";
  final static public String NT_BASE = "nt:base";
  final static public String DEFAULT = "default";
  final static public String JCR_CONTENT = "jcr:content";
  final static public String JCR_MIMETYPE = "jcr:mimeType";
  final static public String JCR_FROZEN = "jcr:frozenNode";
  final public static String JCR_LASTMODIFIED = "jcr:lastModified";
  final public static String JCR_PRIMARYTYPE = "jcr:primaryType";
  final static public String JCR_DATA = "jcr:data";
  final static public String JCR_SCORE = "jcr:score";
  final static public String EXO_ROLES = "exo:roles";
  final static public String EXO_TEMPLATEFILE = "exo:templateFile";
  final static public String EXO_TEMPLATE = "exo:template";
  final static public String EXO_ACTION = "exo:action";
  final static public String EXO_ACTIONS = "exo:actions";
  final static public String MIX_LOCKABLE = "mix:lockable";
  final static public String EXO_CATEGORIZED = "exo:categorized";
  final static public String EXO_CATEGORY = "exo:category";
  final static public String EXO_HIDDENABLE = "exo:hiddenable";
  final static public String EXO_ACCESSPERMISSION = "exo:accessPermissions";
  final static public String EXO_PERMISSIONS = "exo:permissions";
  final static public String EXO_FAVOURITE = "exo:favourite";
  final static public String EXO_FAVOURITE_FOLDER = "exo:favoriteFolder";
  final static public String EXO_FAVOURITER = "exo:favouriter";
  final static public String EXO_RESTOREPATH = "exo:restorePath";
  final static public String EXO_RESTORELOCATION = "exo:restoreLocation";
  final static public String EXO_RESTORE_WORKSPACE = "exo:restoreWorkspace";
  final static public String EXO_LASTMODIFIER = "exo:lastModifier";
  final static public String EXO_TRASH_FOLDER = "exo:trashFolder";
  final static public String EXO_TOTAL = "exo:total";
  final static public String EXO_WEBCONTENT = "exo:webContent";
  final static public String EXO_RSS_ENABLE = "exo:rss-enable";

  final static public String EXO_MUSICFOLDER = "exo:musicFolder";
  final static public String EXO_VIDEOFOLDER = "exo:videoFolder";
  final static public String EXO_PICTUREFOLDER = "exo:pictureFolder";
  final static public String EXO_DOCUMENTFOLDER = "exo:documentFolder";
  final static public String EXO_SEARCHFOLDER = "exo:searchFolder";
  final static public String MIX_COMMENTABLE = "mix:commentable";
  final static public String MIX_VOTABLE = "mix:votable";
  final static public String RMA_RECORD = "rma:record";
  final static public String EXO_SYMLINK = "exo:symlink";
  final static public String EXO_PRIMARYTYPE = "exo:primaryType";

  final static public String EXO_SORTABLE = "exo:sortable";

  final static public String[] SPECIFIC_FOLDERS = { EXO_MUSICFOLDER,
      EXO_VIDEOFOLDER, EXO_PICTUREFOLDER, EXO_DOCUMENTFOLDER, EXO_SEARCHFOLDER };

  final static public String[] FOLDERS = { NT_UNSTRUCTURED, NT_FOLDER };
  final static public String[] NON_EDITABLE_NODETYPES = { NT_UNSTRUCTURED,
      NT_FOLDER, NT_RESOURCE };
  final public static String[] CATEGORY_NODE_TYPES = { NT_FOLDER,
      NT_UNSTRUCTURED, EXO_TAXONOMY };
  final static public String CATEGORY_MANDATORY = "categoryMandatoryWhenFileUpload";
  final static public String UPLOAD_SIZE_LIMIT_MB = "uploadFileSizeLimitMB";
  final static public String FILE_VIEWER_EXTENSION_TYPE = "org.exoplatform.ecm.dms.FileViewer";
  final static public String MIME_TYPE = "mimeType";

  final static public String LOCALE_WEBUI_DMS = "locale.portlet.i18n.WebUIDms";

  final static public String REQUESTCONTEXT = "requestcontext";

  final static public String WORKSPACE_PARAM = "workspaceName";

  final static public String EMPTY = "";

  final static public String PUBLIC = "Public";
  final static public String GROUP = "Group";
  final static public String SITE = "Site";
  final static public String PRIVATE = "Private";
  final static public String URL_BACKTO ="backto";
  private static final Log LOG = ExoLogger.getLogger("webui.Utils");
  public Map<String, Object> maps_ = new HashMap<String, Object>();

  public static final String INPUT_TEXT_AREA = "TEXTAREA";
  public static final String INPUT_WYSIWYG = "WYSIWYG";
  public static final String INPUT_TEXT  = "TEXT";
  public static final String DEFAULT_CSS_NAME = "InlineText";
  public static final String LEFT2RIGHT = "left-to-right";
  public static final String RIGHT2LEFT = "right-to-left";

  public static String encodeHTML(String text) {
    return text.replaceAll("&", "&amp;").replaceAll("\"", "&quot;").replaceAll(
        "<", "&lt;").replaceAll(">", "&gt;");
  }

  public static String formatNodeName(String text) {
    return text.replaceAll("'", "\\\\'");
  }

  public static boolean isVersionable(Node node) throws RepositoryException {
    return node.isNodeType(MIX_VERSIONABLE);
  }

  public static boolean isTrashHomeNode(Node node) throws RepositoryException {
    return node.isNodeType(EXO_TRASH_FOLDER);
  }

  public static boolean isInTrash(Node node) throws RepositoryException {
    TrashService trashService = WCMCoreUtils.getService(TrashService.class);
    return trashService.isInTrash(node);
  }

    public static boolean isReferenceable(Node node) throws RepositoryException {
      return node.isNodeType(MIX_REFERENCEABLE);
    }

  static public class NodeTypeNameComparator implements Comparator<NodeType> {
    public int compare(NodeType n1, NodeType n2) throws ClassCastException {
      String name1 = n1.getName();
      String name2 = n2.getName();
      return name1.compareToIgnoreCase(name2);
    }
  }

  public static boolean isNameValid(String name, String[] regexpression) {
    for (String c : regexpression) {
      if (name.contains(c))
        return false;
    }
    return true;
  }

  public static boolean isNameEmpty(String name) {
    return (name == null || name.trim().length() == 0);
  }

  public static boolean isAuditable(Node node) throws RepositoryException {
    return node.isNodeType(EXO_AUDITABLE);
  }

  public static String getIndexName(Node node) throws RepositoryException {
    StringBuilder buffer = new StringBuilder(128);
    buffer.append(node.getName());
    int index = node.getIndex();
    if (index > 1) {
      buffer.append('[');
      buffer.append(index);
      buffer.append(']');

    }
    return buffer.toString();
  }

  @Deprecated
  public static List<String> getListAllowedFileType(Node currentNode,
                                                    String repository,
                                                    TemplateService templateService) throws Exception {
    return getListAllowedFileType(currentNode, templateService);
  }

  public static List<String> getListAllowedFileType(Node currentNode,
                                                    TemplateService templateService) throws Exception {
    List<String> nodeTypes = new ArrayList<String>();
    NodeTypeManager ntManager = currentNode.getSession().getWorkspace()
        .getNodeTypeManager();
    NodeType currentNodeType = currentNode.getPrimaryNodeType();
    NodeDefinition[] childDefs = currentNodeType.getChildNodeDefinitions();
    List<String> templates = templateService.getDocumentTemplates();
    try {
      for (int i = 0; i < templates.size(); i++) {
        String nodeTypeName = templates.get(i).toString();
        NodeType nodeType = ntManager.getNodeType(nodeTypeName);
        NodeType[] superTypes = nodeType.getSupertypes();
        boolean isCanCreateDocument = false;
        for (NodeDefinition childDef : childDefs) {
          NodeType[] requiredChilds = childDef.getRequiredPrimaryTypes();
          for (NodeType requiredChild : requiredChilds) {
            if (nodeTypeName.equals(requiredChild.getName())) {
              isCanCreateDocument = true;
              break;
            }
          }
          if (nodeTypeName.equals(childDef.getName()) || isCanCreateDocument) {
            if (!nodeTypes.contains(nodeTypeName))
              nodeTypes.add(nodeTypeName);
            isCanCreateDocument = true;
          }
        }
        if (!isCanCreateDocument) {
          for (NodeType superType : superTypes) {
            for (NodeDefinition childDef : childDefs) {
              for (NodeType requiredType : childDef.getRequiredPrimaryTypes()) {
                if (superType.getName().equals(requiredType.getName())) {
                  if (!nodeTypes.contains(nodeTypeName))
                    nodeTypes.add(nodeTypeName);
                  isCanCreateDocument = true;
                  break;
                }
              }
              if (isCanCreateDocument)
                break;
            }
            if (isCanCreateDocument)
              break;
          }
        }
      }
    } catch (Exception e) {
      if (LOG.isErrorEnabled()) {
        LOG.error("Unexpected error", e);
      }
    }
    return nodeTypes;
  }

  public static String getNodeTypeIcon(Node node, String appended, String mode)
      throws RepositoryException {
    StringBuilder str = new StringBuilder();
    if (node == null)
      return "";
    String nodeType = node.getPrimaryNodeType().getName();
    if (node.isNodeType(EXO_SYMLINK)) {
      LinkManager linkManager = Util.getUIPortal().getApplicationComponent(
          LinkManager.class);
      try {
        nodeType = node.getProperty(EXO_PRIMARYTYPE).getString();
        node = linkManager.getTarget(node);
        if (node == null)
          return "";
      } catch (Exception e) {
        return "";
      }
    }
    if (node.isNodeType(EXO_TRASH_FOLDER)) {
      nodeType = EXO_TRASH_FOLDER;
    }
    if (node.isNodeType(EXO_FAVOURITE_FOLDER))
      nodeType = EXO_FAVOURITE_FOLDER;
    if (nodeType.equals(NT_UNSTRUCTURED) || nodeType.equals(NT_FOLDER)) {
      for (String specificFolder : SPECIFIC_FOLDERS) {
        if (node.isNodeType(specificFolder)) {
          nodeType = specificFolder;
          break;
        }
      }
    }
    nodeType = nodeType.replace(':', '_') + appended;
    str.append(nodeType);
    str.append(" ");
    str.append("default16x16Icon");
    if (mode != null && mode.equalsIgnoreCase("Collapse"))
      str.append(' ').append(mode).append(nodeType);
    if (node.isNodeType(NT_FILE)) {
      if (node.hasNode(JCR_CONTENT)) {
        Node jcrContentNode = node.getNode(JCR_CONTENT);
        str.append(' ').append(
            jcrContentNode.getProperty(JCR_MIMETYPE).getString().replaceAll(
                "/|\\.", "_")).append(appended);
      }
    }
    return str.toString();
  }

  public static String getNodeTypeIcon(Node node, String appended)
      throws RepositoryException {
    return getNodeTypeIcon(node, appended, null);
  }

  public static NodeIterator getAuthorizedChildNodes(Node node)
      throws Exception {
    NodeIterator iter = node.getNodes();
    while (iter.hasNext()) {
      if (!PermissionUtil.canRead(iter.nextNode()))
        iter.remove();
    }
    return iter;
  }

  public static List<Node> getAuthorizedChildList(Node node) throws Exception {
    List<Node> children = new ArrayList<Node>();
    NodeIterator iter = node.getNodes();
    while (iter.hasNext()) {
      Node child = iter.nextNode();
      if (PermissionUtil.canRead(child))
        children.add(child);
    }
    return children;
  }

  public static boolean isLockTokenHolder(Node node) throws Exception {
    if (node.getLock().getLockToken() != null) {
      return true;
    }
    return false;
  }

  public static List<String> getMemberships() throws Exception {
    String userId = Util.getPortalRequestContext().getRemoteUser();
    List<String> userMemberships = new ArrayList<String>();
   userMemberships.add(userId);
    // here we must retrieve memberships of the user using the
    // IdentityRegistry Service instead of Organization Service to
    // allow JAAS based authorization
    Collection<MembershipEntry> memberships = getUserMembershipsFromIdentityRegistry(userId);
    if (memberships != null) {
      for (MembershipEntry membership : memberships) {
        String role = membership.getMembershipType() + ":" + membership.getGroup();
        userMemberships.add(role);
      }
   }
   return userMemberships;
  }

  /**
   * this method retrieves memberships of the user having the given id using the
   * IdentityRegistry service instead of the Organization service to allow JAAS
   * based authorization
   *
   * @param authenticatedUser the authenticated user id
   * @return a collection of MembershipEntry
   */
  private static Collection<MembershipEntry> getUserMembershipsFromIdentityRegistry(String authenticatedUser) {
    ExoContainer container = ExoContainerContext.getCurrentContainer();
    IdentityRegistry identityRegistry = (IdentityRegistry) container.getComponentInstanceOfType(IdentityRegistry.class);
    Identity currentUserIdentity = identityRegistry.getIdentity(authenticatedUser);
    return currentUserIdentity.getMemberships();
  }

  public static List<String> getGroups() throws Exception {
    ConversationState conversationState = ConversationState.getCurrent();
    Identity identity = conversationState.getIdentity();
    Set<String> groups = identity.getGroups();
    return new ArrayList<String>(groups);
  }

  public static String getNodeOwner(Node node) throws Exception {
    try {
      if (node.hasProperty(EXO_OWNER)) {
        return node.getProperty(EXO_OWNER).getString();
      }
    } catch (Exception e) {
      return null;
    }
    return null;
  }

  @Deprecated
  public static Node findNodeByUUID(String repository, String uuid)
      throws Exception {
    return findNodeByUUID(uuid);
  }

  public static Node findNodeByUUID(String uuid) throws Exception {
    RepositoryService repositoryService = Util.getUIPortal()
        .getApplicationComponent(RepositoryService.class);
    SessionProviderService sessionProviderService = Util.getUIPortal()
        .getApplicationComponent(SessionProviderService.class);
    SessionProvider sessionProvider = sessionProviderService
        .getSessionProvider(null);
    ManageableRepository manageableRepository = repositoryService.getCurrentRepository();
    Node node = null;
    for (String wsName : manageableRepository.getWorkspaceNames()) {
      try {
        node = sessionProvider.getSession(wsName, manageableRepository)
            .getNodeByUUID(uuid);
      } catch (ItemNotFoundException e) {
        continue;
      }
    }
    return node;
  }

  public static boolean isSymLink(Node node) throws RepositoryException {
    LinkManager linkManager = Util.getUIPortal().getApplicationComponent(
        LinkManager.class);
    return linkManager.isLink(node);
  }

  public static Node getNodeSymLink(Node node) throws Exception {
    LinkManager linkManager = Util.getUIPortal().getApplicationComponent(
        LinkManager.class);
    Node realNode = null;
    if (linkManager.isLink(node)) {
      if (linkManager.isTargetReachable(node)) {
        realNode = linkManager.getTarget(node);
      }
    } else {
      realNode = node;
    }
    return realNode;
  }

  public static InputStream extractFirstEntryFromZipFile(
      ZipInputStream zipStream) throws Exception {
    return zipStream.getNextEntry() == null ? null : zipStream;
  }

  public static String getThumbnailImage(Node node, String propertyName)
      throws Exception {
    ThumbnailService thumbnailService = Util.getUIPortal()
        .getApplicationComponent(ThumbnailService.class);
    if (node.getPrimaryNodeType().getName().equals(NT_FILE)) {
      String mimeType = node.getNode(JCR_CONTENT).getProperty(JCR_MIMETYPE)
          .getString();
      if (mimeType.startsWith("image")) {
        Node thumbnailNode = thumbnailService.addThumbnailNode(node);
        InputStream inputStream = node.getNode(JCR_CONTENT).getProperty(
            JCR_DATA).getStream();
        thumbnailService.createSpecifiedThumbnail(thumbnailNode, ImageIO
            .read(inputStream), propertyName);
      }
    }
    Node thumbnailNode = thumbnailService.getThumbnailNode(node);
    if (thumbnailNode != null && thumbnailNode.hasProperty(propertyName)) {
      DownloadService dservice = Util.getUIPortal().getApplicationComponent(
          DownloadService.class);
      InputStream input = thumbnailNode.getProperty(propertyName).getStream();
      InputStreamDownloadResource dresource = new InputStreamDownloadResource(
          input, "image");
      dresource.setDownloadName(node.getName());
      return dservice.getDownloadLink(dservice.addDownloadResource(dresource));
    }
    return null;
  }

  public static String calculateFileSize(double fileLengthLong) {
    int fileLengthDigitCount = Double.toString(fileLengthLong).length();
    double fileSizeKB = 0.0;
    String howBig = "";
    if (fileLengthDigitCount < 5) {
      fileSizeKB = Math.abs(fileLengthLong);
      howBig = "Byte(s)";
    } else if (fileLengthDigitCount >= 5 && fileLengthDigitCount <= 6) {
      fileSizeKB = Math.abs((fileLengthLong / 1024));
      howBig = "KB";
    } else if (fileLengthDigitCount >= 7 && fileLengthDigitCount <= 9) {
      fileSizeKB = Math.abs(fileLengthLong / (1024 * 1024));
      howBig = "MB";
    } else if (fileLengthDigitCount > 9) {
      fileSizeKB = Math.abs((fileLengthLong / (1024 * 1024 * 1024)));
      howBig = "GB";
    }
    String finalResult = roundTwoDecimals(fileSizeKB);
    return finalResult + " " + howBig;
  }

  private static String roundTwoDecimals(double d) {
    DecimalFormat twoDForm = new DecimalFormat("#.##");
    return twoDForm.format(d);
  }

  /**
   * Get resource bundle from PortalApplication resource bundle
   *
   * @param key
   * @return
   * @throws MissingResourceException
   */
  public static String getResourceBundle(String key)
      throws MissingResourceException {
    RequestContext context = Util.getPortalRequestContext();
    ResourceBundle res = context.getApplicationResourceBundle();
    return res.getString(key);
  }

  /**
   * Get resource bundle from given resource file
   *
   * @param name
   *          : resource file name
   * @param key
   *          : key
   * @param cl
   *          : ClassLoader to load resource file
   * @return
   */
  public static String getResourceBundle(String name, String key, ClassLoader cl) {
    ExoContainer container = ExoContainerContext.getCurrentContainer();
    Locale locale = WebuiRequestContext.getCurrentInstance().getLocale();
    ResourceBundleService resourceBundleService = (ResourceBundleService) container
        .getComponentInstanceOfType(ResourceBundleService.class);
    ResourceBundle resourceBundle = resourceBundleService.getResourceBundle(
        name, locale, cl);
    try {
      return resourceBundle.getString(key);
    } catch (MissingResourceException ex) {
      return key;
    }
  }

  public static String getRestContextName(String portalContainerName) {
    ExoContainer container = ExoContainerContext.getCurrentContainer();
    PortalContainerConfig portalContainerConfig = (PortalContainerConfig) container.
        getComponentInstance(PortalContainerConfig.class);
    return portalContainerConfig.getRestContextName(portalContainerName);
  }

  public static String getInlineEditingField(Node orgNode, String propertyName) throws Exception {
    String defaultValue ="";
    String idGenerator ="";
    Pattern p = Pattern.compile("[^a-zA-Z0-9]");
    Matcher m = p.matcher(propertyName);
    if (orgNode.hasProperty(propertyName)) {
      defaultValue = orgNode.getProperty(propertyName).getString();
    }
    idGenerator = m.replaceAll("_");
    return getInlineEditingField(orgNode, propertyName, defaultValue, INPUT_TEXT, idGenerator
                                  , DEFAULT_CSS_NAME, true);
  }
  /**
   *
   * @param orgNode         Processed node
   * @param propertyName    which property used for editing
   * @param inputType        input type for editing: TEXT, TEXTAREA, WYSIWYG
   * @param cssClass        class name for CSS, should implement: cssClass, [cssClass]Title
   *                         Edit[cssClass] as relative css
   *                         Should create the function: InlineEditor.presentationRequestChange[cssClass]
   *                         to request the rest-service
   * @param isGenericProperty  set as true to use generic javascript function, other wise, must create
   *                        the correctspond function InlineEditor.presentationRequestChange[cssClass]
   * @param arguments       Extra parameter for Input component (toolbar, width, height,.. for CKEditor/TextArea)
   * @return                String that can be put on groovy template
   * @throws                 Exception
   * @author                 vinh_nguyen
   */
  public static String getInlineEditingField(Node orgNode, String propertyName, String defaultValue, String inputType,
                    String idGenerator, String cssClass, boolean isGenericProperty, String... arguments) throws Exception {
    HashMap<String,String> parsedArguments = parseArguments(arguments) ;
    String height = parsedArguments.get(HEIGHT);
    String bDirection = parsedArguments.get(BUTTON_DIR);
    if ( org.exoplatform.wcm.webui.Utils.getCurrentMode().equals(WCMComposer.MODE_LIVE)) {
      if (orgNode.hasProperty(propertyName)) {
        try {
        	if(propertyName.equals(EXO_TITLE))
        		return StringEscapeUtils.escapeHtml(Text.unescapeIllegalJcrChars(orgNode.getProperty(propertyName).getString())) ;  
        	return orgNode.getProperty(propertyName).getString() ;
        } catch (Exception e) {
          return defaultValue;
        }
      }
      return defaultValue;
    }
    String currentValue =defaultValue;
    ResourceBundle resourceBundle;
    if (orgNode.hasProperty(propertyName)) {
      try {
        currentValue =  orgNode.getProperty(propertyName).getString() ;
      } catch (Exception e) {
        if (LOG.isWarnEnabled()) {
          LOG.warn(e.getMessage());
        }
      }
    }
    Locale locale = WebuiRequestContext.getCurrentInstance().getLocale();
    String language = locale.getLanguage();
    ResourceBundleService resourceBundleService = WCMCoreUtils.getService(ResourceBundleService.class);
    resourceBundle = resourceBundleService.getResourceBundle(LOCALE_WEBUI_DMS, locale);

    String portletRealID = org.exoplatform.wcm.webui.Utils.getRealPortletId((PortletRequestContext)
        WebuiRequestContext.getCurrentInstance());
    StringBuffer sb = new StringBuffer();
    StringBuffer actionsb = new StringBuffer();
    String repo = ((ManageableRepository)orgNode.getSession().getRepository()).getConfiguration().getName();
    String workspace = orgNode.getSession().getWorkspace().getName();
    String uuid = orgNode.getUUID();
    String strSuggestion="";
    String acceptButton = "";
    String cancelButton = "";
    portletRealID = portletRealID.replace('-', '_');
    String showBlockId = "Current" + idGenerator + "_" + portletRealID;
    String editBlockEditorID = "Edit" + idGenerator + "_" + portletRealID;
    String editFormID = "Edit" + idGenerator + "Form_" + portletRealID;
    String newValueInputId = "new" + idGenerator + "_" + portletRealID;
    String currentValueID = "old" + idGenerator + "_" + portletRealID;
    String siteName = org.exoplatform.portal.webui.util.Util.getPortalRequestContext().getPortalOwner();
    try {
      strSuggestion = resourceBundle.getString("UIPresentation.label.EditingSuggestion");
      acceptButton = resourceBundle.getString("UIPresentation.title.AcceptButton");
      cancelButton = resourceBundle.getString("UIPresentation.title.CancelButton");
    } catch (Exception e){
      if (LOG.isWarnEnabled()) {
        LOG.warn(e.getMessage());
      }
    }
    actionsb.append(" return InlineEditor.presentationRequestChange");

    if (isGenericProperty) {
      actionsb.append("Property").append("('").append("/property?', '").append(propertyName).append("', '");
    }else {
      actionsb.append(cssClass).append("('");
    }
    actionsb.append(currentValueID).append("', '").append(newValueInputId).append("', '").append(repo)
    .append("', '").append(workspace).append("', '").append(uuid).append("', '")
    .append(editBlockEditorID).append("', '").append(showBlockId).append("', '").append(siteName).append("', '").append(language);

    if (inputType.equals(INPUT_WYSIWYG)) {
      actionsb.append("', 1);");
    }else {
      actionsb.append("');");
    }
    String strAction = actionsb.toString();

    sb.append("<div class=\"InlineEditing\">\n");
    sb.append("\n<div id=\"").append(showBlockId).append("\" Class=\"").append(cssClass).append("\"");
    sb.append("title=\"").append(strSuggestion).append("\"");
    sb.append(" onDblClick=\"InlineEditor.presentationSwitchBlock('").append(showBlockId).
       append("', '").append(editBlockEditorID).append("');\"");
    sb.append("onmouseout=\"this.className='").append(cssClass).
       append("';\" onblur=\"this.className='").append(cssClass).
       append("';\" onfocus=\"this.className='").append(cssClass).append("Hover").
       append("';\" onmouseover=\"this.className='").
       append(cssClass).append("Hover';\">").
       append(currentValue).
       append("</div>\n");
    sb.append("\t<div id=\"").append(editBlockEditorID).append("\" class=\"Edit").append(cssClass).append("\">\n");
    sb.append("\t\t<form name=\"").append(editFormID).append("\" id=\"").append(editFormID).
       append("\" onSubmit=\"").append(strAction).append("\">\n");
    sb.append("<DIV style=\"display:none; visible:hidden\" id=\"").append(currentValueID).
       append("\" name=\"").append(currentValueID).append("\">").append(currentValue).append("</DIV>");

    if (bDirection!=null && bDirection.equals(LEFT2RIGHT)) {
      sb.append("\t\t<a href=\"#\" class =\"AcceptButton\" style=\"float:left\" onclick=\"")
        .append(strAction)
        .append("\" title=\"" + acceptButton + "\">&nbsp;</a>\n");
      sb.append("\t\t<a href=\"#\" class =\"CancelButton\" style=\"float:left\" ").
         append("onClick=\"InlineEditor.presentationSwitchBlock('");
      sb.append(editBlockEditorID)
        .append("', '")
        .append(showBlockId)
        .append("');\" title=\"" + cancelButton + "\">&nbsp;</a>\n");
    } else {
      sb.append("\t\t<a href=\"#\" class =\"CancelButton\" ")
        .append("onClick=\"InlineEditor.presentationSwitchBlock('");
      sb.append(editBlockEditorID)
        .append("', '")
        .append(showBlockId)
        .append("');\" title=\"" + cancelButton + "\">&nbsp;</a>\n");
      sb.append("\t\t<a href=\"#\" class =\"AcceptButton\" onclick=\"")
        .append(strAction)
        .append("\" title=\"" + acceptButton + "\">&nbsp;</a>\n");
    }
    sb.append("\t\t<div class=\"Edit").append(cssClass).append("Input\">\n ");

    if (inputType.equalsIgnoreCase(INPUT_WYSIWYG)) {
      sb.append(createCKEditorField(newValueInputId, currentValue, parsedArguments));
    }else if (inputType.equalsIgnoreCase(INPUT_TEXT_AREA)){
      sb.append("\t\t<TEXTAREA ").append("\" name =\"");
      sb.append(newValueInputId).append("\" id =\"").append(newValueInputId).append("\"");
      if (height!=null && height.length()>0) {
        sb.append(" style =\"height:").append(height);
        if (!height.endsWith("px")) {
          sb.append("px;");
        }
        sb.append("\"");
      }
      sb.append(">");
      sb.append(currentValue).append("</TEXTAREA>");
    }else if (inputType.equalsIgnoreCase(INPUT_TEXT)) {
      sb.append("\t\t<input type=\"TEXT\" name =\"");
      sb.append(newValueInputId).append("\" id =\"").append(newValueInputId).
         append("\" value=\"").append(currentValue).append("\"/>");
    }

    sb.append("\n\t\t</div>\n\t</form>\n</div>\n\n</div>");
    return sb.toString();
  }

  /**
   *
   * @param name
   * @param width
   * @param height
   * @param value_
   * @return
   */
  private static String createCKEditorField(String name, String value_, HashMap<String,String> arguments) {
    String toolbar = arguments.get(TOOLBAR);
    String passedCSS = arguments.get(CSS);

    if (toolbar == null) toolbar = "BasicWCM";
    StringBuffer contentsCss = new StringBuffer();
    contentsCss.append("[");
    SkinService skinService = WCMCoreUtils.getService(SkinService.class);
    String skin = Util.getUIPortalApplication().getUserPortalConfig().getPortalConfig().getSkin();
    String portal = Util.getUIPortal().getName();
    Collection<SkinConfig> portalSkins = skinService.getPortalSkins(skin);
    SkinConfig customSkin = skinService.getSkin(portal, Util.getUIPortalApplication()
        .getUserPortalConfig()
        .getPortalConfig()
        .getSkin());
    if (customSkin != null) portalSkins.add(customSkin);
    for (SkinConfig portalSkin : portalSkins) {
      contentsCss.append("'").append(portalSkin.createURL()).append("',");
    }
    contentsCss.delete(contentsCss.length() - 1, contentsCss.length());
    contentsCss.append("]");

    StringBuffer buffer = new StringBuffer();
     buffer.append("<div style=\"display:none\">" +
         "<textarea id='cssContent" + name + "' name='cssContent" + name + "'>" + passedCSS + "</textarea></div>\n");

    if (value_!=null) {
      buffer.append("<textarea id='" + name + "' name='" + name + "'>" + value_ + "</textarea>\n");
    }else {
      buffer.append("<textarea id='" + name + "' name='" + name + "'></textarea>\n");
    }
    buffer.append("<script type='text/javascript'>\n");
    buffer.append("  //<![CDATA[ \n");
    buffer.append("    var instances = CKEDITOR.instances['" + name + "']; if (instances) instances.destroy(true);\n");
    buffer.append("    CKEDITOR.replace('" + name + "', {toolbar:'" + toolbar + "', width:'98%', height: 200, contentsCss:" +
        contentsCss + ", ignoreEmptyParagraph:true});\n");
    buffer.append("    CKEDITOR.instances['" + name + "'].on(\"instanceReady\", function(){  ");
    buffer.append("       eXo.ecm.CKEditor.insertCSS('" + name + "', 'cssContent" + name + "');\n");
    buffer.append("       });");
    buffer.append("  //]]> \n");
    buffer.append("</script>\n");
    return buffer.toString();
  }
  protected static final String SEPARATOR  = "=";
  protected static final String TOOLBAR    = "toolbar";
  protected static final String CSS        = "CSSData";
  protected static final String HEIGHT     = "height";
  protected static final String BUTTON_DIR = "button_direction";
  protected static final String PREV_HTML  = "prev_html";
  protected static final String POST_HTML  = "post_html";
  private static HashMap<String,String> parseArguments(String... arguments) {
    HashMap<String,String> map = new HashMap<String,String>() ;
    int sIndex =-1;
    for(String argument:arguments) {
      String value = null;
      sIndex = argument.indexOf(SEPARATOR);
      if(sIndex>0) {
        value = argument.substring(sIndex+1) ;
      }else {
        continue;
      }
      if (argument.startsWith(JCR_PATH)) {
        map.put(JCR_PATH, value); continue;
      } else if (argument.startsWith(TOOLBAR)) {
        map.put(TOOLBAR, value); continue;
      } else if (argument.startsWith(CSS)) {
        map.put(CSS, value); continue;
      } else if (argument.startsWith(HEIGHT)) {
        map.put(HEIGHT, value); continue;
      } else if (argument.startsWith(BUTTON_DIR)) {
        map.put(BUTTON_DIR, value); continue;
      } else if (argument.startsWith(PREV_HTML)) {
        map.put(PREV_HTML, value); continue;
      } else if (argument.startsWith(POST_HTML)) {
        map.put(POST_HTML, value); continue;
      }
    }
    return map;
  }
  
  /**
   * Gets the title.
   *
   * @param node the node
   *
   * @return the title
   *
   * @throws Exception the exception
   */
  public static String getTitle(Node node) throws Exception {
    String title = null;
    if (node.hasProperty("exo:title")) {
      title = node.getProperty("exo:title").getValue().getString();
    } else if (node.hasNode("jcr:content")) {
      Node content = node.getNode("jcr:content");
      if (content.hasProperty("dc:title")) {
        try {
          title = content.getProperty("dc:title").getValues()[0].getString();
        } catch(Exception ex) {
          // Do nothing
        }
      }
    } 
    if ((title==null) || ((title!=null) && (title.trim().length()==0))) {
      title = node.getName();
    }
    return StringEscapeUtils.escapeHtml(Text.unescapeIllegalJcrChars(title));
  }
  
  /**
   *
   * @param node
   * @return
   * @throws Exception
   * @Author Nguyen The Vinh from ExoPlatform
   */
  public static String getTitleWithSymlink(Node node) throws Exception {
    String title = null;
    Node nProcessNode = node;
    if (title==null) {
      nProcessNode = node;
      if (nProcessNode.hasProperty("exo:title")) {
        title = nProcessNode.getProperty("exo:title").getValue().getString();
      }
      if (nProcessNode.hasNode("jcr:content")) {
        Node content = nProcessNode.getNode("jcr:content");
        if (content.hasProperty("dc:title")) {
          try {
            title = content.getProperty("dc:title").getValues()[0].getString();
          } catch (Exception e) {
            title = null;
          }
        }
      }
      if (title != null) title = title.trim();
    }
    if (title !=null && title.length()>0) return Text.unescapeIllegalJcrChars(title);
    if (isSymLink(node)) {
      nProcessNode = getNodeSymLink(nProcessNode);
      if (nProcessNode == null ) {
        nProcessNode = node;
      }
      if (nProcessNode.hasProperty("exo:title")) {
        title = nProcessNode.getProperty("exo:title").getValue().getString();
      }
      if (nProcessNode.hasNode("jcr:content")) {
        Node content = nProcessNode.getNode("jcr:content");
        if (content.hasProperty("dc:title")) {
          try {
            title = content.getProperty("dc:title").getValues()[0].getString();
          } catch (Exception e) {
            title = null;
          }
        }
      }
      if (title != null) {
        title = title.trim();
        if (title.length()==0) title = null;
      }
    }

    if (title ==null) title = nProcessNode.getName();
    return Text.unescapeIllegalJcrChars(title);
  }

  /**
   * Get UIComponent to process render a node which has specified mimeType
   * @param mimeType
   * @param container
   * @return
   * @throws Exception
   */
  public static UIComponent getUIComponent(String mimeType, UIContainer container) throws Exception {
    UIExtensionManager manager = WCMCoreUtils.getService(UIExtensionManager.class);
    List<UIExtension> extensions = manager.getUIExtensions(FILE_VIEWER_EXTENSION_TYPE);
    Map<String, Object> context = new HashMap<String, Object>();
    context.put(MIME_TYPE, mimeType);
    for (UIExtension extension : extensions) {
      UIComponent uiComponent = manager.addUIExtension(extension, context, container);
      if(uiComponent != null) return uiComponent;
    }
    return null;
  }

}
