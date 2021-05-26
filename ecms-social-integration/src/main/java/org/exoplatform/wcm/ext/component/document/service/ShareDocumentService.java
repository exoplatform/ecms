/*
 * Copyright (C) 2003-2021 eXo Platform SAS.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.exoplatform.wcm.ext.component.document.service;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.commons.lang.StringUtils;
import org.picocontainer.Startable;

import org.exoplatform.commons.utils.ISO8601;
import org.exoplatform.ecm.utils.permission.PermissionUtil;
import org.exoplatform.services.cms.BasePath;
import org.exoplatform.services.cms.link.LinkManager;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.access.PermissionType;
import org.exoplatform.services.jcr.core.ExtendedNode;
import org.exoplatform.services.jcr.core.ExtendedSession;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.jcr.ext.app.SessionProviderService;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.jcr.ext.hierarchy.NodeHierarchyCreator;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.security.ConversationState;
import org.exoplatform.services.wcm.core.NodetypeConstant;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;
import org.exoplatform.social.core.activity.model.ExoSocialActivity;
import org.exoplatform.social.core.activity.model.ExoSocialActivityImpl;
import org.exoplatform.social.core.identity.model.Identity;
import org.exoplatform.social.core.identity.provider.OrganizationIdentityProvider;
import org.exoplatform.social.core.identity.provider.SpaceIdentityProvider;
import org.exoplatform.social.core.manager.ActivityManager;
import org.exoplatform.social.core.manager.IdentityManager;
import org.exoplatform.social.core.space.SpaceUtils;
import org.exoplatform.social.core.space.model.Space;
import org.exoplatform.social.core.space.spi.SpaceService;
import org.exoplatform.social.plugin.doc.UIDocActivity;
import org.exoplatform.social.webui.activity.UILinkActivity;
import org.exoplatform.wcm.ext.component.activity.FileUIActivity;
import org.exoplatform.wcm.ext.component.activity.SharedFileUIActivity;
import org.exoplatform.wcm.ext.component.activity.listener.Utils;


/**
 * Created by The eXo Platform SAS
 * Author : eXoPlatform
 *          exo@exoplatform.com
 * Nov 19, 2014  
 */
public class ShareDocumentService implements IShareDocumentService, Startable{
  private static final Log       LOG               = ExoLogger.getLogger(ShareDocumentService.class);

  public static final String     MIX_PRIVILEGEABLE = "exo:privilegeable";

  private static final boolean   POST_ACTIVITY     = true;
  
  private SessionProviderService sessionProviderService;

  private LinkManager            linkManager;

  private SpaceService           spaceService;

  private RepositoryService      repoService;

  private ActivityManager        activityManager;

  private IdentityManager        identityManager;

  private static final String   TEMPLATE_PARAMS_SEPARATOR = "|@|";

  public ShareDocumentService(RepositoryService repositoryService,
                              LinkManager linkManager,
                              IdentityManager identityManager,
                              ActivityManager activityManager,
                              SpaceService spaceService,
                              SessionProviderService sessionProviderService){
    this.repoService = repositoryService;
    this.sessionProviderService = sessionProviderService;
    this.linkManager = linkManager;
    this.spaceService = spaceService;
    this.activityManager = activityManager;
    this.identityManager = identityManager;
  }

  /*
   * {@inheritDoc}
   */
  @Override
  public String publishDocumentToSpace(String space, Node currentNode, String comment, String perm) {
    return publishDocumentToSpace(space, currentNode, comment, perm, POST_ACTIVITY);
  }

  /*
   * {@inheritDoc}
   */
  @Override
  public String publishDocumentToSpace(String space, Node currentNode, String comment, String perm, Boolean postActivity) {
    Node rootSpace = null;
    Node shared = null;
    try {
      SessionProvider sessionProvider = sessionProviderService.getSystemSessionProvider(null);
      ManageableRepository repository = repoService.getCurrentRepository();
      Session session = sessionProvider.getSession(repository.getConfiguration().getDefaultWorkspaceName(), repository);
      // add symlink to destination space
      NodeHierarchyCreator nodeCreator = WCMCoreUtils.getService(NodeHierarchyCreator.class);
      nodeCreator.getJcrPath(BasePath.CMS_GROUPS_PATH);

      rootSpace = (Node) session.getItem(nodeCreator.getJcrPath(BasePath.CMS_GROUPS_PATH) + space);
      rootSpace = rootSpace.getNode("Documents");
      if (!rootSpace.hasNode("Shared")) {
        shared = rootSpace.addNode("Shared");
      } else {
        shared = rootSpace.getNode("Shared");
      }
      if (currentNode.isNodeType(NodetypeConstant.EXO_SYMLINK))
        currentNode = linkManager.getTarget(currentNode);
      // Update permission
      String tempPerms = perm.toString();// Avoid ref back to UIFormSelectBox options
      if (!tempPerms.equals(PermissionType.READ))
        tempPerms = PermissionType.READ + "," + PermissionType.ADD_NODE + "," + PermissionType.SET_PROPERTY + ","
            + PermissionType.REMOVE;
      if (PermissionUtil.canChangePermission(currentNode)) {
        setSpacePermission(currentNode, space, tempPerms.split(","));
      } else if (PermissionUtil.canRead(currentNode)) {
        SessionProvider systemSessionProvider = SessionProvider.createSystemProvider();
        Session systemSession = systemSessionProvider.getSession(session.getWorkspace().getName(), repository);
        Node _node = (Node) systemSession.getItem(currentNode.getPath());
        setSpacePermission(_node, space, tempPerms.split(","));
      }
      currentNode.getSession().save();
      Node link = linkManager.createLink(shared, currentNode);
      String nodeMimeType = Utils.getMimeType(currentNode);
      link.addMixin(NodetypeConstant.MIX_FILE_TYPE);
      link.setProperty(NodetypeConstant.EXO_FILE_TYPE, nodeMimeType);
      rootSpace.save();
      // Share activity
      if (postActivity) {
        try {
          ExoSocialActivity activity = null;
          if (currentNode.getPrimaryNodeType().getName().equals(NodetypeConstant.NT_FILE)) {
            activity = Utils.createShareActivity(link, "", Utils.SHARE_FILE, comment, perm);
          } else {
            activity = Utils.createShareActivity(link, "", Utils.SHARE_CONTENT, comment, perm);
          }
          link.save();
          return activity.getId();
        } catch (Exception e1) {
          if (LOG.isErrorEnabled())
            LOG.error(e1.getMessage(), e1);
        }
      }
      if(link.canAddMixin(NodetypeConstant.MIX_REFERENCEABLE)){
        link.addMixin(NodetypeConstant.MIX_REFERENCEABLE);
        link.save();
        return link.getUUID();
      }
    } catch (RepositoryException e) {
      if (LOG.isErrorEnabled())
        LOG.error(e.getMessage(), e);
    } catch (Exception e) {
      if (LOG.isErrorEnabled())
        LOG.error(e.getMessage(), e);
    }
    return "";
  }

  /* (non-Javadoc)
   * @see org.exoplatform.ecm.webui.component.explorer.popup.service.IShareDocumentService#publishDocumentToUser(java.lang.String, javax.jcr.Node, java.lang.String, java.lang.String)
   */
  @Override
  public void publishDocumentToUser(String user, Node currentNode, String comment,String perm) {
    Node userPrivateNode = null;
    Node shared = null;
    try {
      SessionProvider sessionProvider = sessionProviderService.getSystemSessionProvider(null);
      ManageableRepository repository = repoService.getCurrentRepository();
      Session session = sessionProvider.getSession(repository.getConfiguration().getDefaultWorkspaceName(), repository);
      //add symlink to destination user
      userPrivateNode = getPrivateUserNode(sessionProvider, user);
      userPrivateNode = userPrivateNode.getNode("Documents");
      if(!userPrivateNode.hasNode("Shared")){
        shared = userPrivateNode.addNode("Shared");
      }else{
        shared = userPrivateNode.getNode("Shared");
      }
      if(currentNode.isNodeType(NodetypeConstant.EXO_SYMLINK)) currentNode = linkManager.getTarget(currentNode);
      //Update permission
      String tempPerms = perm.toString();//Avoid ref back to UIFormSelectBox options
      if(!tempPerms.equals(PermissionType.READ)) tempPerms = PermissionType.READ+","+PermissionType.ADD_NODE+","+PermissionType.SET_PROPERTY+","+PermissionType.REMOVE;
      if(PermissionUtil.canChangePermission(currentNode)){
        setUserPermission(currentNode, user, tempPerms.split(","));
      }else if(PermissionUtil.canRead(currentNode)){
        SessionProvider systemSessionProvider = SessionProvider.createSystemProvider();
        Session systemSession = systemSessionProvider.getSession(session.getWorkspace().getName(), repository);
        Node _node= (Node)systemSession.getItem(currentNode.getPath());
        setUserPermission(_node, user, tempPerms.split(","));
      }
      currentNode.getSession().save();
      Node link = linkManager.createLink(shared, currentNode);
      String nodeMimeType = Utils.getMimeType(currentNode);
      link.addMixin(NodetypeConstant.MIX_FILE_TYPE);
      link.setProperty(NodetypeConstant.EXO_FILE_TYPE, nodeMimeType);
      userPrivateNode.save();
    } catch (RepositoryException e) {
      if(LOG.isErrorEnabled())
        LOG.error(e.getMessage(), e);
    } catch (Exception e) {
      if(LOG.isErrorEnabled())
        LOG.error(e.getMessage(), e);
    }
  }

  private Node getPrivateUserNode(SessionProvider sessionProvider, String user) throws Exception,
                                                                                PathNotFoundException,
                                                                                RepositoryException {
    NodeHierarchyCreator nodeCreator = WCMCoreUtils.getService(NodeHierarchyCreator.class);
    String privateRelativePath = nodeCreator.getJcrPath("userPrivate");
    Node userNode = nodeCreator.getUserNode(sessionProvider, user);
    return userNode.getNode(privateRelativePath);
  }

  /* (non-Javadoc)
   * @see org.exoplatform.ecm.webui.component.explorer.popup.service.IShareDocumentService#unpublishDocumentToUser(java.lang.String, javax.jcr.ExtendedNode)
   */
  @Override
  public void unpublishDocumentToUser(String user, ExtendedNode node) {
    Node userPrivateNode = null;
    Node sharedNode = null;
    try {
      SessionProvider sessionProvider = sessionProviderService.getSystemSessionProvider(null);
      //remove symlink from destination user
      userPrivateNode = getPrivateUserNode(sessionProvider, user);
      userPrivateNode = userPrivateNode.getNode("Documents");
      sharedNode = userPrivateNode.getNode("Shared");
      sharedNode.getNode(node.getName()).remove();

      removeUserPermission(node, user);

      node.getSession().save();
      userPrivateNode.save();

      }  catch (RepositoryException e) {
      if (LOG.isErrorEnabled())
        LOG.error(e.getMessage(), e);
    } catch (Exception e) {
      if(LOG.isErrorEnabled())
        LOG.error(e.getMessage(), e);
    }
  }

  /* (non-Javadoc)
   * @see org.exoplatform.ecm.webui.component.explorer.popup.service.IShareDocumentService#unpublishDocumentToSpace(java.lang.String, javax.jcr.ExtendedNode)
   */
  @Override
  public void unpublishDocumentToSpace(String space, ExtendedNode node) {
    Node rootSpace = null;
    Node sharedNode = null;
    try {
      SessionProvider sessionProvider = sessionProviderService.getSystemSessionProvider(null);
      ManageableRepository repository = repoService.getCurrentRepository();
      Session session = sessionProvider.getSession(repository.getConfiguration().getDefaultWorkspaceName(), repository);
      //remove symlink to destination space
      NodeHierarchyCreator nodeCreator = WCMCoreUtils.getService(NodeHierarchyCreator.class);

      rootSpace = (Node) session.getItem(nodeCreator.getJcrPath(BasePath.CMS_GROUPS_PATH) + space);
      rootSpace = rootSpace.getNode("Documents");
      if (rootSpace.hasNode("Shared")) {
        sharedNode = rootSpace.getNode("Shared");
        sharedNode.getNode(node.getName()).remove();
        rootSpace.save();
      }

      removeSpacePermission(node, space);
      node.getSession().save();
    } catch (RepositoryException e) {
      if(LOG.isErrorEnabled())
        LOG.error(e.getMessage(), e);
    } catch (Exception e) {
      if(LOG.isErrorEnabled())
        LOG.error(e.getMessage(), e);
    }
  }
  

  /* (non-Javadoc)
   * @see org.exoplatform.wcm.ext.component.document.service.IShareDocumentService#shareDocumentActivityToSpace(java.lang.String, java.lang.String)
   */
  @Override
  public ExoSocialActivity shareDocumentActivityToSpace(String space, String activityId, String title, String type) throws Exception {
    Space targetSpace = spaceService.getSpaceByPrettyName(space);
    String authenticatedUser = ConversationState.getCurrent().getIdentity().getUserId();
    Identity targetSpaceIdentity = identityManager.getOrCreateIdentity(SpaceIdentityProvider.NAME, space);
    if (targetSpaceIdentity != null && SpaceUtils.isSpaceManagerOrSuperManager(authenticatedUser, targetSpace.getGroupId())
        || (spaceService.isMember(targetSpace, authenticatedUser) && SpaceUtils.isRedactor(authenticatedUser, targetSpace.getGroupId()))) {
      Map<String, String> originalActivityTemplateParams = activityManager.getActivity(activityId).getTemplateParams();
      String[] originalActivityFilesWorkspaces = getParameterValues(originalActivityTemplateParams, UIDocActivity.WORKSPACE);
      String[] originalActivityFilesIds = getParameterValues(originalActivityTemplateParams, FileUIActivity.ID);
      Map<String, String> templateParams = new HashMap<>();
      concatenateParam(templateParams, "originalActivityId", activityId);
      if (originalActivityFilesIds != null && originalActivityFilesIds.length > 0) {
        for (int i = 0; i < originalActivityFilesIds.length; i++) {
          String originalActivityFileWorkspace = "collaboration";
          if (originalActivityFilesWorkspaces != null
              && originalActivityFilesWorkspaces.length == originalActivityFilesIds.length
              && StringUtils.isNotBlank(originalActivityFilesWorkspaces[i])) {
            originalActivityFileWorkspace = originalActivityFilesWorkspaces[i];
          }

          ExtendedSession originalActivityFileNodeSession = (ExtendedSession) WCMCoreUtils.getSystemSessionProvider()
                                                                  .getSession(originalActivityFileWorkspace,
                                                                              repoService.getCurrentRepository());
          Node originalActivityFileNode = originalActivityFileNodeSession.getNodeByIdentifier(originalActivityFilesIds[i]);

          String targetSpaceFileNodeUUID = publishDocumentToSpace(targetSpace.getGroupId(),
                                                                                       originalActivityFileNode,
                                                                                       "",
                                                                                       PermissionType.READ,
                                                                                       false);
          Node targetSpaceFileNode = originalActivityFileNode.getSession().getNodeByUUID(targetSpaceFileNodeUUID);
          concatenateParam(templateParams, FileUIActivity.ID, targetSpaceFileNodeUUID);
          String repository = ((ManageableRepository) targetSpaceFileNode.getSession().getRepository()).getConfiguration()
                                                                                                       .getName();
          concatenateParam(templateParams, UIDocActivity.REPOSITORY, repository);
          String workspace = targetSpaceFileNode.getSession().getWorkspace().getName();
          concatenateParam(templateParams, UIDocActivity.WORKSPACE, workspace);

          concatenateParam(templateParams, FileUIActivity.CONTENT_LINK, Utils.getContentLink(targetSpaceFileNode));
          String state;
          try {
            state = targetSpaceFileNode.hasProperty(Utils.CURRENT_STATE_PROP) ? targetSpaceFileNode.getProperty(Utils.CURRENT_STATE_PROP)
                                                                                                   .getValue()
                                                                                                   .getString()
                                                                             : "";
          } catch (Exception e) {
            state = "";
          }
          concatenateParam(templateParams, FileUIActivity.STATE, state);

          /** The date formatter. */
          DateFormat dateFormatter = null;
          dateFormatter = new SimpleDateFormat(ISO8601.SIMPLE_DATETIME_FORMAT);
          String strDateCreated = "";
          if (targetSpaceFileNode.hasProperty(NodetypeConstant.EXO_DATE_CREATED)) {
            Calendar dateCreated = targetSpaceFileNode.getProperty(NodetypeConstant.EXO_DATE_CREATED).getDate();
            strDateCreated = dateFormatter.format(dateCreated.getTime());
            concatenateParam(templateParams, FileUIActivity.DATE_CREATED, strDateCreated);
          }
          String strLastModified = "";
          if (targetSpaceFileNode.hasNode(NodetypeConstant.JCR_CONTENT)) {
            Node contentNode = targetSpaceFileNode.getNode(NodetypeConstant.JCR_CONTENT);
            if (contentNode.hasProperty(NodetypeConstant.JCR_LAST_MODIFIED)) {
              Calendar lastModified = contentNode.getProperty(NodetypeConstant.JCR_LAST_MODIFIED)
                                                 .getDate();
              strLastModified = dateFormatter.format(lastModified.getTime());
              concatenateParam(templateParams, FileUIActivity.LAST_MODIFIED, strLastModified);
            }
          }

          concatenateParam(templateParams, FileUIActivity.MIME_TYPE, Utils.getMimeType(originalActivityFileNode));
          concatenateParam(templateParams, FileUIActivity.IMAGE_PATH, Utils.getIllustrativeImage(targetSpaceFileNode));
          String nodeTitle;
          try {
            nodeTitle = org.exoplatform.ecm.webui.utils.Utils.getTitle(targetSpaceFileNode);
          } catch (Exception e1) {
            nodeTitle = "";
          }
          concatenateParam(templateParams, FileUIActivity.DOCUMENT_TITLE, nodeTitle);
          concatenateParam(templateParams, FileUIActivity.DOCUMENT_VERSION, "");
          concatenateParam(templateParams,
                           FileUIActivity.DOCUMENT_SUMMARY,
                           Utils.getFirstSummaryLines(Utils.getSummary(targetSpaceFileNode), Utils.MAX_SUMMARY_CHAR_COUNT));
          concatenateParam(templateParams, UIDocActivity.DOCPATH, targetSpaceFileNode.getPath());
          concatenateParam(templateParams, UILinkActivity.LINK_PARAM, "");// to
                                                                          // check
                                                                          // if
                                                                          // necessary
          concatenateParam(templateParams, UIDocActivity.IS_SYMLINK, "true");
        }
      }

      // create activity
      ExoSocialActivity sharedActivity = new ExoSocialActivityImpl();
      sharedActivity.setTitle(title);
      sharedActivity.setType(SharedFileUIActivity.ACTIVITY_TYPE);
      Identity authenticatedUserIdentity = identityManager.getOrCreateIdentity(OrganizationIdentityProvider.NAME, authenticatedUser);
      sharedActivity.setUserId(authenticatedUserIdentity.getId());
      sharedActivity.setTemplateParams(templateParams);
      activityManager.saveActivityNoReturn(targetSpaceIdentity, sharedActivity);
      return sharedActivity;
    }
    return null;
  }
  

  @Override
  public void start() {
    // TODO Auto-generated method stub

  }

  @Override
  public void stop() {
    // TODO Auto-generated method stub

  }

  private void removeSpacePermission(ExtendedNode node, String space) {
    try {
      node.removePermission("*:" + space);
      node.save();
    } catch (RepositoryException e) {
      if(LOG.isErrorEnabled())
        LOG.error(e.getMessage(), e);
    }
  }

  private void removeUserPermission(ExtendedNode node, String user) {
    try {
      node.removePermission(user);
      node.save();
    } catch (RepositoryException e) {
      if(LOG.isErrorEnabled())
        LOG.error(e.getMessage(), e);
    }
  }

  private String getMimeType(Node node) {
    try {
      if (node.getPrimaryNodeType().getName().equals(NodetypeConstant.NT_FILE)) {
        if (node.hasNode(NodetypeConstant.JCR_CONTENT))
          return node.getNode(NodetypeConstant.JCR_CONTENT)
              .getProperty(NodetypeConstant.JCR_MIME_TYPE)
              .getString();
      }
    } catch (RepositoryException e) {
      if(LOG.isErrorEnabled())
        LOG.error(e.getMessage(), e);
    }
    return "";
  }

  /**
   * Grant view for parent folder when share a document
   * We need grant assess right for parent in case editing the shared documents
   * @param currentNode
   * @param memberShip
   * @param permissions
   * @throws Exception
   */
  private void setSpacePermission(Node currentNode, String memberShip, String[] permissions) throws Exception{
    ExtendedNode node = (ExtendedNode) currentNode;
    if (node.getACL().getPermissions("*:" + memberShip) == null || node.getACL().getPermissions("*:" + memberShip).size() == 0) {
      if(node.canAddMixin(MIX_PRIVILEGEABLE))node.addMixin(MIX_PRIVILEGEABLE);
      node.setPermission("*:" + memberShip, permissions);
      node.save();
    }
  }

  /**
   * Grant view for parent folder when share a document
   * We need grant assess right for parent in case editing the shared documents
   * @param currentNode
   * @param username
   * @param permissions
   * @throws Exception
   */
  private void setUserPermission(Node currentNode, String username, String[] permissions) throws Exception{
    ExtendedNode node = (ExtendedNode) currentNode;
    if(node.canAddMixin(MIX_PRIVILEGEABLE))node.addMixin(MIX_PRIVILEGEABLE);
    node.setPermission(username, permissions);
    node.save();
  }
  
  private String[] getParameterValues(Map<String, String> activityParams, String paramName) {
    String[] values = null;
    String value = activityParams.get(paramName);
    if (value == null) {
      value = activityParams.get(paramName.toLowerCase());
    }
    if (value != null) {
      values = value.split(FileUIActivity.SEPARATOR_REGEX);
    }
    return values;
  }

  private void concatenateParam(Map<String, String> activityParams, String paramName, String paramValue) {
    String oldParamValue = activityParams.get(paramName);
    if (StringUtils.isBlank(oldParamValue)) {
      activityParams.put(paramName, paramValue);
    } else {
      activityParams.put(paramName, oldParamValue + TEMPLATE_PARAMS_SEPARATOR + paramValue);
    }
  }
}
