/*
 * Copyright (C) 2003-2010 eXo Platform SAS.
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
package org.exoplatform.wcm.webui.authoring;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.jcr.Node;

import org.exoplatform.commons.utils.LazyPageList;
import org.exoplatform.commons.utils.ListAccess;
import org.exoplatform.commons.utils.ListAccessImpl;
import org.exoplatform.portal.application.PortalRequestContext;
import org.exoplatform.portal.webui.util.Util;
import org.exoplatform.services.cms.drives.ManageDriveService;
import org.exoplatform.services.wcm.core.NodeLocation;
import org.exoplatform.services.wcm.extensions.publication.PublicationManager;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;
import org.exoplatform.wcm.webui.Utils;
import org.exoplatform.webui.application.WebuiRequestContext;
import org.exoplatform.webui.application.portlet.PortletRequestContext;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIComponent;
import org.exoplatform.webui.core.UIContainer;
import org.exoplatform.webui.core.lifecycle.Lifecycle;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.EventListener;

/**
 * Created by The eXo Platform SAS
 * Author : eXoPlatform
 *          exo@exoplatform.com
 * Feb 2, 2010
 */
@ComponentConfig(lifecycle = Lifecycle.class,
                 template = "app:/groovy/authoring/UIDashboardForm.gtmpl",
                 events = {
    @EventConfig(listeners = UIDashboardForm.ShowDocumentActionListener.class),
    @EventConfig(listeners = UIDashboardForm.RefreshActionListener.class) })
public class UIDashboardForm extends UIContainer {

  private int pageSize_ = 10;

  public UIDashboardForm() throws Exception {
    addChild(UIDashBoardColumn.class, null, "UIDashboardDraft").setLabel("UIDashboardForm.label.mydraft");
    addChild(UIDashBoardColumn.class, null, "UIDashboardWaiting").setLabel("UIDashboardForm.label.waitingapproval");
    addChild(UIDashBoardColumn.class, null, "UIDashboardPublish").setLabel("UIDashboardForm.label.publishedtomorrow");
    refreshData();
  }

  public List<Node> getContents(String fromstate) {
    return getContents(fromstate, null, null);
  }

  public List<Node> getContents(String fromstate, String tostate) {
    return getContents(fromstate, tostate, null);
  }

  public List<Node> getContents(String fromstate, String tostate, String date) {
    PublicationManager manager = WCMCoreUtils.getService(PublicationManager.class);
    String user = PortalRequestContext.getCurrentInstance().getRemoteUser();
    String lang = Util.getPortalRequestContext().getLocale().getLanguage();
    List<Node> nodes = new ArrayList<Node>();
    List<Node> temp = new ArrayList<Node>();
    try {
      nodes = manager.getContents(fromstate, tostate, date, user, lang, 
              WCMCoreUtils.getRepository().getConfiguration().getDefaultWorkspaceName());
      Set<String> uuidList = new HashSet<String>();
      for(Node node : nodes) {
        String currentState = null;
        if(node.hasProperty("publication:currentState"))
          currentState = node.getProperty("publication:currentState").getString();
        if(currentState == null || !currentState.equals("published")) {
          if(!org.exoplatform.services.cms.impl.Utils.isInTrash(node) &&
            !uuidList.contains(node.getSession().getWorkspace().getName() + node.getUUID())) {
            uuidList.add(node.getSession().getWorkspace().getName() + node.getUUID());
            temp.add(node);
          }
        }
      }
    } catch (Exception e) {
      temp = new ArrayList<Node>();
    }
    return temp;
  }

  private void refreshData() {
    List<UIDashBoardColumn> children = new ArrayList<UIDashBoardColumn>();
    for (UIComponent component : getChildren()) {
      if (component instanceof UIDashBoardColumn) {
        children.add((UIDashBoardColumn)component);
      }
    }
    ListAccess<NodeLocation> draftNodes = new ListAccessImpl<NodeLocation>(NodeLocation.class,
        NodeLocation.getLocationsByNodeList(getContents("draft")));
    children.get(0).getUIPageIterator().setPageList(
    new LazyPageList<NodeLocation>(draftNodes,  pageSize_));

    ListAccess<NodeLocation> waitingNodes = new ListAccessImpl<NodeLocation>(NodeLocation.class,
    NodeLocation.getLocationsByNodeList(getContents("pending", "approved")));
    children.get(1).getUIPageIterator().setPageList(
    new LazyPageList<NodeLocation>(waitingNodes, pageSize_));

    ListAccess<NodeLocation> publishedNodes = new ListAccessImpl<NodeLocation>(NodeLocation.class,
    NodeLocation.getLocationsByNodeList(getContents("staged", null, "2")));
    children.get(2).getUIPageIterator().setPageList(
    new LazyPageList<NodeLocation>(publishedNodes, pageSize_));

  }

  public void processRender(WebuiRequestContext context) throws Exception
  {
    refreshData();
    super.processRender(context);
  }


  /**
   * The listener interface for receiving ShowDocumentAction events.
   * The class that is interested in processing a changeRepositoryAction
   * event implements this interface, and the object created
   * with that class is registered with a component using the
   * component's <code>addShowDocumentActionListener</code> method. When
   * the ShowDocumentAction event occurs, that object's appropriate
   * method is invoked.
   */
  public static class ShowDocumentActionListener extends EventListener<UIDashboardForm> {

    /* (non-Javadoc)
     * @see org.exoplatform.webui.event.EventListener#execute(org.exoplatform.webui.event.Event)
     */
    public void execute(Event<UIDashboardForm> event) throws Exception {
      PortalRequestContext context = Util.getPortalRequestContext();
      String path = event.getRequestContext().getRequestParameter(OBJECTID);
      HashMap<String, String> map = new HashMap<String, String>();
      ManageDriveService driveService = WCMCoreUtils.getService(ManageDriveService.class);
      map.put("repository", "repository");
      map.put("drive", driveService.getDriveOfDefaultWorkspace());
      map.put("path", path);
      context.setAttribute("jcrexplorer-show-document", map);
      Utils.updatePortal((PortletRequestContext) event.getRequestContext());

    }
  }

  /**
   * The listener interface for receiving RefreshAction events.
   * The class that is interested in processing a changeRepositoryAction
   * event implements this interface, and the object created
   * with that class is registered with a component using the
   * component's <code>RefreshActionListener</code> method. When
   * the RefreshAction event occurs, that object's appropriate
   * method is invoked.
   */
  public static class RefreshActionListener extends EventListener<UIDashboardForm> {

      /* (non-Javadoc)
       * @see org.exoplatform.webui.event.EventListener#execute(org.exoplatform.webui.event.Event)
       */
    public void execute(Event<UIDashboardForm> event) throws Exception {
      UIDashboardForm src = event.getSource();
      Utils.updatePortal((PortletRequestContext) event.getRequestContext());
    }
  }

}
