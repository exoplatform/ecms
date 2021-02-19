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
package org.exoplatform.wcm.ext.component.activity;

import java.net.URLDecoder;
import java.text.*;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.*;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import javax.jcr.*;
import javax.portlet.PortletRequest;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.StringEscapeUtils;

import com.ibm.icu.util.Calendar;

import org.exoplatform.commons.utils.ISO8601;
import org.exoplatform.container.*;
import org.exoplatform.container.xml.PortalContainerInfo;
import org.exoplatform.download.DownloadResource;
import org.exoplatform.download.DownloadService;
import org.exoplatform.ecm.connector.dlp.FileDlpConnector;
import org.exoplatform.ecm.webui.utils.PermissionUtil;
import org.exoplatform.ecm.webui.utils.Utils;
import org.exoplatform.portal.webui.util.Util;
import org.exoplatform.services.cms.BasePath;
import org.exoplatform.services.cms.clouddrives.CloudDrive;
import org.exoplatform.services.cms.clouddrives.CloudDriveService;
import org.exoplatform.services.cms.clouddrives.CloudFile;
import org.exoplatform.services.cms.clouddrives.DriveRemovedException;
import org.exoplatform.services.cms.documents.*;
import org.exoplatform.services.cms.drives.DriveData;
import org.exoplatform.services.cms.drives.impl.ManageDriveServiceImpl;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.access.PermissionType;
import org.exoplatform.services.jcr.core.ExtendedNode;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.jcr.ext.app.SessionProviderService;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.jcr.ext.hierarchy.NodeHierarchyCreator;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.organization.*;
import org.exoplatform.services.security.ConversationState;
import org.exoplatform.services.security.Identity;
import org.exoplatform.services.security.MembershipEntry;
import org.exoplatform.services.wcm.core.NodeLocation;
import org.exoplatform.services.wcm.core.NodetypeConstant;
import org.exoplatform.services.wcm.friendly.FriendlyService;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;
import org.exoplatform.social.core.activity.model.ExoSocialActivity;
import org.exoplatform.social.core.identity.provider.OrganizationIdentityProvider;
import org.exoplatform.social.core.manager.IdentityManager;
import org.exoplatform.social.core.space.SpaceUtils;
import org.exoplatform.social.core.space.model.Space;
import org.exoplatform.social.core.space.spi.SpaceService;
import org.exoplatform.social.core.storage.SpaceStorageException;
import org.exoplatform.social.plugin.doc.UIDocActivity;
import org.exoplatform.social.webui.activity.BaseUIActivity;
import org.exoplatform.social.webui.activity.UIActivitiesContainer;
import org.exoplatform.social.webui.activity.UILinkUtil;
import org.exoplatform.social.webui.composer.PopupContainer;
import org.exoplatform.web.CacheUserProfileFilter;
import org.exoplatform.web.application.JavascriptManager;
import org.exoplatform.web.application.RequireJS;
import org.exoplatform.webui.application.WebuiRequestContext;
import org.exoplatform.webui.application.portlet.PortletRequestContext;
import org.exoplatform.webui.config.annotation.*;
import org.exoplatform.webui.core.UIPopupContainer;
import org.exoplatform.webui.core.lifecycle.UIFormLifecycle;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.EventListener;
import org.exoplatform.webui.ext.UIExtension;
import org.exoplatform.webui.ext.UIExtensionManager;
import org.exoplatform.ws.frameworks.json.impl.JsonException;
import org.exoplatform.ws.frameworks.json.impl.JsonGeneratorImpl;


/**
 * Created by The eXo Platform SAS Author : eXoPlatform exo@exoplatform.com Mar
 * 15, 2011
 */
@ComponentConfigs({
        @ComponentConfig(lifecycle = UIFormLifecycle.class,
                template = "war:/groovy/ecm/social-integration/plugin/space/FileUIActivity.gtmpl", events = {
                @EventConfig(listeners = FileUIActivity.ViewDocumentActionListener.class),
                @EventConfig(listeners = BaseUIActivity.LoadLikesActionListener.class),
                @EventConfig(listeners = BaseUIActivity.ToggleDisplayCommentFormActionListener.class),
                @EventConfig(listeners = BaseUIActivity.LikeActivityActionListener.class),
                @EventConfig(listeners = BaseUIActivity.SetCommentListStatusActionListener.class),
                @EventConfig(listeners = BaseUIActivity.PostCommentActionListener.class),
                @EventConfig(listeners = BaseUIActivity.DeleteActivityActionListener.class),
                @EventConfig(listeners = FileUIActivity.OpenFileActionListener.class),
                @EventConfig(listeners = BaseUIActivity.DeleteCommentActionListener.class),
                @EventConfig(listeners = BaseUIActivity.LikeCommentActionListener.class),
                @EventConfig(listeners = BaseUIActivity.EditActivityActionListener.class),
                @EventConfig(listeners = BaseUIActivity.EditCommentActionListener.class),
                @EventConfig(listeners = FileUIActivity.DownloadDocumentActionListener.class)
        }),
})
public class FileUIActivity extends BaseUIActivity{

  public static final String[] EMPTY_ARRAY = new String[0];

  public static final String SEPARATOR_REGEX      = "\\|@\\|";

  private static final String NEW_DATE_FORMAT     = "hh:mm:ss MMM d, yyyy";
  
  private static final Log    LOG                 = ExoLogger.getLogger(FileUIActivity.class);

  public static final String  ID                  = "id";

  public static final String  CONTENT_LINK        = "contenLink";

  public static final String  MESSAGE             = "message";

  public static final String  ACTIVITY_STATUS     = "MESSAGE";

  public static final String  IMAGE_PATH          = "imagePath";

  public static final String  MIME_TYPE           = "mimeType";

  public static final String  STATE               = "state";

  public static final String  AUTHOR              = "author";

  public static final String  DATE_CREATED        = "dateCreated";

  public static final String  LAST_MODIFIED       = "lastModified";

  public static final String  DOCUMENT_TYPE_LABEL = "docTypeLabel";

  public static final String  DOCUMENT_TITLE      = "docTitle";

  public static final String  DOCUMENT_VERSION    = "docVersion";

  public static final String  DOCUMENT_SUMMARY    = "docSummary";

  public static final String  IS_SYSTEM_COMMENT   = "isSystemComment";

  public static final String  SYSTEM_COMMENT      = "systemComment";

  public static final String  ONE_DRIVE_PROVIDER_ID    = "onedrive";

  public static final String  GOOGLE_DRIVE_PROVIDER_ID = "gdrive";
  
  public static final String  ONE_DRIVE_ICON           = "uiIcon-onedrive";

  public static final String  GOOGLE_DRIVE_ICON        = "uiIcon-gdrive";

  private String              message;

  private LinkedHashMap<String, String>[] folderPathWithLinks;

  private String              activityStatus;

  public int                  filesCount          = 0;

  private String              activityTitle;

  private DateTimeFormatter   dateTimeFormatter;

  private DocumentService     documentService;

  private SpaceService        spaceService;

  private OrganizationService organizationService;

  private TrashService         trashService;

  /** The cloud drives. */
  private CloudDriveService   cloudDrivesService;

  private List<ActivityFileAttachment> activityFileAttachments = new ArrayList<>();

  private String                          downloadLink            = null;

  private String                          downloadResourceId      = null;

  private String                          linkSource              = "";

  private String                          linkTitle               = "";

  private String                          linkImage               = "";

  private String                          linkDescription         = "";

  private String                          embedHtml               = "";

  public FileUIActivity() throws Exception {
    super();
    if(WebuiRequestContext.getCurrentInstance() != null) {
      addChild(UIPopupContainer.class, null, "UIDocViewerPopupContainer");
    }
  }

  @Override
  protected void editActivity(String message) {
    super.editActivity(message);
    this.setMessage(message);
    this.setActivityTitle(message.replace("</br></br>", ""));
  }

  public String getActivityTitle() {
    return activityTitle;
  }

  public void setActivityTitle(String activityTitle) {
    this.activityTitle = activityTitle;
  }

  public String getContentLink(int i) {
    if ((i + 1) > activityFileAttachments.size()) {
      return null;
    }
    return activityFileAttachments.get(i).getContentLink();
  }

  public void setContentLink(int i,String contentLink) {
    if ((i + 1) > activityFileAttachments.size()) {
      return;
    }
    ActivityFileAttachment activityFileAttachment = activityFileAttachments.get(i);
    activityFileAttachment.setContentLink(contentLink);
  }

  public String getMessage() {
    return message;
  }

  public void setMessage(String message) {
    this.message = message;
  }

  public String getContentName(int i) {
    if ((i + 1) > activityFileAttachments.size()) {
      return null;
    }
    ActivityFileAttachment activityFileAttachment = activityFileAttachments.get(i);
    // Retrieve name from JCR Node instead of activity parameter
    // To get real file name instead
    if (activityFileAttachment == null
            || activityFileAttachment.getContentNode() == null) {
      return null;
    }
    String activityContentName = activityFileAttachment.getContentName();
    Node contentNode = activityFileAttachment.getContentNode();
    try {
      return getContentName(contentNode, activityContentName);
    } catch (Exception e) {
      LOG.debug("Can't retrieve file name of attachment with path " + activityFileAttachment.getDocPath(), e);
      return null;
    }
  }

  public String getContentName(Node contentNode, String activityContentName) throws Exception{
    activityContentName = activityContentName == null ? contentNode.getName() : URLDecoder.decode(activityContentName, "UTF-8");
    String contentName = contentNode.hasProperty("exo:title") ?
            URLDecoder.decode(contentNode.getProperty("exo:title").getString(), "UTF-8")
            : activityContentName;
    if (StringUtils.isBlank(contentName)) {
      contentName = URLDecoder.decode(activityContentName, "UTF-8");
    }
    return URLDecoder.decode(contentName, "UTF-8");
  }

  public void setContentName(String contentName, int i) {
    if ((i + 1) > activityFileAttachments.size()) {
      return;
    }
    ActivityFileAttachment activityFileAttachment = activityFileAttachments.get(i);
    activityFileAttachment.setContentName(contentName);
  }

  public String getImagePath(int i) {
    if ((i + 1) > activityFileAttachments.size()) {
      return null;
    }
    ActivityFileAttachment activityFileAttachment = activityFileAttachments.get(i);
    return activityFileAttachment.getImagePath();
  }

  public void setImagePath(String imagePath, int i) {
    if ((i + 1) > activityFileAttachments.size()) {
      return;
    }
    ActivityFileAttachment activityFileAttachment = activityFileAttachments.get(i);
    activityFileAttachment.setImagePath(imagePath);
  }

  public String getMimeType(int i) {
    if ((i + 1) > activityFileAttachments.size()) {
      return null;
    }
    ActivityFileAttachment activityFileAttachment = activityFileAttachments.get(i);
    return activityFileAttachment.getMimeType();
  }

  public void setMimeType(String mimeType, int i) {
    if ((i + 1) > activityFileAttachments.size()) {
      return;
    }
    ActivityFileAttachment activityFileAttachment = activityFileAttachments.get(i);
    activityFileAttachment.setMimeType(mimeType);
  }

  public String getNodeUUID(int i) {
    if ((i + 1) > activityFileAttachments.size()) {
      return null;
    }
    ActivityFileAttachment activityFileAttachment = activityFileAttachments.get(i);
    return activityFileAttachment.getNodeUUID();
  }

  public void setNodeUUID(String nodeUUID, int i) {
    if ((i + 1) > activityFileAttachments.size()) {
      return;
    }
    ActivityFileAttachment activityFileAttachment = activityFileAttachments.get(i);
    activityFileAttachment.setNodeUUID(nodeUUID);
  }

  public String getState(int i) {
    if ((i + 1) > activityFileAttachments.size()) {
      return null;
    }
    ActivityFileAttachment activityFileAttachment = activityFileAttachments.get(i);
    return activityFileAttachment.getState();
  }

  public void setState(String state, int i) {
    if ((i + 1) > activityFileAttachments.size()) {
      return;
    }
    ActivityFileAttachment activityFileAttachment = activityFileAttachments.get(i);
    activityFileAttachment.setState(state);
  }

  public String getAuthor(int i) {
    if ((i + 1) > activityFileAttachments.size()) {
      return null;
    }
    ActivityFileAttachment activityFileAttachment = activityFileAttachments.get(i);
    return activityFileAttachment.getAuthor();
  }

  public void setAuthor(String author, int i) {
    if ((i + 1) > activityFileAttachments.size()) {
      return;
    }
    ActivityFileAttachment activityFileAttachment = activityFileAttachments.get(i);
    activityFileAttachment.setAuthor(author);
  }

  public String getDocTypeName(int i) {
    if ((i + 1) > activityFileAttachments.size()) {
      return null;
    }
    ActivityFileAttachment activityFileAttachment = activityFileAttachments.get(i);
    return activityFileAttachment.getDocTypeName();
  }

  public String getDocTitle(int i) {
    if ((i + 1) > activityFileAttachments.size()) {
      return null;
    }
    ActivityFileAttachment activityFileAttachment = activityFileAttachments.get(i);
    return activityFileAttachment.getDocTitle();
  }

  public String getDocVersion(int i) {
    if ((i + 1) > activityFileAttachments.size()) {
      return null;
    }
    ActivityFileAttachment activityFileAttachment = activityFileAttachments.get(i);
    return activityFileAttachment.getDocVersion();
  }

  public String getDocSummary(int i) {
    if ((i + 1) > activityFileAttachments.size()) {
      return null;
    }
    ActivityFileAttachment activityFileAttachment = activityFileAttachments.get(i);
    return activityFileAttachment.getDocSummary();
  }

  public boolean isSymlink(int i) {
    if ((i + 1) > activityFileAttachments.size()) {
      return false;
    }
    ActivityFileAttachment activityFileAttachment = activityFileAttachments.get(i);
    return activityFileAttachment.isSymlink();
  }

  public String getTitle(Node node) throws Exception {
    return Utils.getTitle(node);
  }
  
  private String convertDateFormat(String strDate, String strOldFormat, String strNewFormat) throws ParseException {
    if (strDate == null || strDate.length() <= 0) {
      return "";
    }
    Locale locale = Util.getPortalRequestContext().getLocale();
    SimpleDateFormat sdfSource = new SimpleDateFormat(strOldFormat);
    SimpleDateFormat sdfDestination = new SimpleDateFormat(strNewFormat, locale);
    Date date = sdfSource.parse(strDate);
    return sdfDestination.format(date);
  }

  private String convertDateUsingFormat(Calendar date, String format) throws ParseException {
    Locale locale = Util.getPortalRequestContext().getLocale();
    DateFormat dateFormat = DateFormat.getDateInstance(DateFormat.SHORT, locale);
    return dateFormat.format(date.getTime());
  }

  public String getDateCreated(int i) throws ParseException {
    if ((i + 1) > activityFileAttachments.size()) {
      return null;
    }
    ActivityFileAttachment activityFileAttachment = activityFileAttachments.get(i);
    return convertDateFormat(activityFileAttachment.getDateCreated(), ISO8601.SIMPLE_DATETIME_FORMAT, NEW_DATE_FORMAT);
  }  

  public void setDateCreated(String dateCreated, int i) {
    if ((i + 1) > activityFileAttachments.size()) {
      return;
    }
    ActivityFileAttachment activityFileAttachment = activityFileAttachments.get(i);
    activityFileAttachment.setDateCreated(dateCreated);
  }

  public String getLastModified(int i) throws ParseException {
    if ((i + 1) > activityFileAttachments.size()) {
      return null;
    }
    ActivityFileAttachment activityFileAttachment = activityFileAttachments.get(i);
    return convertDateFormat(activityFileAttachment.getLastModified(), ISO8601.SIMPLE_DATETIME_FORMAT, NEW_DATE_FORMAT);
  }

  public void setLastModified(String lastModified, int i) {
    if ((i + 1) > activityFileAttachments.size()) {
      return;
    }
    ActivityFileAttachment activityFileAttachment = activityFileAttachments.get(i);
    activityFileAttachment.setLastModified(lastModified);
  }

  public Node getContentNode(int i) {
    if ((i + 1) > activityFileAttachments.size()) {
      return null;
    }
    ActivityFileAttachment activityFileAttachment = activityFileAttachments.get(i);
    Node tmpContentNode = activityFileAttachment.getContentNode();
    try {
      if (activityFileAttachment.getNodeLocation() != null && (tmpContentNode == null || !tmpContentNode.getSession().isLive())) {
        tmpContentNode = NodeLocation.getNodeByLocation(activityFileAttachment.getNodeLocation());
      }
    } catch (RepositoryException e) {
      if (activityFileAttachment.getNodeLocation() != null) {
        tmpContentNode = NodeLocation.getNodeByLocation(activityFileAttachment.getNodeLocation());
      }
    }
    activityFileAttachment.setContentNode(tmpContentNode);
    return tmpContentNode;
  }

  public void setContentNode(Node contentNode, int i) {
    if ((i + 1) > activityFileAttachments.size()) {
      return;
    }
    ActivityFileAttachment activityFileAttachment = activityFileAttachments.get(i);
    activityFileAttachment.setContentNode(contentNode);
    activityFileAttachment.setNodeLocation(NodeLocation.getNodeLocationByNode(contentNode));
  }

  public NodeLocation getNodeLocation(int i) {
    if ((i + 1) > activityFileAttachments.size()) {
      return null;
    }
    ActivityFileAttachment activityFileAttachment = activityFileAttachments.get(i);
    return activityFileAttachment.getNodeLocation();
  }

  public void setNodeLocation(NodeLocation nodeLocation, int i) {
    if ((i + 1) > activityFileAttachments.size()) {
      return;
    }
    ActivityFileAttachment activityFileAttachment = activityFileAttachments.get(i);
    activityFileAttachment.setNodeLocation(nodeLocation);
  }

  /**
   * Gets the summary.
   * @param node the node
   * @return the summary of Node. Return empty string if catch an exception.
   */
  public String getSummary(Node node) {
    return org.exoplatform.wcm.ext.component.activity.listener.Utils.getSummary(node);
  }
  
  public String getDocumentSummary(Map<String, String> activityParams) {
    return activityParams.get(FileUIActivity.DOCUMENT_SUMMARY);
  }

  public String getUserFullName(String userId) {
    if(StringUtils.isEmpty(userId)) {
      return "";
    }

    // if the requested user is the connected user, get the fullname from the ConversationState
    ConversationState currentUserState = ConversationState.getCurrent();
    Identity currentUserIdentity = currentUserState.getIdentity();
    if(currentUserIdentity != null) {
      String currentUser = currentUserIdentity.getUserId();
      if (currentUser != null && currentUser.equals(userId)) {
        User user = (User) currentUserState.getAttribute(CacheUserProfileFilter.USER_PROFILE);
        if(user != null) {
          return user.getDisplayName();
        }
      }
    }

    // if the requested user if not the connected user, fetch it from the organization service
    try {
      User user = getOrganizationService().getUserHandler().findUserByName(userId);
      if(user != null) {
        return user.getDisplayName();
      }
    } catch (Exception e) {
      LOG.error("Cannot get information of user " + userId + " : " + e.getMessage(), e);
    }

    return "";
  }
  
  protected String getSize(Node node) {
    double size = 0;    
    try {
      if (node.hasNode(Utils.JCR_CONTENT)) {
        Node contentNode = node.getNode(Utils.JCR_CONTENT);
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
    } catch(NullPointerException e) {
    	return StringUtils.EMPTY;
    }
    return StringUtils.EMPTY;    
  }
  
  protected double getFileSize(Node node) {
    double fileSize = 0;    
    try {
      if(node.isNodeType(NodetypeConstant.EXO_SYMLINK)) {
        node = Utils.getNodeSymLink(node);
      }
      if (node.hasNode(Utils.JCR_CONTENT)) {
        Node contentNode = node.getNode(Utils.JCR_CONTENT);
        if (contentNode.hasProperty(Utils.JCR_DATA)) {
        	fileSize = contentNode.getProperty(Utils.JCR_DATA).getLength();
        }
      }
    } catch(Exception ex) { fileSize = 0; }
    return fileSize;    
  }
  
  protected int getImageWidth(Node node, int i) {
    if ((i + 1) > activityFileAttachments.size()) {
      return 0;
    }
    ActivityFileAttachment activityFileAttachment = activityFileAttachments.get(i);

  	int imageWidth = 0;
  	try {
      if(node.isNodeType(NodetypeConstant.EXO_SYMLINK)) {
        node = Utils.getNodeSymLink(node);
      }
  		if(node.hasNode(NodetypeConstant.JCR_CONTENT)) node = node.getNode(NodetypeConstant.JCR_CONTENT);
    	ImageReader reader = ImageIO.getImageReadersByMIMEType(activityFileAttachment.getMimeType()).next();
    	ImageInputStream iis = ImageIO.createImageInputStream(node.getProperty("jcr:data").getStream());
    	reader.setInput(iis, true);
    	imageWidth = reader.getWidth(0);
    	iis.close();
    	reader.dispose();   	
    } catch (Exception e) {
        if(LOG.isTraceEnabled()) {
          String nodePath = null;
          try {
            nodePath = node.getPath();
          } catch(Exception exp) {
            // Nothing to log
          }
          LOG.trace("Cannot get image from node " + nodePath, e);
        }
    }
  	return imageWidth;
  }
  
  protected int getImageHeight(Node node, int i) {
    if ((i + 1) > activityFileAttachments.size()) {
      return 0;
    }
    ActivityFileAttachment activityFileAttachment = activityFileAttachments.get(i);

  	int imageHeight = 0;
  	try {
      if(node.isNodeType(NodetypeConstant.EXO_SYMLINK)) {
        node = Utils.getNodeSymLink(node);
      }
  		if(node.hasNode(NodetypeConstant.JCR_CONTENT)) node = node.getNode(NodetypeConstant.JCR_CONTENT);
    	ImageReader reader = ImageIO.getImageReadersByMIMEType(activityFileAttachment.getMimeType()).next();
    	ImageInputStream iis = ImageIO.createImageInputStream(node.getProperty("jcr:data").getStream());
    	reader.setInput(iis, true);
    	imageHeight = reader.getHeight(0);
    	iis.close();
    	reader.dispose();   	
    } catch (Exception e) {
        LOG.info("Cannot get node");
    }
  	return imageHeight;
  }

  protected String getDocUpdateDate(Node node) {
    String docUpdatedDate = "";
    try {
      if(node != null && node.hasProperty("exo:lastModifiedDate")) {
        String rawDocUpdatedDate = node.getProperty("exo:lastModifiedDate").getString();
        LocalDateTime parsedDate = LocalDateTime.parse(rawDocUpdatedDate, DateTimeFormatter.ISO_OFFSET_DATE_TIME);
        docUpdatedDate = parsedDate.format(getDateTimeFormatter());
      }
    } catch (RepositoryException e) {
      LOG.error("Cannot get document updated date : " + e.getMessage(), e);
    }
    return docUpdatedDate;
  }

  protected String getCloudfileUpdateDate(CloudFile cloudFile) {
    String cloudfileUpdatedDate = "";

    if (cloudFile != null) {
      java.util.Calendar modifiedDate = cloudFile.getModifiedDate();

      if (modifiedDate == null) {
        return cloudfileUpdatedDate;
      }
      TimeZone tz = modifiedDate.getTimeZone();
      ZoneId zid = tz == null ? ZoneId.systemDefault() : tz.toZoneId();
      LocalDateTime localDateTime = LocalDateTime.ofInstant(modifiedDate.toInstant(), zid);

      cloudfileUpdatedDate = localDateTime.format(getDateTimeFormatter());
    }
    return cloudfileUpdatedDate;
  }

  /**
   * Get a localized DateTimeFormatter
   * @return A localized DateTimeFormatter
   */
  protected DateTimeFormatter getDateTimeFormatter() {
    if(dateTimeFormatter == null) {
      dateTimeFormatter = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM);
      Locale locale = WebuiRequestContext.getCurrentInstance().getLocale();
      if (locale != null) {
        dateTimeFormatter = dateTimeFormatter.withLocale(locale);
      }
    }
    return dateTimeFormatter;
  }

  protected String getDocLastModifier(Node node) {
    String docLastModifier = "";
    try {
      if (node.isNodeType("exo:symlink")){
        String uuid = node.getProperty("exo:uuid").getString();
        node = node.getSession().getNodeByUUID(uuid);
      }
      if(node != null && node.hasProperty("exo:lastModifier")) {
        String docLastModifierUsername = node.getProperty("exo:lastModifier").getString();
        docLastModifier = getUserFullName(docLastModifierUsername);
      }
    } catch (RepositoryException e) {
      LOG.error("Cannot get document last modifier : " + e.getMessage(), e);
    }
    return docLastModifier;
  }

  protected int getVersion(Node node) {
    String currentVersion = null;
    try {
      if (node.isNodeType(VersionHistoryUtils.MIX_DISPLAY_VERSION_NAME) &&
              node.hasProperty(VersionHistoryUtils.MAX_VERSION_PROPERTY)) {
        //Get max version ID
        int max = (int) node.getProperty(VersionHistoryUtils.MAX_VERSION_PROPERTY).getLong();
        return max - 1;
      }
      currentVersion = node.getBaseVersion().getName();
      if (currentVersion.contains("jcr:rootVersion")) currentVersion = "0";
    }catch (Exception e) {
      currentVersion ="0";
    }
    return Integer.parseInt(currentVersion);
  }

  public String getUserProfileUri(String userId) {
    ExoContainer container = ExoContainerContext.getCurrentContainer();
    IdentityManager identityManager = (IdentityManager) container.getComponentInstanceOfType(IdentityManager.class);

    return identityManager.getOrCreateIdentity(OrganizationIdentityProvider.NAME, userId, true).getProfile().getUrl();
  }

  public String getUserAvatarImageSource(String userId) {
    return getOwnerIdentity().getProfile().getAvatarUrl();
  }

  public String getSpaceAvatarImageSource(String spaceIdentityId) {
    try {
      String spaceId = getOwnerIdentity().getRemoteId();
      SpaceService spaceService = getSpaceService();
      Space space = spaceService.getSpaceById(spaceId);
      if (space != null) {
        return space.getAvatarUrl();
      }
    } catch (SpaceStorageException e) {
      LOG.warn("Failed to getSpaceById: " + spaceIdentityId, e);
    }
    return null;
  }

  private SpaceService getSpaceService() {
    if (spaceService == null) {
      spaceService = getApplicationComponent(SpaceService.class);
    }
    return spaceService;
  }

  public String getActivityStatus() {
    if (message == null) {
      return activityStatus;
    } else {
      return message;
    }
  }

  public int getFilesCount() {
    return filesCount;
  }

  public void setUIActivityData(Map<String, String> activityParams) {
    activityFileAttachments.clear();

    this.message =  activityParams.get(FileUIActivity.MESSAGE);
    this.activityStatus =  activityParams.get(FileUIActivity.ACTIVITY_STATUS);

    String[] nodeUUIDs = getParameterValues(activityParams, FileUIActivity.ID);
    String[] repositories = getParameterValues(activityParams,UIDocActivity.REPOSITORY);
    String[] workspaces = getParameterValues(activityParams,UIDocActivity.WORKSPACE);
    if (nodeUUIDs == null || nodeUUIDs.length == 0) {
      String[] docPaths = getParameterValues(activityParams, UIDocActivity.DOCPATH);
      if (docPaths != null && docPaths.length > 0) {
        nodeUUIDs = new String[docPaths.length];
        for (int i = 0; i < docPaths.length; i++)  {
          String repository = "repository";
          if (repositories != null && repositories.length == docPaths.length && StringUtils.isNotBlank(repositories[i])) {
            repository = repositories[i];
          }
          String workspace = "collaboration";
          if (workspaces != null && workspaces.length == docPaths.length && StringUtils.isNotBlank(workspaces[i])) {
            workspace = workspaces[i];
          }
          String docPath = docPaths[i];

          NodeLocation nodeLocation = new NodeLocation(repository, workspace, docPath);
          Node node = NodeLocation.getNodeByLocation(nodeLocation);
          if (node != null) {
            try {
              nodeUUIDs[i] = node.getUUID();
            } catch (RepositoryException e) {
              LOG.error("can not get UUID", e);
            }
          }
        }
      }
    }
    this.filesCount = nodeUUIDs == null ? 0 : nodeUUIDs.length;

    String[] contentLink = getParameterValues(activityParams,FileUIActivity.CONTENT_LINK);
    String[] state = getParameterValues(activityParams, FileUIActivity.STATE);
    String[] author = getParameterValues(activityParams, FileUIActivity.AUTHOR);
    String[] dateCreated =  getParameterValues(activityParams, FileUIActivity.DATE_CREATED);
    String[] lastModified =  getParameterValues(activityParams, FileUIActivity.LAST_MODIFIED);
    String[] mimeType =  getParameterValues(activityParams, FileUIActivity.MIME_TYPE);
    String[] imagePath =  getParameterValues(activityParams, FileUIActivity.IMAGE_PATH);
    String[] docTypeName =  getParameterValues(activityParams, FileUIActivity.DOCUMENT_TYPE_LABEL);
    String[] docTitle =  getParameterValues(activityParams, FileUIActivity.DOCUMENT_TITLE);  
    String[] docVersion =  getParameterValues(activityParams, FileUIActivity.DOCUMENT_VERSION);
    String[] docSummary =  getParameterValues(activityParams, FileUIActivity.DOCUMENT_SUMMARY);
    String[] docPath =  getParameterValues(activityParams, UIDocActivity.DOCPATH);
    Boolean[] isSymlink = null;
    String[] isSymlinkParams = getParameterValues(activityParams, UIDocActivity.IS_SYMLINK);
    if(isSymlinkParams != null) {
      isSymlink = new Boolean[isSymlinkParams.length];
      for (int i = 0; i < isSymlinkParams.length; i++) {
        isSymlink[i] = Boolean.parseBoolean(isSymlinkParams[i]);
      }
    }

    for (int i = 0; i < this.filesCount; i++) {
      ActivityFileAttachment fileAttachment = new ActivityFileAttachment();
      String repositoryName = (String) getValueFromArray(i, repositories);
      String workspaceName = (String) getValueFromArray(i, workspaces);

      if(StringUtils.isBlank(repositoryName)) {
        ManageableRepository repository = WCMCoreUtils.getRepository();
        repositoryName = repository == null ? null : repository.getConfiguration().getName();
      }

      if(StringUtils.isBlank(workspaceName)) {
        ManageableRepository repository = WCMCoreUtils.getRepository();
        workspaceName =  repository == null ? null : repository.getConfiguration().getDefaultWorkspaceName();
      }
      fileAttachment.setNodeUUID(nodeUUIDs[i])
                    .setRepository(repositoryName)
                    .setWorkspace(workspaceName)
                    .setContentLink((String) getValueFromArray(i, contentLink))
                    .setState((String) getValueFromArray(i, state))
                    .setAuthor(getValueFromArray(i, author))
                    .setDateCreated(getValueFromArray(i, dateCreated))
                    .setLastModified(getValueFromArray(i, lastModified))
                    .setMimeType(getValueFromArray(i, mimeType))
                    .setImagePath(getValueFromArray(i, imagePath))
                    .setDocTypeName(getValueFromArray(i, docTypeName))
                    .setDocTitle(getValueFromArray(i, docTitle))
                    .setDocVersion(getValueFromArray(i, docVersion))
                    .setDocSummary(getValueFromArray(i, docSummary))
                    .setSymlink(getValueFromArray(i, isSymlink))
                    .setDocPath(getValueFromArray(i,docPath));

      Node contentNode = NodeLocation.getNodeByLocation(fileAttachment.getNodeLocation());
      if (contentNode != null) {
        try {
          if (!getTrashService().isInTrash(contentNode) && !isQuarantinedItem(contentNode)) {
            fileAttachment.setContentName(getContentName(contentNode, fileAttachment.getContentName()));
            activityFileAttachments.add(fileAttachment);
          }
        } catch (Exception e) {
          LOG.error("Error while testing if the content is in trash", e);
        }
      }
    }
    this.filesCount = this.activityFileAttachments.size();
  }

  private <T> T getValueFromArray(int index, T... valuesArray) {
    return (valuesArray == null || index > (valuesArray.length - 1)) ? null : valuesArray[index];
  }

  private boolean isQuarantinedItem(Node node) throws RepositoryException {
    return node.getPath().startsWith("/" + FileDlpConnector.DLP_SECURITY_FOLDER + "/");
  }

  private String[] getParameterValues(Map<String, String> activityParams, String paramName) {
    String[] values = null;
    String value = activityParams.get(paramName);
    if(value == null) {
      value = activityParams.get(paramName.toLowerCase());
    }
    if(value != null) {
      values = value.split(SEPARATOR_REGEX);
    }
    if (LOG.isDebugEnabled()) {
      if(this.filesCount != 0 && (values == null || values.length != this.filesCount)) {
          LOG.debug("Parameter '{}' hasn't same length as other activity parmameters", paramName);
      }
    }
    return values;
  }

  /**
   * Gets the webdav url.
   * 
   * @return the webdav url
   * @throws Exception the exception
   */
  public String getWebdavURL(int i) throws Exception {
    if ((i + 1) > activityFileAttachments.size()) {
      return null;
    }
    ActivityFileAttachment activityFileAttachment = activityFileAttachments.get(i);
    if (activityFileAttachment.getWebdavURL() != null) {
      return activityFileAttachment.getWebdavURL();
    }

    PortletRequestContext portletRequestContext = WebuiRequestContext.getCurrentInstance();
    PortletRequest portletRequest = portletRequestContext.getRequest();
    String repository = activityFileAttachment.getRepository();
    String workspace = activityFileAttachment.getWorkspace();
    String baseURI = portletRequest.getScheme() + "://" + portletRequest.getServerName() + ":"
        + String.format("%s", portletRequest.getServerPort());

    FriendlyService friendlyService = WCMCoreUtils.getService(FriendlyService.class);
    String link = "#";

    String portalName = PortalContainer.getCurrentPortalContainerName();
    String restContextName = PortalContainer.getCurrentRestContextName();
    Node tmpContentNode = this.getContentNode(i);
    if (tmpContentNode.isNodeType("nt:frozenNode")) {
      String uuid = tmpContentNode.getProperty("jcr:frozenUuid").getString();
      Node originalNode = tmpContentNode.getSession().getNodeByUUID(uuid);
      link = baseURI + "/" + portalName + "/" + restContextName + "/jcr/" + repository + "/"
          + workspace + originalNode.getPath() + "?version=" + this.getContentNode(i).getParent().getName();
    } else {
      link = baseURI + "/" + portalName + "/" + restContextName + "/jcr/" + repository + "/"
          + workspace + tmpContentNode.getPath();
    }

    activityFileAttachment.setWebdavURL(friendlyService.getFriendlyUri(link));
    return activityFileAttachment.getWebdavURL();
  }

  public String[] getSystemCommentBundle(Map<String, String> activityParams) {
    return org.exoplatform.wcm.ext.component.activity.listener.Utils.getSystemCommentBundle(activityParams);
  }

  public String[] getSystemCommentTitle(Map<String, String> activityParams) {
    return org.exoplatform.wcm.ext.component.activity.listener.Utils.getSystemCommentTitle(activityParams);
  }

  public DriveData getDocDrive(int i) {
    if ((i + 1) > activityFileAttachments.size()) {
      return null;
    }
    ActivityFileAttachment activityFileAttachment = activityFileAttachments.get(i);
    if (activityFileAttachment.getDocDrive() != null) {
      return activityFileAttachment.getDocDrive();
    }

    NodeLocation nodeLocation = activityFileAttachment.getNodeLocation();
    if (nodeLocation != null) {
      try {
        String userId = ConversationState.getCurrent().getIdentity().getUserId();
        activityFileAttachment.setDocDrive(getDocumentService().getDriveOfNode(nodeLocation.getPath(), "placeholder_user_name", Utils.getMemberships()));
      } catch (Exception e) {
        LOG.error("Cannot get drive of node " + nodeLocation.getPath() + " by attachment node location : " + e.getMessage(), e);
      }
    }
    return activityFileAttachment.getDocDrive();
  }
  
  protected DriveData getDocDrive(NodeLocation nodeLocation) {
    DriveData driveData = null;
    if (nodeLocation != null) {
      try {
        driveData = getDocumentService().getDriveOfNode(nodeLocation.getPath(), "placeholder_user_name", Utils.getMemberships());
      } catch (Exception e) {
        LOG.error("Cannot get drive of node " + nodeLocation.getPath() + " by shared attachment node location : " + e.getMessage(), e);
      }
    }
    return driveData;
  }

  public String getDefaultIconClass(int i) {
    String iconClass = "uiBgdFile";
    String contentName = getContentName(i);
    if (StringUtils.isNotBlank(contentName)) {
      if (contentName.toLowerCase().contains(".pdf")) {
        iconClass = "uiBgdFilePDF";
      } else if (contentName.toLowerCase().contains(".doc")) {
          iconClass = "uiBgdFileWord";
      } else if (contentName.toLowerCase().contains(".xls")) {
          iconClass = "uiBgdFileExcel";
      } else if (contentName.toLowerCase().contains(".ppt")) {
          iconClass = "uiBgdFilePPT";
      }
    }
    return iconClass;
  }

  public String getDocFileBreadCrumb(int i) {
    LinkedHashMap<String, String> docFolderBreadCrumb = getDocFolderRelativePathWithLinks(i);
    String breadCrumbContent = "";
    if (docFolderBreadCrumb != null) {
      int breadCrumbSize = docFolderBreadCrumb.size();
      int folderIndex = 0;
      for (String folderName : docFolderBreadCrumb.keySet()) {
        String folderPath = docFolderBreadCrumb.get(folderName);
        folderName = folderName.replaceAll("_" + (breadCrumbSize - folderIndex - 1) + "$", "");
        if (folderIndex < (breadCrumbSize - 1)) {
          if (folderIndex > 0) {
            breadCrumbContent += ",";
          }
          breadCrumbContent += "'" + folderName.replace("'", "\\'") + "': '" + folderPath + "'";
          breadCrumbContent = breadCrumbContent.replace("%27", "\\'");
        }
        folderIndex++;
      }
    }
    return breadCrumbContent;
  }
  
  public String getDocFilePath(int i) {
    LinkedHashMap<String, String> folderRelativePathWithLinks = getDocFolderRelativePathWithLinks(i);
    if(folderRelativePathWithLinks != null && !folderRelativePathWithLinks.isEmpty()) {
      String[] nodeNames = folderRelativePathWithLinks.values().toArray(EMPTY_ARRAY);
      return nodeNames[nodeNames.length - 1];
    }
    return null;
  }

  public LinkedHashMap<String, String> getDocFolderRelativePathWithLinks(int i) {
    if ((i + 1) > activityFileAttachments.size()) {
      return null;
    }
    if(folderPathWithLinks == null) {
      folderPathWithLinks = new LinkedHashMap[filesCount];
    }
    if(folderPathWithLinks[i] == null) {
      folderPathWithLinks[i] = new LinkedHashMap<>();
      LinkedHashMap<String, String> reversedFolderPathWithLinks = new LinkedHashMap<>();
      
      DriveData drive;
      Node sharedNode = getSharedNode(i);
      if (sharedNode != null) {
        drive = getDocDrive(NodeLocation.getNodeLocationByNode(sharedNode));
      } else {
        drive = getDocDrive(i);
      }
      if (drive != null) {
        try {
          Map<String, String> parameters = drive.getParameters();
          String driveName = drive.getName();
          if (parameters != null) {
            if (parameters.containsKey("groupId")) {
              String groupId = parameters.get("groupId");
              if (StringUtils.isNotBlank(groupId)) {
                try {
                  groupId = groupId.replaceAll("\\.", "/");
                  if (groupId.startsWith(SpaceUtils.SPACE_GROUP)) {
                    SpaceService spaceService = getSpaceService();
                    Space space = spaceService.getSpaceByGroupId(groupId);
                    if (space != null) {
                      driveName = space.getDisplayName();
                    }
                  } else {
                    Group group = getOrganizationService().getGroupHandler().findGroupById(groupId);
                    driveName = group == null ? driveName : group.getLabel();
                  }
                } catch (Exception e) {
                  LOG.warn("Can't get drive name for group with id '" + groupId + "'", e);
                }
              }
            } else if (parameters.containsKey("userId")) {
              String userId = parameters.get("userId");
              if (StringUtils.isNotBlank(userId)) {
                try {
                  userId = userId.indexOf("/") >= 0 ? userId.substring(userId.lastIndexOf("/") + 1) : userId;
                  User user = getOrganizationService().getUserHandler().findUserByName(userId);
                  if (user != null) {
                    driveName = user.getDisplayName();
                  }
                } catch (Exception e) {
                  LOG.warn("Can't get drive name for user with id '" + userId + "'", e);
                }
              }
            }
          }
          String driveHomePath = drive.getResolvedHomePath();

          // if the drive is the Personal Documents drive, we must handle the special case of the Public symlink
          String drivePublicFolderHomePath = null;
          if (ManageDriveServiceImpl.PERSONAL_DRIVE_NAME.equals(drive.getName())) {
            drivePublicFolderHomePath = driveHomePath.replace("/" + ManageDriveServiceImpl.PERSONAL_DRIVE_PRIVATE_FOLDER_NAME, "/" + ManageDriveServiceImpl.PERSONAL_DRIVE_PUBLIC_FOLDER_NAME);
          }

          // calculate the relative path to the drive by browsing up the content node path
          Node parentContentNode;
          if (sharedNode != null) {
            parentContentNode = sharedNode;
          } else {
            parentContentNode = getContentNode(i);
          }
          while (parentContentNode != null) {
            String parentPath = parentContentNode.getPath();
            // exit condition is check here instead of in the while condition to avoid
            // retrieving the path several times and because there is some logic to handle
            if (!parentPath.contains(driveHomePath)) {
              // The parent path is outside drive
              break;
            } else if (!driveHomePath.equals("/") && parentPath.equals("/")) {
              // we are at the root of the workspace
              break;
            } else if (drivePublicFolderHomePath != null && parentPath.equals(drivePublicFolderHomePath)) {
              // this is a special case : the root of the Public folder of the Personal Documents drive
              // in this case we add the Public folder in the path
              reversedFolderPathWithLinks.put(ManageDriveServiceImpl.PERSONAL_DRIVE_PUBLIC_FOLDER_NAME, getDocOpenUri(parentPath, i));
              break;
            }

            String nodeName;
            // title is used if it exists, otherwise the name is used
            if (parentPath.equals(driveHomePath)) {
              nodeName = driveName;
            } else if (parentContentNode.hasProperty("exo:title")) {
              nodeName = parentContentNode.getProperty("exo:title").getString();
            } else {
              nodeName = parentContentNode.getName();
            }
            reversedFolderPathWithLinks.put(nodeName + "_" + reversedFolderPathWithLinks.size(), getDocOpenUri(parentPath, i));

            if (parentPath.equals("/")) {
              break;
            } else {
              parentContentNode = parentContentNode.getParent();
            }
          }
        } catch (AccessDeniedException e) {
          LOG.debug(e.getMessage());
        } catch (RepositoryException re) {
          ActivityFileAttachment activityFileAttachment = activityFileAttachments.get(i);
          LOG.error("Cannot retrieve path of doc " + activityFileAttachment.getDocPath() + " : " + re.getMessage(), re);
        }
      }

      if(reversedFolderPathWithLinks.size() > 1) {
        List<Map.Entry<String, String>> entries = new ArrayList<>(reversedFolderPathWithLinks.entrySet());
        for(int j = entries.size()-1; j >= 0; j--) {
          Map.Entry<String, String> entry = entries.get(j);
          folderPathWithLinks[i].put(StringEscapeUtils.escapeHtml4(entry.getKey()), entry.getValue());
        }
      } else {
        folderPathWithLinks[i] = reversedFolderPathWithLinks;
      }
    }

    return folderPathWithLinks[i];
  }

  private OrganizationService getOrganizationService() {
    if (organizationService == null) {
      organizationService = getApplicationComponent(OrganizationService.class);
    }
    return organizationService;
  }

  private DocumentService getDocumentService() {
    if (documentService == null) {
      documentService = getApplicationComponent(DocumentService.class);
    }
    return documentService;
  }
  
  private TrashService getTrashService() {
    if (trashService == null) {
      trashService = getApplicationComponent(TrashService.class);
    }
    return trashService;
  }

  public String getDocFolderRelativePath(int i) {
    StringBuilder folderRelativePath = new StringBuilder();

    Set<String> relativePaths = getDocFolderRelativePathWithLinks(i).keySet();
    int pathSize = relativePaths.size();
    Iterator<String> relativePathIterator = relativePaths.iterator();
    int folderIndex = 0;
    while (relativePathIterator.hasNext()) {
      String folderName = relativePathIterator.next();

      // Delete file from parent Path
      if(relativePathIterator.hasNext()) {
        folderName = folderName.replaceAll("_" + (pathSize - folderIndex -1) + "$", "");
        folderRelativePath.append(folderName).append("/");
      }
      folderIndex++;
    }

    if(folderRelativePath.length() > 1) {
      // remove the last /
      folderRelativePath.deleteCharAt(folderRelativePath.length() - 1);
    }

    return folderRelativePath.toString();
  }

  public String getCurrentDocOpenUri(int i) {
    if ((i + 1) > activityFileAttachments.size()) {
      return null;
    }
    ActivityFileAttachment activityFileAttachment = activityFileAttachments.get(i);

    String uri = "";
    if(activityFileAttachment.getNodeLocation() != null) {
      uri = getDocOpenUri(activityFileAttachment.getDocPath(), i);
    }

    return uri;
  }

  public String getDocOpenUri(String nodePath, int i) {
    String uri = "";
    DriveData driveData = null;

    if (nodePath != null) {
      try {
        Space space = getSpace();

        if (space != null) {
          ActivityFileAttachment activityFileAttachment = activityFileAttachments.get(i);
          Node node = activityFileAttachment.getContentNode();
          String[] permissions = new String[] { PermissionType.READ };
          String organizationalIdentity;

          Collection<Membership> memberships = null;
          String currentUserId = ConversationState.getCurrent().getIdentity().getUserId();
          try {
            memberships = getOrganizationService().getMembershipHandler()
                                                  .findMembershipsByUserAndGroup(currentUserId, space.getGroupId());
          } catch (Exception e) {
            LOG.warn("Error getting memberships by user (" + currentUserId + ") and group (" + space.getGroupId() + ")", e);
          }

          try {
            for (Membership membership : memberships) {
              organizationalIdentity = new MembershipEntry(membership.getGroupId(), membership.getMembershipType()).toString();

              if (PermissionUtil.hasPermissions(node, organizationalIdentity, permissions)) {
                Node sharedNode = getSharedNode(i);

                if (sharedNode != null) {
                  driveData = getDocDrive(NodeLocation.getNodeLocationByNode(sharedNode));

                  if (driveData != null) {
                    nodePath = sharedNode.getPath();
                  }
                  break;
                }
              }
            }
          } catch (Exception e) {
            LOG.warn("Error while getting node path and drive data of shared node " + nodePath + " : " + e.getMessage(), e);
          }
        }

        if (driveData == null) {
          driveData = getDocDrive(i);
        }

        if (nodePath.endsWith("/")) {
          nodePath = nodePath.replaceAll("/$", "");
        }
        uri = getDocumentService().getLinkInDocumentsApp(nodePath, driveData);
      } catch (Exception e) {
        LOG.error("Cannot get document open URI of node " + nodePath + " : " + e.getMessage(), e);
        uri = "";
      }
    }

    return uri;
  }

  /**
   * Gets shared node.
   *
   * @param i the activity file attachments index
   * @return the shared node
   */
  protected Node getSharedNode(int i) {
    Node sharedNode = null;
    Space space = getSpace();

    if (space != null) {
      ActivityFileAttachment activityFileAttachment = activityFileAttachments.get(i);
      if (activityFileAttachment != null) {
        Node node = activityFileAttachment.getContentNode();
        String nodePath = null;

        try {
          nodePath = node.getPath();
          Node spaceSharedFolder = getSpaceSharedFolder(space.getGroupId());
          if (spaceSharedFolder != null && spaceSharedFolder.hasNode(node.getName())) {
            sharedNode = spaceSharedFolder.getNode(node.getName());
          }
        } catch (PathNotFoundException e) {
          LOG.info("Node with path " + nodePath + " isn't found in space shared folder");
        } catch (Exception e) {
          LOG.info("Error while getting shared node for " + nodePath + " : " + e.getMessage(), e);
        }
      }
    }

    return sharedNode;
  }

  /**
   * Gets space shared folder.
   *
   * @param spaceGroupId the space group id
   * @return the space shared folder
   */
  protected Node getSpaceSharedFolder(String spaceGroupId) {
    Node rootSpace = null;
    Node shared = null;

    if (!(spaceGroupId == null || spaceGroupId.isEmpty())) {
      NodeHierarchyCreator nodeCreator = WCMCoreUtils.getService(NodeHierarchyCreator.class);
      nodeCreator.getJcrPath(BasePath.CMS_GROUPS_PATH);
      try {
        SessionProvider sessionProvider = WCMCoreUtils.getService(SessionProviderService.class).getSystemSessionProvider(null);
        ManageableRepository repository = WCMCoreUtils.getService(RepositoryService.class).getCurrentRepository();
        Session session = sessionProvider.getSession(repository.getConfiguration().getDefaultWorkspaceName(), repository);

        rootSpace = (Node) session.getItem(nodeCreator.getJcrPath(BasePath.CMS_GROUPS_PATH) + spaceGroupId);
        if (rootSpace.hasNode("Documents")) {
          rootSpace = rootSpace.getNode("Documents");
          if (rootSpace.hasNode("Shared")) {
            shared = rootSpace.getNode("Shared");
          }
        }
      } catch (RepositoryException e) {
        LOG.error("Error while getting space " + spaceGroupId + " shared folder node", e);
      }
    }

    return shared;
  }

  /**
   * Gets space.
   *
   * @return the space
   */
  protected Space getSpace() {
    Space space = null;
    ExoSocialActivity activity = getActivity();

    if (activity != null) {
      String streamOwner = activity.getStreamOwner();
      space = getSpaceService().getSpaceByPrettyName(streamOwner);
    }

    return space;
  }

  public String getEditLink(int i) {
    try {
      return org.exoplatform.wcm.webui.Utils.getEditLink(getContentNode(i), true, false);
    }catch (Exception e) {
      return "";
    }
  }
  
  public String getActivityEditLink(int i) {
  	try {
      return org.exoplatform.wcm.webui.Utils.getActivityEditLink(getContentNode(i));
    }catch (Exception e) {
      return "";
    }
  }

  protected String getCssClassIconFile(String fileName, String fileType, int i) {
    try {
      return org.exoplatform.ecm.webui.utils.Utils.getNodeTypeIcon(this.getContentNode(i), "uiBgd64x64");
    } catch (RepositoryException e) {
      return "uiBgd64x64FileDefault";
    }
  }

  protected String getContainerName() {
    //get portal name
    ExoContainer container = ExoContainerContext.getCurrentContainer();
    PortalContainerInfo containerInfo = (PortalContainerInfo) container.getComponentInstanceOfType(PortalContainerInfo.class);
    return containerInfo.getContainerName();  
  }

  public String getDownloadAllLink() {
    if (activityFileAttachments.isEmpty()) {
      return null;
    }

    // Get binary data from node
    DownloadService dservice = WCMCoreUtils.getService(DownloadService.class);

    if (downloadResourceId != null && downloadLink != null) {
      DownloadResource downloadResource = dservice.getDownloadResource(downloadResourceId);
      if (downloadResource != null) {
        return downloadLink;
      }
    }

    NodeLocation[] nodeLocations = new NodeLocation[activityFileAttachments.size()];

    for (int i = 0; i < activityFileAttachments.size(); i++) {
      nodeLocations[i] = activityFileAttachments.get(i).getNodeLocation();
    }

    String fileName = getDownloadAllFileName();

    return getDownloadURL(dservice, fileName, downloadResourceId, downloadLink, nodeLocations);
  }

  private String getDownloadAllFileName() {
    String fileName = "activity_" + getActivity().getId() + "_";
    Long postedTime = getActivity().getPostedTime();
    if (postedTime != null) {
      Calendar postedDate = Calendar.getInstance();
      try {
        fileName += convertDateUsingFormat(postedDate, ISO8601.COMPLETE_DATE_FORMAT).replaceAll("/", "-");
      } catch (ParseException e) {
        LOG.warn("Error while generating date format for file name", e);
      }
    }
    return fileName + ".zip";
  }

  public String getDownloadLink(int i) {
    if (i >= activityFileAttachments.size()) {
      return null;
    }
    // Get binary data from node
    DownloadService dservice = WCMCoreUtils.getService(DownloadService.class);

    // Make download stream
    NodeLocation[] nodeLocations = new NodeLocation[] { activityFileAttachments.get(i).getNodeLocation() };

    String contentName = activityFileAttachments.get(i).getContentName();
    return getDownloadURL(dservice, contentName, null, null, nodeLocations);
  }

  /**
   * <h2>Check if file node is supported by preview on activity stream
   * A preview from the activity stream is available for the following contents:
   * </h2>
   * <ul>
   * <li>pdf and office file</li>
   * <li>media (audio, video, image)</li>
   * </ul>
   * @param data Content node
   * @return true: support; false: not support
   * @throws Exception
   */
  public boolean isFileSupportPreview(Node data) throws Exception {
    if (data != null && data.isNodeType(Utils.NT_FILE)) {
      UIExtensionManager manager = getApplicationComponent(UIExtensionManager.class);
      List<UIExtension> extensions = manager.getUIExtensions(Utils.FILE_VIEWER_EXTENSION_TYPE);

      Map<String, Object> context = new HashMap<String, Object>();
      context.put(Utils.MIME_TYPE, data.getNode(Utils.JCR_CONTENT).getProperty(Utils.JCR_MIMETYPE).getString());

      for (UIExtension extension : extensions) {
        if (manager.accept(Utils.FILE_VIEWER_EXTENSION_TYPE, extension.getName(), context) && !"Text".equals(extension.getName())) {
          return true;
        }
      }
    }

    return false;
  }

  private String getDownloadURL(DownloadService dservice,
                                String fileName,
                                String resourceId,
                                String resourceLink,
                                NodeLocation[] nodelocations) {
    try {
      if (resourceId != null && resourceLink != null) {
        DownloadResource downloadResource = dservice.getDownloadResource(downloadResourceId);
        if (downloadResource != null) {
          return resourceLink;
        }
      }

      ActivityFilesDownloadResource dresource = new ActivityFilesDownloadResource(nodelocations);
      dresource.setDownloadName(fileName);
      return dservice.getDownloadLink(dservice.addDownloadResource(dresource));
    } catch (Exception e) {
      if (LOG.isDebugEnabled()) {
        LOG.warn("An error occurred while generating download URL", e);
      } else {
        LOG.warn("An error occurred while generating download URL: {}", e.getMessage());
      }
      return "";
    }
  }

  public static class ViewDocumentActionListener extends EventListener<FileUIActivity> {
    @Override
    public void execute(Event<FileUIActivity> event) throws Exception {
      FileUIActivity fileUIActivity = event.getSource();
      String index = event.getRequestContext().getRequestParameter(OBJECTID);
      int i = Integer.parseInt(index);
      UIActivitiesContainer uiActivitiesContainer = fileUIActivity.getAncestorOfType(UIActivitiesContainer.class);
      PopupContainer uiPopupContainer = uiActivitiesContainer.getPopupContainer();

      UIDocumentPreview uiDocumentPreview = uiPopupContainer.createUIComponent(UIDocumentPreview.class, null,
              "UIDocumentPreview");
      uiDocumentPreview.setBaseUIActivity(fileUIActivity);
      if ((i + 1) > fileUIActivity.activityFileAttachments.size()) {
        return;
      }
      ActivityFileAttachment activityFileAttachment = fileUIActivity.activityFileAttachments.get(i);
      uiDocumentPreview.setContentInfo(activityFileAttachment.getDocPath(), activityFileAttachment.getRepository(), activityFileAttachment.getWorkspace(),
              fileUIActivity.getContentNode(i));

      uiPopupContainer.activate(uiDocumentPreview, 0, 0, true);
      event.getRequestContext().addUIComponentToUpdateByAjax(uiPopupContainer);
    }
  }

  public static class DownloadDocumentActionListener extends EventListener<FileUIActivity> {
    @Override
    public void execute(Event<FileUIActivity> event) throws Exception {
      FileUIActivity uiComp = event.getSource();
      String index = event.getRequestContext().getRequestParameter(OBJECTID);
      if (StringUtils.isBlank(index) && uiComp.getFilesCount() > 1) {
        String downloadLink = uiComp.getDownloadAllLink();
        event.getRequestContext().getJavascriptManager().addJavascript("ajaxRedirect('" + downloadLink + "');");
      } else {
        if (StringUtils.isBlank(index)) {
          index = "0";
        }
        String downloadLink = uiComp.getDownloadLink(Integer.parseInt(index));
        event.getRequestContext().getJavascriptManager().addJavascript("ajaxRedirect('" + downloadLink + "');");
      }
    }
  }

  public static class OpenFileActionListener extends EventListener<FileUIActivity> {
    public void execute(Event<FileUIActivity> event) throws Exception {
      FileUIActivity fileUIActivity = event.getSource();
      String index = event.getRequestContext().getRequestParameter(OBJECTID);
      int i = 0;
      if (!StringUtils.isBlank(index)) {
        i = Integer.parseInt(index);
      }

      Node currentNode = fileUIActivity.getContentNode(i);

      FileUIActivity docActivity = event.getSource();
      UIActivitiesContainer activitiesContainer = docActivity.getAncestorOfType(UIActivitiesContainer.class);
      PopupContainer popupContainer = activitiesContainer.getPopupContainer();

      org.exoplatform.ecm.webui.utils.Utils.openDocumentInDesktop(currentNode, popupContainer, event);
    }
  }

  /**
   * <h2>Check file node can edit on activity stream</h2>
   * The file only can edit when user have modify permission on parent folder
   * @param data File node
   * @return true: can edit; false: cannot edit
   */
  public boolean canEditDocument(Node data){
    try {
      ((ExtendedNode)data.getParent()).checkPermission(PermissionType.ADD_NODE);
      return true;
    } catch(Exception e) {
      return false;
    }
  }
  
  /**
   * {@inheritDoc}
   */
  @Override
  public void end() throws Exception {
    if (getDocumentService().getDocumentEditorProviders().size() > 0) {
      WebuiRequestContext requestContext = WebuiRequestContext.getCurrentInstance();
      JavascriptManager js = requestContext.getJavascriptManager();
      String activityId = getActivity().getId();
      Identity identity = ConversationState.getCurrent().getIdentity();
      RequireJS require = js.require("SHARED/editorbuttons", "editorbuttons");
      if (getFilesCount() == 1) {
        Node node = getContentNode(0);
        require.addScripts("editorbuttons.resetButtons();");
        // call plugins init handlers
        documentService.getDocumentEditorProviders().forEach(provider -> {
          try {
            if (provider.isAvailableForUser(identity)) {
              provider.initActivity(node.getUUID(), node.getSession().getWorkspace().getName(), activityId);
            }
          } catch (Exception e) {
            LOG.error("Cannot init activity from plugin {}, {}", provider.getProviderName(), e.getMessage());
          }
        });
        String preferredProvider = documentService.getPreferredEditor(identity.getUserId(),
                                                                     node.getUUID(),
                                                                     node.getSession().getWorkspace().getName());
        String currentProvider = documentService.getCurrentDocumentProvider(node.getUUID(),
                                                                            node.getSession().getWorkspace().getName());
        InitConfig config = new InitConfig.InitConfigBuilder().activityId(activityId)
                                                              .index("0")
                                                              .fileId(node.getUUID())
                                                              .workspace(node.getSession().getWorkspace().getName())
                                                              .preferredProvider(preferredProvider)
                                                              .currentProvider(currentProvider)
                                                              .build();
        require.addScripts("editorbuttons.initActivityButtons(" + config.toJSON() + ");");

      }
    }
    super.end();
  }

  /**
   * The Class InitConfig.
   */
  protected static class InitConfig {

    /** The activity id. */
    protected final String activityId;

    /** The index. */
    protected final String index;

    /** The file id. */
    protected final String fileId;

    /** The workspace. */
    protected final String workspace;

    /** The preferred provider. */
    protected final String preferredProvider;

    /** The current provider. */
    protected final String currentProvider;

    /**
     * Instantiates a new inits the config.
     *
     * @param builder the builder
     */
    private InitConfig(InitConfigBuilder builder) {
      this.activityId = builder.activityId;
      this.index = builder.index;
      this.fileId = builder.fileId;
      this.workspace = builder.workspace;
      this.preferredProvider = builder.preferredProvider;
      this.currentProvider = builder.currentProvider;
    }

    /**
     * Gets the activity id.
     *
     * @return the activity id
     */
    public String getActivityId() {
      return activityId;
    }

    /**
     * Gets the index.
     *
     * @return the index
     */
    public String getIndex() {
      return index;
    }

    /**
     * Gets the file id.
     *
     * @return the file id
     */
    public String getFileId() {
      return fileId;
    }

    /**
     * Gets the workspace.
     *
     * @return the workspace
     */
    public String getWorkspace() {
      return workspace;
    }

    /**
     * Gets the current provider.
     *
     * @return the current provider
     */
    public String getCurrentProvider() {
      return currentProvider;
    }

    /**
     * Gets the preferred provider.
     *
     * @return the preferred provider
     */
    public String getPreferredProvider() {
      return preferredProvider;
    }

    /**
     * To JSON.
     *
     * @return the string
     * @throws JsonException the json exception
     */
    public String toJSON() throws JsonException {
      JsonGeneratorImpl gen = new JsonGeneratorImpl();
      return gen.createJsonObject(this).toString();
    }

    /**
     * The Class InitConfigBuilder.
     */
    static class InitConfigBuilder {
      /** The activity id. */
      protected String activityId;

      /** The index. */
      protected String index;

      /** The file id. */
      protected String fileId;

      /** The workspace. */
      protected String workspace;

      /** The preferred provider. */
      protected String preferredProvider;

      /** The current editor. */
      protected String currentProvider;

      /**
       * Activity id.
       *
       * @param activityId the activity id
       * @return the inits the config builder
       */
      protected InitConfigBuilder activityId(String activityId) {
        this.activityId = activityId;
        return this;
      }

      /**
       * Index.
       *
       * @param index the index
       * @return the inits the config builder
       */
      protected InitConfigBuilder index(String index) {
        this.index = index;
        return this;
      }

      /**
       * File id.
       *
       * @param fileId the file id
       * @return the inits the config builder
       */
      protected InitConfigBuilder fileId(String fileId) {
        this.fileId = fileId;
        return this;
      }

      /**
       * Workspace.
       *
       * @param workspace the workspace
       * @return the inits the config builder
       */
      protected InitConfigBuilder workspace(String workspace) {
        this.workspace = workspace;
        return this;
      }

      /**
       * Preffered editor.
       *
       * @param preferredProvider the preferred provider
       * @return the inits the config builder
       */
      protected InitConfigBuilder preferredProvider(String preferredProvider) {
        this.preferredProvider = preferredProvider;
        return this;
      }

      /**
       * Current editor.
       *
       * @param currentProvider the current provider
       * @return the inits the config builder
       */
      protected InitConfigBuilder currentProvider(String currentProvider) {
        this.currentProvider = currentProvider;
        return this;
      }

      /**
       * Builds the InitConfig.
       *
       * @return the inits the config
       */
      protected InitConfig build() {
        return new InitConfig(this);
      }
    }
  }
  public String getLinkDescription() {
    return UILinkUtil.simpleEscapeHtml(org.exoplatform.social.service.rest.Util.getDecodeQueryURL(linkDescription));
  }
  public void setLinkDescription(String linkDescription) {
    this.linkDescription = linkDescription;
  }
  public String getLinkImage() {
    return linkImage;
  }
  public void setLinkImage(String linkImage) {
    this.linkImage = linkImage;
  }
  public String getLinkSource() {
    return UILinkUtil.simpleEscapeHtml(org.exoplatform.social.service.rest.Util.getDecodeQueryURL(linkSource));
  }
  public void setLinkSource(String linkSource) {
    this.linkSource = linkSource;
  }
  public String getLinkTitle() {
    return UILinkUtil.simpleEscapeHtml(org.exoplatform.social.service.rest.Util.getDecodeQueryURL(linkTitle));
  }
  public void setLinkTitle(String linkTitle) {
    this.linkTitle = linkTitle;
  }
  public String getEmbedHtml() {
    return embedHtml;
  }
  public void setEmbedHtml(String embedHtml) {
    this.embedHtml = embedHtml;
  }

  private CloudDriveService getCloudDrivesService() {
    if (cloudDrivesService == null) {
      cloudDrivesService = getApplicationComponent(CloudDriveService.class);
    }
    return cloudDrivesService;
  }

  public CloudDrive getCloudDrive(Node node) {
    CloudDrive cloudDrive = null;
    try {
      cloudDrive = getCloudDrivesService().findDrive(node);
    } catch (RepositoryException e) {
      LOG.warn("Exception while getting clouddrive", e);
    }
    return cloudDrive;
  }

  public boolean isCloudFile(CloudDrive cloudDrive) {
    boolean isCloudFile = false;

    try {
      if (cloudDrive != null && cloudDrive.isConnected()) {
        isCloudFile = true;
      }
    } catch (Exception e) {
      LOG.warn("Exception while checking node as cloudfile", e);
    }
    return isCloudFile;
  }

  public CloudFile getCloudFile(Node node) throws RepositoryException, DriveRemovedException {
    CloudDrive cloudDrive = getCloudDrive(node);
    CloudFile cloudFile = null;
    try {
      if (cloudDrive != null && cloudDrive.isConnected()) {
        if (!cloudDrive.isDrive(node)) {
          cloudFile = cloudDrive.getFile(node.getPath());
        }
      }
    } catch (Exception e) {
      LOG.warn("Exception while checking node as cloudfile", e);
    }
    return cloudFile;
  }

  public String getCloudFileIcon(CloudDrive cloudDrive) {
    String cloudFileIcon = "";
    String providerId = cloudDrive.getUser().getProvider().getId();
    switch (providerId) {
    case ONE_DRIVE_PROVIDER_ID:
      cloudFileIcon = ONE_DRIVE_ICON;
      break;
    case GOOGLE_DRIVE_PROVIDER_ID:
      cloudFileIcon = GOOGLE_DRIVE_ICON;
      break;
    }
    return cloudFileIcon;
  }

}
