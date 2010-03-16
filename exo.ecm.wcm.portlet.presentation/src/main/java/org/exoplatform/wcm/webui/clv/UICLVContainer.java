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

import javax.portlet.PortletPreferences;

import org.exoplatform.ecm.resolver.JCRResourceResolver;
import org.exoplatform.resolver.ResourceResolver;
import org.exoplatform.services.cms.impl.DMSConfiguration;
import org.exoplatform.wcm.webui.Utils;
import org.exoplatform.wcm.webui.clv.config.UICLVConfig;
import org.exoplatform.webui.application.WebuiRequestContext;
import org.exoplatform.webui.application.portlet.PortletRequestContext;
import org.exoplatform.webui.core.UIContainer;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.EventListener;

/**
 * Created by The eXo Platform SAS
 * Author : anh.do
 * anh.do@exoplatform.com, anhdn86@gmail.com
 * Feb 23, 2009
 */
public abstract class UICLVContainer extends UIContainer implements RefreshDelegateActionListener {

  /** The message key. */
  protected String  messageKey;

  /**
   * Inits the.
   * 
   * @throws Exception the exception
   */
  public abstract void init() throws Exception;

  /**
   * Gets the message.
   * 
   * @return the message
   * 
   * @throws Exception the exception
   */
  public String getMessageKey() throws Exception {
    return messageKey;
  }

  /**
   * Gets the portlet id.
   * 
   * @return the portlet id
   */
  public String getPortletId() {
    PortletRequestContext pContext = (PortletRequestContext) WebuiRequestContext.getCurrentInstance();
    return pContext.getWindowId();
  }

  /* (non-Javadoc)
   * @see org.exoplatform.webui.core.UIComponent#processRender(org.exoplatform.webui.application.WebuiRequestContext)
   */
  public void processRender(WebuiRequestContext context) throws Exception {
    if(!Boolean.parseBoolean(Utils.getCurrentMode()) || context.getFullRender()) {
      init(); 
    }    
    super.processRender(context);
  }
  
  /**
   * Gets the portlet preference.
   * 
   * @return the portlet preference
   */
  protected PortletPreferences getPortletPreference() {
    PortletRequestContext portletRequestContext = WebuiRequestContext.getCurrentInstance();
    return portletRequestContext.getRequest().getPreferences();
  }

  /**
   * Gets the form view template path.
   * 
   * @return the form view template path
   */
  protected String getFormViewTemplatePath() {
    return getPortletPreference().getValue(UICLVPortlet.FORM_VIEW_TEMPLATE_PATH, null);
  }

  /**
   * Gets the template resource resolver.
   * 
   * @return the template resource resolver
   * 
   * @throws Exception the exception
   */
  public ResourceResolver getTemplateResourceResolver() throws Exception {
    String repository = getPortletPreference().getValue(UICLVPortlet.REPOSITORY, null);
    DMSConfiguration dmsConfiguration = getApplicationComponent(DMSConfiguration.class);
    String workspace = dmsConfiguration.getConfig(repository).getSystemWorkspace();
    return new JCRResourceResolver(repository, workspace, "exo:templateFile");
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
  public static class QuickEditActionListener extends EventListener<UICLVFolderMode> {
    
    /* (non-Javadoc)
     * @see org.exoplatform.webui.event.EventListener#execute(org.exoplatform.webui.event.Event)
     */
    public void execute(Event<UICLVFolderMode> event) throws Exception {
      UICLVContainer uiListViewerBase = event.getSource();
      UICLVConfig viewerManagementForm = uiListViewerBase.createUIComponent(UICLVConfig.class, null, null);
      Utils.createPopupWindow(uiListViewerBase, viewerManagementForm, "UIViewerManagementPopupWindow", 800);
    }    
  }

  /* (non-Javadoc)
   * @see org.exoplatform.wcm.webui.clv.RefreshDelegateActionListener#onRefresh(org.exoplatform.webui.event.Event)
   */
  public void onRefresh(Event<UICLVPresentation> event) throws Exception {
    UICLVPresentation contentListPresentation = event.getSource();
    UICLVContainer uiListViewerBase = contentListPresentation.getParent();
    uiListViewerBase.getChildren().clear();
    uiListViewerBase.init();
  }
}
