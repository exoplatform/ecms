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
import java.util.List;

import javax.jcr.Node;

import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.portal.application.PortalRequestContext;
import org.exoplatform.portal.webui.util.Util;
import org.exoplatform.services.wcm.extensions.publication.PublicationManager;
import org.exoplatform.wcm.webui.Utils;
import org.exoplatform.webui.application.portlet.PortletRequestContext;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.lifecycle.UIFormLifecycle;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.EventListener;
import org.exoplatform.webui.form.UIForm;

/**
 * Created by The eXo Platform SAS
 * Author : eXoPlatform
 *          exo@exoplatform.com
 * Feb 2, 2010
 */
@ComponentConfig(lifecycle = UIFormLifecycle.class, 
                 template = "app:/groovy/authoring/UIDashboardForm.gtmpl", 
                 events = {
    @EventConfig(listeners = UIDashboardForm.ShowDocumentActionListener.class),
    @EventConfig(listeners = UIDashboardForm.RefreshActionListener.class) })
public class UIDashboardForm extends UIForm {

  public UIDashboardForm() {
  }

  public List<Node> getContents(String fromstate) {
    return getContents(fromstate, null, null);
  }

  public List<Node> getContents(String fromstate, String tostate) {
    return getContents(fromstate, tostate, null);
  }

  public List<Node> getContents(String fromstate, String tostate, String date) {
    PublicationManager manager = (PublicationManager) ExoContainerContext.getCurrentContainer()
                                                                         .getComponentInstanceOfType(PublicationManager.class);
    String user = PortalRequestContext.getCurrentInstance().getRemoteUser();
    String lang = Util.getPortalRequestContext().getLocale().getLanguage();
    List<Node> nodes = new ArrayList<Node>();
    List<Node> temp = new ArrayList<Node>();
    try {
      nodes = manager.getContents(fromstate, tostate, date, user, lang, "collaboration");
      for(Node node : nodes) {
        if(!org.exoplatform.services.cms.impl.Utils.isInTrash(node)) {
          temp.add(node);
        }
      }
    } catch (Exception e) {
      temp = new ArrayList<Node>();
    }
    return temp;
  }

  /**
   * The listener interface for receiving ShowDocumentAction events.
   * The class that is interested in processing a changeRepositoryAction
   * event implements this interface, and the object created
   * with that class is registered with a component using the
   * component's <code>addShowDocumentActionListener<code> method. When
   * the ShowDocumentAction event occurs, that object's appropriate
   * method is invoked.
   *
   * @see ShowDocumentActionEvent
   */
  public static class ShowDocumentActionListener extends EventListener<UIDashboardForm> {

    /* (non-Javadoc)
     * @see org.exoplatform.webui.event.EventListener#execute(org.exoplatform.webui.event.Event)
     */
    public void execute(Event<UIDashboardForm> event) throws Exception {
      PortalRequestContext context = Util.getPortalRequestContext();
      String path = event.getRequestContext().getRequestParameter(OBJECTID);
      HashMap<String, String> map = new HashMap<String, String>();
    map.put("repository", "repository");
    map.put("drive", "collaboration");
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
   * component's <code>RefreshActionListener<code> method. When
   * the RefreshAction event occurs, that object's appropriate
   * method is invoked.
   *
   * @see RefreshActionEvent
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
