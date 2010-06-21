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
package org.exoplatform.wcm.webui.selector.content.one;

import javax.jcr.Node;
import javax.portlet.PortletPreferences;

import org.exoplatform.services.wcm.core.NodeIdentifier;
import org.exoplatform.services.wcm.core.NodeLocation;
import org.exoplatform.wcm.webui.Utils;
import org.exoplatform.wcm.webui.dialog.UIContentDialogForm;
import org.exoplatform.wcm.webui.selector.content.UIContentBrowsePanel;
import org.exoplatform.web.application.ApplicationMessage;
import org.exoplatform.webui.application.portlet.PortletRequestContext;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.lifecycle.Lifecycle;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.EventListener;

/**
 * Created by The eXo Platform SAS.
 * 
 * @author : Hoa.Pham hoa.pham@exoplatform.com Jun 23, 2008
 */
@ComponentConfig(
  lifecycle = Lifecycle.class,
  template = "classpath:groovy/wcm/webui/selector/content/one/UIContentBrowsePanel.gtmpl",
  events = {
    @EventConfig(listeners = UIContentBrowsePanel.ChangeContentTypeActionListener.class),
    @EventConfig(listeners = UIContentBrowsePanelOne.SelectActionListener.class)
  }
)
public class UIContentBrowsePanelOne extends UIContentBrowsePanel{

  public static class SelectActionListener extends EventListener<UIContentBrowsePanel> {
    public void execute(Event<UIContentBrowsePanel> event) throws Exception {
      UIContentBrowsePanel contentBrowsePanel = event.getSource();
      String itemPath = event.getRequestContext().getRequestParameter(OBJECTID);
      Node node = NodeLocation.getNodeByExpression(itemPath);
      Node realNode = node;
      if (node.isNodeType("exo:symlink")) {
    	String uuid = node.getProperty("exo:uuid").getString();
    	realNode = node.getSession().getNodeByUUID(uuid);
      }
      if(!realNode.isCheckedOut()){
        Utils.createPopupMessage(contentBrowsePanel, "UIContentBrowsePanelOne.msg.node-checkout", null, ApplicationMessage.WARNING);
        return;
      }  	
      NodeIdentifier nodeIdentifier = NodeIdentifier.make(realNode);
      PortletRequestContext pContext = (PortletRequestContext) event.getRequestContext();
      PortletPreferences prefs = pContext.getRequest().getPreferences();
      prefs.setValue("repository", nodeIdentifier.getRepository());
      prefs.setValue("workspace", nodeIdentifier.getWorkspace());
      prefs.setValue("nodeIdentifier", nodeIdentifier.getUUID());
      prefs.store();
  
      Utils.closePopupWindow(contentBrowsePanel, UIContentDialogForm.CONTENT_DIALOG_FORM_POPUP_WINDOW);
    }
  }
}
