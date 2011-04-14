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

import javax.jcr.Node;
import javax.portlet.PortletPreferences;

import org.exoplatform.ecm.resolver.JCRResourceResolver;
import org.exoplatform.resolver.ResourceResolver;
import org.exoplatform.services.cms.impl.DMSConfiguration;
import org.exoplatform.services.jcr.access.PermissionType;
import org.exoplatform.services.jcr.core.ExtendedNode;
import org.exoplatform.services.wcm.core.NodeLocation;
import org.exoplatform.wcm.webui.Utils;
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
public abstract class UICLVContainer extends UIContainer {

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

  public String getEditLink(boolean isEditable, boolean isNew) {
    String itemPath = Utils.getPortletPreference(UICLVPortlet.PREFERENCE_ITEM_PATH);
    if (itemPath==null) itemPath="";
    return Utils.getEditLink(correctPath(itemPath), isEditable, isNew);
  }

  public Node getFolderNode() {
    return NodeLocation.getNodeByExpression(
          Utils.getPortletPreference(UICLVPortlet.PREFERENCE_ITEM_PATH));
  }

  private String correctPath(String oldPath) {
      if ((oldPath==null) || ((oldPath!=null) && (oldPath.length()==0))) return "";
      int slashIndex = oldPath.indexOf("/");
      String path = oldPath.substring(slashIndex + 1);
      String[] repoWorkspace = oldPath.substring(0, slashIndex).split(":");
      return repoWorkspace[0] + '/' + repoWorkspace[1] + '/' + path;
  }


  /**
   * Gets the template resource resolver.
   *
   * @return the template resource resolver
   *
   * @throws Exception the exception
   */
  public ResourceResolver getTemplateResourceResolver() throws Exception {
    DMSConfiguration dmsConfiguration = getApplicationComponent(DMSConfiguration.class);
    String workspace = dmsConfiguration.getConfig().getSystemWorkspace();
    return new JCRResourceResolver(workspace);
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
   */
  public static class PreferencesActionListener extends EventListener<UICLVFolderMode> {
    /* (non-Javadoc)
     * @see org.exoplatform.webui.event.EventListener#execute(org.exoplatform.webui.event.Event)
     */
    public void execute(Event<UICLVFolderMode> event) throws Exception {
      UICLVContainer clvContainer = event.getSource();
      UICLVConfig viewerManagementForm = clvContainer.createUIComponent(UICLVConfig.class, null, null);
      Utils.createPopupWindow(clvContainer, viewerManagementForm, "UIViewerManagementPopupWindow", 800);
    }
  }


  public void onRefresh(Event<UICLVPresentation> event) throws Exception {
    UICLVPresentation clvPresentation = event.getSource();
    UICLVContainer uiListViewerBase = clvPresentation.getParent();
    uiListViewerBase.getChildren().clear();
    uiListViewerBase.init();
  }

  public boolean isModeByFolder() {
    return UICLVPortlet.DISPLAY_MODE_AUTOMATIC.equals(
              Utils.getPortletPreference(UICLVPortlet.PREFERENCE_DISPLAY_MODE));
  }

  public boolean isShowManageContent() {
    return (Utils.isShowQuickEdit() && isModeByFolder());
  }

  public boolean isShowAddContent() {
    if (isShowManageContent()) {
      PortletPreferences portletPreferences = ((PortletRequestContext) WebuiRequestContext.
          getCurrentInstance()).getRequest().getPreferences();
      String itemPath = portletPreferences.getValue(UICLVPortlet.PREFERENCE_ITEM_PATH, null);
      try {
        Node content = NodeLocation.getNodeByExpression(itemPath);
        ((ExtendedNode) content).checkPermission(PermissionType.ADD_NODE);
      } catch (Exception e) {
        return false;
      }
      return true;
    } else return false;
  }

  public boolean isShowPreferences() {
    try {
      return Utils.isShowQuickEdit() && Utils.hasEditPermissionOnPage();
    } catch (Exception e) {
      return false;
    }
  }

}
