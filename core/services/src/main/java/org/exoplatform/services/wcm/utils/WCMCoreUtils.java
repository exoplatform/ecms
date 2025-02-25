/*
 * Copyright (C) 2003-2009 eXo Platform SAS.
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
package org.exoplatform.services.wcm.utils;

import java.io.InputStream;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;

import javax.jcr.Item;
import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.nodetype.NodeType;

import org.quartz.JobExecutionContext;
import org.quartz.impl.JobDetailImpl;

import org.exoplatform.commons.api.settings.SettingService;
import org.exoplatform.commons.api.settings.SettingValue;
import org.exoplatform.commons.api.settings.data.Context;
import org.exoplatform.commons.api.settings.data.Scope;
import org.exoplatform.commons.utils.ListAccess;
import org.exoplatform.container.ExoContainer;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.container.RootContainer;
import org.exoplatform.container.component.ComponentRequestLifecycle;
import org.exoplatform.container.definition.PortalContainerConfig;
import org.exoplatform.container.xml.PortalContainerInfo;
import org.exoplatform.portal.config.UserACL;
import org.exoplatform.portal.webui.util.Util;
import org.exoplatform.services.cms.documents.DocumentService;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.jcr.ext.app.SessionProviderService;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.organization.Membership;
import org.exoplatform.services.organization.OrganizationService;
import org.exoplatform.services.organization.idm.MembershipImpl;
import org.exoplatform.services.security.Authenticator;
import org.exoplatform.services.security.ConversationState;
import org.exoplatform.services.security.Identity;
import org.exoplatform.services.security.IdentityRegistry;
import org.exoplatform.services.security.MembershipEntry;
import org.exoplatform.services.wcm.core.NodeLocation;
import org.exoplatform.services.wcm.core.NodetypeConstant;

/**
 * Created by The eXo Platform SAS
 * Author : Tran Nguyen Ngoc
 * ngoc.tran@exoplatform.com
 * Sep 8, 2009
 */
public class WCMCoreUtils {

  private static final Log LOG = ExoLogger.getLogger(WCMCoreUtils.class.getName());

  private static final String BAR_NAVIGATION_STYLE_KEY = "bar_navigation_style";

  /**
   * Gets the service.
   *
   * @param clazz the clazz
   *
   * @return the service
   */
  public static <T> T getService(Class<T> clazz) {
    return getService(clazz, null);
  }

  /**
   * Gets the system session provider.
   *
   * @return the system session provider
   */
  public static SessionProvider getSystemSessionProvider() {
    SessionProviderService sessionProviderService = getService(SessionProviderService.class);
    return sessionProviderService.getSystemSessionProvider(null);
  }

  /**
   * Use only on system process
   * @param node
   * @return
   * @throws RepositoryException
   */
  public static Node getNodeBySystemSession(Node node) throws RepositoryException{
    SessionProvider systemSessionProvider = getSystemSessionProvider();
    return (Node)systemSessionProvider.getSession(node.getSession().getWorkspace().getName(), getRepository()).getItem(node.getPath());
  }

  /**
   * Check permission can access to parent
   * @param node
   * @return
   */
  public static boolean canAccessParentNode(Node node) {
    try {
      node.getParent();
    } catch (Exception e) {
      return false;
    }
    return true;
  }

  /**
   * Gets the session provider.
   *
   * @return the session provider
   */
  public static SessionProvider getUserSessionProvider() {
    SessionProviderService sessionProviderService = getService(SessionProviderService.class);
    return sessionProviderService.getSessionProvider(null);
  }

  public static boolean isAnonim()
  {
    String userId = Util.getPortalRequestContext().getRemoteUser();
    if (userId == null)
      return true;
    return false;
  }

  public static SessionProvider createAnonimProvider()
  {
    return SessionProvider.createAnonimProvider();
  }

  /**
   * Gets the service.
   *
   * @param clazz the class
   * @param containerName the container's name
   *
   * @return the service
   */
  public static <T> T getService(Class<T> clazz, String containerName) {
    ExoContainer container = ExoContainerContext.getCurrentContainer();
    if (containerName != null) {
      container = RootContainer.getInstance().getPortalContainer(containerName);
    }
    if (container.getComponentInstanceOfType(clazz)==null) {
      containerName = PortalContainer.getCurrentPortalContainerName();
      container = RootContainer.getInstance().getPortalContainer(containerName);
    }
    return clazz.cast(container.getComponentInstanceOfType(clazz));
  }

  public static String getContainerNameFromJobContext(JobExecutionContext context) {
    return ((JobDetailImpl)context.getJobDetail()).getGroup().split(":")[0];
  }

  /**
   * Check current user has permission to access a node or not
   * -    For each permission, compare with user's permissions
   * -      If permission has membership type is "*", just check the user's group id only
   * -      If permission has other membership types, then check the user's membership type and user's group id
   *
   * @param userId the current user's name
   * @param permissions the current node
   * @param isNeedFullAccess if true, count full access (4) then return true, if false, return true if match first permission
   *
   * @return true is user has permissions, otherwise return false
   */
  public static boolean hasPermission(String userId, List<String> permissions, boolean isNeedFullAccess) {
    if (userId == null || userId.length() == 0) {
      return false;
    }
    try {
      OrganizationService organizationService = WCMCoreUtils.getService(OrganizationService.class);
      startRequest(organizationService);
      Identity identity = ConversationState.getCurrent().getIdentity();
      Collection<?> memberships = null;
      if (userId.equals(identity.getUserId())){
        Collection<MembershipEntry> membershipsEntries = identity.getMemberships();
        HashSet<MembershipImpl> membershipsHash = new HashSet<MembershipImpl>();
        for (MembershipEntry membershipEntry : membershipsEntries) {
          MembershipImpl m = new MembershipImpl();
          m.setGroupId(membershipEntry.getGroup());
          m.setMembershipType(membershipEntry.getMembershipType());
          m.setUserName(userId);
          membershipsHash.add(m);
        }
        memberships =  new LinkedList<>(membershipsHash);
      } else {
        memberships = organizationService.getMembershipHandler().findMembershipsByUser(userId);
      }
      String userMembershipTmp;
      Membership userMembership;
      int count = 0;
      String permissionTmp = "";
      for (String permission : permissions) {
        if (!permissionTmp.equals(permission)) count = 0;
        for (Object userMembershipObj : memberships) {
          userMembership = (Membership) userMembershipObj;
          if (permission.equals(userMembership.getUserName())) {
            return true;
          } else if ("any".equals(permission)) {
            if (isNeedFullAccess) {
              count++;
              if (count == 4) return true;
            }
            else return true;
          } else if (permission.startsWith("*") && permission.contains(userMembership.getGroupId())) {
            if (isNeedFullAccess) {
              count++;
              if (count == 4) return true;
            }
            else return true;
          } else {
            userMembershipTmp = userMembership.getMembershipType() + ":" + userMembership.getGroupId();
            if (permission.equals(userMembershipTmp)) {
              if (isNeedFullAccess) {
                count++;
                if (count == 4) return true;
              }
              else return true;
            }
          }
        }
        permissionTmp = permission;
      }
      endRequest(organizationService);
    } catch (Exception e) {
      if (LOG.isErrorEnabled()) {
        LOG.error("hasPermission() failed because of ", e);
      }
    }
    return false;
  }

  public static <T> List<T> getAllElementsOfListAccess(ListAccess<T> listAccess) {
    try {
      return Arrays.asList(listAccess.load(0, listAccess.getSize()));
    } catch (Exception e) {
      if (LOG.isErrorEnabled()) {
        LOG.error("getAllElementsOfListAccess() failed because of ", e);
      }
    }
    return null;
  }

  /**
   * Get the current repository
   *
   * @return the current manageable repository
   */
  public static ManageableRepository getRepository() {
    try {
      RepositoryService repositoryService = getService(RepositoryService.class);
      return repositoryService.getCurrentRepository();
    } catch (Exception e) {
      if (LOG.isErrorEnabled()) {
        LOG.error("getRepository() failed because of ", e);
      }
    }
    return null;
  }

  public static void startRequest(OrganizationService orgService) throws Exception
  {
    if(orgService instanceof ComponentRequestLifecycle) {
      ((ComponentRequestLifecycle) orgService).startRequest(ExoContainerContext.getCurrentContainer());
    }
  }

  public static void endRequest(OrganizationService orgService) throws Exception
  {
    if(orgService instanceof ComponentRequestLifecycle) {
      ((ComponentRequestLifecycle) orgService).endRequest(ExoContainerContext.getCurrentContainer());
    }
  }

  public static String getRestContextName() {
    ExoContainer container = ExoContainerContext.getCurrentContainer();
    PortalContainerConfig portalContainerConfig = (PortalContainerConfig) container.
        getComponentInstance(PortalContainerConfig.class);
    PortalContainerInfo containerInfo =
        (PortalContainerInfo)container.getComponentInstanceOfType(PortalContainerInfo.class) ;
    return portalContainerConfig.getRestContextName(containerInfo.getContainerName());
  }

  /**
   * compares two JsFile node by exo:priority value, tending to sort in DESC order
   * because Js file with higher priority is loaded first
   * @author vu_nguyen
   *
   */
  private static class FileComparatorByPriority implements Comparator<Node> {
    @Override
    public int compare(Node o1, Node o2) {
      try {
        if (!o1.hasProperty(NodetypeConstant.EXO_PRIORITY) && !o2.hasProperty(NodetypeConstant.EXO_PRIORITY)) {
          return o1.getName().compareTo(o2.getName());
        } else if (!o1.hasProperty(NodetypeConstant.EXO_PRIORITY)) {
          return 1;
        } else if (!o2.hasProperty(NodetypeConstant.EXO_PRIORITY)) {
          return -1;
        } else if (o1.getProperty(NodetypeConstant.EXO_PRIORITY).getLong() == 
                  o2.getProperty(NodetypeConstant.EXO_PRIORITY).getLong()){
          return o1.getName().compareTo(o2.getName()); 
        } else {
          return (int)(o2.getProperty(NodetypeConstant.EXO_PRIORITY).getLong() -
              o1.getProperty(NodetypeConstant.EXO_PRIORITY).getLong());
        }
      } catch (Exception e) {
        return 0;
      }
    }
  }
  /**
   * Generate uri.
   *
   * @param file the node
   * @param propertyName the image property name, null if file is an image node
   *
   * @return the string
   *
   * @throws Exception the exception
   */
  public static String generateImageURI(Node file, String propertyName) throws Exception {
    StringBuilder builder = new StringBuilder();
    NodeLocation fileLocation = NodeLocation.getNodeLocationByNode(file);
    String repository = fileLocation.getRepository();
    String workspaceName = fileLocation.getWorkspace();
    String nodeIdentifiler = file.isNodeType("mix:referenceable") ? file.getUUID() : file.getPath().replaceFirst("/","");
    String portalName = PortalContainer.getCurrentPortalContainerName();
    String restContextName = PortalContainer.getCurrentRestContextName();

    if (propertyName == null) {
      if (isNodeTypeOrFrozenType(file, NodetypeConstant.NT_FILE)) {
        InputStream stream = file.getNode("jcr:content").getProperty("jcr:data").getStream();
        if (stream.available() == 0) return null;
        stream.close();
        builder.append("/").append(portalName).append("/")
        .append(restContextName).append("/")
        .append("images/")
        .append(repository).append("/")
        .append(workspaceName).append("/")
        .append(nodeIdentifiler)
        .append("?param=file");
        return builder.toString();
      } else return null;
    }
    builder.append("/").append(portalName).append("/")
    .append(restContextName).append("/")
    .append("images/")
    .append(repository).append("/")
    .append(workspaceName).append("/")
    .append(nodeIdentifiler)
    .append("?param=").append(propertyName);
    return builder.toString();
  }
  
  public static boolean isNodeTypeOrFrozenType(Node node, String type) throws RepositoryException {
    if (node.isNodeType(type)) return true;
    if (!node.isNodeType(NodetypeConstant.NT_FROZEN_NODE)) return false;
    String realType = node.getProperty("jcr:frozenPrimaryType").getString();
    return getRepository().getNodeTypeManager().getNodeType(realType).isNodeType(type);
  }
  
  public static String getPortalName() {
    PortalContainerInfo containerInfo = WCMCoreUtils.getService(PortalContainerInfo.class) ;
    return containerInfo.getContainerName() ;
  }

  public static String getRemoteUser() {
    try {
      return ConversationState.getCurrent().getIdentity().getUserId();
    } catch(NullPointerException npe) {
      return null;
    }
  }

  public static String getSuperUser() {
    return getService(UserACL.class).getSuperUser();
  }

  public static boolean isDocumentNodeType(Node node) throws Exception {
    return node.isNodeType(NodetypeConstant.NT_FILE);
  }
  
  /**
   * Get the bar navigation style of UIToolbarContainer.gtmpl
   * 
   * @return The String is style of bar navigation style
   */
  public static String getBarNavigationStyle() {
    SettingService settingService = getService(SettingService.class);
    String barNavigationStyle = "Dark";
    SettingValue<?> value = settingService.get(Context.GLOBAL, Scope.GLOBAL, BAR_NAVIGATION_STYLE_KEY);
    if (value != null) {
      barNavigationStyle = (String) value.getValue();
    } else {
      settingService.set(Context.GLOBAL, Scope.GLOBAL, BAR_NAVIGATION_STYLE_KEY, SettingValue.create(barNavigationStyle));
    }
    return barNavigationStyle;
  }
  /**
   * Get the link to display a document in the Documents app.
   * It will try to get the best matching context (personal doc, space doc, ...).
   * @param nodePath The path of the node
   * @return The link to open the document
   * @throws Exception
   */
  public static String getLinkInDocumentsApplication(String nodePath) throws Exception {
    DocumentService documentService = WCMCoreUtils.getService(DocumentService.class);
    return documentService.getLinkInDocumentsApp(nodePath);
  }
  
  /**
   * Allows to perform actions using user session provider.
   * Invokes the handler with obtained session provider using the conversation state based on userId.
   * Restores the conversation state after calling the handler.
   *
   * @param userId the user id for conversation state
   * @param handler the handler to be called
   * @throws RepositoryException the repository exception
   */
  @SuppressWarnings("deprecation")
  public static void invokeUserSession(String userId, RepositoryConsumer<SessionProvider> handler) throws RepositoryException {
    IdentityRegistry identityRegistry = getService(IdentityRegistry.class);
    Identity userIdentity = identityRegistry.getIdentity(userId);
    if (userIdentity == null) {
      // We create user identity by authenticator, but not register it in the
      // registry
      try {
        if (LOG.isDebugEnabled()) {
          LOG.debug("User identity not registered, trying to create it for: " + userId);
        }
        Authenticator authenticator = getService(Authenticator.class);
        userIdentity = authenticator.createIdentity(userId);
      } catch (Exception e) {
        LOG.warn("Failed to create user identity: " + userId, e);
        throw new IllegalArgumentException("User identity not found " + userId + " for setting conversation state");
      }
    }
    
    SessionProviderService sessionProviderService = getService(SessionProviderService.class);
    // Remember current conversation state
    ConversationState currentConvoState = ConversationState.getCurrent();
    SessionProvider currentContextProvider = sessionProviderService.getSessionProvider(null);
    try {
      ConversationState state = new ConversationState(userIdentity);
      // Keep subject as attribute in ConversationState.
      state.setAttribute(ConversationState.SUBJECT, userIdentity.getSubject());
      ConversationState.setCurrent(state);
      SessionProvider userProvider = new SessionProvider(state);
      sessionProviderService.setSessionProvider(null, userProvider);
      handler.accept(userProvider);
    } finally {
      // Restore previous conversation state
      ConversationState.setCurrent(currentConvoState);
      sessionProviderService.setSessionProvider(null, currentContextProvider);
    }
  } 
}
