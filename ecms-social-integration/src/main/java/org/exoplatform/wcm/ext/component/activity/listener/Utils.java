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
package org.exoplatform.wcm.ext.component.activity.listener;

import java.io.InputStream;
import java.net.URLEncoder;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

import javax.jcr.*;

import org.apache.commons.lang.StringUtils;

import org.exoplatform.commons.utils.CommonsUtils;
import org.exoplatform.commons.utils.ISO8601;
import org.exoplatform.container.*;
import org.exoplatform.container.xml.PortalContainerInfo;
import org.exoplatform.services.cms.BasePath;
import org.exoplatform.services.cms.documents.DocumentService;
import org.exoplatform.services.cms.jcrext.activity.ActivityCommonService;
import org.exoplatform.services.cms.link.LinkManager;
import org.exoplatform.services.cms.templates.TemplateService;
import org.exoplatform.services.context.DocumentContext;
import org.exoplatform.services.jcr.access.AccessControlEntry;
import org.exoplatform.services.jcr.core.ExtendedNode;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.jcr.ext.ActivityTypeUtils;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.jcr.ext.hierarchy.NodeHierarchyCreator;
import org.exoplatform.services.jcr.impl.core.NodeImpl;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.resources.ResourceBundleService;
import org.exoplatform.services.security.ConversationState;
import org.exoplatform.services.wcm.core.*;
import org.exoplatform.services.wcm.friendly.FriendlyService;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;
import org.exoplatform.services.wcm.webcontent.WebContentSchemaHandler;
import org.exoplatform.social.core.activity.model.ExoSocialActivity;
import org.exoplatform.social.core.activity.model.ExoSocialActivityImpl;
import org.exoplatform.social.core.identity.model.Identity;
import org.exoplatform.social.core.identity.model.Profile;
import org.exoplatform.social.core.identity.provider.OrganizationIdentityProvider;
import org.exoplatform.social.core.identity.provider.SpaceIdentityProvider;
import org.exoplatform.social.core.manager.ActivityManager;
import org.exoplatform.social.core.manager.IdentityManager;
import org.exoplatform.social.core.service.LinkProvider;
import org.exoplatform.social.core.space.SpaceUtils;
import org.exoplatform.social.core.space.model.Space;
import org.exoplatform.social.core.space.spi.SpaceService;
import org.exoplatform.wcm.ext.component.activity.ContentUIActivity;
import org.exoplatform.wcm.ext.component.activity.FileUIActivity;
import org.exoplatform.webui.application.WebuiRequestContext;
import org.exoplatform.webui.cssfile.*;



/**
 * Created by The eXo Platform SAS Author : eXoPlatform exo@exoplatform.com Mar
 * 18, 2011
 */
public class Utils {

  private static final Log    LOG                 = ExoLogger.getLogger(Utils.class);

  /** The Constant Activity Type */
  public static final String CONTENT_SPACES        = "contents:spaces";
  public static final String FILE_SPACES           = "files:spaces";
  public  static final String SHARE_FILE           = "sharefiles:spaces";
  public  static final String SHARE_CONTENT        = "sharecontents:spaces";

  /** the publication:currentState property name */
  private static final String CURRENT_STATE_PROP  = "publication:currentState";

  public static final String EXO_RESOURCES_URI            = "/eXoSkin/skin/images/themes/default/Icons/TypeIcons/EmailNotificationIcons/";
  public static final String ICON_FILE_EXTENSION          = ".png";
  public static final String DEFAULT_AVATAR          = "/eXoSkin/skin/images/themes/default/social/skin/ShareImages/UserAvtDefault.png";

  private static String MIX_COMMENT                = "exo:activityComment";
  private static String MIX_COMMENT_ID             = "exo:activityCommentID";
  private static int    MAX_SUMMARY_LINES_COUNT    = 4;
  private static int    MAX_SUMMARY_CHAR_COUNT     = 430;
  private static String activityType;
  private static final String RESOURCE_BUNDLE_KEY_CREATED_BY = "SocialIntegration.messages.createdBy";


  public static String getActivityType() {
    return activityType;
  }

  public static void setActivityType(String activityType) {
    Utils.activityType = activityType;
  }

  /**
   * Populate activity data with the data from Node
   * 
   * @param node the node
   * @param activityOwnerId the owner id of the activity
   * @param activityMsgBundleKey the message bundle key of the activity
   * @return Map the mapped data
   */
  public static Map<String, String> populateActivityData(Node node,
          String activityOwnerId,
          String activityMsgBundleKey) throws Exception {
	  return populateActivityData(node, activityOwnerId, activityMsgBundleKey, false, null, null);
  }
  public static Map<String, String> populateActivityData(Node node,
                                                         String activityOwnerId, String activityMsgBundleKey, 
                                                         boolean isComment, String systemComment, String perm) throws Exception {
    /** The date formatter. */
    DateFormat dateFormatter = null;
    dateFormatter = new SimpleDateFormat(ISO8601.SIMPLE_DATETIME_FORMAT);
    LinkManager linkManager = CommonsUtils.getService(LinkManager.class);

    if(node.canAddMixin(NodetypeConstant.MIX_REFERENCEABLE)){
      node.addMixin(NodetypeConstant.MIX_REFERENCEABLE);
      node.save();
    }
    // get activity data
    String repository = ((ManageableRepository) node.getSession().getRepository()).getConfiguration()
                                                                                  .getName();
    String workspace = node.getSession().getWorkspace().getName();
    
    String illustrationImg;
    try{
      illustrationImg = Utils.getIllustrativeImage(node);
    }catch(Exception ex){
      illustrationImg="";
    }
    String strDateCreated = "";
    if (node.hasProperty(NodetypeConstant.EXO_DATE_CREATED)) {
      Calendar dateCreated = node.getProperty(NodetypeConstant.EXO_DATE_CREATED).getDate();
      strDateCreated = dateFormatter.format(dateCreated.getTime());
    }
    String strLastModified = "";
    if (node.hasNode(NodetypeConstant.JCR_CONTENT)) {
      Node contentNode = node.getNode(NodetypeConstant.JCR_CONTENT);
      if (contentNode.hasProperty(NodetypeConstant.JCR_LAST_MODIFIED)) {
        Calendar lastModified = contentNode.getProperty(NodetypeConstant.JCR_LAST_MODIFIED)
                                           .getDate();
        strLastModified = dateFormatter.format(lastModified.getTime());
      }
    }

    activityOwnerId = activityOwnerId != null ? activityOwnerId : "";

    // populate data to map object
    Map<String, String> activityParams = new HashMap<String, String>();
    activityParams.put(ContentUIActivity.NODE_UUID, node.getUUID());
    activityParams.put(ContentUIActivity.CONTENT_NAME, node.getName());
    activityParams.put(ContentUIActivity.AUTHOR, activityOwnerId);
    activityParams.put(ContentUIActivity.DATE_CREATED, strDateCreated);
    activityParams.put(ContentUIActivity.LAST_MODIFIED, strLastModified);
    activityParams.put(ContentUIActivity.CONTENT_LINK, getContentLink(node));
    activityParams.put(ContentUIActivity.ID,
                       node.isNodeType(NodetypeConstant.MIX_REFERENCEABLE) ? node.getUUID() : "");
    activityParams.put(ContentUIActivity.REPOSITORY, repository);
    activityParams.put(ContentUIActivity.WORKSPACE, workspace);
    activityParams.put(ContentUIActivity.MESSAGE, activityMsgBundleKey);
    activityParams.put(ContentUIActivity.MIME_TYPE, getMimeType(linkManager.isLink(node)?linkManager.getTarget(node, true):node));
    activityParams.put(ContentUIActivity.IMAGE_PATH, illustrationImg);
    activityParams.put(ContentUIActivity.IMAGE_PATH, illustrationImg);
    if (isComment && StringUtils.isNotBlank(systemComment)) {
      activityParams.put(ContentUIActivity.IS_SYSTEM_COMMENT, String.valueOf(isComment));
    	activityParams.put(ContentUIActivity.SYSTEM_COMMENT, systemComment);
    }else{
      activityParams.put(ContentUIActivity.IS_SYSTEM_COMMENT, String.valueOf(false));
      activityParams.put(ContentUIActivity.SYSTEM_COMMENT, "");
    }
    activityParams.put(ContentUIActivity.PERMISSION, perm);
    activityParams.put(ContentUIActivity.COMMENT, systemComment);
    activityParams.put(ContentUIActivity.THUMBNAIL, getThumbnailUrl(node, repository, workspace) != null ? getThumbnailUrl(node, repository, workspace) : getDefaultThumbnailUrl(node));
    activityParams.put(ContentUIActivity.NODE_PATH, node.getPath());
    return activityParams;
  }

  private static String getDefaultThumbnailUrl(Node node) throws RepositoryException {
    LinkManager linkManager = CommonsUtils.getService(LinkManager.class);
    String cssClass = CssClassUtils.getCSSClassByFileNameAndFileType(
        node.getName(), getMimeType(linkManager.isLink(node)?linkManager.getTarget(node, true):node), CssClassManager.ICON_SIZE.ICON_64);

    if (cssClass.indexOf(CssClassIconFile.DEFAULT_CSS) > 0) {
      return CommonsUtils.getCurrentDomain() + EXO_RESOURCES_URI  + "uiIcon64x64Templatent_file.png";
    }
    return CommonsUtils.getCurrentDomain() + EXO_RESOURCES_URI + cssClass.split(" ")[0] + ICON_FILE_EXTENSION;
  }

  private static String getThumbnailUrl(Node node, String repository, String workspace) {
    try {
      LinkManager linkManager = CommonsUtils.getService(LinkManager.class);
      String mimeType = getMimeType(linkManager.isLink(node)?linkManager.getTarget(node, true):node);
      ExoContainer container = ExoContainerContext.getCurrentContainer();
      PortalContainerInfo containerInfo = (PortalContainerInfo) container.getComponentInstanceOfType(PortalContainerInfo.class);
      String portalName = containerInfo.getContainerName();

      String restContextName = org.exoplatform.ecm.webui.utils.Utils.getRestContextName(portalName);
      String preferenceWS = node.getSession().getWorkspace().getName();
      String encodedPath = URLEncoder.encode(node.getPath(), "utf-8");
      encodedPath = encodedPath.replaceAll ("%2F", "/");

      if (mimeType.startsWith("image")) {

        return CommonsUtils.getCurrentDomain() + "/" + portalName + "/" + restContextName + "/thumbnailImage/custom/300x300/" +
            repository + "/" + preferenceWS + encodedPath;
      }
      else if (mimeType.indexOf("icon") >=0) {
        return getWebdavURL(node, repository, workspace);
      }
      else if (org.exoplatform.services.cms.impl.Utils.isSupportThumbnailView(mimeType)) {
        return CommonsUtils.getCurrentDomain() + "/" + portalName + "/" + restContextName + "/thumbnailImage/big/" + repository + "/" + preferenceWS + encodedPath;
      } else {
        return null;
      }

    }
    catch (Exception e) {
      LOG.debug("Cannot get thumbnail url");
    }
    return StringUtils.EMPTY;
  }

  private static String getWebdavURL(Node contentNode, String repository, String workspace) throws Exception {
    FriendlyService friendlyService = CommonsUtils.getService(FriendlyService.class);
    String link = "#";

    String portalName = PortalContainer.getCurrentPortalContainerName();
    String restContextName = PortalContainer.getCurrentRestContextName();
    if (contentNode.isNodeType("nt:frozenNode")) {
      String uuid = contentNode.getProperty("jcr:frozenUuid").getString();
      Node originalNode = contentNode.getSession().getNodeByUUID(uuid);
      link = CommonsUtils.getCurrentDomain() + "/" + portalName + "/" + restContextName + "/jcr/" + repository + "/"
          + workspace + originalNode.getPath() + "?version=" + contentNode.getParent().getName();
    } else {
      link = CommonsUtils.getCurrentDomain() + "/" + portalName + "/" + restContextName + "/jcr/" + repository + "/"
          + workspace + contentNode.getPath();
    }

    return friendlyService.getFriendlyUri(link);
  }

  /**
   * see the postActivity(Node node, String activityMsgBundleKey, Boolean isSystemComment, String systemComment, String perm)
   */
  public static void postActivity(Node node, String activityMsgBundleKey) throws Exception {
    postActivity(node, activityMsgBundleKey, false, false, null, null);
  }

  public static ExoSocialActivity createShareActivity(Node node, String activityMsgBundleKey, String activityType, String comments, String perm) throws Exception{
    setActivityType(activityType);
    if(SHARE_FILE.equals(activityType)){
      return postFileActivity(node,activityMsgBundleKey,false,false,comments, perm);
    }else if(SHARE_CONTENT.equals(activityType)){
      return postActivity(node,activityMsgBundleKey,false,false,comments, perm);
    }else{
      setActivityType(null);
      return postFileActivity(node,activityMsgBundleKey,false,false,comments, perm);
    }
  }
  /**
   * see the postFileActivity(Node node, String activityMsgBundleKey, Boolean isSystemComment, String systemComment, String perm)
   */
  public static void postFileActivity(Node node, String activityMsgBundleKey) throws Exception {
    postFileActivity(node, activityMsgBundleKey, false, false, null, null);
  }

  /**
   * 
   * @param node : activity raised from this source
   * @param activityMsgBundleKey
   * @param needUpdate
   * @param isSystemComment
   * @param systemComment the new value of System Posted activity,
   *        if (isSystemComment) systemComment can not be set to null, set to empty string instead of.
   * @param perm the permission accorded for sharing file/content
   * @throws Exception
   */
  public static ExoSocialActivity postActivity(Node node, String activityMsgBundleKey, boolean needUpdate, 
                                  boolean isSystemComment, String systemComment, String perm) throws Exception {
    Object isSkipRaiseAct = DocumentContext.getCurrent()
                                           .getAttributes()
                                           .get(DocumentContext.IS_SKIP_RAISE_ACT);
    if (isSkipRaiseAct != null && Boolean.valueOf(isSkipRaiseAct.toString())) {
      return null;
    }
    ActivityManager activityManager = CommonsUtils.getService(ActivityManager.class);
    activityType = StringUtils.isNotEmpty(activityType) ? activityType : FILE_SPACES;
    if(! activityManager.isActivityTypeEnabled(activityType)) {
      return null;
    }
    // get services
    IdentityManager identityManager = CommonsUtils.getService(IdentityManager.class);
    ActivityCommonService activityCommonService = CommonsUtils.getService(ActivityCommonService.class);
    SpaceService spaceService = CommonsUtils.getService(SpaceService.class);

    // refine to get the valid node
    refineNode(node);

    // get owner
    String activityOwnerId = getActivityOwnerId(node);
    String nodeActivityID;
    ExoSocialActivity exa =null;
    if (node.isNodeType(ActivityTypeUtils.EXO_ACTIVITY_INFO)) {
      try {
        nodeActivityID = node.getProperty(ActivityTypeUtils.EXO_ACTIVITY_ID).getString();
        exa =  activityManager.getActivity(nodeActivityID);
      }catch (Exception e){
          LOG.info("No activity is deleted, return no related activity");
      }
    }
    ExoSocialActivity activity = null ;
    String commentID;
    boolean commentFlag = false;
    if (node.isNodeType(MIX_COMMENT) && node.hasProperty(MIX_COMMENT_ID) && activityCommonService.isEditing(node))
    {
      commentID = node.getProperty(MIX_COMMENT_ID).getString();
      if (StringUtils.isNotBlank(commentID)) activity = activityManager.getActivity(commentID);
      commentFlag = (activity != null);
    }
    if (activity==null) {
      activity = createActivity(identityManager, activityOwnerId,
                                node, activityMsgBundleKey, activityType, isSystemComment, systemComment, perm);
      setActivityType(null);
    }
    
    if (exa!=null) {
      if (commentFlag) {
        Map<String, String> paramsMap = activity.getTemplateParams();
        String paramMessage = paramsMap.get(ContentUIActivity.MESSAGE);
        String paramContent = paramsMap.get(ContentUIActivity.SYSTEM_COMMENT);
        if (!StringUtils.isEmpty(paramMessage)) {
          paramMessage += ActivityCommonService.VALUE_SEPERATOR + activityMsgBundleKey;
          if (StringUtils.isEmpty(systemComment)) {
            paramContent += ActivityCommonService.VALUE_SEPERATOR + " ";
          }else {
            paramContent += ActivityCommonService.VALUE_SEPERATOR + systemComment;
          }
        } else {
          paramMessage = activityMsgBundleKey;
          paramContent = systemComment;
        }
        paramsMap.put(ContentUIActivity.MESSAGE, paramMessage);
        paramsMap.put(ContentUIActivity.SYSTEM_COMMENT, paramContent);
        activity.setTemplateParams(paramsMap);
        updateNotifyMessages(activity, activityMsgBundleKey, systemComment);
        activityManager.updateActivity(activity);
      } else {
        updateNotifyMessages(activity, activity.getTemplateParams().get(ContentUIActivity.MESSAGE), activity.getTemplateParams().get(ContentUIActivity.SYSTEM_COMMENT));
        activityManager.saveComment(exa, activity);
        if (activityCommonService.isEditing(node)) {
          commentID = activity.getId();
          if (node.canAddMixin(MIX_COMMENT)) node.addMixin(MIX_COMMENT);
          if (node.isNodeType(MIX_COMMENT)) node.setProperty(MIX_COMMENT_ID, commentID);
        }
      }
      if (needUpdate) {
        updateMainActivity(activityManager, node, exa);
      }
      return activity;
    }else {
      String spaceGroupName = getSpaceName(node);
      Space space = spaceService.getSpaceByGroupId(SpaceUtils.SPACE_GROUP + "/" + spaceGroupName);
      if (spaceGroupName != null && spaceGroupName.length() > 0
          && space != null) {
        // post activity to space stream
        Identity spaceIdentity = identityManager.getOrCreateIdentity(SpaceIdentityProvider.NAME,
            space.getPrettyName());
        activityManager.saveActivityNoReturn(spaceIdentity, activity);
      } else if (activityOwnerId != null && activityOwnerId.length() > 0) {
        // post activity to user status stream
        Identity ownerIdentity = identityManager.getOrCreateIdentity(OrganizationIdentityProvider.NAME,
            activityOwnerId);
        activityManager.saveActivityNoReturn(ownerIdentity, activity);
      } else {
        return null;
      }
      if (!StringUtils.isEmpty(activity.getId())) {
        ActivityTypeUtils.attachActivityId(node, activity.getId());
      }
      updateMainActivity(activityManager, node, activity);

      if (node.isNodeType(ActivityTypeUtils.EXO_ACTIVITY_INFO)) {
        try {
          nodeActivityID = node.getProperty(ActivityTypeUtils.EXO_ACTIVITY_ID).getString();
          exa = activityManager.getActivity(nodeActivityID);
        } catch (Exception e) {
          LOG.info("No activity is deleted, return no related activity");
        }
        if (exa != null && !commentFlag && isSystemComment) {
          activityManager.saveComment(exa, activity);
          if (activityCommonService.isEditing(node)) {
            commentID = activity.getId();
            if (node.canAddMixin(MIX_COMMENT)) node.addMixin(MIX_COMMENT);
            if (node.isNodeType(MIX_COMMENT)) node.setProperty(MIX_COMMENT_ID, commentID);
          }
        }
      }

      return activity;
    }
  }
  
  /**
   * 
   * @param node : activity raised from this source
   * @param activityMsgBundleKey
   * @param isComment
   * @param systemComment the new value of System Posted activity, 
   *        if (isSystemComment) systemComment can not be set to null, set to empty string instead of.
   * @throws Exception
   */
  public static ExoSocialActivity postFileActivity(Node node, String activityMsgBundleKey, boolean needUpdate, 
                                  boolean isComment, String systemComment, String perm) throws Exception {
    Object isSkipRaiseAct = DocumentContext.getCurrent()
                                           .getAttributes()
                                           .get(DocumentContext.IS_SKIP_RAISE_ACT);
    if (isSkipRaiseAct != null && Boolean.valueOf(isSkipRaiseAct.toString())) {
      return null;
    }
    if (RESOURCE_BUNDLE_KEY_CREATED_BY.equals(activityMsgBundleKey)  && !isSystemComment) {
      return null;
    }
    ActivityManager activityManager = CommonsUtils.getService(ActivityManager.class);
    activityType = StringUtils.isNotEmpty(activityType) ? activityType : FILE_SPACES;
    if(! activityManager.isActivityTypeEnabled(activityType)) {
      return null;
    }
    // get services
    IdentityManager identityManager = CommonsUtils.getService(IdentityManager.class);
    ActivityCommonService activityCommonService = CommonsUtils.getService(ActivityCommonService.class);
    SpaceService spaceService = CommonsUtils.getService(SpaceService.class);

    // refine to get the valid node
    refineNode(node);

    // get owner
    String activityOwnerId = getActivityOwnerId(node);
    String nodeActivityID;
    ExoSocialActivity exa =null;
    if (node.isNodeType(ActivityTypeUtils.EXO_ACTIVITY_INFO)) {
      try {
        nodeActivityID = node.getProperty(ActivityTypeUtils.EXO_ACTIVITY_ID).getString();
        exa =  activityManager.getActivity(nodeActivityID);
      }catch (Exception e){
          LOG.info("No activity is deleted, return no related activity");
      }
    }
    ExoSocialActivity activity = null ;
    String commentID;
    boolean commentFlag = false;
    if (node.isNodeType(MIX_COMMENT) && activityCommonService.isEditing(node)) {
      if (node.hasProperty(MIX_COMMENT_ID)) {
        commentID = node.getProperty(MIX_COMMENT_ID).getString();
        if (StringUtils.isNotBlank(commentID)) activity = activityManager.getActivity(commentID);
        commentFlag = (activity != null);
      }
    }
    if (activity==null) {
      activity = createActivity(identityManager, activityOwnerId,
                                node, activityMsgBundleKey, activityType, isComment, systemComment, perm);
      setActivityType(null);
    }
    
    if (exa != null) {
      if (commentFlag) {
        Map<String, String> paramsMap = activity.getTemplateParams();
        String paramMessage = paramsMap.get(ContentUIActivity.MESSAGE);
        String paramContent = paramsMap.get(ContentUIActivity.SYSTEM_COMMENT);
        if (!StringUtils.isEmpty(paramMessage)) {
          paramMessage += ActivityCommonService.VALUE_SEPERATOR + activityMsgBundleKey;
          if (StringUtils.isEmpty(systemComment)) {
            paramContent += ActivityCommonService.VALUE_SEPERATOR + " ";
          }else {
            paramContent += ActivityCommonService.VALUE_SEPERATOR + systemComment;
          }
        } else {
          paramMessage = activityMsgBundleKey;
          paramContent = systemComment;
        }              
        paramsMap.put(ContentUIActivity.MESSAGE, paramMessage);
        paramsMap.put(ContentUIActivity.SYSTEM_COMMENT, paramContent);
        activity.setTemplateParams(paramsMap);
        updateNotifyMessages(activity, activityMsgBundleKey, systemComment);
        activityManager.updateActivity(activity);
      } else {
        updateNotifyMessages(activity, activity.getTemplateParams().get(ContentUIActivity.MESSAGE), activity.getTemplateParams().get(ContentUIActivity.SYSTEM_COMMENT));
        activityManager.saveComment(exa, activity);
        if (activityCommonService.isEditing(node)) {
          commentID = activity.getId();
          if (node.canAddMixin(MIX_COMMENT)) node.addMixin(MIX_COMMENT);
          if (node.isNodeType(MIX_COMMENT)) node.setProperty(MIX_COMMENT_ID, commentID);
        }
      }      
      return activity;
    }else {
      String spaceGroupName = getSpaceName(node);
      Space space = spaceService.getSpaceByGroupId(SpaceUtils.SPACE_GROUP + "/" + spaceGroupName);
      if (spaceGroupName != null && spaceGroupName.length() > 0
          && space != null) {
        // post activity to space stream
        Identity spaceIdentity = identityManager.getOrCreateIdentity(SpaceIdentityProvider.NAME,
            space.getPrettyName());
        activityManager.saveActivityNoReturn(spaceIdentity, activity);
      } else if (activityOwnerId != null && activityOwnerId.length() > 0) {
        if (!isPublic(node)) {
          // only post activity to user status stream if that upload is public
          return null;
        }
        // post activity to user status stream
        Identity ownerIdentity = identityManager.getOrCreateIdentity(OrganizationIdentityProvider.NAME,
            activityOwnerId);
        activityManager.saveActivityNoReturn(ownerIdentity, activity);
      } else {
        return null;
      }
      if (!StringUtils.isEmpty(activity.getId())) {
        ActivityTypeUtils.attachActivityId(node, activity.getId());
      }

      if (node.isNodeType(ActivityTypeUtils.EXO_ACTIVITY_INFO)) {
        try {
          nodeActivityID = node.getProperty(ActivityTypeUtils.EXO_ACTIVITY_ID).getString();
          exa = activityManager.getActivity(nodeActivityID);
        } catch (Exception e) {
          LOG.info("No activity is deleted, return no related activity");
        }
        if (exa != null && !commentFlag && isComment) {
          activity.setId(null);
          updateNotifyMessages(activity, activity.getTemplateParams().get(ContentUIActivity.MESSAGE), activity.getTemplateParams().get(ContentUIActivity.SYSTEM_COMMENT));
          activityManager.saveComment(exa, activity);
          if (activityCommonService.isEditing(node)) {
            commentID = activity.getId();
            if (node.canAddMixin(MIX_COMMENT)) node.addMixin(MIX_COMMENT);
            if (node.isNodeType(MIX_COMMENT)) node.setProperty(MIX_COMMENT_ID, commentID);
          }
        }
      }

      return activity;
    }
  }
  
  public static void updateNotifyMessages(ExoSocialActivity activity, String activityMsgBundleKey, String systemComment)
      throws Exception {     
    Locale locale = new Locale("en");
    ResourceBundleService resourceBundleService = CommonsUtils.getService(ResourceBundleService.class);
    ResourceBundle res = resourceBundleService.getResourceBundle("locale.extension.SocialIntegration", locale);
    StringBuffer sb = new StringBuffer();
    String[] keys = activityMsgBundleKey.split(ActivityCommonService.VALUE_SEPERATOR);
    String[] values = systemComment.split(ActivityCommonService.VALUE_SEPERATOR);
    String message;
    for (String key : keys) {
      try {
        message = res.getString(key);
      } catch(MissingResourceException mre) {
        message = key;
      }
      if(values.length > 0) {
        for(int i = 0; i < values.length; i++) {
          message = message.replace("{"+i+"}", values[i]);
        }
      }
      sb.append(message).append("\n");
    }
    activity.setTitle(sb.toString());
  }
  
  
  private static void updateMainActivity(ActivityManager activityManager, Node contentNode, ExoSocialActivity activity) {
    Map<String, String> activityParams = activity.getTemplateParams();
    String state;
    String nodeTitle;
    String nodeType = null;
    String documentTypeLabel;
    String currentVersion = null;
    TemplateService templateService = CommonsUtils.getService(TemplateService.class);
    try {
      nodeType = contentNode.getPrimaryNodeType().getName();
      documentTypeLabel = templateService.getTemplateLabel(nodeType);
    }catch (Exception e) {
      documentTypeLabel = "";
    }
    try {
      nodeTitle = org.exoplatform.ecm.webui.utils.Utils.getTitle(contentNode);
    } catch (Exception e1) {
      nodeTitle ="";
    }
    try {
      state = contentNode.hasProperty(CURRENT_STATE_PROP) ? contentNode.getProperty(CURRENT_STATE_PROP)
          .getValue()
          .getString() : "";
    } catch (Exception e) {
      state="";
    }
    try {
      currentVersion = contentNode.getBaseVersion().getName();
      
      //TODO Must improve this hardcode later, need specification
      if (currentVersion.contains("jcr:rootVersion")) currentVersion = "0";
    }catch (Exception e) {
      currentVersion ="";
    }
    activityParams.put(ContentUIActivity.STATE, state);
    activityParams.put(ContentUIActivity.DOCUMENT_TYPE_LABEL, documentTypeLabel);
    activityParams.put(ContentUIActivity.DOCUMENT_TITLE, nodeTitle);
    activityParams.put(ContentUIActivity.DOCUMENT_VERSION, currentVersion);
    String summary = getSummary(contentNode);
    summary =getFirstSummaryLines(summary, MAX_SUMMARY_LINES_COUNT);
    activityParams.put(ContentUIActivity.DOCUMENT_SUMMARY, summary);
    activity.setTemplateParams(activityParams);
    activityManager.updateActivity(activity);
  }
  /**
   * check the nodes that we support to post activities
   * 
   * @param node for checking
   * @return result of checking
   * @throws RepositoryException
   */
  private static boolean isSupportedContent(Node node) throws Exception {
    if (getActivityOwnerId(node) != null && getActivityOwnerId(node).length() > 0) {
      NodeHierarchyCreator nodeHierarchyCreator = (NodeHierarchyCreator) ExoContainerContext.getCurrentContainer()
                                                                                            .getComponentInstanceOfType(NodeHierarchyCreator.class);
      SessionProvider sessionProvider = WCMCoreUtils.getUserSessionProvider();
      if(sessionProvider == null){
    	  sessionProvider = WCMCoreUtils.getSystemSessionProvider();
      }
      Node userNode = nodeHierarchyCreator.getUserNode(sessionProvider, getActivityOwnerId(node));
      if (userNode != null && node.getPath().startsWith(userNode.getPath() + "/Private/")) {
        return false;
      }
    }

    return true;
  }

  /**
   * refine node for validation
   * 
   * @param currentNode
   * @throws Exception
   */
  private static void refineNode(Node currentNode) throws Exception {
    if (currentNode instanceof NodeImpl && !((NodeImpl) currentNode).isValid()) {
      ExoContainer container = ExoContainerContext.getCurrentContainer();
      LinkManager linkManager = (LinkManager) container.getComponentInstanceOfType(LinkManager.class);
      if (linkManager.isLink(currentNode)) {
        try {
          currentNode = linkManager.getTarget(currentNode, false);
        } catch (RepositoryException ex) {
          currentNode = linkManager.getTarget(currentNode, true);
        }
      }
    }
  }

  /**
   * get activity owner
   * 
   * @return activity owner
   */
  private static String getActivityOwnerId(Node node) {
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
   * get the space name of node
   * 
   * @param node
   * @return the group name
   * @throws Exception
   */
  private static String getSpaceName(Node node) throws Exception {
    NodeHierarchyCreator nodeHierarchyCreator = (NodeHierarchyCreator) ExoContainerContext.getCurrentContainer()
                                                                                          .getComponentInstanceOfType(NodeHierarchyCreator.class);
    String groupPath = nodeHierarchyCreator.getJcrPath(BasePath.CMS_GROUPS_PATH);
    String spacesFolder = groupPath + "/spaces/";
    String spaceName = "";
    String nodePath = node.getPath();
    if (nodePath.startsWith(spacesFolder)) {
      spaceName = nodePath.substring(spacesFolder.length());
      spaceName = spaceName.substring(0, spaceName.indexOf("/"));
    }

    return spaceName;
  }

  private static boolean isPublic(Node node) {
    if (node instanceof ExtendedNode) {
      ExtendedNode n = (ExtendedNode)node;
      try {
        List<String> permissions =n.getACL().getPermissions("any");
        if(permissions != null && permissions.size() > 0) {
          for (String p : permissions) {
            if ("read".equalsIgnoreCase(p)) {
              return true;
            }
          }
        }
      } catch (RepositoryException ex) {
        return false;
      }
    }
    return false;
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
    return documentService.getShortLinkInDocumentsApp(node.getSession().getWorkspace().getName(), ((NodeImpl)node).getInternalIdentifier());
  }

  /**
   * Create ExoSocialActivity
   * 
   * @param identityManager the identity Manager
   * @param activityOwnerId the remote user name
   * @param node the node
   * @param activityMsgBundleKey the message bundle key
   * @param activityType the activity type
   * @return the ExoSocialActivity
   * @throws Exception the activity storage exception
   */
  public static ExoSocialActivity createActivity(IdentityManager identityManager,
                                                 String activityOwnerId, Node node,
                                                 String activityMsgBundleKey, String activityType) throws Exception {
	  return createActivity(identityManager, activityOwnerId, node, activityMsgBundleKey, activityType, false, null, null);
  }
  public static ExoSocialActivity createActivity(IdentityManager identityManager,
                                                 String activityOwnerId,
                                                 Node node, String activityMsgBundleKey, String activityType,
                                                 boolean isSystemComment,  String systemComment, String perm) throws Exception {
		// Populate activity data
	Map<String, String> activityParams = populateActivityData(node, activityOwnerId, activityMsgBundleKey, isSystemComment, systemComment, perm);
    ExoSocialActivity activity = new ExoSocialActivityImpl();
    String userId = "";
    if(ConversationState.getCurrent() != null)
    {
      userId = ConversationState.getCurrent().getIdentity().getUserId();
    }else{
      userId = activityOwnerId;
    }
    Identity identity = identityManager.getOrCreateIdentity(OrganizationIdentityProvider.NAME, 
      userId, false);
    activity.setUserId(identity.getId());
    activity.setType(activityType);
    activity.setUrl(node.getPath());
    if(StringUtils.isNotEmpty(activityMsgBundleKey) && StringUtils.isNotEmpty(systemComment)) {
      updateNotifyMessages(activity, activityMsgBundleKey, systemComment);
    } else if(StringUtils.isNotEmpty(systemComment)){
        activity.setTitle(systemComment);
    } else {
        activity.setTitle("");
    }
    activity.setTemplateParams(activityParams);
    return activity;
  }
  
  public static void deleteFileActivity(Node node) throws RepositoryException {
    // get services
    ExoContainer container = ExoContainerContext.getCurrentContainer();
    ActivityManager activityManager = (ActivityManager) container.getComponentInstanceOfType(ActivityManager.class);
    
    // get owner
    String nodeActivityID = StringUtils.EMPTY;
    if (node.isNodeType(ActivityTypeUtils.EXO_ACTIVITY_INFO)) {
      try {
        nodeActivityID = node.getProperty(ActivityTypeUtils.EXO_ACTIVITY_ID).getString();
        if(activityManager.getActivity(nodeActivityID) != null) {
          activityManager.deleteActivity(nodeActivityID);
        }
      } catch (Exception e) {
        LOG.info("No activity is deleted, return no related activity");
      }
    }    
  }

  /**
   * Gets the illustrative image.
   * 
   * @param node the node
   * @return the illustrative image
   */
  public static String getIllustrativeImage(Node node) {
    WebSchemaConfigService schemaConfigService = CommonsUtils.getService(WebSchemaConfigService.class);
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
  
  public static String getSummary(Node node) {
    String desc = "";
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
  public static String getFirstSummaryLines(String source) {
    return getFirstSummaryLines(source, MAX_SUMMARY_LINES_COUNT);
  }
  
  
  private static String convertActivityContent(String source){
    String result =  source;
    result = result.replaceAll("(?i)<head>.*</head>", "");
    result = result.replaceAll("(?i)<script.*>.*</script>", "");
    result = result.replaceAll("(?i)<style.*>.*</style>", "");
    result = result.replaceAll("<([a-zA-Z\"]+) *[^/]*?>", "");
    result = result.replaceAll("</p>", "<br>");
    result = result.replaceAll("</([a-zA-Z]+) *[^/]*?>", "");
    result = result.replaceAll("([\r\n\t])+", "");
    result = result.replaceAll("^(<br>)", "");
    result = result.replaceAll("(<br>[ \r\t\n]+<br>)", "\n");
    result = result.replaceAll("(<br>)+", "\n");
    return result;
  }
  
  /**
   * 
   * @param source
   * @param linesCount
   * @return first {@code linesCount} without HTML tag
   */
  public static String getFirstSummaryLines(String source, int linesCount) {
    String result =  convertActivityContent(source);
    int i = 0;
    int index = -1;
    while (true) {
      index = result.indexOf("\n", index+1);
      if (index<0) break;
      i++;
      if (i>=linesCount) break;
    }
    if (index <0) {
      if (result.length()>MAX_SUMMARY_CHAR_COUNT)
      return  result.substring(0, MAX_SUMMARY_CHAR_COUNT-1) + "...";
      return result;
    }
    if (index>MAX_SUMMARY_CHAR_COUNT) index = MAX_SUMMARY_CHAR_COUNT-1;
    result = result.substring(0, index) + "\n...";
    return result;
  }

  public static String[] getSystemCommentTitle(Map<String, String> activityParams) {
    String[] result;
    if (activityParams == null) return null;
    String commentValue = activityParams.get(FileUIActivity.SYSTEM_COMMENT);
    if (!StringUtils.isEmpty(commentValue)) {
      if (commentValue.indexOf(ActivityCommonService.VALUE_SEPERATOR) >= 0) {
        result = commentValue.split(ActivityCommonService.VALUE_SEPERATOR);
        return result;
      } else {
        return new String[]{commentValue};
      }
    }
    return null;
  }

  public static String[] getSystemCommentBundle(Map<String, String> activityParams) {
    String[] result;
    if (activityParams == null) return null;
    String tmp = activityParams.get(FileUIActivity.IS_SYSTEM_COMMENT);
    String commentMessage;
    if (tmp == null) return null;
    try {
      if (Boolean.parseBoolean(tmp)) {
        commentMessage = activityParams.get(FileUIActivity.MESSAGE);
        if (!StringUtils.isEmpty(commentMessage)) {
          if (commentMessage.indexOf(ActivityCommonService.VALUE_SEPERATOR) >= 0) {
            result = commentMessage.split(ActivityCommonService.VALUE_SEPERATOR);
            return result;
          } else {
            return new String[]{commentMessage};
          }
        }
      }
    } catch (Exception e) {
      return null;
    }
    return null;
  }

  public static String getBundleValue(String key) {
    try {
      WebuiRequestContext context = WebuiRequestContext.getCurrentInstance();
      ResourceBundle res = context.getApplicationResourceBundle();
      String value = res.getString(key);
      return value;
    } catch (MissingResourceException e) {
      return key;
    }
  }

  public static String processMentions(String comment) {
    String excerpts[] = comment.split("@");
    comment = excerpts[0];
    String mentioned = "";
    for (int i=1; i<excerpts.length; i++) {
      String name = excerpts[i].split(" ")[0];
      Identity identity = org.exoplatform.social.notification.Utils.getIdentityManager().getOrCreateIdentity(OrganizationIdentityProvider.NAME, name, true);
      if (identity != null) {
        mentioned = addMentioned(name, identity.getProfile().getFullName());
      }
      if (mentioned.isEmpty()) {
        if (excerpts[i].isEmpty()) comment = comment + " ";
        else comment = comment + excerpts[i] + " ";
      } else {
        comment = comment + mentioned + excerpts[i].substring(name.length(),excerpts[i].length());
        mentioned = "";
      }
    }
    return comment;
  }

  private static String addMentioned(String mention, String fullname) {
    String profileURL = CommonsUtils.getCurrentDomain() + LinkProvider.getProfileUri(mention);
    return "<a href=" + profileURL + " type=\"mentionedUser\" rel=\"nofollow\">" + fullname + "</a>";
  }

  public static void setAvatarUrl(Node commentNode) throws RepositoryException {
    String name = commentNode.getProperty("exo:commentor").getString();;
    Identity identity = org.exoplatform.social.notification.Utils.getIdentityManager().getOrCreateIdentity(OrganizationIdentityProvider.NAME, name, true);
    Profile profile = identity.getProfile();
    if (profile.getAvatarUrl() != null ) {
      commentNode.setProperty("exo:commentorAvatar", profile.getAvatarUrl());
    } else {
      commentNode.setProperty("exo:commentorAvatar", DEFAULT_AVATAR);
    } ;
  }
}
