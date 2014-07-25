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
package org.exoplatform.wcm.webui.clv;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.query.Query;
import javax.portlet.PortletPreferences;

import org.exoplatform.ecm.utils.text.Text;
import org.exoplatform.ecm.utils.comparator.PropertyValueComparator;
import org.exoplatform.portal.webui.application.UIPortlet;
import org.exoplatform.portal.webui.util.Util;
import org.exoplatform.resolver.ResourceResolver;
import org.exoplatform.services.cms.link.LinkManager;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.wcm.core.NodeLocation;
import org.exoplatform.services.wcm.core.NodetypeConstant;
import org.exoplatform.services.wcm.publication.PaginatedResultIterator;
import org.exoplatform.services.wcm.publication.Result;
import org.exoplatform.services.wcm.publication.WCMComposer;
import org.exoplatform.services.wcm.search.base.AbstractPageList;
import org.exoplatform.services.wcm.search.base.PageListFactory;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;
import org.exoplatform.wcm.webui.Utils;
import org.exoplatform.wcm.webui.scv.UISingleContentViewerPortlet;
import org.exoplatform.webui.application.WebuiRequestContext;
import org.exoplatform.webui.application.portlet.PortletRequestContext;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.lifecycle.Lifecycle;

/**
 * Created by The eXo Platform SAS
 * Author : anh.do
 * anh.do@exoplatform.com, anhdn86@gmail.com
 * Feb 23, 2009
 */
@ComponentConfig(
                 lifecycle = Lifecycle.class,
                 template = "app:/groovy/ContentListViewer/UICLVContainer.gtmpl",
                 events = {
                   @EventConfig(listeners = UICLVManualMode.PreferencesActionListener.class)
                 }
    )
@SuppressWarnings("deprecation")
public class UICLVManualMode extends UICLVContainer {

  /** The log. */
  private static final Log LOG = ExoLogger.getLogger(UICLVManualMode.class.getName());

  /* (non-Javadoc)
   * @see org.exoplatform.wcm.webui.clv.UICLVContainer#init()
   */
  @SuppressWarnings("unchecked")
  public void init() throws Exception {
    PortletPreferences portletPreferences = Utils.getAllPortletPreferences();
    String query = portletPreferences.getValue(UICLVPortlet.PREFERENCE_CONTENTS_BY_QUERY, "");
    String contextualMode = portletPreferences.getValue(UICLVPortlet.PREFERENCE_CONTEXTUAL_FOLDER, null);
    String workspace = portletPreferences.getValue(UICLVPortlet.PREFERENCE_WORKSPACE, null);
    List<Node> nodes = new ArrayList<Node>();
    String folderPath="";

    HashMap<String, String> filters = new HashMap<String, String>();
    if (UICLVPortlet.PREFERENCE_CONTEXTUAL_FOLDER_ENABLE.equals(contextualMode)) {
      String folderParamName = portletPreferences.getValue(UICLVPortlet.PREFERENCE_SHOW_CLV_BY, null);
      if (folderParamName == null || folderParamName.length() == 0)
        folderParamName = UICLVPortlet.DEFAULT_SHOW_CLV_BY;
      folderPath = Util.getPortalRequestContext().getRequestParameter(folderParamName);
    }
    String sharedCache = portletPreferences.getValue(UISingleContentViewerPortlet.ENABLE_CACHE, "true");
    sharedCache = "true".equals(sharedCache) ? WCMComposer.VISIBILITY_PUBLIC:WCMComposer.VISIBILITY_USER;
    int itemsPerPage = Integer.parseInt(portletPreferences.getValue(UICLVPortlet.PREFERENCE_ITEMS_PER_PAGE, null));

    String strQuery = this.getAncestorOfType(UICLVPortlet.class).getQueryStatement(query);
    if (strQuery != null) strQuery = strQuery.replaceAll("\"", "'");
    if (this.getAncestorOfType(UICLVPortlet.class).isQueryApplication()
        && UICLVPortlet.PREFERENCE_CONTEXTUAL_FOLDER_ENABLE.equals(contextualMode)
        && org.exoplatform.wcm.webui.Utils.checkQuery(workspace, strQuery, Query.SQL)) {
      NodeLocation nodeLocation = new NodeLocation();
      nodeLocation.setWorkspace(workspace);
      nodeLocation.setPath("/");
      nodeLocation.setSystemSession(false);
      filters.put(WCMComposer.FILTER_QUERY_FULL, strQuery);
      Result rNodes = WCMCoreUtils.getService(WCMComposer.class)
          .getPaginatedContents(nodeLocation, filters, WCMCoreUtils.getUserSessionProvider());
      PaginatedResultIterator paginatedResultIterator = new PaginatedResultIterator(rNodes, itemsPerPage);
      getChildren().clear();
      ResourceResolver resourceResolver = getTemplateResourceResolver();
      PortletRequestContext pContext = (PortletRequestContext) WebuiRequestContext.getCurrentInstance();
      UICLVPresentation clvPresentation =
          addChild(UICLVPresentation.class,
                   null,
                   UICLVPresentation.class.getSimpleName() + "_" + pContext.getWindowId()
              );
      clvPresentation.init(resourceResolver, paginatedResultIterator);

      return;
    } else {
      String[] listContent = portletPreferences.getValue(UICLVPortlet.PREFERENCE_ITEM_PATH, null).split(";");
      LinkManager linkManager = WCMCoreUtils.getService(LinkManager.class);
      //get node to sort
      List<Node> originalList = new ArrayList<Node>();
      if (listContent != null && listContent.length != 0) {
        for (String itemPath : listContent) {
          Node currentNode = NodeLocation.getNodeByExpression(itemPath);
          if(currentNode != null){
            try {
              linkManager.updateSymlink(currentNode);
              currentNode = NodeLocation.getNodeByExpression(itemPath);
            } catch (Exception e) {
              if (LOG.isErrorEnabled()) {
                LOG.error("Can not update symlink: " + currentNode.getPath(), e);
              }
            }
            originalList.add(currentNode);
          }
        }
      }
      //sort nodes
      String orderBy = portletPreferences.getValue(UICLVPortlet.PREFERENCE_ORDER_BY, NodetypeConstant.EXO_TITLE);
      String orderType = portletPreferences.getValue(UICLVPortlet.PREFERENCE_ORDER_TYPE, "ASC");

      Collections.sort(originalList, new PropertyValueComparator(orderBy, "ASC".equals(orderType) ? "Ascending" : "Descending"));
      //get real node by portlet mode
      for (Node node : originalList) {
        Node viewNode = Utils.getViewableNodeByComposer(WCMCoreUtils.getRepository().getConfiguration().getName(),
                                                        Text.escapeIllegalJcrChars(node.getSession().getWorkspace().getName()), 
                                                        Text.escapeIllegalJcrChars(node.getPath()),
                                                        null,
                                                        sharedCache);
        if (viewNode != null) nodes.add(viewNode);
      }
    }
    if (nodes.size() == 0) {
      messageKey = "UICLVContainer.msg.non-contents";
    }
    getChildren().clear();
    AbstractPageList<NodeLocation> pageList = 
        PageListFactory.createPageList(nodes, itemsPerPage, null, 
                                       new CLVNodeCreator());
    ResourceResolver resourceResolver = getTemplateResourceResolver();
    PortletRequestContext pContext = (PortletRequestContext) WebuiRequestContext.getCurrentInstance();
    UICLVPresentation clvPresentation =
        addChild(UICLVPresentation.class,
                 null,
                 UICLVPresentation.class.getSimpleName() + "_" + pContext.getWindowId()
            );
    clvPresentation.init(resourceResolver, pageList);
  }
  /**
   * Gets the bar info show.
   *
   * @return the value for info bar setting
   *
   * @throws Exception the exception
   */
  public boolean isShowInfoBar() throws Exception {
    if (UIPortlet.getCurrentUIPortlet().getShowInfoBar())
      return true;
    return false;
  }
  /**
   * Get portlet name.
   *
   * @throws Exception the exception
   */
  public String getPortletName() throws Exception {
    return UICLVManualMode.class.getSimpleName();
  }
}
