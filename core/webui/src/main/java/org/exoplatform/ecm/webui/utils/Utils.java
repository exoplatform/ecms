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

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Value;

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
import org.exoplatform.ecm.utils.permission.PermissionUtil;
import org.exoplatform.portal.webui.util.Util;
import org.exoplatform.services.cms.documents.TrashService;
import org.exoplatform.services.cms.link.LinkManager;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.jcr.ext.app.SessionProviderService;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.security.ConversationState;
import org.exoplatform.services.security.Identity;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;

/**
 * Created by The eXo Platform SARL Author : Dang Van Minh
 * minh.dang@exoplatform.com May 8, 2008 3:13:32 PM
 */
public class Utils {
  public static final String    WORKSPACE_NAME             = "workspace";

  public static final String    JCR_PATH                   = "path";

  public static final String    DRIVE_FOLDER               = "allowCreateFolder";

  public static final String    MIN_WIDTH                  = "minwidth";

  public static final String    CB_DOCUMENT_NAME           = "documentName";

  public static final String    CB_SCRIPT_NAME             = "scriptName";

  public static final String    CB_REF_DOCUMENT            = "reference";

  public static final String    CB_CHILD_DOCUMENT          = "child";

  public static final String    CB_NB_PER_PAGE             = "nbPerPage";

  public static final String    CB_QUERY_STATEMENT         = "queryStatement";

  public static final String    CB_QUERY_ISNEW             = "isAddNew";

  public static final String    CB_QUERY_TYPE              = "queryType";

  public static final String    CB_QUERY_STORE             = "queryStore";

  public static final String    CB_QUERY_LANGUAGE          = "queryLanguage";

  public static final String    CB_VIEW_TOOLBAR            = "viewToolbar";

  public static final String    CB_VIEW_TAGMAP             = "viewTagMap";

  public static final String    CB_VIEW_COMMENT            = "viewComment";

  public static final String    CB_VIEW_VOTE               = "viewVote";

  public static final String    CB_SEARCH_LOCATION         = "searchLocation";

  public static final String    CB_ENABLE_SEARCH_LOCATION  = "enableSearch";

  public static final String    CB_FILTER_CATEGORY         = "filterCategory";

  public static final String    EXO_AUDITABLE              = "exo:auditable";

  public static final String    CB_BOX_TEMPLATE            = "boxTemplate";

  public static final String    CB_TEMPLATE                = "template";

  public static final String    CB_USECASE                 = "usecase";

  public static final String    CB_ALLOW_PUBLISH           = "isAllowPublish";

  public static final String    FROM_PATH                  = "From Path";

  public static final String    USE_DOCUMENT               = "Document";

  public static final String    USE_JCR_QUERY              = "Using a JCR query";

  public static final String    USE_SCRIPT                 = "Using a script";

  public static final String    CB_USE_FROM_PATH           = "path";

  public static final String    CB_USE_DOCUMENT            = "detail-document";

  public static final String    CB_USE_JCR_QUERY           = "query";

  public static final String    CB_USE_SCRIPT              = "script";

  public static final String    SEMI_COLON                 = ";";

  public static final String    COLON                      = ":";

  public static final String    SLASH                      = "/";

  public static final String    BACKSLASH                  = "\\";

  public static final String    EXO_CREATED_DATE           = "exo:dateCreated";

  public static final String    EXO_DATETIME               = "exo:datetime";

  public static final String    EXO_MODIFIED_DATE          = "exo:dateModified";

  public static final String    EXO_OWNER                  = "exo:owner";

  public static final String    SPECIALCHARACTER[]         = { SEMI_COLON, SLASH, BACKSLASH, "|", ">", "<", "\"", "?", "!", "#",
    "$", "&", "*", "(", ")", "{", "}", "[", "]", ":", ".", "'" };

  public static final String    REPOSITORY                 = "repository";

  public static final String    VIEWS                      = "views";

  public static final String    DRIVE                      = "drive";

  public static final String    TRASH_HOME_NODE_PATH       = "trashHomeNodePath";

  public static final String    TRASH_REPOSITORY           = "trashRepository";

  public static final String    TRASH_WORKSPACE            = "trashWorkspace";

  public static final String    JCR_INFO                   = "jcrInfo";

  public static final String    NT_UNSTRUCTURED            = "nt:unstructured";

  public static final String    NT_FILE                    = "nt:file";

  public static final String    NT_FOLDER                  = "nt:folder";

  public static final String    NT_FROZEN                  = "nt:frozenNode";

  public static final String    EXO_TITLE                  = "exo:title";

  public static final String    EXO_SUMMARY                = "exo:summary";

  public static final String    EXO_RELATION               = "exo:relation";

  public static final String    EXO_TAXONOMY               = "exo:taxonomy";

  public static final String    EXO_IMAGE                  = "exo:image";

  public static final String    EXO_LANGUAGE               = "exo:language";

  public static final String    LANGUAGES                  = "languages";

  public static final String    EXO_METADATA               = "exo:metadata";

  public static final String    MIX_REFERENCEABLE          = "mix:referenceable";

  public static final String    MIX_VERSIONABLE            = "mix:versionable";

  public static final String    NT_RESOURCE                = "nt:resource";

  public static final String    NT_BASE                    = "nt:base";

  public static final String    DEFAULT                    = "default";

  public static final String    JCR_CONTENT                = "jcr:content";

  public static final String    JCR_CONTENT_DESCRIPTION    = "jcr:content/dc:description";

  public static final String    JCR_MIMETYPE               = "jcr:mimeType";

  public static final String    JCR_FROZEN                 = "jcr:frozenNode";

  public static final String    JCR_LASTMODIFIED           = "jcr:lastModified";

  public static final String    JCR_PRIMARYTYPE            = "jcr:primaryType";

  public static final String    JCR_DATA                   = "jcr:data";

  public static final String    JCR_SCORE                  = "jcr:score";

  public static final String    EXO_ROLES                  = "exo:roles";

  public static final String    EXO_TEMPLATEFILE           = "exo:templateFile";

  public static final String    EXO_TEMPLATE               = "exo:template";

  public static final String    EXO_ACTION                 = "exo:action";

  public static final String    EXO_ACTIONS                = "exo:actions";

  public static final String    MIX_LOCKABLE               = "mix:lockable";

  public static final String    EXO_CATEGORIZED            = "exo:categorized";

  public static final String    EXO_CATEGORY               = "exo:category";

  public static final String    EXO_HIDDENABLE             = "exo:hiddenable";

  public static final String    EXO_ACCESSPERMISSION       = "exo:accessPermissions";

  public static final String    EXO_PERMISSIONS            = "exo:permissions";

  public static final String    EXO_FAVOURITE              = "exo:favourite";

  public static final String    EXO_FAVOURITE_FOLDER       = "exo:favoriteFolder";

  public static final String    EXO_FAVOURITER             = "exo:favouriter";

  public static final String    EXO_RESTOREPATH            = "exo:restorePath";

  public static final String    EXO_RESTORELOCATION        = "exo:restoreLocation";

  public static final String    EXO_RESTORE_WORKSPACE      = "exo:restoreWorkspace";

  public static final String    EXO_LASTMODIFIER           = "exo:lastModifier";

  public static final String    EXO_TRASH_FOLDER           = "exo:trashFolder";

  public static final String    EXO_TOTAL                  = "exo:total";

  public static final String    EXO_WEBCONTENT             = "exo:webContent";

  public static final String    EXO_RSS_ENABLE             = "exo:rss-enable";

  public static final String    EXO_COMMENTS               = "exo:comments";

  public static final String    EXO_MUSICFOLDER            = "exo:musicFolder";

  public static final String    EXO_VIDEOFOLDER            = "exo:videoFolder";

  public static final String    EXO_PICTUREFOLDER          = "exo:pictureFolder";

  public static final String    EXO_DOCUMENTFOLDER         = "exo:documentFolder";

  public static final String    EXO_SEARCHFOLDER           = "exo:searchFolder";

  public static final String    MIX_COMMENTABLE            = "mix:commentable";

  public static final String    MIX_VOTABLE                = "mix:votable";

  public static final String    EXO_SYMLINK                = "exo:symlink";

  public static final String    EXO_PRIMARYTYPE            = "exo:primaryType";

  public static final String    INLINE_DRAFT               = "Draft";

  public static final String    INLINE_PUBLISHED           = "Published";

  public static final String    EXO_SORTABLE               = "exo:sortable";

  public static final String    EXO_RISIZEABLE             = "exo:documentSize";

  public static final String    FLASH_MIMETYPE             = "flash";

  public static final String[]  SPECIFIC_FOLDERS           = { EXO_MUSICFOLDER, EXO_VIDEOFOLDER, EXO_PICTUREFOLDER,
    EXO_DOCUMENTFOLDER, EXO_SEARCHFOLDER };

  public static final String[]  FOLDERS                    = { NT_UNSTRUCTURED, NT_FOLDER };

  public static final String[]  NON_EDITABLE_NODETYPES     = { NT_UNSTRUCTURED, NT_FOLDER, NT_RESOURCE };

  public static final String[]  CATEGORY_NODE_TYPES        = { NT_FOLDER, NT_UNSTRUCTURED, EXO_TAXONOMY };

  public static final String    CATEGORY_MANDATORY         = "categoryMandatoryWhenFileUpload";

  public static final String    UPLOAD_SIZE_LIMIT_MB       = "uploadFileSizeLimitMB";

  public static final String    FILE_VIEWER_EXTENSION_TYPE = "org.exoplatform.ecm.dms.FileViewer";

  public static final String    MIME_TYPE                  = "mimeType";

  public static final String    LOCALE_WEBUI_DMS           = "locale.portlet.i18n.WebUIDms";

  public static final String    REQUESTCONTEXT             = "requestcontext";

  public static final String    WORKSPACE_PARAM            = "workspaceName";

  public static final String    SPACE_GROUP                = "/spaces";

  public static final String    SITES_PATH                 = "/sites";

  public static final String    COLLABORATION_WS           = "collaboration";

  public static final int       USER_DEPTH                 = 5;

  public static final String    EMPTY                      = "";

  public static final String    PUBLIC                     = "Public";

  public static final String    GROUP                      = "Group";

  public static final String    SITE                       = "Site";

  public static final String    PRIVATE                    = "Private";

  public static final String    URL_BACKTO                 = "backto";

  public static final String    INPUT_TEXT_AREA            = "TEXTAREA";

  public static final String    INPUT_WYSIWYG              = "WYSIWYG";

  public static final String    INPUT_TEXT                 = "TEXT";

  public static final String    DEFAULT_CSS_NAME           = "InlineText";

  public static final String    LEFT2RIGHT                 = "left-to-right";

  public static final String    RIGHT2LEFT                 = "right-to-left";

  protected static final String SEPARATOR                  = "=";

  protected static final String TOOLBAR                    = "toolbar";

  protected static final String CSS                        = "CSSData";

  protected static final String HEIGHT                     = "height";

  protected static final String BUTTON_DIR                 = "button_direction";

  protected static final String PREV_HTML                  = "prev_html";

  protected static final String POST_HTML                  = "post_html";

  protected static final String FAST_PUBLISH_LINK          = "fast_publish";

  private static final Log      LOG                        = ExoLogger.getLogger(Utils.class.getName());

  public static String encodeHTML(String text) {
    return text.replace("&", "&amp;").replace("\"", "&quot;").replace("<", "&lt;").replace(">", "&gt;");
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
    List<Node> children = new ArrayList<>();
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

  public static String getRestContextName(String portalContainerName) {
    ExoContainer container = ExoContainerContext.getCurrentContainer();
    PortalContainerConfig portalContainerConfig =
                                                (PortalContainerConfig) container.getComponentInstance(PortalContainerConfig.class);
    return portalContainerConfig.getRestContextName(portalContainerName);
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
    } catch (Exception e) {
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
   * @param node nt:file node with have the data stream
   * @return Link to download the jcr:data of the given node
   * @throws Exception
   */
  public static String getDownloadRestServiceLink(Node node) throws Exception {
    ExoContainer container = ExoContainerContext.getCurrentContainer();
    PortalContainerInfo containerInfo = (PortalContainerInfo) container.getComponentInstanceOfType(PortalContainerInfo.class);
    String portalName = containerInfo.getContainerName();
    PortalContainerConfig portalContainerConfig =
                                                (PortalContainerConfig) container.getComponentInstance(PortalContainerConfig.class);
    String restContextName = portalContainerConfig.getRestContextName(portalName);
    StringBuilder sb = new StringBuilder();
    String ndPath = node.getPath();
    if (ndPath.startsWith("/")) {
      ndPath = ndPath.substring(1);
    }
    String encodedPath = encodePath(ndPath, "UTF-8");
    sb.append("/").append(restContextName).append("/contents/download/");
    sb.append(node.getSession().getWorkspace().getName()).append("/").append(encodedPath);
    if (node.isNodeType("nt:frozenNode")) {
      sb.append("?version=" + node.getParent().getName());
    }
    return sb.toString();
  }

  public static String getPDFViewerLink(Node node) throws Exception {
    ExoContainer container = ExoContainerContext.getCurrentContainer();
    PortalContainerInfo containerInfo = container.getComponentInstanceOfType(PortalContainerInfo.class);
    String portalName = containerInfo.getContainerName();
    PortalContainerConfig portalContainerConfig =
                                                (PortalContainerConfig) container.getComponentInstance(PortalContainerConfig.class);
    String restContextName = portalContainerConfig.getRestContextName(portalName);
    StringBuilder sb = new StringBuilder();
    String repository = ((ManageableRepository) node.getSession().getRepository()).getConfiguration().getName();
    sb.append("/").append(restContextName).append("/pdfviewer/");
    sb.append(repository).append("/");
    sb.append(node.getSession().getWorkspace().getName()).append("/").append(node.getUUID());
    return sb.toString();
  }

  public static String encodePath(String path, String encoding) {
    try {
      String encodedPath = URLEncoder.encode(path, encoding);
      encodedPath = encodedPath.replaceAll("%2F", "/");
      return encodedPath;
    } catch (UnsupportedEncodingException e) {
      LOG.error("Failed to encode path '" + path + "' with encoding '" + encoding + "'", e);
    }
    return null;
  }

  /**
   * Check the status of download action
   * 
   * @return
   */
  public static boolean isDownloadDocumentActivated() {
    SettingService settingService = CommonsUtils.getService(SettingService.class);
    SettingValue<?> settingValue = settingService.get(Context.GLOBAL.id("downloadDocumentStatus"),
                                                      Scope.APPLICATION.id("downloadDocumentStatus"),
                                                      "exo:downloadDocumentStatus");
    return !(settingValue != null
             && !settingValue.getValue().toString().isEmpty() ? Boolean.valueOf(settingValue.getValue().toString()) : false);
  }
}
