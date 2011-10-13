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
package org.exoplatform.ecm.webui.component.explorer.control;

import org.exoplatform.ecm.jcr.model.Preference;
import org.exoplatform.ecm.webui.component.explorer.UIJCRExplorer;
import org.exoplatform.ecm.webui.component.explorer.UIJCRExplorerPortlet;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIPopupComponent;
import org.exoplatform.webui.core.UIPopupContainer;
import org.exoplatform.webui.core.lifecycle.UIFormLifecycle;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.EventListener;
import org.exoplatform.webui.event.Event.Phase;
import org.exoplatform.webui.form.UIForm;
import org.exoplatform.webui.form.UIFormCheckBoxInput;

/**
 * Created by The eXo Platform SARL
 * Author : Nguyen Anh Vu
 *          anhvurz90@gmail.com
 * Oct 27, 2009
 * 10:22:38 AM
 */
@ComponentConfig(
    lifecycle = UIFormLifecycle.class,
    template = "system:/groovy/webui/form/UIFormWithTitle.gtmpl",
    events = {
      @EventConfig(listeners = UIAllItemsPreferenceForm.SaveActionListener.class),
      @EventConfig(listeners = UIAllItemsPreferenceForm.CancelActionListener.class, phase = Phase.DECODE)
    }
)
public class UIAllItemsPreferenceForm extends UIForm implements UIPopupComponent {

  final static public String FIELD_SHOW_OWNED_BY_USER_DOC = "showOwnedByUser";
  final static public String FIELD_SHOW_FAVOURITES = "showFavourites";
  final static public String FIELD_SHOW_HIDDENS = "showHiddens";

  public UIAllItemsPreferenceForm() throws Exception {
    addUIFormInput(new UIFormCheckBoxInput<Boolean>(FIELD_SHOW_OWNED_BY_USER_DOC,
                            FIELD_SHOW_OWNED_BY_USER_DOC,
                            null));
    addUIFormInput(new UIFormCheckBoxInput<Boolean>(FIELD_SHOW_FAVOURITES,
                            FIELD_SHOW_FAVOURITES,
                            null));
    addUIFormInput(new UIFormCheckBoxInput<Boolean>(FIELD_SHOW_HIDDENS,
                            FIELD_SHOW_HIDDENS,
                            null));
  }

  public void activate() throws Exception {
  }

  public void deActivate() throws Exception {
  }

  public void update(Preference pref) {
    getUIFormCheckBoxInput(FIELD_SHOW_OWNED_BY_USER_DOC).setChecked(pref.isShowOwnedByUserDoc());
    getUIFormCheckBoxInput(FIELD_SHOW_FAVOURITES).setChecked(pref.isShowFavouriteDoc());
    getUIFormCheckBoxInput(FIELD_SHOW_HIDDENS).setChecked(pref.isShowHiddenDoc());
  }

  static public class SaveActionListener extends EventListener<UIAllItemsPreferenceForm> {
    public void execute(Event<UIAllItemsPreferenceForm> event) throws Exception {
      UIAllItemsPreferenceForm uiForm = event.getSource();
      UIJCRExplorerPortlet uiExplorerPortlet = uiForm.getAncestorOfType(UIJCRExplorerPortlet.class);
      UIJCRExplorer uiExplorer = uiExplorerPortlet.findFirstComponentOfType(UIJCRExplorer.class);
      Preference pref = uiExplorer.getPreference();
      pref.setShowOwnedByUserDoc(
          uiForm.getUIFormCheckBoxInput(FIELD_SHOW_OWNED_BY_USER_DOC).isChecked());
      pref.setShowFavouriteDoc(
          uiForm.getUIFormCheckBoxInput(FIELD_SHOW_FAVOURITES).isChecked());
      pref.setShowHiddenDoc(
          uiForm.getUIFormCheckBoxInput(FIELD_SHOW_HIDDENS).isChecked());
      uiExplorer.refreshExplorer();
      uiExplorerPortlet.setRenderedChild(UIJCRExplorer.class);
    }
  }

  static public class CancelActionListener extends EventListener<UIAllItemsPreferenceForm> {
    public void execute(Event<UIAllItemsPreferenceForm> event) throws Exception {
      UIAllItemsPreferenceForm uiForm = event.getSource();
      UIJCRExplorerPortlet uiExplorerPortlet = uiForm.getAncestorOfType(UIJCRExplorerPortlet.class);
      UIJCRExplorer uiExplorer = uiExplorerPortlet.findFirstComponentOfType(UIJCRExplorer.class);
      uiExplorer.getChild(UIPopupContainer.class).cancelPopupAction();
    }
  }

}
