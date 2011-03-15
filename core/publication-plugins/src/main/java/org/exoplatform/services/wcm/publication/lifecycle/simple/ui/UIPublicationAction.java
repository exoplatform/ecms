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
package org.exoplatform.services.wcm.publication.lifecycle.simple.ui;

import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.lifecycle.UIFormLifecycle;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.EventListener;
import org.exoplatform.webui.form.UIForm;

/**
 * Created by The eXo Platform SAS
 * Author : Phan Le Thanh Chuong
 * chuong.phan@exoplatform.com, phan.le.thanh.chuong@gmail.com
 * Sep 25, 2008
 */

@ComponentConfig (
    lifecycle = UIFormLifecycle.class,
    template = "classpath:groovy/wcm/webui/publication/lifecycle/simple/ui/UIPublicationAction.gtmpl",
    events = {
      @EventConfig(listeners = UIPublicationAction.AddActionListener.class),
      @EventConfig(listeners = UIPublicationAction.RemoveActionListener.class)
    }
)
public class UIPublicationAction extends UIForm {

  /**
   * Update ui.
   *
   * @throws Exception the exception
   */
  private void updateUI() throws Exception {}

  /**
   * The listener interface for receiving addAction events.
   * The class that is interested in processing a addAction
   * event implements this interface, and the object created
   * with that class is registered with a component using the
   * component's <code>addAddActionListener<code> method. When
   * the addAction event occurs, that object's appropriate
   * method is invoked.
   *
   * @see AddActionEvent
   */
  public static class AddActionListener extends EventListener<UIPublicationAction> {

    /* (non-Javadoc)
     * @see org.exoplatform.webui.event.EventListener#execute(org.exoplatform.webui.event.Event)
     */
    public void execute(Event<UIPublicationAction> event) throws Exception {
      UIPublicationAction publicationAction = event.getSource();

      publicationAction.updateUI();
    }
  }

  /**
   * The listener interface for receiving removeAction events.
   * The class that is interested in processing a removeAction
   * event implements this interface, and the object created
   * with that class is registered with a component using the
   * component's <code>addRemoveActionListener<code> method. When
   * the removeAction event occurs, that object's appropriate
   * method is invoked.
   *
   * @see RemoveActionEvent
   */
  public static class RemoveActionListener extends EventListener<UIPublicationAction> {

    /* (non-Javadoc)
     * @see org.exoplatform.webui.event.EventListener#execute(org.exoplatform.webui.event.Event)
     */
    public void execute(Event<UIPublicationAction> event) throws Exception {
      UIPublicationAction publicationAction = event.getSource();

      publicationAction.updateUI();
    }
  }
}
