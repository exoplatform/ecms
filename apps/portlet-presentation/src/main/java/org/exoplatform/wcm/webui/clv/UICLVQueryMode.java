
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

import javax.jcr.Node;
import javax.jcr.query.Query;
import javax.portlet.PortletPreferences;

import org.apache.commons.lang.StringUtils;
import org.exoplatform.portal.webui.application.UIPortlet;
import org.exoplatform.portal.webui.util.Util;
import org.exoplatform.resolver.ResourceResolver;
import org.exoplatform.services.wcm.core.NodeLocation;
import org.exoplatform.services.wcm.search.base.AbstractPageList;
import org.exoplatform.services.wcm.search.base.PageListFactory;
import org.exoplatform.wcm.webui.Utils;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.lifecycle.Lifecycle;

/*
 * Created by The eXo Platform SAS Author : Nguyen The Vinh from ECMS
 * July 28, 2011
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
public class UICLVQueryMode extends UICLVContainer {

  private UICLVPresentation clvPresentation;

  /* (non-Javadoc)
   * @see org.exoplatform.wcm.webui.clv.UICLVContainer#init()
   */
  public void init() throws Exception {
    PortletPreferences portletPreferences = Utils.getAllPortletPreferences();
    String queryStatement = portletPreferences.getValue(UICLVPortlet.PREFERENCE_CONTENT_BY_QUERY, "");
    String workspace = portletPreferences.getValue(UICLVPortlet.PREFERENCE_WORKSPACE, null);
    String queryLanguage = Query.SQL;
    int itemsPerPage = Integer.parseInt(portletPreferences.getValue(UICLVPortlet.PREFERENCE_ITEMS_PER_PAGE, null));
    AbstractPageList<NodeLocation> pageList = null;
    if (queryStatement.indexOf(UICLVPortlet.QUERY_USER_PARAMETER)>0) {
      String userId = Util.getPortalRequestContext().getRemoteUser();
      queryStatement = StringUtils.replace(queryStatement, UICLVPortlet.QUERY_USER_PARAMETER, userId);
    }
    if (queryStatement.indexOf(UICLVPortlet.QUERY_FOLDER_ID_PARAMETER)>0) {
      String folderPath = this.getAncestorOfType(UICLVPortlet.class).getFolderPath();
      queryStatement = StringUtils.replace(queryStatement, UICLVPortlet.QUERY_FOLDER_ID_PARAMETER, folderPath);
    }
    if (queryStatement.indexOf(UICLVPortlet.QUERY_LANGUAGE_PARAMETER)>0) {
      String currentLanguage =  Util.getPortalRequestContext().getLocale().getLanguage();
      queryStatement = StringUtils.replace(queryStatement, UICLVPortlet.QUERY_LANGUAGE_PARAMETER, currentLanguage);
    }
    try {
      pageList = PageListFactory.createPageList(queryStatement, workspace, queryLanguage, false, null, 
                                     new CLVNodeCreator(), itemsPerPage, AbstractPageList.DEAFAULT_BUFFER_SIZE);
    }catch (Exception e) {
      //No data found because of wrong query
      pageList = PageListFactory.createPageList(new ArrayList<Node>(), 0, null, null);
    }
    getChildren().clear();
    clvPresentation = addChild(UICLVPresentation.class, null, null);
    ResourceResolver resourceResolver = getTemplateResourceResolver();
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
}