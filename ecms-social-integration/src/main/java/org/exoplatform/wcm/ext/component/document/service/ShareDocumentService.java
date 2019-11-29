/*
 * Copyright (C) 2003-2014 eXo Platform SAS.
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

import javax.jcr.*;

import org.picocontainer.Startable;

import org.exoplatform.ecm.utils.permission.PermissionUtil;
import org.exoplatform.services.cms.BasePath;
import org.exoplatform.services.cms.link.LinkManager;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.access.PermissionType;
import org.exoplatform.services.jcr.core.ExtendedNode;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.jcr.ext.app.SessionProviderService;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.jcr.ext.hierarchy.NodeHierarchyCreator;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.wcm.core.NodetypeConstant;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;
import org.exoplatform.social.core.activity.model.ExoSocialActivity;
import org.exoplatform.wcm.ext.component.activity.listener.Utils;


/**
 * Created by The eXo Platform SAS
 * Author : eXoPlatform
 *          exo@exoplatform.com
 * Nov 19, 2014  
 */
public class ShareDocumentService implements IShareDocumentService, Startable{
  private static final Log    LOG                 = ExoLogger.getLogger(ShareDocumentService.class);

  public static final String MIX_PRIVILEGEABLE          = "exo:privilegeable";
  private RepositoryService repoService;
  private SessionProviderService sessionProviderService;
  private LinkManager linkManager;
  public ShareDocumentService(RepositoryService _repoService,
                              LinkManager _linkManager,
                              //IdentityManager _identityManager,
                              SessionProviderService _sessionProviderService){
    this.repoService = _repoService;
    this.sessionProviderService = _sessionProviderService;
    this.linkManager = _linkManager;

  }

  /* (non-Javadoc)
   * @see org.exoplatform.ecm.webui.component.explorer.popup.service.IShareDocumentService#publishDocumentToSpace(java.lang.String, javax.jcr.Node, java.lang.String, java.lang.String)
   */
  @Override
  public String publishDocumentToSpace(String space, Node currentNode, String comment,String perm) {
    Node rootSpace = null;
    Node shared = null;
    try {
      SessionProvider sessionProvider = sessionProviderService.getSystemSessionProvider(null);
      ManageableRepository repository = repoService.getCurrentRepository();
      Session session = sessionProvider.getSession(repository.getConfiguration().getDefaultWorkspaceName(), repository);
      //add symlink to destination space
      NodeHierarchyCreator nodeCreator = WCMCoreUtils.getService(NodeHierarchyCreator.class);
      nodeCreator.getJcrPath(BasePath.CMS_GROUPS_PATH);

      rootSpace = (Node) session.getItem(nodeCreator.getJcrPath(BasePath.CMS_GROUPS_PATH) + space);
      rootSpace = rootSpace.getNode("Documents");
      if(!rootSpace.hasNode("Shared")){
        shared = rootSpace.addNode("Shared");
      }else{
        shared = rootSpace.getNode("Shared");
      }
      if(currentNode.isNodeType(NodetypeConstant.EXO_SYMLINK)) currentNode = linkManager.getTarget(currentNode);
      //Update permission
      String tempPerms = perm.toString();//Avoid ref back to UIFormSelectBox options
      if(!tempPerms.equals(PermissionType.READ)) tempPerms = PermissionType.READ+","+PermissionType.ADD_NODE+","+PermissionType.SET_PROPERTY+","+PermissionType.REMOVE;
      if(PermissionUtil.canChangePermission(currentNode)){
        setSpacePermission(currentNode, space, tempPerms.split(","));
      }else if(PermissionUtil.canRead(currentNode)){
        SessionProvider systemSessionProvider = SessionProvider.createSystemProvider();
        Session systemSession = systemSessionProvider.getSession(session.getWorkspace().getName(), repository);
        Node _node= (Node)systemSession.getItem(currentNode.getPath());
        setSpacePermission(_node, space, tempPerms.split(","));
      }
      currentNode.getSession().save();
      Node link = linkManager.createLink(shared, currentNode);
      rootSpace.save();
      //Share activity
      try {
        ExoSocialActivity activity = null;
        if(currentNode.getPrimaryNodeType().getName().equals(NodetypeConstant.NT_FILE)){
          activity = Utils.createShareActivity(link, "", Utils.SHARE_FILE, comment, perm);
        }else{
          activity = Utils.createShareActivity(link,"", Utils.SHARE_CONTENT,comment, perm);
        }
        link.save();
        return activity.getId();
      } catch (Exception e1) {
        if(LOG.isErrorEnabled())
          LOG.error(e1.getMessage(), e1);
      }
    } catch (RepositoryException e) {
      if(LOG.isErrorEnabled())
        LOG.error(e.getMessage(), e);
    } catch (Exception e) {
      if(LOG.isErrorEnabled())
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
    if(node.canAddMixin(MIX_PRIVILEGEABLE))node.addMixin(MIX_PRIVILEGEABLE);
    node.setPermission("*:" + memberShip, permissions);
    node.save();
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

  @Override
  public void start() {
    // TODO Auto-generated method stub

  }

  @Override
  public void stop() {
    // TODO Auto-generated method stub

  }
}
