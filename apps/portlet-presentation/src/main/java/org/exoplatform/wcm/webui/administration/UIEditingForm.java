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
package org.exoplatform.wcm.webui.administration;

import java.util.ArrayList;
import java.util.List;

import org.exoplatform.portal.application.PortalRequestContext;
import org.exoplatform.portal.webui.util.Util;
import org.exoplatform.wcm.webui.Utils;
import org.exoplatform.webui.application.portlet.PortletRequestContext;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.lifecycle.UIFormLifecycle;
import org.exoplatform.webui.core.model.SelectItemOption;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.EventListener;
import org.exoplatform.webui.form.UIForm;
import org.exoplatform.webui.form.UIFormSelectBox;

/**
 * Created by The eXo Platform SAS
 * Author : eXoPlatform
 *          exo@exoplatform.com
 * Feb 2, 2010
 */
@ComponentConfig(
                 lifecycle = UIFormLifecycle.class,
                 template = "app:/groovy/Editing/UIEditingToolBar.gtmpl",
                 events = {
                   @EventConfig(listeners = UIEditingForm.ChangeEditingActionListener.class)
})
public class UIEditingForm extends UIForm {

  /** The Constant PUBLISHED. */
  public static final String PUBLISHED = "Published";

  /** The Constant DRAFT. */
  public static final String DRAFT = "Draft";

  /** The Constant EDITING_OPTIONS. */
  public static final String EDITING_OPTIONS = "EditingOptions";

  public UIEditingForm() {
    List<SelectItemOption<String>> editingOptions = new ArrayList<SelectItemOption<String>>();
    editingOptions.add(new SelectItemOption<String>(PUBLISHED, PUBLISHED));
    editingOptions.add(new SelectItemOption<String>(DRAFT, DRAFT));

    UIFormSelectBox orderBySelectBox = new UIFormSelectBox(EDITING_OPTIONS, EDITING_OPTIONS, editingOptions);
    orderBySelectBox.setOnChange("ChangeEditing");
    addChild(orderBySelectBox);
  }

  /**
   * The listener interface for receiving changeRepositoryAction events.
   * The class that is interested in processing a changeRepositoryAction
   * event implements this interface, and the object created
   * with that class is registered with a component using the
   * component's <code>addChangeRepositoryActionListener<code> method. When
   * the changeRepositoryAction event occurs, that object's appropriate
   * method is invoked.
   */
  public static class ChangeEditingActionListener extends EventListener<UIEditingForm> {

    /* (non-Javadoc)
     * @see org.exoplatform.webui.event.EventListener#execute(org.exoplatform.webui.event.Event)
     */
    public void execute(Event<UIEditingForm> event) throws Exception {
      UIEditingForm editingForm = event.getSource();
      PortalRequestContext context = Util.getPortalRequestContext();
      UIFormSelectBox options = editingForm.getChildById(UIEditingForm.EDITING_OPTIONS);
      String option = options.getValue() ;
      if(option.equals(UIEditingForm.PUBLISHED)) {
        context.getRequest().getSession().setAttribute(Utils.TURN_ON_QUICK_EDIT, false);
        Utils.updatePortal((PortletRequestContext) event.getRequestContext());
      } else {
        context.getRequest().getSession().setAttribute(Utils.TURN_ON_QUICK_EDIT, true);
        Utils.updatePortal((PortletRequestContext) event.getRequestContext());
      }
    }
  }
}
