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
import javax.jcr.PathNotFoundException;
import javax.jcr.Session;
import javax.jcr.query.Row;
import javax.portlet.PortletPreferences;

import org.exoplatform.ecm.resolver.JCRResourceResolver;
import org.exoplatform.resolver.ResourceResolver;
import org.exoplatform.services.cms.impl.DMSConfiguration;
import org.exoplatform.services.jcr.access.PermissionType;
import org.exoplatform.services.jcr.core.ExtendedNode;
import org.exoplatform.services.wcm.core.NodeLocation;
import org.exoplatform.services.wcm.search.base.SearchDataCreator;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;
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
   * Get portlet name.
   *
   * @throws Exception the exception
   */
  public abstract String getPortletName() throws Exception;

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

  public String getEditLink(boolean isEditable, boolean isNew) throws Exception {
    String folderPath = this.getAncestorOfType(UICLVPortlet.class).getFolderPath();
    if (folderPath==null) folderPath="";
    Node folderNode = null;
    try{
      folderNode = getFolderNode(folderPath);
    }catch(PathNotFoundException e){
      folderNode = getFolderNode("");
    }
    return Utils.getEditLink(folderNode, isEditable, isNew);
  }

  public Node getFolderNode() {
    return NodeLocation.getNodeByExpression(
          Utils.getPortletPreference(UICLVPortlet.PREFERENCE_ITEM_PATH));
  }

  private Node getFolderNode(String oldPath) throws Exception {
      if ((oldPath==null) || ((oldPath!=null) && (oldPath.length()==0))) return null;
      int slashIndex = oldPath.indexOf("/");
      String path = oldPath.substring(slashIndex);
      String[] repoWorkspace = oldPath.substring(0, slashIndex).split(":");
      String strWorkspace = repoWorkspace[1];
      Session session = WCMCoreUtils.getUserSessionProvider().getSession(strWorkspace, WCMCoreUtils.getRepository());
      return (Node)session.getItem(path);
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
    PortletPreferences portletPreferences = Utils.getAllPortletPreferences();
    String currentApplicationMode = portletPreferences.getValue(UICLVPortlet.PREFERENCE_APPLICATION_TYPE, null);    
    if (currentApplicationMode.equals(UICLVPortlet.APPLICATION_CLV_BY_QUERY)) 
      return false;
    
    return UICLVPortlet.DISPLAY_MODE_AUTOMATIC.equals(
              Utils.getPortletPreference(UICLVPortlet.PREFERENCE_DISPLAY_MODE));
  }
  
  public boolean hasFolderPath() {
    PortletPreferences portletPreferences = Utils.getAllPortletPreferences();
    String itemPath = portletPreferences.getValue(UICLVPortlet.PREFERENCE_ITEM_PATH, null);
        
    return (itemPath != null && itemPath.length() > 0) ? true : false;
  }

  public boolean isShowManageContent() {
    return (Utils.isShowQuickEdit() && isModeByFolder() && hasFolderPath());
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

  public static class CLVNodeCreator implements SearchDataCreator<NodeLocation> {

    @Override
    public NodeLocation createData(Node node, Row row) {
      return NodeLocation.getNodeLocationByNode(node);
    }
  }
  
}
