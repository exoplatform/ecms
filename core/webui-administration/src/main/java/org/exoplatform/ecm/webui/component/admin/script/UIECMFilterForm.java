/*
 * Copyright (C) 2003-2007 eXo Platform SAS.
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
package org.exoplatform.ecm.webui.component.admin.script;

import java.util.ArrayList;
import java.util.List;

import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.lifecycle.UIFormLifecycle;
import org.exoplatform.webui.core.model.SelectItemOption;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.EventListener;
import org.exoplatform.webui.form.UIForm;
import org.exoplatform.webui.form.UIFormSelectBox;

/**
 * Created by The eXo Platform SARL
 * Author : pham tuan
 * phamtuanchip@yahoo.de September 27, 2006 10:27:15 AM
 */

@ComponentConfig(
    lifecycle = UIFormLifecycle.class,
    template = "app:/groovy/webui/component/admin/script/UIECMFilterForm.gtmpl",
    events = {@EventConfig(listeners = UIECMFilterForm.ChangeActionListener.class)}
)

public class UIECMFilterForm extends UIForm {
  final static public String FIELD_SELECT_SCRIPT = "selectScript" ;

  public UIECMFilterForm() throws Exception {
    UIFormSelectBox scriptSelect =
      new UIFormSelectBox(FIELD_SELECT_SCRIPT, FIELD_SELECT_SCRIPT, new ArrayList <SelectItemOption<String>>()) ;
    scriptSelect.setOnChange("Change") ;
    addUIFormInput(scriptSelect) ;
  }

  public void setOptions(List <SelectItemOption<String>> options) {
    getUIFormSelectBox(FIELD_SELECT_SCRIPT).setOptions(options) ;
  }

  static public class ChangeActionListener extends EventListener<UIECMFilterForm> {
    public void execute(Event<UIECMFilterForm> event) throws Exception {
      UIECMFilterForm uiForm = event.getSource() ;
      UIECMScripts uiECMScripts = uiForm.getParent() ;
      UIScriptList uiScriptList = uiECMScripts.getChildById(UIECMScripts.SCRIPTLIST_NAME) ;
      String categoryName = uiForm.getUIFormSelectBox(FIELD_SELECT_SCRIPT).getValue() ;
      uiScriptList.updateGrid(uiECMScripts.getECMScript(categoryName), 1);
      UIScriptManager sManager = uiForm.getAncestorOfType(UIScriptManager.class) ;
      sManager.setRenderedChild(UIECMScripts.class) ;
      event.getRequestContext().addUIComponentToUpdateByAjax(uiECMScripts) ;
    }
  }
}
