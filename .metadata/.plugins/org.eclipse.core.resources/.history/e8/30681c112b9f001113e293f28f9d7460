/*
 * Copyright (C) 2003-2008 eXo Platform SAS.
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
package org.exoplatform.services.wcm.publication.listener.navigation;

import java.util.ArrayList;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Session;
import javax.jcr.ValueFactory;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;

import org.exoplatform.container.ExoContainer;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.portal.config.UserPortalConfigService;
import org.exoplatform.portal.config.model.Page;
import org.exoplatform.portal.config.model.PortalConfig;
import org.exoplatform.portal.mop.navigation.NavigationContext;
import org.exoplatform.portal.mop.navigation.NavigationService;
import org.exoplatform.portal.mop.navigation.NodeContext;
import org.exoplatform.portal.mop.navigation.NodeModel;
import org.exoplatform.portal.mop.navigation.Scope;
import org.exoplatform.services.ecm.publication.NotInPublicationLifecycleException;
import org.exoplatform.services.ecm.publication.PublicationService;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.wcm.core.NodeLocation;
import org.exoplatform.services.wcm.core.WCMConfigurationService;
import org.exoplatform.services.wcm.publication.PublicationUtil;
import org.exoplatform.services.wcm.publication.WebpagePublicationPlugin;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;

/**
 * Created by The eXo Platform SAS
 * Author : Hoa Pham
 * hoa.pham@exoplatform.com
 * Oct 6, 2008
 */
public class NavigationEventListenerDelegate {

  /** The lifecycle name. */
  private String lifecycleName;

  /**
   * Instantiates a new navigation event listener delegate.
   *
   * @param lifecycleName the lifecycle name
   * @param container the container
   */
  public NavigationEventListenerDelegate(String lifecycleName, ExoContainer container) {
    this.lifecycleName = lifecycleName;
  }

  /**
   * Update lifecyle on create navigation.
   *
   * @param pageNavigation the page navigation
   *
   * @throws Exception the exception
   */
  public void updateLifecyleOnCreateNavigation(NavigationContext navigationContext) throws Exception {
  }

  /**
   * Update lifecycle on change navigation.
   *
   * @param pageNavigation the page navigation
   * @param remoteUser the remote user
   * @param plugin
   * @throws Exception the exception
   */
  public void updateLifecycleOnChangeNavigation(
      NavigationContext navigationContext, String remoteUser, WebpagePublicationPlugin plugin) throws Exception {
    if (navigationContext.getKey().getTypeName().equals(PortalConfig.PORTAL_TYPE)) {
      updateRemovedPageNode(navigationContext, remoteUser, plugin);
      updateAddedPageNode(navigationContext, remoteUser);
    }
  }

  /**
   * Update lifecyle on remove navigation.
   *
   * @param pageNavigation the page navigation
   *
   * @throws Exception the exception
   */
  public void updateLifecyleOnRemoveNavigation(NavigationContext navigationContext) throws Exception {
  }

  /**
   * Update added page node.
   *
   * @param pageNavigation the page navigation
   * @param remoteUser remoteUser
   *
   * @throws Exception the exception
   */
  private void updateAddedPageNode(NavigationContext navigationContext, String remoteUser) throws Exception {
    UserPortalConfigService userPortalConfigService = WCMCoreUtils.getService(UserPortalConfigService.class);
    WCMConfigurationService wcmConfigurationService = WCMCoreUtils.getService(WCMConfigurationService.class);
    NavigationService navigationService = WCMCoreUtils.getService(NavigationService.class);
    NodeContext<?> nodeContext = navigationService.loadNode(NodeModel.SELF_MODEL, navigationContext, Scope.ALL, null);
    String portletName = wcmConfigurationService.getRuntimeContextParam(WCMConfigurationService.SCV_PORTLET);
    for (NodeContext<?> context : PublicationUtil.convertAllNodeContextToList(nodeContext)) {
      Page page = null;
      if (remoteUser == null)
        page = userPortalConfigService.getPage(context.getState().getPageRef());
      else
        page = userPortalConfigService.getPage(context.getState().getPageRef(), remoteUser);
      if (page != null) {
        for (String applicationId : PublicationUtil.getListApplicationIdByPage(page, portletName)) {
          Node content = PublicationUtil.getNodeByApplicationId(applicationId);
          if (content != null) {
            List<String> listExistedApplicationId = PublicationUtil.getValuesAsString(content,
                                                                                      "publication:applicationIDs");
            if (!listExistedApplicationId.contains(PublicationUtil.setMixedApplicationId(page.getPageId(),
                                                                                         applicationId))) {
              saveAddedPageNode(navigationContext.getKey().getName(), context, applicationId, content);
            }
          }
        }
      }
    }
  }

  /**
   * Update removed page node.
   *
   * @param pageNavigation the page navigation
   * @param remoteUser remoteUser
   * @param plugin
   * @throws Exception the exception
   */
  private void updateRemovedPageNode(NavigationContext navigationContext,
                                     String remoteUser,
                                     WebpagePublicationPlugin plugin) throws Exception {
    
    ExoContainer container = ExoContainerContext.getCurrentContainer();
    NavigationService navigationService = (NavigationService) container.getComponentInstanceOfType(NavigationService.class);
    NodeContext<?> nodeContext = navigationService.loadNode(NodeModel.SELF_MODEL, navigationContext, Scope.ALL, null);
    
    String portalName = navigationContext.getKey().getName();
    List<NodeContext<?>> listNodeContext = PublicationUtil.convertAllNodeContextToList(nodeContext);
    List<String> listPortalNavigationUri = new ArrayList<String>();
    List<String> listPageReference = new ArrayList<String>();
    for (NodeContext<?> context : listNodeContext) {
      String mixedNavigationNodeUri = PublicationUtil.setMixedNavigationUri(portalName,
                                                                            PublicationUtil.buildUserNodeURI(context)
                                                                                           .toString());
      listPortalNavigationUri.add(mixedNavigationNodeUri);
      listPageReference.add(context.getState().getPageRef());
    }

    RepositoryService repositoryService = WCMCoreUtils.getService(RepositoryService.class);
    WCMConfigurationService wcmConfigurationService = WCMCoreUtils.getService(WCMConfigurationService.class);
    ManageableRepository repository = repositoryService.getCurrentRepository();
    NodeLocation nodeLocation = wcmConfigurationService.getLivePortalsLocation(repository.getConfiguration().getName());
    String portletName = wcmConfigurationService.getRuntimeContextParam(WCMConfigurationService.SCV_PORTLET);

    String workspaceName = nodeLocation.getWorkspace();
    String path = nodeLocation.getPath();
    SessionProvider sessionProvider = WCMCoreUtils.getSystemSessionProvider();
    Session session = sessionProvider.getSession(workspaceName, repositoryService.getCurrentRepository());

    QueryManager queryManager = session.getWorkspace().getQueryManager();
    Query query = queryManager.createQuery("select * from " + plugin.getLifecycleType()
        + " where publication:lifecycleName='" + lifecycleName + "' and jcr:path like '" + path
        + "/%' order by jcr:score", Query.SQL);
    QueryResult results = query.execute();
    for (NodeIterator nodeIterator = results.getNodes(); nodeIterator.hasNext();) {
      Node content = nodeIterator.nextNode();
      String navigationNodeUri = "";
      for (String existedNavigationNodeUri : PublicationUtil.getValuesAsString(content,
                                                                               "publication:navigationNodeURIs")) {
        if (existedNavigationNodeUri.startsWith("/" + portalName)) {
          navigationNodeUri = existedNavigationNodeUri;
        }
      }
      String pageId = "";
      for (String existedPageId : PublicationUtil.getValuesAsString(content,
                                                                    "publication:webPageIDs")) {
        if (!listPageReference.contains(existedPageId)) {
          pageId = existedPageId;
        }
      }
      if (!pageId.equals("")) {
        String applicationId = "";
        UserPortalConfigService userPortalConfigService = WCMCoreUtils.getService(UserPortalConfigService.class);
        Page page = null;
        if (remoteUser == null)
          page = userPortalConfigService.getPage(pageId);
        else
          page = userPortalConfigService.getPage(pageId, remoteUser);
        for (String applicationIdTmp : PublicationUtil.getListApplicationIdByPage(page, portletName)) {
          applicationIdTmp = PublicationUtil.setMixedApplicationId(pageId, applicationIdTmp);
          List<String> listExistedApplicationId = PublicationUtil.getValuesAsString(content,
                                                                                    "publication:applicationIDs");
          if (listExistedApplicationId.contains(applicationIdTmp)) {
            applicationId = applicationIdTmp;
          }
        }
        saveRemovedPageNode(navigationNodeUri, pageId, applicationId, content);
      }
    }
  }

  /**
   * Save added page node.
   *
   * @param portalName the portal name
   * @param nodeContext the page node
   * @param applicationId the application id
   * @param content the content
   *
   * @throws Exception the exception
   */
  private void saveAddedPageNode(String portalName,
                                 NodeContext<?> nodeContext,
                                 String applicationId,
                                 Node content) throws Exception {
    PublicationService publicationService = WCMCoreUtils.getService(PublicationService.class);
    String nodeLifecycleName = null;
    try {
      nodeLifecycleName = publicationService.getNodeLifecycleName(content);
    } catch (NotInPublicationLifecycleException e) { return; }
    if (!lifecycleName.equals(nodeLifecycleName)) return;

    Session session = content.getSession();
    ValueFactory valueFactory = session.getValueFactory();

    List<String> listExistedApplicationId = PublicationUtil.getValuesAsString(content, "publication:applicationIDs");
    String mixedApplicationId = PublicationUtil.setMixedApplicationId(nodeContext.getState().getPageRef(), applicationId);
    if(listExistedApplicationId.contains(mixedApplicationId)) return ;
    listExistedApplicationId.add(mixedApplicationId);
    content.setProperty("publication:applicationIDs", PublicationUtil.toValues(valueFactory, listExistedApplicationId));

    List<String> listExistedNavigationNodeUri = PublicationUtil.getValuesAsString(content, "publication:navigationNodeURIs");
    String mixedNavigationNodeUri = PublicationUtil.setMixedNavigationUri(portalName,
                                                                          PublicationUtil.buildUserNodeURI(nodeContext)
                                                                                         .toString());
    listExistedNavigationNodeUri.add(mixedNavigationNodeUri);
    content.setProperty("publication:navigationNodeURIs", PublicationUtil.toValues(valueFactory, listExistedNavigationNodeUri));

    List<String> listExistedWebPageId = PublicationUtil.getValuesAsString(content, "publication:webPageIDs");
    listExistedWebPageId.add(nodeContext.getState().getPageRef());
    content.setProperty("publication:webPageIDs", PublicationUtil.toValues(valueFactory, listExistedWebPageId));
    session.save();
  }

  /**
   * Save removed page node.
   *
   * @param navigationNodeUri the navigation node uri
   * @param pageId the page id
   * @param applicationId the application id
   * @param content the content
   *
   * @throws Exception the exception
   */
  private void saveRemovedPageNode(String navigationNodeUri,
                                   String pageId,
                                   String applicationId,
                                   Node content) throws Exception {
    Session session = content.getSession();
    ValueFactory valueFactory = session.getValueFactory();

    List<String> listExistedNavigationNodeUri = PublicationUtil.getValuesAsString(content,
                                                                                  "publication:navigationNodeURIs");
    List<String> listExistedNavigationNodeUriTmp = PublicationUtil.getValuesAsString(content,
                                                                                     "publication:navigationNodeURIs");
    if (listExistedNavigationNodeUri.contains(navigationNodeUri)) {
      listExistedNavigationNodeUriTmp.remove(navigationNodeUri);
    }
    content.setProperty("publication:navigationNodeURIs",
                        PublicationUtil.toValues(valueFactory, listExistedNavigationNodeUriTmp));

    List<String> listExistedPageId = PublicationUtil.getValuesAsString(content, "publication:webPageIDs");
    List<String> listExistedPageIdTmp = PublicationUtil.getValuesAsString(content, "publication:webPageIDs");
    if (listExistedPageId.contains(pageId)) {
      listExistedPageIdTmp.remove(pageId);
    }
    content.setProperty("publication:webPageIDs", PublicationUtil.toValues(valueFactory, listExistedPageIdTmp));

    List<String> listExistedApplicationId = PublicationUtil.getValuesAsString(content, "publication:applicationIDs");
    List<String> listExistedApplicationIdTmp = PublicationUtil.getValuesAsString(content, "publication:applicationIDs");
    if (listExistedApplicationId.contains(applicationId)) {
      listExistedApplicationIdTmp.remove(applicationId);
    }
    content.setProperty("publication:applicationIDs", PublicationUtil.toValues(valueFactory, listExistedApplicationIdTmp));
    session.save();
  }
}
