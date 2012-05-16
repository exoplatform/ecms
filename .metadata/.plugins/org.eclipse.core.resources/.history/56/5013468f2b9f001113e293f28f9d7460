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
package org.exoplatform.services.wcm.publication.listener.page;

import java.util.ArrayList;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.ValueFactory;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;

import org.exoplatform.container.ExoContainer;
import org.exoplatform.portal.config.model.Page;
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
public class PageEventListenerDelegate {

  /** The lifecycle name. */
  private String lifecycleName;

  /**
   * Instantiates a new page event listener delegate.
   *
   * @param lifecycleName the lifecycle name
   * @param container the container
   */
  public PageEventListenerDelegate(String lifecycleName, ExoContainer container) {
    this.lifecycleName = lifecycleName;
  }

  /**
   * Update lifecyle on create page.
   *
   * @param page the page
   * @param remoteUser the remote user
   * @param plugin
   * @throws Exception the exception
   */
  public void updateLifecyleOnCreatePage(Page page,
                                         String remoteUser,
                                         WebpagePublicationPlugin plugin) throws Exception {
    updateAddedApplication(page, remoteUser, plugin);
  }

  /**
   * Update lifecyle on change page.
   *
   * @param page the page
   * @param remoteUser the remote user
   * @param plugin
   * @throws Exception the exception
   */
  public void updateLifecyleOnChangePage(Page page,
                                         String remoteUser,
                                         WebpagePublicationPlugin plugin) throws Exception {
    updateAddedApplication(page, remoteUser, plugin);
    updateRemovedApplication(page, remoteUser, plugin);
  }

  /**
   * Update lifecycle on remove page.
   *
   * @param page the page
   * @param remoteUser the remote user
   * @param plugin
   * @throws Exception the exception
   */
  public void updateLifecycleOnRemovePage(Page page,
                                          String remoteUser,
                                          WebpagePublicationPlugin plugin) throws Exception {
    WCMConfigurationService wcmConfigurationService = WCMCoreUtils.getService(WCMConfigurationService.class);
    List<String> listPageApplicationId =
      PublicationUtil.getListApplicationIdByPage(
          page, wcmConfigurationService.getRuntimeContextParam(WCMConfigurationService.SCV_PORTLET));
    for (String applicationId : listPageApplicationId) {
      Node content = PublicationUtil.getNodeByApplicationId(applicationId);
      if (content != null) {
        saveRemovedApplication(page, applicationId, content, remoteUser, plugin);
      }
    }
  }

  /**
   * Update added application.
   *
   * @param page the page
   * @param remoteUser the remote user
   * @param plugin
   * @throws Exception the exception
   */
  private void updateAddedApplication(Page page, String remoteUser, WebpagePublicationPlugin plugin) throws Exception {
    WCMConfigurationService wcmConfigurationService = WCMCoreUtils.getService(WCMConfigurationService.class);
    String portletName = wcmConfigurationService.getRuntimeContextParam(WCMConfigurationService.SCV_PORTLET);
    List<String> listPageApplicationId = PublicationUtil.getListApplicationIdByPage(page,
                                                                                    portletName);
    for (String applicationtId : listPageApplicationId) {
      Node content = PublicationUtil.getNodeByApplicationId(applicationtId);
      if (content != null)
        saveAddedApplication(page, applicationtId, content, lifecycleName, remoteUser, plugin);
    }
  }

  /**
   * Update removed application.
   *
   * @param page the page
   * @param remoteUser the remote user
   * @param plugin
   * @throws Exception the exception
   */
  private void updateRemovedApplication(Page page,
                                        String remoteUser,
                                        WebpagePublicationPlugin plugin) throws Exception {
    List<Node> listNode = getListNodeByApplicationId(page, plugin);
    WCMConfigurationService wcmConfigurationService = WCMCoreUtils.getService(WCMConfigurationService.class);
    String portletName = wcmConfigurationService.getRuntimeContextParam(WCMConfigurationService.SCV_PORTLET);
    List<String> listApplicationId = new ArrayList<String>();
    listApplicationId.addAll(PublicationUtil.getListApplicationIdByPage(page, portletName));
    listApplicationId.addAll(PublicationUtil.getListApplicationIdByPage(page, portletName));

    for (Node content : listNode) {
      for (Value value : content.getProperty("publication:applicationIDs").getValues()) {
        String[] tmp = PublicationUtil.parseMixedApplicationId(value.getString());
        String nodeApplicationId = tmp[1];
        if (tmp[0].equals(page.getPageId()) && !listApplicationId.contains(nodeApplicationId)) {
          saveRemovedApplication(page, nodeApplicationId, content, remoteUser, plugin);
        }
      }
    }
  }

  /**
   * Gets the list node by application id.
   *
   * @param page the page
   * @param plugin
   *
   * @return the list node by application id
   *
   * @throws Exception the exception
   */
  private List<Node> getListNodeByApplicationId(Page page, WebpagePublicationPlugin plugin) throws Exception {
    RepositoryService repositoryService = WCMCoreUtils.getService(RepositoryService.class);
    WCMConfigurationService configurationService = WCMCoreUtils.getService(WCMConfigurationService.class);
    ManageableRepository repository = repositoryService.getCurrentRepository();
    NodeLocation nodeLocation = configurationService.getLivePortalsLocation(repository.getConfiguration().getName());

    String workspaceName = nodeLocation.getWorkspace();
    String path = nodeLocation.getPath();
    SessionProvider sessionProvider = WCMCoreUtils.getSystemSessionProvider();
    Session session = sessionProvider.getSession(workspaceName, repositoryService.getCurrentRepository());

    List<Node> listPublishedNode = new ArrayList<Node>();
    QueryManager queryManager = session.getWorkspace().getQueryManager();
    Query query = queryManager.createQuery("select * from " + plugin.getLifecycleType()
        + " where publication:lifecycleName='" + lifecycleName
        + "' and publication:webPageIDs like '%" + page.getPageId() + "%' and jcr:path like '"
        + path + "/%' order by jcr:score", Query.SQL);
    QueryResult results = query.execute();
    for (NodeIterator nodeIterator = results.getNodes(); nodeIterator.hasNext();) {
      listPublishedNode.add(nodeIterator.nextNode());
    }
    return listPublishedNode;
  }

  /**
   * Save added application.
   *
   * @param page the page
   * @param applicationId the application id
   * @param content the content
   * @param lifecycleName the lifecycle name
   * @param remoteUser the remote user
   * @param plugin
   * @throws Exception the exception
   */
  private void saveAddedApplication(
      Page page, String applicationId, Node content, String lifecycleName,
      String remoteUser, WebpagePublicationPlugin plugin) throws Exception {
    if (!content.isCheckedOut()) content.checkout();
    PublicationService publicationService = WCMCoreUtils.getService(PublicationService.class);
    String nodeLifecycleName = null;
    try {
      nodeLifecycleName = publicationService.getNodeLifecycleName(content);
    } catch (NotInPublicationLifecycleException e) { return; }
    if (!lifecycleName.equals(nodeLifecycleName)) return;

    Session session = content.getSession();
    ValueFactory valueFactory = session.getValueFactory();

    if (content.canAddMixin("publication:webpagesPublication"))
      content.addMixin("publication:webpagesPublication");

    List<String> nodeAppIds = PublicationUtil.getValuesAsString(content, "publication:applicationIDs");
    String mixedAppId = PublicationUtil.setMixedApplicationId(page.getPageId(), applicationId);
    if(nodeAppIds.contains(mixedAppId))
      return;

    List<String> listExistedNavigationNodeUri = PublicationUtil.getValuesAsString(content,
                                                                                  "publication:navigationNodeURIs");
    List<String> listPageNavigationUri = plugin.getListUserNavigationUri(page, remoteUser);
    if (listPageNavigationUri.isEmpty())  {
      return ;
    }
    for (String uri : listPageNavigationUri) {
        listExistedNavigationNodeUri.add(uri);
    }
    content.setProperty("publication:navigationNodeURIs",
                        PublicationUtil.toValues(valueFactory, listExistedNavigationNodeUri));

    List<String> nodeWebPageIds = PublicationUtil.getValuesAsString(content, "publication:webPageIDs");
    nodeWebPageIds.add(page.getPageId());
    nodeAppIds.add(mixedAppId);
    content.setProperty("publication:applicationIDs", PublicationUtil.toValues(valueFactory, nodeAppIds));
    content.setProperty("publication:webPageIDs", PublicationUtil.toValues(valueFactory, nodeWebPageIds));
    session.save();
  }

  /**
   * Save removed application.
   *
   * @param page the page
   * @param applicationId the application id
   * @param content the content
   * @param remoteUser the remote user
   * @param plugin
   * @throws Exception the exception
   */
  private void saveRemovedApplication(Page page,
                                      String applicationId,
                                      Node content,
                                      String remoteUser,
                                      WebpagePublicationPlugin plugin) throws Exception {
    if (!content.isCheckedOut()) content.checkout();
    Session session = content.getSession();
    ValueFactory valueFactory = session.getValueFactory();

    List<String> listExistedApplicationId = PublicationUtil.getValuesAsString(content, "publication:applicationIDs");
    listExistedApplicationId.remove(PublicationUtil.setMixedApplicationId(page.getPageId(), applicationId));
    content.setProperty("publication:applicationIDs", PublicationUtil.toValues(valueFactory, listExistedApplicationId));

    List<String> listExistedPageId = PublicationUtil.getValuesAsString(content, "publication:webPageIDs");
    listExistedPageId.remove(page.getPageId());
    content.setProperty("publication:webPageIDs", PublicationUtil.toValues(valueFactory, listExistedPageId));

    List<String> listPageNavigationUri = plugin.getListUserNavigationUri(page, remoteUser);
    List<String> listExistedNavigationNodeUri = PublicationUtil.getValuesAsString(content,
                                                                                  "publication:navigationNodeURIs");
    List<String> listExistedNavigationNodeUriTmp = new ArrayList<String>();
    listExistedNavigationNodeUriTmp.addAll(listExistedNavigationNodeUri);
    for (String existedNavigationNodeUri : listExistedNavigationNodeUriTmp) {
      if (listPageNavigationUri.contains(existedNavigationNodeUri)) {
        listExistedNavigationNodeUri.remove(existedNavigationNodeUri);
        break;
      }
    }
    content.setProperty("publication:navigationNodeURIs",
                        PublicationUtil.toValues(valueFactory, listExistedNavigationNodeUri));
    session.save();
  }
}
