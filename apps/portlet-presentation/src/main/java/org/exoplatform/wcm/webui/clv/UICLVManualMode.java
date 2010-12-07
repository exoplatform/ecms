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
import org.exoplatform.ecm.utils.text.Text;
import org.exoplatform.portal.webui.application.UIPortlet;
import org.exoplatform.resolver.ResourceResolver;
import org.exoplatform.services.wcm.core.NodeLocation;
import org.exoplatform.wcm.webui.Utils;
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

  /* (non-Javadoc)
   * @see org.exoplatform.wcm.webui.clv.UICLVContainer#init()
   */
	@SuppressWarnings("unchecked")
  public void init() throws Exception {
	  PortletPreferences portletPreferences = Utils.getAllPortletPreferences();
    String[] listContent = portletPreferences.getValue(UICLVPortlet.PREFERENCE_ITEM_PATH, null).split(";");
    int itemsPerPage = Integer.parseInt(portletPreferences.getValue(UICLVPortlet.PREFERENCE_ITEMS_PER_PAGE, null));
    List<Node> nodes = new ArrayList<Node>();
    if (listContent != null && listContent.length != 0) {
      for (String itemPath : listContent) {
      	NodeLocation nodeLocation = NodeLocation.getNodeLocationByExpression(itemPath);
      	Node viewNode = Utils.getViewableNodeByComposer(nodeLocation.getRepository(), 
      	    Text.escapeIllegalJcrChars(nodeLocation.getWorkspace()), Text.escapeIllegalJcrChars(nodeLocation.getPath()));
      	if (viewNode != null) nodes.add(viewNode);    
      }
    }
    if (nodes.size() == 0) {
      messageKey = "UICLVContainer.msg.non-contents";
    }    
    getChildren().clear();
    ObjectPageList pageList = new ObjectPageList(nodes, itemsPerPage);    
    ResourceResolver resourceResolver = getTemplateResourceResolver();
    UICLVPresentation clvPresentation = addChild(UICLVPresentation.class, null, null);
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
