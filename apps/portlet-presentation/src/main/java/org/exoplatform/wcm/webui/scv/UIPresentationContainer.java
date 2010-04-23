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
package org.exoplatform.wcm.webui.scv;

import java.text.SimpleDateFormat;
import java.util.Calendar;

import javax.jcr.Node;
import javax.portlet.PortletPreferences;

import org.exoplatform.portal.webui.util.Util;
import org.exoplatform.services.wcm.core.WCMConfigurationService;
import org.exoplatform.services.wcm.publication.WCMComposer;
import org.exoplatform.wcm.webui.Utils;
import org.exoplatform.wcm.webui.WebUIPropertiesConfigService;
import org.exoplatform.wcm.webui.WebUIPropertiesConfigService.PopupWindowProperties;
import org.exoplatform.wcm.webui.dialog.UIContentDialogForm;
import org.exoplatform.wcm.webui.scv.config.UIPortletConfig;
import org.exoplatform.webui.application.WebuiRequestContext;
import org.exoplatform.webui.application.portlet.PortletRequestContext;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIContainer;
import org.exoplatform.webui.core.lifecycle.Lifecycle;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.EventListener;

/**
 * Author : Do Ngoc Anh *
 * Email: anh.do@exoplatform.com *
 * May 14, 2008
 */
@ComponentConfig(
		lifecycle=Lifecycle.class,
		template="app:/groovy/SingleContentViewer/UIPresentationContainer.gtmpl",
		events = {
			@EventConfig(listeners=UIPresentationContainer.QuickEditActionListener.class)
		}
)
public class UIPresentationContainer extends UIContainer{

	/**
	 * Instantiates a new uI presentation container.
	 * 
	 * @throws Exception the exception
	 */
	public UIPresentationContainer() throws Exception{   	  
		addChild(UIPresentation.class, null, null);
	}

	/**
	 * Gets the title.
	 * 
	 * @param node the node
	 * 
	 * @return the title
	 * 
	 * @throws Exception the exception
	 */
	public String getTitle(Node node) throws Exception {
		String title = null;
		if (node.hasProperty("exo:title")) {
			title = node.getProperty("exo:title").getValue().getString();
		}
		if (node.hasNode("jcr:content")) {
			Node content = node.getNode("jcr:content");
			if (content.hasProperty("dc:title")) {
				try {
					title = content.getProperty("dc:title").getValues()[0].getString();
				} catch (Exception e) {
					title = null;
				}
			}
		}
		if (title==null) title = node.getName();

		return title;
	}

	/**
	 * Gets the created date.
	 * 
	 * @param node the node
	 * 
	 * @return the created date
	 * 
	 * @throws Exception the exception
	 */
	public String getCreatedDate(Node node) throws Exception {
		if (node.hasProperty("exo:dateCreated")) {
			Calendar calendar = node.getProperty("exo:dateCreated").getValue().getDate();
			return new SimpleDateFormat("dd.MM.yyyy '|' hh'h'mm").format(calendar.getTime());
		}
		return null;
	}

	/**
	 * Gets the node.
	 * 
	 * @return the node
	 * 
	 * @throws Exception the exception
	 */
	public Node getNodeView() {
		try {
			UIPresentation presentation = getChild(UIPresentation.class);
			PortletRequestContext portletRequestContext = WebuiRequestContext.getCurrentInstance();
			PortletPreferences preferences = portletRequestContext.getRequest().getPreferences();
			String repository = preferences.getValue(UISingleContentViewerPortlet.REPOSITORY, null);    
			String workspace = preferences.getValue(UISingleContentViewerPortlet.WORKSPACE, null);
			String nodeIdentifier = preferences.getValue(UISingleContentViewerPortlet.IDENTIFIER, null) ;
			Node viewNode = Utils.getViewableNodeByComposer(repository, workspace, nodeIdentifier);
			presentation.setNode(viewNode);
			Node orgNode = Utils.getViewableNodeByComposer(repository, workspace, nodeIdentifier, WCMComposer.BASE_VERSION);
			presentation.setOriginalNode(orgNode);
			return viewNode;
		} catch (Exception e) {
			return null;
		}
	} 

	/**
	 * Get the print's page URL
	 * 
	 * @return <code>true</code> if the Quick Print is shown. Otherwise, <code>false</code>
	 */
	public String getPrintUrl() {
		PortletRequestContext portletRequestContext = WebuiRequestContext.getCurrentInstance();
		PortletPreferences preferences = portletRequestContext.getRequest().getPreferences();
		String repository = preferences.getValue(UISingleContentViewerPortlet.REPOSITORY, null);    
		String workspace = preferences.getValue(UISingleContentViewerPortlet.WORKSPACE, null);
		String path = preferences.getValue(UISingleContentViewerPortlet.IDENTIFIER, null) ;
		if (!path.startsWith("/")) path = "/" + path;

		String portalURI = Util.getPortalRequestContext().getPortalURI();
		WCMConfigurationService wcmConfigurationService = getApplicationComponent(WCMConfigurationService.class);
		String printPageUrl = wcmConfigurationService.getRuntimeContextParam("printViewerPage");
		String printUrl = portalURI + printPageUrl + "/" + repository + "/" + workspace + path + "&isPrint=true";
		return printUrl;
	}

	/**
	 * The listener interface for receiving quickEditAction events.
	 * The class that is interested in processing a quickEditAction
	 * event implements this interface, and the object created
	 * with that class is registered with a component using the
	 * component's <code>addQuickEditActionListener<code> method. When
	 * the quickEditAction event occurs, that object's appropriate
	 * method is invoked.
	 * 
	 * @see QuickEditActionEvent
	 */
	public static class QuickEditActionListener extends EventListener<UIPresentationContainer>{   
		/* (non-Javadoc)
		 * @see org.exoplatform.webui.event.EventListener#execute(org.exoplatform.webui.event.Event)
		 */
		public void execute(Event<UIPresentationContainer> event) throws Exception {
			UIPresentationContainer presentationContainer = event.getSource();
			UIPortletConfig portletConfig = presentationContainer.createUIComponent(UIPortletConfig.class, null, null);
			WebUIPropertiesConfigService propertiesConfigService = presentationContainer.getApplicationComponent(WebUIPropertiesConfigService.class);
			PopupWindowProperties popupProperties = (PopupWindowProperties)propertiesConfigService.getProperties(WebUIPropertiesConfigService.SCV_POPUP_SIZE_QUICK_EDIT);
			Utils.createPopupWindow(presentationContainer, portletConfig, UIContentDialogForm.CONTENT_DIALOG_FORM_POPUP_WINDOW, popupProperties.getWidth());
			portletConfig.init();
		}
	}
}