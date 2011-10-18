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

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collection;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

import javax.jcr.Item;
import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Session;
import javax.jcr.nodetype.NodeType;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;

import org.apache.commons.lang.StringUtils;
import org.exoplatform.commons.utils.ListAccess;
import org.exoplatform.container.ExoContainer;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.container.RootContainer;
import org.exoplatform.container.component.ComponentRequestLifecycle;
import org.exoplatform.container.configuration.ConfigurationManager;
import org.exoplatform.portal.webui.util.Util;
import org.exoplatform.services.cms.metadata.MetadataService;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.jcr.ext.app.SessionProviderService;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.organization.Membership;
import org.exoplatform.services.organization.OrganizationService;
import org.exoplatform.services.wcm.core.NodeLocation;
import org.exoplatform.services.wcm.core.NodetypeConstant;
import org.quartz.JobExecutionContext;

/**
 * Created by The eXo Platform SAS
 * Author : Tran Nguyen Ngoc
 * ngoc.tran@exoplatform.com
 * Sep 8, 2009
 */
public class WCMCoreUtils {

  private static Log log = ExoLogger.getLogger("wcm.WCMCoreUtils");
  
  private static String WEBCONTENT_CSS_QUERY = "select * from exo:cssFile where jcr:path like '{path}/%' "
                                                + "and exo:active='true' "
                                                + "and jcr:mixinTypes <> 'exo:restoreLocation' "
                                                + "order by exo:priority ASC";

  
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
    SessionProvider sessionProvider = sessionProviderService.getSystemSessionProvider(null);
    return sessionProvider;
  }

  /**
   * Gets the session provider.
   *
   * @return the session provider
   */
  public static SessionProvider getUserSessionProvider() {
    SessionProviderService sessionProviderService = getService(SessionProviderService.class);
    SessionProvider sessionProvider = sessionProviderService.getSessionProvider(null);
    return sessionProvider;
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
    return context.getJobDetail().getGroup().split(":")[0];
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
    try {
      OrganizationService organizationService = WCMCoreUtils.getService(OrganizationService.class);
      startRequest(organizationService);
      Collection<?> memberships = null;
      Membership userMembership = null;
      String userMembershipTmp = null;
      int count = 0;
      String permissionTmp = "";
      for (String permission : permissions) {
        if (!permissionTmp.equals(permission)) count = 0;
        memberships = organizationService.getMembershipHandler().findMembershipsByUser(userId);
        Iterator<?> membershipIterator = memberships.iterator();
        while (membershipIterator.hasNext()) {
          userMembership = (Membership)membershipIterator.next();
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
      log.error("hasPermission() failed because of ", e);
    }
    return false;
  }

  public static <T> List<T> getAllElementsOfListAccess(ListAccess<T> listAccess) {
    try {
      return Arrays.asList(listAccess.load(0, listAccess.getSize()));
    } catch (Exception e) {
      log.error("getAllElementsOfListAccess() failed because of ", e);
    }
    return null;
  }

  /**
   * Get the repository by name
   *
   * @param repository the repository name
   *
   * @return the manageable repository by name, the current repository if name is null
   */
  @Deprecated
  public static ManageableRepository getRepository(String repository) {
    return getRepository();
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
      log.error("getRepository() failed because of ", e);
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
  
  public static String getProjectVersion() throws Exception {
    String filePath = "jar:/conf/projectInfo.properties";
    Properties productInformationProperties = new Properties();
    try {
      ConfigurationManager configManager = WCMCoreUtils.getService(ConfigurationManager.class);
      log.info("Read products versions from " + filePath);
      InputStream inputStream = configManager.getInputStream(filePath);
      
      productInformationProperties.load(inputStream);
    } catch (IOException exception) {
      throw new RuntimeException("Couldn't parse the file " + filePath, exception);
    } catch (Exception exception) {
      throw new RuntimeException("Error occured while reading the file " + filePath, exception);
    }
    
    if (!productInformationProperties.containsKey("project.current.version")) {
      throw new RuntimeException("Missing product information.");
    }
    return productInformationProperties.getProperty("project.current.version");
  }
  
  public static String getActiveStylesheet(Node webcontent) throws Exception {
    StringBuffer buffer = new StringBuffer();
    String cssQuery = StringUtils.replaceOnce(WEBCONTENT_CSS_QUERY, "{path}", webcontent.getPath());
    // Need re-login to get session because this node is get from template and the session is not live anymore.
    // If node is version (which is stored in system workspace) we have to login to system workspace to get data
    NodeLocation webcontentLocation = NodeLocation.getNodeLocationByNode(webcontent);
    ManageableRepository repository = (ManageableRepository)webcontent.getSession().getRepository();
    Session session = null;
    try {
      if (webcontentLocation.getPath().startsWith("/jcr:system"))
        session = 
          WCMCoreUtils.getSystemSessionProvider().getSession(repository.getConfiguration().getSystemWorkspaceName(), repository);
      else {
        session = WCMCoreUtils.getSystemSessionProvider().getSession(webcontentLocation.getWorkspace(), repository);
      }

      QueryManager queryManager = session.getWorkspace().getQueryManager();
      Query query = queryManager.createQuery(cssQuery, Query.SQL);
      QueryResult queryResult = query.execute();
      NodeIterator iterator = queryResult.getNodes();
      while (iterator.hasNext()) {
        Node registeredCSSFile = iterator.nextNode();
        buffer.append(registeredCSSFile.getNode(NodetypeConstant.JCR_CONTENT)
                                       .getProperty(NodetypeConstant.JCR_DATA)
                                       .getString());
      }
    } catch(Exception e) {
      log.error("Unexpected problem happen when active stylesheet", e);
    } 
    return buffer.toString();
  }
  
  public static Hashtable<String, String> getMetadataTemplates(Node node) throws Exception {
    MetadataService metadataService = WCMCoreUtils.getService(MetadataService.class);    
    Hashtable<String, String> templates = new Hashtable<String, String>();
    List<String> metaDataList = metadataService.getMetadataList();

    NodeType[] nodeTypes = node.getMixinNodeTypes();
    for(NodeType nt : nodeTypes) {
      if(metaDataList.contains(nt.getName())) {
        templates.put(nt.getName(), metadataService.getMetadataPath(nt.getName(), false));
      }
    }
    Item primaryItem = null;
    try {
      primaryItem = node.getPrimaryItem();
    } catch (ItemNotFoundException e) {
    }
    if (primaryItem != null && primaryItem.isNode()) {
      Node primaryNode = (Node) node.getPrimaryItem();
      NodeType[] primaryTypes = primaryNode.getMixinNodeTypes();
      for(NodeType nt : primaryTypes) {
        if(metaDataList.contains(nt.getName())) {
          templates.put(nt.getName(), metadataService.getMetadataPath(nt.getName(), false));
        }
      }
    }
    return templates;
  }
}