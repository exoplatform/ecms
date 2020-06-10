/*
 * Copyright (C) 2003-2010 eXo Platform SAS.
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
package org.exoplatform.social.plugin.doc;

import java.io.InputStream;

import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.ValueFormatException;
import javax.portlet.PortletRequest;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;

import org.exoplatform.commons.utils.CommonsUtils;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.ecm.jcr.model.VersionNode;
import org.exoplatform.ecm.webui.utils.Utils;
import org.exoplatform.services.cms.documents.DocumentService;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.security.ConversationState;
import org.exoplatform.services.wcm.core.NodeLocation;
import org.exoplatform.services.wcm.core.NodetypeConstant;
import org.exoplatform.services.wcm.core.WebSchemaConfigService;
import org.exoplatform.services.wcm.friendly.FriendlyService;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;
import org.exoplatform.services.wcm.webcontent.WebContentSchemaHandler;
import org.exoplatform.social.webui.activity.BaseUIActivity;
import org.exoplatform.social.webui.activity.UIActivitiesContainer;
import org.exoplatform.social.webui.composer.PopupContainer;
import org.exoplatform.web.application.ApplicationMessage;
import org.exoplatform.webui.application.WebuiRequestContext;
import org.exoplatform.webui.application.portlet.PortletRequestContext;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIApplication;
import org.exoplatform.webui.core.lifecycle.UIFormLifecycle;
import org.exoplatform.webui.cssfile.CssClassManager;
import org.exoplatform.webui.cssfile.CssClassUtils;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.EventListener;

/**
 * Created by The eXo Platform SAS
 * Author : Zun
 *          exo@exoplatform.com
 * Jul 23, 2010  
 */

@ComponentConfig(
   lifecycle = UIFormLifecycle.class,
   template = "war:/groovy/social/plugin/doc/UIDocActivity.gtmpl",
   events = {
     @EventConfig(listeners = UIDocActivity.DownloadDocumentActionListener.class),
     @EventConfig(listeners = UIDocActivity.ViewDocumentActionListener.class),
     @EventConfig(listeners = BaseUIActivity.LoadLikesActionListener.class),
     @EventConfig(listeners = BaseUIActivity.ToggleDisplayCommentFormActionListener.class),
     @EventConfig(listeners = BaseUIActivity.LikeActivityActionListener.class),
     @EventConfig(listeners = BaseUIActivity.SetCommentListStatusActionListener.class),
     @EventConfig(listeners = BaseUIActivity.PostCommentActionListener.class),
     @EventConfig(listeners = BaseUIActivity.DeleteActivityActionListener.class),
     @EventConfig(listeners = BaseUIActivity.DeleteCommentActionListener.class),
     @EventConfig(listeners = BaseUIActivity.LikeCommentActionListener.class)
   }
 )
public class UIDocActivity extends BaseUIActivity {
  
  private static final Log LOG = ExoLogger.getLogger(UIDocActivity.class);
  private static final String IMAGE_PREFIX = "image/";
  private static final String DOCUMENT_POSTFIX = "/pdf";
  
  public static final String DOCLINK = "DOCLINK";
  public static final String MESSAGE = "MESSAGE";
  public static final String REPOSITORY = "REPOSITORY";
  public static final String WORKSPACE = "WORKSPACE";
  public static final String DOCNAME = "DOCNAME";
  public static final String ID = "id";
  public static final String DOCPATH = "DOCPATH";
  
  public static final String CONTENT_NAME       = "contentName";
  public static final String CONTENT_LINK       = "contenLink";
  public static final String IMAGE_PATH         = "imagePath";
  public static final String MIME_TYPE          = "mimeType";
  public static final String STATE              = "state";
  public static final String AUTHOR             = "author";
  public static final String DATE_CREATED       = "dateCreated";
  public static final String LAST_MODIFIED      = "lastModified";
  public static final String DOCUMENT_TYPE_LABEL= "docTypeLabel";  
  public static final String DOCUMENT_TITLE     = "docTitle";  
  public static final String DOCUMENT_VERSION   = "docVersion";  
  public static final String DOCUMENT_SUMMARY   = "docSummary";
  public static final String IS_SYMLINK         = "isSymlink";
  public static final String REPOSITORY_NAME = "repository";
  public static final String WORKSPACE_NAME = "collaboration";
  
  public String docLink;
  public String message;
  public String docName;
  public String docPath;
  public String repository;
  public String workspace;

  private DocumentService documentService;

  public UIDocActivity() {
    documentService = CommonsUtils.getService(DocumentService.class);
  }

  protected boolean isPreviewable() {
    return getMimeType().endsWith(DOCUMENT_POSTFIX);    
  }
  
  protected boolean isImageFile() {
    return getMimeType().startsWith(IMAGE_PREFIX);
  }
  
  protected String getDocThumbnail(){    
    String portalContainerName = PortalContainer.getCurrentPortalContainerName();
    String restContextName = PortalContainer.getRestContextName(portalContainerName);
    return new StringBuffer().append("/").append(portalContainerName).
                               append("/").append(restContextName).
                               append("/thumbnailImage/big").
                               append("/").append(REPOSITORY_NAME).
                               append("/").append(WORKSPACE_NAME).
                               append(docPath).toString();
  }
  
  protected String getSize() {
    double size = 0;
    Node docNode = getDocNode();
    try {
      if (docNode.hasNode(Utils.JCR_CONTENT)) {
        Node contentNode = docNode.getNode(Utils.JCR_CONTENT);
        if (contentNode.hasProperty(Utils.JCR_DATA)) {
          size = contentNode.getProperty(Utils.JCR_DATA).getLength();
        }
        
        return FileUtils.byteCountToDisplaySize((long)size);
      }
    } catch (PathNotFoundException e) {
      return StringUtils.EMPTY;
    } catch (ValueFormatException e) {
      return StringUtils.EMPTY;
    } catch (RepositoryException e) {
      return StringUtils.EMPTY;
    }
    return StringUtils.EMPTY;
  }
  
  protected int getVersion() {
    try {
      VersionNode rootVersion_ = new VersionNode(NodeLocation.getNodeByLocation(new NodeLocation(repository, workspace, docPath))
                                                 .getVersionHistory().getRootVersion(), getDocNode().getSession());
      if (rootVersion_ != null) {
        return rootVersion_.getChildren().size();
      }
    } catch (Exception e) {
        if(LOG.isDebugEnabled()) {
          LOG.debug("cannot version node", e);
        }
    }
    return 0;
  }

  protected String getCssClassIconFile(String fileName, String fileType) {
    String cssClass = CssClassUtils.getCSSClassByFileNameAndFileType(fileName, fileType, CssClassManager.ICON_SIZE.ICON_64).replace("uiIcon", "uiBgd");
    return cssClass;
  }

  protected boolean isDisplayThumbnail(String mimeType) {
    if( mimeType.startsWith("application/pdf") || 
        mimeType.startsWith("application/msword") || 
        mimeType.startsWith("application/vnd.oasis.opendocument.text") || 
        mimeType.startsWith("application/vnd.openxmlformats-officedocument.wordprocessingml.document") || 
        mimeType.startsWith("application/rtf") ){
      return true;
    }
    return false;
  }

  public String getDocOpenUri() {
    String uri = "";

    if(docPath != null) {
      try {
        uri = documentService.getLinkInDocumentsApp(docPath);
      } catch(Exception e) {
        LOG.error("Cannot get document open URI of node " + docPath + " : " + e.getMessage(), e);
        uri = "";
      }
    }

    return uri;
  }

  private boolean hasPermissionViewFile() {
    return (getDocNode() != null);
  }
  
  public static class ViewDocumentActionListener extends EventListener<UIDocActivity> {
    @Override
    public void execute(Event<UIDocActivity> event) throws Exception {
      final UIDocActivity docActivity = event.getSource();
      if (! docActivity.hasPermissionViewFile()) {
        WebuiRequestContext ctx = event.getRequestContext();
        UIApplication uiApplication = ctx.getUIApplication();
        uiApplication.addMessage(new ApplicationMessage("UIDocActivity.msg.noPermission", null, ApplicationMessage.WARNING));
        return;
      }
      final UIActivitiesContainer activitiesContainer = docActivity.getAncestorOfType(UIActivitiesContainer.class);
      final PopupContainer popupContainer = activitiesContainer.getPopupContainer();

      if (docActivity.getChild(UIDocViewer.class) != null) {
        docActivity.removeChild(UIDocViewer.class);
      }
      
      UIDocViewer docViewer = popupContainer.createUIComponent(UIDocViewer.class, null, "DocViewer");
      docViewer.docPath = docActivity.docPath;
      docViewer.repository = docActivity.repository;
      docViewer.workspace = docActivity.workspace;

      popupContainer.activate(docViewer, 800, 600, true);
      event.getRequestContext().addUIComponentToUpdateByAjax(popupContainer);
    }
  }
  
  public static class DownloadDocumentActionListener extends EventListener<UIDocActivity> {
    @Override
    public void execute(Event<UIDocActivity> event) throws Exception {
      UIDocActivity uiComp = event.getSource() ;
      if (! uiComp.hasPermissionViewFile()) {
        WebuiRequestContext ctx = event.getRequestContext();
        UIApplication uiApplication = ctx.getUIApplication();
        uiApplication.addMessage(new ApplicationMessage("UIDocActivity.msg.noPermission", null, ApplicationMessage.WARNING));
        return;
      }
      String downloadLink = null;
      if (getRealNode(uiComp.getDocNode()).getPrimaryNodeType().getName().equals(Utils.NT_FILE)) {
        downloadLink = Utils.getDownloadRestServiceLink(uiComp.getDocNode());
      }
      event.getRequestContext().getJavascriptManager().addJavascript("ajaxRedirect('" + downloadLink + "');");
    }
    
    private Node getRealNode(Node node) throws Exception {
      // TODO: Need to add to check symlink node
      if (node.isNodeType("nt:frozenNode")) {
        String uuid = node.getProperty("jcr:frozenUuid").getString();
        return node.getSession().getNodeByUUID(uuid);
      }
      return node;
    }
  }
  
  protected Node getDocNode() {
    NodeLocation nodeLocation = new NodeLocation(repository, workspace, docPath);
    return NodeLocation.getNodeByLocation(nodeLocation);
  }
  
  /**
   * Gets the webdav url.
   * 
   * @return the webdav url
   * @throws Exception the exception
   */
  public String getWebdavURL() throws Exception {
    Node contentNode = getDocNode();
    NodeLocation nodeLocation = new NodeLocation(repository, workspace, docPath);
    PortletRequestContext portletRequestContext = WebuiRequestContext.getCurrentInstance();
    PortletRequest portletRequest = portletRequestContext.getRequest();
    String repository = nodeLocation.getRepository();
    String workspace = nodeLocation.getWorkspace();
    String baseURI = portletRequest.getScheme() + "://" + portletRequest.getServerName() + ":"
        + String.format("%s", portletRequest.getServerPort());

    FriendlyService friendlyService = WCMCoreUtils.getService(FriendlyService.class);
    String link = "#";

    String portalName = PortalContainer.getCurrentPortalContainerName();
    String restContextName = PortalContainer.getCurrentRestContextName();
    if (contentNode.isNodeType("nt:frozenNode")) {
      String uuid = contentNode.getProperty("jcr:frozenUuid").getString();
      Node originalNode = contentNode.getSession().getNodeByUUID(uuid);
      link = baseURI + "/" + portalName + "/" + restContextName + "/jcr/" + repository + "/"
          + workspace + originalNode.getPath() + "?version=" + contentNode.getParent().getName();
    } else {
      link = baseURI + "/" + portalName + "/" + restContextName + "/jcr/" + repository + "/"
          + workspace + contentNode.getPath();
    }

    return friendlyService.getFriendlyUri(link);
  }
  
  /**
   * Gets the summary.
   * 
   * @return the summary of Node. Return empty string if catch an exception.
   */
  public String getSummary() {
    String desc = "";
    Node node = getDocNode();
    try {
      if (node != null) {
        if (node.hasProperty("exo:summary")) {
          desc = node.getProperty("exo:summary").getValue().getString();
        } else if (node.hasNode("jcr:content")) {
          Node content = node.getNode("jcr:content");
          if (content.hasProperty("dc:description") && content.getProperty("dc:description").getValues().length > 0) {
            desc = content.getProperty("dc:description").getValues()[0].getString();
          }
        }
      }
    } catch (RepositoryException re) {
      if (LOG.isWarnEnabled())
        LOG.warn("RepositoryException: ", re);
    }

    return desc;
  }
  
  public String getTitle() throws Exception {
    return Utils.getTitle(getDocNode());
  }
  
  /**
   * get activity owner
   * 
   * @return activity owner
   */
  public static String getActivityOwnerId(Node node) {
    String activityOwnerId = "";
    ConversationState conversationState = ConversationState.getCurrent();
    if (conversationState != null) {
      activityOwnerId = conversationState.getIdentity().getUserId();
    }else{
      try {
        activityOwnerId = node.getProperty("publication:lastUser").getString();
      } catch (Exception e) {
        LOG.info("No lastUser publication");
      } 
    }
    return activityOwnerId;
  }
  
  /**
   * Gets the illustrative image.
   * 
   * @param node the node
   * @return the illustrative image
   */
  public static String getIllustrativeImage(Node node) {
    WebSchemaConfigService schemaConfigService = WCMCoreUtils.getService(WebSchemaConfigService.class);
    WebContentSchemaHandler contentSchemaHandler = schemaConfigService.getWebSchemaHandlerByType(WebContentSchemaHandler.class);
    Node illustrativeImage = null;
    String uri = "";
    try {
      illustrativeImage = contentSchemaHandler.getIllustrationImage(node);
      uri = generateThumbnailImageURI(illustrativeImage);
    } catch (PathNotFoundException ex) {
      return uri;
    } catch (Exception e) { // WebContentSchemaHandler
      LOG.warn(e.getMessage(), e);
    }
    return uri;
  }
  
  /**
   * Generate the Thumbnail Image URI.
   * 
   * @param file the node
   * @return the Thumbnail uri with medium size
   * @throws Exception the exception
   */
  public static String generateThumbnailImageURI(Node file) throws Exception {
    StringBuilder builder = new StringBuilder();
    NodeLocation fielLocation = NodeLocation.getNodeLocationByNode(file);
    String repository = fielLocation.getRepository();
    String workspaceName = fielLocation.getWorkspace();
    String nodeIdentifiler = file.getPath().replaceFirst("/", "");
    String portalName = PortalContainer.getCurrentPortalContainerName();
    String restContextName = PortalContainer.getCurrentRestContextName();
    InputStream stream = file.getNode(NodetypeConstant.JCR_CONTENT)
                             .getProperty(NodetypeConstant.JCR_DATA)
                             .getStream();
    if (stream.available() == 0)
      return null;
    stream.close();
    builder.append("/")
           .append(portalName)
           .append("/")
           .append(restContextName)
           .append("/")
           .append("thumbnailImage/medium/")
           .append(repository)
           .append("/")
           .append(workspaceName)
           .append("/")
           .append(nodeIdentifiler);
    return builder.toString();
  }
  
  /**
   * Generate the viewer link to site explorer by node
   * 
   * @param node the node
   * @return String the viewer link
   * @throws RepositoryException
   */
  public static String getContentLink(Node node) throws Exception {
    DocumentService documentService = CommonsUtils.getService(DocumentService.class);
    return documentService.getLinkInDocumentsApp(node.getPath());
  }
  
  /**
   * Get the MimeType
   * 
   * @param node the node
   * @return the MimeType
   */
  public static String getMimeType(Node node) {
    try {
      if (node.getPrimaryNodeType().getName().equals(NodetypeConstant.NT_FILE)) {
        if (node.hasNode(NodetypeConstant.JCR_CONTENT))
          return node.getNode(NodetypeConstant.JCR_CONTENT)
                     .getProperty(NodetypeConstant.JCR_MIME_TYPE)
                     .getString();
      }
    } catch (RepositoryException e) {
      LOG.error(e.getMessage(), e);
    }
    return "";
  }

  
  private String getMimeType() {
    String mimeType = "";    
      try {
        mimeType = getDocNode().getNode("jcr:content").getProperty("jcr:mimeType").getString();
      } catch (ValueFormatException e) {
        if (LOG.isDebugEnabled())
          LOG.debug(e);
        return StringUtils.EMPTY;
      } catch (PathNotFoundException e) {
        if (LOG.isDebugEnabled())
          LOG.debug(e);
        return StringUtils.EMPTY;
      } catch (RepositoryException e) {
        if (LOG.isDebugEnabled())
          LOG.debug(e);
        return StringUtils.EMPTY;
      }
    return mimeType;
  }
}
