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
package org.exoplatform.wcm.webui.newsletter.manager;

import org.exoplatform.ecm.webui.form.UIFormInputSetWithAction;
import org.exoplatform.ecm.webui.selector.UISelectable;
import org.exoplatform.wcm.webui.Utils;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.lifecycle.UIFormLifecycle;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.EventListener;
import org.exoplatform.webui.form.UIForm;
import org.exoplatform.webui.form.UIFormStringInput;

/**
 * Created by The eXo Platform SAS
 * Author : eXoPlatform
 * chuong.phan@exoplatform.com, phan.le.thanh.chuong@gmail.com
 * Jun 15, 2009
 */
@ComponentConfig (
    lifecycle = UIFormLifecycle.class,
    events = {
      @EventConfig (listeners = UINewsletterEntryWebcontentSelectorForm.SelectWebcontentActionListener.class)
    }
)
public class UINewsletterEntryWebcontentSelectorForm extends UIForm implements UISelectable {

  /** The popup id. */
  private String popupId;

  /** The Constant FORM_WEBCONTENT_SELECTOR. */
  public static final String FORM_WEBCONTENT_SELECTOR = "FormWebcontentSelector";

  /** The Constant INPUT_WEBCONTENT_SELECTOR. */
  public static final String INPUT_WEBCONTENT_SELECTOR = "WebcontentSelector";

  /**
   * Instantiates a new uI newsletter entry webcontent selector form.
   */
  public UINewsletterEntryWebcontentSelectorForm() {
    UIFormStringInput inputWebcontentSelector = new UIFormStringInput(INPUT_WEBCONTENT_SELECTOR, INPUT_WEBCONTENT_SELECTOR, null);
    inputWebcontentSelector.setEditable(false);

    UIFormInputSetWithAction formWebcontentSelector = new UIFormInputSetWithAction(FORM_WEBCONTENT_SELECTOR);
    formWebcontentSelector.addChild(inputWebcontentSelector);
    formWebcontentSelector.setActionInfo(INPUT_WEBCONTENT_SELECTOR, new String[] {"SelectWebcontent"});
    formWebcontentSelector.showActionInfo(true);

    addChild(formWebcontentSelector);
  }

  /**
   * Gets the popup id.
   *
   * @return the popup id
   */
  public String getPopupId() {
    return popupId;
  }

  /**
   * Sets the popup id.
   *
   * @param popupId the new popup id
   */
  public void setPopupId(String popupId) {
    this.popupId = popupId;
  }

  /* (non-Javadoc)
   * @see org.exoplatform.ecm.webui.selector.UISelectable#doSelect(java.lang.String, java.lang.Object)
   */
  public void doSelect(String selectField, Object value) throws Exception {
    getUIStringInput(selectField).setValue((String) value);
    Utils.closePopupWindow(this, popupId);
  }

  /**
   * The listener interface for receiving selectWebcontentAction events.
   * The class that is interested in processing a selectWebcontentAction
   * event implements this interface, and the object created
   * with that class is registered with a component using the
   * component's <code>addSelectWebcontentActionListener<code> method. When
   * the selectWebcontentAction event occurs, that object's appropriate
   * method is invoked.
   *
   * @see SelectWebcontentActionEvent
   */
  public static class SelectWebcontentActionListener extends EventListener<UINewsletterEntryWebcontentSelectorForm> {

    /* (non-Javadoc)
     * @see org.exoplatform.webui.event.EventListener#execute(org.exoplatform.webui.event.Event)
     */
    public void execute(Event<UINewsletterEntryWebcontentSelectorForm> event) throws Exception {
    }
  }
}
