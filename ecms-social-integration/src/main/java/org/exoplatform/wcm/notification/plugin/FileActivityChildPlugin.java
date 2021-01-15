/*
 * Copyright (C) 2003-2013 eXo Platform SAS.
 *
 * This program is free software; you can redistribute it and/or
* modify it under the terms of the GNU Affero General Public License
* as published by the Free Software Foundation; either version 3
* of the License, or (at your option) any later version.
*
* This program is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
* GNU General Public License for more details.
*
* You should have received a copy of the GNU General Public License
* along with this program; if not, see<http://www.gnu.org/licenses/>.
 */
package org.exoplatform.wcm.notification.plugin;

import java.util.Map;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;

import org.exoplatform.commons.api.notification.NotificationContext;
import org.exoplatform.commons.api.notification.model.ArgumentLiteral;
import org.exoplatform.commons.api.notification.model.NotificationInfo;
import org.exoplatform.commons.api.notification.plugin.AbstractNotificationChildPlugin;
import org.exoplatform.commons.api.notification.service.template.TemplateContext;
import org.exoplatform.commons.notification.NotificationUtils;
import org.exoplatform.commons.notification.template.TemplateUtils;
import org.exoplatform.commons.utils.CommonsUtils;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.services.cms.documents.DocumentService;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.jcr.ext.app.SessionProviderService;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.wcm.core.NodeLocation;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;
import org.exoplatform.social.core.activity.model.ExoSocialActivity;
import org.exoplatform.social.core.identity.model.Identity;
import org.exoplatform.social.core.identity.provider.SpaceIdentityProvider;
import org.exoplatform.social.core.manager.ActivityManager;
import org.exoplatform.social.core.manager.IdentityManager;
import org.exoplatform.social.core.space.spi.SpaceService;
import org.exoplatform.wcm.ext.component.activity.FileUIActivity;
import org.exoplatform.wcm.ext.component.activity.listener.Utils;
import org.exoplatform.webui.cssfile.*;


public class FileActivityChildPlugin extends AbstractNotificationChildPlugin {
  private static final Log   LOG                          = ExoLogger.getLogger(FileActivityChildPlugin.class);
  public final static ArgumentLiteral<String> ACTIVITY_ID = new ArgumentLiteral<String>(String.class, "activityId");

  public final static String ACTIVITY_URL                 = "view_full_activity";
  public static final String ID                           = "files:spaces";
  public static final String MESSAGE                      = "MESSAGE";
  public static final String DOCPATH                      = "DOCPATH";
  public static final String WORKSPACE                    = "WORKSPACE";
  public static final String NODE_UUID                    = "id";
  public static final String AUTHOR                       = "author";
  public static final String MIME_TYPE                    = "mimeType";
  public static final String DOCUMENT_TITLE               = "docTitle";
  public static final String CONTENT_NAME                 = "contentName";
  public static final String DOCUMENT_SUMMARY             = "docSummary";
  public static final String EXO_RESOURCES_URI            = "/eXoSkin/skin/images/themes/default/Icons/TypeIcons/EmailNotificationIcons/";
  public static final String DOCNAME                      = "DOCNAME";
  public static final String ICON_FILE_EXTENSION          = ".png";

  private String[]             mimeType;
  private String[]             nodeUUID;
  private Node[]               contentNode;
  private NodeLocation[]       nodeLocation;
  private String[]             documentTitle;
  private ExoSocialActivity    activity;
  private String               baseURI;
  private String[]             docName;
  private String[]             contentLink;
  private int                  filesCount;

  public FileActivityChildPlugin(InitParams initParams) {
    super(initParams);
  }

  @Override
  public String makeContent(NotificationContext ctx) {
    try {
      ActivityManager activityM = CommonsUtils.getService(ActivityManager.class);

      NotificationInfo notification = ctx.getNotificationInfo();

      String language = getLanguage(notification);
      TemplateContext templateContext = new TemplateContext(ID, language);

      String activityId = notification.getValueOwnerParameter(ACTIVITY_ID.getKey());
      activity = activityM.getActivity(activityId);

      if (activity.isComment()) {
        activity = activityM.getParentActivity(activity);  
      }

      //

      Map<String, String> templateParams = activity.getTemplateParams();
      getAndSetFileInfo(templateParams,activity);

      //
      
      // File uploaded to Content Explorer hasn't MESSAGE field
      String message = templateParams.get(MESSAGE) != null ? NotificationUtils.processLinkTitle(templateParams.get(MESSAGE)) : "";

      templateContext.put("ACTIVITY_TITLE", message);

      boolean[] isVideo = new boolean[this.filesCount];
      String[] thumbnailURL = new String[this.filesCount];
      String[] summaries = new String[this.filesCount];
      String[] sizes = new String[this.filesCount];
      int[] versions = new int[this.filesCount];
      for (int i = 0; i < this.filesCount; i++) {
        isVideo[i] = this.mimeType[i].startsWith("video");
        thumbnailURL[i] = getDefaultThumbnail(i);
        Node currentNode = getContentNode(i);
        summaries[i] = Utils.getSummary(currentNode);
        sizes[i] = getSize(currentNode);
        versions[i] = getVersion(currentNode);
        if(this.contentLink != null && this.contentLink.length > i) {
          this.contentLink[i] = this.contentLink[i];
        }
      }

      templateContext.put("ACTIVITY_URL", this.contentLink);
      templateContext.put("DOCUMENT_TITLE", this.docName);
      templateContext.put("SUMMARY", summaries);
      templateContext.put("SIZE", sizes);
      templateContext.put("VERSION", versions);
      templateContext.put("IS_VIDEO", isVideo);
      templateContext.put("DEFAULT_THUMBNAIL_URL", thumbnailURL);
      templateContext.put("THUMBNAIL_URL", null);
      templateContext.put("COUNT", filesCount);

      String content = TemplateUtils.processGroovy(templateContext);
      return content;
    } catch (Exception e) {
      LOG.error("Failed at makeContent().", e);
      return (activity != null) ? activity.getTitle() : "";
    }
  }

  @Override
  public String getId() {
    return ID;
  }

  @Override
  public boolean isValid(NotificationContext ctx) {
    return false;
  }
  
  private void getAndSetFileInfo(Map<String, String> templateParams, ExoSocialActivity activity) throws Exception {
    DocumentService docService = CommonsUtils.getService(DocumentService.class);
    IdentityManager identityManager = CommonsUtils.getService(IdentityManager.class);
    Identity spaceIdentity = identityManager.getOrCreateIdentity(SpaceIdentityProvider.NAME, activity.getStreamOwner());
    String spaceId = CommonsUtils.getService(SpaceService.class).getSpaceByPrettyName(spaceIdentity.getRemoteId()).getGroupId();
    this.nodeUUID = getParameterValues(templateParams, NODE_UUID);
    this.filesCount = this.nodeUUID.length;
    this.mimeType = getParameterValues(templateParams, MIME_TYPE);
    this.docName = getTitlesFromPath(templateParams, DOCPATH);
    this.contentNode = new Node[this.filesCount];
    this.nodeLocation = new NodeLocation[this.filesCount];
    this.contentLink = new String[this.filesCount];
    String[] documentTitle = getParameterValues(templateParams, DOCUMENT_TITLE);
    if (documentTitle != null) {
      this.documentTitle = documentTitle;
    } else {
      this.documentTitle = getParameterValues(templateParams, CONTENT_NAME);
    }
    
    //get node data
    RepositoryService repositoryService = WCMCoreUtils.getService(RepositoryService.class);
    ManageableRepository manageRepo = null;
    try {
      manageRepo = repositoryService.getCurrentRepository();
      SessionProvider sessionProvider = SessionProviderService.getSystemSessionProvider();
      for (String ws : manageRepo.getWorkspaceNames()) {
        for (int i = 0; i < this.filesCount; i++) {
          try {
            this.contentNode[i] = sessionProvider.getSession(ws, manageRepo).getNodeByUUID(this.nodeUUID[i]);
            this.nodeLocation[i] = NodeLocation.getNodeLocationByNode(contentNode[i]);
            this.contentLink[i] = docService.getDocumentUrlInSpaceDocuments(this.contentNode[i],spaceId);
          } catch (RepositoryException e) {
            continue;
          }
        }
      }
    } catch (RepositoryException re) {
      LOG.error("Can not get the repository. ", re);
    }

    //
    this.baseURI = CommonsUtils.getCurrentDomain();
  }

  private String getDefaultThumbnail(int i) {
    String cssClass = CssClassUtils.getCSSClassByFileNameAndFileType(
        (this.docName == null ? (this.documentTitle == null ? null : this.documentTitle[i]) : this.docName[i]), (this.mimeType == null ? null : this.mimeType[i]), CssClassManager.ICON_SIZE.ICON_64);
    
    if (cssClass.indexOf(CssClassIconFile.DEFAULT_CSS) > 0) {
      return baseURI + EXO_RESOURCES_URI  + "uiIcon64x64Templatent_file.png";
    }
    return baseURI + EXO_RESOURCES_URI + cssClass.split(" ")[0] + ICON_FILE_EXTENSION;
  }

  private Node getContentNode(int i) {
    return NodeLocation.getNodeByLocation(nodeLocation[i]);
  }

  private String getSize(Node node) {
    double size = getFileSize(node);   
    try {
      if (node.hasNode(org.exoplatform.ecm.webui.utils.Utils.JCR_CONTENT)) {
        return FileUtils.byteCountToDisplaySize((long)size);
      }
    } catch (Exception e) {
      return StringUtils.EMPTY;
    }
    return StringUtils.EMPTY;    
  }

  private double getFileSize(Node node) {
    double fileSize = 0;    
    try {
      if (node.hasNode(org.exoplatform.ecm.webui.utils.Utils.JCR_CONTENT)) {
        Node contentNode = node.getNode(org.exoplatform.ecm.webui.utils.Utils.JCR_CONTENT);
        if (contentNode.hasProperty(org.exoplatform.ecm.webui.utils.Utils.JCR_DATA)) {
          fileSize = contentNode.getProperty(org.exoplatform.ecm.webui.utils.Utils.JCR_DATA).getLength();
        }
      }
    } catch(Exception ex) { fileSize = 0; }
    return fileSize;    
  }

  private int getVersion(Node node) {
    String currentVersion = null;
    try {
      currentVersion = node.getBaseVersion().getName();
      if (currentVersion.contains("jcr:rootVersion")) currentVersion = "0";
    }catch (Exception e) {
      currentVersion ="0";
    }
    return Integer.parseInt(currentVersion);
  }

  private String[] getParameterValues(Map<String, String> activityParams, String paramName) {
    String[] values = null;
    String value = activityParams.get(paramName);
    if(value != null) {
      values = value.split(FileUIActivity.SEPARATOR_REGEX);
    }
    if (LOG.isDebugEnabled()) {
      if(this.filesCount != 0 && (values == null || values.length != this.filesCount)) {
          LOG.debug("Parameter '{}' hasn't same length as other activity parmameters", paramName);
      }
    }
    return values;
  }
  private String[] getTitlesFromPath(Map<String, String> activityParams, String paramName) {
    String[] values = null;
    String value = activityParams.get(paramName);
    if(value != null) {
      values = value.split(FileUIActivity.SEPARATOR_REGEX);

      for(int i =0 ; i<values.length; i++){
        String str = values[i];
        values[i] = str.substring(str.lastIndexOf('/')+1);
      }

    }
    if (LOG.isDebugEnabled()) {
      if(this.filesCount != 0 && (values == null || values.length != this.filesCount)) {
        LOG.debug("Parameter '{}' hasn't same length as other activity parameters", paramName);
      }
    }
    return values;
  }

}
