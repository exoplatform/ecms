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
package org.exoplatform.wcm.webui.selector.content.multi;

import org.exoplatform.ecm.webui.selector.UISelectable;
import org.exoplatform.wcm.webui.selector.content.UIContentBrowsePanel;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.lifecycle.Lifecycle;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.EventListener;

/**
 * The Class UIContentBrowsePanelMulti.
 */
@ComponentConfig(
  lifecycle = Lifecycle.class,
  template = "classpath:groovy/wcm/webui/selector/content/multi/UIContentBrowsePanel.gtmpl",
  events = {
    @EventConfig(listeners = UIContentBrowsePanel.ChangeContentTypeActionListener.class),
    @EventConfig(listeners = UIContentBrowsePanelMulti.SelectActionListener.class),
    @EventConfig(listeners = UIContentBrowsePanelMulti.CloseActionListener.class)
  }
)

public class UIContentBrowsePanelMulti extends UIContentBrowsePanel {

  /** The item paths. */
  private String itemPaths;
  
  /**
   * Gets the item paths.
   * 
   * @return the item paths
   */
  public String getItemPaths() {
    return itemPaths;
  }
  
  /**
   * Sets the item paths.
   * 
   * @param itemPaths the new item paths
   */
  public void setItemPaths(String itemPaths) {
    this.itemPaths = itemPaths;
  }

  /**
   * The listener interface for receiving selectAction events.
   * The class that is interested in processing a selectAction
   * event implements this interface, and the object created
   * with that class is registered with a component using the
   * component's <code>addSelectActionListener<code> method. When
   * the selectAction event occurs, that object's appropriate
   * method is invoked.
   * 
   * @see SelectActionEvent
   */
  public static class SelectActionListener extends EventListener<UIContentBrowsePanelMulti> {
    
    /* (non-Javadoc)
     * @see org.exoplatform.webui.event.EventListener#execute(org.exoplatform.webui.event.Event)
     */
    public void execute(Event<UIContentBrowsePanelMulti> event) throws Exception {
      UIContentBrowsePanelMulti contentBrowsePanelMulti = event.getSource();
      String returnFieldName = contentBrowsePanelMulti.getReturnFieldName();
      String itemPaths = event.getRequestContext().getRequestParameter(OBJECTID);
      ((UISelectable)(contentBrowsePanelMulti.getSourceComponent())).doSelect(returnFieldName, itemPaths);
    }
  }
  
  /**
   * The listener interface for receiving closeAction events.
   * The class that is interested in processing a closeAction
   * event implements this interface, and the object created
   * with that class is registered with a component using the
   * component's <code>addCloseActionListener<code> method. When
   * the closeAction event occurs, that object's appropriate
   * method is invoked.
   * 
   * @see CloseActionEvent
   */
  public static class CloseActionListener extends EventListener<UIContentBrowsePanelMulti> {
    
    /* (non-Javadoc)
     * @see org.exoplatform.webui.event.EventListener#execute(org.exoplatform.webui.event.Event)
     */
    public void execute(Event<UIContentBrowsePanelMulti> event) throws Exception {
      UIContentBrowsePanelMulti contentBrowsePanelMulti = event.getSource();
      ((UISelectable)(contentBrowsePanelMulti.getSourceComponent())).doSelect(null, null);
    }
  }
	
}
