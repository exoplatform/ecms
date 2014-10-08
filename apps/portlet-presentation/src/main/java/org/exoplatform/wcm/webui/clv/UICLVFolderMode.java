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
package org.exoplatform.wcm.webui.clv;

import java.util.ArrayList;
import java.util.HashMap;

import javax.jcr.AccessDeniedException;
import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.query.Query;
import javax.portlet.PortletPreferences;

import org.exoplatform.portal.webui.application.UIPortlet;
import org.exoplatform.portal.webui.util.Util;
import org.exoplatform.resolver.ResourceResolver;
import org.exoplatform.services.wcm.core.NodeLocation;
import org.exoplatform.services.wcm.publication.NodeLocationPaginatedResultIterator;
import org.exoplatform.services.wcm.publication.PaginatedResultIterator;
import org.exoplatform.services.wcm.publication.Result;
import org.exoplatform.services.wcm.publication.WCMComposer;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;
import org.exoplatform.wcm.webui.Utils;
import org.exoplatform.webui.application.WebuiRequestContext;
import org.exoplatform.webui.application.portlet.PortletRequestContext;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.lifecycle.Lifecycle;

/*
 * Created by The eXo Platform SAS Author : Anh Do Ngoc anh.do@exoplatform.com
 * Oct 15, 2008
 */

/**
 * The Class UICLVFolderMode.
 */
@ComponentConfig(
                 lifecycle = Lifecycle.class,
                 template = "app:/groovy/ContentListViewer/UICLVContainer.gtmpl",
                 events = {
                   @EventConfig(listeners = UICLVFolderMode.PreferencesActionListener.class)
                 }
    )
public class UICLVFolderMode extends UICLVContainer {

  private UICLVPresentation clvPresentation;

  /* (non-Javadoc)
   * @see org.exoplatform.wcm.webui.clv.UICLVContainer#init()
   */
  public void init() throws Exception {
    PortletPreferences portletPreferences = Utils.getAllPortletPreferences();

    Result result = null;
    messageKey = null;
    try {
      result = getRenderedContentNodes();
    } catch (ItemNotFoundException e) {
      messageKey = "UICLVContainer.msg.item-not-found";
      return;
    } catch (AccessDeniedException e) {
      messageKey = "UICLVContainer.msg.no-permission";
      result = new Result(new ArrayList<Node>(), 0, 0, null, null);
    }
    if (result.getNumTotal() == 0) {
      messageKey = "UICLVContainer.msg.non-contents";
    }
    int itemsPerPage = Integer.parseInt(portletPreferences.getValue(UICLVPortlet.PREFERENCE_ITEMS_PER_PAGE, null));
    PaginatedResultIterator paginatedResultIterator = new NodeLocationPaginatedResultIterator(result, itemsPerPage); 
    getChildren().clear();
    PortletRequestContext pContext = (PortletRequestContext) WebuiRequestContext.getCurrentInstance();
    clvPresentation =
        addChild(UICLVPresentation.class,
                 null,
                 UICLVPresentation.class.getSimpleName() + "_" + pContext.getWindowId()
            );
    ResourceResolver resourceResolver = getTemplateResourceResolver();
    clvPresentation.init(resourceResolver, paginatedResultIterator);
  }

  /**
   * Gets the rendered content nodes.
   *
   * @return the rendered content nodes
   *
   * @throws Exception the exception
   */
  public Result getRenderedContentNodes() throws Exception {
    PortletRequestContext portletRequestContext = WebuiRequestContext.getCurrentInstance();
    PortletPreferences preferences = portletRequestContext.getRequest().getPreferences();
    WCMComposer wcmComposer = getApplicationComponent(WCMComposer.class);
    HashMap<String, String> filters = new HashMap<String, String>();
    filters.put(WCMComposer.FILTER_MODE, Utils.getCurrentMode());
    String orderBy = preferences.getValue(UICLVPortlet.PREFERENCE_ORDER_BY, null);
    String orderType = preferences.getValue(UICLVPortlet.PREFERENCE_ORDER_TYPE, null);
    String itemsPerPage = preferences.getValue(UICLVPortlet.PREFERENCE_ITEMS_PER_PAGE, null);
    String sharedCache = preferences.getValue(UICLVPortlet.PREFERENCE_SHARED_CACHE, "true");
    String contextualMode = preferences.getValue(UICLVPortlet.PREFERENCE_CONTEXTUAL_FOLDER, "true");
    String workspace = preferences.getValue(UICLVPortlet.PREFERENCE_WORKSPACE, null);
    String query = preferences.getValue(UICLVPortlet.PREFERENCE_CONTENTS_BY_QUERY, null);
    if (orderType == null) orderType = "DESC";
    if (orderBy == null) orderBy = "exo:title";
    filters.put(WCMComposer.FILTER_ORDER_BY, orderBy);
    filters.put(WCMComposer.FILTER_ORDER_TYPE, orderType);
    StringBuffer filterLang = new StringBuffer(Util.getPortalRequestContext().getLocale().getLanguage());
    String country = Util.getPortalRequestContext().getLocale().getCountry();
    if (country != null && country.length() > 0) {
      filterLang.append("_").append(country);
    }
    filters.put(WCMComposer.FILTER_LANGUAGE, filterLang.toString());
    filters.put(WCMComposer.FILTER_LIMIT, itemsPerPage);
    filters.put(WCMComposer.FILTER_VISIBILITY, ("true".equals(sharedCache))?
                                                                            WCMComposer.VISIBILITY_PUBLIC:WCMComposer.VISIBILITY_USER);


    if (this.getAncestorOfType(UICLVPortlet.class).isQueryApplication()) {
      String folderPath = preferences.getValue(UICLVPortlet.PREFERENCE_ITEM_PATH, null);
      if (folderPath == null) {
        return new Result(new ArrayList<Node>(), 0, 0, null, null);
      }
      NodeLocation nodeLocation = new NodeLocation();
      nodeLocation.setWorkspace(workspace);
      nodeLocation.setPath("/");
      nodeLocation.setSystemSession(false);
      String strQuery = this.getAncestorOfType(UICLVPortlet.class).getQueryStatement(query);
      strQuery = strQuery.replaceAll("\"", "'");
      if (UICLVPortlet.PREFERENCE_CONTEXTUAL_FOLDER_ENABLE.equals(contextualMode)
          && org.exoplatform.wcm.webui.Utils.checkQuery(workspace, strQuery, Query.SQL)) {
        filters.put(WCMComposer.FILTER_QUERY_FULL, strQuery);
        return wcmComposer.getPaginatedContents(nodeLocation,
                                                filters,
                                                WCMCoreUtils.getUserSessionProvider());
      }
    }
    String folderPath = this.getAncestorOfType(UICLVPortlet.class).getFolderPath();
    if (folderPath == null) {
      folderPath = preferences.getValue(UICLVPortlet.PREFERENCE_ITEM_PATH, null);
    }
    if (folderPath == null) {
      return new Result(new ArrayList<Node>(), 0, 0, null, null);
    }
    NodeLocation nodeLocation = NodeLocation.getNodeLocationByExpression(folderPath);
    Node targetNode = NodeLocation.getNodeByLocation(nodeLocation);
    //check if folder is empty, return empty result
    if (targetNode == null || !targetNode.hasNodes()) {
      return new Result(new ArrayList<Node>(), 0, 0, nodeLocation, filters);
    } else {
      return wcmComposer.getPaginatedContents(nodeLocation,
                                              filters,
                                              WCMCoreUtils.getUserSessionProvider());
    }
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
    return UICLVFolderMode.class.getSimpleName();
  }
}
