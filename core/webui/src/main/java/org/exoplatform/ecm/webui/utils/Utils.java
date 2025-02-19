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
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.security.AccessControlException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.zip.ZipInputStream;

import javax.imageio.ImageIO;
import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.nodetype.NodeDefinition;
import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.NodeTypeManager;

import org.apache.commons.lang3.StringUtils;

import org.exoplatform.commons.api.settings.SettingService;
import org.exoplatform.commons.api.settings.SettingValue;
import org.exoplatform.commons.api.settings.data.Context;
import org.exoplatform.commons.api.settings.data.Scope;
import org.exoplatform.commons.utils.CommonsUtils;
import org.exoplatform.container.ExoContainer;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.container.definition.PortalContainerConfig;
import org.exoplatform.container.xml.PortalContainerInfo;
import org.exoplatform.download.DownloadService;
import org.exoplatform.download.InputStreamDownloadResource;
import org.exoplatform.portal.webui.util.Util;
import org.exoplatform.services.cms.documents.TrashService;
import org.exoplatform.services.cms.drives.DriveData;
import org.exoplatform.services.cms.link.LinkManager;
import org.exoplatform.services.cms.thumbnail.ThumbnailService;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.jcr.ext.app.SessionProviderService;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.jcr.impl.Constants;
import org.exoplatform.services.jcr.impl.core.NodeImpl;
import org.exoplatform.services.jcr.impl.core.nodetype.NodeTypeImpl;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.resources.ResourceBundleService;
import org.exoplatform.services.security.ConversationState;
import org.exoplatform.services.security.Identity;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;
import org.exoplatform.web.application.RequestContext;
import org.exoplatform.webui.application.WebuiRequestContext;
import org.exoplatform.webui.core.UIComponent;
import org.exoplatform.webui.core.UIContainer;
import org.exoplatform.webui.ext.UIExtension;
import org.exoplatform.webui.ext.UIExtensionManager;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;

/**
 * Created by The eXo Platform SARL Author : Dang Van Minh
 * minh.dang@exoplatform.com May 8, 2008 3:13:32 PM
 */
public class Utils {
  final public static String   WORKSPACE_NAME             = "workspace";

  final public static String   JCR_PATH                   = "path";

  final public static String   DRIVE_FOLDER               = "allowCreateFolder";

  final public static String   MIN_WIDTH                  = "minwidth";

  final public static String   CB_DOCUMENT_NAME           = "documentName";

  final public static String   CB_SCRIPT_NAME             = "scriptName";

  final public static String   CB_REF_DOCUMENT            = "reference";

  final public static String   CB_CHILD_DOCUMENT          = "child";

  final public static String   CB_NB_PER_PAGE             = "nbPerPage";

  final public static String   CB_QUERY_STATEMENT         = "queryStatement";

  final public static String   CB_QUERY_ISNEW             = "isAddNew";

  final public static String   CB_QUERY_TYPE              = "queryType";

  final public static String   CB_QUERY_STORE             = "queryStore";

  final public static String   CB_QUERY_LANGUAGE          = "queryLanguage";

  final public static String   CB_VIEW_TOOLBAR            = "viewToolbar";

  final public static String   CB_VIEW_TAGMAP             = "viewTagMap";

  final public static String   CB_VIEW_COMMENT            = "viewComment";

  final public static String   CB_VIEW_VOTE               = "viewVote";

  final public static String   CB_SEARCH_LOCATION         = "searchLocation";

  final public static String   CB_ENABLE_SEARCH_LOCATION  = "enableSearch";

  final public static String   CB_FILTER_CATEGORY         = "filterCategory";

  final static public String   EXO_AUDITABLE              = "exo:auditable";

  final public static String   CB_BOX_TEMPLATE            = "boxTemplate";

  final public static String   CB_TEMPLATE                = "template";

  final public static String   CB_USECASE                 = "usecase";

  final public static String   CB_ALLOW_PUBLISH           = "isAllowPublish";

  final public static String   FROM_PATH                  = "From Path";

  final public static String   USE_DOCUMENT               = "Document";

  final public static String   USE_JCR_QUERY              = "Using a JCR query";

  final public static String   USE_SCRIPT                 = "Using a script";

  final public static String   CB_USE_FROM_PATH           = "path";

  final public static String   CB_USE_DOCUMENT            = "detail-document";

  final public static String   CB_USE_JCR_QUERY           = "query";

  final public static String   CB_USE_SCRIPT              = "script";

  final public static String   SEMI_COLON                 = ";";

  final public static String   COLON                      = ":";

  final public static String   SLASH                      = "/";

  final public static String   BACKSLASH                  = "\\";

  final public static String   EXO_CREATED_DATE           = "exo:dateCreated";

  final public static String   EXO_DATETIME               = "exo:datetime";

  final public static String   EXO_MODIFIED_DATE          = "exo:dateModified";

  final public static String   EXO_OWNER                  = "exo:owner";

  final public static String   SPECIALCHARACTER[]         = { SEMI_COLON, SLASH, BACKSLASH, "|", ">", "<", "\"", "?", "!", "#",
      "$", "&", "*", "(", ")", "{", "}", "[", "]", ":", ".", "'" };

  final public static String   REPOSITORY                 = "repository";

  final public static String   VIEWS                      = "views";

  final public static String   DRIVE                      = "drive";

  final static public String   TRASH_HOME_NODE_PATH       = "trashHomeNodePath";

  final static public String   TRASH_REPOSITORY           = "trashRepository";

  final static public String   TRASH_WORKSPACE            = "trashWorkspace";

  final public static String   JCR_INFO                   = "jcrInfo";

  final static public String   NT_UNSTRUCTURED            = "nt:unstructured";

  final static public String   NT_FILE                    = "nt:file";

  final static public String   NT_FOLDER                  = "nt:folder";

  final static public String   NT_FROZEN                  = "nt:frozenNode";

  final static public String   EXO_TITLE                  = "exo:title";

  final static public String   EXO_SUMMARY                = "exo:summary";

  final static public String   EXO_RELATION               = "exo:relation";

  final static public String   EXO_TAXONOMY               = "exo:taxonomy";

  final static public String   EXO_IMAGE                  = "exo:image";

  final static public String   EXO_LANGUAGE               = "exo:language";

  final static public String   LANGUAGES                  = "languages";

  final static public String   EXO_METADATA               = "exo:metadata";

  final static public String   MIX_REFERENCEABLE          = "mix:referenceable";

  final static public String   MIX_VERSIONABLE            = "mix:versionable";

  final static public String   NT_RESOURCE                = "nt:resource";

  final static public String   NT_BASE                    = "nt:base";

  final static public String   DEFAULT                    = "default";

  final static public String   JCR_CONTENT                = "jcr:content";

  final static public String   JCR_CONTENT_DESCRIPTION    = "jcr:content/dc:description";

  final static public String   JCR_MIMETYPE               = "jcr:mimeType";

  final static public String   JCR_FROZEN                 = "jcr:frozenNode";

  final public static String   JCR_LASTMODIFIED           = "jcr:lastModified";

  final public static String   JCR_PRIMARYTYPE            = "jcr:primaryType";

  final static public String   JCR_DATA                   = "jcr:data";

  final static public String   JCR_SCORE                  = "jcr:score";

  final static public String   EXO_ROLES                  = "exo:roles";

  final static public String   EXO_TEMPLATEFILE           = "exo:templateFile";

  final static public String   EXO_TEMPLATE               = "exo:template";

  final static public String   EXO_ACTION                 = "exo:action";

  final static public String   EXO_ACTIONS                = "exo:actions";

  final static public String   MIX_LOCKABLE               = "mix:lockable";

  final static public String   EXO_CATEGORIZED            = "exo:categorized";

  final static public String   EXO_CATEGORY               = "exo:category";

  final static public String   EXO_HIDDENABLE             = "exo:hiddenable";

  final static public String   EXO_ACCESSPERMISSION       = "exo:accessPermissions";

  final static public String   EXO_PERMISSIONS            = "exo:permissions";

  final static public String   EXO_FAVOURITE              = "exo:favourite";

  final static public String   EXO_FAVOURITE_FOLDER       = "exo:favoriteFolder";

  final static public String   EXO_FAVOURITER             = "exo:favouriter";

  final static public String   EXO_RESTOREPATH            = "exo:restorePath";

  final static public String   EXO_RESTORELOCATION        = "exo:restoreLocation";

  final static public String   EXO_RESTORE_WORKSPACE      = "exo:restoreWorkspace";

  final static public String   EXO_LASTMODIFIER           = "exo:lastModifier";

  final static public String   EXO_TRASH_FOLDER           = "exo:trashFolder";

  final static public String   EXO_TOTAL                  = "exo:total";

  final static public String   EXO_WEBCONTENT             = "exo:webContent";

  final static public String   EXO_RSS_ENABLE             = "exo:rss-enable";

  final static public String   EXO_COMMENTS               = "exo:comments";

  final static public String   EXO_MUSICFOLDER            = "exo:musicFolder";

  final static public String   EXO_VIDEOFOLDER            = "exo:videoFolder";

  final static public String   EXO_PICTUREFOLDER          = "exo:pictureFolder";

  final static public String   EXO_DOCUMENTFOLDER         = "exo:documentFolder";

  final static public String   EXO_SEARCHFOLDER           = "exo:searchFolder";

  final static public String   MIX_COMMENTABLE            = "mix:commentable";

  final static public String   MIX_VOTABLE                = "mix:votable";

  final static public String   EXO_SYMLINK                = "exo:symlink";

  final static public String   EXO_PRIMARYTYPE            = "exo:primaryType";

  final static public String   INLINE_DRAFT               = "Draft";

  final static public String   INLINE_PUBLISHED           = "Published";

  final static public String   EXO_SORTABLE               = "exo:sortable";

  final static public String   EXO_RISIZEABLE             = "exo:documentSize";

  final static public String   FLASH_MIMETYPE             = "flash";

  final static public String[] SPECIFIC_FOLDERS           = { EXO_MUSICFOLDER, EXO_VIDEOFOLDER, EXO_PICTUREFOLDER,
      EXO_DOCUMENTFOLDER, EXO_SEARCHFOLDER               };

  final static public String[] FOLDERS                    = { NT_UNSTRUCTURED, NT_FOLDER };

  final static public String[] NON_EDITABLE_NODETYPES     = { NT_UNSTRUCTURED, NT_FOLDER, NT_RESOURCE };

  final public static String[] CATEGORY_NODE_TYPES        = { NT_FOLDER, NT_UNSTRUCTURED, EXO_TAXONOMY };

  final static public String   CATEGORY_MANDATORY         = "categoryMandatoryWhenFileUpload";

  final static public String   UPLOAD_SIZE_LIMIT_MB       = "uploadFileSizeLimitMB";

  final static public String   FILE_VIEWER_EXTENSION_TYPE = "org.exoplatform.ecm.dms.FileViewer";

  final static public String   MIME_TYPE                  = "mimeType";

  final static public String   LOCALE_WEBUI_DMS           = "locale.portlet.i18n.WebUIDms";

  final static public String   REQUESTCONTEXT             = "requestcontext";

  final static public String   WORKSPACE_PARAM            = "workspaceName";

  final static public String   SPACE_GROUP                 = "/spaces";

  final static public String   SITES_PATH                 = "/sites";

  final static public String   COLLABORATION_WS           = "collaboration";

  final static public int      USER_DEPTH                 = 5;

  final static public String   EMPTY                      = "";

  final static public String   PUBLIC                     = "Public";

  final static public String   GROUP                      = "Group";

  final static public String   SITE                       = "Site";

  final static public String   PRIVATE                    = "Private";

  final static public String   URL_BACKTO                 = "backto";
  public static final String   INPUT_TEXT_AREA            = "TEXTAREA";
  public static final String   INPUT_WYSIWYG              = "WYSIWYG";
  public static final String   INPUT_TEXT                 = "TEXT";
  public static final String   DEFAULT_CSS_NAME           = "InlineText";
  public static final String   LEFT2RIGHT                 = "left-to-right";
  public static final String   RIGHT2LEFT                 = "right-to-left";
  protected static final String SEPARATOR         = "=";
  protected static final String TOOLBAR           = "toolbar";
  protected static final String CSS               = "CSSData";
  protected static final String HEIGHT            = "height";
  protected static final String BUTTON_DIR        = "button_direction";
  protected static final String PREV_HTML         = "prev_html";
  protected static final String POST_HTML         = "post_html";
  protected static final String FAST_PUBLISH_LINK = "fast_publish";
  private static final Log     LOG                        = ExoLogger.getLogger(Utils.class.getName());

  public static String encodeHTML(String text) {
    return text.replaceAll("&", "&amp;").replaceAll("\"", "&quot;").replaceAll("<", "&lt;").replaceAll(">", "&gt;");
  }

  public static String formatNodeName(String text) {
    return text.replaceAll("('|\")", "\\\\'");
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

  /** check a symlink node and its target are in Trash or not */
  public static boolean targetNodeAndLinkInTrash(Node currentNode) throws Exception {
    if (Utils.isInTrash(currentNode) && Utils.isSymLink(currentNode)) {
      Node targetNode = Utils.getNodeSymLink(currentNode);
      if (Utils.isInTrash(targetNode)) {
        return true;
      }
    }
    return false;
  }

  /** check if we can restore a node */
  public static boolean isAbleToRestore(Node currentNode) throws Exception {
    String restorePath;
    String restoreWorkspace;
    Node restoreLocationNode;

    if (!Utils.isInTrash(currentNode)) {
      return false;
    }

    // return false if the node is exo:actions
    if (Utils.EXO_ACTIONS.equals(currentNode.getName()) && Utils.isInTrash(currentNode)) {
      return false;
    }

    // return false if the target has been already in Trash.
    if (Utils.targetNodeAndLinkInTrash(currentNode)) {
      return false;
    }

    if (ConversationState.getCurrent().getIdentity().getUserId().equalsIgnoreCase(WCMCoreUtils.getSuperUser())) {
      return true;
    }

    if (currentNode.isNodeType(TrashService.EXO_RESTORE_LOCATION)) {
      restorePath = currentNode.getProperty(TrashService.RESTORE_PATH).getString();
      restoreWorkspace = currentNode.getProperty(TrashService.RESTORE_WORKSPACE).getString();
      restorePath = restorePath.substring(0, restorePath.lastIndexOf("/"));
    } else {
      // Is not a deleted node, may be groovy action, hidden node,...
      return false;
    }
    Session session = WCMCoreUtils.getUserSessionProvider().getSession(restoreWorkspace, WCMCoreUtils.getRepository());
    try {
      if (restorePath == null || restorePath.length() == 0) {
        restoreLocationNode = session.getRootNode();
      } else {
        restoreLocationNode = (Node) session.getItem(restorePath);
      }
    } catch (Exception e) {
      return false;
    }
    return PermissionUtil.canAddNode(restoreLocationNode);
  }

  public static boolean isReferenceable(Node node) throws RepositoryException {
    return node.isNodeType(MIX_REFERENCEABLE);
  }

  public static boolean isNameValid(String name, String[] regexpression) {
    for (String c : regexpression) {
      if (name == null || name.contains(c))
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

  public static String getNodeTypeIcon(Node node, String appended, String mode) throws RepositoryException {
    return org.exoplatform.services.cms.impl.Utils.getNodeTypeIcon(node, appended, mode);
  }

  public static String getNodeTypeIcon(Node node, String appended) throws RepositoryException {
    return org.exoplatform.services.cms.impl.Utils.getNodeTypeIcon(node, appended);
  }

  public static NodeIterator getAuthorizedChildNodes(Node node) throws Exception {
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
    return org.exoplatform.services.cms.impl.Utils.getMemberships();
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

  public static Node findNodeByUUID(String uuid) throws Exception {
    RepositoryService repositoryService = Util.getUIPortal().getApplicationComponent(RepositoryService.class);
    SessionProviderService sessionProviderService = Util.getUIPortal().getApplicationComponent(SessionProviderService.class);
    SessionProvider sessionProvider = sessionProviderService.getSessionProvider(null);
    ManageableRepository manageableRepository = repositoryService.getCurrentRepository();
    Node node = null;
    for (String wsName : manageableRepository.getWorkspaceNames()) {
      try {
        node = sessionProvider.getSession(wsName, manageableRepository).getNodeByUUID(uuid);
      } catch (ItemNotFoundException e) {
        continue;
      }
    }
    return node;
  }

  public static boolean isSymLink(Node node) throws RepositoryException {
    LinkManager linkManager = Util.getUIPortal().getApplicationComponent(LinkManager.class);
    return linkManager.isLink(node);
  }

  public static Node getNodeSymLink(Node node) throws Exception {
    LinkManager linkManager = Util.getUIPortal().getApplicationComponent(LinkManager.class);
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

  public static InputStream extractFirstEntryFromZipFile(ZipInputStream zipStream) throws Exception {
    return zipStream.getNextEntry() == null ? null : zipStream;
  }

  public static String getThumbnailImage(InputStream input, String downloadName) throws Exception {
    DownloadService dservice = WCMCoreUtils.getService(DownloadService.class);
    InputStreamDownloadResource dresource = new InputStreamDownloadResource(input, "image");
    dresource.setDownloadName(downloadName);
    return dservice.getDownloadLink(dservice.addDownloadResource(dresource));
  }

  public static String getThumbnailImage(Node node, String propertyName) throws Exception {
    ThumbnailService thumbnailService = Util.getUIPortal().getApplicationComponent(ThumbnailService.class);
    if (node.isNodeType(NT_FILE)) {
      String mimeType = node.getNode(JCR_CONTENT).getProperty(JCR_MIMETYPE).getString();
      if (mimeType.startsWith("image")) {
        Node thumbnailNode = thumbnailService.addThumbnailNode(node);
        InputStream inputStream = node.getNode(JCR_CONTENT).getProperty(JCR_DATA).getStream();
        thumbnailService.createSpecifiedThumbnail(thumbnailNode, ImageIO.read(inputStream), propertyName);
      }
    }
    Node thumbnailNode = thumbnailService.getThumbnailNode(node);
    if (thumbnailNode != null && thumbnailNode.hasProperty(propertyName)) {
      DownloadService dservice = Util.getUIPortal().getApplicationComponent(DownloadService.class);
      InputStream input = thumbnailNode.getProperty(propertyName).getStream();
      InputStreamDownloadResource dresource = new InputStreamDownloadResource(input, "image");
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
  public static String getResourceBundle(String key) throws MissingResourceException {
    RequestContext context = Util.getPortalRequestContext();
    ResourceBundle res = context.getApplicationResourceBundle();
    return res.getString(key);
  }

  /**
   * Get resource bundle from given resource file
   *
   * @param name : resource file name
   * @param key : key
   * @param cl : ClassLoader to load resource file
   * @return
   */
  public static String getResourceBundle(String name, String key, ClassLoader cl) {
    Locale locale = WebuiRequestContext.getCurrentInstance().getLocale();
    ResourceBundleService resourceBundleService = WCMCoreUtils.getService(ResourceBundleService.class);
    ResourceBundle resourceBundle = resourceBundleService.getResourceBundle(name, locale, cl);
    try {
      return resourceBundle.getString(key);
    } catch (MissingResourceException ex) {
      return key;
    }
  }

  public static String getRestContextName(String portalContainerName) {
    ExoContainer container = ExoContainerContext.getCurrentContainer();
    PortalContainerConfig portalContainerConfig = (PortalContainerConfig) container.getComponentInstance(PortalContainerConfig.class);
    return portalContainerConfig.getRestContextName(portalContainerName);
  }

  private static HashMap<String, String> parseArguments(String... arguments) {
    HashMap<String, String> map = new HashMap<String, String>();
    int sIndex = -1;
    for (String argument : arguments) {
      String value = null;
      sIndex = argument.indexOf(SEPARATOR);
      if (sIndex > 0) {
        value = argument.substring(sIndex + 1);
      } else {
        continue;
      }
      if (argument.startsWith(JCR_PATH)) {
        map.put(JCR_PATH, value);
        continue;
      } else if (argument.startsWith(TOOLBAR)) {
        map.put(TOOLBAR, value);
        continue;
      } else if (argument.startsWith(CSS)) {
        map.put(CSS, value);
        continue;
      } else if (argument.startsWith(HEIGHT)) {
        map.put(HEIGHT, value);
        continue;
      } else if (argument.startsWith(BUTTON_DIR)) {
        map.put(BUTTON_DIR, value);
        continue;
      } else if (argument.startsWith(PREV_HTML)) {
        map.put(PREV_HTML, value);
        continue;
      } else if (argument.startsWith(POST_HTML)) {
        map.put(POST_HTML, value);
        continue;
      } else if (argument.startsWith(FAST_PUBLISH_LINK)) {
        map.put(FAST_PUBLISH_LINK, value);
        continue;
      }
    }
    return map;
  }

  /**
   * Gets the title.
   *
   * @param node the node
   * @return the title
   * @throws Exception the exception
   */
  public static String getTitle(Node node) throws Exception {
    String title = null;
    try {
      title = node.getProperty("exo:title").getValue().getString();
    } catch (PathNotFoundException pnf1) {
      try {
        Value[] values = node.getNode("jcr:content").getProperty("dc:title").getValues();
        if (values.length != 0) {
          title = values[0].getString();
        }
      } catch (PathNotFoundException pnf2) {
        title = null;
      }
    } catch (IllegalStateException | RepositoryException e) {
      title = null;
    }
    if (StringUtils.isBlank(title)) {
      title = node.getName();
    }
    int index = node.getIndex();
    StringBuilder buffer = new StringBuilder(128);
    if (index > 1) {
      buffer.append(title);
      buffer.append('[');
      buffer.append(index);
      buffer.append(']');
      title = buffer.toString();
    }
    try {
      title = URLDecoder.decode(title, "UTF-8");
      return URLDecoder.decode(title, "UTF-8");
    }catch (Exception e){
      return title;
    }
  }

  /**
   * Gets the name.
   *
   * @param node the node
   * @return the name
   * @throws Exception the exception
   */
  public static String getName(Node node) throws Exception {
    String name = null;
    try {
      name = node.getProperty("exo:name").getValue().getString();
    } catch (PathNotFoundException pnf1) {
      try {
        Value[] values = node.getNode("jcr:content").getProperty("dc:name").getValues();
        if (values.length != 0) {
          name = values[0].getString();
        }
      } catch (PathNotFoundException pnf2) {
        name = null;
      }
    } catch (IllegalStateException | RepositoryException e) {
      name = null;
    }
    
    return URLDecoder.decode(name, "UTF-8");
  }

  /**
   * Get UIComponent to process render a node which has specified mimeType
   *
   * @param mimeType
   * @param container
   * @return
   * @throws Exception
   */
  public static UIComponent getUIComponent(String mimeType, UIContainer container) throws Exception {
    UIExtensionManager manager = WCMCoreUtils.getService(UIExtensionManager.class);
    List<UIExtension> extensions = manager.getUIExtensions(FILE_VIEWER_EXTENSION_TYPE);
    Map<String, Object> context = new HashMap<String, Object>();
    context.put(MIME_TYPE, mimeType.toLowerCase());
    for (UIExtension extension : extensions) {
      UIComponent uiComponent = manager.addUIExtension(extension, context, container);
      if (uiComponent != null)
        return uiComponent;
    }
    return null;
  }

  /*
   * Check the current node is eligible to add mix:versionable or not
   * @param node the current node
   * @param nodetypes The list of node types have child nodes which are not add
   * mix:versionaboe while enrolling.
   * @throws Exception the exception
   */
  public static boolean isMakeVersionable(Node node, String[] nodeTypes) throws Exception {
    String ws = node.getSession().getWorkspace().getName();
    SessionProvider sessionProvider = WCMCoreUtils.getSystemSessionProvider();
    ManageableRepository manageableRepository = WCMCoreUtils.getRepository();
    Session session = sessionProvider.getSession(ws, manageableRepository);
    node = (Node) session.getItem(node.getPath());
    int deep = node.getDepth();
    for (int i = 0; i < deep; i++) {
      Node parent = node.getParent();
      for (String nodeType : nodeTypes) {
        if (nodeType != null && nodeType.length() > 0 && parent.isNodeType(nodeType))
          return false;
      }
      node = parent;
    }
    return true;
  }

  /**
   * Get a cookie value with given name
   *
   * @param cookieName cookies
   * @return a cookies value
   */
  public static String getCookieByCookieName(String cookieName) {
    HttpServletRequest request = Util.getPortalRequestContext().getRequest();
    Cookie[] cookies = request.getCookies();
    if (cookies == null) {
      return null;
    }
    for (int loopIndex = 0; loopIndex < cookies.length; loopIndex++) {
      Cookie cookie1 = cookies[loopIndex];
      if (cookie1.getName().equals(cookieName))
        return cookie1.getValue();
    }
    return null;
  }

  /**
   * @param node nt:file node with have the data stream
   * @return Link to download the jcr:data of the given node
   * @throws Exception
   */
  public static String getDownloadRestServiceLink(Node node) throws Exception {
    ExoContainer container = ExoContainerContext.getCurrentContainer();
    PortalContainerInfo containerInfo = (PortalContainerInfo) container.getComponentInstanceOfType(PortalContainerInfo.class);
    String portalName = containerInfo.getContainerName();
    PortalContainerConfig portalContainerConfig = (PortalContainerConfig) container.getComponentInstance(PortalContainerConfig.class);
    String restContextName = portalContainerConfig.getRestContextName(portalName);
    StringBuilder sb = new StringBuilder();
    Node currentNode = org.exoplatform.wcm.webui.Utils.getRealNode(node);
    String ndPath = currentNode.getPath();
    if (ndPath.startsWith("/")) {
      ndPath = ndPath.substring(1);
    }
    String encodedPath = encodePath(ndPath,"UTF-8");
    sb.append("/").append(restContextName).append("/contents/download/");
    sb.append(currentNode.getSession().getWorkspace().getName()).append("/").append(encodedPath);
    if (node.isNodeType("nt:frozenNode")) {
      sb.append("?version=" + node.getParent().getName());
    }
    return sb.toString();
  }

  public static String getPDFViewerLink(Node node) throws Exception {
    ExoContainer container = ExoContainerContext.getCurrentContainer();
    PortalContainerInfo containerInfo = (PortalContainerInfo) container.getComponentInstanceOfType(PortalContainerInfo.class);
    String portalName = containerInfo.getContainerName();
    PortalContainerConfig portalContainerConfig = (PortalContainerConfig) container.getComponentInstance(PortalContainerConfig.class);
    String restContextName = portalContainerConfig.getRestContextName(portalName);
    StringBuilder sb = new StringBuilder();
    String repository = ((ManageableRepository) node.getSession().getRepository()).getConfiguration().getName();
    sb.append("/").append(restContextName).append("/pdfviewer/");
    sb.append(repository).append("/");
    sb.append(node.getSession().getWorkspace().getName()).append("/").append(node.getUUID());
    return sb.toString();
  }

  /**
   * Get allowed folder types in current path.
   *
   * @param currentNode
   * @param currentDrive
   * @return list of node types
   * @throws Exception
   */
  public static List<String> getAllowedFolderTypesInCurrentPath(Node currentNode, DriveData currentDrive) throws Exception {
    List<String> allowedTypes = new ArrayList<String>();
    NodeTypeImpl currentNodeType = (NodeTypeImpl) currentNode.getPrimaryNodeType();
    String[] arrFoldertypes = currentDrive.getAllowCreateFolders().split(",");
    NodeTypeManager ntManager = currentNode.getSession().getWorkspace().getNodeTypeManager();

    for (String strFolderType : arrFoldertypes) {
      if (strFolderType.isEmpty())
        continue;
      NodeType folderType = ntManager.getNodeType(strFolderType);
      if ((currentNodeType).isChildNodePrimaryTypeAllowed(Constants.JCR_ANY_NAME, ((NodeTypeImpl) folderType).getQName())) {
        allowedTypes.add(strFolderType);
      }
    }

    return allowedTypes;
  }

  /**
   * removes child nodes in path list if ancestor of the node exists in list
   *
   * @param srcPath the list of nodes
   * @return the node list with out child nodes
   */
  public static String[] removeChildNodes(String srcPath) {
    if (StringUtils.isEmpty(srcPath)) {
      return new String[] {};
    }
    if (!srcPath.contains(";")) {
      return new String[] { srcPath };
    }
    String[] paths = srcPath.split(";");
    List<String> ret = new ArrayList<String>();
    for (int i = 0; i < paths.length; i++) {
      boolean ok = true;
      for (int j = 0; j < paths.length; j++) {
        // check if [i] is child of [j]
        if ((i != j) && paths[i].startsWith(paths[j]) && (paths[i].length() > paths[j].length())
            && (paths[i].charAt(paths[j].length()) == '/')) {
          ok = false;
          break;
        }
      }
      if (ok) {
        ret.add(paths[i]);
      }
    }
    return ret.toArray(new String[] {});
  }

  public static String encodePath(String path, String encoding) {
    try {
      String encodedPath = URLEncoder.encode(path,encoding);
      encodedPath = encodedPath.replaceAll("%2F","/");
      return encodedPath;
    } catch (UnsupportedEncodingException e){
      LOG.error("Failed to encode path '" + path + "' with encoding '" + encoding + "'",e);
    }
    return null;
  }

  static public class NodeTypeNameComparator implements Comparator<NodeType> {
    public int compare(NodeType n1, NodeType n2) throws ClassCastException {
      String name1 = n1.getName();
      String name2 = n2.getName();
      return name1.compareToIgnoreCase(name2);
    }
  }

  /**
   * Generate the webdav mount  URL
   * @param nodePath : jcr path
   * @param ws :  workspace name
   * @return
   */
  public static String generateMountURL(String nodePath, String ws, String userPath, String groupPath) {
    if (StringUtils.isNotBlank(nodePath) && COLLABORATION_WS.equals(ws)) {
      StringBuilder mountPath = new StringBuilder();
      String[] ancestors = nodePath.substring(1).split("/");

      if (ancestors.length <= 1)
        return "/";
      //User folder mount
      if (nodePath.startsWith(userPath)) {
        if (ancestors.length >= USER_DEPTH + 1)
          mountPath.append(nodePath.substring(0, nodePath.indexOf(ancestors[USER_DEPTH + 1])-1));
        else
          mountPath.append(nodePath.substring(0, nodePath.indexOf(ancestors[ancestors.length-1])-1));
      }
      //Space folder mount
      else if (nodePath.startsWith(groupPath + SPACE_GROUP)) {
        if (ancestors.length > 4)
          mountPath.append(nodePath.substring(0, nodePath.indexOf(ancestors[4])-1));
        else if (ancestors.length == 4)
        {
          mountPath.append(nodePath.substring(0, nodePath.indexOf(ancestors[3])-1));
        }
        else
          mountPath.append(groupPath + SPACE_GROUP);
      }
      //Group folder mount
      else if (nodePath.startsWith(groupPath)) {
        if (ancestors.length > 2)
          mountPath.append(nodePath.substring(0, nodePath.indexOf(ancestors[2])-1));
        else
          mountPath.append(groupPath);
      }
      //Site folder mount
      else if (nodePath.startsWith(SITES_PATH)) {
        if (ancestors.length > 2)
          mountPath.append(nodePath.substring(0, nodePath.indexOf(ancestors[2])-1));
        else
          mountPath.append(SITES_PATH);
      }
      //other case mount level -1
      else{
        if (ancestors.length > 4){
          mountPath.append(nodePath.substring(0, nodePath.indexOf(ancestors[3])-1));
        }else if (ancestors.length > 1){
          mountPath.append(nodePath.substring(0, nodePath.indexOf(ancestors[ancestors.length-1])-1));
        }
      }
      return mountPath.toString();
    }

    return "/";
  }

  private static String checkMountPath(Node node, String mountPath) throws AccessControlException, RepositoryException {
    NodeImpl parent = (NodeImpl) node.getParent();
    boolean hasPermission = false;

    while (PermissionUtil.canRead(parent)) {
      hasPermission = true;
      if(mountPath.equals(parent.getPath()) || parent.isRoot())
        break;

      parent = parent.getParent();
    }
    if (hasPermission) {
      return parent.getPath();
    } else {
      LOG.warn("Cannot mount webdav path {} You don't have read permission access", parent.getPath());
      throw new AccessControlException("You don't have read permission access to " + parent.getPath());
    }
  }

  /**
   * Check the status of download action
   * @return
   */
  public static boolean isDownloadDocumentActivated() {
    SettingService settingService = CommonsUtils.getService(SettingService.class);
    SettingValue<?> settingValue = settingService.get(Context.GLOBAL.id("downloadDocumentStatus"),
                                                      Scope.APPLICATION.id("downloadDocumentStatus"),
                                                      "exo:downloadDocumentStatus");
    return !(settingValue != null && !settingValue.getValue().toString().isEmpty() ? Boolean.valueOf(settingValue.getValue().toString()) : false);
  }
}
