/*
 * Copyright (C) 2003-2008 eXo Platform SAS. This program is free software; you
 * can redistribute it and/or modify it under the terms of the GNU Affero
 * General Public License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version. This program
 * is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU General Public License for more details. You
 * should have received a copy of the GNU General Public License along with this
 * program; if not, see<http://www.gnu.org/licenses/>.
 */
package org.exoplatform.wcm.webui.search;

import java.net.URLEncoder;

import javax.portlet.PortletPreferences;
import javax.servlet.http.HttpServletRequest;

import org.exoplatform.ecm.resolver.JCRResourceResolver;
import org.exoplatform.portal.application.PortalRequestContext;
import org.exoplatform.portal.webui.portal.UIPortal;
import org.exoplatform.portal.webui.util.Util;
import org.exoplatform.resolver.ResourceResolver;
import org.exoplatform.services.cms.impl.DMSConfiguration;
import org.exoplatform.webui.application.WebuiRequestContext;
import org.exoplatform.webui.application.portlet.PortletRequestContext;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.lifecycle.UIFormLifecycle;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.EventListener;
import org.exoplatform.webui.form.UIForm;
import org.exoplatform.webui.form.UIFormStringInput;

/*
 * Created by The eXo Platform SAS Author : Anh Do Ngoc anh.do@exoplatform.com
 * Oct 31, 2008
 */
@ComponentConfig(
	lifecycle = UIFormLifecycle.class, 
	events = { 
		@EventConfig(listeners = UISearchBox.SearchActionListener.class) 
	}
)
public class UISearchBox extends UIForm {

	/** The template path. */
	private String							templatePath;

	/** The Constant KEYWORD_INPUT. */
	public static final String	KEYWORD_INPUT			= "keywordInput".intern();

	/** The Constant PORTAL_NAME_PARAM. */
	public static final String	PORTAL_NAME_PARAM	= "portal".intern();

	/** The Constant KEYWORD_PARAM. */
	public static final String	KEYWORD_PARAM			= "keyword".intern();

	/**
	 * Instantiates a new uI search box.
	 * 
	 * @throws Exception the exception
	 */
	public UISearchBox() throws Exception {
		UIFormStringInput uiKeywordInput = new UIFormStringInput(KEYWORD_INPUT, KEYWORD_INPUT, null);
		addChild(uiKeywordInput);
	}

	/**
	 * Sets the template path.
	 * 
	 * @param templatePath the new template path
	 */
	public void setTemplatePath(String templatePath) {
		this.templatePath = templatePath;
	}

	/*
	 * (non-Javadoc)
	 * @see org.exoplatform.webui.core.UIComponent#getTemplate()
	 */
	public String getTemplate() {
		return templatePath;
	}

	/**
	 * Gets the repository.
	 * 
	 * @return the repository
	 */
	private String getRepository() {
		PortletRequestContext portletRequestContext = WebuiRequestContext.getCurrentInstance();
		PortletPreferences portletPreferences = portletRequestContext.getRequest().getPreferences();
		return portletPreferences.getValue(UIWCMSearchPortlet.REPOSITORY, null);
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * org.exoplatform.webui.core.UIComponent#getTemplateResourceResolver(org.
	 * exoplatform.webui.application.WebuiRequestContext, java.lang.String)
	 */
	public ResourceResolver getTemplateResourceResolver(WebuiRequestContext context, String template) {
		try {
			String repository = getRepository();
			DMSConfiguration dmsConfiguration = getApplicationComponent(DMSConfiguration.class);
			String workspace = dmsConfiguration.getConfig().getSystemWorkspace();
			return new JCRResourceResolver(repository, workspace, "exo:templateFile");
		} catch (Exception e) {
			return null;
		}
	}

	/**
	 * The listener interface for receiving searchAction events. The class that is
	 * interested in processing a searchAction event implements this interface,
	 * and the object created with that class is registered with a component using
	 * the component's
	 * <code>addSearchActionListener<code> method. When the searchAction
	 * event occurs, that object's appropriate method is invoked.
	 * 
	 * @see SearchActionEvent
	 */
	public static class SearchActionListener extends EventListener<UISearchBox> {

		/*
		 * (non-Javadoc)
		 * @see
		 * org.exoplatform.webui.event.EventListener#execute(org.exoplatform.webui
		 * .event.Event)
		 */
		public void execute(Event<UISearchBox> event) throws Exception {
			UISearchBox uiSearchBox = event.getSource();
			UIPortal uiPortal = Util.getUIPortal();
			String keyword = uiSearchBox.getUIStringInput(UISearchBox.KEYWORD_INPUT).getValue();
			String portalName = uiPortal.getName();
			PortalRequestContext prContext = Util.getPortalRequestContext();
			HttpServletRequest request = prContext.getRequest();
			prContext.setResponseComplete(true);
			portalName = URLEncoder.encode(portalName, "UTF-8");
			String redirect = request.getContextPath() + "/private/" + portalName + "/searchResult?"
					+ PORTAL_NAME_PARAM + "=" + portalName + "&" + KEYWORD_PARAM + "=" + keyword;
			prContext.getResponse().sendRedirect(redirect);
		}
	}
}
