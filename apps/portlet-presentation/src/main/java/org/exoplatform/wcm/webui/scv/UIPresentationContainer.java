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
import javax.jcr.RepositoryException;
import javax.portlet.PortletPreferences;

import org.exoplatform.portal.application.PortalRequestContext;
import org.exoplatform.portal.webui.util.Util;
import org.exoplatform.services.wcm.core.WCMConfigurationService;
import org.exoplatform.wcm.webui.Utils;
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
		    @EventConfig(listeners=UIPresentationContainer.PreferencesActionListener.class)
		}
)
public class UIPresentationContainer extends UIContainer{
  
  private boolean isPrint = false;

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
	public boolean getIsPrint() {
	  return this.isPrint;
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
			Node viewNode = Utils.getRealNode(repository, workspace, nodeIdentifier, false);
			presentation.setNode(viewNode);
			Node orgNode = Utils.getRealNode(repository, workspace, nodeIdentifier, true);
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
		String printUrl = portalURI + printPageUrl + "?path=/" + repository + "/" + workspace + path + "&isPrint=true";
		return printUrl;
	}

	public String getQuickEditLink() throws RepositoryException{
    PortalRequestContext pContext = Util.getPortalRequestContext();
    String portalURI = pContext.getPortalURI();
    PortletPreferences portletPreferences = ((PortletRequestContext) WebuiRequestContext.getCurrentInstance()).getRequest().getPreferences();
    String strIdentifier = portletPreferences.getValue(UISingleContentViewerPortlet.IDENTIFIER, null);
    String strRepository  = portletPreferences.getValue(UISingleContentViewerPortlet.REPOSITORY, null);
    String strWorkspace = portletPreferences.getValue(UISingleContentViewerPortlet.WORKSPACE, null);
    String backto = pContext.getRequestURI();
    Node tempNode = Utils.getRealNode(strRepository, strWorkspace, strIdentifier, false);
    String strPath = tempNode.getPath();
	  StringBuilder link = new StringBuilder().append(portalURI).append("siteExplorer?").
                                              append("path=/").append(strRepository).
                                              append("/").append(strWorkspace).append(strPath).
                                              append("&backto=").append(backto).
                                              append("&edit=true");
	  return link.toString();
	}
	
	/**
   * The listener interface for receiving preferencesAction events.
   * The class that is interested in processing a preferencesAction
   * event implements this interface, and the object created
   * with that class is registered with a component using the
   * component's <code>addPreferencesActionListener<code> method. When
   * the preferencesAction event occurs, that object's appropriate
   * method is invoked.
   */ 
	public static class PreferencesActionListener extends EventListener<UIPresentationContainer>{   
    /* (non-Javadoc)
     * @see org.exoplatform.webui.event.EventListener#execute(org.exoplatform.webui.event.Event)
     */
    public void execute(Event<UIPresentationContainer> event) throws Exception {      
      UIPresentationContainer presentationContainer = event.getSource();
      UISCVPreferences pcvConfigForm = presentationContainer.createUIComponent(UISCVPreferences.class, null, null);
      Utils.createPopupWindow(presentationContainer, pcvConfigForm, UISingleContentViewerPortlet.UIPreferencesPopupID, 600);
    }
  }
}