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

import org.exoplatform.portal.config.UserPortalConfigService;
import org.exoplatform.portal.webui.container.UIContainer;
import org.exoplatform.portal.webui.page.UIPageBody;
import org.exoplatform.portal.webui.portal.UIPortal;
import org.exoplatform.portal.webui.util.Util;
import org.exoplatform.services.wcm.core.NodeIdentifier;
import org.exoplatform.services.wcm.publication.NotInWCMPublicationException;
import org.exoplatform.services.wcm.publication.WCMPublicationService;
import org.exoplatform.wcm.webui.Utils;
import org.exoplatform.wcm.webui.dialog.UIContentDialogForm;
import org.exoplatform.wcm.webui.selector.UISelectPathPanel;
import org.exoplatform.wcm.webui.selector.content.UIContentBrowsePanel;
import org.exoplatform.web.application.ApplicationMessage;
import org.exoplatform.webui.application.WebuiRequestContext;
import org.exoplatform.webui.application.portlet.PortletRequestContext;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.ComponentConfigs;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.lifecycle.Lifecycle;
import org.exoplatform.wcm.webui.Utils;

/**
 * Created by The eXo Platform SAS.
 * 
 * @author : Hoa.Pham hoa.pham@exoplatform.com Jun 23, 2008
 */
@ComponentConfigs({
  @ComponentConfig(
      lifecycle = Lifecycle.class,
      template = "classpath:groovy/wcm/webui/selector/content/one/UIContentBrowsePanel.gtmpl",
      events = {
        @EventConfig(listeners = UIContentBrowsePanelOne.ChangeContentTypeActionListener.class)
      }
  ),
  @ComponentConfig(
      type = UISelectPathPanel.class,
      id = "UIContentBrowsePathSelector",
      template = "classpath:groovy/wcm/webui/selector/content/UIContentBrowsePathSelector.gtmpl",
      events = @EventConfig(listeners = UISelectPathPanel.SelectActionListener.class)
  )
})
public class UIContentBrowsePanelOne extends UIContentBrowsePanel{

  /**
   * Instantiates a new uI content browse panel one.
   * 
   * @throws Exception the exception
   */
  public UIContentBrowsePanelOne() throws Exception {
    super();
    addChild(org.exoplatform.wcm.webui.selector.content.UIContentTreeBuilder.class,null, org.exoplatform.wcm.webui.selector.content.UIContentTreeBuilder.class.getName()+hashCode());
    addChild(UISelectPathPanel.class, "UIContentBrowsePathSelector", "UIContentBrowsePathSelector");
  }
  
  /* (non-Javadoc)
   * @see org.exoplatform.wcm.webui.selector.content.UIContentBrowsePanel#doSelect(javax.jcr.Node, org.exoplatform.webui.application.WebuiRequestContext)
   */
  public void doSelect(Node node, WebuiRequestContext requestContext) throws Exception{
  	Node realNode = node;
  	if (node.isNodeType("exo:symlink")) {
  		String uuid = node.getProperty("exo:uuid").getString();
  		realNode = node.getSession().getNodeByUUID(uuid);
  	}
  	if(!realNode.isCheckedOut()){
  	  Utils.createPopupMessage(this, "UIContentBrowsePanelOne.msg.node-checkout", null, ApplicationMessage.WARNING);
  	  return;
  	}  	
	  NodeIdentifier nodeIdentifier = NodeIdentifier.make(realNode);
	  PortletRequestContext pContext = (PortletRequestContext) requestContext;
	  PortletPreferences prefs = pContext.getRequest().getPreferences();
	  prefs.setValue("repository", nodeIdentifier.getRepository());
	  prefs.setValue("workspace", nodeIdentifier.getWorkspace());
	  prefs.setValue("nodeIdentifier", nodeIdentifier.getUUID());
	  prefs.store();
	  
	  String remoteUser = Util.getPortalRequestContext().getRemoteUser();
	  String portalOwner = Util.getPortalRequestContext().getPortalOwner();
	  
	  WCMPublicationService wcmPublicationService = this.getApplicationComponent(WCMPublicationService.class);
	  
	  try {
	    wcmPublicationService.isEnrolledInWCMLifecycle(realNode);
	  } catch (NotInWCMPublicationException e){
	    wcmPublicationService.unsubcribeLifecycle(realNode);
	    wcmPublicationService.enrollNodeInLifecycle(realNode, portalOwner, remoteUser);          
	  }
	  
	  wcmPublicationService.updateLifecyleOnChangeContent(realNode, portalOwner, remoteUser, null);
	  if (!Utils.isEditPortletInCreatePageWizard()) {
	    String pageId = Util.getUIPortal().getSelectedNode().getPageReference();
	    UserPortalConfigService upcService = getApplicationComponent(UserPortalConfigService.class);
	    wcmPublicationService.updateLifecyleOnChangePage(upcService.getPage(pageId), remoteUser);
	  }
	  
	  // Update Page And Close PopUp
	  UIPortal uiPortal = Util.getUIPortal();
	  UIPageBody uiPageBody = uiPortal.findFirstComponentOfType(UIPageBody.class);
	  uiPageBody.setUIComponent(null);
	  uiPageBody.setMaximizedUIComponent(null);
	  Utils.updatePortal((PortletRequestContext)requestContext);
	  Utils.closePopupWindow(this, UIContentDialogForm.CONTENT_DIALOG_FORM_POPUP_WINDOW);
  }
}
