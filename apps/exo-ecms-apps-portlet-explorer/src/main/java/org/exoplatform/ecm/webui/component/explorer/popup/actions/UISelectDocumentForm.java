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
package org.exoplatform.ecm.webui.component.explorer.popup.actions;

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
 * Author : Pham Tuan
 *          phamtuanchip@yahoo.de
 * Nov 8, 2006 10:06:40 AM 
 */
@ComponentConfig(
    lifecycle = UIFormLifecycle.class,
    template = "app:/groovy/webui/component/explorer/UIFormWithoutAction.gtmpl",
    events = {
      @EventConfig(listeners = UISelectDocumentForm.ChangeActionListener.class)
    }
)
public class UISelectDocumentForm extends UIForm {

  final static public String FIELD_SELECT = "selectTemplate" ;

  public UISelectDocumentForm() {
    List<SelectItemOption<String>> options = new ArrayList<SelectItemOption<String>>() ;
    UIFormSelectBox templateSelect = new UIFormSelectBox(FIELD_SELECT, FIELD_SELECT, options) ;
    templateSelect.setOnChange("Change") ;  
    addUIFormInput(templateSelect) ;
  }

  static public class ChangeActionListener extends EventListener<UISelectDocumentForm> {
    public void execute(Event<UISelectDocumentForm> event) throws Exception {
      UISelectDocumentForm uiSelectForm = event.getSource() ;
      UIDocumentFormController uiDCFormController = uiSelectForm.getParent() ;
      UIDocumentForm documentForm = uiDCFormController.getChild(UIDocumentForm.class) ;
      documentForm.getChildren().clear() ;
      //  reset the interceptors
      documentForm.resetInterceptors();
      documentForm.resetProperties() ;
      // set path to DocumentForm
      documentForm.setContentType(uiSelectForm.getUIFormSelectBox(UISelectDocumentForm.FIELD_SELECT).getValue()) ;
      event.getRequestContext().addUIComponentToUpdateByAjax(uiDCFormController) ;
    }
  }

  public String getSelectValue() {
    return getUIFormSelectBox(FIELD_SELECT).getValue();
  }
}
