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
package org.exoplatform.ecm.webui.component.explorer.search;

import java.util.ArrayList;
import java.util.List;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

import org.exoplatform.services.cms.templates.TemplateService;
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
 * Author : Dang Van Minh
 *          minh.dang@exoplatform.com
 * May 11, 2007 4:21:57 PM
 */
@ComponentConfig(
    lifecycle = UIFormLifecycle.class,
    template =  "system:/groovy/webui/form/UIForm.gtmpl",
    events = {
      @EventConfig(listeners = UINodeTypeSelectForm.SaveActionListener.class),
      @EventConfig(listeners = UINodeTypeSelectForm.CancelActionListener.class, phase=Phase.DECODE)
    }
)
public class UINodeTypeSelectForm extends UIForm implements UIPopupComponent {

  public UINodeTypeSelectForm() throws Exception {
  }

  public String getLabel(ResourceBundle res, String id)  {
    try {
      return res.getString("UINodeTypeSelectForm.label." + id) ;
    } catch (MissingResourceException ex) {
      return id + " "; // Need for changed in gatein !fieldName.equals(field.getName())
    }
  }

  @SuppressWarnings("unchecked")
  public void setRenderNodeTypes() throws Exception {
    getChildren().clear() ;
    UIFormCheckBoxInput<String> uiCheckBox ;
    TemplateService templateService = getApplicationComponent(TemplateService.class) ;
    List<String> templates = templateService.getDocumentTemplates() ;
    for(String template : templates) {
      uiCheckBox = new UIFormCheckBoxInput<String>(template, template, "") ;
      if(propertiesSelected(template)) uiCheckBox.setChecked(true) ;
      else uiCheckBox.setChecked(false) ;
      addUIFormInput(uiCheckBox) ;
    }
  }

  private boolean propertiesSelected(String name) {
    UISearchContainer uiSearchContainer = getAncestorOfType(UISearchContainer.class) ;
    UIConstraintsForm uiConstraintsForm =
      uiSearchContainer.findFirstComponentOfType(UIConstraintsForm.class) ;
    String typeValues = uiConstraintsForm.getUIStringInput(UIConstraintsForm.DOC_TYPE).getValue() ;
    if(typeValues == null) return false ;
    if(typeValues.indexOf(",") > -1) {
      String[] values = typeValues.split(",") ;
      for(String value : values) {
        if(value.equals(name)) return true ;
      }
    } else if(typeValues.equals(name)) {
      return true ;
    }
    return false ;
  }

  public void setNodeTypes(List<String> selectedNodeTypes) {
    StringBuffer strNodeTypes = null;
    UISearchContainer uiContainer = getAncestorOfType(UISearchContainer.class);
    UIConstraintsForm uiConstraintsForm = uiContainer.findFirstComponentOfType(UIConstraintsForm.class);
    for (int i = 0; i < selectedNodeTypes.size(); i++) {
      if (strNodeTypes == null) {
        strNodeTypes = new StringBuffer(selectedNodeTypes.get(i));
      } else {
        strNodeTypes.append(",").append(selectedNodeTypes.get(i));
      }
    }
    uiConstraintsForm.getUIStringInput(UIConstraintsForm.DOC_TYPE)
                     .setValue(strNodeTypes.toString());
  }

  public void activate() throws Exception {}

  public void deActivate() throws Exception {}

  static public class SaveActionListener extends EventListener<UINodeTypeSelectForm> {
    public void execute(Event<UINodeTypeSelectForm> event) throws Exception {
      UINodeTypeSelectForm uiForm = event.getSource() ;
      UISearchContainer uiSearchContainer = uiForm.getAncestorOfType(UISearchContainer.class) ;
      UIConstraintsForm uiConstraintsForm =
        uiSearchContainer.findFirstComponentOfType(UIConstraintsForm.class) ;
      List<String> selectedNodeTypes = new ArrayList<String>() ;
      List<UIFormCheckBoxInput> listCheckbox =  new ArrayList<UIFormCheckBoxInput>();
      uiForm.findComponentOfType(listCheckbox, UIFormCheckBoxInput.class);
      String nodeTypesValue =
        uiConstraintsForm.getUIStringInput(UIConstraintsForm.DOC_TYPE).getValue() ;
      if(nodeTypesValue != null && nodeTypesValue.length() > 0) {
        String[] array = nodeTypesValue.split(",") ;
        for(int i = 0; i < array.length; i ++) {
          selectedNodeTypes.add(array[i].trim()) ;
        }
      }
      for(int i = 0; i < listCheckbox.size(); i ++) {
        if(listCheckbox.get(i).isChecked()) {
          if(!selectedNodeTypes.contains(listCheckbox.get(i).getName())) {
            selectedNodeTypes.add(listCheckbox.get(i).getName()) ;
          }
        } else if(selectedNodeTypes.contains(listCheckbox.get(i))) {
          selectedNodeTypes.remove(listCheckbox.get(i).getName()) ;
        } else {
          selectedNodeTypes.remove(listCheckbox.get(i).getName()) ;
        }
      }
      /* Set value for textbox */
      uiForm.setNodeTypes(selectedNodeTypes) ;
      /* Set value of checkbox is checked */
      uiConstraintsForm.getUIFormCheckBoxInput(UIConstraintsForm.NODETYPE_PROPERTY).setChecked(true);
      UIPopupContainer uiPopup = uiSearchContainer.getChild(UIPopupContainer.class) ;
      uiPopup.deActivate() ;
      event.getRequestContext().addUIComponentToUpdateByAjax(uiSearchContainer) ;
    }
  }

  static public class CancelActionListener extends EventListener<UINodeTypeSelectForm> {
    public void execute(Event<UINodeTypeSelectForm> event) throws Exception {
      UISearchContainer uiSearchContainer = event.getSource().getAncestorOfType(UISearchContainer.class) ;
      UIPopupContainer uiPopup = uiSearchContainer.getChild(UIPopupContainer.class) ;
      uiPopup.deActivate() ;
      event.getRequestContext().addUIComponentToUpdateByAjax(uiSearchContainer) ;
    }
  }
}
