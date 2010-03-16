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
import java.util.List;

import javax.jcr.Node;
import javax.portlet.PortletPreferences;

import org.exoplatform.commons.utils.ObjectPageList;
import org.exoplatform.resolver.ResourceResolver;
import org.exoplatform.wcm.webui.Utils;
import org.exoplatform.web.application.ApplicationMessage;
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
     @EventConfig(listeners = UICLVManualMode.QuickEditActionListener.class) 
   }
)
@SuppressWarnings("deprecation")
public class UICLVManualMode extends UICLVContainer {

  /* (non-Javadoc)
   * @see org.exoplatform.wcm.webui.clv.UICLVContainer#init()
   */
	@SuppressWarnings("unchecked")
  public void init() throws Exception {                       
    PortletPreferences portletPreferences = getPortletPreference();
    String repositoryName = portletPreferences.getValue(UICLVPortlet.REPOSITORY, null);
    String workspaceName = portletPreferences.getValue(UICLVPortlet.WORKSPACE, null);
    String[] listContent = portletPreferences.getValues(UICLVPortlet.CONTENT_LIST, null);
    if (listContent == null || listContent.length == 0) {
      messageKey = "UIMessageBoard.msg.contents-not-found";
      return;
    }
    int itemsPerPage = Integer.parseInt(portletPreferences.getValue(UICLVPortlet.ITEMS_PER_PAGE, null));
    List<Node> nodes = new ArrayList<Node>();
    if (listContent != null && listContent.length != 0) {
      for (String path : listContent) {
        try {
        	Node viewNode = Utils.getViewableNodeByComposer(repositoryName, workspaceName, path);
        	if (viewNode != null) nodes.add(viewNode);    
        } catch (Exception e) {
          Utils.createPopupMessage(this, "UIMessageBoard.msg.add-node-error", null, ApplicationMessage.ERROR);
        }
      }
    }
    if (nodes.size() == 0) {
      messageKey = "UIMessageBoard.msg.contents-not-found";
    }    
    getChildren().clear();
    ObjectPageList pageList = new ObjectPageList(nodes, itemsPerPage);    
    String templatePath = getFormViewTemplatePath();
    ResourceResolver resourceResolver = getTemplateResourceResolver();
    UICLVPresentation clvPresentation = addChild(UICLVPresentation.class, null, null);
    clvPresentation.init(templatePath, resourceResolver, pageList);    
    clvPresentation.setContentColumn(portletPreferences.getValue(UICLVPortlet.HEADER, null));
    clvPresentation.setShowLink(Boolean.parseBoolean(portletPreferences.getValue(UICLVPortlet.SHOW_LINK, null)));
    clvPresentation.setShowHeader(Boolean.parseBoolean(portletPreferences.getValue(UICLVPortlet.SHOW_HEADER, null)));
    clvPresentation.setShowReadmore(Boolean.parseBoolean(portletPreferences.getValue(UICLVPortlet.SHOW_READMORE, null)));
    clvPresentation.setHeader(portletPreferences.getValue(UICLVPortlet.HEADER, null));
  }  
}
