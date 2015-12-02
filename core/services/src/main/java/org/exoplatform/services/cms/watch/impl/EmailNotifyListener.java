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
package org.exoplatform.services.cms.watch.impl;

import groovy.text.GStringTemplateEngine;
import groovy.text.TemplateEngine;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.observation.EventIterator;
import javax.jcr.observation.EventListener;
import javax.portlet.PortletRequest;

import org.exoplatform.portal.application.PortalRequestContext;
import org.exoplatform.portal.mop.SiteType;
import org.exoplatform.portal.mop.user.UserNavigation;
import org.exoplatform.portal.mop.user.UserNode;
import org.exoplatform.portal.mop.user.UserPortal;
import org.exoplatform.portal.webui.util.Util;
import org.exoplatform.services.cms.drives.DriveData;
import org.exoplatform.services.cms.drives.ManageDriveService;
import org.exoplatform.services.cms.watch.WatchDocumentService;
import org.exoplatform.services.jcr.ext.hierarchy.NodeHierarchyCreator;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.mail.MailService;
import org.exoplatform.services.mail.Message;
import org.exoplatform.services.organization.OrganizationService;
import org.exoplatform.services.organization.Query;
import org.exoplatform.services.organization.User;
import org.exoplatform.services.security.ConversationState;
import org.exoplatform.services.security.Identity;
import org.exoplatform.services.security.IdentityRegistry;
import org.exoplatform.services.security.MembershipEntry;
import org.exoplatform.services.wcm.core.NodeLocation;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;
import org.exoplatform.web.url.navigation.NodeURL;
import org.exoplatform.webui.application.WebuiRequestContext;
import org.exoplatform.webui.application.portlet.PortletRequestContext;

/**
 * Created by The eXo Platform SAS
 * Author : Xuan Hoa Pham
 *          hoapham@exoplatform.com
 *          phamvuxuanhoa@gmail.com
 * Dec 6, 2006
 */
public class EmailNotifyListener implements EventListener {

  private NodeLocation observedNode_ ;

  final public static String  EMAIL_WATCHERS_PROP = "exo:emailWatcher";

  private static final String SITE_EXPLORER       = "siteExplorer";

  private static final String PATH_PARAM          = "path";
  private static final String USER_ID             = "${userId}";

  private static final Log    LOG                 = ExoLogger.getLogger(EmailNotifyListener.class.getName());

  public EmailNotifyListener(Node oNode) {
    observedNode_ = NodeLocation.getNodeLocationByNode(oNode);
  }

  /**
   * This method is used for listening to all changes of property of a node, when there is a change,
   * message is sent to list of email
   */
  public void onEvent(EventIterator arg0) {
    MailService mailService = WCMCoreUtils.getService(MailService.class);
    WatchDocumentServiceImpl watchService = (WatchDocumentServiceImpl)WCMCoreUtils.getService(WatchDocumentService.class);
    MessageConfig messageConfig = watchService.getMessageConfig();
    List<String> emailList = getEmailList(NodeLocation.getNodeByLocation(observedNode_));
    if (arg0.getSize() > 5) {
     for (String receiver : emailList) {
      try {
        Message message = createMessage(receiver, messageConfig);
        mailService.sendMessage(message);
      } catch (Exception e) {
        if (LOG.isErrorEnabled()) {
          LOG.error("Unexpected error", e);
        }
      }
    }
   }
  }

  /**
   * Create message when there is any changes of property of a node.
   * @param receiver
   * @param messageConfig
   * @return
   * @throws Exception
   */
  private Message createMessage(String receiver, MessageConfig messageConfig) throws Exception {
    Message message = new Message();
    message.setFrom(messageConfig.getSender());
    message.setTo(receiver);
    message.setSubject(messageConfig.getSubject());
    TemplateEngine engine = new GStringTemplateEngine();
    Map<String, String> binding = new HashMap<String, String>();
    Query query = new Query();
    query.setEmail(receiver);
    binding.put("user_name", WCMCoreUtils.getService(OrganizationService.class)
                                         .getUserHandler()
                                         .findUsersByQuery(query)
                                         .load(0, 1)[0].getFullName());

    Node node = NodeLocation.getNodeByLocation(observedNode_);
    binding.put("doc_title", org.exoplatform.services.cms.impl.Utils.getTitle(node));
    binding.put("doc_name", node.getName());
    binding.put("doc_url", getViewableLink());
    message.setBody(engine.createTemplate(messageConfig.getContent()).make(binding).toString());
    message.setMimeType(messageConfig.getMimeType());
    return message;
  }

  /**
   *
   * @return
   * @throws Exception
   */
  private String getViewableLink() throws Exception {
    PortalRequestContext pContext = Util.getPortalRequestContext();
    NodeURL nodeURL = pContext.createURL(NodeURL.TYPE);
    String nodePath = NodeLocation.getNodeByLocation(observedNode_).getPath();

    ManageDriveService manageDriveService = WCMCoreUtils.getService(ManageDriveService.class);
    List<DriveData> driveList = manageDriveService.getDriveByUserRoles(pContext.getRemoteUser(),
                                                                       getMemberships());
    DriveData drive = getDrive(driveList,
                               WCMCoreUtils.getRepository()
                                           .getConfiguration()
                                           .getDefaultWorkspaceName(),
                               nodePath);

    String driverName = drive.getName();
    String nodePathInDrive = "/".equals(drive.getHomePath()) ? nodePath
                                                            : nodePath.substring(drive.getHomePath()
                                                                                      .length());
    UserNode siteExNode = getUserNodeByURI(SITE_EXPLORER);
    nodeURL.setNode(siteExNode);

    nodeURL.setQueryParameterValue(PATH_PARAM, "/" + driverName + nodePathInDrive);

    PortletRequestContext portletRequestContext = WebuiRequestContext.getCurrentInstance();
    PortletRequest portletRequest = portletRequestContext.getRequest();
    String baseURI = portletRequest.getScheme() + "://" + portletRequest.getServerName() + ":"
        + String.format("%s", portletRequest.getServerPort());
    return baseURI + nodeURL.toString();
  }

  /**
   *
   * @param lstDrive
   * @param workspace
   * @param nodePath
   * @return
   * @throws RepositoryException
   */
  private DriveData getDrive(List<DriveData> lstDrive, String workspace, String nodePath) throws RepositoryException {
    NodeHierarchyCreator nhc = WCMCoreUtils.getService(NodeHierarchyCreator.class);
    String userName = ConversationState.getCurrent().getIdentity().getUserId();
    int idx;
    String userNodePath = null;
    try {
      userNodePath = nhc.getUserNode(WCMCoreUtils.getSystemSessionProvider(), userName).getPath();
    }catch (Exception e) {
      //Exception while finding the user home node
      userNodePath = null;
    }
    DriveData driveData = null;
    for (DriveData drive : lstDrive) {
      String driveHomePath = drive.getHomePath();
      idx = driveHomePath.indexOf(USER_ID) ;
      if (idx >=0 && userNodePath!=null) {
        driveHomePath = userNodePath + driveHomePath.substring(idx + USER_ID.length());
      }
      if (workspace.equals(drive.getWorkspace()) && nodePath.startsWith(driveHomePath)) {
        driveData = drive;
        break;
      }
    }
    return driveData;
  }

  /**
   *
   * @param uri
   * @return
   */
  private UserNode getUserNodeByURI(String uri) {
    UserPortal userPortal = Util.getPortalRequestContext().getUserPortalConfig().getUserPortal();
    List<UserNavigation> allNavs = userPortal.getNavigations();

    for (UserNavigation nav : allNavs) {
      if (nav.getKey().getType().equals(SiteType.GROUP)) {
        UserNode userNode = userPortal.resolvePath(nav, null, uri);
        if (userNode != null) {
          return userNode;
        }
      }
    }
    return null;
  }

  /**
   *
   * @return
   * @throws Exception
   */
  public List<String> getMemberships() {
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
   *
   * @param authenticatedUser
   * @return
   */
  private Collection<MembershipEntry> getUserMembershipsFromIdentityRegistry(String authenticatedUser) {
    IdentityRegistry identityRegistry = WCMCoreUtils.getService(IdentityRegistry.class);
    Identity currentUserIdentity = identityRegistry.getIdentity(authenticatedUser);
    return currentUserIdentity.getMemberships();
  }

  /**
   * This Method will get email of watchers when they watch a document
   * @param observedNode
   * @return
   */
  private List<String> getEmailList(Node observedNode) {
    List<String> emailList = new ArrayList<String>() ;
    OrganizationService orgService = WCMCoreUtils.getService(OrganizationService.class);
    try{
      if(observedNode.hasProperty(EMAIL_WATCHERS_PROP)) {
        Value[] watcherNames = observedNode.getProperty(EMAIL_WATCHERS_PROP).getValues() ;
        for(Value value: watcherNames) {
          String userName = value.getString() ;
          User user = orgService.getUserHandler().findUserByName(userName) ;
          if(user != null) {
            emailList.add(user.getEmail()) ;
          }
        }
      }
    } catch (Exception e) {
      if (LOG.isErrorEnabled()) {
        LOG.error("Unexpected error", e);
      }
    }
    return emailList ;
  }
}

