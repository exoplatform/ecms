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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

import javax.jcr.Item;
import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.ValueFormatException;
import javax.jcr.nodetype.NodeType;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;

import org.apache.commons.lang.StringUtils;
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
import org.exoplatform.container.configuration.ConfigurationManager;
import org.exoplatform.container.definition.PortalContainerConfig;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.container.xml.ObjectParameter;
import org.exoplatform.container.xml.PortalContainerInfo;
import org.exoplatform.container.xml.ValueParam;
import org.exoplatform.portal.config.UserACL;
import org.exoplatform.portal.webui.util.Util;
import org.exoplatform.services.cms.CmsService;
import org.exoplatform.services.cms.link.LinkManager;
import org.exoplatform.services.cms.metadata.MetadataService;
import org.exoplatform.services.cms.templates.TemplateService;
import org.exoplatform.services.deployment.plugins.LinkDeploymentDescriptor;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.jcr.ext.app.SessionProviderService;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.listener.ListenerService;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.organization.Membership;
import org.exoplatform.services.organization.OrganizationService;
import org.exoplatform.services.security.ConversationState;
import org.exoplatform.services.wcm.core.NodeLocation;
import org.exoplatform.services.wcm.core.NodetypeConstant;
import org.exoplatform.services.wcm.portal.LivePortalManagerService;
import org.quartz.JobExecutionContext;
import org.quartz.impl.JobDetailImpl;

/**
 * Created by The eXo Platform SAS
 * Author : Tran Nguyen Ngoc
 * ngoc.tran@exoplatform.com
 * Sep 8, 2009
 */
public class WCMCoreUtils {

  private static final Log LOG = ExoLogger.getLogger(WCMCoreUtils.class.getName());

  private static String WEBCONTENT_CSS_QUERY = "select * from exo:cssFile where jcr:path like '{path}/%' "
      + "and exo:active='true' "
      + "and jcr:mixinTypes <> 'exo:restoreLocation' "
      + "order by exo:priority ASC";

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
    try {
      OrganizationService organizationService = WCMCoreUtils.getService(OrganizationService.class);
      startRequest(organizationService);
      Collection<?> memberships = organizationService.getMembershipHandler().findMembershipsByUser(userId);
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

  public static String getProjectVersion() throws Exception {
    String filePath = "jar:/conf/projectInfo.properties";
    Properties productInformationProperties = new Properties();
    try {
      ConfigurationManager configManager = WCMCoreUtils.getService(ConfigurationManager.class);
      if (LOG.isInfoEnabled()) {
        LOG.info("Read products versions from " + filePath);
      }
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
    StringBuilder buffer = new StringBuilder();
    String cssQuery = StringUtils.replaceOnce(WEBCONTENT_CSS_QUERY, "{path}", webcontent.getPath());
    // Need re-login to get session because this node is get from template and the session is not live anymore.
    // If node is version (which is stored in system workspace) we have to login to system workspace to get data
    NodeLocation webcontentLocation = NodeLocation.getNodeLocationByNode(webcontent);
    ManageableRepository repository = (ManageableRepository)webcontent.getSession().getRepository();
    Session session;
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
      if (LOG.isErrorEnabled()) {
        LOG.error("Unexpected problem happen when active stylesheet", e);
      }
    }
    return buffer.toString();
  }

  /**
   * gets the global css of given site node. For example, if the site is acme<br/>
   * we then return all css code only inside acme/css
   * @param siteNode the root node of the site
   * @return global css code inside this site
   * @throws Exception
   */
  public static String getSiteGlobalActiveStylesheet(Node siteNode) throws Exception {
    if (siteNode == null) return StringUtils.EMPTY;

    StringBuilder buffer = new StringBuilder();
    try {
      List<Node> cssNodeList = new ArrayList<Node>();
      NodeIterator iterator = siteNode.getNodes();
      //get all cssFolder child nodes of siteNode
      while (iterator.hasNext()) {
        Node cssFolder = iterator.nextNode();
        if (cssFolder.isNodeType(NodetypeConstant.EXO_CSS_FOLDER)) {
          NodeIterator iter = cssFolder.getNodes();
          //get all cssFile child nodes of cssFolder node
          while (iter.hasNext()) {
            Node registeredCSSFile = iter.nextNode();
            if (registeredCSSFile.isNodeType(NodetypeConstant.EXO_CSS_FILE) &&
                registeredCSSFile.getProperty(NodetypeConstant.EXO_ACTIVE).getBoolean()) {
              cssNodeList.add(registeredCSSFile);
            }
          }
        }
      }
      //sort cssFile by priority and merge them
      Collections.sort(cssNodeList, new FileCSSComparatorByPriority());
      for (Node registeredCSSFile : cssNodeList) {
        try {
          buffer.append(registeredCSSFile.getNode(NodetypeConstant.JCR_CONTENT)
                        .getProperty(NodetypeConstant.JCR_DATA)
                        .getString());
        } catch (Exception e) {
          continue;
        }
      }
    } catch(Exception e) {
      if (LOG.isErrorEnabled()) {
        LOG.error("Unexpected problem happen when active stylesheet", e);
      }
    }
    return buffer.toString();
  }

  /**
   * gets the global javascript of given site node. For example, if the site is acme<br/>
   * we then return all javascript code only inside acme/js
   * @param siteNode the root node of the site
   * @return global javascript code inside this site
   * @throws Exception
   */
  public static String getSiteGlobalActiveJs(Node siteNode) throws Exception {
    StringBuilder buffer = new StringBuilder();
    LivePortalManagerService livePortalService = getService(LivePortalManagerService.class);
    buffer.append(getSiteActiveJs(livePortalService.getLiveSharedPortal(getUserSessionProvider()))).append(getSiteActiveJs(siteNode));
    return buffer.toString();
  }

  public static String getSiteActiveJs(Node siteNode) throws Exception {
    if (siteNode == null) return StringUtils.EMPTY;

    StringBuilder buffer = new StringBuilder();
    try {
      List<Node> jsNodeList = new ArrayList<Node>();
      NodeIterator iterator = siteNode.getNodes();
      //get all jsFolder child nodes of siteNode
      while (iterator.hasNext()) {
        Node jsFolder = iterator.nextNode();
        if (jsFolder.isNodeType(NodetypeConstant.EXO_JS_FOLDER)) {
          NodeIterator iter = jsFolder.getNodes();
          //get all jsFile child nodes of jsFolder node
          while (iter.hasNext()) {
            Node registeredJSFile = iter.nextNode();
            if (registeredJSFile.isNodeType(NodetypeConstant.EXO_JS_FILE) &&
                registeredJSFile.getProperty(NodetypeConstant.EXO_ACTIVE).getBoolean()) {
              jsNodeList.add(registeredJSFile);
            }
          }
        }
      }
      //sort jsFile by priority and merge them
      Collections.sort(jsNodeList, new FileComparatorByPriority());
      for (Node registeredJSFile : jsNodeList) {
        try {
          buffer.append(registeredJSFile.getNode(NodetypeConstant.JCR_CONTENT)
                        .getProperty(NodetypeConstant.JCR_DATA)
                        .getString());
        } catch (Exception e) {
          continue;
        }
      }
    } catch(Exception e) {
      if (LOG.isErrorEnabled()) {
        LOG.error("Unexpected problem happen when active javascript", e);
      }
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
    Item primaryItem;
    try {
      primaryItem = node.getPrimaryItem();
    } catch (ItemNotFoundException e) {
      primaryItem = null;
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

  public static String getRestContextName() {
    ExoContainer container = ExoContainerContext.getCurrentContainer();
    PortalContainerConfig portalContainerConfig = (PortalContainerConfig) container.
        getComponentInstance(PortalContainerConfig.class);
    PortalContainerInfo containerInfo =
        (PortalContainerInfo)container.getComponentInstanceOfType(PortalContainerInfo.class) ;
    return portalContainerConfig.getRestContextName(containerInfo.getContainerName());
  }

  public static void deployLinkToPortal(InitParams initParams,
                                        RepositoryService repositoryService,
                                        LinkManager linkManager,
                                        SessionProvider sessionProvider,
                                        String portalName) throws Exception {
    Iterator iterator = initParams.getObjectParamIterator();
    LinkDeploymentDescriptor deploymentDescriptor = null;
    ValueParam valueParam = initParams.getValueParam("override");
    boolean overrideData = false;
    if (valueParam != null) {
        overrideData = "true".equals(valueParam.getValue());
    }
    try {
      while (iterator.hasNext()) {
        String sourcePath = null;
        String targetPath = null;
        try {
          ObjectParameter objectParameter = (ObjectParameter) iterator.next();
          deploymentDescriptor = (LinkDeploymentDescriptor) objectParameter.getObject();
          sourcePath = deploymentDescriptor.getSourcePath();
          targetPath = deploymentDescriptor.getTargetPath();
  
          //in case: create portal from template
          if (portalName != null && portalName.length() > 0) {
            sourcePath = StringUtils.replace(sourcePath, "{portalName}", portalName);
            targetPath = StringUtils.replace(targetPath, "{portalName}", portalName);
          }
  
          // sourcePath should looks like : repository:collaboration:/sites
          // content/live/acme
          String[] src = sourcePath.split(":");
          String[] tgt = targetPath.split(":");
  
          if (src.length == 3 && tgt.length == 3) {
            ManageableRepository repository = repositoryService.getCurrentRepository();
            Session session = sessionProvider.getSession(src[1], repository);
            ManageableRepository repository2 = repositoryService.getCurrentRepository();
            Session session2 = sessionProvider.getSession(tgt[1], repository2);
            Node nodeSrc = session.getRootNode().getNode(src[2].substring(1));
            Node nodeTgt = session2.getRootNode().getNode(tgt[2].substring(1));
            Node tnode = (Node) session.getItem(nodeTgt.getPath());
            //check if the link node already exist then remove it 
            if (overrideData && tnode.hasNode(nodeSrc.getName())) {
                 NodeIterator nodeIterator = tnode.getNodes(nodeSrc.getName());
                while (nodeIterator.hasNext()) {
                  String path = "";
                  try {
                    Node targetNode = nodeIterator.nextNode();
                    path = targetNode.getPath();
                    LOG.info(" - Remove " + targetNode.getPath());
                    targetNode.remove();
                    session.save();
                  } catch (Exception e) {
                    if (LOG.isDebugEnabled()) {
                      LOG.debug("Can not remove node: " + path, e);
                    } else if (LOG.isWarnEnabled()) {
                      LOG.warn("Can not remove node: " + path);
                    }
                  }
                }
            }
            linkManager.createLink(nodeTgt, "exo:taxonomyLink", nodeSrc);
            ExoContainer container = ExoContainerContext.getCurrentContainer();
            PortalContainerInfo containerInfo =
              (PortalContainerInfo) container.getComponentInstanceOfType(PortalContainerInfo.class);
            String containerName = containerInfo.getContainerName();
            ListenerService listenerService = WCMCoreUtils.getService(ListenerService.class,
                                                                      containerName);
            CmsService cmsService = WCMCoreUtils.getService(CmsService.class, containerName);
            listenerService.broadcast("WCMPublicationService.event.updateState", cmsService, nodeSrc);
          }
          if (LOG.isInfoEnabled()) {
            LOG.info(sourcePath + " has a link into " + targetPath);
          }
        } catch (Exception e) {
          if (LOG.isDebugEnabled()) {
            LOG.debug("An error occurs when deploy link from " + sourcePath + " to " + targetPath, e);
          } else if (LOG.isWarnEnabled()) {
            LOG.warn("Can not deploy link from " + sourcePath + " to " + targetPath + ": " + e.getMessage());
          }
        }
      }
    } catch (Exception ex) {
      if (LOG.isErrorEnabled()) {
        LOG.error("create link from " + deploymentDescriptor.getSourcePath() + " to "
            + deploymentDescriptor.getTargetPath() + " is FAILURE at "
            + new Date().toString() + "\n",
            ex);
      }
      throw ex;
    }
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
        if (!o1.hasProperty(NodetypeConstant.EXO_PRIORITY)) {
          return 1;
        } else if (!o2.hasProperty(NodetypeConstant.EXO_PRIORITY)) {
          return -1;
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
   * compares two CSSFile node by exo:priority value, tending to sort in ASC order
   * because CSSFile file with higher priority is loaded last
   * @author vinh_nguyen
   */
  private static class FileCSSComparatorByPriority implements Comparator<Node>{
    @Override
    public int compare(Node o1, Node o2) {
      try {
        if (!o1.hasProperty(NodetypeConstant.EXO_PRIORITY)) {
          return -1;
        } else if (!o2.hasProperty(NodetypeConstant.EXO_PRIORITY)) {
          return 1;
        } else {
          return (int)(o1.getProperty(NodetypeConstant.EXO_PRIORITY).getLong() -
              o2.getProperty(NodetypeConstant.EXO_PRIORITY).getLong());
        }
      } catch (ValueFormatException e) {
        return 0;
      } catch (PathNotFoundException e) {
        return 0;
      } catch (RepositoryException e) {
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
      if(!file.isNodeType("nt:file")) return null;
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
    boolean isDocument = true;
    TemplateService templateService = WCMCoreUtils.getService(TemplateService.class);
    isDocument = templateService.getAllDocumentNodeTypes().contains(node.getPrimaryNodeType().getName()); 
    return isDocument;
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
}
