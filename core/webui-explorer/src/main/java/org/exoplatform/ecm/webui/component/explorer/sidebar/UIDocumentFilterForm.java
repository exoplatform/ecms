/***************************************************************************
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
 *
 **************************************************************************/
package org.exoplatform.ecm.webui.component.explorer.sidebar;

import java.util.ArrayList;
import java.util.List;

import org.exoplatform.ecm.webui.component.explorer.UIJCRExplorer;
import org.exoplatform.ecm.webui.component.explorer.UIJCRExplorerPortlet;
import org.exoplatform.services.cms.documents.DocumentTypeService;
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
 * Author : Phan Trong Lam
 *          lamptdev@gmail.com
 * Oct 27, 2009
 */
@ComponentConfig(lifecycle = UIFormLifecycle.class,
    template = "system:/groovy/webui/form/UIFormWithTitle.gtmpl",
    events = {
      @EventConfig(listeners = UIDocumentFilterForm.SaveActionListener.class),
      @EventConfig(phase = Phase.DECODE, listeners = UIDocumentFilterForm.CancelActionListener.class)
    }
)

  public class UIDocumentFilterForm extends UIForm implements UIPopupComponent {

  public UIDocumentFilterForm(){
  }

  public void activate() throws Exception {
  }

  public void deActivate() throws Exception {
  }

  public void invoke(List<String> checkedTypes) {
    DocumentTypeService documentTypeService = getApplicationComponent(DocumentTypeService.class);
    List<String> supportedTypes = documentTypeService.getAllSupportedType();

    for (String supportedName : supportedTypes) {
      addUIFormInput(new UIFormCheckBoxInput<Boolean>(supportedName, supportedName,null));

      for (String checkedTypeName : checkedTypes) {
        if (supportedName.equals(checkedTypeName)) {
          getUIFormCheckBoxInput(supportedName).setChecked(true);
          continue;
        }
      }
    }
  }

  static public class SaveActionListener extends EventListener<UIDocumentFilterForm> {
    public void execute(Event<UIDocumentFilterForm> event) throws Exception {
      UIDocumentFilterForm uiForm = event.getSource();
      UIJCRExplorerPortlet uiExplorerPorltet = uiForm.getAncestorOfType(UIJCRExplorerPortlet.class);
      UIJCRExplorer uiExplorer = uiExplorerPorltet.findFirstComponentOfType(UIJCRExplorer.class);
      DocumentTypeService documentTypeService = uiForm.getApplicationComponent(DocumentTypeService.class);
      List<String> supportedTypes = documentTypeService.getAllSupportedType();
      List<String> checkedSupportTypes = new ArrayList<String>();

      for (String checkedName : supportedTypes) {
        if (uiForm.getUIFormCheckBoxInput(checkedName).isChecked())
          checkedSupportTypes.add(checkedName);
      }
      uiExplorer.setCheckedSupportType(checkedSupportTypes);
      uiExplorer.setFilterSave(true);
      uiExplorer.refreshExplorer();
      uiExplorerPorltet.setRenderedChild(UIJCRExplorer.class);
    }
  }

  static public class CancelActionListener extends EventListener<UIDocumentFilterForm> {
    public void execute(Event<UIDocumentFilterForm> event) throws Exception {
      UIDocumentFilterForm uiForm = event.getSource();
      UIJCRExplorerPortlet explorerPorltet = uiForm.getAncestorOfType(UIJCRExplorerPortlet.class);
      UIJCRExplorer uiExplorer = explorerPorltet.findFirstComponentOfType(UIJCRExplorer.class);
      uiExplorer.getChild(UIPopupContainer.class).cancelPopupAction();
    }
  }
}
