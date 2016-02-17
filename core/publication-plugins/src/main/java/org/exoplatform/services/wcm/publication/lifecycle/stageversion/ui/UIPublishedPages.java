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
package org.exoplatform.services.wcm.publication.lifecycle.stageversion.ui;

import java.util.ArrayList;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.Value;

import org.exoplatform.portal.webui.container.UIContainer;
import org.exoplatform.services.wcm.publication.PublicationUtil;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.lifecycle.Lifecycle;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.EventListener;

/**
 * Created by The eXo Platform SAS
 * Author : Hoa Pham
 * hoa.pham@exoplatform.com
 * Sep 9, 2008
 */

@ComponentConfig(
    lifecycle = Lifecycle.class,
    template = "classpath:groovy/wcm/webui/publication/lifecycle/stageversion/ui/UIPublishedPages.gtmpl",
    events = {
      @EventConfig(listeners=UIPublishedPages.SelectNavigationNodeURIActionListener.class)
    }
)

public class UIPublishedPages extends UIContainer {

  /** The selected navigation node uri. */
  private String selectedNavigationNodeURI;

  /** The list navigation node uri. */
  private List<String> listNavigationNodeURI;

  /**
   * Gets the list navigation node uri.
   *
   * @return the list navigation node uri
   */
  public List<String> getListNavigationNodeURI() {return listNavigationNodeURI;}

  /**
   * Sets the list navigation node uri.
   *
   * @param listNavigationNodeURI the new list navigation node uri
   */
  public void setListNavigationNodeURI(List<String> listNavigationNodeURI) {this.listNavigationNodeURI = listNavigationNodeURI;}

  /**
   * Gets the selected navigation node uri.
   *
   * @return the selected navigation node uri
   */
  public String getSelectedNavigationNodeURI() {return selectedNavigationNodeURI;}

  /**
   * Sets the selected navigation node uri.
   *
   * @param selectedNavigationNodeURI the new selected navigation node uri
   */
  public void setSelectedNavigationNodeURI(String selectedNavigationNodeURI) {
    this.selectedNavigationNodeURI = selectedNavigationNodeURI;
  }

  /**
   * Inits the.
   *
   * @throws Exception the exception
   */
  public void init() throws Exception {
    UIPublicationPages publishingPanel = getAncestorOfType(UIPublicationPages.class);
    Node contentNode = publishingPanel.getNode();
    if (contentNode.hasProperty("publication:navigationNodeURIs")) {
      listNavigationNodeURI = new ArrayList<String>();
      Value[] values = null;
      try{
        values = contentNode.getProperty("publication:navigationNodeURIs").getValues();
      }catch(Exception ex){
        values = new Value[]{contentNode.getProperty("publication:navigationNodeURIs").getValue()};
      }
      for (Value value : values) {
        if (PublicationUtil.isNodeContentPublishedToPageNode(contentNode, value.getString())) {
          if(!listNavigationNodeURI.contains(value.getString()))
            listNavigationNodeURI.add(value.getString());
        }
      }
    } else {
      listNavigationNodeURI = new ArrayList<String>();
    }
  }

  /**
   * The listener interface for receiving selectNavigationNodeURIAction events.
   * The class that is interested in processing a selectNavigationNodeURIAction
   * event implements this interface, and the object created
   * with that class is registered with a component using the
   * component's <code>addSelectNavigationNodeURIActionListener</code> method. When
   * the selectNavigationNodeURIAction event occurs, that object's appropriate
   * method is invoked.
   */
  public static class SelectNavigationNodeURIActionListener extends EventListener<UIPublishedPages> {

    /* (non-Javadoc)
     * @see org.exoplatform.webui.event.EventListener#execute(org.exoplatform.webui.event.Event)
     */
    public void execute(Event<UIPublishedPages> event) throws Exception {
      UIPublishedPages publishedPages = event.getSource();
      String selectedTreeNode = event.getRequestContext().getRequestParameter(OBJECTID);
      publishedPages.setSelectedNavigationNodeURI(selectedTreeNode);

      UIPublicationContainer publicationContainer = publishedPages.getAncestorOfType(UIPublicationContainer.class);
      UIPublicationPagesContainer publicationPagesContainer = publishedPages.
          getAncestorOfType(UIPublicationPagesContainer.class);
      publicationContainer.setActiveTab(publicationPagesContainer, event.getRequestContext());
    }
  }
}
